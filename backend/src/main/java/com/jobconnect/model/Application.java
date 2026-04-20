package com.jobconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String jobTitle;
    private String company;
    private String status;
    private Long jobId;
    private LocalDateTime appliedAt;

    @Column(length = 1000)
    private String employerNote;

    public Application() {}

    public Application(User user, String jobTitle, String company) {
        this.user = user;
        this.jobTitle = jobTitle;
        this.company = company;
        this.status = "Applied";
        this.appliedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public String getEmployerNote() { return employerNote; }
    public void setEmployerNote(String employerNote) { this.employerNote = employerNote; }
}
