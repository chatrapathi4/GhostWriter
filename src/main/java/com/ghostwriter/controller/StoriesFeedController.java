package com.ghostwriter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StoriesFeedController {

    @GetMapping("/feed")
    public String feedPage() {
        return "feed";
    }

    @GetMapping("/story-view/{id}")
    public String storyChaptersPage(@PathVariable String id) {
        return "story-chapters";
    }
}
