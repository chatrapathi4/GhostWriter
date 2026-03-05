package com.ghostwriter.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/stories")
    public String stories() {
        return "stories";
    }

    @GetMapping("/story/{id}")
    public String storyView(@PathVariable String id) {
        return "story";
    }
}
