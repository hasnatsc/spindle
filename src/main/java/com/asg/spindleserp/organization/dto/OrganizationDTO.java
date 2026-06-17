package com.asg.spindleserp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * OrganizationDTO
 *
 * Transfer object for Organization CRUD.
 * Follows the same pattern as BusinessUnitDTO:
 *  - @Builder.Default for active = true
 *  - Wrapper Boolean (nullable) for active so JSON null doesn't throw
 *  - Audit fields (createdAt, updatedBy) included for view display
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationDTO {

    private Long id;

    // ── Identity ──────────────────────────────────────────────────────────────

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 200, message = "Name (Bangla) must not exceed 200 characters")
    private String nameBn;

    @Size(max = 5000, message = "About must not exceed 5000 characters")
    private String about;

    // ── Address ───────────────────────────────────────────────────────────────

    private String address;

    @Size(max = 100) private String city;
    @Size(max = 100) private String state;
    @Size(max = 100) private String country;
    @Size(max = 20)  private String postalCode;

    // ── Contact ───────────────────────────────────────────────────────────────

    @Size(max = 20)  private String phone;
    @Size(max = 100) private String email;
    @Size(max = 255) private String website;

    // ── Tax identifiers ───────────────────────────────────────────────────────

    @Size(max = 50) private String taxId;
    @Size(max = 50) private String vatNo;
    @Size(max = 50) private String binNo;

    @Size(max = 500) private String logoUrl;

    // ── Status & audit ────────────────────────────────────────────────────────

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
