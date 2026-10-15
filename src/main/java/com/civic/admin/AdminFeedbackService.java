package com.civic.admin;

import com.civic.feedback.Feedback;
import com.civic.feedback.FeedbackService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminFeedbackService {

    private final FeedbackService feedbackService;

    public AdminFeedbackService(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    public Map<String, Object> getOverview() {
        List<Feedback> all = feedbackService.getAll();
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            int s = star;
            distribution.put(String.valueOf(star), all.stream().filter(f -> f.getRating() == s).count());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", all.size());
        data.put("averageRating", feedbackService.getAverageRating());
        data.put("distribution", distribution);
        data.put("feedback", all);
        return data;
    }
}
