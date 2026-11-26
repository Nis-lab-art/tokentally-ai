package tokentally.app;

import java.util.ArrayList;
import java.util.Scanner;

import tokentally.cost.CostCalculator;
import tokentally.cost.Recommender;
import tokentally.cost.Workload;
import tokentally.model.ModelPrice;
import tokentally.parse.HtmlCleaner;
import tokentally.rank.PageRanker;
import tokentally.regex.InputValidator;
import tokentally.text.FrequencyCounter;
import tokentally.text.SpellChecker;

// owner: Aakriti Mainali
// command line menu
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
        "spell check a word",
        "inverted index lookup of a word",
        "check an input with the regex validator",
        "clean a piece of html",
        "error handling demo (bad inputs on every feature)",
        "experiments (measured times on the real data)",
        "what if I ... (pick a situation, see how it is handled)",
        "exit"
    };

    // option owners
    private static final String[] OWNER = {
        "all", "Aakriti", "Abhinav", "Khem Raj", "all", "all", "all", "Nishan", "all", "Aakriti",
        "Shrijana", "all", "all", "Nishan", "Abhinav", "Shrijana", "Khem Raj", "all", "all", "all", ""
    };

    private final Engine engine;
    private String member = ""; // show one member only
    private final Scanner in = new Scanner(System.in);

    public TokenTallyApp(Engine engine) {
        this.engine = engine;
    }

    public TokenTallyApp(Engine engine, String member) {
        this.engine = engine;
        this.member = member == null ? "" : member.trim().toLowerCase();
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
        // optional member name
        new TokenTallyApp(engine, args.length > 1 ? args[1] : "").loop();
    }

    void loop() {
        while (true) {
            System.out.println();
            for (int i = 0; i < MENU.length; i++) {
                String owner = OWNER[i];
                if (!member.isEmpty() && !owner.isEmpty() && !owner.equals("all") && !owner.toLowerCase().contains(member)) {
                    continue; // hide others
                }
                System.out.printf("%2d  %-52s %s%n", i + 1, MENU[i], owner.isEmpty() ? "" : "(" + owner + ")");
            }
            if (!in.hasNextLine()) {
                return; // no more input
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
            case 14 -> spellCheck();
            case 15 -> indexLookup();
            case 16 -> validate();
            case 17 -> cleanHtml();
            case 18 -> errorDemo();
            case 19 -> experiments();
            case 20 -> whatIf();
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
        for (PriceHistory.Entry e : engine.history.added()) {
            System.out.printf("   %s  new  %-10s %-30s %-9s input %s  output %s%n", e.date, e.provider, e.model, e.tier,
                ModelPrice.money(e.input), ModelPrice.money(e.output));
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

    private void spellCheck() {
        String word = ask("word to check");
        if (!InputValidator.searchWord(word)) {
            throw new IllegalArgumentException("use letters, digits, dots and dashes only (max 60 chars)");
        }
        if (engine.spell.isKnown(word)) {
            System.out.println("\"" + word + "\" is spelled right, it is in the pages " + engine.index.total(word.toLowerCase()) + " time(s)");
            return;
        }
        ArrayList<SpellChecker.Suggestion> s = engine.spell.suggest(word, 5);
        if (s.isEmpty()) {
            System.out.println("\"" + word + "\" is not in the pages and nothing is within 2 edits of it");
        }
        for (SpellChecker.Suggestion x : s) {
            System.out.printf("   %-30s %d edit(s)  seen %d time(s)%n", x.word, x.distance, x.frequency);
        }
    }

    private void indexLookup() {
        String word = ask("word to look up").toLowerCase();
        if (!InputValidator.searchWord(word)) {
            throw new IllegalArgumentException("use letters, digits, dots and dashes only (max 60 chars)");
        }
        var postings = engine.index.postings(word);
        if (postings.isEmpty()) {
            System.out.println("\"" + word + "\" is on no page");
            return;
        }
        System.out.println("\"" + word + "\" is on " + postings.size() + " page(s), " + engine.index.total(word) + " time(s) in all");
        postings.entrySet().stream()
            .sorted((a, b) -> b.getValue().size() - a.getValue().size())
            .limit(8)
            .forEach(e -> System.out.printf("   %4d  %s  first positions %s%n", e.getValue().size(), e.getKey(),
                e.getValue().subList(0, Math.min(5, e.getValue().size()))));
    }

    private void validate() {
        System.out.println("what do you want to check?");
        String[] kinds = {"token amount (2M, 500K)", "money ($49.99)", "percent (20%)", "date (2026-11-24)", "model name", "search text"};
        for (int i = 0; i < kinds.length; i++) {
            System.out.println("   " + (i + 1) + "  " + kinds[i]);
        }
        int kind = InputValidator.choice(ask("kind"), kinds.length);
        if (kind == -1) {
            throw new IllegalArgumentException("type a number from 1 to " + kinds.length);
        }
        String v = ask("value");
        boolean ok;
        String reading = "";
        switch (kind) {
            case 1 -> {
                long n = InputValidator.tokens(v);
                ok = n >= 0;
                reading = ok ? n + " tokens" : "";
            }
            case 2 -> {
                double d = InputValidator.money(v);
                ok = d >= 0;
                reading = ok ? "$" + d : "";
            }
            case 3 -> {
                double d = InputValidator.percent(v);
                ok = d >= 0;
                reading = ok ? (d * 100) + " percent" : "";
            }
            case 4 -> ok = InputValidator.date(v);
            case 5 -> ok = InputValidator.modelName(v);
            default -> ok = InputValidator.searchWord(v);
        }
        System.out.println(ok ? "valid " + reading : "not valid, " + kinds[kind - 1] + " was expected");
    }

    private void cleanHtml() {
        String html = ask("html (one line)");
        if (html.isBlank()) {
            throw new IllegalArgumentException("type or paste some html");
        }
        System.out.println("title: " + HtmlCleaner.title(html));
        System.out.println("text : " + HtmlCleaner.text(html).replace('\n', ' '));
    }

    private void errorDemo() {
        System.out.println("running bad, odd and extreme inputs through every feature ...");
        var cases = ErrorDemo.run(engine);
        String last = "";
        for (ErrorDemo.Case c : cases) {
            if (!member.isEmpty() && !c.member.toLowerCase().contains(member) && !c.member.equals("shared parts")) {
                continue;
            }
            if (!c.member.equals(last)) {
                System.out.println();
                System.out.println("== " + c.member);
                last = c.member;
            }
            System.out.printf("%s %s / %s  [%s]%n      -> %s%n", c.handled ? "ok " : "BAD", c.feature, c.scenario, c.input, c.result);
        }
        int bad = ErrorDemo.crashed(cases).size();
        System.out.println();
        System.out.println(cases.size() + " cases, " + (cases.size() - bad) + " handled, " + bad + " crashed");
    }

    // pick a situation
    private void whatIf() {
        var all = ErrorDemo.run(engine);
        ArrayList<ErrorDemo.Case> list = new ArrayList<>();
        for (ErrorDemo.Case c : all) {
            if (member.isEmpty() || c.member.toLowerCase().contains(member) || c.member.equals("shared parts")) {
                list.add(c);
            }
        }
        String last = "";
        for (int i = 0; i < list.size(); i++) {
            ErrorDemo.Case c = list.get(i);
            if (!c.member.equals(last)) {
                System.out.println();
                System.out.println("== " + c.member);
                last = c.member;
            }
            System.out.printf("%3d  %-18s %-34s [%s]%n", i + 1, c.feature, c.scenario, c.input);
        }
        while (true) {
            System.out.println();
            String pick = ask("what if I ... (number, or enter to go back)");
            if (pick.isEmpty()) {
                return;
            }
            int n = InputValidator.choice(pick, list.size());
            if (n == -1) {
                System.out.println("type a number from 1 to " + list.size());
                continue;
            }
            ErrorDemo.Case c = list.get(n - 1);
            System.out.println("   what if   " + c.feature + ": " + c.scenario);
            System.out.println("   you type  " + c.input);
            System.out.println("   result    " + (c.handled ? (c.kind.startsWith("rejected") ? "REFUSED with a message, nothing crashed" : "ANSWERED safely, nothing crashed") : "CRASHED"));
            System.out.println("   the program says: " + c.result);
        }
    }

    private void experiments() {
        System.out.println("measuring on the real data, this takes a few seconds ...");
        String last = "";
        for (Experiments.Row r : Experiments.run(engine)) {
            if (!member.isEmpty() && !r.member.toLowerCase().contains(member)) {
                continue;
            }
            if (!r.member.equals(last)) {
                System.out.println();
                System.out.println("== " + r.member);
                last = r.member;
            }
            System.out.println(r.feature + ": " + r.title);
            System.out.printf("      %-28s %12s%n      %-28s %12s   (%s)%n      %s%n",
                r.aLabel, trimNumber(r.aValue), r.bLabel, trimNumber(r.bValue), r.unit, r.takeaway);
        }
    }

    private static String trimNumber(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
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
