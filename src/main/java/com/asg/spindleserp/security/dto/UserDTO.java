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
 * ══════════════════════════════════════════════════════════════════════
 * FIX 1 — Boolean (wrapper) not boolean (primitive) for flag fields
 * ══════════════════════════════════════════════════════════════════════
 *
 * ROOT CAUSE:
 *   The form only submits ONE flag: "enabled" (from the checkbox).
 *   The other three flags (accountNonExpired, accountNonLocked,
 *   credentialsNonExpired) are not present in the JSON payload at all,
 *   so Jackson receives null for those fields.
 *
 *   With primitive boolean:  null → cannot be mapped → EXCEPTION
 *     "Cannot map `null` into type `boolean`"
 *
 *   With wrapper Boolean: null → field stays null → no exception.
 *   The service then treats null as "use the existing entity value"
 *   (for update) or the safe default true (for create).
 *
 * PATTERN:
 *   Boolean fields in DTOs that come from JSON must use wrapper Boolean.
 *   The "is" prefix on field names is also dropped so Jackson maps
 *   JSON key "enabled" → field "enabled" cleanly (no getIsEnabled mess).
 *
 * Access-control fields (organizationIds, businessUnitIds, costCenterIds,
 * warehouseIds) are stored in sec_user_access_scopes (soft-link table).
 * They are resolved and passed through the DTO so the form can pre-populate
 * and the service can persist them in one shot.
 *
 * Added: defaultOrganizationId, defaultBusinessUnitId, defaultCostCenterId,
 * defaultWarehouseId — these map to the user_context table and give every
 * module a zero-DB way to seed new documents with the user's working context.
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
    // ✅ FIX 1: Boolean (wrapper) — accepts null from JSON without exception.
    //    When the form omits these fields Jackson deserializes them as null.
    //    The service checks for null and applies a safe default (true).
    //    Using primitive boolean causes:
    //      "Cannot map `null` into type `boolean`" on the first POST.

    @Builder.Default private Boolean enabled               = Boolean.TRUE;
    @Builder.Default private Boolean accountNonExpired     = Boolean.TRUE;
    @Builder.Default private Boolean accountNonLocked      = Boolean.TRUE;
    @Builder.Default private Boolean credentialsNonExpired = Boolean.TRUE;

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

    // ── Default Working Context ───────────────────────────────────────────────
    // Stored in user_context table.
    // Any module reads these via ContextProvider.currentContext() — zero DB hits.
    // Example usage in a service:
    //   UserContextDTO ctx = ContextProvider.currentContext();
    //   Long orgId = ctx.getDefaultOrganizationId();

    private Long   defaultOrganizationId;
    private String defaultOrganizationName;

    private Long   defaultBusinessUnitId;
    private String defaultBusinessUnitName;

    private Long   defaultCostCenterId;
    private String defaultCostCenterName;

    private Long   defaultWarehouseId;
    private String defaultWarehouseName;

    // ── Audit ─────────────────────────────────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Safe null-guard accessor used by the service when building the entity */
    public boolean isEnabled()               { return enabled               == null || Boolean.TRUE.equals(enabled); }
    public boolean isAccountNonExpired()     { return accountNonExpired     == null || Boolean.TRUE.equals(accountNonExpired); }
    public boolean isAccountNonLocked()      { return accountNonLocked      == null || Boolean.TRUE.equals(accountNonLocked); }
    public boolean isCredentialsNonExpired() { return credentialsNonExpired == null || Boolean.TRUE.equals(credentialsNonExpired); }

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
