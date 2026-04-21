package com.jobconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    private String jobTitle;
    private String company;
    private LocalDateTime scheduledAt;
    private String location; // "Online" or address
    private String meetingLink;
    private String notes;
    private String status; // SCHEDULED, COMPLETED, CANCELLED

    public Interview() {}

    public Interview(Application application, User candidate, String jobTitle, String company,
                     LocalDateTime scheduledAt, String location, String meetingLink, String notes) {
        this.application = application;
        this.candidate = candidate;
        this.jobTitle = jobTitle;
        this.company = company;
        this.scheduledAt = scheduledAt;
        this.location = location;
        this.meetingLink = meetingLink;
        this.notes = notes;
        this.status = "SCHEDULED";
    }

    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public User getCandidate() { return candidate; }
    public String getJobTitle() { return jobTitle; }
    public String getCompany() { return company; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getLocation() { return location; }
    public String getMeetingLink() { return meetingLink; }
    public String getNotes() { return notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public void setLocation(String location) { this.location = location; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }
    public void setNotes(String notes) { this.notes = notes; }
}
