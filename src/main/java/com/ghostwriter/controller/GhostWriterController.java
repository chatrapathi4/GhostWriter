package com.ghostwriter.controller;

import com.ghostwriter.model.GhostWriterResponse;
import com.ghostwriter.model.StoryRequest;
import com.ghostwriter.service.GeminiService;
import com.ghostwriter.service.GhostWriterService;
import com.ghostwriter.service.OpenAiService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class GhostWriterController {

    private final GhostWriterService ghostWriterService;
    private final GeminiService geminiService;
    private final OpenAiService openAiService;

    public GhostWriterController(GhostWriterService ghostWriterService,
            GeminiService geminiService,
            OpenAiService openAiService) {
        this.ghostWriterService = ghostWriterService;
        this.geminiService = geminiService;
        this.openAiService = openAiService;
    }

    /**
     * Analyze story text and return directions.
     */
    @PostMapping("/analyze")
    public GhostWriterResponse analyze(@RequestBody StoryRequest request) {
        return ghostWriterService.analyze(request);
    }

    /**
     * Expand a chosen path into a 3-4 sentence preview.
     * Tries: Gemini → OpenAI-compatible → static fallback.
     */
    @PostMapping("/expand")
    public ResponseEntity<Map<String, String>> expandPath(@RequestBody Map<String, String> request) {
        String storyContext = request.getOrDefault("storyContext", "");
        String pathName = request.getOrDefault("pathName", "");
        String pathDescription = request.getOrDefault("pathDescription", "");

        // Try Gemini
        if (geminiService.isAvailable()) {
            String preview = geminiService.expandPath(storyContext, pathName, pathDescription);
            if (preview != null && !preview.isBlank()) {
                return ResponseEntity.ok(Map.of("preview", preview.trim()));
            }
        }

        // Try OpenAI-compatible
        if (openAiService.isAvailable()) {
            String preview = openAiService.expandPath(storyContext, pathName, pathDescription);
            if (preview != null && !preview.isBlank()) {
                return ResponseEntity.ok(Map.of("preview", preview.trim()));
            }
        }

        // Static fallback
        String fallback = pathName + " unfolds as " + pathDescription.toLowerCase()
                + " The consequences of this choice ripple through the story, revealing new truths and challenging everything the characters thought they knew.";
        return ResponseEntity.ok(Map.of("preview", fallback));
    }

    /**
     * Upload a PDF or TXT file and extract its text.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null)
                filename = "";

            String text;

            if (filename.toLowerCase().endsWith(".pdf")) {
                // Extract text from PDF
                try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    text = stripper.getText(doc);
                }
            } else if (filename.toLowerCase().endsWith(".txt")) {
                // Read as plain text
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    text = reader.lines().collect(Collectors.joining("\n"));
                }
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Unsupported file type. Please upload a .pdf or .txt file."));
            }

            // Trim to reasonable length (first ~5000 chars)
            if (text.length() > 5000) {
                text = text.substring(0, 5000) + "...";
            }

            return ResponseEntity.ok(Map.of("text", text.trim(), "filename", filename));

        } catch (Exception e) {
            System.err.println("[Upload] Error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }
}
