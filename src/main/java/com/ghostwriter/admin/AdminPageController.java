package com.ghostwriter.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }

    @GetMapping("/admin/users")
    public String adminUsersPage() {
        return "admin-users";
    }

    @GetMapping("/admin/review")
    public String adminReviewPage() {
        return "admin";
    }
}
