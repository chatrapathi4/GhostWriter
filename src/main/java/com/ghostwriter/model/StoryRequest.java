package com.ghostwriter.model;

public class StoryRequest {

    private String fullContext;
    private String shortMemory;
    private String lastParagraph;

    public StoryRequest() {}

    public StoryRequest(String fullContext, String shortMemory, String lastParagraph) {
        this.fullContext = fullContext;
        this.shortMemory = shortMemory;
        this.lastParagraph = lastParagraph;
    }

    public String getFullContext() { return fullContext; }
    public void setFullContext(String fullContext) { this.fullContext = fullContext; }

    public String getShortMemory() { return shortMemory; }
    public void setShortMemory(String shortMemory) { this.shortMemory = shortMemory; }

    public String getLastParagraph() { return lastParagraph; }
    public void setLastParagraph(String lastParagraph) { this.lastParagraph = lastParagraph; }
}
