package tokentally.crawl;

// khem
// xai pricing page and the model pages
public class XaiCrawler implements Crawler {

    public String provider() {
        return "xai";
    }

    public String startUrl() {
        return "https://docs.x.ai/developers/pricing";
    }

    public boolean follow(String url) {
        return url.startsWith("https://docs.x.ai/developers/models");
    }
}
