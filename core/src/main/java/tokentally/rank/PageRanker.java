package tokentally.rank;

import java.util.ArrayList;
import java.util.HashMap;

import tokentally.text.InvertedIndex;
import tokentally.text.Tokenizer;
import tokentally.util.Heap;

// shrijana
// page ranking: a page scores the number of times the search keywords show up in it
// a max heap gives the best pages first. a tie goes to the page with more different keywords
public class PageRanker {

    public static class Result {
        public String url;
        public int score;
        public int keywordsFound;

        Result(String url) {
            this.url = url;
        }
    }

    private final InvertedIndex index;

    public PageRanker(InvertedIndex index) {
        this.index = index;
    }

    public ArrayList<Result> rank(String query, int howMany) {
        if (query == null || query.isBlank() || howMany <= 0) {
            return new ArrayList<>(); // no keywords, no ranking
        }
        HashMap<String, Result> byUrl = new HashMap<>();
        ArrayList<String> seen = new ArrayList<>();
        for (String word : Tokenizer.words(query)) {
            if (seen.contains(word)) {
                continue; // the same keyword twice should not count twice
            }
            seen.add(word);
            for (var e : index.postings(word).entrySet()) {
                Result r = byUrl.computeIfAbsent(e.getKey(), Result::new);
                r.score += e.getValue().size();
                r.keywordsFound++;
            }
        }

        Heap<Result> maxHeap = new Heap<>((a, b) -> {
            if (a.score != b.score) {
                return Integer.compare(b.score, a.score);
            }
            if (a.keywordsFound != b.keywordsFound) {
                return Integer.compare(b.keywordsFound, a.keywordsFound);
            }
            return a.url.compareTo(b.url);
        });
        for (Result r : byUrl.values()) {
            maxHeap.push(r);
        }

        ArrayList<Result> top = new ArrayList<>();
        while (!maxHeap.isEmpty() && top.size() < howMany) {
            top.add(maxHeap.pop());
        }
        return top;
    }
}
