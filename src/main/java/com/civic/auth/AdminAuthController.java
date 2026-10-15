package com.civic.auth;

import com.civic.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "auth/admin-login";
    }

    @PostMapping("/api/admin/auth/login")
    @ResponseBody
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Welcome back!", adminAuthService.login(request, AuthController.freshSession(http)));
    }

    @PostMapping("/api/admin/auth/logout")
    @ResponseBody
    public ApiResponse<Void> logout(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.ok("Logged out", null);
    }
}
