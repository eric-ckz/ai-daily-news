package com.aidailynews.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class TranslationService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String translateToChinese(String text, int maxLength) {
        if (text == null || text.isBlank()) return "";

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return text;
        }

        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");
        String input = text.length() > maxLength ? text.substring(0, maxLength) : text;

        try {
            String payload = mapper.createObjectNode()
                    .put("model", model)
                    .set("messages", mapper.createArrayNode()
                            .add(mapper.createObjectNode()
                                    .put("role", "system")
                                    .put("content", "你是专业新闻翻译编辑。请将输入翻译为简体中文，忠实、简洁、可读，保留专有名词。只输出翻译结果，不要解释。"))
                            .add(mapper.createObjectNode()
                                    .put("role", "user")
                                    .put("content", input)))
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                return input;
            }

            JsonNode root = mapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText(input).trim();
        } catch (IOException | InterruptedException e) {
            return input;
        }
    }
}
