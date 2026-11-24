package tokentally.crawl;

// shrijana
// gemini pricing page (it now redirects to ai studio) and its model pages
public class GeminiCrawler implements Crawler {

    public String provider() {
        return "gemini";
    }

    public String startUrl() {
        return "https://ai.google.dev/gemini-api/docs/pricing";
    }

    public boolean follow(String url) {
        return url.startsWith("https://aistudio.google.com/docs/models/")
                || url.startsWith("https://ai.google.dev/gemini-api/docs/models/");
    }
}
