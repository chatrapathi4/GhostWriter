package com.ghostwriter.moderation;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ModerationService {

    // Copyrighted content markers
    private static final List<String> COPYRIGHTED_PHRASES = Arrays.asList(
            "harry potter", "lord of the rings", "game of thrones",
            "star wars", "marvel cinematic", "disney princess",
            "hunger games", "twilight saga", "percy jackson");

    // Inappropriate content keywords
    private static final List<String> INAPPROPRIATE_WORDS = Arrays.asList(
            "hate speech", "racial slur", "explicit violence",
            "graphic content", "extremist", "terrorist propaganda");

    // Spam patterns
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALL_CAPS_PATTERN = Pattern.compile("[A-Z\\s]{50,}");
    private static final Pattern REPETITIVE_PATTERN = Pattern.compile("(.{3,})\\1{4,}");

    /**
     * Check content for moderation issues.
     */
    public ModerationResult moderate(String content) {
        if (content == null || content.isBlank()) {
            return new ModerationResult(true, null);
        }

        String lowerContent = content.toLowerCase();

        // Check for copyrighted content
        for (String phrase : COPYRIGHTED_PHRASES) {
            if (lowerContent.contains(phrase)) {
                return new ModerationResult(false,
                        "Content may contain copyrighted material: \"" + phrase + "\"");
            }
        }

        // Check for inappropriate content
        for (String word : INAPPROPRIATE_WORDS) {
            if (lowerContent.contains(word)) {
                return new ModerationResult(false,
                        "Content flagged for inappropriate material: \"" + word + "\"");
            }
        }

        // Check for spam: excessive URLs
        long urlCount = URL_PATTERN.matcher(content).results().count();
        if (urlCount > 5) {
            return new ModerationResult(false,
                    "Content flagged as spam: too many URLs (" + urlCount + " found)");
        }

        // Check for spam: ALL CAPS blocks
        if (ALL_CAPS_PATTERN.matcher(content).find()) {
            return new ModerationResult(false,
                    "Content flagged as spam: excessive use of capital letters");
        }

        // Check for spam: repetitive patterns
        if (REPETITIVE_PATTERN.matcher(content).find()) {
            return new ModerationResult(false,
                    "Content flagged as spam: repetitive text patterns detected");
        }

        return new ModerationResult(true, null);
    }
}
