package com.civic.complaint;

import com.civic.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A civic issue reported by a citizen. */
@Entity
@Table(name = "complaints")
public class Complaint {

    /** PENDING -> ASSIGNED -> IN_PROGRESS -> RESOLVED, or REJECTED by an admin. */
    public enum Status {
        PENDING, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 60)
    private String category;

    /** The category the AI classifier suggested (kept even if the citizen picked another one). */
    @Column(length = 60)
    private String aiCategory;

    /** HIGH, MEDIUM or LOW */
    @Column(nullable = false, length = 10)
    private String priority;

    @Column(nullable = false, length = 255)
    private String location;

    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(length = 1000)
    private String adminRemarks;

    @ManyToOne(optional = false)
    @JoinColumn(name = "citizen_id")
    private User citizen;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAiCategory() { return aiCategory; }
    public void setAiCategory(String aiCategory) { this.aiCategory = aiCategory; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getAdminRemarks() { return adminRemarks; }
    public void setAdminRemarks(String adminRemarks) { this.adminRemarks = adminRemarks; }

    public User getCitizen() { return citizen; }
    public void setCitizen(User citizen) { this.citizen = citizen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
