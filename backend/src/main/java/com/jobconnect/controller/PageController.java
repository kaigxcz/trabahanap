package com.jobconnect.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // No-cache headers on every protected page so browser never stores them
    private void noCache(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Expires", "0");
    }

    @GetMapping("/")
    public String landing() { return "forward:/landing.html"; }

    @GetMapping("/login")
    public String login() { return "forward:/login.html"; }

    @GetMapping("/register")
    public String register() { return "forward:/login.html"; }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletResponse res) { noCache(res); return "forward:/dashboard.html"; }

    @GetMapping("/profile")
    public String profile(HttpServletResponse res) { noCache(res); return "forward:/profile.html"; }

    @GetMapping("/applications")
    public String applications(HttpServletResponse res) { noCache(res); return "forward:/applications.html"; }

    @GetMapping("/saved")
    public String saved(HttpServletResponse res) { noCache(res); return "forward:/saved.html"; }

    @GetMapping("/employer")
    public String employer(HttpServletResponse res) { noCache(res); return "forward:/employer.html"; }

    @GetMapping("/admin")
    public String admin(HttpServletResponse res) { noCache(res); return "forward:/admin.html"; }

    @GetMapping("/tips")
    public String tips() { return "forward:/tips.html"; }

    @GetMapping("/messages")
    public String messages(HttpServletResponse res) { noCache(res); return "forward:/messages.html"; }
}
