package com.jobconnect.controller;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobconnect.model.Job;
import com.jobconnect.model.User;
import com.jobconnect.repository.ApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.MessageRepository;
import com.jobconnect.repository.NotificationRepository;
import com.jobconnect.repository.SavedJobRepository;
import com.jobconnect.repository.UserRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final SavedJobRepository savedJobRepository;

    public AdminController(UserRepository userRepository, JobRepository jobRepository,
                           ApplicationRepository applicationRepository,
                           MessageRepository messageRepository,
                           NotificationRepository notificationRepository,
                           SavedJobRepository savedJobRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.savedJobRepository = savedJobRepository;
    }

    private void requireAdmin() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByUsername(username).orElseThrow();
        if (!"ADMIN".equals(u.getRole())) throw new RuntimeException("Access denied");
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        requireAdmin();
        long totalUsers = userRepository.count();
        long candidates = userRepository.findAll().stream().filter(u -> "CANDIDATE".equals(u.getRole())).count();
        long employers = userRepository.findAll().stream().filter(u -> "EMPLOYER".equals(u.getRole())).count();
        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.findAll().stream().filter(Job::isActive).count();
        long totalApplications = applicationRepository.count();
        return ResponseEntity.ok(Map.of(
            "totalUsers", totalUsers,
            "candidates", candidates,
            "employers", employers,
            "totalJobs", totalJobs,
            "activeJobs", activeJobs,
            "totalApplications", totalApplications
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        requireAdmin();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
        List<Map<String, Object>> users = userRepository.findAll().stream().map(u -> {
            String firstName = u.getFirstName() != null ? u.getFirstName() : "";
            String middleName = u.getMiddleName() != null && !u.getMiddleName().isBlank() ? " " + u.getMiddleName() : "";
            String lastName  = u.getLastName()  != null ? u.getLastName()  : "";
            String suffix    = u.getSuffix()    != null && !u.getSuffix().isBlank() ? " " + u.getSuffix() : "";
            String fullName  = (firstName + middleName + " " + lastName + suffix).trim();
            if (fullName.isEmpty()) fullName = u.getUsername();
            Map<String, Object> map = new HashMap<>();
            map.put("id",        u.getId());
            map.put("username",  u.getUsername());
            map.put("fullName",  fullName);
            map.put("email",     u.getEmail()    != null ? u.getEmail()    : "");
            map.put("role",      u.getRole()     != null ? u.getRole()     : "CANDIDATE");
            map.put("location",  u.getLocation() != null ? u.getLocation() : "");
            map.put("jobTitle",  u.getJobTitle() != null ? u.getJobTitle() : "");
            map.put("skills",    u.getSkills()   != null ? u.getSkills()   : "");
            map.put("joinedAt",  u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : "—");
            return map;
        }).toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireAdmin();
        String newRole = body.get("role").toUpperCase();
        if (!List.of("CANDIDATE", "EMPLOYER", "ADMIN").contains(newRole))
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role."));
        return userRepository.findById(id).map(u -> {
            u.setRole(newRole);
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("message", "Role updated."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        requireAdmin();
        return userRepository.findById(id).map(u -> {
            // Cascade delete related data to avoid FK constraint errors
            applicationRepository.deleteAll(applicationRepository.findByUser(u));
            savedJobRepository.deleteAll(savedJobRepository.findByUser(u));
            notificationRepository.deleteAll(notificationRepository.findByUserOrderByCreatedAtDesc(u));
            messageRepository.deleteAll(messageRepository.findConversations(u));
            userRepository.delete(u);
            return ResponseEntity.ok(Map.of("message", "User deleted."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> getAllJobs() {
        requireAdmin();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
        return jobRepository.findAll().stream().map(j -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",          j.getId());
            map.put("title",       j.getTitle());
            map.put("company",     j.getCompany());
            map.put("location",    j.getLocation());
            map.put("salary",      j.getSalary());
            map.put("category",    j.getCategory());
            map.put("icon",        j.getIcon());
            map.put("type",        j.getType());
            map.put("experience",  j.getExperience());
            map.put("postedBy",    j.getPostedBy());
            map.put("active",      j.isActive());
            map.put("featured",    j.isFeatured());
            map.put("description", j.getDescription());
            map.put("requirements",j.getRequirements());
            map.put("postedAt",    j.getPostedAt() != null ? j.getPostedAt().format(fmt) : "—");
            return map;
        }).toList();
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        requireAdmin();
        return jobRepository.findById(id).map(j -> {
            jobRepository.delete(j);
            return ResponseEntity.ok(Map.of("message", "Job deleted."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/jobs/{id}/toggle")
    public ResponseEntity<?> toggleJob(@PathVariable Long id) {
        requireAdmin();
        return jobRepository.findById(id).map(j -> {
            j.setActive(!j.isActive());
            jobRepository.save(j);
            return ResponseEntity.ok(Map.of("active", j.isActive()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/jobs/{id}/feature")
    public ResponseEntity<?> featureJob(@PathVariable Long id) {
        requireAdmin();
        return jobRepository.findById(id).map(j -> {
            j.setFeatured(!j.isFeatured());
            jobRepository.save(j);
            return ResponseEntity.ok(Map.of("featured", j.isFeatured()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/export/users")
    public ResponseEntity<String> exportUsers() {
        requireAdmin();
        StringBuilder csv = new StringBuilder("ID,Full Name,Username,Email,Role,Location\n");
        userRepository.findAll().forEach(u -> {
            String mn = u.getMiddleName() != null && !u.getMiddleName().isBlank() ? " " + u.getMiddleName() : "";
            csv.append(String.format("%d,\"%s%s %s\",%s,%s,%s,%s\n",
                u.getId(),
                u.getFirstName() != null ? u.getFirstName() : "",
                mn,
                u.getLastName() != null ? u.getLastName() : "",
                u.getUsername(),
                u.getEmail() != null ? u.getEmail() : "",
                u.getRole() != null ? u.getRole() : "CANDIDATE",
                u.getLocation() != null ? u.getLocation() : ""));
        });
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=users.csv")
            .body(csv.toString());
    }

    @GetMapping("/export/jobs")
    public ResponseEntity<String> exportJobs() {
        requireAdmin();
        StringBuilder csv = new StringBuilder("ID,Title,Company,Location,Category,Type,Salary,Active,Featured,Posted By\n");
        jobRepository.findAll().forEach(j -> csv.append(String.format("%d,\"%s\",\"%s\",%s,%s,%s,%s,%s,%s,%s\n",
            j.getId(), j.getTitle(), j.getCompany(), j.getLocation(),
            j.getCategory(), j.getType(), j.getSalary(),
            j.isActive(), j.isFeatured(),
            j.getPostedBy() != null ? j.getPostedBy() : "system")));
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=jobs.csv")
            .body(csv.toString());
    }
}
