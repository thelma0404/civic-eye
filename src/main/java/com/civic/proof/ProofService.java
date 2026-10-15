package com.civic.proof;

import com.civic.assignment.Assignment;
import com.civic.assignment.AssignmentService;
import com.civic.assignment.AssignmentStatus;
import com.civic.common.util.FileUploadUtil;
import com.civic.common.util.TextUtil;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProofService {

    private final ProofRepository proofRepository;
    private final AssignmentService assignmentService;
    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;
    private final FileUploadUtil fileUploadUtil;

    public ProofService(ProofRepository proofRepository, AssignmentService assignmentService,
                        ComplaintRepository complaintRepository, NotificationService notificationService,
                        FileUploadUtil fileUploadUtil) {
        this.proofRepository = proofRepository;
        this.assignmentService = assignmentService;
        this.complaintRepository = complaintRepository;
        this.notificationService = notificationService;
        this.fileUploadUtil = fileUploadUtil;
    }

    /** Worker uploads a photo of the finished work. The assignment is completed and waits for admin review. */
    @Transactional
    public WorkProof uploadProof(Long workerId, Long assignmentId, MultipartFile file, String remarks) {
        Assignment assignment = assignmentService.getWorkerAssignment(workerId, assignmentId);
        if (assignment.getStatus() == AssignmentStatus.CANCELLED) {
            throw new IllegalArgumentException("This assignment was withdrawn, so proof can't be uploaded");
        }
        if (proofRepository.existsByAssignmentIdAndStatus(assignmentId, ProofStatus.PENDING)) {
            throw new IllegalArgumentException("You already submitted proof for this complaint. Please wait for the admin to review it.");
        }
        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            throw new IllegalArgumentException("This work has already been completed and approved");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please attach a photo of the completed work");
        }
        String path = fileUploadUtil.saveFile(file, "work-proof");

        WorkProof proof = new WorkProof();
        proof.setAssignment(assignment);
        proof.setImagePath(path);
        proof.setRemarks(TextUtil.clean(remarks));
        proof.setStatus(ProofStatus.PENDING);
        WorkProof saved = proofRepository.save(proof);

        assignmentService.markCompleted(assignment);
        Complaint complaint = assignment.getComplaint();
        if (complaint.getStatus() != Complaint.Status.IN_PROGRESS) {
            complaint.setStatus(Complaint.Status.IN_PROGRESS);
            complaintRepository.save(complaint);
        }
        notificationService.notifyAdmins("Proof submitted for #" + complaint.getId(),
                assignment.getWorker().getName() + " uploaded proof of work for \"" + complaint.getTitle()
                        + "\". Review it to close the complaint.");
        notificationService.notifyCitizen(complaint.getCitizen().getId(), "Work completed",
                "Work on your complaint #" + complaint.getId() + " is done and is being verified.");
        return saved;
    }

    /** Admin approves (complaint resolved) or rejects (work goes back to the worker) a proof. */
    @Transactional
    public WorkProof reviewProof(Long proofId, boolean approve, String comment) {
        WorkProof proof = proofRepository.findById(proofId).orElseThrow(() -> new IllegalArgumentException("Proof not found"));
        if (proof.getStatus() != ProofStatus.PENDING) {
            throw new IllegalArgumentException("This proof has already been reviewed");
        }
        Assignment assignment = proof.getAssignment();
        Complaint complaint = assignment.getComplaint();
        if (assignment.getStatus() == AssignmentStatus.CANCELLED
                || complaint.getStatus() == Complaint.Status.RESOLVED
                || complaint.getStatus() == Complaint.Status.REJECTED) {
            throw new IllegalArgumentException("This complaint is already closed");
        }
        String note = TextUtil.clean(comment);
        if (!approve && note == null) {
            throw new IllegalArgumentException("Please tell the worker why the proof was rejected");
        }
        LocalDateTime now = LocalDateTime.now();
        proof.setStatus(approve ? ProofStatus.APPROVED : ProofStatus.REJECTED);
        proof.setReviewComment(note);
        proof.setReviewedAt(now);

        Long workerId = assignment.getWorker().getId();
        if (approve) {
            complaint.setStatus(Complaint.Status.RESOLVED);
            complaint.setResolvedAt(now);
            notificationService.notifyCitizen(complaint.getCitizen().getId(), "Complaint resolved",
                    "Your complaint #" + complaint.getId() + " has been resolved. Please rate the service.");
            notificationService.notifyWorker(workerId, "Proof approved",
                    "Great work! Your proof for complaint #" + complaint.getId() + " was approved.");
        } else {
            assignmentService.reopen(assignment);
            complaint.setStatus(Complaint.Status.IN_PROGRESS);
            notificationService.notifyWorker(workerId, "Proof rejected",
                    "Your proof for complaint #" + complaint.getId() + " was not accepted: " + note
                            + ". Please fix it and upload new proof.");
        }
        complaintRepository.save(complaint);
        return proofRepository.save(proof);
    }

    /** Closes proofs still waiting for review when an admin closes or reopens a complaint manually. */
    @Transactional
    public void closePendingProofs(Long complaintId, String note) {
        for (WorkProof proof : proofRepository.findByAssignmentComplaintIdOrderByUploadedAtDesc(complaintId)) {
            if (proof.getStatus() == ProofStatus.PENDING) {
                proof.setStatus(ProofStatus.REJECTED);
                proof.setReviewComment(note);
                proof.setReviewedAt(LocalDateTime.now());
                proofRepository.save(proof);
            }
        }
    }

    public List<WorkProof> getProofsForAssignment(Long assignmentId) {
        return proofRepository.findByAssignmentIdOrderByUploadedAtDesc(assignmentId);
    }

    public List<WorkProof> getProofsForComplaint(Long complaintId) {
        return proofRepository.findByAssignmentComplaintIdOrderByUploadedAtDesc(complaintId);
    }

    public List<WorkProof> getPendingProofs() {
        return proofRepository.findByStatusOrderByUploadedAtAsc(ProofStatus.PENDING);
    }

    public long countForWorker(Long workerId, ProofStatus status) {
        return proofRepository.countByAssignmentWorkerIdAndStatus(workerId, status);
    }
}
