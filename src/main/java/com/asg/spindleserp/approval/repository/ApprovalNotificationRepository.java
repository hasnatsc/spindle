package com.asg.spindleserp.approval.repository;

import com.asg.spindleserp.approval.entity.ApprovalNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalNotificationRepository extends JpaRepository<ApprovalNotification, Long> {
    List<ApprovalNotification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);
    long countByRecipientIdAndIsReadFalse(Long recipientId);
}
