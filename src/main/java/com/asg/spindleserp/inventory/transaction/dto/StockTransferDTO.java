package com.asg.spindleserp.inventory.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * StockTransferDTO
 *
 * Maps to DocumentType.STOCK_TRANSFER backed by global_business_documents.
 * Moves stock from one warehouse to another.
 * The service creates TWO InventoryTransaction rows per line:
 *   TRANSFER_OUT from sourceWarehouseId
 *   TRANSFER_IN  to   warehouseId (header)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockTransferDTO {

    private Long id;
    private String documentNo;

    /** Destination warehouse */
    @NotNull(message = "Destination warehouse is required")
    private Long warehouseId;
    private String warehouseName;

    /** Source warehouse */
    @NotNull(message = "Source warehouse is required")
    private Long sourceWarehouseId;
    private String sourceWarehouseName;

    @NotNull(message = "Transfer date is required")
    private LocalDate documentDate;

    /** DRAFT | CONFIRMED | CANCELLED */
    @Builder.Default
    private String status = "DRAFT";

    @Size(max = 100)
    private String referenceNo;

    @Size(max = 100)
    private String vehicleNumber;

    @Size(max = 100)
    private String driverName;

    @Size(max = 1000)
    private String remarks;

    @NotEmpty(message = "At least one transfer line is required")
    @Valid
    private List<LineDTO> lines;

    // Audit
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;

    // ── Line ──────────────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineDTO {
        private Long id;
        private Integer lineNumber;

        @NotNull(message = "Item is required")
        private Long itemId;
        private String itemCode;
        private String itemName;
        private String unitCode;

        private Long lotId;
        private String lotNumber;

        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;

        private BigDecimal grossWeight;
        private BigDecimal netWeight;

        @Size(max = 500)
        private String remarks;
    }
}
