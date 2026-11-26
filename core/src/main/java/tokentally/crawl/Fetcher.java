package tokentally.crawl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// owner: Khem Raj Adhikari
// downloads pages
public class Fetcher {

    public static final String USER_AGENT = "Mozilla/5.0 (compatible; TokenTallyBot/1.0; COMP8547 course project)";

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // html and final url
    // retry once on timeout, 429, 5xx
    public String[] get(String url) throws Exception {
        try {
            return once(url);
        } catch (RetryableException e) {
            Thread.sleep(1500);
            try {
                return once(url);
            } catch (RetryableException again) {
                throw new Exception(again.getMessage() + " (tried twice)");
            }
        }
    }

    private static class RetryableException extends Exception {
        RetryableException(String message) {
            super(message);
        }
    }

    private String[] once(String url) throws Exception {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "en")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new Exception("bad url " + url);
        }
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new RetryableException("timeout for " + url);
        } catch (java.io.IOException e) {
            throw new RetryableException("no connection for " + url);
        }
        int code = response.statusCode();
        if (code == 429 || code >= 500) {
            throw new RetryableException("http " + code + " for " + url);
        }
        if (code != 200) {
            throw new Exception("http " + code + " for " + url);
        }
        String type = response.headers().firstValue("content-type").orElse("");
        if (!type.isEmpty() && !type.contains("html") && !type.contains("text")) {
            throw new Exception("not a html page " + url);
        }
        return new String[] {response.body(), response.uri().toString()};
    }

    // null on failure
    public String getQuietly(String url) {
        try {
            return get(url)[0];
        } catch (Exception e) {
            return null;
        }
    }
}
