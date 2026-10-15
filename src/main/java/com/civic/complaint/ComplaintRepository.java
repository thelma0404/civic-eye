package com.civic.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    List<Complaint> findTop5ByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    Optional<Complaint> findByIdAndCitizenId(Long id, Long citizenId);

    List<Complaint> findByCitizenIdAndStatusOrderByResolvedAtDesc(Long citizenId, Complaint.Status status);

    long countByCitizenId(Long citizenId);

    long countByCitizenIdAndStatus(Long citizenId, Complaint.Status status);

    long countByCitizenIdAndStatusIn(Long citizenId, Collection<Complaint.Status> statuses);

    long countByStatus(Complaint.Status status);

    long countByPriorityAndStatusIn(String priority, Collection<Complaint.Status> statuses);

    long countByCreatedAtAfter(LocalDateTime time);

    long countByResolvedAtAfter(LocalDateTime time);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findTop6ByOrderByCreatedAtDesc();

    List<Complaint> findByStatusOrderByCreatedAtAsc(Complaint.Status status);

    List<Complaint> findByPriorityAndStatusInOrderByCreatedAtAsc(String priority, Collection<Complaint.Status> statuses);

    List<Complaint> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    @Query("select c.category, count(c) from Complaint c group by c.category order by count(c) desc")
    List<Object[]> countGroupedByCategory();
}
