package com.asg.spindleserp.global.repository;

import com.asg.spindleserp.global.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByItemIdOrderByCreatedAtDesc(Long itemId);

    List<InventoryTransaction> findByBusinessDocumentId(Long docId);

    List<InventoryTransaction> findByOrganizationIdAndTransactionDate(
            Long orgId, java.time.LocalDate date);
}
