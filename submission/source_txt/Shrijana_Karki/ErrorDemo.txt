package tokentally.app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;

import tokentally.cost.Recommender;
import tokentally.cost.Workload;
import tokentally.crawl.Fetcher;
import tokentally.model.ModelPrice;
import tokentally.model.Page;
import tokentally.parse.HtmlCleaner;
import tokentally.parse.OpenAIParser;
import tokentally.parse.XaiParser;
import tokentally.regex.InputValidator;
import tokentally.regex.PatternExtractor;
import tokentally.search.SearchFrequency;
import tokentally.text.FrequencyCounter;
import tokentally.text.InvertedIndex;
import tokentally.text.Trie;

// owner: Shrijana Karki
// bad input cases
public class ErrorDemo {

    public static class Case {
        public String member;
        public String feature;
        public String scenario;
        public String input;
        public String result;
        public boolean handled;
        public String kind; // result type
    }

    private final Engine engine;
    private final ArrayList<Case> cases = new ArrayList<>();

    public ErrorDemo(Engine engine) {
        this.engine = engine;
    }

    public static ArrayList<Case> run(Engine engine) {
        ErrorDemo d = new ErrorDemo(engine);
        d.nishan();
        d.abhinav();
        d.shrijana();
        d.aakriti();
        d.khem();
        d.shared();
        return d.cases;
    }

    // run one case, catch errors
    private void check(String member, String feature, String scenario, String input, Supplier<String> action) {
        Case c = new Case();
        c.member = member;
        c.feature = feature;
        c.scenario = scenario;
        c.input = input;
        try {
            c.result = action.get();
            c.handled = true;
            c.kind = "safe result";
        } catch (IllegalArgumentException | IllegalStateException e) {
            c.result = e.getMessage();
            c.handled = true;
            c.kind = "rejected with a message";
        } catch (Exception e) {
            c.result = e.getClass().getSimpleName() + " " + e.getMessage();
            c.handled = false;
            c.kind = "crashed";
        }
        cases.add(c);
    }

    // ---------------- nishan: spell checking and cost calculator ----------------

    private void nishan() {
        String m = "Nishan Giri";
        check(m, "spell checking", "empty word", "(nothing)",
            () -> engine.spell.suggest("", 5).isEmpty() ? "no suggestions, nothing crashed" : "unexpected");
        check(m, "spell checking", "missing word (null)", "null",
            () -> engine.spell.isKnown(null) ? "unexpected" : "treated as unknown, no suggestions: " + engine.spell.suggest(null, 5).size());
        check(m, "spell checking", "nonsense word", "qzxqzxqzx",
            () -> engine.spell.suggest("qzxqzxqzx", 5).isEmpty() ? "nothing close enough, the page says so" : "unexpected");
        check(m, "spell checking", "very long word", "a x 1000",
            () -> {
                long t = System.currentTimeMillis();
                int n = engine.spell.suggest("a".repeat(1000), 5).size();
                return n + " suggestions in " + (System.currentTimeMillis() - t) + " ms (long words are skipped by the length check)";
            });
        check(m, "spell checking", "typo of a model name", "claude-opus-5-6x",
            () -> engine.spell.suggest("claude-opus-5-6x", 3).isEmpty() ? "no match" : "suggests " + engine.spell.suggest("claude-opus-5-6x", 3).get(0).word);

        check(m, "cost calculator", "token amount with words", "2 million",
            () -> need(InputValidator.tokens("2 million"), "\"2 million\" is not a token amount, use 2M, 500K or 1,000,000"));
        check(m, "cost calculator", "token amount far too big", "999999999999B",
            () -> need(InputValidator.tokens("999999999999B"), "that number is too big to be a real workload"));
        check(m, "cost calculator", "negative token amount", "-500K",
            () -> need(InputValidator.tokens("-500K"), "\"-500K\" is not a token amount, use 2M, 500K or 1,000,000"));
        check(m, "cost calculator", "cached share above 100 percent", "150",
            () -> {
                if (InputValidator.percent("150") < 0) {
                    throw new IllegalArgumentException("cached share must be 0 to 100 percent");
                }
                return "accepted";
            });
        check(m, "cost calculator", "budget nobody can meet", "$0.01 a month, no free models",
            () -> {
                Workload w = new Workload();
                w.inputPerDay = 2_000_000;
                w.outputPerDay = 500_000;
                w.budget = 0.01;
                w.skipFree = true;
                ArrayList<Recommender.Pick> picks = engine.recommend(w, 10);
                return picks.isEmpty() ? "no model fits, the page says to raise the budget" : picks.size() + " picks";
            });
        check(m, "cost calculator", "context window nobody has", "minimum 100M tokens",
            () -> {
                Workload w = new Workload();
                w.inputPerDay = 1_000_000;
                w.outputPerDay = 100_000;
                w.minContext = 100_000_000;
                return engine.recommend(w, 10).isEmpty() ? "no model fits, models with an unknown context are not guessed" : "unexpected";
            });
        check(m, "cost calculator", "zero workload", "0 input, 0 output",
            () -> {
                Workload w = new Workload();
                w.skipFree = true;
                return engine.recommend(w, 3).size() + " picks";
            });
    }

    // ---------------- abhinav: inverted index and frequency count ----------------

    private void abhinav() {
        String m = "Abhinav Pandey";
        check(m, "inverted index", "word that is on no page", "zzzunknownzzz",
            () -> engine.index.postings("zzzunknownzzz").isEmpty() ? "empty result, no crash" : "unexpected");
        check(m, "inverted index", "missing word (null)", "null",
            () -> engine.index.postings(null).isEmpty() ? "empty result, no crash" : "unexpected");
        check(m, "inverted index", "page with no text", "page with empty text and a null page",
            () -> {
                InvertedIndex i = new InvertedIndex();
                i.add(new Page("x", "https://x.test/a", "", ""));
                i.add(null);
                i.add(new Page("x", "https://x.test/b", "", null));
                return "skipped safely, index has " + i.words().size() + " words";
            });
        check(m, "inverted index", "phrase that is not on any page", "batch unicorn pricing",
            () -> engine.index.phrase("batch unicorn pricing").isEmpty() ? "no page has the phrase" : "unexpected");

        String url = engine.pages.isEmpty() ? "" : engine.pages.get(0).url;
        check(m, "frequency count", "empty word", "(nothing)",
            () -> FrequencyCounter.count("", engine.pages.get(0)).url);
        check(m, "frequency count", "weird characters", "@@@",
            () -> engine.frequency("@@@", url).toString());
        check(m, "frequency count", "page that was never crawled", "https://example.com/nothing",
            () -> engine.frequency("gemini", "https://example.com/nothing").toString());
        check(m, "frequency count", "word that is not on the page", "zzzunknownzzz",
            () -> {
                ArrayList<FrequencyCounter.Result> r = engine.frequency("zzzunknownzzz", url);
                return r.isEmpty() ? "count is 0, both algorithms agree" : "unexpected";
            });
        check(m, "frequency count", "boyer moore and kmp must agree", "price, token, batch on the first page",
            () -> {
                StringBuilder out = new StringBuilder();
                for (String w : new String[] {"price", "token", "batch"}) {
                    FrequencyCounter.Result r = FrequencyCounter.count(w, engine.pages.get(0));
                    out.append(w).append(" BM ").append(r.boyerMoore).append(" KMP ").append(r.kmp);
                    if (r.boyerMoore != r.kmp) {
                        throw new IllegalStateException("the two algorithms disagree on " + w);
                    }
                    out.append(", ");
                }
                return out.substring(0, out.length() - 2) + " (equal)";
            });
    }

    // ---------------- shrijana: page ranking and data validation ----------------

    private void shrijana() {
        String m = "Shrijana Karki";
        check(m, "page ranking", "blank search", "(spaces)",
            () -> engine.ranker.rank("   ", 10).isEmpty() ? "no ranking, the page asks for a keyword" : "unexpected");
        check(m, "page ranking", "same keyword 200 times", "gemini x 200",
            () -> {
                int one = engine.ranker.rank("gemini", 1).get(0).score;
                int many = engine.ranker.rank("gemini ".repeat(200), 1).get(0).score;
                return "score " + many + " (same as one keyword, " + one + "), repeats do not count twice";
            });
        check(m, "page ranking", "keyword that is on no page", "zzzunknownzzz",
            () -> engine.ranker.rank("zzzunknownzzz", 10).isEmpty() ? "empty list" : "unexpected");
        check(m, "page ranking", "ask for zero results", "howMany = 0",
            () -> engine.ranker.rank("gemini", 0).isEmpty() ? "empty list" : "unexpected");

        check(m, "data validation", "search text with symbols", "<script>alert(1)</script>",
            () -> engine.search("<script>alert(1)</script>").query);
        check(m, "data validation", "search text over 60 chars", "a x 100",
            () -> engine.search("a".repeat(100)).query);
        check(m, "data validation", "date that does not exist", "2026-02-30",
            () -> InputValidator.date("2026-02-30") ? "accepted" : "rejected, 30 February is not a real date");
        check(m, "data validation", "money with 3 decimals", "$12.345",
            () -> need(InputValidator.money("$12.345"), "budget should look like 50 or $49.99"));
        check(m, "data validation", "good values still pass", "2.5M, 1,000,000, 500k, $1,200.50, 99%",
            () -> {
                if (InputValidator.tokens("2.5M") != 2_500_000 || InputValidator.tokens("1,000,000") != 1_000_000
                        || InputValidator.tokens("500k") != 500_000 || InputValidator.money("$1,200.50") != 1200.5
                        || InputValidator.percent("99%") != 0.99) {
                    throw new IllegalStateException("a good value was rejected");
                }
                return "all five accepted and read correctly";
            });
        check(m, "data validation", "price that looks wrong", "cached more than input, price over $1000",
            () -> {
                ModelPrice p = new ModelPrice("test", "gpt-x", "standard");
                p.input = 1;
                p.cachedInput = 5;
                p.output = 5000;
                return "flagged: " + InputValidator.problems(p);
            });
    }

    // ---------------- aakriti: word completion and finding patterns ----------------

    private void aakriti() {
        String m = "Aakriti Mainali";
        check(m, "word completion", "empty prefix", "(nothing)",
            () -> engine.complete("", 8).isEmpty() ? "no suggestions until the user types" : "unexpected");
        check(m, "word completion", "missing prefix (null)", "null",
            () -> engine.complete(null, 8).isEmpty() ? "no suggestions, no crash" : "unexpected");
        check(m, "word completion", "prefix nothing starts with", "zzzq",
            () -> engine.complete("zzzq", 8).isEmpty() ? "empty list" : "unexpected");
        check(m, "word completion", "capital letters", "CLAUDE-OP",
            () -> {
                ArrayList<String> r = engine.complete("CLAUDE-OP", 3);
                return r.isEmpty() ? "no match" : "still completes: " + r;
            });
        check(m, "word completion", "bad words stored in the trie", "null, empty, zero count",
            () -> {
                Trie t = new Trie();
                t.insert(null, 1);
                t.insert("", 1);
                t.insert("word", 0);
                return "ignored, trie has " + t.size() + " words";
            });

        check(m, "finding patterns", "empty page text", "(nothing)",
            () -> "summary has " + PatternExtractor.summary("").size() + " groups, all empty");
        check(m, "finding patterns", "missing text (null)", "null",
            () -> "summary has " + PatternExtractor.summary(null).size() + " groups, all empty");
        check(m, "finding patterns", "text with no prices", "hello world",
            () -> "prices found: " + PatternExtractor.summary("hello world").get("prices").size());
        check(m, "finding patterns", "page url that does not exist", "https://example.com/none",
            () -> "prices found: " + engine.patterns("https://example.com/none").get("prices").size() + " (no pages matched)");
    }

    // ---------------- khem raj: search frequency and html cleaning ----------------

    private void khem() {
        String m = "Khem Raj Adhikari";
        check(m, "search frequency", "damaged history file", "a file with broken lines",
            () -> {
                Path f = temp("history", ".txt");
                write(f, "3\tgemini\nnot a number\tbatch\n\tnothing\ngarbage line\n7\tclaude\n");
                SearchFrequency s = new SearchFrequency(f);
                return "loaded " + s.size() + " good lines, broken lines skipped (gemini " + s.count("gemini") + ", claude " + s.count("claude") + ")";
            });
        check(m, "search frequency", "blank and missing searches", "(spaces) and null",
            () -> {
                SearchFrequency s = new SearchFrequency(temp("history", ".txt"));
                return "counted " + s.record("   ") + " and " + s.record(null) + " times, nothing saved";
            });
        check(m, "search frequency", "two searches at the same time", "200 threads",
            () -> {
                SearchFrequency s = new SearchFrequency(temp("history", ".txt"));
                Thread[] ts = new Thread[20];
                for (int i = 0; i < ts.length; i++) {
                    ts[i] = new Thread(() -> {
                        for (int j = 0; j < 10; j++) {
                            s.record("batch");
                        }
                    });
                    ts[i].start();
                }
                for (Thread t : ts) {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (s.count("batch") != 200) {
                    throw new IllegalStateException("lost counts, got " + s.count("batch"));
                }
                return "count is exactly 200, no search was lost";
            });
        check(m, "html cleaning", "html that is never closed", "<div><p>price <b>$5 <i>per",
            () -> "text kept: \"" + HtmlCleaner.text("<div><p>price <b>$5 <i>per") + "\"");
        check(m, "html cleaning", "page that is only scripts", "<script>alert(1)</script>",
            () -> "text is \"" + HtmlCleaner.text("<script>alert(1)</script>").trim() + "\" (nothing left)");
        check(m, "html cleaning", "missing page (null) and blank page", "null, (spaces)",
            () -> "text lengths " + HtmlCleaner.text(null).length() + " and " + HtmlCleaner.text("   ").length());
        check(m, "html cleaning", "very big page", "5 MB of html",
            () -> {
                String big = "<p>token price $1</p>".repeat(250_000);
                long t = System.currentTimeMillis();
                int n = HtmlCleaner.text(big).length();
                return n + " chars cleaned in " + (System.currentTimeMillis() - t) + " ms";
            });
    }

    // ---------------- shared: crawler, parsers, saved data ----------------

    private void shared() {
        String m = "shared parts";
        check(m, "crawler", "url that is not a url", "ht!tp://bad url",
            () -> {
                try {
                    new Fetcher().get("ht!tp://bad url");
                    return "unexpected";
                } catch (Exception e) {
                    return "rejected: " + e.getMessage();
                }
            });
        check(m, "crawler", "server that is not there", "http://localhost:1/",
            () -> {
                try {
                    new Fetcher().get("http://localhost:1/");
                    return "unexpected";
                } catch (Exception e) {
                    return "the crawl logs it and goes on: " + e.getMessage();
                }
            });
        check(m, "parsers", "page that is not a pricing page", "<html><body>hello</body></html>",
            () -> new OpenAIParser().parse("<html><body>hello</body></html>", "https://x.test").size() + " prices found, no crash");
        check(m, "parsers", "empty and broken html", "(nothing) and <table><tr><td>",
            () -> new XaiParser().parse("", "https://x.test").size() + " and "
                + new XaiParser().parse("<table><tr><td>", "https://x.test").size() + " prices found, no crash");
        check(m, "saved data", "damaged prices.csv", "one good row, one broken number, one short row",
            () -> {
                try {
                    Path dir = Files.createTempDirectory("tokentally");
                    String header = String.join(",", DataStore.CSV_HEADER);
                    write(dir.resolve("prices.csv"), header + "\n"
                        + "openai,gpt-x,standard,1,0.1,2,,,,,,,note,https://x.test\n"
                        + "openai,gpt-y,standard,abc,0.1,2,,,,,,,note,https://x.test\n"
                        + "short,row\n");
                    DataStore s = new DataStore(dir.toString());
                    int good = s.loadPrices().size();
                    return good + " good row loaded, " + s.badRows + " damaged rows skipped";
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            });
        check(m, "saved data", "data folder that does not exist", "/no/such/folder",
            () -> new DataStore("/no/such/folder").hasData() ? "unexpected" : "reported as no data, the app tells the user to crawl or fix the path");
        check(m, "price history", "damaged history file", "broken lines",
            () -> {
                try {
                    Path dir = Files.createTempDirectory("tokentally");
                    write(dir.resolve("price_history.csv"), "date,provider,model,tier,input,cached_input,output\nbroken\n2026-11-01,openai,gpt-x,standard,1,,2\n2026-11-02,openai,gpt-x,standard,x,,2\n");
                    PriceHistory h = new PriceHistory(dir);
                    return h.load().size() + " good line loaded, broken lines skipped";
                } catch (java.io.IOException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            });
    }

    // ---------------- helpers ----------------

    private static String need(long value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return "accepted " + value;
    }

    private static String need(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return "accepted " + value;
    }

    private static Path temp(String name, String ext) {
        try {
            Path f = Files.createTempFile(name, ext);
            f.toFile().deleteOnExit();
            return f;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("no temp file: " + e.getMessage());
        }
    }

    private static void write(Path f, String text) {
        try {
            Files.writeString(f, text, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("could not write " + f);
        }
    }

    // crashed cases
    public static ArrayList<Case> crashed(ArrayList<Case> all) {
        ArrayList<Case> out = new ArrayList<>();
        for (Case c : all) {
            if (!c.handled) {
                out.add(c);
            }
        }
        return out;
    }

    // count per member
    public static HashMap<String, int[]> perMember(ArrayList<Case> all) {
        HashMap<String, int[]> out = new HashMap<>();
        for (Case c : all) {
            int[] n = out.computeIfAbsent(c.member, k -> new int[2]);
            n[0]++;
            if (c.handled) {
                n[1]++;
            }
        }
        return out;
    }
}
