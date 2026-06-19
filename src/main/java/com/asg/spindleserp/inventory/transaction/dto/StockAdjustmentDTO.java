package com.asg.spindleserp.inventory.transaction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * StockAdjustmentDTO
 *
 * Maps to DocumentType.STOCK_ADJUSTMENT backed by global_business_documents.
 * Adjustment lines describe either IN (+) or OUT (-) movements.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockAdjustmentDTO {

    private Long id;
    private String documentNo;

    private Long warehouseId;
    private String warehouseName;

    @NotNull(message = "Adjustment date is required")
    private LocalDate documentDate;

    /** DRAFT | CONFIRMED | CANCELLED */
    @Builder.Default
    private String status = "DRAFT";

    @Size(max = 100)
    private String referenceNo;

    @Size(max = 1000)
    private String remarks;

    @NotEmpty(message = "At least one adjustment line is required")
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

        /** Lot ID — optional; null means non-lot-tracked item */
        private Long lotId;
        private String lotNumber;

        /**
         * ADJUSTMENT_IN  → quantity must be positive
         * ADJUSTMENT_OUT → quantity must be positive (sign applied by service)
         */
        @NotBlank(message = "Movement direction is required")
        private String movementType;   // ADJUSTMENT_IN | ADJUSTMENT_OUT

        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;

        private BigDecimal unitCost;
        private BigDecimal lineAmount;

        @Size(max = 500)
        private String remarks;
    }
}
