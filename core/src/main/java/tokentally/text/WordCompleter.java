package tokentally.text;

import java.util.ArrayList;

import tokentally.util.Heap;

// owner: Aakriti Mainali
// word completion
public class WordCompleter {

    private final Trie trie;

    public WordCompleter(Trie trie) {
        this.trie = trie;
    }

    public ArrayList<String> complete(String prefix, int howMany) {
        ArrayList<String> out = new ArrayList<>();
        if (prefix == null || prefix.isBlank()) {
            return out;
        }
        Heap<Object[]> heap = new Heap<>((a, b) -> {
            int c = Integer.compare((int) b[1], (int) a[1]);
            return c != 0 ? c : ((String) a[0]).compareTo((String) b[0]);
        });
        for (Object[] w : trie.wordsWithPrefix(prefix.trim())) {
            heap.push(w);
        }
        while (!heap.isEmpty() && out.size() < howMany) {
            out.add((String) heap.pop()[0]);
        }
        return out;
    }
}
