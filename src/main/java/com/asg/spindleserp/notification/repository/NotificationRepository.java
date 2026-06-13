package com.asg.spindleserp.notification.repository;

import com.asg.spindleserp.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    long countByRecipientIdAndIsReadFalse(Long userId);

    List<Notification> findByReferenceTypeAndReferenceId(String refType, Long refId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
            "WHERE n.recipient.id = :userId AND n.isRead = false")
    int markAllReadByUser(Long userId);
}
