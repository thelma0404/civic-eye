package com.civic.admin;

import com.civic.common.dto.ApiResponse;
import com.civic.notification.Notification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    public AdminNotificationController(AdminNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public record SendRequest(String audience, String title, String message) {
    }

    @GetMapping
    public ApiResponse<List<Notification>> inbox() {
        return ApiResponse.ok(notificationService.getInbox());
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unread() {
        return ApiResponse.ok(notificationService.unreadCount());
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ApiResponse.ok("Marked as read", null);
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.ok("All notifications marked as read", null);
    }

    @PostMapping("/send")
    public ApiResponse<Integer> send(@RequestBody SendRequest request) {
        int count = notificationService.send(request.audience(), request.title(), request.message());
        return ApiResponse.ok("Notification sent to " + count + " recipient" + (count == 1 ? "" : "s"), count);
    }
}
