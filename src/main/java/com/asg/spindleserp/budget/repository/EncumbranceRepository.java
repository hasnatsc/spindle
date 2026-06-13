package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.Encumbrance;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface EncumbranceRepository extends JpaRepository<Encumbrance, Long> {
    List<Encumbrance> findByBudgetLineIdAndStatus(Long lineId, Encumbrance.EncumbranceStatus status);
    List<Encumbrance> findBySourceDocumentId(Long docId);

    @Query(value = "SELECT id, outstanding_amount FROM bgt_encumbrances " +
                   "WHERE budget_line_id = :lineId AND status IN ('OPEN','PARTIAL')",
           nativeQuery = true)
    List<Object[]> findOutstandingByLine(@Param("lineId") Long lineId);
}

