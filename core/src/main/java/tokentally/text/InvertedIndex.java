package tokentally.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import tokentally.model.Page;

// owner: Abhinav Pandey
// word -> page -> positions
public class InvertedIndex {

    private final HashMap<String, HashMap<String, ArrayList<Integer>>> index = new HashMap<>();
    private final HashMap<String, Integer> totals = new HashMap<>();
    private final HashMap<String, Integer> pageLengths = new HashMap<>();

    public void add(Page page) {
        if (page == null || page.text == null || page.url == null) {
            return; // empty page
        }
        ArrayList<String> words = Tokenizer.words(page.text);
        pageLengths.put(page.url, words.size());
        for (int pos = 0; pos < words.size(); pos++) {
            String w = words.get(pos);
            if (!Tokenizer.hasLetter(w)) {
                continue;
            }
            index.computeIfAbsent(w, k -> new HashMap<>())
                 .computeIfAbsent(page.url, k -> new ArrayList<>())
                 .add(pos);
            totals.merge(w, 1, Integer::sum);
        }
    }

    // pages and positions
    public HashMap<String, ArrayList<Integer>> postings(String word) {
        if (word == null) {
            return new HashMap<>();
        }
        HashMap<String, ArrayList<Integer>> p = index.get(word.toLowerCase());
        return p == null ? new HashMap<>() : p;
    }

    public boolean contains(String word) {
        return index.containsKey(word.toLowerCase());
    }

    // count on one page
    public int count(String word, String url) {
        ArrayList<Integer> pos = postings(word).get(url);
        return pos == null ? 0 : pos.size();
    }

    // phrase search
    public HashMap<String, Integer> phrase(String phrase) {
        HashMap<String, Integer> hits = new HashMap<>();
        ArrayList<String> words = Tokenizer.words(phrase);
        if (words.isEmpty()) {
            return hits;
        }
        HashMap<String, ArrayList<Integer>> first = postings(words.get(0));
        for (String url : first.keySet()) {
            int found = 0;
            for (int start : first.get(url)) {
                boolean all = true;
                for (int k = 1; k < words.size() && all; k++) {
                    ArrayList<Integer> next = postings(words.get(k)).get(url);
                    all = next != null && next.contains(start + k);
                }
                if (all) {
                    found++;
                }
            }
            if (found > 0) {
                hits.put(url, found);
            }
        }
        return hits;
    }

    public Set<String> words() {
        return index.keySet();
    }

    // total count
    public int total(String word) {
        return totals.getOrDefault(word.toLowerCase(), 0);
    }

    public HashMap<String, Integer> totals() {
        return totals;
    }

    public int pageCount() {
        return pageLengths.size();
    }
}
