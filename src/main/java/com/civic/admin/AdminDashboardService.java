package com.civic.admin;

import com.civic.admin.dto.AdminDashboardDTO;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.feedback.FeedbackService;
import com.civic.proof.ProofRepository;
import com.civic.proof.ProofStatus;
import com.civic.proof.WorkProof;
import com.civic.user.Role;
import com.civic.user.UserRepository;
import com.civic.worker.WorkerRepository;
import com.civic.worker.WorkerStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final ProofRepository proofRepository;
    private final FeedbackService feedbackService;

    public AdminDashboardService(ComplaintRepository complaintRepository, UserRepository userRepository,
                                 WorkerRepository workerRepository, ProofRepository proofRepository,
                                 FeedbackService feedbackService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.workerRepository = workerRepository;
        this.proofRepository = proofRepository;
        this.feedbackService = feedbackService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboard() {
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : complaintRepository.countGroupedByCategory()) {
            byCategory.put((String) row[0], ((Number) row[1]).longValue());
        }

        LocalDate today = LocalDate.now();
        List<Complaint> lastWeek = complaintRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                today.minusDays(6).atStartOfDay(), LocalDateTime.now().plusMinutes(1));
        Map<String, Long> last7Days = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            last7Days.put(day.toString(), lastWeek.stream()
                    .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().equals(day)).count());
        }

        List<WorkProof> queue = proofRepository.findByStatusOrderByUploadedAtAsc(ProofStatus.PENDING);
        List<Complaint.Status> open = List.of(Complaint.Status.PENDING, Complaint.Status.ASSIGNED, Complaint.Status.IN_PROGRESS);

        return new AdminDashboardDTO(
                complaintRepository.count(),
                complaintRepository.countByStatus(Complaint.Status.PENDING),
                complaintRepository.countByStatus(Complaint.Status.ASSIGNED),
                complaintRepository.countByStatus(Complaint.Status.IN_PROGRESS),
                complaintRepository.countByStatus(Complaint.Status.RESOLVED),
                complaintRepository.countByStatus(Complaint.Status.REJECTED),
                complaintRepository.countByPriorityAndStatusIn("HIGH", open),
                last7Days.get(today.toString()),
                userRepository.countByRole(Role.CITIZEN),
                workerRepository.count() - workerRepository.countByStatus(WorkerStatus.INACTIVE),
                workerRepository.countByStatus(WorkerStatus.AVAILABLE),
                queue.size(),
                feedbackService.getAverageRating(),
                byCategory,
                last7Days,
                complaintRepository.findTop6ByOrderByCreatedAtDesc(),
                queue.stream().limit(5).toList());
    }
}
