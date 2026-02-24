package com.ghostwriter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * OpenAI-compatible API service.
 * Works with: OpenAI, Groq, Together AI, OpenRouter, Ollama, and any
 * provider that implements the /v1/chat/completions endpoint.
 */
@Service
public class OpenAiService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.url:}")
    private String apiUrl;

    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public boolean isAvailable() {
        boolean hasUrl = apiUrl != null && !apiUrl.isBlank();
        boolean hasKey = apiKey != null && !apiKey.isBlank();
        boolean isLocal = hasUrl && apiUrl.contains("localhost"); // Ollama needs no key
        boolean ok = hasUrl && (hasKey || isLocal);
        if (ok)
            System.out.println("[OpenAI] Available: url=" + apiUrl + ", model=" + model);
        return ok;
    }

    /**
     * Analyze a story — returns raw JSON text from the AI.
     */
    public String generateAnalysis(String fullContext, String shortMemory, String lastParagraph) {
        return callApi(buildAnalysisPrompt(
                safe(fullContext), safe(shortMemory), safe(lastParagraph)));
    }

    /**
     * Expand a path direction into a preview.
     */
    public String expandPath(String storyContext, String pathName, String pathDescription) {
        return callApi(buildExpandPrompt(
                safe(storyContext), safe(pathName), safe(pathDescription)));
    }

    private String callApi(String prompt) {
        if (!isAvailable())
            return null;

        try {
            // Build request body in OpenAI chat format
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", "user");
            msg.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(msg));
            body.put("temperature", 0.9);
            body.put("max_tokens", 1024);

            String jsonBody = objectMapper.writeValueAsString(body);
            System.out.println("[OpenAI] Calling " + apiUrl + " model=" + model);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            // Add auth header (not needed for Ollama localhost)
            if (apiKey != null && !apiKey.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(
                    reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            System.out.println("[OpenAI] Status: " + response.statusCode());

            if (response.statusCode() == 200 && response.body() != null) {
                String text = extractText(response.body());
                System.out.println("[OpenAI] Success! Response length: " +
                        (text != null ? text.length() : 0));
                return text;
            } else {
                System.err.println("[OpenAI] Error " + response.statusCode() + ": " +
                        response.body().substring(0, Math.min(300, response.body().length())));
            }
        } catch (Exception e) {
            System.err.println("[OpenAI] FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return null;
    }

    private String buildAnalysisPrompt(String fullContext, String shortMemory, String lastParagraph) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Ghost Writer, an AI narrative shadow that analyzes stories.\n\n");
        sb.append("STORY CONTEXT:\n").append(fullContext).append("\n\n");
        if (!"(not provided)".equals(shortMemory))
            sb.append("SHORT MEMORY:\n").append(shortMemory).append("\n\n");
        sb.append("LAST PARAGRAPH:\n").append(lastParagraph).append("\n\n");
        sb.append("INSTRUCTIONS:\n");
        sb.append("1. Detect genre: Fantasy/Sci-Fi/Romance/Horror/Thriller/Comedy/Drama/Mystery/Adventure/Mixed\n");
        sb.append("2. Detect tone: Dark/Lighthearted/Suspenseful/Emotional/Epic/Humorous/Neutral\n");
        sb.append("3. Extract key entities (character names, locations, objects) max 8\n");
        sb.append("4. Write a 1-sentence narrative bridge setting up the branching moment\n");
        sb.append("5. Generate EXACTLY 3 named narrative paths:\n");
        sb.append("   - Each has a creative name and 1-2 sentence description\n");
        sb.append("   - DEEPLY specific to this story's characters and events\n");
        sb.append("   - Balance: 2 logical continuations + 1 surprising twist\n\n");
        sb.append("Return ONLY valid JSON, no markdown fences, no extra text:\n");
        sb.append("{\"genre_detected\":\"...\",\"tone_detected\":\"...\",\"key_entities\":[\"...\"],");
        sb.append("\"narrative_bridge\":\"...\",\"directions\":[");
        sb.append("{\"name\":\"...\",\"description\":\"...\"},");
        sb.append("{\"name\":\"...\",\"description\":\"...\"},");
        sb.append("{\"name\":\"...\",\"description\":\"...\"}]}");
        return sb.toString();
    }

    private String buildExpandPrompt(String storyContext, String pathName, String pathDescription) {
        return "You are Ghost Writer. A writer has chosen this direction:\n\n" +
                "STORY SO FAR:\n" + storyContext + "\n\n" +
                "CHOSEN PATH: " + pathName + "\n" +
                "DESCRIPTION: " + pathDescription + "\n\n" +
                "Write a 3-4 sentence preview of how this path unfolds. " +
                "Be specific to the characters. Write in present tense.\n" +
                "Return ONLY the preview text, no JSON, no markdown.";
    }

    /**
     * Extract text from OpenAI-compatible response.
     * Format: { "choices": [{ "message": { "content": "..." } }] }
     */
    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
        } catch (Exception e) {
            System.err.println("[OpenAI] Parse error: " + e.getMessage());
        }
        return null;
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "(not provided)" : s;
    }
}
