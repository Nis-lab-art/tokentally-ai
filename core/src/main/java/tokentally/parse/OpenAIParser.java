package tokentally.parse;

import java.util.ArrayList;
import java.util.HashMap;

import tokentally.model.ModelPrice;

// owner: Abhinav Pandey
// openai price tables
public class OpenAIParser implements PriceParser {

    public ArrayList<ModelPrice> parse(String html, String url) {
        ArrayList<ModelPrice> list = new ArrayList<>();
        HashMap<String, ModelPrice> byKey = new HashMap<>();
        for (HtmlTable t : HtmlCleaner.tables(html)) {
            // text token tables only
            if (!t.hasHeader("Model") || !t.hasHeader("Input") || !t.hasHeader("Output")
                    || t.hasHeader("Modality") || t.hasHeader("Training") || t.hasHeader("Use case")) {
                continue;
            }
            int cModel = t.column("Model");
            int cIn = t.column("Input");
            int cOut = t.column("Output");
            int cCached = t.column("Cached input");
            int cWrite = t.column("30m write");
            String tier = t.tab.isEmpty() ? "standard" : t.tab;

            for (ArrayList<String> row : t.rows) {
                if (row.size() <= cOut || row.get(cIn).equalsIgnoreCase("Input")) {
                    continue;
                }
                double in = Prices.first(row.get(cIn));
                double out = Prices.first(row.get(cOut));
                if (in < 0 && out < 0) {
                    continue;
                }
                String id = Prices.firstWord(row.get(cModel));
                if (!id.matches("[a-z][a-z0-9.\\-]*")) {
                    continue;
                }
                // later price, keep first
                ModelPrice old = byKey.get("openai|" + id + "|" + tier);
                if (old != null) {
                    if (!old.notes.contains("price change")) {
                        old.notes = (old.notes + " | scheduled price change, later input " + ModelPrice.money(in)).trim();
                    }
                    continue;
                }
                ModelPrice p = new ModelPrice("openai", id, tier);
                byKey.put(p.key(), p);
                p.input = in;
                p.output = out;
                if (cCached >= 0 && cCached < row.size()) {
                    p.cachedInput = Prices.first(row.get(cCached));
                }
                if (cWrite >= 0 && cWrite < row.size() && Prices.first(row.get(cWrite)) >= 0) {
                    p.notes = "cache write 30m " + ModelPrice.money(Prices.first(row.get(cWrite)));
                }
                if (row.get(cModel).contains("Deprecated")) {
                    p.notes = (p.notes + " deprecated").trim();
                }
                p.sourceUrl = url;
                list.add(p);
            }
        }
        return list;
    }
}
