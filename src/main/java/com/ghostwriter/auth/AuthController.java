package com.ghostwriter.auth;

import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${admin.github.id:}")
    private String adminGithubId;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> authStatus(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> result = new HashMap<>();

        if (principal == null) {
            result.put("authenticated", false);
            return ResponseEntity.ok(result);
        }

        String githubId = String.valueOf(principal.getAttributes().get("id"));
        User user = userRepository.findByGithubId(githubId).orElse(null);

        result.put("authenticated", true);
        result.put("username", principal.getAttributes().getOrDefault("login", ""));
        result.put("avatarUrl", principal.getAttributes().getOrDefault("avatar_url", ""));
        result.put("userId", user != null ? user.getId() : "");
        result.put("isAdmin", adminGithubId != null && !adminGithubId.isBlank()
                && adminGithubId.equals(githubId));

        return ResponseEntity.ok(result);
    }
}
