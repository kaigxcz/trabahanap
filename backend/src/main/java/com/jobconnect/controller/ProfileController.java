package com.jobconnect.controller;

import com.jobconnect.model.Job;
import com.jobconnect.model.User;
import com.jobconnect.repository.ApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.SavedJobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;

    public ProfileController(UserRepository userRepository, ApplicationRepository applicationRepository,
                              JobRepository jobRepository, SavedJobRepository savedJobRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.savedJobRepository = savedJobRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<?> getProfile() {
        User user = getCurrentUser();
        long appCount = applicationRepository.countByUser(user);
        long savedCount = savedJobRepository.findByUser(user).size();
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                          (user.getLastName() != null ? user.getLastName() : "")).trim();
        if (fullName.isEmpty()) fullName = user.getUsername();

        return ResponseEntity.ok(Map.of(
            "fullName", fullName,
            "username", user.getUsername(),
            "email", user.getEmail() != null ? user.getEmail() : "",
            "location", user.getLocation() != null ? user.getLocation() : "",
            "jobTitle", user.getJobTitle() != null ? user.getJobTitle() : "",
            "skills", user.getSkills() != null ? user.getSkills() : "",
            "hasResume", user.getResumeFilename() != null,
            "resumeFilename", user.getResumeFilename() != null ? user.getResumeFilename() : "",
            "totalApplied", appCount,
            "totalSaved", savedCount
        ));
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body) {
        User user = getCurrentUser();
        if (body.containsKey("jobTitle")) user.setJobTitle(body.get("jobTitle"));
        if (body.containsKey("location")) user.setLocation(body.get("location"));
        if (body.containsKey("skills")) user.setSkills(body.get("skills"));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated."));
    }

    @GetMapping("/resume-score")
    public ResponseEntity<?> getResumeScore() {
        User user = getCurrentUser();
        int score = 0;
        java.util.List<String> tips = new java.util.ArrayList<>();
        java.util.List<String> strengths = new java.util.ArrayList<>();

        if (user.getFirstName() != null && user.getLastName() != null) { score += 15; strengths.add("Full name provided"); }
        else tips.add("Add your full name to your profile");

        if (user.getEmail() != null && !user.getEmail().isEmpty()) { score += 10; strengths.add("Email address on file"); }
        else tips.add("Add a contact email address");

        if (user.getLocation() != null && !user.getLocation().isEmpty()) { score += 10; strengths.add("Location specified"); }
        else tips.add("Add your location so employers can find you");

        if (user.getJobTitle() != null && !user.getJobTitle().isEmpty()) { score += 20; strengths.add("Job title / position set"); }
        else tips.add("Add your current or desired job title");

        if (user.getSkills() != null && !user.getSkills().isEmpty()) {
            String[] skillArr = user.getSkills().split(",");
            if (skillArr.length >= 5) { score += 25; strengths.add("Strong skills list (" + skillArr.length + " skills)"); }
            else { score += 15; tips.add("Add at least 5 skills to strengthen your profile"); }
        } else tips.add("Add your skills — this is the most important section for matching");

        if (user.getResumeFilename() != null) { score += 20; strengths.add("Resume uploaded"); }
        else tips.add("Upload your resume to complete your profile");

        String grade = score >= 90 ? "Excellent" : score >= 70 ? "Good" : score >= 50 ? "Fair" : "Needs Work";
        String gradeColor = score >= 90 ? "green" : score >= 70 ? "blue" : score >= 50 ? "amber" : "red";

        return ResponseEntity.ok(java.util.Map.of(
            "score", score,
            "grade", grade,
            "gradeColor", gradeColor,
            "strengths", strengths,
            "tips", tips
        ));
    }
    @GetMapping("/recommendations")
    public List<Job> getRecommendations() {
        User user = getCurrentUser();
        List<Job> allJobs = jobRepository.findAll();

        // Build a score based on skills match and saved job categories
        String skills = user.getSkills() != null ? user.getSkills().toLowerCase() : "";
        String jobTitle = user.getJobTitle() != null ? user.getJobTitle().toLowerCase() : "";

        Set<String> savedCategories = savedJobRepository.findByUser(user)
                .stream().map(s -> s.getJob().getCategory()).collect(Collectors.toSet());

        List<Long> appliedJobIds = applicationRepository.findByUser(user)
                .stream().map(a -> {
                    // match by title+company — approximate
                    return allJobs.stream()
                            .filter(j -> j.getTitle().equalsIgnoreCase(a.getJobTitle()) && j.getCompany().equalsIgnoreCase(a.getCompany()))
                            .map(Job::getId).findFirst().orElse(-1L);
                }).toList();

        return allJobs.stream()
                .filter(j -> !appliedJobIds.contains(j.getId()))
                .sorted((a, b) -> {
                    int scoreA = score(a, skills, jobTitle, savedCategories);
                    int scoreB = score(b, skills, jobTitle, savedCategories);
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(4)
                .collect(Collectors.toList());
    }

    private int score(Job job, String skills, String jobTitle, Set<String> savedCategories) {
        int s = 0;
        if (savedCategories.contains(job.getCategory())) s += 3;
        if (!skills.isEmpty()) {
            String[] skillArr = skills.split(",");
            for (String skill : skillArr) {
                String sk = skill.trim().toLowerCase();
                if (job.getRequirements() != null && job.getRequirements().toLowerCase().contains(sk)) s += 2;
                if (job.getTitle().toLowerCase().contains(sk)) s += 1;
            }
        }
        if (!jobTitle.isEmpty() && job.getTitle().toLowerCase().contains(jobTitle)) s += 2;
        return s;
    }
}
