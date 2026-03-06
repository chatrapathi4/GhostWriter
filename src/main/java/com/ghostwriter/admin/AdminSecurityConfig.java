package com.ghostwriter.admin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AdminSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**", "/api/admin/**",
                        "/feed", "/write", "/write/**", "/story-view/**",
                        "/api/chapters/**", "/api/moderation/**",
                        "/api/import/**",
                        "/api/stories/*/like", "/api/stories/*/view",
                        "/api/stories/*/interactions",
                        "/css/**", "/js/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public: new pages and read APIs
                        .requestMatchers("/feed", "/story-view/**").permitAll()
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chapters/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stories/*/interactions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/stories/*/view").permitAll()
                        // Authenticated: write operations
                        .requestMatchers("/write", "/write/**").authenticated()
                        .requestMatchers("/api/chapters/**").authenticated()
                        .requestMatchers("/api/moderation/**").authenticated()
                        .requestMatchers("/api/import/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/stories/*/like").authenticated()
                        // Admin: admin panel
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().permitAll());

        return http.build();
    }
}
