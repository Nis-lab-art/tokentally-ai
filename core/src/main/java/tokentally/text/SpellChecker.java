package tokentally.text;

import java.util.ArrayList;
import java.util.HashMap;

import tokentally.util.Heap;

// nishan
// the vocabulary is every word from the crawled pages with how often it shows up
// a word that is not in it gets the closest words by edit distance
// a heap keeps the best ones: smaller distance first, then the more common word
public class SpellChecker {

    public static class Suggestion {
        public String word;
        public int distance;
        public int frequency;

        Suggestion(String word, int distance, int frequency) {
            this.word = word;
            this.distance = distance;
            this.frequency = frequency;
        }
    }

    private final HashMap<String, Integer> vocabulary;
    private final int maxDistance;

    public SpellChecker(HashMap<String, Integer> vocabulary, int maxDistance) {
        this.vocabulary = vocabulary;
        this.maxDistance = maxDistance;
    }

    public boolean isKnown(String word) {
        return vocabulary.containsKey(word.toLowerCase());
    }

    public ArrayList<Suggestion> suggest(String word, int howMany) {
        String w = word.toLowerCase();
        Heap<Suggestion> heap = new Heap<>((a, b) -> a.distance != b.distance
                ? Integer.compare(a.distance, b.distance)
                : Integer.compare(b.frequency, a.frequency));

        for (String v : vocabulary.keySet()) {
            // words with a very different length can not be close, skip the dp table for them
            if (Math.abs(v.length() - w.length()) > maxDistance) {
                continue;
            }
            int d = EditDistance.between(w, v);
            if (d <= maxDistance && d > 0) {
                heap.push(new Suggestion(v, d, vocabulary.get(v)));
            }
        }

        ArrayList<Suggestion> best = new ArrayList<>();
        while (!heap.isEmpty() && best.size() < howMany) {
            best.add(heap.pop());
        }
        return best;
    }
}
