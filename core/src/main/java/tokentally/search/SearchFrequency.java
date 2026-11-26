package tokentally.search;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

// owner: Khem Raj Adhikari
// counts searches
public class SearchFrequency {

    private final HashMap<String, Integer> counts = new HashMap<>();
    private final Path file;

    public SearchFrequency(Path file) {
        this.file = file;
        load();
    }

    // safe with two users
    public synchronized int record(String query) {
        if (query == null) {
            return 0;
        }
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            return 0;
        }
        int n = counts.merge(q, 1, Integer::sum);
        save();
        return n;
    }

    public synchronized int count(String query) {
        if (query == null) {
            return 0;
        }
        return counts.getOrDefault(query.trim().toLowerCase(), 0);
    }

    // top searches
    public synchronized ArrayList<Object[]> top(int howMany) {
        TreeMap<Integer, TreeSet<String>> byCount = new TreeMap<>((a, b) -> b - a);
        for (var e : counts.entrySet()) {
            byCount.computeIfAbsent(e.getValue(), k -> new TreeSet<>()).add(e.getKey());
        }
        ArrayList<Object[]> out = new ArrayList<>();
        for (var e : byCount.entrySet()) {
            for (String word : e.getValue()) {
                if (out.size() == howMany) {
                    return out;
                }
                out.add(new Object[] {word, e.getKey()});
            }
        }
        return out;
    }

    public synchronized int size() {
        return counts.size();
    }

    // count TAB word
    private void load() {
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    int tab = line.indexOf('\t');
                    if (tab > 0) {
                        try {
                            counts.put(line.substring(tab + 1), Integer.parseInt(line.substring(0, tab)));
                        } catch (NumberFormatException e) {
                            // skip bad line
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("could not read search history, starting empty");
        }
    }

    private void save() {
        StringBuilder out = new StringBuilder();
        for (var e : counts.entrySet()) {
            out.append(e.getValue()).append('\t').append(e.getKey()).append('\n');
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("could not save search history: " + e.getMessage());
        }
    }
}
