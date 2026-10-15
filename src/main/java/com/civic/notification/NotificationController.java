package com.civic.notification;

import com.civic.auth.AuthService;
import com.civic.common.dto.ApiResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Notifications for citizens (/api/citizen/...) and workers (/api/worker/...). */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private static String type(String audience) {
        return "worker".equals(audience) ? NotificationService.WORKER : NotificationService.CITIZEN;
    }

    @GetMapping("/api/{audience:citizen|worker}/notifications")
    public ApiResponse<List<Notification>> list(@PathVariable String audience, HttpSession session) {
        return ApiResponse.ok(notificationService.getNotifications(type(audience), AuthService.currentUserId(session)));
    }

    @GetMapping("/api/{audience:citizen|worker}/notifications/unread-count")
    public ApiResponse<Long> unread(@PathVariable String audience, HttpSession session) {
        return ApiResponse.ok(notificationService.unreadCount(type(audience), AuthService.currentUserId(session)));
    }

    @PostMapping("/api/{audience:citizen|worker}/notifications/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable String audience, @PathVariable Long id, HttpSession session) {
        notificationService.markRead(type(audience), AuthService.currentUserId(session), id);
        return ApiResponse.ok("Marked as read", null);
    }

    @PostMapping("/api/{audience:citizen|worker}/notifications/read-all")
    public ApiResponse<Void> markAllRead(@PathVariable String audience, HttpSession session) {
        notificationService.markAllRead(type(audience), AuthService.currentUserId(session));
        return ApiResponse.ok("All notifications marked as read", null);
    }
}
