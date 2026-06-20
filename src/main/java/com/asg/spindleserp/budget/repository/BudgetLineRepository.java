package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.BudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BudgetLineRepository extends JpaRepository<BudgetLine, Long> {
    List<BudgetLine> findByBudgetIdOrderByLineNumber(Long budgetId);

    List<BudgetLine> findByBudgetIdAndBudgetHeadId(Long budgetId, Long headId);

//    @Query(value = "SELECT id, available_amount FROM bgt_budget_lines WHERE budget_id = :budgetId",
    @Query(value = "SELECT id, revised_amount FROM bgt_budget_lines WHERE budget_id = :budgetId",
            nativeQuery = true)
    List<Object[]> findAvailableAmounts(@Param("budgetId") Long budgetId);

    @Transactional
    long deleteByBudgetId(Long budgetId);
}
