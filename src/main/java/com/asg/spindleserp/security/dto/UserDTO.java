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
 * Two additions over the prior version:
 *
 *   1. Boolean (wrapper) for account flags — avoids
 *      "Cannot map null into boolean" when the form omits them.
 *      Null-safe isXxx() helpers return true by default.
 *
 *   2. Default working context fields:
 *      defaultOrganizationId / defaultBusinessUnitId /
 *      defaultCostCenterId   / defaultWarehouseId
 *      These are submitted by users-form and persisted to user_context
 *      by UserService → UserContextService.saveDefaultContext().
 *      On the user's next login, loadContext() picks them up as the
 *      active context.
 *
 * Access-control fields (organizationIds, businessUnitIds, costCenterIds,
 * warehouseIds) are stored in sec_user_organizations etc. via the User's
 * @ManyToMany sets and persisted by UserService.persistScopes().
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;

    // ── Identity ──────────────────────────────────────────────────────────

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

    // ── Password ──────────────────────────────────────────────────────────

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // ── Preferences ───────────────────────────────────────────────────────

    private User.DefaultDashboard defaultDashboard;

    // ── Account flags — Boolean (wrapper) not boolean (primitive) ─────────
    // When the form omits these fields Jackson sets them to null.
    // Primitive boolean cannot hold null → HttpMessageNotReadableException.
    // The isXxx() helpers below treat null as true (safe default).

    @Builder.Default private Boolean enabled               = Boolean.TRUE;
    @Builder.Default private Boolean accountNonExpired     = Boolean.TRUE;
    @Builder.Default private Boolean accountNonLocked      = Boolean.TRUE;
    @Builder.Default private Boolean credentialsNonExpired = Boolean.TRUE;

    // ── Roles ─────────────────────────────────────────────────────────────

    @Builder.Default private Set<Long>   roleIds   = new HashSet<>();
    @Builder.Default private Set<String> roleNames = new HashSet<>();
    private Integer roleCount;

    // ── Allowed scope IDs (stored in sec_user_organizations etc.) ─────────
    // These are the MULTI-select "access control" sets — which orgs/BU/CC/WH
    // the user is PERMITTED to switch to. Stored via User's @ManyToMany.

    @Builder.Default private Set<Long>   organizationIds   = new HashSet<>();
    @Builder.Default private Set<Long>   businessUnitIds   = new HashSet<>();
    @Builder.Default private Set<Long>   costCenterIds     = new HashSet<>();
    @Builder.Default private Set<Long>   warehouseIds      = new HashSet<>();

    // Display names for the view modal (populated on read, ignored on write)
    @Builder.Default private Set<String> organizationNames = new HashSet<>();
    @Builder.Default private Set<String> businessUnitNames = new HashSet<>();
    @Builder.Default private Set<String> costCenterNames   = new HashSet<>();
    @Builder.Default private Set<String> warehouseNames    = new HashSet<>();

    // ── Default working context (stored in user_context table) ────────────
    // Single-select — the org/BU/CC/WH that will be auto-selected for the
    // user on login. Set by admin via the users-form "Default Context" section.
    // Submitted as part of the save payload, persisted by UserContextService.

    private Long   defaultOrganizationId;
    private String defaultOrganizationName;   // read-only, populated on load

    private Long   defaultBusinessUnitId;
    private String defaultBusinessUnitName;

    private Long   defaultCostCenterId;
    private String defaultCostCenterName;

    private Long   defaultWarehouseId;
    private String defaultWarehouseName;

    // ── Audit ─────────────────────────────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // ── Null-safe Boolean helpers (used by UserServiceImpl) ───────────────

    public boolean isEnabled()               { return !Boolean.FALSE.equals(enabled); }
    public boolean isAccountNonExpired()     { return !Boolean.FALSE.equals(accountNonExpired); }
    public boolean isAccountNonLocked()      { return !Boolean.FALSE.equals(accountNonLocked); }
    public boolean isCredentialsNonExpired() { return !Boolean.FALSE.equals(credentialsNonExpired); }

    // ── Misc helpers ──────────────────────────────────────────────────────

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
