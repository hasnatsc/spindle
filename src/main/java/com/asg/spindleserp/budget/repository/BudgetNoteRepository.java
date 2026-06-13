package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.BudgetNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetNoteRepository extends JpaRepository<BudgetNote, Long> {
    List<BudgetNote> findByBudgetIdOrderByCreatedAtDesc(Long budgetId);
}
