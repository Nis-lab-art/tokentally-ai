package tokentally.crawl;

// owner: Abhinav Pandey
// openai pages
public class OpenAICrawler implements Crawler {

    public String provider() {
        return "openai";
    }

    public String startUrl() {
        return "https://developers.openai.com/api/docs/pricing";
    }

    public boolean follow(String url) {
        return url.equals("https://developers.openai.com/api/docs/models")
                || url.startsWith("https://developers.openai.com/api/docs/models/");
    }
}
