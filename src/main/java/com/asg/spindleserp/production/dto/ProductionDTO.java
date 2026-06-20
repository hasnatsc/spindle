package com.asg.spindleserp.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** ProductionDTO — generic production work order with cost sheet */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductionDTO {
    private Long   id;
    private String productionNo;    // auto-generated

    // BOM (optional) — AJAX Select2 → /production/boms/search
    private Long   bomId;
    private String bomDisplay;

    // Finished item — AJAX Select2
    @NotNull(message = "Finished item is required")
    private Long   finishedItemId;
    private String finishedItemDisplay;

    // Output warehouse — AJAX Select2
    @NotNull(message = "Output warehouse is required")
    private Long   outputWarehouseId;
    private String outputWarehouseDisplay;

    // Cost center (optional) — AJAX Select2
    private Long   costCenterId;
    private String costCenterDisplay;

    // Output UOM
    @NotNull(message = "Output unit is required")
    private Long   outputUnitId;
    private String outputUnitDisplay;

    // Optional link to SO
    private Long   salesOrderId;
    private String salesOrderDisplay;

    @NotNull(message = "Production date is required")
    private LocalDate productionDate;

    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    @NotNull(message = "Planned quantity is required")
    private BigDecimal plannedQuantity;

    @Builder.Default private BigDecimal producedQuantity  = BigDecimal.ZERO;
    @Builder.Default private BigDecimal rejectedQuantity  = BigDecimal.ZERO;
    @Builder.Default private BigDecimal wasteQuantity     = BigDecimal.ZERO;

    // ── Cost sheet ────────────────────────────────────────────────────────────
    @Builder.Default private BigDecimal materialCost = BigDecimal.ZERO;
    @Builder.Default private BigDecimal laborCost    = BigDecimal.ZERO;
    @Builder.Default private BigDecimal overheadCost = BigDecimal.ZERO;
    @Builder.Default private BigDecimal otherCost    = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalCost    = BigDecimal.ZERO;
    @Builder.Default private BigDecimal unitCost     = BigDecimal.ZERO;

    /** DRAFT | SUBMITTED | APPROVED | RELEASED | IN_PROGRESS | COMPLETED | REJECTED | CANCELLED */
    @Builder.Default private String status = "DRAFT";
    private String approvalStatus;
    private String remarks;

    private Long journalEntryId;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;

    private List<InputDTO>  inputs;
    private List<OutputDTO> outputs;

    // ── Input line ────────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InputDTO {
        private Long   id;
        private Integer lineNumber;

        private Long   rawItemId;
        private String rawItemDisplay;

        private Long   lotId;
        private String lotDisplay;

        private Long   warehouseId;
        private String warehouseDisplay;

        private Long   unitId;
        private String unitDisplay;

        private Long   bomItemId;         // which BOM line this satisfies

        private BigDecimal plannedQuantity;
        private BigDecimal actualQuantity;
        @Builder.Default private BigDecimal unitCost      = BigDecimal.ZERO;
        @Builder.Default private BigDecimal totalCost     = BigDecimal.ZERO;
        @Builder.Default private BigDecimal scrapQuantity = BigDecimal.ZERO;
        private String remarks;
    }

    // ── Output line ───────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OutputDTO {
        private Long   id;
        private Integer lineNumber;

        private Long   finishedItemId;
        private String finishedItemDisplay;

        private Long   lotId;
        private String lotDisplay;           // created on completion

        private Long   warehouseId;
        private String warehouseDisplay;

        private Long   unitId;
        private String unitDisplay;

        private BigDecimal quantity;
        @Builder.Default private BigDecimal rejectedQuantity = BigDecimal.ZERO;
        @Builder.Default private BigDecimal unitCost         = BigDecimal.ZERO;
        @Builder.Default private BigDecimal totalCost        = BigDecimal.ZERO;

        private String batchNo;
        private String remarks;
    }
}
