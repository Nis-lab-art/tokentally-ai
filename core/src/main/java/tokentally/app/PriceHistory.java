package tokentally.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;

import tokentally.model.ModelPrice;

// keeps a dated log of prices so we can see when a price changed
// data/price_history.csv has one line each time a model price is new or different
// the log only grows, nothing is rewritten
public class PriceHistory {

    private static final String HEADER = "date,provider,model,tier,input,cached_input,output";

    public static class Entry {
        public String date;
        public String provider;
        public String model;
        public String tier;
        public double input;
        public double cachedInput;
        public double output;

        public String key() {
            return provider + "|" + model + "|" + tier;
        }

        boolean samePrice(double in, double cached, double out) {
            return Math.abs(input - in) < 1e-9 && Math.abs(cachedInput - cached) < 1e-9 && Math.abs(output - out) < 1e-9;
        }
    }

    // one price that moved between two log lines
    public static class Change {
        public Entry before;
        public Entry after;
        public double inputPercent;   // change of the input price in percent, 0 when unknown
        public double outputPercent;
    }

    private final Path file;

    public PriceHistory(Path folder) {
        this.file = folder.resolve("price_history.csv");
    }

    public ArrayList<Entry> load() throws IOException {
        ArrayList<Entry> list = new ArrayList<>();
        if (!Files.exists(file)) {
            return list;
        }
        boolean header = true;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (header) {
                header = false;
                continue;
            }
            String[] c = line.split(",", -1);
            if (c.length < 7) {
                continue; // broken line
            }
            try {
                Entry e = new Entry();
                e.date = c[0];
                e.provider = c[1];
                e.model = c[2];
                e.tier = c[3];
                e.input = dbl(c[4]);
                e.cachedInput = dbl(c[5]);
                e.output = dbl(c[6]);
                list.add(e);
            } catch (NumberFormatException ex) {
                // skip lines we cannot read
            }
        }
        return list;
    }

    // adds a line for every price that is new or different from the last one we know
    // returns how many lines were added
    public int record(String date, ArrayList<ModelPrice> prices) throws IOException {
        HashMap<String, Entry> last = new HashMap<>();
        for (Entry e : load()) {
            last.put(e.key(), e); // later lines replace earlier ones
        }
        StringBuilder out = new StringBuilder();
        int added = 0;
        for (ModelPrice p : prices) {
            Entry old = last.get(p.key());
            if (old != null && old.samePrice(p.input, p.cachedInput, p.output)) {
                continue;
            }
            out.append(date).append(',').append(p.provider).append(',').append(p.model).append(',').append(p.tier)
                .append(',').append(num(p.input)).append(',').append(num(p.cachedInput)).append(',').append(num(p.output)).append('\n');
            added++;
        }
        if (added == 0) {
            return 0;
        }
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, HEADER + "\n", StandardCharsets.UTF_8);
        }
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        return added;
    }

    // every time a known price moved, newest first
    public ArrayList<Change> changes() throws IOException {
        ArrayList<Change> out = new ArrayList<>();
        HashMap<String, Entry> last = new HashMap<>();
        for (Entry e : load()) {
            Entry old = last.put(e.key(), e);
            if (old != null) {
                Change c = new Change();
                c.before = old;
                c.after = e;
                c.inputPercent = percent(old.input, e.input);
                c.outputPercent = percent(old.output, e.output);
                out.add(0, c);
            }
        }
        return out;
    }

    // prices that showed up after the first day of the log, so new models or new tiers, newest first
    public ArrayList<Entry> added() throws IOException {
        ArrayList<Entry> out = new ArrayList<>();
        HashMap<String, Entry> seen = new HashMap<>();
        String firstDay = null;
        for (Entry e : load()) {
            if (firstDay == null) {
                firstDay = e.date;
            }
            if (seen.put(e.key(), e) == null && !e.date.equals(firstDay)) {
                out.add(0, e);
            }
        }
        return out;
    }

    // the log lines of one model in the order they were written
    public ArrayList<Entry> of(String model) throws IOException {
        ArrayList<Entry> out = new ArrayList<>();
        for (Entry e : load()) {
            if (e.model.equals(model)) {
                out.add(e);
            }
        }
        return out;
    }

    static double percent(double before, double after) {
        if (before <= 0 || after < 0) {
            return 0;
        }
        return Math.round((after - before) / before * 1000) / 10.0;
    }

    private static String num(double v) {
        if (v < 0) {
            return "";
        }
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static double dbl(String s) {
        return s.isEmpty() ? -1 : Double.parseDouble(s);
    }
}
