package com.jobconnect.controller;

import com.jobconnect.model.Job;
import com.jobconnect.repository.JobRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
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
