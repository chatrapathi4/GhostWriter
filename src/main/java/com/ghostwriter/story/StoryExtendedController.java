package com.ghostwriter.story;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Extends story management with new fields (summary, coverImage)
 * without modifying the existing StoryController.
 */
@RestController
@RequestMapping("/api/stories")
public class StoryExtendedController {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;

    public StoryExtendedController(StoryRepository storyRepository, UserRepository userRepository) {
        this.storyRepository = storyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Update extended story fields (summary, coverImage).
     */
    @PatchMapping("/{id}/extended")
    public ResponseEntity<?> updateExtendedFields(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {

        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Optional<Story> opt = storyRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Story story = opt.get();
        if (!story.getUserId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
        }

        if (body.containsKey("summary")) {
            story.setSummary(body.get("summary"));
        }
        if (body.containsKey("coverImage")) {
            story.setCoverImage(body.get("coverImage"));
        }
        story.setUpdatedAt(Instant.now());
        storyRepository.save(story);

        return ResponseEntity.ok(story);
    }

    private User resolveUser(OAuth2User principal) {
        if (principal == null)
            return null;
        String githubId = String.valueOf(principal.getAttributes().get("id"));
        return userRepository.findByGithubId(githubId).orElse(null);
    }
}
