package com.jobconnect.controller;

import com.jobconnect.model.Notification;
import com.jobconnect.model.User;
import com.jobconnect.repository.NotificationRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public ResumeController(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) throws IOException {
        User user = getCurrentUser();

        String originalName = file.getOriginalFilename();
        if (originalName == null || (!originalName.endsWith(".pdf") && !originalName.endsWith(".docx"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only PDF or DOCX files are allowed."));
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "File size must be under 5MB."));
        }

        Path uploadDir = Paths.get("uploads");
        Files.createDirectories(uploadDir);

        String filename = "resume_" + user.getUsername() + "_" + System.currentTimeMillis()
                + originalName.substring(originalName.lastIndexOf('.'));
        Path dest = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        user.setResumeFilename(filename);
        userRepository.save(user);

        notificationRepository.save(new Notification(user, "Your resume has been uploaded successfully.", "system"));

        return ResponseEntity.ok(Map.of("message", "Resume uploaded!", "filename", filename));
    }

    @GetMapping("/info")
    public ResponseEntity<?> getResumeInfo() {
        User user = getCurrentUser();
        String filename = user.getResumeFilename();
        if (filename == null) return ResponseEntity.ok(Map.of("hasResume", false));
        return ResponseEntity.ok(Map.of("hasResume", true, "filename", filename));
    }
}
