package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long>, JpaSpecificationExecutor<Budget> {
    Optional<Budget> findByOrganizationIdAndBudgetNo(Long orgId, String no);

    List<Budget> findByOrganizationIdAndStatus(Long orgId, Budget.BudgetStatus status);

    List<Budget> findByFiscalYearIdAndOrganizationId(Long fyId, Long orgId);

    boolean existsByOrganizationIdAndBudgetNo(Long orgId, String no);
}
