package com.asg.spindleserp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * WarehouseDTO
 *
 * Transfer object for Warehouse CRUD.
 * Follows the same pattern as BusinessUnitDTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseDTO {

    private Long id;

    // ── Parent ────────────────────────────────────────────────────────────────

    @NotNull(message = "Business Unit is required")
    private Long businessUnitId;

    private String businessUnitName;
    private String organizationName;    // denormalised — org of the BU

    // ── Identity ──────────────────────────────────────────────────────────────

    @NotBlank(message = "Warehouse code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String warehouseCode;

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String warehouseName;

    @NotBlank(message = "Item type is required")
    @Size(max = 30, message = "Item type must not exceed 30 characters")
    private String itemType;            // Enum name stored as String

    // ── Contact / location ────────────────────────────────────────────────────

    private String address;

    @Size(max = 100) private String managerName;
    @Size(max = 20)  private String contactNumber;

    // ── Status & audit ────────────────────────────────────────────────────────

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
