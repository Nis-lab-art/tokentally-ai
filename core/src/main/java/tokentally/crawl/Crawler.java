package tokentally.crawl;

// every provider crawler gives the start page and which links it may follow
public interface Crawler {

    String provider();

    String startUrl();

    // true if the crawler should visit this link
    boolean follow(String url);
}
