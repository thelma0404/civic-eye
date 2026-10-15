package com.civic.auth;

import com.civic.common.util.TextUtil;
import com.civic.user.Role;
import com.civic.user.User;
import com.civic.user.UserRepository;
import com.civic.worker.Worker;
import com.civic.worker.WorkerRepository;
import com.civic.worker.WorkerStatus;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Citizen registration/login, worker login and the session helpers shared by every controller. */
@Service
public class AuthService {

    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_ROLE = "role";
    public static final String SESSION_NAME = "name";

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, WorkerRepository workerRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workerRepository = workerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String name, String email, String phone, String address, String password) {
        String cleanName = TextUtil.clean(name);
        String cleanEmail = TextUtil.normalizeEmail(email);
        if (cleanName == null) {
            throw new IllegalArgumentException("Please enter your name");
        }
        if (!TextUtil.isValidEmail(cleanEmail)) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        String cleanPhone = TextUtil.clean(phone);
        String cleanAddress = TextUtil.clean(address);
        if (cleanName.length() > 100) {
            throw new IllegalArgumentException("Name must be 100 characters or fewer");
        }
        if (cleanEmail.length() > 150) {
            throw new IllegalArgumentException("Email must be 150 characters or fewer");
        }
        if (cleanPhone != null && cleanPhone.length() > 20) {
            throw new IllegalArgumentException("Phone number must be 20 characters or fewer");
        }
        if (cleanAddress != null && cleanAddress.length() > 255) {
            throw new IllegalArgumentException("Address must be 255 characters or fewer");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        User user = new User();
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        user.setPhone(cleanPhone);
        user.setAddress(cleanAddress);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.CITIZEN);
        user.setActive(true);
        return userRepository.save(user);
    }

    public LoginResponse loginCitizen(LoginRequest request, HttpSession session) {
        requireCredentials(request);
        User user = userRepository.findByEmail(TextUtil.normalizeEmail(request.email()))
                .filter(u -> u.getRole() == Role.CITIZEN)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("Your account has been deactivated. Please contact the municipality.");
        }
        startSession(session, user.getId(), user.getName(), "CITIZEN");
        return new LoginResponse(user.getId(), user.getName(), user.getEmail(), "CITIZEN", "/citizen/dashboard");
    }

    public LoginResponse loginWorker(LoginRequest request, HttpSession session) {
        requireCredentials(request);
        Worker worker = workerRepository.findByEmail(TextUtil.normalizeEmail(request.email()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), worker.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        if (worker.getStatus() == WorkerStatus.INACTIVE) {
            throw new IllegalArgumentException("Your worker account is inactive. Please contact the administrator.");
        }
        startSession(session, worker.getId(), worker.getName(), "WORKER");
        return new LoginResponse(worker.getId(), worker.getName(), worker.getEmail(), "WORKER", "/worker/dashboard");
    }

    static void requireCredentials(LoginRequest request) {
        if (request == null || TextUtil.clean(request.email()) == null || request.password() == null || request.password().isEmpty()) {
            throw new IllegalArgumentException("Please enter your email and password");
        }
    }

    public static void startSession(HttpSession session, Long id, String name, String role) {
        session.setAttribute(SESSION_USER_ID, id);
        session.setAttribute(SESSION_NAME, name);
        session.setAttribute(SESSION_ROLE, role);
    }

    /** The logged-in user's (or worker's) id, or 401 when nobody is logged in. */
    public static Long currentUserId(HttpSession session) {
        Object id = session == null ? null : session.getAttribute(SESSION_USER_ID);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in to continue");
        }
        return (Long) id;
    }
}
