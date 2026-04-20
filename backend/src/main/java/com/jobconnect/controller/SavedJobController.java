package com.jobconnect.controller;

import com.jobconnect.model.Job;
import com.jobconnect.model.SavedJob;
import com.jobconnect.model.User;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.SavedJobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/saved")
public class SavedJobController {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public SavedJobController(SavedJobRepository savedJobRepository, JobRepository jobRepository, UserRepository userRepository) {
        this.savedJobRepository = savedJobRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @GetMapping
    public List<Job> getSavedJobs() {
        return savedJobRepository.findByUser(getCurrentUser())
                .stream().map(SavedJob::getJob).toList();
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<?> toggleSave(@PathVariable Long jobId) {
        User user = getCurrentUser();
        Job job = jobRepository.findById(jobId).orElseThrow();
        Optional<SavedJob> existing = savedJobRepository.findByUserAndJob(user, job);
        if (existing.isPresent()) {
            savedJobRepository.delete(existing.get());
            return ResponseEntity.ok(Map.of("saved", false, "message", "Job removed from saved."));
        } else {
            savedJobRepository.save(new SavedJob(user, job));
            return ResponseEntity.ok(Map.of("saved", true, "message", "Job saved!"));
        }
    }

    @GetMapping("/ids")
    public List<Long> getSavedJobIds() {
        return savedJobRepository.findByUser(getCurrentUser())
                .stream().map(s -> s.getJob().getId()).toList();
    }
}
