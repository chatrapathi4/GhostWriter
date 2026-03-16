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
    private final AdminUserService adminUserService;

    @Value("${admin.github.id:}")
    private String adminGithubId;

    public AdminController(AdminService adminService, AdminUserService adminUserService) {
        this.adminService = adminService;
        this.adminUserService = adminUserService;
    }

    // ═══════════════════════════════════════
    // Story Management Endpoints
    // ═══════════════════════════════════════

    @GetMapping("/stories/pending")
    public ResponseEntity<?> getPendingStories(@AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminService.getPendingStories());
    }

    @GetMapping("/stories/published")
    public ResponseEntity<?> getPublishedStories(@AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminService.getPublishedStories());
    }

    @GetMapping("/stories/rejected")
    public ResponseEntity<?> getRejectedStories(@AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminService.getRejectedStories());
    }

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

    // ═══════════════════════════════════════
    // User Management Endpoints
    // ═══════════════════════════════════════

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminUserService.getAllUsersWithStats());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        try {
            adminUserService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users/{id}/stories")
    public ResponseEntity<?> getUserStories(@PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        if (!isAdmin(principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }
        return ResponseEntity.ok(adminUserService.getUserStories(id));
    }

    // ═══════════════════════════════════════
    // Helper
    // ═══════════════════════════════════════

    private boolean isAdmin(OAuth2User principal) {
        if (principal == null || adminGithubId == null || adminGithubId.isBlank()) {
            return false;
        }
        String githubId = String.valueOf(principal.getAttributes().get("id"));
        return adminGithubId.equals(githubId);
    }
}
