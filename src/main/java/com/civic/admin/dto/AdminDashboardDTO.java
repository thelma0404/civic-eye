package com.civic.admin.dto;

import com.civic.complaint.Complaint;
import com.civic.proof.WorkProof;

import java.util.List;
import java.util.Map;

public record AdminDashboardDTO(
        long totalComplaints,
        long pending,
        long assigned,
        long inProgress,
        long resolved,
        long rejected,
        long highPriorityOpen,
        long todayComplaints,
        long totalCitizens,
        long totalWorkers,
        long availableWorkers,
        long pendingProofs,
        double averageRating,
        Map<String, Long> byCategory,
        Map<String, Long> last7Days,
        List<Complaint> recentComplaints,
        List<WorkProof> proofQueue) {
}
