package com.asg.spindleserp.approval.repository;

import com.asg.spindleserp.approval.entity.ApprovalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalConfigRepository extends JpaRepository<ApprovalConfig, Long> {
    Optional<ApprovalConfig> findByCode(String code);

    List<ApprovalConfig> findByOrganizationIdAndDocumentTypeAndIsActiveTrue(Long orgId, String docType);
}
