package com.ghostwriter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public GeminiService() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15));
        try {
            builder.sslContext(SSLContext.getDefault());
        } catch (NoSuchAlgorithmException ignored) {
        }
        this.httpClient = builder.build();
    }

    public boolean isAvailable() {
        boolean ok = apiKey != null && !apiKey.isBlank();
        System.out.println("[Gemini] Key configured: " + ok);
        return ok;
    }

    /**
     * Analyze a story and return raw JSON text from Gemini.
     */
    public String generateAnalysis(String fullContext, String shortMemory, String lastParagraph) {
        return callGemini(buildAnalysisPrompt(
                safe(fullContext), safe(shortMemory), safe(lastParagraph)));
    }

    /**
     * Expand a chosen path direction into a 3-4 sentence summary.
     */
    public String expandPath(String storyContext, String pathName, String pathDescription) {
        return callGemini(buildExpandPrompt(
                safe(storyContext), safe(pathName), safe(pathDescription)));
    }

    private String callGemini(String prompt) {
        if (!isAvailable())
            return null;

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                        + model + ":generateContent?key=" + apiKey;

                Map<String, Object> textPart = new HashMap<>();
                textPart.put("text", prompt);

                Map<String, Object> content = new HashMap<>();
                content.put("parts", List.of(textPart));

                Map<String, Object> config = new HashMap<>();
                config.put("temperature", 0.9);
                config.put("maxOutputTokens", 1024);

                Map<String, Object> body = new HashMap<>();
                body.put("contents", List.of(content));
                body.put("generationConfig", config);

                String jsonBody = objectMapper.writeValueAsString(body);
                System.out.println("[Gemini] Attempt " + attempt + "/" + maxRetries + "...");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                System.out.println("[Gemini] Status: " + response.statusCode());

                if (response.statusCode() == 200 && response.body() != null) {
                    String text = extractText(response.body());
                    System.out.println("[Gemini] Success! Response: " +
                            (text != null ? text.substring(0, Math.min(150, text.length())) + "..." : "null"));
                    return text;
                } else if (response.statusCode() == 429 && attempt < maxRetries) {
                    // Rate limited → wait and retry
                    long waitMs = attempt * 2000L;
                    System.out.println("[Gemini] Rate limited (429). Waiting " + waitMs + "ms before retry...");
                    Thread.sleep(waitMs);
                    continue;
                } else {
                    System.err.println("[Gemini] Error " + response.statusCode() + ": " +
                            response.body().substring(0, Math.min(300, response.body().length())));
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[Gemini] FAILED: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            }
        }
        return null;
    }

    private String buildAnalysisPrompt(String fullContext, String shortMemory, String lastParagraph) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Ghost Writer, an AI narrative shadow that analyzes stories.\n\n");
        sb.append("STORY CONTEXT:\n").append(fullContext).append("\n\n");
        sb.append("SHORT MEMORY:\n").append(shortMemory).append("\n\n");
        sb.append("LAST PARAGRAPH:\n").append(lastParagraph).append("\n\n");
        sb.append("INSTRUCTIONS:\n");
        sb.append("1. Detect genre: Fantasy/Sci-Fi/Romance/Horror/Thriller/Comedy/Drama/Mystery/Adventure/Mixed\n");
        sb.append("2. Detect tone: Dark/Lighthearted/Suspenseful/Emotional/Epic/Humorous/Neutral\n");
        sb.append("3. Extract key entities (character names, locations, objects) max 8\n");
        sb.append("4. Write a 1-sentence narrative bridge setting up the branching moment\n");
        sb.append("5. Generate EXACTLY 3 named narrative paths:\n");
        sb.append("   - Each has a creative name and a 1-2 sentence description\n");
        sb.append("   - DEEPLY specific to this story's characters and events\n");
        sb.append("   - Each is a DIFFERENT branch. 2 logical + 1 twist.\n");
        sb.append("   - Reference actual characters by name.\n\n");
        sb.append("Return ONLY valid JSON, no markdown fences, no extra text:\n");
        sb.append("{\"genre_detected\":\"...\",\"tone_detected\":\"...\",\"key_entities\":[\"...\"],");
        sb.append("\"narrative_bridge\":\"...\",\"directions\":[");
        sb.append("{\"name\":\"...\",\"description\":\"...\"},");
        sb.append("{\"name\":\"...\",\"description\":\"...\"},");
        sb.append("{\"name\":\"...\",\"description\":\"...\"}]}");
        return sb.toString();
    }

    private String buildExpandPrompt(String storyContext, String pathName, String pathDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Ghost Writer. A writer has chosen a narrative direction for their story.\n\n");
        sb.append("STORY SO FAR:\n").append(storyContext).append("\n\n");
        sb.append("CHOSEN PATH: ").append(pathName).append("\n");
        sb.append("PATH DESCRIPTION: ").append(pathDescription).append("\n\n");
        sb.append("Write a 3-4 sentence preview/summary of how this path would unfold. ");
        sb.append("Be specific to the characters and world. Write in present tense, like a story outline. ");
        sb.append("Do NOT write the actual story — just a compelling preview of what happens next.\n\n");
        sb.append("Return ONLY the preview text, no JSON, no markdown, no extra commentary.");
        return sb.toString();
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
        } catch (Exception e) {
            System.err.println("[Gemini] Parse error: " + e.getMessage());
        }
        return null;
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "(not provided)" : s;
    }
}
