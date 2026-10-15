package com.civic.proof;

import com.civic.auth.AuthService;
import com.civic.common.dto.ApiResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ProofController {

    private final ProofService proofService;

    public ProofController(ProofService proofService) {
        this.proofService = proofService;
    }

    public record ReviewRequest(Boolean approve, String comment) {
    }

    // ---------- worker ----------

    @PostMapping(value = "/api/worker/assignments/{id}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WorkProof> upload(@PathVariable Long id,
                                         @RequestParam(required = false) MultipartFile file,
                                         @RequestParam(required = false) String remarks,
                                         HttpSession session) {
        return ApiResponse.ok("Proof uploaded. The admin will review it shortly.",
                proofService.uploadProof(AuthService.currentUserId(session), id, file, remarks));
    }

    @GetMapping("/api/worker/assignments/{id}/proofs")
    public ApiResponse<List<WorkProof>> workerProofs(@PathVariable Long id, HttpSession session) {
        AuthService.currentUserId(session);
        return ApiResponse.ok(proofService.getProofsForAssignment(id));
    }

    // ---------- admin ----------

    @GetMapping("/api/admin/proofs/pending")
    public ApiResponse<List<WorkProof>> pending() {
        return ApiResponse.ok(proofService.getPendingProofs());
    }

    @GetMapping("/api/admin/complaints/{id}/proofs")
    public ApiResponse<List<WorkProof>> complaintProofs(@PathVariable Long id) {
        return ApiResponse.ok(proofService.getProofsForComplaint(id));
    }

    @PostMapping("/api/admin/proofs/{id}/review")
    public ApiResponse<WorkProof> review(@PathVariable Long id, @RequestBody ReviewRequest request) {
        if (request == null || request.approve() == null) {
            throw new IllegalArgumentException("Please approve or reject the proof");
        }
        WorkProof proof = proofService.reviewProof(id, request.approve(), request.comment());
        return ApiResponse.ok(request.approve() ? "Proof approved - complaint resolved" : "Proof rejected - sent back to the worker", proof);
    }
}
