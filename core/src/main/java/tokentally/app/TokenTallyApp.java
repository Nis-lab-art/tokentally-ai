package tokentally.app;

import java.util.ArrayList;
import java.util.Scanner;

import tokentally.cost.CostCalculator;
import tokentally.cost.Recommender;
import tokentally.cost.Workload;
import tokentally.model.ModelPrice;
import tokentally.rank.PageRanker;
import tokentally.regex.InputValidator;
import tokentally.text.FrequencyCounter;
import tokentally.text.SpellChecker;

// command line menu for every feature
public class TokenTallyApp {

    private static final String[] MENU = {
        "search pages and models",
        "word completion",
        "frequency count of a word",
        "top searches",
        "list models",
        "compare models side by side",
        "model card",
        "workload cost calculator",
        "free models and price warnings",
        "find patterns in the pages",
        "check the crawled data",
        "crawl the live pages again",
        "price history and changes",
        "exit"
    };

    private final Engine engine;
    private final Scanner in = new Scanner(System.in);

    public TokenTallyApp(Engine engine) {
        this.engine = engine;
    }

    public static void main(String[] args) {
        String folder = args.length > 0 ? args[0] : "data";
        Engine engine = new Engine(folder);
        try {
            System.out.println("loading data from " + folder + " ...");
            engine.load();
        } catch (Exception e) {
            System.out.println("could not load the data: " + e.getMessage());
            return;
        }
        System.out.println("TokenTally AI  |  " + engine.prices.size() + " model prices  |  "
                + engine.pages.size() + " pages  |  " + engine.index.words().size() + " words");
        new TokenTallyApp(engine).loop();
    }

    void loop() {
        while (true) {
            System.out.println();
            for (int i = 0; i < MENU.length; i++) {
                System.out.printf("%2d  %s%n", i + 1, MENU[i]);
            }
            if (!in.hasNextLine()) {
                return; // input closed
            }
            int c = InputValidator.choice(ask("choose"), MENU.length);
            if (c == -1) {
                System.out.println("type a number from 1 to " + MENU.length);
                continue;
            }
            if (c == MENU.length) {
                System.out.println("bye");
                return;
            }
            try {
                run(c);
            } catch (IllegalArgumentException e) {
                System.out.println("input problem: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("something went wrong: " + e.getMessage());
            }
        }
    }

    private void run(int c) throws Exception {
        switch (c) {
            case 1 -> search();
            case 2 -> complete();
            case 3 -> frequency();
            case 4 -> topSearches();
            case 5 -> listModels();
            case 6 -> compare();
            case 7 -> card();
            case 8 -> calculator();
            case 9 -> freeAndWarnings();
            case 10 -> patterns();
            case 11 -> dataCheck();
            case 12 -> recrawl();
            case 13 -> priceHistory();
            default -> { }
        }
    }

    private void search() {
        Engine.SearchResult r = engine.search(ask("search for"));
        System.out.println("searched " + r.timesSearched + " time(s) so far");
        for (String w : r.unknownWords) {
            ArrayList<SpellChecker.Suggestion> s = r.suggestions.get(w);
            if (s.isEmpty()) {
                System.out.println("\"" + w + "\" is not in the pages and nothing is close to it");
            } else {
                StringBuilder line = new StringBuilder("\"" + w + "\" not found. did you mean:");
                for (SpellChecker.Suggestion x : s) {
                    line.append(' ').append(x.word);
                }
                System.out.println(line);
            }
        }
        if (!r.searchedInstead.isEmpty()) {
            System.out.println("showing results for \"" + r.searchedInstead + "\"");
        }
        if (!r.models.isEmpty()) {
            System.out.println("models:");
            printPrices(r.models);
        }
        if (r.pages.isEmpty()) {
            System.out.println("no page has these words");
        } else {
            System.out.println("pages ranked by how often the words show up:");
            int rank = 1;
            for (PageRanker.Result p : r.pages) {
                System.out.printf("%2d. %-4d %s (%d keyword(s))%n", rank++, p.score, p.url, p.keywordsFound);
            }
        }
        if (!r.phraseHits.isEmpty()) {
            System.out.println("exact phrase found in:");
            r.phraseHits.forEach((url, n) -> System.out.println("   " + n + "  " + url));
        }
    }

    private void complete() {
        String prefix = ask("start of a word");
        ArrayList<String> words = engine.complete(prefix, 10);
        System.out.println(words.isEmpty() ? "no word starts with \"" + prefix + "\"" : String.join(", ", words));
    }

    private void frequency() {
        String word = ask("word");
        System.out.println("pages:");
        for (int i = 0; i < engine.pages.size(); i++) {
            System.out.printf("%2d  %s%n", i + 1, engine.pages.get(i).url);
        }
        String pick = ask("page number (enter for all pages)");
        String url = null;
        if (!pick.isBlank()) {
            int n = InputValidator.choice(pick, engine.pages.size());
            if (n == -1) {
                throw new IllegalArgumentException("no page " + pick);
            }
            url = engine.pages.get(n - 1).url;
        }
        ArrayList<FrequencyCounter.Result> list = engine.frequency(word, url);
        int total = 0;
        for (FrequencyCounter.Result r : list) {
            System.out.printf("%4d  %s  (boyer moore %d checks, kmp %d checks)%n",
                    r.boyerMoore, r.url, r.bmComparisons, r.kmpComparisons);
            total += r.boyerMoore;
        }
        System.out.println("\"" + word + "\" shows up " + total + " time(s)");
    }

    private void topSearches() {
        ArrayList<Object[]> top = engine.searches.top(10);
        if (top.isEmpty()) {
            System.out.println("nothing searched yet");
        }
        for (Object[] t : top) {
            System.out.printf("%4d  %s%n", (int) t[1], t[0]);
        }
    }

    private void listModels() {
        String provider = ask("provider (openai, gemini, anthropic, zai, xai or enter for all)").toLowerCase();
        String tier = ask("tier (standard, batch, flex ... or enter for all)").toLowerCase();
        ArrayList<ModelPrice> list = engine.models(provider, tier);
        list.sort((a, b) -> Double.compare(a.input, b.input));
        printPrices(list);
        System.out.println(list.size() + " row(s), cheapest input first");
    }

    private void compare() {
        String[] names = ask("model ids separated by commas").split(",");
        for (String n : names) {
            if (!InputValidator.modelName(n)) {
                throw new IllegalArgumentException("\"" + n.trim() + "\" is not a model id");
            }
        }
        ArrayList<ModelPrice> list = engine.compare(names);
        if (list.isEmpty()) {
            System.out.println("none of these models were found, try option 2 to complete a name");
            return;
        }
        printPrices(list);
    }

    private void card() {
        String m = ask("model id");
        ArrayList<ModelPrice> list = engine.card(m);
        if (list.isEmpty()) {
            ArrayList<String> close = engine.complete(m, 5);
            System.out.println("no model called " + m + (close.isEmpty() ? "" : ". maybe: " + String.join(", ", close)));
            return;
        }
        ModelPrice f = list.get(0);
        System.out.println(f.model + "  (" + f.provider + ")");
        System.out.println("context window: " + size(f.contextWindow) + "   max output: " + size(f.maxOutput));
        System.out.println("source: " + f.sourceUrl);
        printPrices(list);
    }

    private void calculator() {
        Workload w = new Workload();
        w.inputPerDay = askTokens("input tokens per day (like 2M or 500K)");
        w.outputPerDay = askTokens("output tokens per day");
        String cached = ask("share of input that hits the cache in percent (enter for 0)");
        if (!cached.isBlank()) {
            w.cachedShare = InputValidator.percent(cached);
            if (w.cachedShare < 0) {
                throw new IllegalArgumentException("percent must be between 0 and 100");
            }
        }
        w.batchOk = ask("is batch processing fine? (y/n)").toLowerCase().startsWith("y");
        String prompt = ask("tokens in one request (enter to skip)");
        if (!prompt.isBlank()) {
            w.promptSize = askTokensFrom(prompt);
        }
        String ctx = ask("smallest context window you need (enter to skip)");
        if (!ctx.isBlank()) {
            w.minContext = askTokensFrom(ctx);
        }
        String budget = ask("monthly budget in $ (enter for no limit)");
        if (!budget.isBlank()) {
            w.budget = InputValidator.money(budget);
            if (w.budget < 0) {
                throw new IllegalArgumentException("budget should look like 50 or $49.99");
            }
        }
        w.provider = ask("only one provider? (enter for all)").toLowerCase();

        ArrayList<Recommender.Pick> picks = engine.recommend(w, 10);
        if (picks.isEmpty()) {
            System.out.println("no model fits, try a bigger budget or a smaller context");
            return;
        }
        System.out.println("cheapest models for this workload, per month:");
        int rank = 1;
        for (Recommender.Pick p : picks) {
            CostCalculator.Breakdown b = p.cost;
            System.out.printf("%2d. %-34s %-10s $%10.2f  (input $%.2f, cached $%.2f, output $%.2f) %s%s%n",
                    rank++, p.price.model, p.price.provider, b.total, b.input, b.cached, b.output, p.why,
                    b.longPrices ? ", long context prices" : "");
        }
    }

    private void freeAndWarnings() {
        System.out.println("free models:");
        printPrices(engine.freeModels());
        System.out.println("warnings:");
        for (ModelPrice p : engine.warnings()) {
            System.out.println("  " + p.provider + " " + p.model + " " + p.tier + ": " + p.notes);
        }
    }

    private void patterns() {
        String pick = ask("page number from option 3's list (enter for all pages)");
        String url = null;
        if (!pick.isBlank()) {
            int n = InputValidator.choice(pick, engine.pages.size());
            if (n == -1) {
                throw new IllegalArgumentException("no page " + pick);
            }
            url = engine.pages.get(n - 1).url;
        }
        engine.patterns(url).forEach((kind, found) -> {
            System.out.println(kind + " (" + found.size() + " different):");
            int shown = 0;
            for (var e : found.entrySet()) {
                if (shown++ == 12) {
                    System.out.println("   ...");
                    break;
                }
                System.out.println("   " + e.getValue() + "x  " + e.getKey());
            }
        });
    }

    private void dataCheck() {
        var probs = engine.dataProblems();
        if (probs.isEmpty()) {
            System.out.println("all " + engine.prices.size() + " prices passed the checks");
        }
        probs.forEach((k, v) -> System.out.println(k + ": " + String.join(", ", v)));
    }

    private void recrawl() throws Exception {
        System.out.println("crawling the five providers, this takes about half a minute ...");
        for (String line : engine.recrawl()) {
            System.out.println(line);
        }
    }

    private void priceHistory() throws Exception {
        ArrayList<PriceHistory.Change> changes = engine.history.changes();
        int lines = engine.history.load().size();
        System.out.println(lines + " price lines in the log, " + changes.size() + " price changes");
        if (changes.isEmpty()) {
            System.out.println("no price has changed yet, crawl again later to see changes");
        }
        int shown = 0;
        for (PriceHistory.Change c : changes) {
            if (shown++ == 15) {
                break;
            }
            System.out.printf("   %s  %-10s %-30s %-9s input %s -> %s (%+.1f%%)  output %s -> %s (%+.1f%%)%n",
                c.after.date, c.after.provider, c.after.model, c.after.tier,
                ModelPrice.money(c.before.input), ModelPrice.money(c.after.input), c.inputPercent,
                ModelPrice.money(c.before.output), ModelPrice.money(c.after.output), c.outputPercent);
        }
    }

    // ---------------- helpers ----------------

    private String ask(String label) {
        System.out.print(label + ": ");
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    private long askTokens(String label) {
        return askTokensFrom(ask(label));
    }

    private long askTokensFrom(String text) {
        long n = InputValidator.tokens(text);
        if (n < 0) {
            throw new IllegalArgumentException("\"" + text + "\" is not a token amount, use 2M, 500K or 1,000,000");
        }
        return n;
    }

    private static String size(long n) {
        if (n < 0) {
            return "not listed";
        }
        if (n >= 1_000_000) {
            return (Math.round(n / 10_000.0) / 100.0) + "M";
        }
        return (n / 1000) + "K";
    }

    static void printPrices(ArrayList<ModelPrice> list) {
        System.out.printf("   %-34s %-10s %-11s %9s %9s %9s %9s%n", "model", "provider", "tier", "input", "cached", "output", "context");
        for (ModelPrice p : list) {
            System.out.printf("   %-34s %-10s %-11s %9s %9s %9s %9s%n", p.model, p.provider, p.tier,
                    ModelPrice.money(p.input), ModelPrice.money(p.cachedInput), ModelPrice.money(p.output),
                    p.contextWindow > 0 ? size(p.contextWindow) : "-");
        }
    }
}
