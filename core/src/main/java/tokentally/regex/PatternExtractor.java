package tokentally.regex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// owner: Aakriti Mainali
// regex pattern finder
public class PatternExtractor {

    // prices
    public static final Pattern PRICE = Pattern.compile("\\$\\s?\\d+(?:,\\d{3})*(?:\\.\\d+)?(?:\\s*(?:USD\\s*)?/\\s*(?:MTok|1M|M tokens|million tokens))?");
    // context sizes
    public static final Pattern CONTEXT = Pattern.compile("(?i)\\b(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d+)?\\s?[KM])\\s*(?:tokens?|context)\\b");
    // dated prices
    public static final Pattern DATED = Pattern.compile("(?i)\\b(through|until|starting|from|after|before)\\s+(january|february|march|april|may|june|july|august|september|october|november|december)\\s+\\d{1,2},\\s*\\d{4}");
    // long context limits
    public static final Pattern LONG_CONTEXT = Pattern.compile("(?i)(≥|>=|>|over|above)\\s*(\\d{1,3}(?:,\\d{3})+|\\d+\\s?[kKmM])\\s*(?:tokens)?");
    public static final Pattern MODEL_ID = Pattern.compile("\\b(?:gpt|claude|gemini|glm|grok|o\\d)[-.]?[a-z0-9]+(?:[-.][a-z0-9]+)*\\b");
    public static final Pattern SPECIAL = Pattern.compile("(?i)\\b(limited-time free|free of charge|free)\\b");
    // label then value
    private static final String SIZE_VALUE = "(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d+)?\\s?[KM]\\b|\\d{4,})";
    public static final Pattern CONTEXT_WINDOW = Pattern.compile("(?is)context (?:window|length)[^0-9$]{0,80}?" + SIZE_VALUE);
    public static final Pattern MAX_OUTPUT = Pattern.compile("(?is)max(?:imum)? output(?: tokens)?[^0-9$]{0,80}?" + SIZE_VALUE);
    // value then label
    public static final Pattern CONTEXT_WINDOW_FIRST = Pattern.compile("(?i)" + SIZE_VALUE + "[\\s-]*(?:tokens?)?[\\s-]*context (?:window|length)");
    public static final Pattern MAX_OUTPUT_FIRST = Pattern.compile("(?i)" + SIZE_VALUE + "[\\s-]*max(?:imum)? output");

    public static ArrayList<String> findAll(Pattern p, String text) {
        ArrayList<String> out = new ArrayList<>();
        Matcher m = p.matcher(text);
        while (m.find()) {
            out.add(m.group().trim());
        }
        return out;
    }

    // count all patterns
    public static LinkedHashMap<String, LinkedHashMap<String, Integer>> summary(String text) {
        if (text == null) {
            text = "";
        }
        LinkedHashMap<String, LinkedHashMap<String, Integer>> out = new LinkedHashMap<>();
        String lower = text.toLowerCase();
        put(out, "prices", findAll(PRICE, text));
        put(out, "context sizes", findAll(CONTEXT, text));
        put(out, "dated prices", findAll(DATED, text));
        put(out, "long context thresholds", findAll(LONG_CONTEXT, text));
        put(out, "model ids", findAll(MODEL_ID, lower));
        put(out, "free offers", findAll(SPECIAL, lower));
        return out;
    }

    private static void put(LinkedHashMap<String, LinkedHashMap<String, Integer>> out, String name, ArrayList<String> found) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        for (String f : found) {
            counts.merge(f, 1, Integer::sum);
        }
        out.put(name, counts);
    }

    // first size, -1 if none
    public static long firstSize(Pattern labelled, String text) {
        Matcher m = labelled.matcher(text);
        return m.find() ? toTokens(m.group(1)) : -1;
    }

    static long toTokens(String s) {
        String t = s.replace(",", "").replace(" ", "").toUpperCase();
        double mult = 1;
        if (t.endsWith("K")) {
            mult = 1_000;
            t = t.substring(0, t.length() - 1);
        } else if (t.endsWith("M")) {
            mult = 1_000_000;
            t = t.substring(0, t.length() - 1);
        }
        return Math.round(Double.parseDouble(t) * mult);
    }
}
