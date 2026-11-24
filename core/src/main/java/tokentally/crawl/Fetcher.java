package tokentally.crawl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// downloads a page with the http client that comes with java
public class Fetcher {

    public static final String USER_AGENT = "Mozilla/5.0 (compatible; TokenTallyBot/1.0; COMP8547 course project)";

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // gives the html and the final url after redirects
    public String[] get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("http " + response.statusCode() + " for " + url);
        }
        String type = response.headers().firstValue("content-type").orElse("");
        if (!type.isEmpty() && !type.contains("html") && !type.contains("text")) {
            throw new Exception("not a html page " + url);
        }
        return new String[] {response.body(), response.uri().toString()};
    }

    // same as get but null when anything goes wrong
    public String getQuietly(String url) {
        try {
            return get(url)[0];
        } catch (Exception e) {
            return null;
        }
    }
}
