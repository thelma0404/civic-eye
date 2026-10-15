package com.civic.admin;

import com.civic.auth.AuthService;
import com.civic.common.dto.ApiResponse;
import com.civic.user.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/** Admin pages and the admin profile API. */
@Controller
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    public record ProfileRequest(String name, String phone) {
    }

    public record PasswordRequest(String currentPassword, String newPassword) {
    }

    @GetMapping("/admin")
    public String root() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/admin-dashboard";
    }

    @GetMapping("/admin/complaints")
    public String complaints() {
        return "admin/admin-complaints";
    }

    @GetMapping("/admin/complaints/{id}")
    public String complaintDetails(@PathVariable Long id) {
        return "admin/admin-complaint-details";
    }

    @GetMapping("/admin/users")
    public String users() {
        return "admin/admin-users";
    }

    @GetMapping("/admin/notifications")
    public String notifications() {
        return "admin/admin-notifications";
    }

    @GetMapping("/admin/feedback")
    public String feedback() {
        return "admin/admin-feedback";
    }

    @GetMapping("/admin/reports")
    public String reports() {
        return "admin/admin-reports";
    }

    @GetMapping("/admin/chatbot")
    public String chatbot() {
        return "admin/admin-chatbot";
    }

    @GetMapping("/admin/profile")
    public String profile() {
        return "admin/admin-profile";
    }

    @GetMapping("/api/admin/profile")
    @ResponseBody
    public ApiResponse<User> getProfile(HttpSession session) {
        return ApiResponse.ok(adminService.getProfile(AuthService.currentUserId(session)));
    }

    @PutMapping("/api/admin/profile")
    @ResponseBody
    public ApiResponse<User> updateProfile(@RequestBody ProfileRequest request, HttpSession session) {
        User admin = adminService.updateProfile(AuthService.currentUserId(session), request.name(), request.phone());
        session.setAttribute(AuthService.SESSION_NAME, admin.getName());
        return ApiResponse.ok("Profile updated", admin);
    }

    @PutMapping("/api/admin/password")
    @ResponseBody
    public ApiResponse<Void> changePassword(@RequestBody PasswordRequest request, HttpSession session) {
        adminService.changePassword(AuthService.currentUserId(session), request.currentPassword(), request.newPassword());
        return ApiResponse.ok("Password updated", null);
    }
}
