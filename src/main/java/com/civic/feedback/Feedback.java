package com.civic.feedback;

import com.civic.complaint.Complaint;
import com.civic.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A citizen's 1-5 star rating of how a resolved complaint was handled. */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @ManyToOne(optional = false)
    @JoinColumn(name = "citizen_id")
    private User citizen;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comments;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public User getCitizen() { return citizen; }
    public void setCitizen(User citizen) { this.citizen = citizen; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
