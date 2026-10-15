package com.civic.citizen;

import com.civic.common.util.TextUtil;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.notification.NotificationService;
import com.civic.user.User;
import com.civic.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CitizenService {

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    public CitizenService(UserRepository userRepository, ComplaintRepository complaintRepository,
                          NotificationService notificationService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    public User getProfile(Long citizenId) {
        return userRepository.findById(citizenId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public User updateProfile(Long citizenId, String name, String phone, String address) {
        User user = getProfile(citizenId);
        String cleanName = TextUtil.clean(name);
        if (cleanName == null) {
            throw new IllegalArgumentException("Name can't be empty");
        }
        user.setName(cleanName);
        user.setPhone(TextUtil.clean(phone));
        user.setAddress(TextUtil.clean(address));
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long citizenId, String currentPassword, String newPassword) {
        User user = getProfile(citizenId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Your current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public Map<String, Object> getDashboard(Long citizenId) {
        User user = getProfile(citizenId);
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", complaintRepository.countByCitizenId(citizenId));
        stats.put("pending", complaintRepository.countByCitizenIdAndStatus(citizenId, Complaint.Status.PENDING));
        stats.put("active", complaintRepository.countByCitizenIdAndStatusIn(citizenId,
                List.of(Complaint.Status.ASSIGNED, Complaint.Status.IN_PROGRESS)));
        stats.put("resolved", complaintRepository.countByCitizenIdAndStatus(citizenId, Complaint.Status.RESOLVED));
        stats.put("rejected", complaintRepository.countByCitizenIdAndStatus(citizenId, Complaint.Status.REJECTED));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", user.getName());
        data.put("stats", stats);
        data.put("recent", complaintRepository.findTop5ByCitizenIdOrderByCreatedAtDesc(citizenId));
        data.put("notifications", notificationService.getLatest(NotificationService.CITIZEN, citizenId));
        data.put("unread", notificationService.unreadCount(NotificationService.CITIZEN, citizenId));
        return data;
    }
}
