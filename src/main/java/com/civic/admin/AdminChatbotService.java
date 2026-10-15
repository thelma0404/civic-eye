package com.civic.admin;

import com.civic.assignment.AssignmentRepository;
import com.civic.assignment.AssignmentStatus;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.feedback.FeedbackRepository;
import com.civic.proof.ProofRepository;
import com.civic.proof.ProofStatus;
import com.civic.proof.WorkProof;
import com.civic.user.Role;
import com.civic.user.UserRepository;
import com.civic.worker.Worker;
import com.civic.worker.WorkerRepository;
import com.civic.worker.WorkerStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Rule-based operations assistant for admins. It understands common questions
 * ("how many pending?", "status of #12", "which workers are free?") and answers
 * from live data. Replace {@link #reply} with an LLM call if you want free-form answers.
 */
@Service
public class AdminChatbotService {

    private static final Pattern COMPLAINT_ID = Pattern.compile("(?:#|complaint\\s*(?:no\\.?|number|id)?\\s*#?)\\s*(\\d{1,9})");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final List<Complaint.Status> OPEN =
            List.of(Complaint.Status.PENDING, Complaint.Status.ASSIGNED, Complaint.Status.IN_PROGRESS);
    private static final List<AssignmentStatus> ACTIVE = List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.IN_PROGRESS);
    private static final List<String> SUGGESTIONS = List.of("Give me a summary", "How many complaints are pending?",
            "Show high priority issues", "Which workers are available?", "Proofs awaiting review", "Most common category");

    private final ComplaintRepository complaintRepository;
    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final ProofRepository proofRepository;
    private final AssignmentRepository assignmentRepository;

    public AdminChatbotService(ComplaintRepository complaintRepository, WorkerRepository workerRepository,
                               UserRepository userRepository, FeedbackRepository feedbackRepository,
                               ProofRepository proofRepository, AssignmentRepository assignmentRepository) {
        this.complaintRepository = complaintRepository;
        this.workerRepository = workerRepository;
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.proofRepository = proofRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reply(String message) {
        String q = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        Matcher id = COMPLAINT_ID.matcher(q);
        String answer;
        if (q.isEmpty()) {
            answer = "Ask me anything about complaints, workers, proofs or feedback.";
        } else if (id.find()) {
            answer = complaintStatus(Long.parseLong(id.group(1)));
        } else if (has(q, "hello", "hi", "hey", "good morning", "good afternoon", "good evening")) {
            answer = "Hello! Here's where things stand:\n" + summary();
        } else if (has(q, "help", "what can you do", "commands")) {
            answer = help();
        } else if (has(q, "summary", "overview", "dashboard", "snapshot", "status report")) {
            answer = summary();
        } else if (has(q, "high priority", "urgent", "critical", "priority", "dangerous")) {
            answer = highPriority();
        } else if (has(q, "proof", "proofs", "review", "verify", "approval")) {
            answer = proofs();
        } else if (has(q, "worker", "workers", "staff", "team", "available", "free")) {
            answer = workers();
        } else if (has(q, "category", "categories", "common", "most", "type", "types")) {
            answer = categories();
        } else if (has(q, "feedback", "rating", "ratings", "satisfaction", "satisfied", "stars")) {
            answer = feedback();
        } else if (has(q, "today")) {
            answer = today();
        } else if (has(q, "pending", "unassigned", "waiting", "new")) {
            answer = pending();
        } else if (has(q, "in progress", "ongoing", "assigned", "active", "open")) {
            answer = inProgress();
        } else if (has(q, "resolved", "completed", "closed", "fixed", "done")) {
            answer = resolved();
        } else if (has(q, "rejected", "declined")) {
            answer = "**" + complaintRepository.countByStatus(Complaint.Status.REJECTED) + "** complaints have been rejected.";
        } else if (has(q, "citizen", "citizens", "user", "users", "registered")) {
            answer = "There are **" + userRepository.countByRole(Role.CITIZEN) + "** registered citizens.";
        } else if (has(q, "total", "how many", "count", "number of")) {
            answer = summary();
        } else {
            answer = "I'm not sure about that one yet.\n" + help();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", answer);
        result.put("suggestions", SUGGESTIONS);
        return result;
    }

    private boolean has(String text, String... words) {
        for (String w : words) {
            if (Pattern.compile("\\b" + Pattern.quote(w) + "\\b").matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private String summary() {
        long total = complaintRepository.count();
        return String.format("**%d** complaints in total: %d pending, %d assigned, %d in progress, %d resolved and %d rejected.\n"
                        + "%d workers are available and %d proofs are waiting for your review.",
                total,
                complaintRepository.countByStatus(Complaint.Status.PENDING),
                complaintRepository.countByStatus(Complaint.Status.ASSIGNED),
                complaintRepository.countByStatus(Complaint.Status.IN_PROGRESS),
                complaintRepository.countByStatus(Complaint.Status.RESOLVED),
                complaintRepository.countByStatus(Complaint.Status.REJECTED),
                workerRepository.countByStatus(WorkerStatus.AVAILABLE),
                proofRepository.countByStatus(ProofStatus.PENDING));
    }

    private String complaintStatus(Long id) {
        return complaintRepository.findById(id).map(c -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Complaint #").append(c.getId()).append(": \"").append(c.getTitle()).append("\"\n")
                    .append("Status: **").append(label(c.getStatus())).append("** · Priority: ").append(c.getPriority())
                    .append(" · Category: ").append(c.getCategory()).append('\n')
                    .append("Location: ").append(c.getLocation()).append('\n')
                    .append("Reported by ").append(c.getCitizen().getName()).append(" on ").append(c.getCreatedAt().format(DATE)).append('.');
            assignmentRepository.findFirstByComplaintIdAndStatusIn(id, ACTIVE).ifPresent(a ->
                    sb.append("\nAssigned to ").append(a.getWorker().getName()).append(" (").append(label(a.getStatus())).append(")."));
            return sb.toString();
        }).orElse("I couldn't find complaint #" + id + ".");
    }

    private String highPriority() {
        List<Complaint> list = complaintRepository.findByPriorityAndStatusInOrderByCreatedAtAsc("HIGH", OPEN);
        if (list.isEmpty()) {
            return "Good news: there are no open high-priority complaints.";
        }
        return "There are **" + list.size() + "** open high-priority complaints. The oldest ones:\n"
                + list.stream().limit(5).map(c -> "• #" + c.getId() + " " + c.getTitle() + " (" + label(c.getStatus()) + ")")
                .collect(Collectors.joining("\n"));
    }

    private String proofs() {
        List<WorkProof> list = proofRepository.findByStatusOrderByUploadedAtAsc(ProofStatus.PENDING);
        if (list.isEmpty()) {
            return "No proofs are waiting for review right now.";
        }
        return "**" + list.size() + "** proofs are waiting for your review:\n"
                + list.stream().limit(5).map(p -> "• #" + p.getAssignment().getComplaint().getId() + " "
                        + p.getAssignment().getComplaint().getTitle() + ", by " + p.getAssignment().getWorker().getName())
                .collect(Collectors.joining("\n"));
    }

    private String workers() {
        List<Worker> available = workerRepository.findByStatusOrderByNameAsc(WorkerStatus.AVAILABLE);
        long busy = workerRepository.countByStatus(WorkerStatus.BUSY);
        long inactive = workerRepository.countByStatus(WorkerStatus.INACTIVE);
        String names = available.isEmpty() ? "nobody is free right now"
                : available.stream().limit(8).map(w -> w.getName() + (w.getDepartment() != null ? " (" + w.getDepartment() + ")" : ""))
                .collect(Collectors.joining(", "));
        return "**" + available.size() + "** available, " + busy + " busy, " + inactive + " inactive.\nAvailable: " + names + ".";
    }

    private String categories() {
        List<Object[]> rows = complaintRepository.countGroupedByCategory();
        if (rows.isEmpty()) {
            return "There are no complaints yet.";
        }
        return "Most reported categories:\n" + rows.stream().limit(5)
                .map(r -> "• " + r[0] + ": " + r[1]).collect(Collectors.joining("\n"));
    }

    private String feedback() {
        Double avg = feedbackRepository.averageRating();
        long count = feedbackRepository.count();
        if (count == 0) {
            return "No citizen feedback has been submitted yet.";
        }
        return String.format("Average rating is **%.1f / 5** from %d reviews. %d reviews are 2 stars or lower.",
                avg == null ? 0 : avg, count, feedbackRepository.countByRatingLessThanEqual(2));
    }

    private String today() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return "Today: **" + complaintRepository.countByCreatedAtAfter(start) + "** new complaints and **"
                + complaintRepository.countByResolvedAtAfter(start) + "** resolved.";
    }

    private String pending() {
        List<Complaint> list = complaintRepository.findByStatusOrderByCreatedAtAsc(Complaint.Status.PENDING);
        if (list.isEmpty()) {
            return "No complaints are waiting to be assigned.";
        }
        Complaint oldest = list.get(0);
        long days = Duration.between(oldest.getCreatedAt(), LocalDateTime.now()).toDays();
        return "**" + list.size() + "** complaints are waiting to be assigned. The oldest is #" + oldest.getId()
                + " \"" + oldest.getTitle() + "\", waiting " + (days == 0 ? "less than a day" : days + (days == 1 ? " day" : " days")) + ".";
    }

    private String inProgress() {
        return "**" + complaintRepository.countByStatus(Complaint.Status.ASSIGNED) + "** complaints are assigned and **"
                + complaintRepository.countByStatus(Complaint.Status.IN_PROGRESS) + "** are being worked on.";
    }

    private String resolved() {
        long total = complaintRepository.count();
        long resolved = complaintRepository.countByStatus(Complaint.Status.RESOLVED);
        double rate = total == 0 ? 0 : resolved * 100.0 / total;
        return String.format("**%d** complaints resolved, a resolution rate of %.1f%%.", resolved, rate);
    }

    private String help() {
        return "Try asking:\n• Give me a summary\n• How many complaints are pending?\n• Status of #12\n"
                + "• Show high priority issues\n• Which workers are available?\n• Proofs awaiting review\n"
                + "• Most common category\n• What's the average rating?";
    }

    private static String label(Enum<?> status) {
        String s = status.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
