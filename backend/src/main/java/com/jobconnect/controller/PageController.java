package com.jobconnect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String landing() { return "forward:/landing.html"; }

    @GetMapping("/login")
    public String login() { return "forward:/index.html"; }

    @GetMapping("/register")
    public String register() { return "forward:/index.html"; }

    @GetMapping("/dashboard")
    public String dashboard() { return "forward:/dashboard.html"; }

    @GetMapping("/profile")
    public String profile() { return "forward:/profile.html"; }

    @GetMapping("/applications")
    public String applications() { return "forward:/applications.html"; }

    @GetMapping("/saved")
    public String saved() { return "forward:/saved.html"; }

    @GetMapping("/employer")
    public String employer() { return "forward:/employer.html"; }

    @GetMapping("/admin")
    public String admin() { return "forward:/admin.html"; }

    @GetMapping("/tips")
    public String tips() { return "forward:/tips.html"; }
}
