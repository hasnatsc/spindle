package com.asg.spindleserp.approval.repository;

import com.asg.spindleserp.approval.entity.ApprovalLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalLevelRepository extends JpaRepository<ApprovalLevel, Long> {
    List<ApprovalLevel> findByApprovalConfigIdOrderByLevelNumberAsc(Long configId);
}
