package tokentally.parse;

import java.util.ArrayList;
import java.util.HashMap;

import tokentally.model.ModelPrice;

// khem
// xai text table has a context column then short context and long context prices
// a separate table gives the batch discount and another one the grok 4.7 fast prices
public class XaiParser implements PriceParser {

    public ArrayList<ModelPrice> parse(String html, String url) {
        ArrayList<ModelPrice> list = new ArrayList<>();
        HashMap<String, ModelPrice> standard = new HashMap<>();
        ArrayList<HtmlTable> tables = HtmlCleaner.tables(html);

        for (HtmlTable t : tables) {
            if (!t.heading.toLowerCase().startsWith("text api")) {
                continue;
            }
            for (ArrayList<String> row : t.rows) {
                if (row.size() < 5 || !row.get(0).startsWith("grok")) {
                    continue;
                }
                ModelPrice p = new ModelPrice("xai", Prices.firstWord(row.get(0)), "standard");
                p.contextWindow = Prices.tokens(row.get(1));
                p.input = Prices.first(row.get(2));
                p.cachedInput = Prices.first(row.get(3));
                p.output = Prices.first(row.get(4));
                if (row.size() >= 8) {
                    p.longInput = Prices.first(row.get(5));
                    p.longCachedInput = Prices.first(row.get(6));
                    p.longOutput = Prices.first(row.get(7));
                    int at = row.get(0).indexOf('≥');
                    p.longThreshold = at < 0 ? 200000 : Prices.tokens(row.get(0).substring(at + 1));
                    p.notes = "long price for prompts of " + (p.longThreshold / 1000) + "k tokens or more";
                }
                p.sourceUrl = url;
                list.add(p);
                standard.put(p.model, p);
            }
        }

        for (HtmlTable t : tables) {
            // batch: one cell lists the models and the next cell has the discount like "20 %"
            if (t.hasHeader("Batch discount")) {
                for (ArrayList<String> row : t.rows) {
                    if (row.size() < 2 || !row.get(0).startsWith("grok")) {
                        continue;
                    }
                    double off = Prices.tokens(row.get(1)) / 100.0;
                    for (String id : row.get(0).split("\\s+")) {
                        ModelPrice s = standard.get(id);
                        if (s != null && off > 0 && off < 1) {
                            list.add(scaled(s, "batch", 1 - off, "batch api " + Math.round(off * 100) + " percent off"));
                        }
                    }
                }
            }
            // grok 4.7 fast has its own small table with below and above 200k rows
            if (t.heading.toLowerCase().contains("fast pricing")) {
                ModelPrice p = new ModelPrice("xai", "grok-4.7-fast", "fast");
                for (ArrayList<String> row : t.rows) {
                    if (row.size() < 4) {
                        continue;
                    }
                    String label = row.get(0).toLowerCase();
                    if (label.startsWith("below")) {
                        p.input = Prices.first(row.get(1));
                        p.cachedInput = Prices.first(row.get(2));
                        p.output = Prices.first(row.get(3));
                    } else if (label.startsWith("above")) {
                        p.longInput = Prices.first(row.get(1));
                        p.longCachedInput = Prices.first(row.get(2));
                        p.longOutput = Prices.first(row.get(3));
                        p.longThreshold = Prices.tokens(label.substring(5));
                    }
                }
                if (p.hasTokenPrice()) {
                    p.notes = "only in cursor and grok build";
                    p.sourceUrl = url;
                    list.add(p);
                }
            }
        }
        return list;
    }

    private ModelPrice scaled(ModelPrice s, String tier, double f, String note) {
        ModelPrice p = new ModelPrice(s.provider, s.model, tier);
        p.input = s.input * f;
        p.cachedInput = s.cachedInput < 0 ? -1 : s.cachedInput * f;
        p.output = s.output * f;
        p.longInput = s.longInput < 0 ? -1 : s.longInput * f;
        p.longCachedInput = s.longCachedInput < 0 ? -1 : s.longCachedInput * f;
        p.longOutput = s.longOutput < 0 ? -1 : s.longOutput * f;
        p.longThreshold = s.longThreshold;
        p.contextWindow = s.contextWindow;
        p.notes = note;
        p.sourceUrl = s.sourceUrl;
        return p;
    }
}
