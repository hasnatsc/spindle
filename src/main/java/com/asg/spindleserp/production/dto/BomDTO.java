package com.asg.spindleserp.production.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/** BomDTO — Bill of Materials header with item lines */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BomDTO {
    private Long   id;

    // Finished item — AJAX Select2 → /inventory/items/search
    @NotNull(message = "Finished item is required")
    private Long   finishedItemId;
    private String finishedItemDisplay;  // "{code} — {name}"

    // Output UOM — AJAX Select2 → /inventory/uoms/active
    @NotNull(message = "Output unit is required")
    private Long   outputUnitId;
    private String outputUnitDisplay;

    @NotBlank(message = "BOM code is required")
    @Size(max = 50)
    private String bomCode;

    @NotBlank(message = "BOM name is required")
    @Size(max = 200)
    private String bomName;

    @Builder.Default private String bomVersion = "1.0";

    @NotNull(message = "Output quantity is required")
    @Builder.Default private BigDecimal outputQuantity = BigDecimal.ONE;

    /** Expected yield % — 100 = no waste, 90 = 10% expected waste */
    @Builder.Default private BigDecimal yieldPercent = new BigDecimal("100.00");

    @Builder.Default private Boolean active    = true;
    @Builder.Default private Boolean isDefault = false;

    private String description;
    private String notes;
    private String approvedBy;
    private String approvedAt;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;

    @Valid @NotEmpty(message = "At least one raw material line is required")
    private List<ItemDTO> items;

    // ── BOM line ──────────────────────────────────────────────────────────────
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemDTO {
        private Long   id;
        private Integer lineNumber;

        @NotNull(message = "Raw item is required on BOM line")
        private Long   rawItemId;
        private String rawItemDisplay;

        @NotNull(message = "Unit is required")
        private Long   unitId;
        private String unitDisplay;

        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;

        @Builder.Default private BigDecimal scrapPct  = BigDecimal.ZERO;
        @Builder.Default private Boolean    isOptional = false;
        private String remarks;
    }
}
