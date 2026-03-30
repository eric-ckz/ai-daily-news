package com.aidailynews.service;

import com.aidailynews.model.NewsItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class TavilyService {

    private static final String TAVILY_URL = "https://api.tavily.com/search";
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public List<NewsItem> searchNews(String category, String query, int maxResults, int daysBack) {
        String apiKey = System.getenv("TAVILY_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing TAVILY_API_KEY");
        }

        try {
            String body = mapper.createObjectNode()
                    .put("api_key", apiKey)
                    .put("query", query)
                    .put("topic", "news")
                    .put("search_depth", "advanced")
                    .put("max_results", maxResults)
                    .put("include_raw_content", true)
                    .put("days", daysBack)
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TAVILY_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Tavily request failed: " + response.statusCode() + " " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode results = root.path("results");
            List<NewsItem> items = new ArrayList<>();

            for (JsonNode r : results) {
                NewsItem item = new NewsItem();
                item.setCategory(category);
                item.setTitle(clean(r.path("title").asText("")));
                item.setContent(clean(r.path("raw_content").asText(r.path("content").asText(""))));
                item.setUrl(r.path("url").asText(""));
                item.setSource(r.path("url").asText(""));
                item.setPublishedDate(r.path("published_date").asText(""));
                item.setScore(r.path("score").asDouble(0.0));
                items.add(item);
            }
            return items;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch Tavily results", e);
        }
    }

    private String clean(String s) {
        if (s == null) return "";
        String t = StringEscapeUtils.unescapeHtml4(s)
                .replaceAll("\\s+", " ")
                .trim();
        return t.length() > 4000 ? t.substring(0, 4000) + "..." : t;
    }
}
