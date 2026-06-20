package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.Budget;
import com.asg.spindleserp.budget.entity.BudgetRevision;
import com.asg.spindleserp.budget.entity.BudgetRevision.RevisionStatus;
import com.asg.spindleserp.budget.entity.BudgetRevision.RevisionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRevisionRepository extends JpaRepository<BudgetRevision, Long> {

    Optional<BudgetRevision> findByBudgetIdAndRevisionNumber(Long budgetId, Integer revisionNumber);

    Optional<BudgetRevision> findByRevisionNo(String revisionNo);

    List<BudgetRevision> findByBudgetIdOrderByRevisionNumberDesc(Long budgetId);

    Page<BudgetRevision> findByBudgetId(Long budgetId, Pageable pageable);

    List<BudgetRevision> findByStatus(RevisionStatus status);

    List<BudgetRevision> findByRevisionType(RevisionType revisionType);

    List<BudgetRevision> findByBudgetAndStatus(Budget budget, RevisionStatus status);

    boolean existsByBudgetIdAndRevisionNumber(Long budgetId, Integer revisionNumber);

    long countByBudgetId(Long budgetId);

    @Query("""
        select coalesce(max(br.revisionNumber), 0)
        from BudgetRevision br
        where br.budget.id = :budgetId
    """)
    Integer getMaxRevisionNumber(Long budgetId);

    @Query("""
        select br
        from BudgetRevision br
        where br.budget.id = :budgetId
        order by br.revisionNumber desc
    """)
    List<BudgetRevision> findLatestRevisions(Long budgetId);

    @Query("""
        select br
        from BudgetRevision br
        where (:budgetId is null or br.budget.id = :budgetId)
          and (:status is null or br.status = :status)
          and (:type is null or br.revisionType = :type)
    """)
    Page<BudgetRevision> search(
            Long budgetId,
            RevisionStatus status,
            RevisionType type,
            Pageable pageable
    );

    Optional<BudgetRevision> findTopByBudgetIdOrderByRevisionNumberDesc(Long budgetId);

}