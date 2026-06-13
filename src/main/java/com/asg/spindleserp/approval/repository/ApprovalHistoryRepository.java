package com.asg.spindleserp.approval.repository;

import com.asg.spindleserp.approval.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    List<ApprovalHistory> findByApprovalRequestIdOrderByActionAtDesc(Long requestId);
}
