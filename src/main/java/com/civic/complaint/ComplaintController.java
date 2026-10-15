package com.civic.complaint;

import com.civic.assignment.Assignment;
import com.civic.assignment.AssignmentService;
import com.civic.assignment.AssignmentStatus;
import com.civic.auth.AuthService;
import com.civic.common.dto.ApiResponse;
import com.civic.feedback.FeedbackService;
import com.civic.proof.ProofService;
import com.civic.proof.ProofStatus;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complaint API for citizens. */
@RestController
@RequestMapping("/api/citizen/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final AiCategoryService aiCategoryService;
    private final AssignmentService assignmentService;
    private final ProofService proofService;
    private final FeedbackService feedbackService;

    public ComplaintController(ComplaintService complaintService, AiCategoryService aiCategoryService,
                               AssignmentService assignmentService, ProofService proofService,
                               FeedbackService feedbackService) {
        this.complaintService = complaintService;
        this.aiCategoryService = aiCategoryService;
        this.assignmentService = assignmentService;
        this.proofService = proofService;
        this.feedbackService = feedbackService;
    }

    public record ClassifyRequest(String title, String description) {
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Complaint> create(@RequestParam String title,
                                         @RequestParam String description,
                                         @RequestParam(required = false) String category,
                                         @RequestParam String location,
                                         @RequestParam(required = false) MultipartFile image,
                                         HttpSession session) {
        Complaint complaint = complaintService.createComplaint(AuthService.currentUserId(session),
                title, description, category, location, image);
        return ApiResponse.ok("Complaint submitted successfully", complaint);
    }

    @GetMapping
    public ApiResponse<List<Complaint>> myComplaints(HttpSession session) {
        return ApiResponse.ok(complaintService.getCitizenComplaints(AuthService.currentUserId(session)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> details(@PathVariable Long id, HttpSession session) {
        Complaint complaint = complaintService.getCitizenComplaint(AuthService.currentUserId(session), id);
        Assignment assignment = assignmentService.getAssignmentsForComplaint(id).stream()
                .filter(a -> a.getStatus() != AssignmentStatus.CANCELLED)
                .findFirst().orElse(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("complaint", complaint);
        data.put("assignment", assignment);
        data.put("proofs", proofService.getProofsForComplaint(id).stream()
                .filter(p -> p.getStatus() == ProofStatus.APPROVED).toList());
        data.put("feedback", feedbackService.getForComplaint(id).orElse(null));
        return ApiResponse.ok(data);
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(aiCategoryService.categories());
    }

    /** Live AI suggestion while the citizen is typing. */
    @PostMapping("/classify")
    public ApiResponse<AiCategoryService.Result> classify(@RequestBody ClassifyRequest request) {
        return ApiResponse.ok(aiCategoryService.classify(request.title(), request.description()));
    }
}
