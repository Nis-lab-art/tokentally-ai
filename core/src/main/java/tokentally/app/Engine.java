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

// loads the data and builds every feature on it
// the command line menu and the web api both use this one class
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

    // use the saved data, and crawl only when there is none yet
    public void load() throws Exception {
        if (!store.hasData()) {
            CrawlAll.run(store, 20);
        }
        prices = store.loadPrices();
        pages = store.loadPages();
        history.record(today(), prices);
        build();
    }

    // fetch the live pages again and rebuild everything
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

        // vocabulary: every word with its count, model ids get counted too
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

    // context window and max output come from each model page, found with regex
    private void fillSizesFromModelPages() {
        for (ModelPrice p : prices) {
            for (Page page : pages) {
                if (!page.provider.equals(p.provider) || !pageIsAbout(page.url, p.model)) {
                    continue;
                }
                // try "1M context window" first and then "context window 1M"
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

    // .../models/gpt-6.2-astra or .../models/opus-5-5/overview belongs to that model
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
        public String searchedInstead = ""; // the suggestion used when the typed words found nothing
    }

    public SearchResult search(String query) {
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

        // nothing found but every unknown word has a close match, so search with the matches
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

    // every tier of one model
    public ArrayList<ModelPrice> card(String model) {
        ArrayList<ModelPrice> out = new ArrayList<>();
        for (ModelPrice p : prices) {
            if (p.model.equalsIgnoreCase(model.trim())) {
                out.add(p);
            }
        }
        return out;
    }

    public ArrayList<ModelPrice> compare(String[] models) {
        ArrayList<ModelPrice> out = new ArrayList<>();
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

    // scheduled price changes and limited time offers found in the notes
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

    // crawled prices that look wrong
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
