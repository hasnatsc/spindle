package com.asg.spindleserp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DepartmentDTO
 *
 * Transfer object for Department CRUD.
 * Follows the same pattern as BusinessUnitDTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDTO {

    private Long id;

    // ── Parent ────────────────────────────────────────────────────────────────

    @NotNull(message = "Organisation is required")
    private Long organizationId;

    private String organizationName;

    private Long   parentDepartmentId;
    private String parentDepartmentName;

    /** Deferred FK to hrm_employees — kept as Long to avoid circular dependency */
    private Long headEmployeeId;

    // ── Identity ──────────────────────────────────────────────────────────────

    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;          // optional

    @NotBlank(message = "Department name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    // ── Status & audit ────────────────────────────────────────────────────────

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
