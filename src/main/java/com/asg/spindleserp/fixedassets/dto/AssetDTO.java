package com.asg.spindleserp.fixedassets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** AssetDTO — mirrors Asset entity */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetDTO {

    private Long   id;

    // ── Category — AJAX Select2 ───────────────────────────────────────────
    @NotNull(message = "Asset category is required")
    private Long   assetCategoryId;
    private String assetCategoryDisplay;    // "{code} — {name}"

    // ── Org references — AJAX Select2 ────────────────────────────────────
    private Long   departmentId;       private String departmentDisplay;
    private Long   costCenterId;       private String costCenterDisplay;
    private Long   warehouseId;        private String warehouseDisplay;
    private Long   responsibleEmployeeId; private String responsibleEmployeeDisplay;
    private Long   supplierId;         private String supplierDisplay;
    private Long   linkedItemId;       private String linkedItemDisplay;
    private Long   linkedGrnId;        private String linkedGrnDisplay;
    private Long   linkedPoId;         private String linkedPoDisplay;

    @NotBlank(message = "Asset code is required")
    @Size(max = 50)
    private String assetCode;

    @NotBlank(message = "Asset name is required")
    @Size(max = 200)
    private String assetName;

    @Size(max = 65535) private String description;
    @Size(max = 100)   private String serialNumber;
    @Size(max = 100)   private String model;
    @Size(max = 100)   private String manufacturer;

    @NotNull(message = "Acquisition date is required")
    private LocalDate acquisitionDate;
    private LocalDate capitalisationDate;

    @NotNull(message = "Purchase cost is required")
    private BigDecimal purchaseCost;

    @Builder.Default private BigDecimal installationCost = BigDecimal.ZERO;

    @Builder.Default private String currency     = "BDT";
    @Builder.Default private BigDecimal exchangeRate = BigDecimal.ONE;

    /** STRAIGHT_LINE | DECLINING_BALANCE | UNITS_OF_PRODUCTION */
    @Builder.Default private String depreciationMethod = "STRAIGHT_LINE";

    private Integer    usefulLifeYears;
    @Builder.Default private BigDecimal residualValue = BigDecimal.ZERO;
    private BigDecimal depreciationRate;
    private LocalDate  depreciationStartDate;

    // ── Computed / running values ─────────────────────────────────────────
    @Builder.Default private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;
    private BigDecimal currentBookValue;
    private LocalDate  lastDepRunDate;

    @Size(max = 200)   private String location;

    /** ACTIVE | DISPOSED | TRANSFERRED | SOLD | WRITTEN_OFF | UNDER_MAINTENANCE */
    @Builder.Default private String status    = "ACTIVE";
    @Builder.Default private String condition = "GOOD";

    private LocalDate warrantyExpiryDate;
    @Size(max = 100) private String insurancePolicyNo;
    private LocalDate insuranceExpiryDate;
    @Size(max = 100) private String barcode;
    @Size(max = 65535) private String notes;

    // ── Audit ─────────────────────────────────────────────────────────────
    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
