package com.jobconnect.controller;

import com.jobconnect.model.Application;
import com.jobconnect.model.Job;
import com.jobconnect.model.Notification;
import com.jobconnect.model.User;
import com.jobconnect.repository.ApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.NotificationRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/employer")
public class EmployerController {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public EmployerController(JobRepository jobRepository, ApplicationRepository applicationRepository,
                               UserRepository userRepository, NotificationRepository notificationRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    private void requireEmployer() {
        User u = getCurrentUser();
        if (!"EMPLOYER".equals(u.getRole()) && !"ADMIN".equals(u.getRole()))
            throw new RuntimeException("Access denied");
    }

    // Post a new job
    @PostMapping("/jobs")
    public ResponseEntity<?> postJob(@RequestBody Map<String, String> body) {
        requireEmployer();
        User employer = getCurrentUser();
        Job job = new Job(
            body.get("title"), body.get("company"), body.get("location"),
            body.get("salary"), body.get("category"), body.getOrDefault("icon", "💼"),
            body.get("type"), body.getOrDefault("experience", "Any"),
            body.get("description"), body.get("requirements")
        );
        job.setPostedBy(employer.getUsername());
        jobRepository.save(job);
        return ResponseEntity.ok(Map.of("message", "Job posted successfully!", "id", job.getId()));
    }

    // Get employer's own jobs
    @GetMapping("/jobs")
    public List<Job> getMyJobs() {
        requireEmployer();
        return jobRepository.findByPostedBy(getCurrentUser().getUsername());
    }

    // Update a job
    @PutMapping("/jobs/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireEmployer();
        User employer = getCurrentUser();
        return jobRepository.findById(id)
            .filter(j -> j.getPostedBy().equals(employer.getUsername()))
            .map(j -> {
                if (body.containsKey("title")) j.setTitle(body.get("title"));
                if (body.containsKey("company")) j.setCompany(body.get("company"));
                if (body.containsKey("location")) j.setLocation(body.get("location"));
                if (body.containsKey("salary")) j.setSalary(body.get("salary"));
                if (body.containsKey("type")) j.setType(body.get("type"));
                if (body.containsKey("description")) j.setDescription(body.get("description"));
                if (body.containsKey("requirements")) j.setRequirements(body.get("requirements"));
                if (body.containsKey("active")) j.setActive(Boolean.parseBoolean(body.get("active")));
                jobRepository.save(j);
                return ResponseEntity.ok(Map.of("message", "Job updated."));
            }).orElse(ResponseEntity.status(403).body(Map.of("error", "Not found or unauthorized.")));
    }

    // Delete a job
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        requireEmployer();
        User employer = getCurrentUser();
        return jobRepository.findById(id)
            .filter(j -> j.getPostedBy().equals(employer.getUsername()))
            .map(j -> {
                jobRepository.delete(j);
                return ResponseEntity.ok(Map.of("message", "Job deleted."));
            }).orElse(ResponseEntity.status(403).body(Map.of("error", "Not found or unauthorized.")));
    }

    // Get applicants for a job (with optional blind mode)
    @GetMapping("/jobs/{id}/applicants")
    public ResponseEntity<?> getApplicants(@PathVariable Long id,
                                            @RequestParam(defaultValue = "false") boolean blind) {
        requireEmployer();
        List<Application> apps = applicationRepository.findByJobId(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Application app : apps) {
            User candidate = app.getUser();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("applicationId", app.getId());
            entry.put("status", app.getStatus());
            entry.put("appliedAt", app.getAppliedAt());
            entry.put("jobTitle", app.getJobTitle());
            if (blind) {
                entry.put("name", "Candidate #" + app.getId());
                entry.put("email", "hidden");
                entry.put("location", candidate.getLocation() != null ? candidate.getLocation() : "");
            } else {
                entry.put("name", candidate.getFirstName() + " " + candidate.getLastName());
                entry.put("email", candidate.getEmail() != null ? candidate.getEmail() : "");
                entry.put("location", candidate.getLocation() != null ? candidate.getLocation() : "");
            }
            entry.put("skills", candidate.getSkills() != null ? candidate.getSkills() : "");
            entry.put("jobTitle_candidate", candidate.getJobTitle() != null ? candidate.getJobTitle() : "");
            entry.put("hasResume", candidate.getResumeFilename() != null);
            entry.put("employerNote", app.getEmployerNote() != null ? app.getEmployerNote() : "");
            // Match score based on skills overlap
            entry.put("matchScore", calcMatchScore(candidate, id));
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    // Update application status (Kanban)
    @PutMapping("/applications/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireEmployer();
        String newStatus = body.get("status");
        List<String> valid = List.of("Applied", "Under Review", "Interview", "Offer", "Rejected");
        if (!valid.contains(newStatus))
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status."));

        return applicationRepository.findById(id).map(app -> {
            app.setStatus(newStatus);
            applicationRepository.save(app);
            notificationRepository.save(new Notification(app.getUser(),
                "Your application for " + app.getJobTitle() + " is now: " + newStatus, "application"));
            return ResponseEntity.ok(Map.of("message", "Status updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Save employer note on applicant
    @PutMapping("/applications/{id}/note")
    public ResponseEntity<?> saveNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireEmployer();
        return applicationRepository.findById(id).map(app -> {
            app.setEmployerNote(body.get("note"));
            applicationRepository.save(app);
            return ResponseEntity.ok(Map.of("message", "Note saved."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Employer stats
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        requireEmployer();
        String username = getCurrentUser().getUsername();
        List<Job> jobs = jobRepository.findByPostedBy(username);
        long totalApplicants = jobs.stream().mapToLong(j -> applicationRepository.countByJobId(j.getId())).sum();
        return ResponseEntity.ok(Map.of(
            "totalJobs", jobs.size(),
            "activeJobs", jobs.stream().filter(Job::isActive).count(),
            "totalApplicants", totalApplicants
        ));
    }

    // Analytics per job
    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        requireEmployer();
        String username = getCurrentUser().getUsername();
        List<Job> jobs = jobRepository.findByPostedBy(username);
        List<Map<String, Object>> data = jobs.stream().map(j -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("jobTitle", j.getTitle());
            m.put("company", j.getCompany());
            m.put("applicants", applicationRepository.countByJobId(j.getId()));
            m.put("active", j.isActive());
            return m;
        }).toList();
        return ResponseEntity.ok(data);
    }

    private int calcMatchScore(User candidate, Long jobId) {
        if (candidate.getSkills() == null) return 0;
        return jobRepository.findById(jobId).map(job -> {
            if (job.getRequirements() == null) return 0;
            String[] reqs = job.getRequirements().toLowerCase().split(",");
            String[] skills = candidate.getSkills().toLowerCase().split(",");
            long matches = Arrays.stream(skills)
                .filter(s -> Arrays.stream(reqs).anyMatch(r -> r.trim().contains(s.trim()) || s.trim().contains(r.trim())))
                .count();
            return (int) Math.min(100, (matches * 100) / Math.max(reqs.length, 1));
        }).orElse(0);
    }
}
