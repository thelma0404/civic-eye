package com.civic.assignment;

import com.civic.auth.AuthService;
import com.civic.common.dto.ApiResponse;
import com.civic.proof.ProofService;
import com.civic.proof.ProofStatus;
import com.civic.proof.WorkProof;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ProofService proofService;

    public AssignmentController(AssignmentService assignmentService, ProofService proofService) {
        this.assignmentService = assignmentService;
        this.proofService = proofService;
    }

    public record AssignRequest(Long workerId, String notes) {
    }

    public record StatusRequest(AssignmentStatus status, String remarks) {
    }

    // ---------- admin ----------

    @PostMapping("/api/admin/complaints/{complaintId}/assign")
    public ApiResponse<Assignment> assign(@PathVariable Long complaintId, @RequestBody AssignRequest request) {
        if (request == null || request.workerId() == null) {
            throw new IllegalArgumentException("Please choose a worker to assign");
        }
        return ApiResponse.ok("Complaint assigned",
                assignmentService.assignComplaint(complaintId, request.workerId(), request.notes()));
    }

    // ---------- worker ----------

    @GetMapping("/api/worker/assignments")
    public ApiResponse<List<Assignment>> myAssignments(HttpSession session) {
        return ApiResponse.ok(assignmentService.getActiveAssignments(AuthService.currentUserId(session)));
    }

    @GetMapping("/api/worker/assignments/{id}")
    public ApiResponse<Map<String, Object>> assignment(@PathVariable Long id, HttpSession session) {
        Assignment assignment = assignmentService.getWorkerAssignment(AuthService.currentUserId(session), id);
        List<WorkProof> proofs = proofService.getProofsForAssignment(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("assignment", assignment);
        data.put("proofs", proofs);
        data.put("pendingReview", proofs.stream().anyMatch(p -> p.getStatus() == ProofStatus.PENDING));
        return ApiResponse.ok(data);
    }

    @PutMapping("/api/worker/assignments/{id}/status")
    public ApiResponse<Assignment> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request, HttpSession session) {
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("Please choose a status");
        }
        return ApiResponse.ok("Status updated", assignmentService.updateStatus(
                AuthService.currentUserId(session), id, request.status(), request.remarks()));
    }

    @GetMapping("/api/worker/history")
    public ApiResponse<List<Map<String, Object>>> history(HttpSession session) {
        List<Map<String, Object>> rows = assignmentService.getWorkHistory(AuthService.currentUserId(session)).stream()
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("assignment", a);
                    row.put("proof", proofService.getProofsForAssignment(a.getId()).stream().findFirst().orElse(null));
                    return row;
                })
                .toList();
        return ApiResponse.ok(rows);
    }
}
