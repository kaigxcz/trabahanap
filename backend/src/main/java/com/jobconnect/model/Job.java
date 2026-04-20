package com.jobconnect.model;

import jakarta.persistence.*;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String company;
    private String location;
    private String salary;
    private String category;
    private String icon;
    private String type;
    private String experience;
    private String postedBy; // employer username
    private boolean active = true;
    private boolean featured = false;

    @Column(name = "posted_at")
    private java.time.LocalDateTime postedAt = java.time.LocalDateTime.now();

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String requirements;

    public Job() {}

    public Job(String title, String company, String location, String salary,
               String category, String icon, String type, String experience,
               String description, String requirements) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
        this.category = category;
        this.icon = icon;
        this.type = type;
        this.experience = experience;
        this.description = description;
        this.requirements = requirements;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public java.time.LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(java.time.LocalDateTime postedAt) { this.postedAt = postedAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
}
