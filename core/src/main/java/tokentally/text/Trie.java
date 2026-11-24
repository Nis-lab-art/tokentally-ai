package tokentally.text;

import java.util.ArrayList;
import java.util.TreeMap;

// aakriti
// trie of words, each node has its children by char and the count of the word ending there
public class Trie {

    static class Node {
        TreeMap<Character, Node> children = new TreeMap<>();
        int count; // 0 when no word ends here
    }

    private final Node root = new Node();
    private int size;

    // add the word, or add to its count if it is already there
    public void insert(String word, int times) {
        Node cur = root;
        for (char c : word.toLowerCase().toCharArray()) {
            cur = cur.children.computeIfAbsent(c, k -> new Node());
        }
        if (cur.count == 0) {
            size++;
        }
        cur.count += times;
    }

    public int count(String word) {
        Node n = find(word.toLowerCase());
        return n == null ? 0 : n.count;
    }

    public int size() {
        return size;
    }

    // walk down the trie to the node of the prefix
    private Node find(String prefix) {
        Node cur = root;
        for (char c : prefix.toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) {
                return null;
            }
        }
        return cur;
    }

    // every word under the prefix with its count
    public ArrayList<Object[]> wordsWithPrefix(String prefix) {
        ArrayList<Object[]> out = new ArrayList<>();
        String p = prefix.toLowerCase();
        Node start = find(p);
        if (start != null) {
            collect(start, new StringBuilder(p), out);
        }
        return out;
    }

    private void collect(Node node, StringBuilder path, ArrayList<Object[]> out) {
        if (node.count > 0) {
            out.add(new Object[] {path.toString(), node.count});
        }
        for (var e : node.children.entrySet()) {
            path.append(e.getKey());
            collect(e.getValue(), path, out);
            path.setLength(path.length() - 1);
        }
    }
}
