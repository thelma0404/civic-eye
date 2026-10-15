package com.civic.admin;

import com.civic.complaint.ComplaintRepository;
import com.civic.user.Role;
import com.civic.user.User;
import com.civic.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;

    public AdminUserService(UserRepository userRepository, ComplaintRepository complaintRepository) {
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
    }

    public List<Map<String, Object>> getCitizens() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.CITIZEN).stream().map(u -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId());
            row.put("name", u.getName());
            row.put("email", u.getEmail());
            row.put("phone", u.getPhone());
            row.put("address", u.getAddress());
            row.put("active", u.isActive());
            row.put("createdAt", u.getCreatedAt());
            row.put("complaints", complaintRepository.countByCitizenId(u.getId()));
            return row;
        }).toList();
    }

    @Transactional
    public User toggleActive(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin accounts can't be deactivated here");
        }
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }
}
