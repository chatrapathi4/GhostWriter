package com.ghostwriter.auth;

import com.ghostwriter.user.User;
import com.ghostwriter.user.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "github"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.getOrDefault("login", "");
        String email = (String) attributes.getOrDefault("email", "");
        String avatarUrl = (String) attributes.getOrDefault("avatar_url", "");

        // Create or update user
        User user = userRepository.findByGithubId(githubId).orElse(null);
        if (user == null) {
            user = new User(githubId, username, email, avatarUrl, provider);
        } else {
            user.setUsername(username);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
        }
        userRepository.save(user);

        return oAuth2User;
    }
}
