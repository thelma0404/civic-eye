package com.civic.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(String recipientType, Long recipientId);

    List<Notification> findTop5ByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(String recipientType, Long recipientId);

    List<Notification> findByRecipientTypeOrderByCreatedAtDesc(String recipientType);

    long countByRecipientTypeAndRecipientIdAndReadFalse(String recipientType, Long recipientId);

    long countByRecipientTypeAndReadFalse(String recipientType);
}
