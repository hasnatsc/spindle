package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.Budget;
import com.asg.spindleserp.budget.entity.BudgetTransfer;
import com.asg.spindleserp.budget.entity.BudgetTransfer.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetTransferRepository extends JpaRepository<BudgetTransfer, Long> {

    Optional<BudgetTransfer> findByTransferNo(String transferNo);

    boolean existsByTransferNo(String transferNo);

    List<BudgetTransfer> findByBudgetIdOrderByTransferDateDesc(Long budgetId);

    Page<BudgetTransfer> findByBudgetId(Long budgetId, Pageable pageable);

    List<BudgetTransfer> findByStatus(TransferStatus status);

    Page<BudgetTransfer> findByStatus(TransferStatus status, Pageable pageable);

    List<BudgetTransfer> findByBudgetAndStatus(Budget budget, TransferStatus status);

    Optional<BudgetTransfer> findTopByOrderByIdDesc();

    Optional<BudgetTransfer> findTopByBudgetIdOrderByTransferDateDesc(Long budgetId);

    @Query("""
        select coalesce(sum(bt.transferAmount),0)
        from BudgetTransfer bt
        where bt.fromLine.id = :budgetLineId
          and bt.status = 'APPROVED'
    """)
    BigDecimal getTotalTransferredOut(Long budgetLineId);

    @Query("""
        select coalesce(sum(bt.transferAmount),0)
        from BudgetTransfer bt
        where bt.toLine.id = :budgetLineId
          and bt.status = 'APPROVED'
    """)
    BigDecimal getTotalTransferredIn(Long budgetLineId);

    @Query("""
        select bt
        from BudgetTransfer bt
        where (:budgetId is null or bt.budget.id = :budgetId)
          and (:status is null or bt.status = :status)
          and (:fromDate is null or bt.transferDate >= :fromDate)
          and (:toDate is null or bt.transferDate <= :toDate)
        order by bt.transferDate desc
    """)
    Page<BudgetTransfer> search(
            Long budgetId,
            TransferStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    @Query("""
        select coalesce(max(bt.id),0)
        from BudgetTransfer bt
    """)
    Long getLastTransferId();

}