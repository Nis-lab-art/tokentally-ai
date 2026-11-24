package tokentally.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tokentally.app.Engine;
import tokentally.cost.Recommender;
import tokentally.cost.Workload;
import tokentally.model.ModelPrice;
import tokentally.model.Page;
import tokentally.regex.InputValidator;
import tokentally.text.FrequencyCounter;

// rest endpoints for the react ui. each one just calls the engine
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

    // the form sends text like "2M" so the same regex checks as the command line are used
    @PostMapping("/recommend")
    public ArrayList<Recommender.Pick> recommend(@RequestBody Map<String, Object> body) {
        Workload w = new Workload();
        w.inputPerDay = tokens(body, "inputPerDay", true);
        w.outputPerDay = tokens(body, "outputPerDay", true);
        w.promptSize = Math.max(0, tokens(body, "promptSize", false));
        w.minContext = Math.max(0, tokens(body, "minContext", false));
        String cached = text(body, "cachedPercent");
        if (!cached.isEmpty()) {
            w.cachedShare = InputValidator.percent(cached);
            if (w.cachedShare < 0) {
                throw new IllegalArgumentException("cached share must be 0 to 100 percent");
            }
        }
        String budget = text(body, "budget");
        if (!budget.isEmpty()) {
            w.budget = InputValidator.money(budget);
            if (w.budget < 0) {
                throw new IllegalArgumentException("budget should look like 50 or $49.99");
            }
        }
        w.batchOk = Boolean.parseBoolean(text(body, "batchOk"));
        w.provider = text(body, "provider").toLowerCase();
        return engine.recommend(w, 10);
    }

    @PostMapping("/crawl")
    public synchronized ArrayList<String> crawl() throws Exception {
        return engine.recrawl();
    }

    private static String text(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private static long tokens(Map<String, Object> body, String key, boolean required) {
        String v = text(body, key);
        if (v.isEmpty()) {
            if (required) {
                throw new IllegalArgumentException(key + " is required");
            }
            return -1;
        }
        long n = InputValidator.tokens(v);
        if (n < 0) {
            throw new IllegalArgumentException("\"" + v + "\" is not a token amount, use 2M, 500K or 1,000,000");
        }
        return n;
    }

    // bad input gives a 400 with a short message instead of a stack trace
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
