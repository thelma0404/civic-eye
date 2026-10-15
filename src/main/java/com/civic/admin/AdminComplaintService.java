package com.civic.admin;

import com.civic.admin.dto.ComplaintStatusDTO;
import com.civic.assignment.Assignment;
import com.civic.assignment.AssignmentService;
import com.civic.assignment.AssignmentStatus;
import com.civic.common.util.TextUtil;
import com.civic.complaint.AiCategoryService;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.feedback.FeedbackService;
import com.civic.notification.NotificationService;
import com.civic.proof.ProofService;
import com.civic.worker.WorkerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AdminComplaintService {

    private static final Set<String> PRIORITIES = Set.of("HIGH", "MEDIUM", "LOW");

    private final ComplaintRepository complaintRepository;
    private final AssignmentService assignmentService;
    private final ProofService proofService;
    private final FeedbackService feedbackService;
    private final WorkerService workerService;
    private final NotificationService notificationService;
    private final AiCategoryService aiCategoryService;

    public AdminComplaintService(ComplaintRepository complaintRepository, AssignmentService assignmentService,
                                 ProofService proofService, FeedbackService feedbackService, WorkerService workerService,
                                 NotificationService notificationService, AiCategoryService aiCategoryService) {
        this.complaintRepository = complaintRepository;
        this.assignmentService = assignmentService;
        this.proofService = proofService;
        this.feedbackService = feedbackService;
        this.workerService = workerService;
        this.notificationService = notificationService;
        this.aiCategoryService = aiCategoryService;
    }

    public List<Complaint> search(String status, String category, String priority, String query) {
        String q = TextUtil.clean(query) == null ? null : query.trim().toLowerCase(Locale.ROOT);
        String st = TextUtil.clean(status);
        String cat = TextUtil.clean(category);
        String pr = TextUtil.clean(priority);
        return complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> st == null || c.getStatus().name().equalsIgnoreCase(st))
                .filter(c -> cat == null || c.getCategory().equalsIgnoreCase(cat))
                .filter(c -> pr == null || c.getPriority().equalsIgnoreCase(pr))
                .filter(c -> q == null || (c.getId() + " " + c.getTitle() + " " + c.getLocation() + " "
                        + c.getCitizen().getName() + " " + c.getDescription()).toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    public Map<String, Object> getDetails(Long id) {
        Complaint complaint = getComplaint(id);
        List<Assignment> assignments = assignmentService.getAssignmentsForComplaint(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("complaint", complaint);
        data.put("assignments", assignments);
        data.put("activeAssignment", assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED || a.getStatus() == AssignmentStatus.IN_PROGRESS)
                .findFirst().orElse(null));
        data.put("proofs", proofService.getProofsForComplaint(id));
        data.put("feedback", feedbackService.getForComplaint(id).orElse(null));
        data.put("workers", workerService.getAssignableWorkers());
        data.put("categories", aiCategoryService.categories());
        return data;
    }

    @Transactional
    public Complaint updateStatus(Long id, ComplaintStatusDTO dto) {
        if (dto == null || dto.status() == null) {
            throw new IllegalArgumentException("Please choose a status");
        }
        Complaint complaint = getComplaint(id);
        String remarks = TextUtil.clean(dto.remarks());
        Long citizenId = complaint.getCitizen().getId();
        switch (dto.status()) {
            case REJECTED -> {
                if (remarks == null) {
                    throw new IllegalArgumentException("Please add a reason so the citizen knows why it was rejected");
                }
                assignmentService.cancelActiveForComplaint(id);
                proofService.closePendingProofs(id, "Complaint rejected by admin");
                complaint.setStatus(Complaint.Status.REJECTED);
                complaint.setResolvedAt(null);
                notificationService.notifyCitizen(citizenId, "Complaint rejected",
                        "Your complaint #" + id + " was rejected: " + remarks);
            }
            case RESOLVED -> {
                assignmentService.cancelActiveForComplaint(id);
                proofService.closePendingProofs(id, "Complaint closed by admin");
                complaint.setStatus(Complaint.Status.RESOLVED);
                complaint.setResolvedAt(LocalDateTime.now());
                notificationService.notifyCitizen(citizenId, "Complaint resolved",
                        "Your complaint #" + id + " has been marked as resolved." + (remarks != null ? " " + remarks : ""));
            }
            case PENDING -> {
                assignmentService.cancelActiveForComplaint(id);
                proofService.closePendingProofs(id, "Complaint reopened by admin");
                complaint.setStatus(Complaint.Status.PENDING);
                complaint.setResolvedAt(null);
                notificationService.notifyCitizen(citizenId, "Complaint reopened",
                        "Your complaint #" + id + " has been reopened and will be reviewed again.");
            }
            default -> throw new IllegalArgumentException("Use \"Assign a worker\" to move a complaint to " + dto.status());
        }
        if (remarks != null) {
            complaint.setAdminRemarks(remarks);
        }
        return complaintRepository.save(complaint);
    }

    @Transactional
    public Complaint updateClassification(Long id, String category, String priority) {
        Complaint complaint = getComplaint(id);
        String cat = TextUtil.clean(category);
        String pr = TextUtil.clean(priority);
        if (cat != null) {
            if (!aiCategoryService.categories().contains(cat)) {
                throw new IllegalArgumentException("Unknown category");
            }
            complaint.setCategory(cat);
        }
        if (pr != null) {
            pr = pr.toUpperCase(Locale.ROOT);
            if (!PRIORITIES.contains(pr)) {
                throw new IllegalArgumentException("Priority must be HIGH, MEDIUM or LOW");
            }
            complaint.setPriority(pr);
        }
        return complaintRepository.save(complaint);
    }

    public List<String> categories() {
        return aiCategoryService.categories();
    }

    private Complaint getComplaint(Long id) {
        return complaintRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
    }
}
