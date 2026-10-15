package com.civic.citizen;

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

import java.util.Map;

/** Citizen pages and the citizen profile/dashboard API. */
@Controller
public class CitizenController {

    private final CitizenService citizenService;

    public CitizenController(CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    public record ProfileRequest(String name, String phone, String address) {
    }

    public record PasswordRequest(String currentPassword, String newPassword) {
    }

    // ---------- pages ----------

    @GetMapping("/citizen")
    public String root() {
        return "redirect:/citizen/dashboard";
    }

    @GetMapping("/citizen/dashboard")
    public String dashboard() {
        return "citizen/citizen-dashboard";
    }

    @GetMapping("/citizen/report")
    public String reportIssue() {
        return "citizen/report-issue";
    }

    @GetMapping("/citizen/complaints")
    public String myComplaints() {
        return "citizen/my-complaints";
    }

    @GetMapping("/citizen/complaints/{id}")
    public String complaintDetails(@PathVariable Long id) {
        return "citizen/complaint-details";
    }

    @GetMapping("/citizen/notifications")
    public String notifications() {
        return "citizen/notifications";
    }

    @GetMapping("/citizen/feedback")
    public String feedback() {
        return "citizen/feedback";
    }

    @GetMapping("/citizen/profile")
    public String profile() {
        return "citizen/profile";
    }

    // ---------- API ----------

    @GetMapping("/api/citizen/dashboard")
    @ResponseBody
    public ApiResponse<Map<String, Object>> dashboardData(HttpSession session) {
        return ApiResponse.ok(citizenService.getDashboard(AuthService.currentUserId(session)));
    }

    @GetMapping("/api/citizen/profile")
    @ResponseBody
    public ApiResponse<User> getProfile(HttpSession session) {
        return ApiResponse.ok(citizenService.getProfile(AuthService.currentUserId(session)));
    }

    @PutMapping("/api/citizen/profile")
    @ResponseBody
    public ApiResponse<User> updateProfile(@RequestBody ProfileRequest request, HttpSession session) {
        User user = citizenService.updateProfile(AuthService.currentUserId(session), request.name(), request.phone(), request.address());
        session.setAttribute(AuthService.SESSION_NAME, user.getName());
        return ApiResponse.ok("Profile updated", user);
    }

    @PutMapping("/api/citizen/password")
    @ResponseBody
    public ApiResponse<Void> changePassword(@RequestBody PasswordRequest request, HttpSession session) {
        citizenService.changePassword(AuthService.currentUserId(session), request.currentPassword(), request.newPassword());
        return ApiResponse.ok("Password updated", null);
    }
}
