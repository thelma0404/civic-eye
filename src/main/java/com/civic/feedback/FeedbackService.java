package com.civic.feedback;

import com.civic.common.util.TextUtil;
import com.civic.complaint.Complaint;
import com.civic.complaint.ComplaintRepository;
import com.civic.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;

    public FeedbackService(FeedbackRepository feedbackRepository, ComplaintRepository complaintRepository,
                           NotificationService notificationService) {
        this.feedbackRepository = feedbackRepository;
        this.complaintRepository = complaintRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Feedback submit(Long citizenId, Long complaintId, Integer rating, String comments) {
        if (complaintId == null) {
            throw new IllegalArgumentException("Please choose the complaint you are rating");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Please give a rating between 1 and 5 stars");
        }
        Complaint complaint = complaintRepository.findByIdAndCitizenId(complaintId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        if (complaint.getStatus() != Complaint.Status.RESOLVED) {
            throw new IllegalArgumentException("You can give feedback once the complaint is resolved");
        }
        if (feedbackRepository.existsByComplaintId(complaintId)) {
            throw new IllegalArgumentException("You have already given feedback for this complaint");
        }
        String text = TextUtil.clean(comments);
        if (text != null && text.length() > 1000) {
            throw new IllegalArgumentException("Comments must be 1000 characters or fewer");
        }
        Feedback feedback = new Feedback();
        feedback.setComplaint(complaint);
        feedback.setCitizen(complaint.getCitizen());
        feedback.setRating(rating);
        feedback.setComments(text);
        Feedback saved = feedbackRepository.save(feedback);
        notificationService.notifyAdmins("New feedback: " + rating + "/5",
                "Rating for complaint #" + complaintId + (text != null ? ": " + TextUtil.truncate(text, 120) : ""));
        return saved;
    }

    public List<Feedback> getCitizenFeedback(Long citizenId) {
        return feedbackRepository.findByCitizenIdOrderByCreatedAtDesc(citizenId);
    }

    /** Resolved complaints the citizen hasn't rated yet. */
    public List<Complaint> getPendingFeedback(Long citizenId) {
        return complaintRepository.findByCitizenIdAndStatusOrderByResolvedAtDesc(citizenId, Complaint.Status.RESOLVED)
                .stream().filter(c -> !feedbackRepository.existsByComplaintId(c.getId())).toList();
    }

    public Optional<Feedback> getForComplaint(Long complaintId) {
        return feedbackRepository.findFirstByComplaintId(complaintId);
    }

    public List<Feedback> getAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public double getAverageRating() {
        Double avg = feedbackRepository.averageRating();
        return avg == null ? 0 : Math.round(avg * 10) / 10.0;
    }
}
