package tokentally.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import tokentally.model.ModelPrice;
import tokentally.model.Page;
import tokentally.parse.HtmlCleaner;

// saves crawled data to the data folder so the app also works without internet
// data/raw/<provider>.html   the pricing page (without scripts) for the parser
// data/pages/<provider>.txt  clean text of every crawled page
// data/prices.csv            all model prices in one format
public class DataStore {

    public static final String[] CSV_HEADER = {"provider", "model", "tier", "input", "cached_input", "output",
        "long_input", "long_cached_input", "long_output", "long_threshold", "context_window", "max_output", "notes", "url"};

    private final Path dir;
    public int badRows; // rows of prices.csv that could not be read the last time it was loaded

    public DataStore(String folder) {
        this.dir = Path.of(folder);
    }

    public boolean hasData() {
        return Files.exists(dir.resolve("prices.csv")) && Files.exists(dir.resolve("pages"));
    }

    public void saveRaw(String provider, String html) throws IOException {
        Files.createDirectories(dir.resolve("raw"));
        String small = html;
        for (String tag : new String[] {"script", "style", "svg"}) {
            small = HtmlCleaner.removeBlocks(small, tag);
        }
        Files.writeString(dir.resolve("raw").resolve(provider + ".html"), small, StandardCharsets.UTF_8);
    }

    public String loadRaw(String provider) throws IOException {
        Path p = dir.resolve("raw").resolve(provider + ".html");
        return Files.exists(p) ? Files.readString(p, StandardCharsets.UTF_8) : null;
    }

    public void savePages(String provider, ArrayList<Page> pages) throws IOException {
        Files.createDirectories(dir.resolve("pages"));
        StringBuilder out = new StringBuilder();
        for (Page p : pages) {
            out.append("@@url ").append(p.url).append('\n');
            out.append("@@title ").append(p.title).append('\n');
            out.append(p.text).append('\n');
        }
        Files.writeString(dir.resolve("pages").resolve(provider + ".txt"), out.toString(), StandardCharsets.UTF_8);
    }

    public ArrayList<Page> loadPages() throws IOException {
        ArrayList<Page> pages = new ArrayList<>();
        Path folder = dir.resolve("pages");
        if (!Files.exists(folder)) {
            return pages;
        }
        try (var files = Files.list(folder)) {
            for (Path f : files.sorted().toList()) {
                String name = f.getFileName().toString();
                if (!name.endsWith(".txt")) {
                    continue;
                }
                String provider = name.substring(0, name.length() - 4);
                Page current = null;
                StringBuilder text = new StringBuilder();
                for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    if (line.startsWith("@@url ")) {
                        if (current != null) {
                            current.text = text.toString().trim();
                            pages.add(current);
                        }
                        current = new Page(provider, line.substring(6).trim(), "", "");
                        text.setLength(0);
                    } else if (line.startsWith("@@title ") && current != null) {
                        current.title = line.substring(8).trim();
                    } else {
                        text.append(line).append('\n');
                    }
                }
                if (current != null) {
                    current.text = text.toString().trim();
                    pages.add(current);
                }
            }
        }
        return pages;
    }

    public void savePrices(ArrayList<ModelPrice> prices) throws IOException {
        Files.createDirectories(dir);
        StringBuilder out = new StringBuilder(String.join(",", CSV_HEADER)).append('\n');
        for (ModelPrice p : prices) {
            String[] v = {p.provider, p.model, p.tier, num(p.input), num(p.cachedInput), num(p.output),
                num(p.longInput), num(p.longCachedInput), num(p.longOutput), num(p.longThreshold),
                num(p.contextWindow), num(p.maxOutput), p.notes, p.sourceUrl};
            for (int i = 0; i < v.length; i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(quote(v[i]));
            }
            out.append('\n');
        }
        Files.writeString(dir.resolve("prices.csv"), out.toString(), StandardCharsets.UTF_8);
    }

    public ArrayList<ModelPrice> loadPrices() throws IOException {
        ArrayList<ModelPrice> list = new ArrayList<>();
        badRows = 0;
        Path f = dir.resolve("prices.csv");
        if (!Files.exists(f)) {
            return list;
        }
        boolean header = true;
        for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
            if (header) {
                header = false;
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            String[] c = splitCsv(line);
            if (c.length < CSV_HEADER.length) {
                badRows++;
                continue;
            }
            ModelPrice p;
            try {
                p = new ModelPrice(c[0], c[1], c[2]);
                p.input = dbl(c[3]);
                p.cachedInput = dbl(c[4]);
                p.output = dbl(c[5]);
                p.longInput = dbl(c[6]);
                p.longCachedInput = dbl(c[7]);
                p.longOutput = dbl(c[8]);
                p.longThreshold = (long) dbl(c[9]);
                p.contextWindow = (long) dbl(c[10]);
                p.maxOutput = (long) dbl(c[11]);
                p.notes = c[12];
                p.sourceUrl = c[13];
            } catch (NumberFormatException e) {
                badRows++; // a damaged row is skipped, the rest still loads
                continue;
            }
            list.add(p);
        }
        return list;
    }

    public Path folder() {
        return dir;
    }

    private static String num(double v) {
        if (v < 0) {
            return "";
        }
        if (v == Math.rint(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(Math.round(v * 1000000) / 1000000.0);
    }

    private static double dbl(String s) {
        return s.isEmpty() ? -1 : Double.parseDouble(s);
    }

    private static String quote(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // csv split where commas inside quotes stay in the value
    public static String[] splitCsv(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
