package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.BudgetRevisionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BudgetRevisionLineRepository extends JpaRepository<BudgetRevisionLine, Long> {

    List<BudgetRevisionLine> findByRevisionId(Long revisionId);

    List<BudgetRevisionLine> findByBudgetLineId(Long budgetLineId);

    List<BudgetRevisionLine> findByRevisionIdOrderByIdAsc(Long revisionId);

    long countByRevisionId(Long revisionId);

    boolean existsByRevisionId(Long revisionId);

    boolean existsByBudgetLineId(Long budgetLineId);

    void deleteByRevisionId(Long revisionId);

    void deleteByBudgetLineId(Long budgetLineId);

    @Query("""
        select coalesce(sum(
            case
                when r.direction = '+' then r.changeAmount
                else -r.changeAmount
            end
        ), 0)
        from BudgetRevisionLine r
        where r.budgetLine.id = :budgetLineId
    """)
    BigDecimal getNetRevisionAmount(Long budgetLineId);

    @Query("""
        select coalesce(sum(r.changeAmount),0)
        from BudgetRevisionLine r
        where r.revision.id = :revisionId
          and r.direction = '+'
    """)
    BigDecimal getTotalIncrease(Long revisionId);

    @Query("""
        select coalesce(sum(r.changeAmount),0)
        from BudgetRevisionLine r
        where r.revision.id = :revisionId
          and r.direction = '-'
    """)
    BigDecimal getTotalDecrease(Long revisionId);

}