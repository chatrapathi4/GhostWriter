package com.ghostwriter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GhostWriterResponse {

    @JsonProperty("genre_detected")
    private String genreDetected;

    @JsonProperty("tone_detected")
    private String toneDetected;

    @JsonProperty("key_entities")
    private List<String> keyEntities;

    @JsonProperty("narrative_bridge")
    private String narrativeBridge;

    @JsonProperty("directions")
    private List<Direction> directions;

    @JsonProperty("source")
    private String source;

    public GhostWriterResponse() {
    }

    // ─── Direction inner class ───
    public static class Direction {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        public Direction() {
        }

        public Direction(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // ─── Getters & Setters ───
    public String getGenreDetected() {
        return genreDetected;
    }

    public void setGenreDetected(String g) {
        this.genreDetected = g;
    }

    public String getToneDetected() {
        return toneDetected;
    }

    public void setToneDetected(String t) {
        this.toneDetected = t;
    }

    public List<String> getKeyEntities() {
        return keyEntities;
    }

    public void setKeyEntities(List<String> e) {
        this.keyEntities = e;
    }

    public String getNarrativeBridge() {
        return narrativeBridge;
    }

    public void setNarrativeBridge(String n) {
        this.narrativeBridge = n;
    }

    public List<Direction> getDirections() {
        return directions;
    }

    public void setDirections(List<Direction> d) {
        this.directions = d;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String s) {
        this.source = s;
    }
}
