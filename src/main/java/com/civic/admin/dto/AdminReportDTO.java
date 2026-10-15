package com.civic.admin.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AdminReportDTO(
        LocalDate from,
        LocalDate to,
        long totalComplaints,
        long resolved,
        long rejected,
        long open,
        double resolutionRate,
        double avgResolutionHours,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        Map<String, Long> byPriority,
        List<Map<String, Object>> workerPerformance,
        double averageRating,
        long feedbackCount) {
}
