package tokentally.crawl;

// owner: Aakriti Mainali
// z.ai pages
public class ZaiCrawler implements Crawler {

    public String provider() {
        return "zai";
    }

    public String startUrl() {
        return "https://docs.z.ai/guides/overview/pricing";
    }

    public boolean follow(String url) {
        return url.startsWith("https://docs.z.ai/guides/llm/") || url.startsWith("https://docs.z.ai/guides/vlm/");
    }
}
