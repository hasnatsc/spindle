package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.BudgetHead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetHeadRepository extends JpaRepository<BudgetHead, Long> {
    List<BudgetHead> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<BudgetHead> findByOrganizationIdAndHeadType(Long orgId, BudgetHead.HeadType type);

    List<BudgetHead> findByParentIdIsNullAndOrganizationId(Long orgId);

    Optional<BudgetHead> findByOrganizationIdAndHeadCode(Long orgId, String code);

    boolean existsByOrganizationIdAndHeadCode(Long orgId, String code);
}
