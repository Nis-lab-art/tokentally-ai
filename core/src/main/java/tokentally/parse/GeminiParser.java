package tokentally.parse;

import java.util.ArrayList;

import tokentally.model.ModelPrice;

// owner: Shrijana Karki
// gemini prices
public class GeminiParser implements PriceParser {

    private String model;
    private String tier;
    private ModelPrice current;
    private ArrayList<ModelPrice> list;
    private String url;

    public ArrayList<ModelPrice> parse(String html, String url) {
        this.url = url;
        list = new ArrayList<>();
        model = null;
        tier = "standard";
        current = null;

        ArrayList<String> cells = new ArrayList<>();
        boolean inTable = false;

        for (String raw : HtmlCleaner.decode(html).split("\n")) {
            String line = raw.trim();

            if (line.startsWith("## ")) {
                finishTier();
                model = idFromHeading(line);
                tier = "standard";
            } else if (line.startsWith("*[`") && model != null) {
                // real model id
                int a = line.indexOf('`') + 1;
                int b = line.indexOf('`', a);
                if (b > a) {
                    model = line.substring(a, b);
                }
            } else if (line.startsWith("{% tab title=")) {
                finishTier();
                int a = line.indexOf('"') + 1;
                tier = line.substring(a, line.indexOf('"', a)).toLowerCase();
            } else if (line.startsWith("{% table")) {
                inTable = true;
                cells.clear();
            } else if (line.startsWith("{% /table")) {
                useRow(cells);
                inTable = false;
            } else if (inTable) {
                if (line.equals("---")) {
                    useRow(cells);
                    cells.clear();
                } else if (line.startsWith("* ")) {
                    cells.add(line.substring(2));
                } else if (!line.isEmpty() && !cells.isEmpty()) {
                    // multi line cell
                    int lastCell = cells.size() - 1;
                    cells.set(lastCell, cells.get(lastCell) + " " + line);
                }
            }
        }
        finishTier();
        return list;
    }

    // heading -> model id
    private String idFromHeading(String line) {
        int at = line.indexOf("id=\"");
        if (at >= 0) {
            at += 4;
            return line.substring(at, line.indexOf('"', at));
        }
        return line.substring(3).trim().toLowerCase().replace(' ', '-');
    }

    // use paid price
    private void useRow(ArrayList<String> cells) {
        if (model == null || cells.size() < 3) {
            return;
        }
        String feature = cells.get(0).toLowerCase();
        String paid = cells.get(cells.size() - 1);
        if (paid.toLowerCase().startsWith("not available")) {
            return;
        }
        if (current == null) {
            current = new ModelPrice("gemini", model, tier);
            current.sourceUrl = url;
        }
        ArrayList<Double> amounts = Prices.all(paid);
        if (amounts.isEmpty()) {
            if (paid.toLowerCase().contains("free of charge") && feature.startsWith("input")) {
                current.input = 0;
            }
            return;
        }
        // short and long price
        boolean hasLong = paid.contains("> 200k") || paid.contains(">200k");
        double shortPrice = amounts.get(0);
        double longPrice = hasLong && amounts.size() > 1 ? amounts.get(1) : -1;

        if (feature.startsWith("input price")) {
            current.input = shortPrice;
            current.longInput = longPrice;
        } else if (feature.startsWith("output price")) {
            current.output = shortPrice;
            current.longOutput = longPrice;
        } else if (feature.startsWith("context caching price")) {
            current.cachedInput = shortPrice;
            current.longCachedInput = longPrice;
        }
        if (hasLong) {
            current.longThreshold = 200000;
        }
    }

    private void finishTier() {
        if (current != null && (current.input >= 0 || current.output >= 0)) {
            if (current.longThreshold > 0) {
                current.notes = "long price for prompts over 200k tokens";
            }
            list.add(current);
        }
        current = null;
    }
}
