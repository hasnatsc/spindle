package com.asg.spindleserp.approval.repository;

import com.asg.spindleserp.approval.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long>, JpaSpecificationExecutor<ApprovalRequest> {
    Optional<ApprovalRequest> findByReferenceIdAndDocumentType(Long refId, String docType);

    List<ApprovalRequest> findByCurrentApproverUserIdAndStatus(Long userId, String status);

    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.organization.id = :orgId " +
            "AND ar.status IN ('IN_APPROVAL','SUBMITTED') " +
            "AND ar.currentApproverUser.id = :userId")
    List<ApprovalRequest> findInbox(@Param("orgId") Long orgId, @Param("userId") Long userId);
}
