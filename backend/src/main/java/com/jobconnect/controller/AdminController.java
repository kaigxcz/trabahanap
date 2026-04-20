package com.jobconnect.controller;

import com.jobconnect.model.Job;
import com.jobconnect.model.User;
import com.jobconnect.repository.ApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public AdminController(UserRepository userRepository, JobRepository jobRepository,
                           ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
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
        List<Map<String, Object>> users = userRepository.findAll().stream().map(u -> Map.<String, Object>of(
            "id", u.getId(),
            "username", u.getUsername(),
            "fullName", u.getFirstName() + " " + u.getLastName(),
            "email", u.getEmail() != null ? u.getEmail() : "",
            "role", u.getRole() != null ? u.getRole() : "CANDIDATE",
            "location", u.getLocation() != null ? u.getLocation() : ""
        )).toList();
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
            userRepository.delete(u);
            return ResponseEntity.ok(Map.of("message", "User deleted."));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs")
    public List<Job> getAllJobs() {
        requireAdmin();
        return jobRepository.findAll();
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
        userRepository.findAll().forEach(u -> csv.append(String.format("%d,\"%s %s\",%s,%s,%s,%s\n",
            u.getId(),
            u.getFirstName() != null ? u.getFirstName() : "",
            u.getLastName() != null ? u.getLastName() : "",
            u.getUsername(),
            u.getEmail() != null ? u.getEmail() : "",
            u.getRole() != null ? u.getRole() : "CANDIDATE",
            u.getLocation() != null ? u.getLocation() : "")));
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
