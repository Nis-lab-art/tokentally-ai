package tokentally.crawl;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

// reads robots.txt once per site and checks a path against the rules for user-agent *
public class RobotsTxtChecker {

    private final Fetcher fetcher;
    private final HashMap<String, ArrayList<String[]>> rulesBySite = new HashMap<>();

    public RobotsTxtChecker(Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    public boolean allowed(String url) {
        URI u = URI.create(url);
        String site = u.getScheme() + "://" + u.getHost();
        ArrayList<String[]> rules = rulesBySite.get(site);
        if (rules == null) {
            rules = parse(fetcher.getQuietly(site + "/robots.txt"));
            rulesBySite.put(site, rules);
        }
        String path = u.getPath().isEmpty() ? "/" : u.getPath();
        return allowedByRules(rules, path);
    }

    // keep only the allow and disallow lines of the user-agent * group
    public static ArrayList<String[]> parse(String robots) {
        ArrayList<String[]> rules = new ArrayList<>();
        if (robots == null) {
            return rules;
        }
        boolean forUs = false;
        boolean lastWasAgent = false;
        for (String raw : robots.split("\n")) {
            String line = raw.split("#")[0].trim();
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).trim().toLowerCase();
            String value = line.substring(colon + 1).trim();
            if (key.equals("user-agent")) {
                // agents listed one after another share a group
                boolean star = value.equals("*");
                forUs = lastWasAgent ? (forUs || star) : star;
                lastWasAgent = true;
            } else {
                lastWasAgent = false;
                if (forUs && (key.equals("allow") || key.equals("disallow")) && !value.isEmpty()) {
                    rules.add(new String[] {key, value});
                }
            }
        }
        return rules;
    }

    // the longest matching rule wins and allow wins a tie
    public static boolean allowedByRules(ArrayList<String[]> rules, String path) {
        int bestLength = -1;
        boolean result = true;
        for (String[] rule : rules) {
            String prefix = rule[1].replace("*", "");
            if (rule[1].equals("*") || path.startsWith(prefix)) {
                int length = rule[1].equals("*") ? 0 : prefix.length();
                boolean allow = rule[0].equals("allow");
                if (length > bestLength || (length == bestLength && allow)) {
                    bestLength = length;
                    result = allow;
                }
            }
        }
        return result;
    }
}
