package com.civic.notification;

import com.civic.common.util.TextUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class NotificationService {

    public static final String CITIZEN = "CITIZEN";
    public static final String WORKER = "WORKER";
    public static final String ADMIN = "ADMIN";

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification create(String recipientType, Long recipientId, String title, String message, String level) {
        Notification n = new Notification();
        n.setRecipientType(recipientType);
        n.setRecipientId(recipientId);
        n.setTitle(TextUtil.truncate(title, 150));
        n.setMessage(TextUtil.truncate(message, 1000));
        n.setLevel(level == null ? "INFO" : level);
        return notificationRepository.save(n);
    }

    public void notifyCitizen(Long citizenId, String title, String message) {
        create(CITIZEN, citizenId, title, message, levelFor(title));
    }

    public void notifyWorker(Long workerId, String title, String message) {
        create(WORKER, workerId, title, message, levelFor(title));
    }

    public void notifyAdmins(String title, String message) {
        create(ADMIN, null, title, message, levelFor(title));
    }

    public List<Notification> getNotifications(String recipientType, Long recipientId) {
        if (ADMIN.equals(recipientType)) {
            return notificationRepository.findByRecipientTypeOrderByCreatedAtDesc(ADMIN);
        }
        return notificationRepository.findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, recipientId);
    }

    public List<Notification> getLatest(String recipientType, Long recipientId) {
        return notificationRepository.findTop5ByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, recipientId);
    }

    public long unreadCount(String recipientType, Long recipientId) {
        if (ADMIN.equals(recipientType)) {
            return notificationRepository.countByRecipientTypeAndReadFalse(ADMIN);
        }
        return notificationRepository.countByRecipientTypeAndRecipientIdAndReadFalse(recipientType, recipientId);
    }

    @Transactional
    public void markRead(String recipientType, Long recipientId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        boolean owner = recipientType.equals(n.getRecipientType())
                && (ADMIN.equals(recipientType) || Objects.equals(recipientId, n.getRecipientId()));
        if (!owner) {
            throw new IllegalArgumentException("Notification not found");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(String recipientType, Long recipientId) {
        List<Notification> list = getNotifications(recipientType, recipientId);
        list.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(list);
    }

    private static String levelFor(String title) {
        String t = title == null ? "" : title.toLowerCase();
        if (t.contains("resolved") || t.contains("approved") || t.contains("completed")) {
            return "SUCCESS";
        }
        if (t.contains("rejected") || t.contains("withdrawn") || t.contains("closed")) {
            return "WARNING";
        }
        return "INFO";
    }
}
