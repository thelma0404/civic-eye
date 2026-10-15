package com.civic.admin;

import com.civic.admin.dto.AdminReportDTO;
import com.civic.common.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
public class AdminReportController {

    private final AdminReportService reportService;

    public AdminReportController(AdminReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/admin/reports")
    public ApiResponse<AdminReportDTO> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.generate(from, to));
    }

    /** Downloads the complaints in the date range as a CSV file (opens in Excel). */
    @GetMapping("/api/admin/reports/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] range = reportService.range(from, to);
        byte[] body = ("﻿" + reportService.exportCsv(range[0], range[1])).getBytes(StandardCharsets.UTF_8);
        String filename = "civic-report-" + range[0] + "-to-" + range[1] + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
