package tokentally.crawl;

// owner: Nishan Giri
// claude pages
public class AnthropicCrawler implements Crawler {

    public String provider() {
        return "anthropic";
    }

    public String startUrl() {
        return "https://platform.claude.com/docs/en/about-claude/pricing";
    }

    public boolean follow(String url) {
        return url.equals("https://platform.claude.com/docs/en/about-claude/models/overview")
                || (url.startsWith("https://platform.claude.com/docs/en/models/") && url.endsWith("/overview"));
    }
}
