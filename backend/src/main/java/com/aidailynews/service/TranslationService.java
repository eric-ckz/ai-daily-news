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

        String apiKey = envFirst("LLM_API_KEY", "OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return text;
        }

        String model = envFirst("LLM_MODEL", "OPENAI_MODEL");
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }

        String baseUrl = envFirst("LLM_BASE_URL", "OPENAI_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.openai.com/v1";
        }
        baseUrl = baseUrl.replaceAll("/+$", "");

        String chatPath = envFirst("LLM_CHAT_PATH");
        if (chatPath == null || chatPath.isBlank()) {
            chatPath = "/chat/completions";
        }
        if (!chatPath.startsWith("/")) {
            chatPath = "/" + chatPath;
        }

        String authHeader = envFirst("LLM_AUTH_HEADER");
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = "Authorization";
        }

        String authScheme = envFirst("LLM_AUTH_SCHEME");
        if (authScheme == null || authScheme.isBlank()) {
            authScheme = "Bearer";
        }

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

            int timeoutSeconds = 20;
            try {
                String timeoutEnv = envFirst("LLM_TIMEOUT_SECONDS");
                if (timeoutEnv != null && !timeoutEnv.isBlank()) {
                    timeoutSeconds = Math.max(5, Math.min(120, Integer.parseInt(timeoutEnv.trim())));
                }
            } catch (Exception ignored) {
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + chatPath))
                    .header(authHeader, authScheme + " " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
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

    private String envFirst(String... keys) {
        for (String k : keys) {
            String v = System.getenv(k);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
