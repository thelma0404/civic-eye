package com.civic.admin;

import com.civic.admin.dto.AdminDashboardDTO;
import com.civic.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/admin/dashboard")
    public ApiResponse<AdminDashboardDTO> dashboard() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }
}
