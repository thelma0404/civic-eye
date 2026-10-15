package com.civic.admin;

import com.civic.common.util.TextUtil;
import com.civic.user.Role;
import com.civic.user.User;
import com.civic.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The admin's own profile. */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getProfile(Long adminId) {
        return userRepository.findById(adminId).filter(u -> u.getRole() == Role.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
    }

    @Transactional
    public User updateProfile(Long adminId, String name, String phone) {
        User admin = getProfile(adminId);
        String cleanName = TextUtil.clean(name);
        if (cleanName == null) {
            throw new IllegalArgumentException("Name can't be empty");
        }
        admin.setName(cleanName);
        admin.setPhone(TextUtil.clean(phone));
        return userRepository.save(admin);
    }

    @Transactional
    public void changePassword(Long adminId, String currentPassword, String newPassword) {
        User admin = getProfile(adminId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, admin.getPassword())) {
            throw new IllegalArgumentException("Your current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
    }
}
