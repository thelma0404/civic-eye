package com.civic.proof;

import com.civic.assignment.Assignment;
import com.civic.assignment.AssignmentService;
import com.civic.assignment.AssignmentStatus;
import com.civic.common.util.FileUploadUtil;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.notification.NotificationService;
import com.civic.user.User;
import com.civic.worker.Worker;
import com.civic.worker.WorkerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProofServiceTest {

    @Mock
    ProofRepository proofRepository;
    @Mock
    AssignmentService assignmentService;
    @Mock
    ComplaintRepository complaintRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    FileUploadUtil fileUploadUtil;
    @InjectMocks
    ProofService proofService;

    private Assignment assignment(AssignmentStatus status, Complaint.Status complaintStatus) {
        User citizen = new User();
        citizen.setId(100L);
        Complaint complaint = new Complaint();
        complaint.setId(1L);
        complaint.setTitle("Overflowing drain");
        complaint.setStatus(complaintStatus);
        complaint.setCitizen(citizen);
        Worker worker = new Worker();
        worker.setId(5L);
        worker.setName("Ravi Kumar");
        worker.setStatus(WorkerStatus.BUSY);
        Assignment a = new Assignment();
        a.setId(10L);
        a.setComplaint(complaint);
        a.setWorker(worker);
        a.setStatus(status);
        return a;
    }

    private WorkProof pendingProof(Assignment a) {
        WorkProof p = new WorkProof();
        p.setId(1L);
        p.setAssignment(a);
        p.setImagePath("/uploads/work-proof/x.jpg");
        p.setStatus(ProofStatus.PENDING);
        return p;
    }

    @Test
    void uploadProof_savesPendingProofAndCompletesAssignment() {
        Assignment a = assignment(AssignmentStatus.IN_PROGRESS, Complaint.Status.IN_PROGRESS);
        MockMultipartFile file = new MockMultipartFile("file", "done.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(assignmentService.getWorkerAssignment(5L, 10L)).thenReturn(a);
        when(proofRepository.existsByAssignmentIdAndStatus(10L, ProofStatus.PENDING)).thenReturn(false);
        when(fileUploadUtil.saveFile(file, "work-proof")).thenReturn("/uploads/work-proof/x.jpg");
        when(proofRepository.save(any(WorkProof.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkProof proof = proofService.uploadProof(5L, 10L, file, " Drain cleared ");

        assertEquals(ProofStatus.PENDING, proof.getStatus());
        assertEquals("/uploads/work-proof/x.jpg", proof.getImagePath());
        assertEquals("Drain cleared", proof.getRemarks());
        verify(assignmentService).markCompleted(a);
        verify(notificationService).notifyAdmins(anyString(), anyString());
    }

    @Test
    void uploadProof_requiresAPhoto() {
        Assignment a = assignment(AssignmentStatus.IN_PROGRESS, Complaint.Status.IN_PROGRESS);
        when(assignmentService.getWorkerAssignment(5L, 10L)).thenReturn(a);
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> proofService.uploadProof(5L, 10L, empty, null));
        verify(proofRepository, never()).save(any());
    }

    @Test
    void uploadProof_isBlockedWhilePreviousProofAwaitsReview() {
        Assignment a = assignment(AssignmentStatus.COMPLETED, Complaint.Status.IN_PROGRESS);
        when(assignmentService.getWorkerAssignment(5L, 10L)).thenReturn(a);
        when(proofRepository.existsByAssignmentIdAndStatus(10L, ProofStatus.PENDING)).thenReturn(true);
        MockMultipartFile file = new MockMultipartFile("file", "done.jpg", "image/jpeg", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> proofService.uploadProof(5L, 10L, file, null));
        verify(fileUploadUtil, never()).saveFile(any(), anyString());
    }

    @Test
    void reviewProof_approvalResolvesComplaint() {
        Assignment a = assignment(AssignmentStatus.COMPLETED, Complaint.Status.IN_PROGRESS);
        WorkProof proof = pendingProof(a);
        when(proofRepository.findById(1L)).thenReturn(Optional.of(proof));
        when(proofRepository.save(any(WorkProof.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkProof result = proofService.reviewProof(1L, true, "Looks good");

        assertEquals(ProofStatus.APPROVED, result.getStatus());
        assertEquals(Complaint.Status.RESOLVED, a.getComplaint().getStatus());
        assertNotNull(a.getComplaint().getResolvedAt());
        verify(notificationService).notifyCitizen(eq(100L), anyString(), anyString());
        verify(assignmentService, never()).reopen(any());
    }

    @Test
    void reviewProof_rejectionSendsWorkBackToTheWorker() {
        Assignment a = assignment(AssignmentStatus.COMPLETED, Complaint.Status.IN_PROGRESS);
        WorkProof proof = pendingProof(a);
        when(proofRepository.findById(1L)).thenReturn(Optional.of(proof));
        when(proofRepository.save(any(WorkProof.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkProof result = proofService.reviewProof(1L, false, "Photo is blurry");

        assertEquals(ProofStatus.REJECTED, result.getStatus());
        assertEquals("Photo is blurry", result.getReviewComment());
        assertEquals(Complaint.Status.IN_PROGRESS, a.getComplaint().getStatus());
        verify(assignmentService).reopen(a);
        verify(notificationService).notifyWorker(eq(5L), anyString(), anyString());
    }

    @Test
    void reviewProof_rejectionNeedsAReason() {
        WorkProof proof = pendingProof(assignment(AssignmentStatus.COMPLETED, Complaint.Status.IN_PROGRESS));
        when(proofRepository.findById(1L)).thenReturn(Optional.of(proof));

        assertThrows(IllegalArgumentException.class, () -> proofService.reviewProof(1L, false, "  "));
        assertEquals(ProofStatus.PENDING, proof.getStatus());
    }
}
