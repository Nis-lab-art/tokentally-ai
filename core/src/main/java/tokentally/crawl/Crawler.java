package tokentally.crawl;

// owner: Aakriti Mainali
// crawler interface
public interface Crawler {

    String provider();

    String startUrl();

    // follow this link?
    boolean follow(String url);
}
