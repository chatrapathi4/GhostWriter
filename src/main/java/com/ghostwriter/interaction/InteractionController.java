package com.ghostwriter.interaction;

import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
public class InteractionController {

    private final LikeService likeService;
    private final ViewService viewService;
    private final UserRepository userRepository;

    public InteractionController(LikeService likeService, ViewService viewService,
            UserRepository userRepository) {
        this.likeService = likeService;
        this.viewService = viewService;
        this.userRepository = userRepository;
    }

    /**
     * Toggle like on a story.
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        boolean liked = likeService.toggleLike(id, user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeService.getLikeCount(id));
        return ResponseEntity.ok(result);
    }

    /**
     * Record a view on a story.
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<?> recordView(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        String userId = user != null ? user.getId() : null;
        viewService.recordView(id, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("viewCount", viewService.getViewCount(id));
        return ResponseEntity.ok(result);
    }

    /**
     * Get interactions data for a story.
     */
    @GetMapping("/{id}/interactions")
    public ResponseEntity<?> getInteractions(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        User user = resolveUser(principal);
        Map<String, Object> result = new HashMap<>();
        result.put("likeCount", likeService.getLikeCount(id));
        result.put("viewCount", viewService.getViewCount(id));
        result.put("userLiked", user != null && likeService.hasUserLiked(id, user.getId()));
        return ResponseEntity.ok(result);
    }

    private User resolveUser(OAuth2User principal) {
        if (principal == null)
            return null;
        String githubId = String.valueOf(principal.getAttributes().get("id"));
        return userRepository.findByGithubId(githubId).orElse(null);
    }
}
