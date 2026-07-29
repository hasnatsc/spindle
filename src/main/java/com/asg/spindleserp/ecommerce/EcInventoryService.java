package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.ecommerce.order.entity.EcOrder;
import java.math.BigDecimal;

/**
 * EcInventoryService — ecommerce-to-inventory stock management bridge.
 *
 * Handles stock reservation, deduction, and restoration for the ecommerce
 * order lifecycle:
 *   CONFIRMED  → reserveStock()  — increments reservedQuantity
 *   SHIPPED    → deductStock()   — posts SALES_ISSUE, decrements qty + reservedQty
 *   CANCELLED  → releaseReservation() — decrements reservedQuantity only
 *   RETURNED / REFUNDED → restock()   — posts RETURN_FROM_CUSTOMER
 *
 * Read-only methods provide real stock availability for the storefront.
 */
public interface EcInventoryService {

    /**
     * Sum available qty (quantity - reservedQuantity) across ALL warehouses
     * for an item. Used by the storefront where there is no user warehouse
     * context.
     */
    BigDecimal availableStockAcrossWarehouses(Long itemId);

    /**
     * Available qty for a specific warehouse + item + lot combination.
     * Used by admin operations with a known warehouse context.
     */
    BigDecimal availableStock(Long itemId, Long warehouseId, Long lotId);

    /**
     * Reserve stock: increment reservedQuantity for each EcOrderItem.
     * Called when order status transitions to CONFIRMED.
     */
    void reserveStock(EcOrder order);

    /**
     * Deduct stock: create an ECOMMERCE_SALE BusinessDocument and post
     * SALES_ISSUE for each line. Decrements both quantity and
     * reservedQuantity.
     * Called when order status transitions to SHIPPED.
     */
    void deductStock(EcOrder order);

    /**
     * Release reservation: decrement reservedQuantity for each item.
     * Called when order is CANCELLED from a pre-SHIPPED state.
     */
    void releaseReservation(EcOrder order);

    /**
     * Restock: post RETURN_FROM_CUSTOMER transactions, incrementing
     * quantity for each returned item.
     * Called when order status transitions to RETURNED or REFUNDED.
     */
    void restock(EcOrder order);
}
