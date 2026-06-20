package com.asg.spindleserp.fixedassets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;

/** AssetCategoryDTO — mirrors AssetCategory entity */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetCategoryDTO {

    private Long   id;
    private Long   parentId;
    private String parentName;

    @NotBlank(message = "Category code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Category name is required")
    @Size(max = 200)
    private String name;

    @Size(max = 65535) private String description;

    /** STRAIGHT_LINE | DECLINING_BALANCE | UNITS_OF_PRODUCTION */
    @Builder.Default private String defaultDepMethod = "STRAIGHT_LINE";

    private Integer     defaultUsefulLifeYears;
    private BigDecimal  defaultDepRate;
    private BigDecimal  defaultResidualPct;

    // ── GL Accounts — AJAX Select2 ────────────────────────────────────────
    private Long   glAssetAccountId;       private String glAssetAccountDisplay;
    private Long   glDepExpAccountId;      private String glDepExpAccountDisplay;
    private Long   glAccumDepAccountId;    private String glAccumDepAccountDisplay;
    private Long   glDisposalAccountId;    private String glDisposalAccountDisplay;

    @Builder.Default private Boolean active = true;

    // audit
    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
