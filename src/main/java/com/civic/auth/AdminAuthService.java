package com.civic.auth;

import com.civic.common.util.TextUtil;
import com.civic.user.Role;
import com.civic.user.User;
import com.civic.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request, HttpSession session) {
        AuthService.requireCredentials(request);
        User admin = userRepository.findByEmail(TextUtil.normalizeEmail(request.email()))
                .filter(u -> u.getRole() == Role.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin credentials"));
        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new IllegalArgumentException("Invalid admin credentials");
        }
        if (!admin.isActive()) {
            throw new IllegalArgumentException("This admin account is disabled");
        }
        AuthService.startSession(session, admin.getId(), admin.getName(), "ADMIN");
        return new LoginResponse(admin.getId(), admin.getName(), admin.getEmail(), "ADMIN", "/admin/dashboard");
    }
}
