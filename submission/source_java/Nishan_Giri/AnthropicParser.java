package tokentally.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tokentally.model.ModelPrice;

// owner: Nishan Giri
// claude price tables
public class AnthropicParser implements PriceParser {

    private static final Pattern NAME = Pattern.compile("Claude (Fable|Mythos|Opus|Sonnet|Haiku) ([0-9]+(?:\\.[0-9]+)?)");
    private static final char RETIRED_ICON = '';

    public ArrayList<ModelPrice> parse(String html, String url) {
        ArrayList<ModelPrice> list = new ArrayList<>();
        HashMap<String, ModelPrice> standard = new HashMap<>();

        for (HtmlTable t : HtmlCleaner.tables(html)) {
            String h = t.heading.toLowerCase();
            String tier;
            if (h.startsWith("model pricing")) {
                tier = "standard";
            } else if (h.startsWith("batch processing")) {
                tier = "batch";
            } else if (h.startsWith("fast mode")) {
                tier = "fast";
            } else {
                continue;
            }

            ModelPrice last = null;
            for (ArrayList<String> row : t.rows) {
                if (row.isEmpty()) {
                    continue;
                }
                String first = row.get(0);

                // long prompt row
                if (first.startsWith("$") && last != null && first.contains("over")) {
                    last.longInput = Prices.first(first);
                    last.longThreshold = Prices.tokens(first.substring(first.indexOf("over") + 4));
                    if (row.size() > 1) {
                        last.longOutput = Prices.first(row.get(1));
                    }
                    if (tier.equals("standard") && row.size() > 4) {
                        last.longCachedInput = Prices.first(row.get(4));
                    }
                    continue;
                }

                if (first.indexOf(RETIRED_ICON) >= 0 || row.size() < 3) {
                    last = null;
                    continue;
                }
                // two models in one cell
                Matcher m = NAME.matcher(first);
                last = null;
                while (m.find()) {
                    String id = "claude-" + m.group(1).toLowerCase() + "-" + m.group(2).replace('.', '-');
                    ModelPrice p = new ModelPrice("anthropic", id, tier);
                    p.input = Prices.first(row.get(1));
                    p.output = Prices.first(row.get(2));
                    if (tier.equals("standard") && row.size() >= 6) {
                        p.cachedInput = Prices.first(row.get(5));
                        p.notes = "cache write 5m " + ModelPrice.money(Prices.first(row.get(3)))
                                + " 1h " + ModelPrice.money(Prices.first(row.get(4)));
                        standard.put(id, p);
                    }
                    if (tier.equals("batch") && standard.containsKey(id)) {
                        // batch halves cached price
                        double cached = standard.get(id).cachedInput;
                        p.cachedInput = cached < 0 ? -1 : cached / 2;
                    }
                    if (first.contains("For prompts") || row.get(1).contains("up to")) {
                        p.notes = (p.notes + " short price is for prompts up to 100k").trim();
                    }
                    p.sourceUrl = url;
                    list.add(p);
                    last = p;
                }
            }
        }
        return list;
    }
}
