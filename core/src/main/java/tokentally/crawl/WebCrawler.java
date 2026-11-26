package tokentally.crawl;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tokentally.model.Page;
import tokentally.parse.HtmlCleaner;

// owner: Khem Raj Adhikari
// bfs crawl, queue + hash set
public class WebCrawler {

    private static final Pattern HREF = Pattern.compile("href=\"([^\"#]+)");
    private static final Pattern MD_LINK = Pattern.compile("\\]\\((/[^)\\s]+)\\)");
    private static final String[] SKIP = {".png", ".jpg", ".jpeg", ".gif", ".svg", ".pdf", ".zip", ".css", ".js", ".json", ".xml", ".ico", ".webp", ".mp4", ".md", ".txt"};

    private final Fetcher fetcher = new Fetcher();
    private final RobotsTxtChecker robots = new RobotsTxtChecker(fetcher);
    private final int maxPages;
    private final int maxDepth;
    private final long delayMs;

    // first page html
    public String startHtml;
    public ArrayList<String> errors = new ArrayList<>();

    public WebCrawler(int maxPages, int maxDepth, long delayMs) {
        this.maxPages = maxPages;
        this.maxDepth = maxDepth;
        this.delayMs = delayMs;
    }

    public ArrayList<Page> crawl(Crawler site) {
        ArrayList<Page> pages = new ArrayList<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        ArrayDeque<Integer> depths = new ArrayDeque<>();
        HashSet<String> seen = new HashSet<>();
        startHtml = null;

        String first = normalize(site.startUrl());
        queue.add(first);
        depths.add(0);
        seen.add(first);

        while (!queue.isEmpty() && pages.size() < maxPages) {
            String url = queue.poll();
            int depth = depths.poll();

            if (!robots.allowed(url)) {
                errors.add("robots.txt blocks " + url);
                continue;
            }

            String html;
            String finalUrl;
            try {
                String[] got = fetcher.get(url);
                html = got[0];
                finalUrl = normalize(got[1]);
            } catch (Exception e) {
                errors.add(e.getMessage());
                continue;
            }

            if (startHtml == null) {
                startHtml = html;
            }
            pages.add(new Page(site.provider(), finalUrl, HtmlCleaner.title(html), HtmlCleaner.text(html)));

            if (depth < maxDepth) {
                for (String link : links(html, finalUrl)) {
                    if (!seen.contains(link) && site.follow(link)) {
                        seen.add(link);
                        queue.add(link);
                        depths.add(depth + 1);
                    }
                }
            }
            pause();
        }
        return pages;
    }

    // href and markdown links
    public static ArrayList<String> links(String html, String base) {
        ArrayList<String> out = new ArrayList<>();
        URI baseUri = URI.create(base);
        for (Pattern p : new Pattern[] {HREF, MD_LINK}) {
            Matcher m = p.matcher(html);
            while (m.find()) {
                try {
                    String abs = baseUri.resolve(HtmlCleaner.decode(m.group(1)).trim()).toString();
                    if (abs.startsWith("http") && !skipType(abs)) {
                        out.add(normalize(abs));
                    }
                } catch (Exception e) {
                    // bad link
                }
            }
        }
        return out;
    }

    // clean url
    public static String normalize(String url) {
        int cut = url.indexOf('#');
        if (cut >= 0) {
            url = url.substring(0, cut);
        }
        cut = url.indexOf('?');
        if (cut >= 0) {
            url = url.substring(0, cut);
        }
        while (url.endsWith("/") && url.indexOf('/', 8) < url.length() - 1) {
            url = url.substring(0, url.length() - 1);
        }
        int hostEnd = url.indexOf('/', 8);
        if (hostEnd < 0) {
            return url.toLowerCase();
        }
        return url.substring(0, hostEnd).toLowerCase() + url.substring(hostEnd);
    }

    private static boolean skipType(String url) {
        String lower = url.toLowerCase();
        for (String ext : SKIP) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void pause() {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
