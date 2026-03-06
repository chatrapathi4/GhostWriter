package com.ghostwriter.chapter;

import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    private final ChapterService chapterService;
    private final UserRepository userRepository;

    public ChapterController(ChapterService chapterService, UserRepository userRepository) {
        this.chapterService = chapterService;
        this.userRepository = userRepository;
    }

    /**
     * Get all chapters for a story (public).
     */
    @GetMapping("/story/{storyId}")
    public ResponseEntity<List<Chapter>> getChapters(@PathVariable String storyId) {
        return ResponseEntity.ok(chapterService.getChaptersByStoryId(storyId));
    }

    /**
     * Get a single chapter by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getChapter(@PathVariable String id) {
        return chapterService.getChapterById(id)
                .map(ch -> ResponseEntity.ok((Object) ch))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new chapter.
     */
    @PostMapping
    public ResponseEntity<?> createChapter(@RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {
        if (resolveUser(principal) == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String storyId = body.getOrDefault("storyId", "");
        int chapterNumber = Integer.parseInt(body.getOrDefault("chapterNumber", "1"));
        String title = body.getOrDefault("title", "Untitled Chapter");
        String content = body.getOrDefault("content", "");

        Chapter chapter = chapterService.createChapter(storyId, chapterNumber, title, content);
        return ResponseEntity.ok(chapter);
    }

    /**
     * Update a chapter.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateChapter(@PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {
        if (resolveUser(principal) == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            String title = body.getOrDefault("title", "");
            String content = body.getOrDefault("content", "");
            Chapter chapter = chapterService.updateChapter(id, title, content);
            return ResponseEntity.ok(chapter);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a chapter.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChapter(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        if (resolveUser(principal) == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        chapterService.deleteChapter(id);
        return ResponseEntity.ok(Map.of("message", "Chapter deleted"));
    }

    private User resolveUser(OAuth2User principal) {
        if (principal == null)
            return null;
        String githubId = String.valueOf(principal.getAttributes().get("id"));
        return userRepository.findByGithubId(githubId).orElse(null);
    }
}
