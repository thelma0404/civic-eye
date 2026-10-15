package com.civic.assignment;

import com.civic.common.exception.WorkerNotFoundException;
import com.civic.common.util.TextUtil;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.notification.NotificationService;
import com.civic.worker.Worker;
import com.civic.worker.WorkerRepository;
import com.civic.worker.WorkerStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentService {

    static final List<AssignmentStatus> ACTIVE = List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.IN_PROGRESS);

    private final AssignmentRepository assignmentRepository;
    private final ComplaintRepository complaintRepository;
    private final WorkerRepository workerRepository;
    private final NotificationService notificationService;

    public AssignmentService(AssignmentRepository assignmentRepository, ComplaintRepository complaintRepository,
                             WorkerRepository workerRepository, NotificationService notificationService) {
        this.assignmentRepository = assignmentRepository;
        this.complaintRepository = complaintRepository;
        this.workerRepository = workerRepository;
        this.notificationService = notificationService;
    }

    /** Assigns (or reassigns) a complaint to a worker. */
    @Transactional
    public Assignment assignComplaint(Long complaintId, Long workerId, String notes) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        if (complaint.getStatus() == Complaint.Status.RESOLVED || complaint.getStatus() == Complaint.Status.REJECTED) {
            throw new IllegalArgumentException("This complaint is closed and can't be assigned");
        }
        Worker worker = workerRepository.findById(workerId).orElseThrow(() -> new WorkerNotFoundException(workerId));
        if (worker.getStatus() == WorkerStatus.INACTIVE) {
            throw new IllegalArgumentException("This worker is inactive. Please choose an active worker.");
        }

        assignmentRepository.findFirstByComplaintIdAndStatusIn(complaintId, ACTIVE).ifPresent(existing -> {
            if (existing.getWorker().getId().equals(workerId)) {
                throw new IllegalArgumentException("This complaint is already assigned to " + worker.getName());
            }
            existing.setStatus(AssignmentStatus.CANCELLED);
            assignmentRepository.save(existing);
            releaseWorkerIfIdle(existing.getWorker());
            notificationService.notifyWorker(existing.getWorker().getId(), "Assignment withdrawn",
                    "Complaint #" + complaintId + " has been reassigned to another worker.");
        });

        Assignment assignment = new Assignment();
        assignment.setComplaint(complaint);
        assignment.setWorker(worker);
        assignment.setNotes(TextUtil.clean(notes));
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        Assignment saved = assignmentRepository.save(assignment);

        complaint.setStatus(Complaint.Status.ASSIGNED);
        complaintRepository.save(complaint);
        worker.setStatus(WorkerStatus.BUSY);
        workerRepository.save(worker);

        notificationService.notifyWorker(worker.getId(), "New complaint assigned",
                "You have been assigned complaint #" + complaint.getId() + ": " + complaint.getTitle());
        notificationService.notifyCitizen(complaint.getCitizen().getId(), "Worker assigned",
                "A field worker has been assigned to your complaint #" + complaint.getId() + ".");
        return saved;
    }

    public List<Assignment> getActiveAssignments(Long workerId) {
        return assignmentRepository.findByWorkerIdAndStatusInOrderByAssignedAtDesc(workerId, ACTIVE);
    }

    public List<Assignment> getWorkHistory(Long workerId) {
        return assignmentRepository.findByWorkerIdAndStatusOrderByCompletedAtDesc(workerId, AssignmentStatus.COMPLETED);
    }

    /** Loads an assignment and makes sure it belongs to this worker. */
    public Assignment getWorkerAssignment(Long workerId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
        if (!assignment.getWorker().getId().equals(workerId)) {
            throw new IllegalArgumentException("Assignment not found");
        }
        return assignment;
    }

    /** Workers move work to IN_PROGRESS here; completing it happens by uploading proof. */
    @Transactional
    public Assignment updateStatus(Long workerId, Long assignmentId, AssignmentStatus newStatus, String remarks) {
        Assignment assignment = getWorkerAssignment(workerId, assignmentId);
        if (assignment.getStatus() == AssignmentStatus.COMPLETED || assignment.getStatus() == AssignmentStatus.CANCELLED) {
            throw new IllegalArgumentException("This assignment is already closed");
        }
        if (newStatus != AssignmentStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("You can mark work as in progress here. Upload proof of work to complete it.");
        }
        boolean firstStart = assignment.getStatus() == AssignmentStatus.ASSIGNED;
        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
        if (assignment.getStartedAt() == null) {
            assignment.setStartedAt(LocalDateTime.now());
        }
        String cleanRemarks = TextUtil.clean(remarks);
        if (cleanRemarks != null) {
            assignment.setWorkerRemarks(cleanRemarks);
        }
        Complaint complaint = assignment.getComplaint();
        complaint.setStatus(Complaint.Status.IN_PROGRESS);
        complaintRepository.save(complaint);
        if (firstStart) {
            notificationService.notifyCitizen(complaint.getCitizen().getId(), "Work started",
                    "Work has started on your complaint #" + complaint.getId() + ".");
        }
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void markCompleted(Assignment assignment) {
        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setCompletedAt(now);
        if (assignment.getStartedAt() == null) {
            assignment.setStartedAt(now);
        }
        assignmentRepository.save(assignment);
        releaseWorkerIfIdle(assignment.getWorker());
    }

    /** Sends work back to the worker after an admin rejects the proof. */
    @Transactional
    public void reopen(Assignment assignment) {
        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
        assignment.setCompletedAt(null);
        assignmentRepository.save(assignment);
        Worker worker = assignment.getWorker();
        if (worker.getStatus() != WorkerStatus.INACTIVE) {
            worker.setStatus(WorkerStatus.BUSY);
            workerRepository.save(worker);
        }
    }

    /** Cancels the open assignment of a complaint (used when an admin closes or reopens it). */
    @Transactional
    public void cancelActiveForComplaint(Long complaintId) {
        assignmentRepository.findFirstByComplaintIdAndStatusIn(complaintId, ACTIVE).ifPresent(active -> {
            active.setStatus(AssignmentStatus.CANCELLED);
            assignmentRepository.save(active);
            releaseWorkerIfIdle(active.getWorker());
            notificationService.notifyWorker(active.getWorker().getId(), "Assignment closed",
                    "Complaint #" + complaintId + " was closed by the admin. No further work is needed.");
        });
    }

    public void releaseWorkerIfIdle(Worker worker) {
        if (worker.getStatus() == WorkerStatus.BUSY
                && assignmentRepository.countByWorkerIdAndStatusIn(worker.getId(), ACTIVE) == 0) {
            worker.setStatus(WorkerStatus.AVAILABLE);
            workerRepository.save(worker);
        }
    }

    public List<Assignment> getAssignmentsForComplaint(Long complaintId) {
        return assignmentRepository.findByComplaintIdOrderByAssignedAtDesc(complaintId);
    }

    public Map<String, Long> getWorkerStats(Long workerId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("assigned", assignmentRepository.countByWorkerIdAndStatus(workerId, AssignmentStatus.ASSIGNED));
        stats.put("inProgress", assignmentRepository.countByWorkerIdAndStatus(workerId, AssignmentStatus.IN_PROGRESS));
        stats.put("completed", assignmentRepository.countByWorkerIdAndStatus(workerId, AssignmentStatus.COMPLETED));
        return stats;
    }
}
