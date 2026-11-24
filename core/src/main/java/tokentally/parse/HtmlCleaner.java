package tokentally.parse;

import java.util.ArrayList;

// shared html cleaning used by every parser
// removes scripts styles and the page chrome, turns tags into text and reads tables
public class HtmlCleaner {

    private static final String[] DROP = {"script", "style", "svg", "noscript", "nav", "header", "footer", "aside", "head"};

    // the readable text of a page with one block per line
    public static String text(String html) {
        String s = html;
        for (String tag : DROP) {
            s = removeBlocks(s, tag);
        }
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</(p|div|li|tr|h[1-6]|table|section|pre)>", "\n");
        s = s.replaceAll("(?i)</t[dh]>", " ");
        s = s.replaceAll("<[^>]*>", " ");
        s = decode(s);
        return tidy(s);
    }

    // text inside the title tag
    public static String title(String html) {
        int a = html.indexOf("<title");
        if (a < 0) {
            return "";
        }
        a = html.indexOf('>', a) + 1;
        int b = html.indexOf("</title>", a);
        return b < 0 ? "" : decode(html.substring(a, b)).trim();
    }

    // all tables with the last heading before them and the tab pane they are in
    public static ArrayList<HtmlTable> tables(String html) {
        ArrayList<HtmlTable> list = new ArrayList<>();
        int from = 0;
        while (true) {
            int start = html.indexOf("<table", from);
            if (start < 0) {
                break;
            }
            int end = html.indexOf("</table>", start);
            if (end < 0) {
                break;
            }
            String body = html.substring(start, end);
            HtmlTable t = new HtmlTable();
            t.heading = lastHeading(html, start);
            t.tab = lastTab(html, start);

            int r = 0;
            while ((r = body.indexOf("<tr", r)) >= 0) {
                int rEnd = body.indexOf("</tr>", r);
                if (rEnd < 0) {
                    break;
                }
                t.rows.add(cells(body.substring(r, rEnd)));
                r = rEnd;
            }
            list.add(t);
            from = end + 8;
        }
        return list;
    }

    // text of every th and td in one row
    private static ArrayList<String> cells(String row) {
        ArrayList<String> out = new ArrayList<>();
        int i = 0;
        while (true) {
            int th = row.indexOf("<th", i);
            int td = row.indexOf("<td", i);
            int c = (th < 0) ? td : (td < 0 ? th : Math.min(th, td));
            if (c < 0) {
                break;
            }
            // skip things like <thead> that also start with <th
            char next = row.charAt(c + 3);
            if (next != '>' && next != ' ') {
                i = c + 3;
                continue;
            }
            int open = row.indexOf('>', c) + 1;
            int close = row.indexOf("</t", open);
            if (close < 0) {
                close = row.length();
            }
            out.add(tidy(decode(row.substring(open, close).replaceAll("<[^>]*>", " "))).replace("\n", " "));
            i = close;
        }
        return out;
    }

    private static String lastHeading(String html, int before) {
        int best = -1;
        for (int level = 1; level <= 4; level++) {
            int p = html.lastIndexOf("<h" + level, before);
            if (p > best) {
                best = p;
            }
        }
        if (best < 0) {
            return "";
        }
        int open = html.indexOf('>', best) + 1;
        int close = html.indexOf("</h", open);
        return close < 0 ? "" : tidy(decode(html.substring(open, close).replaceAll("<[^>]*>", " ")));
    }

    // openai puts each tier in a div with data-content-switcher-pane and data-value
    private static String lastTab(String html, int before) {
        int p = html.lastIndexOf("data-content-switcher-pane", before);
        if (p < 0) {
            return "";
        }
        int v = html.indexOf("data-value=\"", p);
        if (v < 0 || v > html.indexOf('>', p)) {
            return "";
        }
        v += 12;
        return html.substring(v, html.indexOf('"', v));
    }

    public static String removeBlocks(String html, String tag) {
        StringBuilder out = new StringBuilder();
        String lower = html.toLowerCase();
        int i = 0;
        while (true) {
            int a = lower.indexOf("<" + tag, i);
            // make sure it is the tag and not a longer name like <header vs <head
            while (a >= 0 && a + tag.length() + 1 < lower.length()) {
                char c = lower.charAt(a + tag.length() + 1);
                if (c == '>' || c == ' ' || c == '\n' || c == '/') {
                    break;
                }
                a = lower.indexOf("<" + tag, a + 1);
            }
            if (a < 0) {
                break;
            }
            int b = lower.indexOf("</" + tag + ">", a);
            if (b < 0) {
                break;
            }
            out.append(html, i, a).append('\n');
            i = b + tag.length() + 3;
        }
        out.append(html.substring(i));
        return out.toString();
    }

    public static String decode(String s) {
        s = s.replace("&nbsp;", " ").replace("&#160;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'").replace("&apos;", "'")
                .replace("&rarr;", "->").replace("&ge;", ">=").replace("&le;", "<=").replace("&mdash;", "-")
                .replace("&ndash;", "-").replace("&times;", "x").replace("&middot;", ".");
        // numeric codes like &#8212; or &#x2014;
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int amp = s.indexOf("&#", i);
            int semi = amp < 0 ? -1 : s.indexOf(';', amp);
            if (amp < 0 || semi < 0 || semi - amp > 8) {
                out.append(s.substring(i));
                break;
            }
            out.append(s, i, amp);
            String code = s.substring(amp + 2, semi);
            try {
                int n = code.startsWith("x") ? Integer.parseInt(code.substring(1), 16) : Integer.parseInt(code);
                out.append((char) n);
            } catch (NumberFormatException e) {
                out.append(s, amp, semi + 1);
            }
            i = semi + 1;
        }
        return out.toString().replace("&amp;", "&");
    }

    // trim every line, drop empty lines and squeeze spaces
    private static String tidy(String s) {
        StringBuilder out = new StringBuilder();
        for (String line : s.split("\n")) {
            String t = line.replaceAll("[ \\t\\r\\u00a0]+", " ").trim();
            if (!t.isEmpty()) {
                out.append(t).append('\n');
            }
        }
        return out.toString().trim();
    }
}
