package com.ghostwriter.chapter;

import com.ghostwriter.story.Story;
import com.ghostwriter.story.StoryRepository;
import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/import")
public class TxtImportController {

    private final TxtImportService txtImportService;
    private final ChapterService chapterService;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    public TxtImportController(TxtImportService txtImportService,
            ChapterService chapterService,
            StoryRepository storyRepository,
            UserRepository userRepository) {
        this.txtImportService = txtImportService;
        this.chapterService = chapterService;
        this.storyRepository = storyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Upload a TXT file, auto-split into chapters, create story + chapters.
     */
    @PostMapping("/txt")
    public ResponseEntity<?> importTxt(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", defaultValue = "Imported Story") String title,
            @RequestParam(value = "genre", defaultValue = "") String genre,
            @RequestParam(value = "tone", defaultValue = "") String tone,
            @AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".txt")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Please upload a .txt file"));
            }

            // Read file contents
            String text;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                text = reader.lines().collect(Collectors.joining("\n"));
            }

            // Parse into chapters
            List<Chapter> chapters = txtImportService.parseTextIntoChapters(text);

            // Create the story
            Story story = new Story(user.getId(), user.getUsername(), title, null,
                    genre, tone, "draft");
            story.setSummary("");
            story.setUpdatedAt(Instant.now());
            story = storyRepository.save(story);

            // Save chapters
            List<Chapter> savedChapters = chapterService.saveAllChapters(story.getId(), chapters);

            Map<String, Object> result = new HashMap<>();
            result.put("story", story);
            result.put("chapters", savedChapters);
            result.put("chapterCount", savedChapters.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to import file: " + e.getMessage()));
        }
    }

    /**
     * Preview chapter split from text without saving.
     */
    @PostMapping("/txt/preview")
    public ResponseEntity<?> previewSplit(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        List<Chapter> chapters = txtImportService.parseTextIntoChapters(text);
        return ResponseEntity.ok(Map.of("chapters", chapters, "chapterCount", chapters.size()));
    }

    private User resolveUser(OAuth2User principal) {
        if (principal == null)
            return null;
        String githubId = String.valueOf(principal.getAttributes().get("id"));
        return userRepository.findByGithubId(githubId).orElse(null);
    }
}
