package com.ghostwriter.moderation;

import com.ghostwriter.story.Story;
import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final PublishingService publishingService;
    private final UserRepository userRepository;

    public ModerationController(PublishingService publishingService, UserRepository userRepository) {
        this.publishingService = publishingService;
        this.userRepository = userRepository;
    }

    /**
     * Submit a story for publishing (goes through moderation).
     */
    @PostMapping("/publish/{storyId}")
    public ResponseEntity<?> publishStory(@PathVariable String storyId,
            @AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            Story story = publishingService.submitForPublishing(storyId, user.getId());
            return ResponseEntity.ok(story);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User resolveUser(OAuth2User principal) {
        if (principal == null)
            return null;
        String githubId = String.valueOf(principal.getAttributes().get("id"));
        return userRepository.findByGithubId(githubId).orElse(null);
    }
}
