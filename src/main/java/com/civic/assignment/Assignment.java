package com.civic.assignment;

import com.civic.complaint.Complaint;
import com.civic.worker.Worker;
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

/** Links a complaint to the worker responsible for fixing it. */
@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @ManyToOne(optional = false)
    @JoinColumn(name = "worker_id")
    private Worker worker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    /** Instructions from the admin. */
    @Column(length = 1000)
    private String notes;

    /** Latest progress remarks from the worker. */
    @Column(length = 1000)
    private String workerRemarks;

    private LocalDateTime assignedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public Worker getWorker() { return worker; }
    public void setWorker(Worker worker) { this.worker = worker; }

    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getWorkerRemarks() { return workerRemarks; }
    public void setWorkerRemarks(String workerRemarks) { this.workerRemarks = workerRemarks; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
