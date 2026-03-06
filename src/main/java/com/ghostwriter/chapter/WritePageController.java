package com.ghostwriter.chapter;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WritePageController {

    @GetMapping("/write")
    public String writePage() {
        return "write";
    }

    @GetMapping("/write/{id}")
    public String editStoryPage(@PathVariable String id) {
        return "write";
    }
}
