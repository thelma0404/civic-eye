package com.civic.worker;

import com.civic.assignment.AssignmentService;
import com.civic.auth.AuthService;
import com.civic.common.dto.ApiResponse;
import com.civic.proof.ProofService;
import com.civic.proof.ProofStatus;
import com.civic.worker.dto.WorkerRequest;
import com.civic.worker.dto.WorkerResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Worker pages, the worker's own API, and the admin's worker-management API. */
@Controller
public class WorkerController {

    private final WorkerService workerService;
    private final AssignmentService assignmentService;
    private final ProofService proofService;

    public WorkerController(WorkerService workerService, AssignmentService assignmentService, ProofService proofService) {
        this.workerService = workerService;
        this.assignmentService = assignmentService;
        this.proofService = proofService;
    }

    public record ProfileRequest(String name, String phone) {
    }

    public record PasswordRequest(String currentPassword, String newPassword) {
    }

    // ---------- worker pages ----------

    @GetMapping("/worker")
    public String root() {
        return "redirect:/worker/dashboard";
    }

    @GetMapping("/worker/dashboard")
    public String dashboard() {
        return "worker/dashboard";
    }

    @GetMapping("/worker/complaints")
    public String assigned() {
        return "worker/assigned_complaints";
    }

    @GetMapping("/worker/complaints/{id}")
    public String details(@PathVariable Long id) {
        return "worker/complaint_details";
    }

    @GetMapping("/worker/complaints/{id}/status")
    public String updateStatus(@PathVariable Long id) {
        return "worker/update_status";
    }

    @GetMapping("/worker/complaints/{id}/proof")
    public String uploadProof(@PathVariable Long id) {
        return "worker/upload_proof";
    }

    @GetMapping("/worker/history")
    public String history() {
        return "worker/work_history";
    }

    @GetMapping("/worker/notifications")
    public String notifications() {
        return "worker/notifications";
    }

    @GetMapping("/worker/profile")
    public String profile() {
        return "worker/profile";
    }

    // ---------- worker API ----------

    @GetMapping("/api/worker/dashboard")
    @ResponseBody
    public ApiResponse<Map<String, Object>> dashboardData(HttpSession session) {
        Long workerId = AuthService.currentUserId(session);
        Worker worker = workerService.findWorker(workerId);
        Map<String, Long> stats = new LinkedHashMap<>(assignmentService.getWorkerStats(workerId));
        stats.put("awaitingReview", proofService.countForWorker(workerId, ProofStatus.PENDING));
        stats.put("approved", proofService.countForWorker(workerId, ProofStatus.APPROVED));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", worker.getName());
        data.put("status", worker.getStatus());
        data.put("stats", stats);
        data.put("active", assignmentService.getActiveAssignments(workerId));
        return ApiResponse.ok(data);
    }

    @GetMapping("/api/worker/profile")
    @ResponseBody
    public ApiResponse<WorkerResponse> getProfile(HttpSession session) {
        return ApiResponse.ok(workerService.getWorker(AuthService.currentUserId(session)));
    }

    @PutMapping("/api/worker/profile")
    @ResponseBody
    public ApiResponse<WorkerResponse> updateProfile(@RequestBody ProfileRequest request, HttpSession session) {
        WorkerResponse worker = workerService.updateProfile(AuthService.currentUserId(session), request.name(), request.phone());
        session.setAttribute(AuthService.SESSION_NAME, worker.name());
        return ApiResponse.ok("Profile updated", worker);
    }

    @PutMapping("/api/worker/password")
    @ResponseBody
    public ApiResponse<Void> changePassword(@RequestBody PasswordRequest request, HttpSession session) {
        workerService.changePassword(AuthService.currentUserId(session), request.currentPassword(), request.newPassword());
        return ApiResponse.ok("Password updated", null);
    }

    // ---------- admin: manage workers ----------

    @GetMapping("/api/admin/workers")
    @ResponseBody
    public ApiResponse<List<WorkerResponse>> allWorkers() {
        return ApiResponse.ok(workerService.getAllWorkers());
    }

    @GetMapping("/api/admin/workers/available")
    @ResponseBody
    public ApiResponse<List<WorkerResponse>> assignableWorkers() {
        return ApiResponse.ok(workerService.getAssignableWorkers());
    }

    @GetMapping("/api/admin/workers/{id}")
    @ResponseBody
    public ApiResponse<WorkerResponse> worker(@PathVariable Long id) {
        return ApiResponse.ok(workerService.getWorker(id));
    }

    @PostMapping("/api/admin/workers")
    @ResponseBody
    public ApiResponse<WorkerResponse> createWorker(@RequestBody WorkerRequest request) {
        return ApiResponse.ok("Worker added", workerService.createWorker(request));
    }

    @PutMapping("/api/admin/workers/{id}")
    @ResponseBody
    public ApiResponse<WorkerResponse> updateWorker(@PathVariable Long id, @RequestBody WorkerRequest request) {
        return ApiResponse.ok("Worker updated", workerService.updateWorker(id, request));
    }

    @DeleteMapping("/api/admin/workers/{id}")
    @ResponseBody
    public ApiResponse<Void> deactivateWorker(@PathVariable Long id) {
        workerService.deactivateWorker(id);
        return ApiResponse.ok("Worker deactivated", null);
    }
}
