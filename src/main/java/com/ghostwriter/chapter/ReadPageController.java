package com.ghostwriter.chapter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ReadPageController {

    @GetMapping("/read/{storyId}/{chapterNumber}")
    public String readChapter(@PathVariable String storyId, @PathVariable int chapterNumber) {
        return "read";
    }
}
