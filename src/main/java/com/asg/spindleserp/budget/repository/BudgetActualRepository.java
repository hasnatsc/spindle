package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.BudgetActual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetActualRepository extends JpaRepository<BudgetActual, Long> {
    List<BudgetActual> findByBudgetLineId(Long lineId);

    List<BudgetActual> findByJournalEntryId(Long journalEntryId);
}
