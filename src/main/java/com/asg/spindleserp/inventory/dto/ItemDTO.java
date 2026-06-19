package com.asg.spindleserp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemDTO {

    private Long id;

    // ── Parent references ─────────────────────────────────────────────────────
    @NotNull(message = "Category is required")
    private Long   categoryId;
    private String categoryName;

    @NotNull(message = "Purchase unit is required")
    private Long   purchaseUnitId;
    private String purchaseUnitCode;

    @NotNull(message = "Sales unit is required")
    private Long   salesUnitId;
    private String salesUnitCode;

    @NotNull(message = "Operation unit is required")
    private Long   operationUnitId;
    private String operationUnitCode;

    private Long   brandId;
    private String brandName;

    private Long   modelId;
    private String modelName;

    // Soft FKs
    private Long   hsCodeId;
    private String hsCode;

    private Long   originId;     // → stp_location_countries
    private String originName;

    // ── Core identification ───────────────────────────────────────────────────
    @NotBlank(message = "Item code is required")
    @Size(max = 50) private String itemCode;

    @NotBlank(message = "Item name is required")
    @Size(max = 200) private String itemName;

    @Size(max = 200) private String itemNameBn;

    /**
     * RAW_MATERIAL | SEMI_FINISHED | FINISHED_GOOD | SERVICE |
     * SPARE_PART | CONSUMABLE | MRO | GENERAL | FIXED_ASSET
     */
    @Builder.Default
    private String itemType = "GENERAL";

    @Size(max = 20) private String unitOfMeasure;   // denormalized op UOM code
    @Size(max = 100) private String sku;
    @Size(max = 100) private String barcode;

    // ── Costing ──────────────────────────────────────────────────────────────
    private BigDecimal costPrice;
    private BigDecimal unitPrice;
    private BigDecimal taxRate;
    private BigDecimal standardCost;

    // ── Stock control ─────────────────────────────────────────────────────────
    private BigDecimal minimumStock;
    private BigDecimal maximumStock;
    private BigDecimal reorderLevel;

    // ── Production ────────────────────────────────────────────────────────────
    private BigDecimal yieldPercent;
    private BigDecimal processLossPct;

    // ── Physical specs ────────────────────────────────────────────────────────
    private BigDecimal weight;
    private BigDecimal volume;
    @Size(max = 100) private String dimensions;

    // ── Shelf life ────────────────────────────────────────────────────────────
    private Integer   shelfLifeDays;
    private LocalDate expiryDate;
    @Builder.Default private Boolean hasLotTracking = false;
    @Builder.Default private Boolean hasSerial      = false;

    // ── Asset-specific ────────────────────────────────────────────────────────
    @Size(max = 100) private String serialNumber;
    @Size(max = 100) private String manufacturer;
    private Integer   warrantyMonths;
    private BigDecimal depreciationRate;

    // ── Chemical / hazardous ──────────────────────────────────────────────────
    @Size(max = 50) private String casNumber;
    @Builder.Default private Boolean isHazardous   = false;
    @Size(max = 255) private String safetyDataSheet;

    // ── Descriptions ─────────────────────────────────────────────────────────
    private String description;
    private String internalNotes;

    // ── Status ───────────────────────────────────────────────────────────────
    @Builder.Default private Boolean active     = true;
    @Builder.Default private Boolean isApproved = false;
    private String approvedBy;
    private LocalDateTime approvedAt;

    // ── Audit ────────────────────────────────────────────────────────────────
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
