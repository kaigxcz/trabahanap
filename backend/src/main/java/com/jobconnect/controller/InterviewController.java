package com.jobconnect.controller;

import com.jobconnect.model.*;
import com.jobconnect.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public InterviewController(InterviewRepository interviewRepository,
                                ApplicationRepository applicationRepository,
                                UserRepository userRepository,
                                NotificationRepository notificationRepository) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    // Employer schedules interview
    @PostMapping
    public ResponseEntity<?> schedule(@RequestBody Map<String, String> body) {
        Long appId = Long.parseLong(body.get("applicationId"));
        Application app = applicationRepository.findById(appId).orElseThrow();
        User candidate = app.getUser();

        LocalDateTime scheduledAt = LocalDateTime.parse(body.get("scheduledAt"));

        Interview interview = new Interview(app, candidate,
            app.getJobTitle(), app.getCompany(),
            scheduledAt,
            body.getOrDefault("location", "Online"),
            body.getOrDefault("meetingLink", ""),
            body.getOrDefault("notes", ""));

        interviewRepository.save(interview);

        // Update application status
        app.setStatus("Interview");
        applicationRepository.save(app);

        // Notify candidate
        notificationRepository.save(new Notification(candidate,
            "Interview scheduled for " + app.getJobTitle() + " at " + app.getCompany() +
            " on " + scheduledAt.toLocalDate() + " at " + scheduledAt.toLocalTime(), "application"));

        return ResponseEntity.ok(Map.of("message", "Interview scheduled!", "id", interview.getId()));
    }

    // Candidate views their interviews
    @GetMapping("/mine")
    public List<Map<String, Object>> getMyInterviews() {
        User user = getCurrentUser();
        return interviewRepository.findByCandidate(user).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("jobTitle", i.getJobTitle());
            m.put("company", i.getCompany());
            m.put("scheduledAt", i.getScheduledAt());
            m.put("location", i.getLocation());
            m.put("meetingLink", i.getMeetingLink());
            m.put("notes", i.getNotes());
            m.put("status", i.getStatus());
            return m;
        }).toList();
    }

    // Employer views interviews for a job
    @GetMapping("/job/{jobId}")
    public List<Map<String, Object>> getJobInterviews(@PathVariable Long jobId) {
        return interviewRepository.findByApplication_JobId(jobId).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("candidateName", i.getCandidate().getFirstName() + " " + i.getCandidate().getLastName());
            m.put("jobTitle", i.getJobTitle());
            m.put("scheduledAt", i.getScheduledAt());
            m.put("location", i.getLocation());
            m.put("status", i.getStatus());
            return m;
        }).toList();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return interviewRepository.findById(id).map(i -> {
            i.setStatus(body.get("status"));
            interviewRepository.save(i);
            return ResponseEntity.ok(Map.of("message", "Status updated."));
        }).orElse(ResponseEntity.notFound().build());
    }
}
