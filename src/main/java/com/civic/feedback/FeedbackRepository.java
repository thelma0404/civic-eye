package com.civic.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByCitizenIdOrderByCreatedAtDesc(Long citizenId);

    Optional<Feedback> findFirstByComplaintId(Long complaintId);

    boolean existsByComplaintId(Long complaintId);

    List<Feedback> findAllByOrderByCreatedAtDesc();

    List<Feedback> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    long countByRatingLessThanEqual(int rating);

    @Query("select avg(f.rating) from Feedback f")
    Double averageRating();
}
