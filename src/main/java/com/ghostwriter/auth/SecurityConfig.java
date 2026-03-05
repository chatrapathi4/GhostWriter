package com.ghostwriter.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;

        @Value("${spring.security.oauth2.client.registration.github.client-id:}")
        private String githubClientId;

        @Value("${spring.security.oauth2.client.registration.github.client-secret:}")
        private String githubClientSecret;

        @Value("${spring.security.oauth2.client.registration.github.scope:read:user,user:email}")
        private String githubScope;

        public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
                this.customOAuth2UserService = customOAuth2UserService;
        }

        private boolean isOAuthConfigured() {
                return githubClientId != null && !githubClientId.isBlank();
        }

        @Bean
        public ClientRegistrationRepository clientRegistrationRepository() {
                List<ClientRegistration> registrations = new ArrayList<>();

                if (isOAuthConfigured()) {
                        registrations.add(
                                        ClientRegistration.withRegistrationId("github")
                                                        .clientId(githubClientId)
                                                        .clientSecret(githubClientSecret)
                                                        .clientAuthenticationMethod(
                                                                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                                                        .authorizationGrantType(
                                                                        AuthorizationGrantType.AUTHORIZATION_CODE)
                                                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                                        .scope(githubScope.split(","))
                                                        .authorizationUri("https://github.com/login/oauth/authorize")
                                                        .tokenUri("https://github.com/login/oauth/access_token")
                                                        .userInfoUri("https://api.github.com/user")
                                                        .userNameAttributeName("id")
                                                        .clientName("GitHub")
                                                        .build());
                }

                if (registrations.isEmpty()) {
                        // Placeholder so Spring doesn't fail — OAuth2 login won't be enabled
                        registrations.add(
                                        ClientRegistration.withRegistrationId("none")
                                                        .clientId("none")
                                                        .authorizationGrantType(
                                                                        AuthorizationGrantType.AUTHORIZATION_CODE)
                                                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                                        .authorizationUri("https://example.com/auth")
                                                        .tokenUri("https://example.com/token")
                                                        .build());
                }

                return new InMemoryClientRegistrationRepository(registrations);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                // ─── Public: existing pages & APIs ───
                                                .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
                                                .requestMatchers("/api/analyze", "/api/expand", "/api/upload")
                                                .permitAll()
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/stories", "/story/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/stories/published",
                                                                "/api/stories/*")
                                                .permitAll()
                                                // ─── Authenticated: dashboard & story management ───
                                                .requestMatchers("/dashboard").authenticated()
                                                .requestMatchers("/api/stories/**").authenticated()
                                                .anyRequest().permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID"));

                // Only enable OAuth2 login if GitHub credentials are configured
                if (isOAuthConfigured()) {
                        http.oauth2Login(oauth2 -> oauth2
                                        .userInfoEndpoint(userInfo -> userInfo
                                                        .userService(customOAuth2UserService))
                                        .defaultSuccessUrl("/dashboard", true));
                }

                return http.build();
        }
}
