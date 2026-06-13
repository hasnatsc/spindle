package com.asg.spindleserp.approval.repository;

import com.asg.spindleserp.approval.entity.ApprovalDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalDelegationRepository extends JpaRepository<ApprovalDelegation, Long> {
    List<ApprovalDelegation> findByDelegatorIdAndStatus(Long delegatorId, String status);

    List<ApprovalDelegation> findByDelegateIdAndStatus(Long delegateId, String status);
}
