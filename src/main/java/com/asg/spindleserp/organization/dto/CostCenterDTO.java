package com.asg.spindleserp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * CostCenterDTO
 *
 * Transfer object for CostCenter CRUD.
 * Follows the same pattern as BusinessUnitDTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenterDTO {

    private Long id;

    // ── Parent ────────────────────────────────────────────────────────────────

    @NotNull(message = "Business Unit is required")
    private Long businessUnitId;

    private String businessUnitName;

    private Long   parentCostCenterId;
    private String parentCostCenterName;

    // ── Identity ──────────────────────────────────────────────────────────────

    @NotBlank(message = "Cost center code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String costCenterCode;

    @NotBlank(message = "Cost center name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String costCenterName;

    /**
     * One of: DEPARTMENT, PROJECT, BRANCH, DIVISION, PRODUCT, SERVICE
     * Stored as String — mapped to CostCenter.CostCenterType enum in service.
     */
    @Size(max = 20, message = "Type must not exceed 20 characters")
    private String costCenterType;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    // ── Manager ───────────────────────────────────────────────────────────────

    @Size(max = 100) private String managerName;
    @Size(max = 100) private String managerEmail;

    // ── Status & audit ────────────────────────────────────────────────────────

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
