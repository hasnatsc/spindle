package com.asg.spindleserp.inventory.transaction.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * StockBalanceDTO — current stock position per item / warehouse / lot.
 * Populated from global_inventory_stock_balances via JdbcTemplate.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockBalanceDTO {
    private Long   id;
    private String itemCode;
    private String itemName;
    private String itemType;
    private String warehouseCode;
    private String warehouseName;
    private String lotNumber;
    private String unitCode;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;   // quantity - reservedQuantity
    private BigDecimal averageCost;
    private BigDecimal stockValue;
    private String lastTransactionTime;
}
