package com.civic.admin;

import com.civic.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminFeedbackController {

    private final AdminFeedbackService feedbackService;

    public AdminFeedbackController(AdminFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/api/admin/feedback")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(feedbackService.getOverview());
    }
}
