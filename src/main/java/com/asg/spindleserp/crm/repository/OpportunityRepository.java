package com.asg.spindleserp.crm.repository;

import com.asg.spindleserp.crm.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
    Optional<Opportunity> findByOrganizationIdAndOpportunityNo(Long orgId, String no);

    List<Opportunity> findByOrganizationIdAndStage(Long orgId, Opportunity.OpportunityStage stage);

    boolean existsByOrganizationIdAndOpportunityNo(Long orgId, String no);
}
