package com.civic.admin;

import com.civic.common.util.TextUtil;
import com.civic.notification.Notification;
import com.civic.notification.NotificationService;
import com.civic.user.Role;
import com.civic.user.User;
import com.civic.user.UserRepository;
import com.civic.worker.Worker;
import com.civic.worker.WorkerRepository;
import com.civic.worker.WorkerStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminNotificationService {

    private static final Set<String> AUDIENCES = Set.of("ALL", "CITIZENS", "WORKERS");

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;

    public AdminNotificationService(NotificationService notificationService, UserRepository userRepository,
                                    WorkerRepository workerRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.workerRepository = workerRepository;
    }

    public List<Notification> getInbox() {
        return notificationService.getNotifications(NotificationService.ADMIN, null);
    }

    public long unreadCount() {
        return notificationService.unreadCount(NotificationService.ADMIN, null);
    }

    public void markRead(Long id) {
        notificationService.markRead(NotificationService.ADMIN, null, id);
    }

    public void markAllRead() {
        notificationService.markAllRead(NotificationService.ADMIN, null);
    }

    /** Broadcasts an announcement. Returns how many people received it. */
    @Transactional
    public int send(String audience, String title, String message) {
        String aud = audience == null ? "ALL" : audience.trim().toUpperCase(Locale.ROOT);
        if (!AUDIENCES.contains(aud)) {
            throw new IllegalArgumentException("Choose who should receive the notification");
        }
        String t = TextUtil.clean(title);
        String m = TextUtil.clean(message);
        if (t == null) {
            throw new IllegalArgumentException("Please add a title");
        }
        if (m == null) {
            throw new IllegalArgumentException("Please write a message");
        }
        int count = 0;
        if (aud.equals("ALL") || aud.equals("CITIZENS")) {
            for (User user : userRepository.findByRoleOrderByCreatedAtDesc(Role.CITIZEN)) {
                if (user.isActive()) {
                    notificationService.notifyCitizen(user.getId(), t, m);
                    count++;
                }
            }
        }
        if (aud.equals("ALL") || aud.equals("WORKERS")) {
            for (Worker worker : workerRepository.findByStatusNotOrderByNameAsc(WorkerStatus.INACTIVE)) {
                notificationService.notifyWorker(worker.getId(), t, m);
                count++;
            }
        }
        return count;
    }
}
