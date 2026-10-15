package com.civic.auth;

import com.civic.common.dto.ApiResponse;
import com.civic.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record RegisterRequest(String name, String email, String phone, String address, String password) {
    }

    // ---------- pages ----------

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @GetMapping("/worker/login")
    public String workerLoginPage() {
        return "auth/worker_login";
    }

    // ---------- API ----------

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request.name(), request.email(), request.phone(), request.address(), request.password());
        return ApiResponse.ok("Account created. You can now log in.", Map.of("id", user.getId()));
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Welcome back!", authService.loginCitizen(request, freshSession(http)));
    }

    @PostMapping("/api/auth/worker/login")
    @ResponseBody
    public ApiResponse<LoginResponse> workerLogin(@RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Welcome back!", authService.loginWorker(request, freshSession(http)));
    }

    @PostMapping("/api/auth/logout")
    @ResponseBody
    public ApiResponse<Void> logout(HttpServletRequest http) {
        HttpSession session = http.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.ok("Logged out", null);
    }

    /** Who is logged in (used by every dashboard page for the name in the top bar). */
    @GetMapping("/api/auth/me")
    @ResponseBody
    public ApiResponse<Map<String, Object>> me(HttpSession session) {
        Object role = session.getAttribute(AuthService.SESSION_ROLE);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("id", session.getAttribute(AuthService.SESSION_USER_ID));
        me.put("name", session.getAttribute(AuthService.SESSION_NAME));
        me.put("role", role);
        return ApiResponse.ok(me);
    }

    /** Starts a brand-new session on login (prevents session fixation). */
    static HttpSession freshSession(HttpServletRequest http) {
        HttpSession old = http.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        return http.getSession(true);
    }
}
