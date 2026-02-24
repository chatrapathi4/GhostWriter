package com.ghostwriter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghostwriter.model.GhostWriterResponse;
import com.ghostwriter.model.GhostWriterResponse.Direction;
import com.ghostwriter.model.StoryRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GhostWriterService {

        private final GeminiService geminiService;
        private final OpenAiService openAiService;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public GhostWriterService(GeminiService geminiService, OpenAiService openAiService) {
                this.geminiService = geminiService;
                this.openAiService = openAiService;
        }

        /**
         * Main analysis — tries Gemini first, then OpenAI-compatible, falls back to
         * templates.
         */
        public GhostWriterResponse analyze(StoryRequest request) {
                // 1. Try Gemini
                if (geminiService.isAvailable()) {
                        String rawJson = geminiService.generateAnalysis(
                                        request.getFullContext(), request.getShortMemory(), request.getLastParagraph());
                        GhostWriterResponse aiResponse = parseAiResponse(rawJson);
                        if (aiResponse != null) {
                                aiResponse.setSource("ai");
                                return aiResponse;
                        }
                        System.out.println("[GhostWriterService] Gemini failed, trying OpenAI...");
                }

                // 2. Try OpenAI-compatible
                if (openAiService.isAvailable()) {
                        String rawJson = openAiService.generateAnalysis(
                                        request.getFullContext(), request.getShortMemory(), request.getLastParagraph());
                        GhostWriterResponse aiResponse = parseAiResponse(rawJson);
                        if (aiResponse != null) {
                                aiResponse.setSource("ai");
                                return aiResponse;
                        }
                        System.out.println("[GhostWriterService] OpenAI failed, falling back to templates");
                }

                // 3. Template fallback
                return fallbackAnalysis(request);
        }

        private GhostWriterResponse parseAiResponse(String rawJson) {
                try {
                        if (rawJson == null || rawJson.isBlank())
                                return null;

                        // Strip markdown fences
                        String cleaned = rawJson.trim();
                        if (cleaned.startsWith("```json"))
                                cleaned = cleaned.substring(7);
                        else if (cleaned.startsWith("```"))
                                cleaned = cleaned.substring(3);
                        if (cleaned.endsWith("```"))
                                cleaned = cleaned.substring(0, cleaned.length() - 3);
                        cleaned = cleaned.trim();

                        System.out.println("[GhostWriterService] Parsing AI response: "
                                        + cleaned.substring(0, Math.min(300, cleaned.length())));

                        JsonNode root = objectMapper.readTree(cleaned);

                        GhostWriterResponse resp = new GhostWriterResponse();
                        resp.setGenreDetected(root.path("genre_detected").asText("Drama"));
                        resp.setToneDetected(root.path("tone_detected").asText("Neutral"));
                        resp.setNarrativeBridge(root.path("narrative_bridge").asText(""));

                        // Entities
                        List<String> entities = new ArrayList<>();
                        if (root.has("key_entities") && root.get("key_entities").isArray()) {
                                for (JsonNode e : root.get("key_entities"))
                                        entities.add(e.asText());
                        }
                        resp.setKeyEntities(entities);

                        // Directions — handle both object format and string format
                        List<Direction> directions = new ArrayList<>();
                        if (root.has("directions") && root.get("directions").isArray()) {
                                for (JsonNode d : root.get("directions")) {
                                        if (d.isObject()) {
                                                directions.add(new Direction(
                                                                d.path("name").asText(
                                                                                "Path " + (directions.size() + 1)),
                                                                d.path("description").asText("")));
                                        } else {
                                                // Plain string fallback
                                                String text = d.asText();
                                                directions.add(new Direction("Path " + (directions.size() + 1), text));
                                        }
                                }
                        }

                        if (directions.size() < 3) {
                                System.err.println("[GhostWriterService] AI returned fewer than 3 directions: "
                                                + directions.size());
                                return null;
                        }
                        if (directions.size() > 3)
                                directions = directions.subList(0, 3);

                        resp.setDirections(directions);
                        System.out.println("[GhostWriterService] AI analysis successful!");
                        return resp;

                } catch (Exception e) {
                        System.err.println("[GhostWriterService] AI parsing failed: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        }

        // ─── Fallback Template Engine ───

        private GhostWriterResponse fallbackAnalysis(StoryRequest request) {
                String fullText = safe(request.getFullContext()) + " " +
                                safe(request.getShortMemory()) + " " + safe(request.getLastParagraph());

                String genre = detectGenre(fullText);
                String tone = detectTone(fullText);
                List<String> entities = extractEntities(fullText);

                String entity = entities.isEmpty() ? "The protagonist" : entities.get(0);
                List<Direction> directions = generateFallbackDirections(genre, entity);

                GhostWriterResponse resp = new GhostWriterResponse();
                resp.setGenreDetected(genre);
                resp.setToneDetected(tone);
                resp.setKeyEntities(entities);
                resp.setNarrativeBridge(entity + "'s story reaches a critical turning point. Three paths lie ahead:");
                resp.setDirections(directions);
                resp.setSource("template");
                return resp;
        }

        private List<Direction> generateFallbackDirections(String genre, String entity) {
                Map<String, List<Direction>> templates = new HashMap<>();
                templates.put("Fantasy", List.of(
                                new Direction("The Chosen Path",
                                                entity + " discovers the prophecy was meant for someone else entirely"),
                                new Direction("The Betrayer's Path",
                                                "A trusted ally reveals a secret allegiance to the enemy forces"),
                                new Direction("The Forbidden Path",
                                                entity + " unlocks ancient magic at the cost of their memories")));
                templates.put("Sci-Fi", List.of(
                                new Direction("The Override Path",
                                                entity + " discovers they can rewrite the system's core protocols"),
                                new Direction("The Signal Path",
                                                "A mysterious transmission reveals another consciousness within the network"),
                                new Direction("The Glitch Path", entity
                                                + " realizes the simulation has been running their decisions in reverse")));
                templates.put("Horror", List.of(
                                new Direction("The Descent", entity
                                                + " follows the sounds deeper into the darkness against all reason"),
                                new Direction("The Mirror's Truth",
                                                "The reflection begins moving independently, revealing a darker version"),
                                new Direction("The Escape", entity
                                                + " finds a way out only to realize they were never truly trapped")));
                templates.put("Thriller", List.of(
                                new Direction("The Hunter's Path", entity
                                                + " turns from prey to predator, setting a trap for the pursuer"),
                                new Direction("The Insider",
                                                "The real threat is revealed to come from within their own circle"),
                                new Direction("The Clock Path",
                                                "A countdown begins that forces an impossible choice between two lives")));
                templates.put("Drama", List.of(
                                new Direction("The Confession",
                                                entity + " finally speaks the truth that has been weighing on them"),
                                new Direction("The Departure",
                                                "Someone leaves without warning, forcing everyone to confront what was unsaid"),
                                new Direction("The Return",
                                                "A figure from the past reappears, reopening old wounds and old hopes")));
                // Default
                templates.put("default", List.of(
                                new Direction("The Revelation",
                                                entity + " uncovers a truth that changes everything they believed"),
                                new Direction("The Alliance",
                                                "An unlikely partnership forms to face a shared and growing threat"),
                                new Direction("The Sacrifice", entity
                                                + " must give up something precious to protect what matters most")));

                return templates.getOrDefault(genre, templates.get("default"));
        }

        // ─── Genre / Tone Detection ───

        private static final Map<String, List<String>> GENRE_KEYWORDS = new LinkedHashMap<>();
        static {
                GENRE_KEYWORDS.put("Fantasy", Arrays.asList("dragon", "wizard", "magic", "kingdom", "sword", "spell",
                                "throne", "castle", "prophecy", "quest", "warrior", "knight", "curse"));
                GENRE_KEYWORDS.put("Sci-Fi",
                                Arrays.asList("spaceship", "galaxy", "robot", "android", "planet", "alien", "quantum",
                                                "starship", "AI", "code", "simulation", "matrix", "program", "system",
                                                "grid", "node", "cursor", "root", "access", "hack", "digital",
                                                "console", "algorithm", "terminal", "data"));
                GENRE_KEYWORDS.put("Horror", Arrays.asList("blood", "scream", "shadow", "ghost", "dead", "terror",
                                "nightmare", "monster", "demon", "haunted", "dark"));
                GENRE_KEYWORDS.put("Romance", Arrays.asList("love", "heart", "kiss", "passion", "embrace", "desire",
                                "romance", "beloved", "longing", "wedding"));
                GENRE_KEYWORDS.put("Thriller", Arrays.asList("chase", "escape", "gun", "danger", "suspect", "detective",
                                "crime", "murder", "spy", "assassin", "bomb"));
                GENRE_KEYWORDS.put("Mystery", Arrays.asList("clue", "mystery", "secret", "hidden", "disappear",
                                "puzzle", "riddle", "detective", "cryptic", "investigate"));
                GENRE_KEYWORDS.put("Adventure", Arrays.asList("journey", "explore", "treasure", "map", "expedition",
                                "discover", "wilderness", "mountain", "brave"));
                GENRE_KEYWORDS.put("Drama", Arrays.asList("family", "struggle", "emotion", "conflict", "relationship",
                                "betrayal", "forgive", "grief", "sacrifice", "choice"));
        }

        private static final Map<String, List<String>> TONE_KEYWORDS = new LinkedHashMap<>();
        static {
                TONE_KEYWORDS.put("Dark", Arrays.asList("shadow", "blood", "death", "darkness", "grim", "cold",
                                "despair", "sinister"));
                TONE_KEYWORDS.put("Suspenseful", Arrays.asList("suddenly", "watched", "silence", "waiting", "nervous",
                                "tense", "frozen", "suspended", "stopped", "pause"));
                TONE_KEYWORDS.put("Emotional", Arrays.asList("tears", "cry", "heart", "pain", "loss", "remember",
                                "lonely", "hope", "scared", "scariest"));
                TONE_KEYWORDS.put("Epic", Arrays.asList("destiny", "kingdom", "war", "battle", "glory", "legend",
                                "army", "throne", "empire"));
                TONE_KEYWORDS.put("Lighthearted",
                                Arrays.asList("smile", "laugh", "bright", "cheerful", "warm", "happy", "playful"));
        }

        private String detectGenre(String text) {
                String lower = text.toLowerCase();
                String best = "Drama";
                int bestScore = 0;
                for (Map.Entry<String, List<String>> e : GENRE_KEYWORDS.entrySet()) {
                        int score = 0;
                        for (String kw : e.getValue())
                                if (lower.contains(kw))
                                        score++;
                        if (score > bestScore) {
                                bestScore = score;
                                best = e.getKey();
                        }
                }
                return best;
        }

        private String detectTone(String text) {
                String lower = text.toLowerCase();
                String best = "Neutral";
                int bestScore = 0;
                for (Map.Entry<String, List<String>> e : TONE_KEYWORDS.entrySet()) {
                        int score = 0;
                        for (String kw : e.getValue())
                                if (lower.contains(kw))
                                        score++;
                        if (score > bestScore) {
                                bestScore = score;
                                best = e.getKey();
                        }
                }
                return best;
        }

        private List<String> extractEntities(String text) {
                Set<String> entities = new LinkedHashSet<>();
                Pattern pattern = Pattern.compile("(?<=[\\s\"'(\\[])([A-Z][a-z]{2,})");
                Matcher matcher = pattern.matcher(text);
                Set<String> stopWords = new HashSet<>(Arrays.asList(
                                "The", "This", "That", "Then", "They", "There", "Their", "These", "Those",
                                "When", "Where", "What", "Which", "While", "With", "After", "Before",
                                "Because", "Since", "About", "From", "Into", "Through", "During",
                                "Without", "Between", "Each", "Every", "Some", "Many", "Most", "Other",
                                "Another", "Such", "Only", "Just", "Also", "Even", "Still", "Already",
                                "Here", "Never", "Always", "Sometimes", "Perhaps", "Maybe", "However",
                                "Although", "Though", "But", "And", "For", "Not", "She", "His", "Her",
                                "Its", "Our", "Has", "Had", "Was", "Were", "Are", "Been", "Being",
                                "Have", "Did", "Does", "Could", "Would", "Should", "Must", "Shall",
                                "Will", "May", "Might", "Like", "Okay", "Either", "Every", "Outside",
                                "Inside", "Below", "Above", "Near", "Except"));
                while (matcher.find()) {
                        String word = matcher.group(1);
                        if (!stopWords.contains(word))
                                entities.add(word);
                }
                if (text.length() > 3) {
                        Pattern fp = Pattern.compile("^([A-Z][a-z]{2,})");
                        Matcher fm = fp.matcher(text.trim());
                        if (fm.find() && !stopWords.contains(fm.group(1)))
                                entities.add(fm.group(1));
                }
                return entities.stream().limit(8).collect(Collectors.toList());
        }

        private String safe(String s) {
                return s == null ? "" : s;
        }
}
