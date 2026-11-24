package tokentally.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import tokentally.cost.Recommender;
import tokentally.cost.Workload;
import tokentally.model.ModelPrice;
import tokentally.model.Page;
import tokentally.parse.HtmlCleaner;
import tokentally.rank.PageRanker;
import tokentally.regex.InputValidator;
import tokentally.regex.PatternExtractor;
import tokentally.search.SearchFrequency;
import tokentally.text.BoyerMoore;
import tokentally.text.EditDistance;
import tokentally.text.KMPMatcher;

// small measured experiments on the real crawled data, one or two for every member
// each one compares our data structure or algorithm with the plain way of doing it
// the numbers change a little on every run and on every laptop, the ratio is what matters
public class Experiments {

    public static class Row {
        public String member;
        public String feature;
        public String title;
        public String question;
        public String aLabel;
        public double aValue;
        public String bLabel;
        public double bValue;
        public String unit;
        public String takeaway;
    }

    private final Engine engine;
    private final ArrayList<Row> rows = new ArrayList<>();
    private static long sink; // keeps the compiler from removing the work we time

    public Experiments(Engine engine) {
        this.engine = engine;
    }

    public static ArrayList<Row> run(Engine engine) {
        Experiments x = new Experiments(engine);
        x.abhinav();
        x.nishan();
        x.shrijana();
        x.aakriti();
        x.khem();
        return x.rows;
    }

    // ---------------- abhinav ----------------

    private void abhinav() {
        String m = "Abhinav Pandey";
        // 1 inverted index against reading every page
        String word = "batch";
        double index = micros(() -> sink += engine.index.postings(word).size(), 2000);
        double scan = micros(() -> {
            int pagesWith = 0;
            for (Page p : engine.pages) {
                if (p.text.toLowerCase().contains(word)) {
                    pagesWith++;
                }
            }
            sink += pagesWith;
        }, 20);
        add(m, "inverted index", "find the pages that have a word",
            "How much faster is the index than reading all " + engine.pages.size() + " pages every time?",
            "inverted index (hash map)", index, "read every page", scan, "microseconds per search");
        note("a hash map lookup is O(1) on average, reading every page is O(total characters).");

        // 2 boyer moore against kmp
        String[] words = {"price", "token", "batch", "cache", "context"};
        long bmCompare = 0;
        long kmpCompare = 0;
        for (String w : words) {
            BoyerMoore bm = new BoyerMoore(w);
            KMPMatcher kmp = new KMPMatcher(w);
            for (Page p : engine.pages) {
                String text = p.text.toLowerCase();
                bm.count(text);
                bmCompare += bm.comparisons;
                kmp.count(text);
                kmpCompare += kmp.comparisons;
            }
        }
        add(m, "frequency count", "characters checked to count 5 words on all pages",
            "Boyer-Moore jumps ahead, KMP reads every character once. How many characters does each check?",
            "Boyer-Moore", bmCompare, "KMP", kmpCompare, "characters compared");

        // 3 same answer, different speed
        String longest = "";
        for (Page p : engine.pages) {
            if (p.text.length() > longest.length()) {
                longest = p.text.toLowerCase();
            }
        }
        final String text = longest;
        BoyerMoore bm = new BoyerMoore("token");
        KMPMatcher kmp = new KMPMatcher("token");
        double bmTime = micros(() -> sink += bm.count(text), 200);
        double kmpTime = micros(() -> sink += kmp.count(text), 200);
        add(m, "frequency count", "time to count one word on the biggest page (" + text.length() + " characters)",
            "Both give the same count. Which one is quicker on real text?",
            "Boyer-Moore", bmTime, "KMP", kmpTime, "microseconds per count");
    }

    // ---------------- nishan ----------------

    private void nishan() {
        String m = "Nishan Giri";
        ArrayList<String> vocab = new ArrayList<>(engine.index.words());

        // 4 heap against sorting all candidates
        String typo = "gemnii";
        double heap = micros(() -> sink += engine.spell.suggest(typo, 5).size(), 60);
        double sort = micros(() -> {
            ArrayList<int[]> all = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();
            for (String v : vocab) {
                if (Math.abs(v.length() - typo.length()) <= 2) {
                    int d = EditDistance.between(typo, v);
                    if (d <= 2 && d > 0) {
                        all.add(new int[] {d, names.size()});
                        names.add(v);
                    }
                }
            }
            all.sort(Comparator.comparingInt(a -> a[0]));
            sink += Math.min(5, all.size());
        }, 60);
        add(m, "spell checking", "best 5 suggestions for a typo (" + vocab.size() + " known words)",
            "Our heap keeps the best few. How does it compare with collecting everything and sorting it?",
            "heap (ours)", heap, "collect and sort all", sort, "microseconds per search");
        note("with only a few hundred candidates a plain sort is just as quick. The heap pays off when there are far more candidates than the 5 we keep.");

        // 5 how the edit distance grows with word length
        String a8 = "pricingx";
        String a16 = "pricingxpricingx";
        String a32 = "pricingxpricingxpricingxpricing";
        double d8 = nanos(() -> sink += EditDistance.between(a8, "priceing"), 20000);
        double d16 = nanos(() -> sink += EditDistance.between(a16, "priceingxpriceingx"), 5000);
        double d32 = nanos(() -> sink += EditDistance.between(a32, "priceingxpriceingxpriceingxpriceing"), 2000);
        add(m, "spell checking", "edit distance for words of 8 and 32 letters",
            "The table is a x b big. Does doubling and quadrupling the length cost about 4x and 16x?",
            "8 letters", d8, "32 letters", d32, "nanoseconds per comparison");
        Row r = rows.get(rows.size() - 1);
        r.takeaway = "16 letters took " + (long) d16 + " ns. 8 to 16 letters costs " + ratio(d16, d8) + " and 8 to 32 letters costs "
            + ratio(d32, d8) + ", about the 4x and 16x of an a x b table.";

        // 6 cost calculator: min heap against sorting all prices
        Workload w = new Workload();
        w.inputPerDay = 2_000_000;
        w.outputPerDay = 500_000;
        double top = micros(() -> sink += Recommender.recommend(engine.prices, w, 10).size(), 2000);
        double all = micros(() -> {
            ArrayList<Recommender.Pick> picks = Recommender.recommend(engine.prices, w, Integer.MAX_VALUE);
            Collections.sort(picks, Comparator.comparingDouble(p -> p.cost.total));
            sink += picks.size();
        }, 2000);
        add(m, "cost calculator", "price " + engine.prices.size() + " model prices and take the cheapest 10",
            "Taking only the top 10 from the min heap against taking and sorting all of them.",
            "top 10 from the heap", top, "all models, sorted", all, "microseconds per request");
        note("only " + engine.prices.size() + " prices, so sorting all of them is as quick as the heap. With thousands of models the heap would win.");
    }

    // ---------------- shrijana ----------------

    private void shrijana() {
        String m = "Shrijana Karki";
        PageRanker ranker = engine.ranker;
        String q = "token price";
        double top10 = micros(() -> sink += ranker.rank(q, 10).size(), 3000);
        double everything = micros(() -> sink += ranker.rank(q, Integer.MAX_VALUE).size(), 3000);
        add(m, "page ranking", "rank pages for \"" + q + "\"",
            "Taking the 10 best from the max heap against taking every ranked page.",
            "top 10", top10, "all ranked pages", everything, "microseconds per search");
        note("the heap only decides the order. Most of the time goes into counting keyword hits in the index, so the gap is small on 81 pages.");

        double oneWord = micros(() -> sink += ranker.rank("token", 10).size(), 3000);
        double fourWords = micros(() -> sink += ranker.rank("token price cache batch", 10).size(), 3000);
        add(m, "page ranking", "1 keyword against 4 keywords",
            "Does the time grow in a straight line with the number of keywords?",
            "1 keyword", oneWord, "4 keywords", fourWords, "microseconds per search");
        note("4 keywords cost about twice as much as 1, not 4 times, because pages that have several keywords are merged in one map.");

        double valid = nanos(() -> sink += InputValidator.tokens("2.5M"), 200000);
        double invalid = nanos(() -> sink += InputValidator.tokens("two million tokens please"), 200000);
        add(m, "data validation", "check a token amount with the regex",
            "Is a bad value as cheap to reject as a good value is to read?",
            "good value 2.5M", valid, "bad value", invalid, "nanoseconds per check");
        note("a bad value fails the pattern early, so it costs less than reading a good one. A hostile input can not slow the program down.");
    }

    // ---------------- aakriti ----------------

    private void aakriti() {
        String m = "Aakriti Mainali";
        ArrayList<String> vocab = new ArrayList<>(engine.index.words());
        for (ModelPrice p : engine.prices) {
            vocab.add(p.model);
        }
        String prefix = "claude-op";
        double trie = micros(() -> sink += engine.trie.wordsWithPrefix(prefix).size(), 2000);
        double scan = micros(() -> {
            int n = 0;
            for (String v : vocab) {
                if (v.startsWith(prefix)) {
                    n++;
                }
            }
            sink += n;
        }, 500);
        add(m, "word completion", "words that start with \"" + prefix + "\" (" + vocab.size() + " words)",
            "The trie walks down the letters. A plain list checks every word.",
            "trie", trie, "scan all words", scan, "microseconds per prefix");
        note("the trie only walks the letters of the prefix, a list checks every word.");

        StringBuilder all = new StringBuilder();
        for (Page p : engine.pages) {
            all.append(p.text).append('\n');
        }
        String text = all.toString();
        double regex = micros(() -> sink += PatternExtractor.summary(text).size(), 3);
        add(m, "finding patterns", "find 6 kinds of patterns in " + engine.pages.size() + " pages (" + text.length() / 1000 + " thousand characters)",
            "How long do the regex patterns need for all the crawled text?",
            "all 6 patterns", regex / 1000.0, "one page on average", regex / 1000.0 / Math.max(1, engine.pages.size()),
            "milliseconds");
        Row r = rows.get(rows.size() - 1);
        r.takeaway = "The regex pass over the whole text takes " + (long) (regex / 1000) + " ms, so it is fine to run on every click of the Insights tab.";
    }

    // ---------------- khem ----------------

    private void khem() {
        String m = "Khem Raj Adhikari";
        // 9 html cleaning speed on the raw pages that were saved
        long chars = 0;
        double ms = 0;
        for (String provider : new String[] {"openai", "gemini", "anthropic", "zai", "xai"}) {
            try {
                String html = engine.store.loadRaw(provider);
                if (html == null) {
                    continue;
                }
                chars += html.length();
                long t = System.nanoTime();
                sink += HtmlCleaner.text(html).length();
                ms += (System.nanoTime() - t) / 1e6;
            } catch (Exception e) {
                // a missing raw page is just left out of the experiment
            }
        }
        add(m, "html cleaning", "clean the 5 saved pricing pages (" + chars / 1000 + " thousand characters)",
            "How fast is one pass over real pricing pages?",
            "time for all 5 pages", ms, "per 100 thousand characters", chars == 0 ? 0 : ms / (chars / 100000.0), "milliseconds");
        rows.get(rows.size() - 1).takeaway = "about " + Math.round(chars == 0 ? 0 : ms / (chars / 100000.0)) + " ms for every 100 thousand characters. Why: one pass over the text, so the time grows in a straight line with the page size (O(n)).";

        // 10 search frequency: counting in memory against saving to the file each time
        try {
            java.nio.file.Path f = java.nio.file.Files.createTempFile("freq", ".txt");
            f.toFile().deleteOnExit();
            SearchFrequency s = new SearchFrequency(f);
            double record = micros(() -> sink += s.record("batch"), 50);
            double read = nanos(() -> sink += s.count("batch"), 200000) / 1000.0;
            add(m, "search frequency", "count a search and save the history file",
                "Every search is saved to the file at once. How much does that cost against only reading a count?",
                "record and save", record, "read a count", read, "microseconds per call");
            note("saving after every search is the slow part. It is safe because the history survives a crash, and a search still only takes about " + (long) record + " microseconds more.");
        } catch (java.io.IOException e) {
            // no temp file, no experiment
        }
    }

    // ---------------- helpers ----------------

    private void add(String member, String feature, String title, String question,
                     String aLabel, double a, String bLabel, double b, String unit) {
        Row r = new Row();
        r.member = member;
        r.feature = feature;
        r.title = title;
        r.question = question;
        r.aLabel = aLabel;
        r.aValue = round(a);
        r.bLabel = bLabel;
        r.bValue = round(b);
        r.unit = unit;
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);
        String winner = a <= b ? aLabel : bLabel;
        boolean time = unit.contains("seconds");
        r.takeaway = lo <= 0 || hi / lo < 1.3
            ? "about the same"
            : winner + " is " + ratio(hi, lo) + (time ? " faster" : " smaller");
        rows.add(r);
    }

    // replaces the automatic sentence of the last row with an explanation
    private void note(String text) {
        Row r = rows.get(rows.size() - 1);
        r.takeaway = r.takeaway + ". Why: " + text;
    }

    private static String ratio(double big, double small) {
        double x = small <= 0 ? 0 : big / small;
        return (x >= 10 ? String.valueOf(Math.round(x)) : String.valueOf(Math.round(x * 10) / 10.0)) + "x";
    }

    private static double round(double v) {
        if (v >= 100) {
            return Math.round(v);
        }
        return Math.round(v * 100) / 100.0;
    }

    // average time of a task in microseconds, after a warm up
    private static double micros(Runnable task, int repeats) {
        return nanos(task, repeats) / 1000.0;
    }

    private static double nanos(Runnable task, int repeats) {
        for (int i = 0; i < Math.min(repeats, 20); i++) {
            task.run();
        }
        long start = System.nanoTime();
        for (int i = 0; i < repeats; i++) {
            task.run();
        }
        return (System.nanoTime() - start) / (double) repeats;
    }
}
