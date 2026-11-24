package tokentally.app;

import java.util.ArrayList;

import tokentally.crawl.AnthropicCrawler;
import tokentally.crawl.Crawler;
import tokentally.crawl.GeminiCrawler;
import tokentally.crawl.OpenAICrawler;
import tokentally.crawl.WebCrawler;
import tokentally.crawl.XaiCrawler;
import tokentally.crawl.ZaiCrawler;
import tokentally.model.ModelPrice;
import tokentally.model.Page;
import tokentally.parse.AnthropicParser;
import tokentally.parse.GeminiParser;
import tokentally.parse.OpenAIParser;
import tokentally.parse.PriceParser;
import tokentally.parse.XaiParser;
import tokentally.parse.ZaiParser;

// runs the five crawlers, saves what they found and parses the prices
public class CrawlAll {

    public static final Crawler[] CRAWLERS = {
        new OpenAICrawler(), new GeminiCrawler(), new AnthropicCrawler(), new ZaiCrawler(), new XaiCrawler()
    };

    public static PriceParser parserFor(String provider) {
        switch (provider) {
            case "openai": return new OpenAIParser();
            case "gemini": return new GeminiParser();
            case "anthropic": return new AnthropicParser();
            case "zai": return new ZaiParser();
            case "xai": return new XaiParser();
            default: throw new IllegalArgumentException("no parser for " + provider);
        }
    }

    // crawl every provider, then parse all the saved pricing pages
    public static ArrayList<String> run(DataStore store, int maxPages) throws Exception {
        ArrayList<String> log = new ArrayList<>();
        for (Crawler c : CRAWLERS) {
            WebCrawler crawler = new WebCrawler(maxPages, 2, 400);
            ArrayList<Page> pages = crawler.crawl(c);
            if (crawler.startHtml == null) {
                log.add(c.provider() + " failed, keeping the old saved data " + crawler.errors);
                continue;
            }
            // parse first, so a page redesign that breaks the parser can not wipe the saved data
            int parsed;
            try {
                String startUrl = pages.isEmpty() ? c.startUrl() : pages.get(0).url;
                parsed = parserFor(c.provider()).parse(crawler.startHtml, startUrl).size();
            } catch (Exception e) {
                parsed = 0;
                log.add(c.provider() + " parser error " + e.getMessage());
            }
            if (parsed == 0 && store.loadRaw(c.provider()) != null) {
                log.add(c.provider() + " page changed and no price was found, keeping the old saved data");
                continue;
            }
            store.saveRaw(c.provider(), crawler.startHtml);
            store.savePages(c.provider(), pages);
            log.add(c.provider() + " crawled " + pages.size() + " pages, " + parsed + " prices");
            for (String e : crawler.errors) {
                log.add("  " + e);
            }
        }
        ArrayList<ModelPrice> prices = parseSaved(store);
        store.savePrices(prices);
        log.add("parsed " + prices.size() + " model prices");
        return log;
    }

    // run every parser on the saved pricing pages
    public static ArrayList<ModelPrice> parseSaved(DataStore store) throws Exception {
        ArrayList<ModelPrice> all = new ArrayList<>();
        ArrayList<Page> pages = store.loadPages();
        for (Crawler c : CRAWLERS) {
            String html = store.loadRaw(c.provider());
            if (html == null) {
                continue;
            }
            String url = c.startUrl();
            for (Page p : pages) {
                if (p.provider.equals(c.provider())) {
                    url = p.url; // the first page is the pricing page after redirects
                    break;
                }
            }
            all.addAll(parserFor(c.provider()).parse(html, url));
        }
        return all;
    }

    public static void main(String[] args) throws Exception {
        DataStore store = new DataStore(args.length > 0 ? args[0] : "data");
        for (String line : run(store, 20)) {
            System.out.println(line);
        }
    }
}
