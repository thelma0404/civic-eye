package com.civic.admin;

import com.civic.common.dto.ApiResponse;
import com.civic.user.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/admin/users")
    public ApiResponse<List<Map<String, Object>>> citizens() {
        return ApiResponse.ok(userService.getCitizens());
    }

    @PutMapping("/api/admin/users/{id}/toggle-active")
    public ApiResponse<User> toggle(@PathVariable Long id) {
        User user = userService.toggleActive(id);
        return ApiResponse.ok(user.isActive() ? "Account activated" : "Account deactivated", user);
    }
}
