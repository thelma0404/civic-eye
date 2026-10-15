package com.civic.proof;

import com.civic.assignment.Assignment;
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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A photo a worker uploads to show the work is done. An admin approves or rejects it. */
@Entity
@Table(name = "work_proofs")
public class WorkProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @Column(nullable = false)
    private String imagePath;

    @Column(length = 1000)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProofStatus status = ProofStatus.PENDING;

    @Column(length = 1000)
    private String reviewComment;

    private LocalDateTime uploadedAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public ProofStatus getStatus() { return status; }
    public void setStatus(ProofStatus status) { this.status = status; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
