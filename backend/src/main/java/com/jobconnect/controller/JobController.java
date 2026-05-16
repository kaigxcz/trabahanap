package com.jobconnect.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobconnect.model.Job;
import com.jobconnect.repository.ApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public JobController(JobRepository jobRepository, UserRepository userRepository, ApplicationRepository applicationRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/stats")
    public Map<String, Long> getPublicStats() {
        long activeJobs = jobRepository.findAll().stream().filter(Job::isActive).count();
        long companies  = userRepository.findAll().stream().filter(u -> "EMPLOYER".equals(u.getRole())).count();
        long hired      = applicationRepository.findAll().stream()
                            .filter(a -> "ACCEPTED".equalsIgnoreCase(a.getStatus())).count();
        return Map.of("activeJobs", activeJobs, "companies", companies, "hired", hired);
    }

    @GetMapping
    public List<Job> getJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String category) {

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword;
        String loc = (location == null || location.isBlank()) ? null : location;
        String cat = (category == null || category.equalsIgnoreCase("all")) ? null : category;

        List<Job> jobs = jobRepository.search(kw, loc, cat);
        // Featured jobs first
        jobs.sort((a, b) -> Boolean.compare(b.isFeatured(), a.isFeatured()));
        return jobs;
    }
}
