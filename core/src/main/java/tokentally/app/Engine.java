package tokentally.app;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import tokentally.cost.Recommender;
import tokentally.cost.Workload;
import tokentally.model.ModelPrice;
import tokentally.model.Page;
import tokentally.rank.PageRanker;
import tokentally.regex.InputValidator;
import tokentally.regex.PatternExtractor;
import tokentally.search.SearchFrequency;
import tokentally.text.FrequencyCounter;
import tokentally.text.InvertedIndex;
import tokentally.text.SpellChecker;
import tokentally.text.Tokenizer;
import tokentally.text.Trie;
import tokentally.text.WordCompleter;

// owner: Abhinav Pandey
// loads data, runs features
public class Engine {

    public final DataStore store;
    public ArrayList<ModelPrice> prices = new ArrayList<>();
    public ArrayList<Page> pages = new ArrayList<>();
    public InvertedIndex index;
    public Trie trie;
    public WordCompleter completer;
    public SpellChecker spell;
    public PageRanker ranker;
    public SearchFrequency searches;
    public PriceHistory history;

    public Engine(String dataFolder) {
        store = new DataStore(dataFolder);
        searches = new SearchFrequency(Path.of(dataFolder, "search_history.txt"));
        history = new PriceHistory(Path.of(dataFolder));
    }

    // saved data first
    public void load() throws Exception {
        if (!store.hasData()) {
            CrawlAll.run(store, 20);
        }
        prices = store.loadPrices();
        pages = store.loadPages();
        if (prices.isEmpty() || pages.isEmpty()) {
            throw new IllegalStateException("no saved data in " + store.folder() + " and the crawl found nothing, "
                    + "check the internet connection and the data folder");
        }
        history.record(today(), prices);
        build();
    }

    // crawl again
    public ArrayList<String> recrawl() throws Exception {
        ArrayList<String> log = CrawlAll.run(store, 20);
        prices = store.loadPrices();
        pages = store.loadPages();
        int moved = history.record(today(), prices);
        log.add("price history: " + moved + " new or changed price lines");
        build();
        return log;
    }

    private static String today() {
        return java.time.LocalDate.now().toString();
    }

    private void build() {
        fillSizesFromModelPages();

        index = new InvertedIndex();
        for (Page p : pages) {
            index.add(p);
        }

        // words and model ids
        HashMap<String, Integer> vocab = new HashMap<>(index.totals());
        for (ModelPrice p : prices) {
            vocab.merge(p.model, 1, Integer::sum);
        }
        trie = new Trie();
        for (var e : vocab.entrySet()) {
            trie.insert(e.getKey(), e.getValue());
        }
        completer = new WordCompleter(trie);
        spell = new SpellChecker(vocab, 2);
        ranker = new PageRanker(index);
    }

    // sizes from model pages
    private void fillSizesFromModelPages() {
        for (ModelPrice p : prices) {
            for (Page page : pages) {
                if (!page.provider.equals(p.provider) || !pageIsAbout(page.url, p.model)) {
                    continue;
                }
                // try both word orders
                if (p.contextWindow < 0) {
                    p.contextWindow = PatternExtractor.firstSize(PatternExtractor.CONTEXT_WINDOW_FIRST, page.text);
                }
                if (p.contextWindow < 0) {
                    p.contextWindow = PatternExtractor.firstSize(PatternExtractor.CONTEXT_WINDOW, page.text);
                }
                if (p.maxOutput < 0) {
                    p.maxOutput = PatternExtractor.firstSize(PatternExtractor.MAX_OUTPUT_FIRST, page.text);
                }
                if (p.maxOutput < 0) {
                    p.maxOutput = PatternExtractor.firstSize(PatternExtractor.MAX_OUTPUT, page.text);
                }
            }
        }
    }

    // is this the model page
    static boolean pageIsAbout(String url, String model) {
        String u = url.endsWith("/overview") ? url.substring(0, url.length() - 9) : url;
        String last = u.substring(u.lastIndexOf('/') + 1).toLowerCase();
        return last.equals(model) || ("claude-" + last).equals(model);
    }

    // ---------------- search ----------------

    public static class SearchResult {
        public String query;
        public int timesSearched;
        public ArrayList<String> unknownWords = new ArrayList<>();
        public LinkedHashMap<String, ArrayList<SpellChecker.Suggestion>> suggestions = new LinkedHashMap<>();
        public ArrayList<PageRanker.Result> pages = new ArrayList<>();
        public HashMap<String, Integer> phraseHits = new HashMap<>();
        public ArrayList<ModelPrice> models = new ArrayList<>();
        public String searchedInstead = ""; // suggestion used
    }

    public SearchResult search(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("type something to search for");
        }
        if (!InputValidator.searchWord(query)) {
            throw new IllegalArgumentException("use letters, digits, spaces, dots and dashes only (max 60 chars)");
        }
        SearchResult r = new SearchResult();
        r.query = query.trim().toLowerCase();
        r.timesSearched = searches.record(r.query);

        for (String w : Tokenizer.words(r.query)) {
            if (!spell.isKnown(w)) {
                r.unknownWords.add(w);
                r.suggestions.put(w, spell.suggest(w, 5));
            }
        }
        r.pages = ranker.rank(r.query, 10);

        // retry with suggestion
        if (r.pages.isEmpty() && !r.unknownWords.isEmpty()) {
            StringBuilder fixed = new StringBuilder();
            boolean allFixed = true;
            for (String w : Tokenizer.words(r.query)) {
                ArrayList<SpellChecker.Suggestion> s = r.suggestions.get(w);
                if (s == null) {
                    fixed.append(w).append(' ');
                } else if (!s.isEmpty()) {
                    fixed.append(s.get(0).word).append(' ');
                } else {
                    allFixed = false;
                }
            }
            if (allFixed) {
                r.searchedInstead = fixed.toString().trim();
                r.pages = ranker.rank(r.searchedInstead, 10);
            }
        }
        if (Tokenizer.words(r.query).size() > 1) {
            r.phraseHits = index.phrase(r.query);
        }
        for (ModelPrice p : prices) {
            if (p.tier.equals("standard") && p.model.contains(r.query.replace(' ', '-'))) {
                r.models.add(p);
            }
        }
        return r;
    }

    public ArrayList<String> complete(String prefix, int howMany) {
        if (prefix == null) {
            return new ArrayList<>();
        }
        return completer.complete(prefix.trim().toLowerCase(), howMany);
    }

    public ArrayList<FrequencyCounter.Result> frequency(String word, String url) {
        if (!InputValidator.searchWord(word)) {
            throw new IllegalArgumentException("not a valid word");
        }
        ArrayList<Page> scope = new ArrayList<>();
        for (Page p : pages) {
            if (url == null || url.isBlank() || p.url.equals(url.trim())) {
                scope.add(p);
            }
        }
        if (scope.isEmpty()) {
            throw new IllegalArgumentException("no crawled page with that url");
        }
        return FrequencyCounter.countAll(word.trim(), scope);
    }

    // ---------------- models ----------------

    public ArrayList<ModelPrice> models(String provider, String tier) {
        ArrayList<ModelPrice> out = new ArrayList<>();
        for (ModelPrice p : prices) {
            if ((provider == null || provider.isEmpty() || p.provider.equals(provider))
                    && (tier == null || tier.isEmpty() || p.tier.equals(tier))) {
                out.add(p);
            }
        }
        return out;
    }

    // all tiers
    public ArrayList<ModelPrice> card(String model) {
        ArrayList<ModelPrice> out = new ArrayList<>();
        if (model == null) {
            return out;
        }
        for (ModelPrice p : prices) {
            if (p.model.equalsIgnoreCase(model.trim())) {
                out.add(p);
            }
        }
        return out;
    }

    public ArrayList<ModelPrice> compare(String[] models) {
        ArrayList<ModelPrice> out = new ArrayList<>();
        if (models == null || models.length == 0) {
            throw new IllegalArgumentException("type at least one model name");
        }
        if (models.length > 10) {
            throw new IllegalArgumentException("compare up to 10 models at a time");
        }
        for (String m : models) {
            for (ModelPrice p : card(m)) {
                if (p.tier.equals("standard")) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    public ArrayList<ModelPrice> freeModels() {
        ArrayList<ModelPrice> out = new ArrayList<>();
        for (ModelPrice p : prices) {
            if (p.isFree()) {
                out.add(p);
            }
        }
        return out;
    }

    // price warnings
    public ArrayList<ModelPrice> warnings() {
        ArrayList<ModelPrice> out = new ArrayList<>();
        for (ModelPrice p : prices) {
            String n = p.notes.toLowerCase();
            if (n.contains("price change") || n.contains("limited-time") || n.contains("deprecated")) {
                out.add(p);
            }
        }
        return out;
    }

    public ArrayList<Recommender.Pick> recommend(Workload w, int howMany) {
        if (w.inputPerDay + w.outputPerDay <= 0) {
            throw new IllegalArgumentException("enter some input or output tokens, a workload of zero costs nothing");
        }
        return Recommender.recommend(prices, w, howMany);
    }

    public LinkedHashMap<String, LinkedHashMap<String, Integer>> patterns(String url) {
        StringBuilder text = new StringBuilder();
        for (Page p : pages) {
            if (url == null || url.isBlank() || p.url.equals(url.trim())) {
                text.append(p.text).append('\n');
            }
        }
        return PatternExtractor.summary(text.toString());
    }

    // suspicious prices
    public LinkedHashMap<String, ArrayList<String>> dataProblems() {
        LinkedHashMap<String, ArrayList<String>> out = new LinkedHashMap<>();
        for (ModelPrice p : prices) {
            ArrayList<String> probs = InputValidator.problems(p);
            if (!probs.isEmpty()) {
                out.put(p.provider + " " + p.model + " " + p.tier, probs);
            }
        }
        return out;
    }
}
