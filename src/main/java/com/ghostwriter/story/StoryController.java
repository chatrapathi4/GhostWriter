package com.ghostwriter.story;

import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;
    private final UserRepository userRepository;

    public StoryController(StoryService storyService, UserRepository userRepository) {
        this.storyService = storyService;
        this.userRepository = userRepository;
    }

    /**
     * Create a new story (draft or published).
     */
    @PostMapping
    public ResponseEntity<?> createStory(@RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String title = body.getOrDefault("title", "Untitled");
        String content = body.getOrDefault("content", "");
        String genre = body.getOrDefault("genre", "");
        String tone = body.getOrDefault("tone", "");
        String status = body.getOrDefault("status", "draft");

        Story story = storyService.createStory(user.getId(), user.getUsername(),
                title, content, genre, tone, status);
        return ResponseEntity.ok(story);
    }

    /**
     * Update an existing story.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStory(@PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            String title = body.getOrDefault("title", "Untitled");
            String content = body.getOrDefault("content", "");
            String genre = body.getOrDefault("genre", "");
            String tone = body.getOrDefault("tone", "");
            String status = body.getOrDefault("status", "draft");

            Story story = storyService.updateStory(id, user.getId(),
                    title, content, genre, tone, status);
            return ResponseEntity.ok(story);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a story.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStory(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            storyService.deleteStory(id, user.getId());
            return ResponseEntity.ok(Map.of("message", "Story deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current user's stories.
     */
    @GetMapping("/mine")
    public ResponseEntity<?> myStories(@AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        List<Story> stories = storyService.getUserStories(user.getId());
        return ResponseEntity.ok(stories);
    }

    /**
     * Get all published stories (public).
     */
    @GetMapping("/published")
    public ResponseEntity<List<Story>> publishedStories() {
        return ResponseEntity.ok(storyService.getAllPublished());
    }

    /**
     * Get a single story by ID (public for published).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getStory(@PathVariable String id) {
        Optional<Story> opt = storyService.getStoryById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    // ─── Helper ───

    private User resolveUser(OAuth2User principal) {
        if (principal == null)
            return null;
        String githubId = String.valueOf(principal.getAttributes().get("id"));
        return userRepository.findByGithubId(githubId).orElse(null);
    }
}
