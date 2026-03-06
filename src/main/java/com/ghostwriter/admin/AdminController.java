package com.ghostwriter.admin;

import com.ghostwriter.story.Story;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @Value("${admin.github.id:}")
    private String adminGithubId;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Get pending stories.
     */
    @GetMapping("/stories/pending")
    public ResponseEntity<?> getPendingStories(@AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminService.getPendingStories());
    }

    /**
     * Get published stories.
     */
    @GetMapping("/stories/published")
    public ResponseEntity<?> getPublishedStories(@AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminService.getPublishedStories());
    }

    /**
     * Get rejected stories.
     */
    @GetMapping("/stories/rejected")
    public ResponseEntity<?> getRejectedStories(@AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminService.getRejectedStories());
    }

    /**
     * Approve a story.
     */
    @PostMapping("/stories/{id}/approve")
    public ResponseEntity<?> approveStory(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        try {
            Story story = adminService.approveStory(id);
            return ResponseEntity.ok(story);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject a story with reason.
     */
    @PostMapping("/stories/{id}/reject")
    public ResponseEntity<?> rejectStory(@PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        try {
            String reason = body.getOrDefault("reason", "Rejected by admin");
            Story story = adminService.rejectStory(id, reason);
            return ResponseEntity.ok(story);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete any story (admin privilege).
     */
    @DeleteMapping("/stories/{id}")
    public ResponseEntity<?> deleteStory(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        try {
            adminService.deleteStory(id);
            return ResponseEntity.ok(Map.of("message", "Story deleted by admin"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isAdmin(OAuth2User principal) {
        if (principal == null || adminGithubId == null || adminGithubId.isBlank()) {
            return false;
        }
        String githubId = String.valueOf(principal.getAttributes().get("id"));
        return adminGithubId.equals(githubId);
    }
}
