package com.civic.admin;

import com.civic.admin.dto.ComplaintStatusDTO;
import com.civic.common.dto.ApiResponse;
import com.civic.complaint.Complaint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/complaints")
public class AdminComplaintController {

    private final AdminComplaintService complaintService;

    public AdminComplaintController(AdminComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    public record ClassificationRequest(String category, String priority) {
    }

    @GetMapping
    public ApiResponse<List<Complaint>> search(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String priority,
                                               @RequestParam(required = false) String q) {
        return ApiResponse.ok(complaintService.search(status, category, priority, q));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(complaintService.categories());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> details(@PathVariable Long id) {
        return ApiResponse.ok(complaintService.getDetails(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Complaint> updateStatus(@PathVariable Long id, @RequestBody ComplaintStatusDTO request) {
        return ApiResponse.ok("Status updated", complaintService.updateStatus(id, request));
    }

    @PutMapping("/{id}/classification")
    public ApiResponse<Complaint> updateClassification(@PathVariable Long id, @RequestBody ClassificationRequest request) {
        return ApiResponse.ok("Category and priority saved",
                complaintService.updateClassification(id, request.category(), request.priority()));
    }
}
