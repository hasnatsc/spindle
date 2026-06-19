package com.asg.spindleserp.inventory.transaction.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * StockLedgerDTO — single ledger row returned by JdbcTemplate query.
 * Used in /inventory/stocks (ledger report view).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockLedgerDTO {
    private Long   txId;
    private String documentNo;
    private String documentType;
    private String movementType;
    private LocalDate transactionDate;
    private String warehouseName;
    private String itemCode;
    private String itemName;
    private String unitCode;
    private String lotNumber;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private BigDecimal balanceAfter;
    private String remarks;
    private LocalDateTime createdAt;
    private String createdBy;
}
