package tokentally.text;

import java.util.ArrayList;
import java.util.HashMap;

import tokentally.util.Heap;

// owner: Nishan Giri
// suggests close words
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
        if (word == null) {
            return false;
        }
        return vocabulary.containsKey(word.toLowerCase());
    }

    public ArrayList<Suggestion> suggest(String word, int howMany) {
        if (word == null || word.isBlank()) {
            return new ArrayList<>(); // empty word
        }
        String w = word.toLowerCase();
        Heap<Suggestion> heap = new Heap<>((a, b) -> a.distance != b.distance
                ? Integer.compare(a.distance, b.distance)
                : Integer.compare(b.frequency, a.frequency));

        for (String v : vocabulary.keySet()) {
            // length too different
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
