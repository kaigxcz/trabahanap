package com.jobconnect.controller;

import com.jobconnect.model.Application;
import com.jobconnect.model.Notification;
import com.jobconnect.model.User;
import com.jobconnect.repository.ApplicationRepository;
import com.jobconnect.repository.NotificationRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public ApplicationController(ApplicationRepository applicationRepository,
                                  UserRepository userRepository,
                                  NotificationRepository notificationRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @GetMapping
    public List<Application> getMyApplications() {
        return applicationRepository.findByUser(getCurrentUser());
    }

    @PostMapping
    public ResponseEntity<?> apply(@RequestBody Map<String, String> body) {
        String jobTitle = body.get("jobTitle");
        String company = body.get("company");
        Long jobId = body.containsKey("jobId") ? Long.parseLong(body.get("jobId")) : null;
        User user = getCurrentUser();

        if (applicationRepository.existsByUserAndJobTitleAndCompany(user, jobTitle, company)) {
            return ResponseEntity.badRequest().body(Map.of("error", "You already applied for this job."));
        }

        Application app = new Application(user, jobTitle, company);
        if (jobId != null) app.setJobId(jobId);
        applicationRepository.save(app);

        notificationRepository.save(new Notification(user,
            "Your application for " + jobTitle + " at " + company + " has been submitted.", "application"));

        return ResponseEntity.ok(Map.of("message", "Application submitted successfully!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> withdraw(@PathVariable Long id) {
        User user = getCurrentUser();
        return applicationRepository.findById(id)
                .filter(app -> app.getUser().getId().equals(user.getId()))
                .map(app -> {
                    notificationRepository.save(new Notification(user,
                        "You withdrew your application for " + app.getJobTitle() + " at " + app.getCompany() + ".", "application"));
                    applicationRepository.delete(app);
                    return ResponseEntity.ok(Map.of("message", "Application withdrawn."));
                })
                .orElse(ResponseEntity.status(403).body(Map.of("error", "Not found or unauthorized.")));
    }
}
