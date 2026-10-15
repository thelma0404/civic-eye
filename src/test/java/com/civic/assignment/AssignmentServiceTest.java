package com.civic.assignment;

import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.notification.NotificationService;
import com.civic.user.User;
import com.civic.worker.Worker;
import com.civic.worker.WorkerRepository;
import com.civic.worker.WorkerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssignmentServiceTest {

    @Mock
    AssignmentRepository assignmentRepository;
    @Mock
    ComplaintRepository complaintRepository;
    @Mock
    WorkerRepository workerRepository;
    @Mock
    NotificationService notificationService;
    @InjectMocks
    AssignmentService assignmentService;

    private Complaint complaint(long id, Complaint.Status status) {
        User citizen = new User();
        citizen.setId(100L);
        citizen.setName("Meera");
        Complaint c = new Complaint();
        c.setId(id);
        c.setTitle("Pothole near school");
        c.setStatus(status);
        c.setCitizen(citizen);
        return c;
    }

    private Worker worker(long id, WorkerStatus status) {
        Worker w = new Worker();
        w.setId(id);
        w.setName("Ravi Kumar");
        w.setStatus(status);
        return w;
    }

    private Assignment assignment(long id, Complaint c, Worker w, AssignmentStatus status) {
        Assignment a = new Assignment();
        a.setId(id);
        a.setComplaint(c);
        a.setWorker(w);
        a.setStatus(status);
        return a;
    }

    @Test
    void assignComplaint_assignsWorkerAndNotifiesBothSides() {
        Complaint c = complaint(1L, Complaint.Status.PENDING);
        Worker w = worker(5L, WorkerStatus.AVAILABLE);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(c));
        when(workerRepository.findById(5L)).thenReturn(Optional.of(w));
        when(assignmentRepository.findFirstByComplaintIdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        Assignment a = assignmentService.assignComplaint(1L, 5L, "  Bring safety cones ");

        assertEquals(AssignmentStatus.ASSIGNED, a.getStatus());
        assertEquals("Bring safety cones", a.getNotes());
        assertEquals(Complaint.Status.ASSIGNED, c.getStatus());
        assertEquals(WorkerStatus.BUSY, w.getStatus());
        verify(notificationService).notifyWorker(eq(5L), anyString(), anyString());
        verify(notificationService).notifyCitizen(eq(100L), anyString(), anyString());
    }

    @Test
    void assignComplaint_rejectsClosedComplaint() {
        when(complaintRepository.findById(2L)).thenReturn(Optional.of(complaint(2L, Complaint.Status.RESOLVED)));

        assertThrows(IllegalArgumentException.class, () -> assignmentService.assignComplaint(2L, 5L, null));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignComplaint_rejectsInactiveWorker() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint(1L, Complaint.Status.PENDING)));
        when(workerRepository.findById(6L)).thenReturn(Optional.of(worker(6L, WorkerStatus.INACTIVE)));

        assertThrows(IllegalArgumentException.class, () -> assignmentService.assignComplaint(1L, 6L, null));
    }

    @Test
    void updateStatus_marksWorkInProgress() {
        Assignment a = assignment(10L, complaint(1L, Complaint.Status.ASSIGNED), worker(5L, WorkerStatus.BUSY), AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        Assignment result = assignmentService.updateStatus(5L, 10L, AssignmentStatus.IN_PROGRESS, "On site now");

        assertEquals(AssignmentStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getStartedAt());
        assertEquals("On site now", result.getWorkerRemarks());
        assertEquals(Complaint.Status.IN_PROGRESS, a.getComplaint().getStatus());
        verify(notificationService).notifyCitizen(eq(100L), anyString(), anyString());
    }

    @Test
    void updateStatus_rejectsAnotherWorkersAssignment() {
        Assignment a = assignment(10L, complaint(1L, Complaint.Status.ASSIGNED), worker(5L, WorkerStatus.BUSY), AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findById(10L)).thenReturn(Optional.of(a));

        assertThrows(IllegalArgumentException.class,
                () -> assignmentService.updateStatus(6L, 10L, AssignmentStatus.IN_PROGRESS, null));
    }

    @Test
    void markCompleted_releasesIdleWorker() {
        Worker w = worker(5L, WorkerStatus.BUSY);
        Assignment a = assignment(10L, complaint(1L, Complaint.Status.IN_PROGRESS), w, AssignmentStatus.IN_PROGRESS);
        when(assignmentRepository.countByWorkerIdAndStatusIn(eq(5L), anyCollection())).thenReturn(0L);

        assignmentService.markCompleted(a);

        assertEquals(AssignmentStatus.COMPLETED, a.getStatus());
        assertNotNull(a.getCompletedAt());
        assertEquals(WorkerStatus.AVAILABLE, w.getStatus());
    }
}
