package tokentally.search;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

// khem
// every search adds one to the count of that word in a hash map
// the map is saved to a file so the history stays after the app closes
// the top list is built with a tree map sorted by count, biggest first
public class SearchFrequency {

    private final HashMap<String, Integer> counts = new HashMap<>();
    private final Path file;

    public SearchFrequency(Path file) {
        this.file = file;
        load();
    }

    public int record(String query) {
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            return 0;
        }
        int n = counts.merge(q, 1, Integer::sum);
        save();
        return n;
    }

    public int count(String query) {
        return counts.getOrDefault(query.trim().toLowerCase(), 0);
    }

    // the most searched words, ties in a to z order
    public ArrayList<Object[]> top(int howMany) {
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

    public int size() {
        return counts.size();
    }

    // one line per word: count then a tab then the word
    private void load() {
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    int tab = line.indexOf('\t');
                    if (tab > 0) {
                        try {
                            counts.put(line.substring(tab + 1), Integer.parseInt(line.substring(0, tab)));
                        } catch (NumberFormatException e) {
                            // skip a broken line instead of failing
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
