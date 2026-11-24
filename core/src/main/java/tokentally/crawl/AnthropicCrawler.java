package tokentally.crawl;

// nishan
// claude pricing page and the model overview pages
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
