package com.civic.feedback;

import com.civic.auth.AuthService;
import com.civic.common.dto.ApiResponse;
import com.civic.complaint.Complaint;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/citizen/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    public record FeedbackRequest(Long complaintId, Integer rating, String comments) {
    }

    @PostMapping
    public ApiResponse<Feedback> submit(@RequestBody FeedbackRequest request, HttpSession session) {
        return ApiResponse.ok("Thank you for your feedback!", feedbackService.submit(
                AuthService.currentUserId(session), request.complaintId(), request.rating(), request.comments()));
    }

    @GetMapping
    public ApiResponse<List<Feedback>> mine(HttpSession session) {
        return ApiResponse.ok(feedbackService.getCitizenFeedback(AuthService.currentUserId(session)));
    }

    @GetMapping("/pending")
    public ApiResponse<List<Complaint>> pending(HttpSession session) {
        return ApiResponse.ok(feedbackService.getPendingFeedback(AuthService.currentUserId(session)));
    }
}
