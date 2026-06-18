package com.asg.spindleserp.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HsCodeDTO {

    private Long id;

    @NotNull(message = "Organisation is required")
    private Long organizationId;

    @NotBlank(message = "HS Code is required")
    @Size(max = 20, message = "HS Code must not exceed 20 characters")
    private String hsCode;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 200) private String shortDescription;

    /** EXPORT | IMPORT | BOTH */
    @Builder.Default
    private String hsType = "BOTH";

    // Duty / tax percentages
    private BigDecimal vatPercent;
    private BigDecimal customsDutyPercent;
    private BigDecimal supplementaryDutyPercent;
    private BigDecimal aitPercent;

    // Flags
    @Builder.Default private Boolean active                = true;
    @Builder.Default private Boolean bondedAllowed         = false;
    @Builder.Default private Boolean requiresImportPermit  = false;
    @Builder.Default private Boolean requiresExportPermit  = false;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
