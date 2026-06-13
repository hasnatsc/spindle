package com.asg.spindleserp.global.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryLotRepository extends JpaRepository<InventoryLot, Long>, JpaSpecificationExecutor<InventoryLot> {
    Optional<InventoryLot> findByOrganizationIdAndLotNumber(Long orgId, String lotNumber);

    List<InventoryLot> findByItemIdAndStatus(Long itemId, String status);

    List<InventoryLot> findByOrganizationIdAndItemIdAndDeletedFalse(Long orgId, Long itemId);

    boolean existsByOrganizationIdAndLotNumber(Long orgId, String lotNumber);

    // ★ Find production lots (lots created from a production order)
    List<InventoryLot> findByProductionOrderId(Long productionOrderId);
}
