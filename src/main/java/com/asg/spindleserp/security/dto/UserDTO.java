package com.asg.spindleserp.security.dto;

import com.asg.spindleserp.security.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * UserDTO — create / update / view.
 *
 * Access-control fields (organizationIds, businessUnitIds, costCenterIds,
 * warehouseIds) are stored in sec_user_access_scopes (soft-link table).
 * They are resolved and passed through the DTO so the form can pre-populate
 * and the service can persist them in one shot.
 *
 * Boolean flags use primitive boolean with @Builder.Default = true.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;

    // ── Identity ──────────────────────────────────────────────────────────────

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 80, message = "Username must be 3–80 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    private String email;

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    private String phone;

    @Size(max = 200, message = "Full name cannot exceed 200 characters")
    private String fullName;

    // ── Password ──────────────────────────────────────────────────────────────
    // Required on create (≥8 chars), ignored when blank on update

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // ── Preferences ───────────────────────────────────────────────────────────

    private User.DefaultDashboard defaultDashboard;

    // ── Account flags ─────────────────────────────────────────────────────────

    @Builder.Default private boolean enabled               = true;
    @Builder.Default private boolean accountNonExpired     = true;
    @Builder.Default private boolean accountNonLocked      = true;
    @Builder.Default private boolean credentialsNonExpired = true;

    // ── Roles ─────────────────────────────────────────────────────────────────

    @Builder.Default private Set<Long>   roleIds   = new HashSet<>();
    @Builder.Default private Set<String> roleNames = new HashSet<>();
    private Integer roleCount;

    // ── Access Control ────────────────────────────────────────────────────────
    // IDs of orgs / BUs / cost-centers / warehouses the user is allowed to use.
    // Empty = unrestricted (super-admin / org-admin behaviour).

    @Builder.Default private Set<Long> organizationIds  = new HashSet<>();
    @Builder.Default private Set<Long> businessUnitIds  = new HashSet<>();
    @Builder.Default private Set<Long> costCenterIds    = new HashSet<>();
    @Builder.Default private Set<Long> warehouseIds     = new HashSet<>();

    // Display names — populated on read, ignored on write
    @Builder.Default private Set<String> organizationNames  = new HashSet<>();
    @Builder.Default private Set<String> businessUnitNames  = new HashSet<>();
    @Builder.Default private Set<String> costCenterNames    = new HashSet<>();
    @Builder.Default private Set<String> warehouseNames     = new HashSet<>();

    // ── Audit ─────────────────────────────────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getDefaultDashboardLabel() {
        if (defaultDashboard == null) return "Default";
        return switch (defaultDashboard) {
            case DEFAULT    -> "Default";
            case ACCOUNTS   -> "Accounts";
            case INVENTORY  -> "Inventory";
            case PRODUCTION -> "Production";
            case SALES      -> "Sales";
            case PURCHASE   -> "Purchase";
            case HRM        -> "HRM";
            case COMMERCIAL -> "Commercial";
            case HR         -> "HR";
            case FINANCE    -> "Finance";
        };
    }

    public boolean isNew() { return id == null; }
}
