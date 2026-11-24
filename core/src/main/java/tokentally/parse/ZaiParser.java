package tokentally.parse;

import java.util.ArrayList;
import java.util.HashSet;

import tokentally.model.ModelPrice;

// aakriti
// z.ai has plain tables for text and vision models: Model Input Cached Input Cached Input Storage Output
// free models say Free and a missing cached price is "-" or "\"
public class ZaiParser implements PriceParser {

    public ArrayList<ModelPrice> parse(String html, String url) {
        ArrayList<ModelPrice> list = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        for (HtmlTable t : HtmlCleaner.tables(html)) {
            int cModel = t.column("Model");
            int cIn = t.column("Input");
            int cOut = t.column("Output");
            int cCached = t.column("Cached Input");
            int cStorage = t.column("Cached Input Storage");
            if (cModel < 0 || cIn < 0 || cOut < 0) {
                continue;
            }
            for (ArrayList<String> row : t.rows) {
                if (row.size() <= cOut || row.get(cIn).equalsIgnoreCase("Input")) {
                    continue;
                }
                String id = Prices.firstWord(row.get(cModel));
                // the same model can show up in two tables, keep the first one
                if (!seen.add(id)) {
                    continue;
                }
                ModelPrice p = new ModelPrice("zai", id, "standard");
                p.input = Prices.first(row.get(cIn));
                p.output = Prices.first(row.get(cOut));
                if (cCached >= 0) {
                    p.cachedInput = Prices.first(row.get(cCached));
                }
                String type = t.heading.toLowerCase();
                p.notes = type.contains("vision") ? "vision model" : "text model";
                if (cStorage >= 0 && row.get(cStorage).toLowerCase().contains("limited-time free")) {
                    p.notes += " | cached storage limited-time free";
                }
                if (p.isFree()) {
                    p.notes += " | free";
                }
                p.sourceUrl = url;
                list.add(p);
            }
        }
        return list;
    }
}
