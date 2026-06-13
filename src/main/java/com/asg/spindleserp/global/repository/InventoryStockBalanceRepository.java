package com.asg.spindleserp.global.repository;

import com.asg.spindleserp.global.entity.InventoryStockBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryStockBalanceRepository extends JpaRepository<InventoryStockBalance, Long> {
    Optional<InventoryStockBalance> findByItemIdAndWarehouseIdAndLotId(
            Long itemId, Long warehouseId, Long lotId);

    List<InventoryStockBalance> findByItemIdAndQuantityGreaterThan(Long itemId, java.math.BigDecimal qty);

    List<InventoryStockBalance> findByWarehouseId(Long warehouseId);

    @Query("SELECT s FROM InventoryStockBalance s WHERE s.item.organizationId = :orgId AND s.quantity > 0")
    List<InventoryStockBalance> findAllPositiveByOrg(@Param("orgId") Long orgId);
}
