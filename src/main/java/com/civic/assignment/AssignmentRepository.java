package com.civic.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByWorkerIdAndStatusInOrderByAssignedAtDesc(Long workerId, Collection<AssignmentStatus> statuses);

    List<Assignment> findByWorkerIdAndStatusOrderByCompletedAtDesc(Long workerId, AssignmentStatus status);

    Optional<Assignment> findFirstByComplaintIdAndStatusIn(Long complaintId, Collection<AssignmentStatus> statuses);

    List<Assignment> findByComplaintIdOrderByAssignedAtDesc(Long complaintId);

    long countByWorkerIdAndStatusIn(Long workerId, Collection<AssignmentStatus> statuses);

    long countByWorkerIdAndStatus(Long workerId, AssignmentStatus status);

    List<Assignment> findByAssignedAtBetween(LocalDateTime from, LocalDateTime to);
}
