package com.asg.spindleserp.crm.repository;

import com.asg.spindleserp.crm.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
    Optional<Lead> findByOrganizationIdAndLeadNo(Long orgId, String no);

    List<Lead> findByOrganizationIdAndStatus(Long orgId, Lead.LeadStatus status);

    List<Lead> findByAssignedToId(Long userId);

    boolean existsByOrganizationIdAndLeadNo(Long orgId, String no);
}
