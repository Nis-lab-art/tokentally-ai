package tokentally.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tokentally.app.Engine;
import tokentally.app.ErrorDemo;
import tokentally.app.Experiments;
import tokentally.cost.Recommender;
import tokentally.cost.Workload;
import tokentally.model.ModelPrice;
import tokentally.model.Page;
import tokentally.regex.InputValidator;
import tokentally.text.FrequencyCounter;

// owner: Abhinav Pandey, Nishan Giri
// web requests to the engine
@RestController
@RequestMapping("/api")
public class TokenTallyController {

    private final Engine engine;

    public TokenTallyController(Engine engine) {
        this.engine = engine;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("prices", engine.prices.size());
        m.put("pages", engine.pages.size());
        m.put("words", engine.index.words().size());
        m.put("searches", engine.searches.size());
        return m;
    }

    @GetMapping("/search")
    public Engine.SearchResult search(@RequestParam String q) {
        return engine.search(q);
    }

    @GetMapping("/complete")
    public ArrayList<String> complete(@RequestParam String prefix) {
        return engine.complete(prefix, 8);
    }

    @GetMapping("/frequency")
    public ArrayList<FrequencyCounter.Result> frequency(@RequestParam String word, @RequestParam(required = false) String url) {
        return engine.frequency(word, url);
    }

    @GetMapping("/top-searches")
    public ArrayList<Map<String, Object>> topSearches() {
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        for (Object[] t : engine.searches.top(10)) {
            out.add(Map.of("word", t[0], "count", t[1]));
        }
        return out;
    }

    @GetMapping("/pages")
    public ArrayList<Map<String, String>> pages() {
        ArrayList<Map<String, String>> out = new ArrayList<>();
        for (Page p : engine.pages) {
            out.add(Map.of("provider", p.provider, "url", p.url, "title", p.title));
        }
        return out;
    }

    @GetMapping("/models")
    public ArrayList<ModelPrice> models(@RequestParam(required = false) String provider, @RequestParam(required = false) String tier) {
        return engine.models(provider, tier);
    }

    @GetMapping("/models/{id}")
    public ArrayList<ModelPrice> card(@PathVariable String id) {
        if (!InputValidator.modelName(id)) {
            throw new IllegalArgumentException("not a model id");
        }
        return engine.card(id);
    }

    @GetMapping("/compare")
    public ArrayList<ModelPrice> compare(@RequestParam String ids) {
        if (ids == null || ids.isBlank()) {
            throw new IllegalArgumentException("type at least one model name, separated by commas");
        }
        String[] list = ids.split(",");
        for (String id : list) {
            if (!InputValidator.modelName(id)) {
                throw new IllegalArgumentException("\"" + id.trim() + "\" is not a model id");
            }
        }
        return engine.compare(list);
    }

    @GetMapping("/free")
    public ArrayList<ModelPrice> free() {
        return engine.freeModels();
    }

    @GetMapping("/warnings")
    public ArrayList<ModelPrice> warnings() {
        return engine.warnings();
    }

    @GetMapping("/patterns")
    public Object patterns(@RequestParam(required = false) String url) {
        return engine.patterns(url);
    }

    @GetMapping("/problems")
    public Object problems() {
        return engine.dataProblems();
    }

    // same checks as the menu
    @PostMapping("/recommend")
    public ArrayList<Recommender.Pick> recommend(@RequestBody Map<String, Object> body) {
        Workload w = new Workload();
        w.inputPerDay = tokens(body, "inputPerDay", "input tokens per day", true);
        w.outputPerDay = tokens(body, "outputPerDay", "output tokens per day", true);
        w.promptSize = Math.max(0, tokens(body, "promptSize", "tokens in one request", false));
        w.minContext = Math.max(0, tokens(body, "minContext", "smallest context window", false));
        String cached = text(body, "cachedPercent");
        if (!cached.isEmpty()) {
            w.cachedShare = InputValidator.percent(cached);
            if (w.cachedShare < 0) {
                throw new FieldProblem("cachedPercent", "the cached share must be a number from 0 to 100");
            }
        }
        String budget = text(body, "budget");
        if (!budget.isEmpty()) {
            w.budget = InputValidator.money(budget);
            if (w.budget < 0) {
                throw new FieldProblem("budget", "the budget should look like 50 or $49.99");
            }
        }
        if (w.inputPerDay + w.outputPerDay <= 0) {
            throw new FieldProblem("inputPerDay", "enter some input or output tokens, a workload of zero costs nothing");
        }
        w.batchOk = Boolean.parseBoolean(text(body, "batchOk"));
        w.skipFree = Boolean.parseBoolean(text(body, "skipFree"));
        w.provider = text(body, "provider").toLowerCase();
        return engine.recommend(w, 10);
    }

    // price changes
    @GetMapping("/history/changes")
    public Object historyChanges() throws Exception {
        return engine.history.changes();
    }

    // new models
    @GetMapping("/history/new")
    public Object historyNew() throws Exception {
        return engine.history.added();
    }

    @GetMapping("/history")
    public Object history(@RequestParam String model) throws Exception {
        return engine.history.of(model);
    }

    // bad input cases
    @GetMapping("/error-demo")
    public synchronized ArrayList<ErrorDemo.Case> errorDemo() {
        return ErrorDemo.run(engine);
    }

    // timing experiments
    @GetMapping("/experiments")
    public synchronized ArrayList<Experiments.Row> experiments() {
        return Experiments.run(engine);
    }

    @PostMapping("/crawl")
    public synchronized ArrayList<String> crawl() throws Exception {
        return engine.recrawl();
    }

    private static String text(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private static long tokens(Map<String, Object> body, String key, String label, boolean required) {
        String v = text(body, key);
        if (v.isEmpty()) {
            if (required) {
                throw new FieldProblem(key, "enter the " + label + ", for example 2M or 500K");
            }
            return -1;
        }
        long n = InputValidator.tokens(v);
        if (n < 0) {
            throw new FieldProblem(key, "\"" + v + "\" is not a valid amount for " + label + ", use 2M, 500K or 1,000,000 (not above 1000 trillion)");
        }
        return n;
    }

    // error tied to a field
    static class FieldProblem extends IllegalArgumentException {
        final String field;

        FieldProblem(String field, String message) {
            super(message);
            this.field = field;
        }
    }

    // bad input -> 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badInput(IllegalArgumentException e) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", e.getMessage());
        if (e instanceof FieldProblem f) {
            body.put("field", f.field);
        }
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> missingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "\"" + e.getParameterName() + "\" is missing, fill it in"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> badBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "the request could not be read, check the values and try again"));
    }

    // other errors -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "something went wrong on the server, nothing was changed. Please try again"));
    }
}
