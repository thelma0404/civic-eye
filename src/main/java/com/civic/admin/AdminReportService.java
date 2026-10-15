package com.civic.admin;

import com.civic.admin.dto.AdminReportDTO;
import com.civic.assignment.Assignment;
import com.civic.assignment.AssignmentRepository;
import com.civic.assignment.AssignmentStatus;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.feedback.Feedback;
import com.civic.feedback.FeedbackRepository;
import com.civic.worker.Worker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminReportService {

    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ComplaintRepository complaintRepository;
    private final AssignmentRepository assignmentRepository;
    private final FeedbackRepository feedbackRepository;

    public AdminReportService(ComplaintRepository complaintRepository, AssignmentRepository assignmentRepository,
                              FeedbackRepository feedbackRepository) {
        this.complaintRepository = complaintRepository;
        this.assignmentRepository = assignmentRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public AdminReportDTO generate(LocalDate fromDate, LocalDate toDate) {
        LocalDate[] range = range(fromDate, toDate);
        LocalDate from = range[0];
        LocalDate to = range[1];
        List<Complaint> list = complaints(from, to);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Complaint.Status s : Complaint.Status.values()) {
            byStatus.put(s.name(), list.stream().filter(c -> c.getStatus() == s).count());
        }
        Map<String, Long> byCategory = list.stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (String p : List.of("HIGH", "MEDIUM", "LOW")) {
            byPriority.put(p, list.stream().filter(c -> p.equals(c.getPriority())).count());
        }

        long total = list.size();
        long resolved = byStatus.get("RESOLVED");
        long rejected = byStatus.get("REJECTED");
        double rate = total == 0 ? 0 : round1(resolved * 100.0 / total);
        double avgHours = round1(list.stream()
                .filter(c -> c.getStatus() == Complaint.Status.RESOLVED && c.getResolvedAt() != null)
                .mapToLong(c -> Duration.between(c.getCreatedAt(), c.getResolvedAt()).toMinutes())
                .average().orElse(0) / 60.0);

        List<Feedback> feedback = feedbackRepository.findByCreatedAtBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        double avgRating = round1(feedback.stream().mapToInt(Feedback::getRating).average().orElse(0));

        return new AdminReportDTO(from, to, total, resolved, rejected, total - resolved - rejected, rate, avgHours,
                byStatus, byCategory, byPriority, workerPerformance(from, to), avgRating, feedback.size());
    }

    private List<Map<String, Object>> workerPerformance(LocalDate from, LocalDate to) {
        Map<Long, List<Assignment>> byWorker = assignmentRepository
                .findByAssignedAtBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .filter(a -> a.getStatus() != AssignmentStatus.CANCELLED)
                .collect(Collectors.groupingBy(a -> a.getWorker().getId(), LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> rows = new ArrayList<>();
        byWorker.forEach((id, assignments) -> {
            Worker worker = assignments.get(0).getWorker();
            List<Assignment> done = assignments.stream()
                    .filter(a -> a.getStatus() == AssignmentStatus.COMPLETED && a.getCompletedAt() != null).toList();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workerId", id);
            row.put("name", worker.getName());
            row.put("department", worker.getDepartment());
            row.put("assigned", assignments.size());
            row.put("completed", done.size());
            row.put("open", assignments.size() - done.size());
            row.put("completionRate", round1(done.size() * 100.0 / assignments.size()));
            row.put("avgCompletionHours", round1(done.stream()
                    .mapToLong(a -> Duration.between(a.getAssignedAt(), a.getCompletedAt()).toMinutes())
                    .average().orElse(0) / 60.0));
            rows.add(row);
        });
        rows.sort(Comparator.comparing((Map<String, Object> r) -> (Integer) r.get("completed")).reversed());
        return rows;
    }

    @Transactional(readOnly = true)
    public String exportCsv(LocalDate fromDate, LocalDate toDate) {
        LocalDate[] range = range(fromDate, toDate);
        StringBuilder sb = new StringBuilder("ID,Title,Category,Priority,Status,Location,Citizen,Reported On,Resolved On\n");
        for (Complaint c : complaints(range[0], range[1])) {
            sb.append(c.getId()).append(',')
                    .append(csv(c.getTitle())).append(',')
                    .append(csv(c.getCategory())).append(',')
                    .append(csv(c.getPriority())).append(',')
                    .append(csv(c.getStatus())).append(',')
                    .append(csv(c.getLocation())).append(',')
                    .append(csv(c.getCitizen().getName())).append(',')
                    .append(csv(format(c.getCreatedAt()))).append(',')
                    .append(csv(format(c.getResolvedAt()))).append('\n');
        }
        return sb.toString();
    }

    public LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("The start date must be before the end date");
        }
        return new LocalDate[]{start, end};
    }

    private List<Complaint> complaints(LocalDate from, LocalDate to) {
        return complaintRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(CSV_DATE);
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s; // protect against spreadsheet formula injection
        }
        boolean quote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        s = s.replace("\"", "\"\"");
        return quote ? "\"" + s + "\"" : s;
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
