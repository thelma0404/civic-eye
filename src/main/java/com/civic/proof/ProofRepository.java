package com.civic.proof;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProofRepository extends JpaRepository<WorkProof, Long> {

    List<WorkProof> findByAssignmentIdOrderByUploadedAtDesc(Long assignmentId);

    List<WorkProof> findByAssignmentComplaintIdOrderByUploadedAtDesc(Long complaintId);

    List<WorkProof> findByStatusOrderByUploadedAtAsc(ProofStatus status);

    boolean existsByAssignmentIdAndStatus(Long assignmentId, ProofStatus status);

    long countByStatus(ProofStatus status);

    long countByAssignmentWorkerIdAndStatus(Long workerId, ProofStatus status);
}
