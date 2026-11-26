package tokentally.text;

import java.util.ArrayList;
import java.util.TreeMap;

// owner: Aakriti Mainali
// trie of words
public class Trie {

    static class Node {
        TreeMap<Character, Node> children = new TreeMap<>();
        int count; // 0 = no word
    }

    private final Node root = new Node();
    private int size;

    // add word
    public void insert(String word, int times) {
        if (word == null || word.isEmpty() || times <= 0) {
            return; // ignore
        }
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
        if (word == null || word.isEmpty()) {
            return 0;
        }
        Node n = find(word.toLowerCase());
        return n == null ? 0 : n.count;
    }

    public int size() {
        return size;
    }

    // find prefix node
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

    // words under prefix
    public ArrayList<Object[]> wordsWithPrefix(String prefix) {
        ArrayList<Object[]> out = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) {
            return out;
        }
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
