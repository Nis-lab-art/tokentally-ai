package tokentally.model;

// owner: Shrijana Karki
// one crawled page
public class Page {

    public String provider;
    public String url;
    public String title;
    public String text;

    public Page(String provider, String url, String title, String text) {
        this.provider = provider;
        this.url = url;
        this.title = title;
        this.text = text;
    }
}
