package tokentally;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import tokentally.app.CrawlAll;
import tokentally.app.DataStore;
import tokentally.app.Engine;
import tokentally.app.ErrorDemo;
import tokentally.app.Experiments;
import tokentally.app.PriceHistory;
import tokentally.cost.CostCalculator;
import tokentally.cost.Recommender;
import tokentally.cost.Workload;
import tokentally.crawl.RobotsTxtChecker;
import tokentally.crawl.WebCrawler;
import tokentally.model.ModelPrice;
import tokentally.model.Page;
import tokentally.parse.AnthropicParser;
import tokentally.parse.GeminiParser;
import tokentally.parse.HtmlCleaner;
import tokentally.parse.HtmlTable;
import tokentally.parse.OpenAIParser;
import tokentally.parse.Prices;
import tokentally.parse.XaiParser;
import tokentally.parse.ZaiParser;
import tokentally.rank.PageRanker;
import tokentally.regex.InputValidator;
import tokentally.regex.PatternExtractor;
import tokentally.search.SearchFrequency;
import tokentally.text.BoyerMoore;
import tokentally.text.EditDistance;
import tokentally.text.InvertedIndex;
import tokentally.text.KMPMatcher;
import tokentally.text.SpellChecker;
import tokentally.text.Tokenizer;
import tokentally.text.Trie;
import tokentally.text.WordCompleter;
import tokentally.util.Heap;

// a very small test runner, no test library needed
// every test is a method below and check() counts passes and failures
public class TestRunner {

    static int passed = 0;
    static int failed = 0;
    static String current = "";

    public static void main(String[] args) throws Exception {
        String data = args.length > 0 ? args[0] : "data";

        run("heap keeps order", TestRunner::heap);
        run("robots.txt rules", TestRunner::robots);
        run("url normalize and links", TestRunner::urls);
        run("html cleaner", TestRunner::cleaner);
        run("price cell helpers", TestRunner::priceHelpers);
        run("openai parser", TestRunner::openai);
        run("claude parser", TestRunner::claude);
        run("gemini parser", TestRunner::gemini);
        run("z.ai parser", TestRunner::zai);
        run("xai parser", TestRunner::xai);
        run("tokenizer", TestRunner::tokenizer);
        run("inverted index and phrase search", TestRunner::index);
        run("boyer moore and kmp match a slow count", TestRunner::stringSearch);
        run("edit distance", TestRunner::editDistance);
        run("spell checker", TestRunner::spell);
        run("trie and word completion", TestRunner::completion);
        run("search frequency saved to a file", TestRunner::searchFrequency);
        run("hardening: bad input and damaged files", TestRunner::hardening);
        run("price history log", TestRunner::priceHistory);
        run("error handling demo (no case may crash)", () -> errorDemo(data));
        run("experiments give sane numbers", () -> experiments(data));
        run("page ranking", TestRunner::ranking);
        run("input validation", TestRunner::validation);
        run("pattern finding", TestRunner::patterns);
        run("cost calculation", TestRunner::cost);
        run("recommendation filters and order", TestRunner::recommend);
        run("saved data: parsers and accuracy", () -> savedData(data));
        run("saved data: engine and speed", () -> engine(data));

        System.out.println();
        System.out.println(passed + " checks passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    interface Test {
        void go() throws Exception;
    }

    static void run(String name, Test t) {
        current = name;
        int before = failed;
        try {
            t.go();
        } catch (Exception e) {
            failed++;
            System.out.println("FAIL " + name + " threw " + e);
        }
        System.out.println((failed == before ? "ok   " : "FAIL ") + name);
    }

    static void check(boolean ok, String what) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("     failed: " + what);
        }
    }

    static void near(double got, double want, String what) {
        check(Math.abs(got - want) < 1e-6, what + " (got " + got + ", want " + want + ")");
    }

    static void throwsError(Test t, String what) {
        try {
            t.go();
            check(false, what + " should throw");
        } catch (Exception e) {
            check(true, what);
        }
    }

    // ---------------- tests ----------------

    static void heap() {
        Heap<Integer> min = new Heap<>(Integer::compare);
        Heap<Integer> max = new Heap<>((a, b) -> b - a);
        int[] values = {5, 1, 9, 3, 7, 3, 0, 8};
        for (int v : values) {
            min.push(v);
            max.push(v);
        }
        int last = -1;
        boolean sorted = true;
        while (!min.isEmpty()) {
            int v = min.pop();
            sorted &= v >= last;
            last = v;
        }
        check(sorted, "min heap pops smallest first");
        check(max.pop() == 9 && max.pop() == 8, "max heap pops biggest first");
        check(new Heap<Integer>(Integer::compare).pop() == null, "empty heap pops null");
    }

    static void robots() {
        String txt = "User-agent: Twitterbot\nDisallow: /\n\nUser-agent: *\nDisallow: /api/\nAllow: /api/public\n# comment\n";
        ArrayList<String[]> rules = RobotsTxtChecker.parse(txt);
        check(rules.size() == 2, "only the * group is kept");
        check(!RobotsTxtChecker.allowedByRules(rules, "/api/keys"), "disallowed path");
        check(RobotsTxtChecker.allowedByRules(rules, "/api/public/x"), "longer allow wins");
        check(RobotsTxtChecker.allowedByRules(rules, "/docs/pricing"), "other paths allowed");
        check(RobotsTxtChecker.allowedByRules(RobotsTxtChecker.parse(null), "/"), "no robots.txt means allowed");
    }

    static void urls() {
        check(WebCrawler.normalize("https://Docs.X.ai/developers/pricing/#text?a=1").equals("https://docs.x.ai/developers/pricing"),
                "drops fragment, query, slash and lowercases the host");
        ArrayList<String> links = WebCrawler.links("<a href=\"/models/a\">a</a> <a href=\"logo.png\">x</a> see [b](/models/b)",
                "https://site.com/docs/pricing");
        check(links.contains("https://site.com/models/a"), "relative href resolved");
        check(links.contains("https://site.com/models/b"), "markdown link found");
        check(!links.contains("https://site.com/docs/logo.png"), "images skipped");
    }

    static void cleaner() {
        String html = "<html><head><title>Pricing &amp; Models</title></head><nav>menu</nav><script>var x=1</script>"
                + "<h2>Text API</h2><p>Price is &lt;= $2 &#8212; nice</p><table><tr><th>Model</th><th>Input</th></tr>"
                + "<tr><td><b>m-1</b></td><td>$1.00</td></tr></table><footer>foot</footer></html>";
        String text = HtmlCleaner.text(html);
        check(!text.contains("menu") && !text.contains("var x") && !text.contains("foot"), "nav, script and footer removed");
        check(text.contains("Price is <= $2 — nice"), "entities decoded");
        check(HtmlCleaner.title(html).equals("Pricing & Models"), "title read");
        ArrayList<HtmlTable> tables = HtmlCleaner.tables(html);
        check(tables.size() == 1 && tables.get(0).heading.equals("Text API"), "table with its heading");
        check(tables.get(0).rows.get(1).get(0).equals("m-1"), "cell text without tags");
    }

    static void priceHelpers() {
        near(Prices.first("$0.10 / MTok for prompts up to 100,000 tokens"), 0.10, "first dollar amount");
        near(Prices.first("Free"), 0, "free is zero");
        near(Prices.first("-"), -1, "dash is missing");
        check(Prices.tokens("500k") == 500000 && Prices.tokens("1M") == 1000000 && Prices.tokens("1,050,000") == 1050000, "token sizes");
        check(Prices.firstWord("grok-4.7 Long context").equals("grok-4.7"), "first word");
    }

    static void openai() {
        String html = "<div data-content-switcher-pane=\"true\" data-value=\"standard\"><table>"
                + "<tr><th>Model</th><th>Input</th><th>Output</th><th>Cached input</th><th>30m write</th></tr>"
                + "<tr><td>gpt-x</td><td>$2.00</td><td>$8.00</td><td>$0.20</td><td>$2.50</td></tr></table></div>"
                + "<div data-content-switcher-pane=\"true\" data-value=\"batch\" hidden><table>"
                + "<tr><th>Model</th><th>Input</th><th>Output</th><th>Cached input</th></tr>"
                + "<tr><td>gpt-x</td><td>$1.00</td><td>$4.00</td><td>$0.10</td></tr></table></div>"
                + "<table><tr><th>Model</th><th>Modality</th><th>Input</th><th>Output</th></tr><tr><td>img</td><td>Image</td><td>$5</td><td>$9</td></tr></table>";
        ArrayList<ModelPrice> list = new OpenAIParser().parse(html, "u");
        check(list.size() == 2, "two tiers and the image table skipped");
        check(list.get(0).tier.equals("standard") && list.get(1).tier.equals("batch"), "tier from the tab");
        near(list.get(0).cachedInput, 0.20, "cached input");
    }

    static void claude() {
        String html = "<h2>Model pricing</h2><table><tr><th>Model</th><th>Base tokens</th><th>Prompt caching</th></tr>"
                + "<tr><th>Name</th><th>Input</th><th>Output</th><th>5m writes</th><th>1h writes</th><th>Hits and refreshes</th></tr>"
                + "<tr><td>Claude Opus 5.5 For coding</td><td>$4 / MTok</td><td>$20 / MTok</td><td>$5 / MTok</td><td>$8 / MTok</td><td>$0.20 / MTok</td></tr>"
                + "<tr><td>Claude Haiku 5.5</td><td>$0.10 / MTok for prompts up to 100,000 tokens</td><td>$0.50 / MTok</td><td>$0.125 / MTok</td><td>$0.20 / MTok</td><td>$0.01 / MTok</td></tr>"
                + "<tr><td>$0.50 / MTok for prompts over 100,000 tokens</td><td>$2.50 / MTok</td><td>$0.625 / MTok</td><td>$1 / MTok</td><td>$0.05 / MTok</td></tr>"
                + "<tr><td>Claude Opus 4 </td><td>$15 / MTok</td><td>$75 / MTok</td><td>$1</td><td>$1</td><td>$1</td></tr></table>"
                + "<h2>Batch processing</h2><table><tr><td>Claude Opus 5.5</td><td>$2 / MTok</td><td>$10 / MTok</td></tr></table>";
        ArrayList<ModelPrice> list = new AnthropicParser().parse(html, "u");
        check(list.size() == 3, "retired model skipped (got " + list.size() + ")");
        ModelPrice opus = list.get(0);
        check(opus.model.equals("claude-opus-5-5"), "model id from the name");
        near(opus.cachedInput, 0.20, "cache hit price");
        ModelPrice haiku = list.get(1);
        near(haiku.longInput, 0.50, "long input from the second row");
        check(haiku.longThreshold == 100000, "long threshold");
        near(list.get(2).cachedInput, 0.10, "batch cache price is half");
    }

    static void gemini() {
        String text = "## Gemini Test  {% id=\"gemini-test\" %}\n  *[`gemini-test-1`](/docs/models/x)*\n"
                + "  {% tab title=\"Standard\" %}\n  {% table %}\n  * Feature\n  * Free Tier\n  * Paid Tier\n  ---\n"
                + "  * Input price\n  * Free of charge\n  * $1.25, prompts &lt;= 200k tokens\n  $2.50, prompts &gt; 200k\n  ---\n"
                + "  * Output price (including thinking tokens)\n  * Free of charge\n  * $10.00\n  ---\n"
                + "  * Context caching price\n  * Not available\n  * Not available\n  {% /table %}\n  {% /tab %}\n"
                + "  {% tab title=\"Batch\" %}\n  {% table %}\n  * Feature\n  * Free\n  * Paid\n  ---\n  * Input price\n  * Not available\n  * $0.60\n  ---\n"
                + "  * Output price\n  * Not available\n  * $5.00\n  {% /table %}\n";
        ArrayList<ModelPrice> list = new GeminiParser().parse(text, "u");
        check(list.size() == 2, "one row per tab");
        ModelPrice s = list.get(0);
        check(s.model.equals("gemini-test-1") && s.tier.equals("standard"), "id from the link and tier from the tab");
        near(s.input, 1.25, "short input");
        near(s.longInput, 2.50, "long input");
        near(s.cachedInput, -1, "no caching price");
        near(list.get(1).output, 5.0, "batch output");
    }

    static void zai() {
        String html = "<h2>Text Models</h2><table><tr><th>Model</th><th>Input</th><th>Cached Input</th><th>Cached Input Storage</th><th>Output</th></tr>"
                + "<tr><td>GLM-5.3</td><td>$1.4</td><td>$0.26</td><td>Limited-time Free</td><td>$4.4</td></tr>"
                + "<tr><td>GLM-4.7-Flash</td><td>Free</td><td>Free</td><td>Free</td><td>Free</td></tr>"
                + "<tr><td>GLM-OCR</td><td>$0.03</td><td>\\</td><td>\\</td><td>$0.03</td></tr></table>";
        ArrayList<ModelPrice> list = new ZaiParser().parse(html, "u");
        check(list.size() == 3 && list.get(0).model.equals("glm-5.3"), "three models, lowercase ids");
        check(list.get(1).isFree(), "free model");
        near(list.get(2).cachedInput, -1, "missing cached price");
        check(list.get(0).notes.contains("limited-time"), "limited time note");
    }

    static void xai() {
        String html = "<h2>Text API</h2><table><tr><th>Model</th><th>Context</th><th>Short context</th><th>Long context</th></tr>"
                + "<tr><th>Input</th><th>Cached</th><th>Output</th><th>Input</th><th>Cached</th><th>Output</th></tr>"
                + "<tr><td>grok-9 Long context ≥ 200k tokens</td><td>500k</td><td>$2.00</td><td>$0.50</td><td>$6.00</td><td>$4.00</td><td>$1.00</td><td>$12.00</td></tr></table>"
                + "<h2>Batch API Pricing</h2><table><tr><th>Model</th><th>Batch discount</th></tr><tr><td>grok-9</td><td>20 %</td></tr></table>";
        ArrayList<ModelPrice> list = new XaiParser().parse(html, "u");
        check(list.size() == 2, "standard and batch");
        check(list.get(0).contextWindow == 500000 && list.get(0).longThreshold == 200000, "context and threshold");
        near(list.get(1).input, 1.6, "batch is 20 percent off");
    }

    static void tokenizer() {
        ArrayList<String> w = Tokenizer.words("Use GPT-6.1-Sol, now! Price: $2.50.");
        check(w.contains("gpt-6.1-sol") && w.contains("price") && w.contains("2.50"), "model name kept as one word " + w);
        check(!Tokenizer.hasLetter("2.50") && Tokenizer.hasLetter("glm-5"), "has letter");
    }

    static void index() {
        InvertedIndex idx = new InvertedIndex();
        idx.add(new Page("p", "u1", "", "batch api pricing and batch api limits"));
        idx.add(new Page("p", "u2", "", "the api has no batch"));
        check(idx.count("batch", "u1") == 2 && idx.count("batch", "u2") == 1, "count per page");
        check(idx.phrase("batch api").getOrDefault("u1", 0) == 2, "phrase found twice in u1");
        check(!idx.phrase("batch api").containsKey("u2"), "words in the wrong order do not match");
        check(idx.postings("missing").isEmpty(), "unknown word gives nothing");
    }

    static void stringSearch() {
        Random r = new Random(7);
        for (int k = 0; k < 3000; k++) {
            String word = randomText(r, 1 + r.nextInt(4), "ab-");
            String text = randomText(r, r.nextInt(80), "ab -.");
            int slow = 0;
            for (int s = 0; s + word.length() <= text.length(); s++) {
                if (text.startsWith(word, s) && (s == 0 || !Tokenizer.isLetterOrDigit(text.charAt(s - 1)))
                        && (s + word.length() == text.length() || !Tokenizer.isLetterOrDigit(text.charAt(s + word.length())))) {
                    slow++;
                }
            }
            int bm = new BoyerMoore(word).count(text);
            int kmp = new KMPMatcher(word).count(text);
            if (bm != slow || kmp != slow) {
                check(false, "word " + word + " text " + text + " slow " + slow + " bm " + bm + " kmp " + kmp);
                return;
            }
        }
        check(true, "3000 random cases agree");
        String longText = "the price of the model is listed on the pricing page ".repeat(2000);
        BoyerMoore bm = new BoyerMoore("pricing");
        KMPMatcher kmp = new KMPMatcher("pricing");
        check(bm.count(longText) == 2000 && kmp.count(longText) == 2000, "2000 hits in long text");
        check(bm.comparisons < kmp.comparisons, "boyer moore checks fewer chars than kmp on normal text");
    }

    static String randomText(Random r, int n, String chars) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) {
            b.append(chars.charAt(r.nextInt(chars.length())));
        }
        return b.toString();
    }

    static void editDistance() {
        check(EditDistance.between("kitten", "sitting") == 3, "kitten sitting");
        check(EditDistance.between("gemnii", "gemini") == 2, "swapped letters");
        check(EditDistance.between("", "abc") == 3 && EditDistance.between("same", "same") == 0, "empty and equal");
    }

    static void spell() {
        HashMap<String, Integer> vocab = new HashMap<>();
        vocab.put("gemini", 50);
        vocab.put("genie", 3);
        vocab.put("pricing", 40);
        SpellChecker s = new SpellChecker(vocab, 2);
        check(s.isKnown("Gemini"), "known word, any case");
        ArrayList<SpellChecker.Suggestion> sug = s.suggest("gemnii", 3);
        check(!sug.isEmpty() && sug.get(0).word.equals("gemini"), "gemnii -> gemini");
        check(s.suggest("zzzzzzzz", 3).isEmpty(), "nothing close");
    }

    static void completion() {
        Trie t = new Trie();
        t.insert("glm-5", 3);
        t.insert("glm-5.1", 10);
        t.insert("glm-5.3", 7);
        t.insert("grok", 1);
        t.insert("glm-5", 2);
        check(t.count("glm-5") == 5 && t.size() == 4, "counts add up");
        ArrayList<String> c = new WordCompleter(t).complete("GLM-5", 3);
        check(c.size() == 3 && c.get(0).equals("glm-5.1") && c.get(1).equals("glm-5.3"), "most frequent first " + c);
        check(new WordCompleter(t).complete("xyz", 3).isEmpty() && new WordCompleter(t).complete("  ", 3).isEmpty(), "no match and blank prefix");
    }

    static void hardening() throws Exception {
        check(InputValidator.tokens("999999999999B") == -1, "a token amount of 999 trillion is rejected");
        check(InputValidator.tokens("1000B") == 1_000_000_000_000L, "1000B is still fine");
        boolean threw = false;
        try {
            new BoyerMoore("");
        } catch (IllegalArgumentException ex) {
            threw = true;
        }
        check(threw, "boyer moore refuses an empty word");
        threw = false;
        try {
            new KMPMatcher(null);
        } catch (IllegalArgumentException ex) {
            threw = true;
        }
        check(threw, "kmp refuses a missing word");
        Trie t = new Trie();
        t.insert(null, 1);
        t.insert("", 3);
        check(t.size() == 0 && t.wordsWithPrefix(null).isEmpty(), "trie ignores null and empty words");
        check(new PageRanker(new InvertedIndex()).rank(null, 5).isEmpty(), "ranking a null query gives nothing");

        // a damaged prices.csv still loads the good rows
        Path dir = Files.createTempDirectory("badcsv");
        Files.writeString(dir.resolve("prices.csv"), String.join(",", DataStore.CSV_HEADER) + "\n"
            + "openai,gpt-x,standard,1,0.1,2,,,,,,,n,u\nopenai,gpt-y,standard,abc,0.1,2,,,,,,,n,u\nshort,row\n\n");
        DataStore ds = new DataStore(dir.toString());
        check(ds.loadPrices().size() == 1 && ds.badRows == 2, "damaged prices rows are skipped and counted");
    }

    static void priceHistory() throws Exception {
        Path dir = Files.createTempDirectory("history");
        PriceHistory h = new PriceHistory(dir);
        ArrayList<ModelPrice> prices = new ArrayList<>();
        ModelPrice a = new ModelPrice("openai", "gpt-x", "standard");
        a.input = 2;
        a.cachedInput = 0.2;
        a.output = 10;
        ModelPrice b = new ModelPrice("xai", "grok-x", "standard");
        b.input = 1;
        b.output = 4;
        prices.add(a);
        prices.add(b);
        check(h.record("2026-11-01", prices) == 2, "first crawl writes every price");
        check(h.record("2026-11-02", prices) == 0, "same prices write nothing");
        check(h.changes().isEmpty(), "no change yet");

        a.input = 1;
        ModelPrice c = new ModelPrice("zai", "glm-x", "standard");
        c.input = 0.5;
        c.output = 1;
        prices.add(c);
        check(h.record("2026-11-10", prices) == 2, "a changed price and a new model are written");
        ArrayList<PriceHistory.Change> ch = h.changes();
        check(ch.size() == 1, "only the changed price counts as a change");
        check(ch.get(0).after.model.equals("gpt-x") && ch.get(0).inputPercent == -50.0, "input price dropped 50 percent");
        check(ch.get(0).outputPercent == 0, "output did not move");
        check(h.added().size() == 1 && h.added().get(0).model.equals("glm-x"), "only the model that came later is listed as new");
        check(h.of("gpt-x").size() == 2 && h.of("glm-x").size() == 1, "history of one model");

        PriceHistory again = new PriceHistory(dir);
        check(again.load().size() == 4, "log loads again from the file");
        Files.writeString(dir.resolve("price_history.csv"), "date,provider,model,tier,input,cached_input,output\nbroken\n", java.nio.charset.StandardCharsets.UTF_8);
        check(again.load().isEmpty(), "broken lines are skipped");
    }

    static void errorDemo(String folder) throws Exception {
        Path copy = Files.createTempDirectory("demo");
        Files.createDirectories(copy);
        Engine e = new Engine(folder);
        e.load();
        ArrayList<ErrorDemo.Case> all = ErrorDemo.run(e);
        check(all.size() >= 40, "at least 40 error and edge cases (" + all.size() + ")");
        ArrayList<ErrorDemo.Case> bad = ErrorDemo.crashed(all);
        for (ErrorDemo.Case c : bad) {
            check(false, c.feature + " / " + c.scenario + " crashed with " + c.result);
        }
        check(bad.isEmpty(), "every case was handled without a crash");
    }

    static void experiments(String folder) throws Exception {
        Engine e = new Engine(folder);
        e.load();
        ArrayList<Experiments.Row> rows = Experiments.run(e);
        check(rows.size() >= 10, "at least 10 experiments (" + rows.size() + ")");
        boolean positive = true;
        for (Experiments.Row r : rows) {
            positive = positive && r.aValue >= 0 && r.bValue >= 0 && r.takeaway != null && !r.takeaway.isEmpty();
        }
        check(positive, "every experiment has two measured values and a sentence");
        // the index must beat reading all pages, the trie must beat the scan
        Experiments.Row index = null;
        Experiments.Row trie = null;
        for (Experiments.Row r : rows) {
            if (r.feature.equals("inverted index")) {
                index = r;
            }
            if (r.feature.equals("word completion")) {
                trie = r;
            }
        }
        check(index != null && index.aValue * 10 < index.bValue, "inverted index is more than 10 times faster than reading every page");
        check(trie != null && trie.aValue * 5 < trie.bValue, "trie is more than 5 times faster than scanning every word");
    }

    static void searchFrequency() throws Exception {
        Path f = Files.createTempFile("searches", ".txt");
        Files.writeString(f, "2\tgemini\nbroken line\n");
        SearchFrequency s = new SearchFrequency(f);
        s.record("Gemini");
        s.record("batch");
        s.record("batch");
        s.record("batch");
        SearchFrequency again = new SearchFrequency(f);
        check(again.count("gemini") == 3, "loaded from the file and broken lines skipped");
        ArrayList<Object[]> top = again.top(5);
        check(top.get(0)[0].equals("batch") && (int) top.get(0)[1] == 3 && top.size() == 2, "top list");
        check(s.record("   ") == 0, "blank search not saved");
        Files.deleteIfExists(f);
    }

    static void ranking() {
        InvertedIndex idx = new InvertedIndex();
        idx.add(new Page("p", "a", "", "batch batch batch"));
        idx.add(new Page("p", "b", "", "batch price batch"));
        idx.add(new Page("p", "c", "", "price"));
        ArrayList<PageRanker.Result> r = new PageRanker(idx).rank("batch price", 10);
        check(r.size() == 3, "three pages");
        check(r.get(0).url.equals("a") || r.get(0).url.equals("b"), "top score first");
        check(r.get(0).url.equals("b") && r.get(0).keywordsFound == 2, "tie goes to more keywords");
        check(r.get(2).url.equals("c"), "lowest last");
        check(new PageRanker(idx).rank("batch batch", 10).get(0).score == 3, "repeated keyword counted once");
    }

    static void validation() {
        check(InputValidator.tokens("2M") == 2_000_000 && InputValidator.tokens("500K") == 500_000, "2M and 500K");
        check(InputValidator.tokens("1,000,000") == 1_000_000 && InputValidator.tokens("1.5m tokens") == 1_500_000, "commas and decimals");
        check(InputValidator.tokens("abc") == -1 && InputValidator.tokens("2MM") == -1 && InputValidator.tokens("-5") == -1
                && InputValidator.tokens("1,00") == -1, "bad token amounts");
        near(InputValidator.money("$1,200.50"), 1200.5, "money");
        check(InputValidator.money("12.345") == -1 && InputValidator.money("$-3") == -1, "bad money");
        check(InputValidator.date("2026-12-31") && !InputValidator.date("2026-02-30") && !InputValidator.date("26-1-1"), "dates");
        check(InputValidator.modelName("gpt-6.1-sol") && InputValidator.modelName("claude-opus-5-5") && !InputValidator.modelName("gpt 6"), "model names");
        check(InputValidator.choice("3", 13) == 3 && InputValidator.choice("14", 13) == -1 && InputValidator.choice("x", 13) == -1, "menu choice");
        near(InputValidator.percent("40%"), 0.4, "percent");
        check(InputValidator.percent("140") == -1, "percent over 100");
        ModelPrice bad = new ModelPrice("p", "m-1", "standard");
        bad.input = 1;
        bad.output = 2;
        bad.cachedInput = 5;
        check(InputValidator.problems(bad).contains("cached input is more than input"), "price check finds a parse mistake");
    }

    static void patterns() {
        String text = "Input $1.25 / MTok, output $10. Context 1,048,576 tokens or 200K tokens. "
                + "Price is $2 through December 31, 2026 and $4 starting January 1, 2027. Long context ≥ 200k tokens. "
                + "Try gemini-2.5-pro or glm-4.7-flash, Free and Limited-time Free.";
        var s = PatternExtractor.summary(text);
        check(s.get("prices").containsKey("$1.25 / MTok") && s.get("prices").containsKey("$10"), "prices");
        check(s.get("context sizes").containsKey("1,048,576 tokens") && s.get("context sizes").containsKey("200K tokens"), "context sizes " + s.get("context sizes"));
        check(s.get("dated prices").size() == 2, "dated prices");
        check(s.get("long context thresholds").containsKey("≥ 200k tokens"), "long context threshold");
        check(s.get("model ids").containsKey("gemini-2.5-pro") && s.get("model ids").containsKey("glm-4.7-flash"), "model ids");
        check(s.get("free offers").containsKey("limited-time free"), "free offers");
        check(PatternExtractor.firstSize(PatternExtractor.CONTEXT_WINDOW_FIRST, "1,050,000 context window") == 1050000, "value before label");
        check(PatternExtractor.firstSize(PatternExtractor.CONTEXT_WINDOW, "Context window\n1M tokens") == 1000000, "value after label");
    }

    static ModelPrice price(String model, String tier, double in, double cached, double out) {
        ModelPrice p = new ModelPrice("p", model, tier);
        p.input = in;
        p.cachedInput = cached;
        p.output = out;
        return p;
    }

    static void cost() {
        Workload w = new Workload();
        w.inputPerDay = 2_000_000;
        w.outputPerDay = 500_000;
        ModelPrice p = price("m", "standard", 2, 0.5, 6);
        // 30 days * (2M * $2 + 0.5M * $6) / 1M = 30 * (4 + 3) = 210
        near(CostCalculator.monthly(p, w).total, 210, "plain cost");
        w.cachedShare = 0.5;
        // 30 * (1M * 2 + 1M * 0.5 + 0.5M * 6) / 1M = 30 * 5.5 = 165
        near(CostCalculator.monthly(p, w).total, 165, "half the input cached");
        p.cachedInput = -1;
        near(CostCalculator.monthly(p, w).total, 210, "no cache price falls back to input price");
        p.longThreshold = 200000;
        p.longInput = 4;
        p.longOutput = 12;
        w.cachedShare = 0;
        w.promptSize = 250000;
        CostCalculator.Breakdown b = CostCalculator.monthly(p, w);
        near(b.total, 420, "long context prices used");
        check(b.longPrices, "long flag");
        w.cachedShare = 2;
        throwsError(() -> CostCalculator.monthly(price("m", "standard", 1, 1, 1), w), "cached share over 1");
        throwsError(() -> CostCalculator.monthly(new ModelPrice("p", "m", "standard"), new Workload()), "model without prices");
    }

    static void recommend() {
        ArrayList<ModelPrice> list = new ArrayList<>();
        list.add(price("cheap", "standard", 0.1, -1, 0.4));
        list.add(price("mid", "standard", 1, 0.1, 4));
        list.add(price("mid", "batch", 0.5, 0.05, 2));
        list.add(price("pricey", "standard", 10, 1, 50));
        list.add(price("fast", "priority", 0.01, 0.01, 0.01));
        list.get(0).contextWindow = 32000;
        list.get(1).contextWindow = 200000;
        list.get(2).contextWindow = 200000;
        list.add(price("unknown-size", "standard", 0.2, 0.02, 0.8)); // no context window listed
        Workload w = new Workload();
        w.inputPerDay = 1_000_000;
        w.outputPerDay = 100_000;

        ArrayList<Recommender.Pick> r = Recommender.recommend(list, w, 10);
        check(r.size() == 4 && r.get(0).price.model.equals("cheap") && r.get(3).price.model.equals("pricey"), "cheapest first, priority tier left out");
        check(r.get(2).price.model.equals("mid") && r.get(2).price.tier.equals("standard"), "batch not used when not allowed");
        w.batchOk = true;
        check(Recommender.recommend(list, w, 10).get(2).price.tier.equals("batch"), "batch used when allowed, one row per model");
        w.minContext = 100000;
        ArrayList<Recommender.Pick> big = Recommender.recommend(list, w, 10);
        check(big.size() == 1 && big.get(0).price.model.equals("mid"), "small and unknown context filtered");
        w.minContext = 0;
        w.budget = 10;
        check(Recommender.recommend(list, w, 10).size() == 2, "budget filter");
    }

    static void savedData(String folder) throws Exception {
        DataStore store = new DataStore(folder);
        if (!store.hasData()) {
            check(false, "no saved data in " + folder);
            return;
        }
        ArrayList<ModelPrice> all = CrawlAll.parseSaved(store);
        int[] perProvider = new int[5];
        String[] names = {"openai", "gemini", "anthropic", "zai", "xai"};
        int problems = 0;
        for (ModelPrice p : all) {
            for (int i = 0; i < 5; i++) {
                if (p.provider.equals(names[i])) {
                    perProvider[i]++;
                }
            }
            problems += InputValidator.problems(p).size();
        }
        for (int i = 0; i < 5; i++) {
            check(perProvider[i] >= 10, names[i] + " has at least 10 prices (" + perProvider[i] + ")");
        }
        check(problems == 0, "no price fails the sanity checks");

        // values copied by hand from the official pages on the crawl day
        expect(all, "anthropic", "claude-opus-5-5", "standard", 4, 0.2, 20);
        expect(all, "anthropic", "claude-haiku-4-5", "batch", 0.5, 0.05, 2.5);
        expect(all, "xai", "grok-4.7", "standard", 2, 0.5, 6);
        expect(all, "zai", "glm-5.3", "standard", 1.4, 0.26, 4.4);
        expect(all, "gemini", "gemini-2.5-pro", "standard", 1.25, 0.125, 10);
        expect(all, "openai", "gpt-6-astra", "batch", 5, 0.5, 25);
    }

    static void expect(ArrayList<ModelPrice> all, String provider, String model, String tier, double in, double cached, double out) {
        for (ModelPrice p : all) {
            if (p.provider.equals(provider) && p.model.equals(model) && p.tier.equals(tier)) {
                check(Math.abs(p.input - in) < 1e-9 && Math.abs(p.cachedInput - cached) < 1e-9 && Math.abs(p.output - out) < 1e-9,
                        model + " " + tier + " matches the official page");
                return;
            }
        }
        check(false, model + " " + tier + " was not parsed");
    }

    static void engine(String folder) throws Exception {
        Path copy = Files.createTempDirectory("tt");
        for (String sub : new String[] {"raw", "pages"}) {
            Files.createDirectories(copy.resolve(sub));
            try (var files = Files.list(Path.of(folder, sub))) {
                for (Path f : files.toList()) {
                    Files.copy(f, copy.resolve(sub).resolve(f.getFileName()));
                }
            }
        }
        Files.copy(Path.of(folder, "prices.csv"), copy.resolve("prices.csv"));

        long t0 = System.nanoTime();
        Engine e = new Engine(copy.toString());
        e.load();
        long loadMs = (System.nanoTime() - t0) / 1_000_000;
        check(e.pages.size() >= 40 && e.index.words().size() > 1000, "index built from saved pages");
        check(loadMs < 5000, "load and index in under 5 seconds (" + loadMs + " ms)");

        long t1 = System.nanoTime();
        Engine.SearchResult r = e.search("gemnii pricing");
        long searchMs = (System.nanoTime() - t1) / 1_000_000;
        check(r.unknownWords.contains("gemnii") && r.suggestions.get("gemnii").get(0).word.equals("gemini"), "spell suggestion in search");
        check(!r.pages.isEmpty() && r.timesSearched == 1, "ranked pages and search counted");
        check(searchMs < 2000, "search in under 2 seconds (" + searchMs + " ms)");
        throwsError(() -> e.search("<script>"), "bad characters rejected");
        throwsError(() -> e.frequency("pricing", "https://not.crawled"), "unknown url rejected");

        check(e.complete("claude-op", 5).contains("claude-opus-5-5"), "completion finds a model id");
        check(e.card("claude-opus-5-5").get(0).contextWindow == 1_000_000, "context window read from the model page");

        Workload w = new Workload();
        w.inputPerDay = 2_000_000;
        w.outputPerDay = 500_000;
        w.batchOk = true;
        ArrayList<Recommender.Pick> picks = e.recommend(w, 5);
        boolean ordered = true;
        for (int i = 1; i < picks.size(); i++) {
            ordered &= picks.get(i - 1).cost.total <= picks.get(i).cost.total;
        }
        check(picks.size() == 5 && ordered, "five picks cheapest first");
    }
}
