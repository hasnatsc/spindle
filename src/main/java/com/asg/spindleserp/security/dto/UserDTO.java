package com.asg.spindleserp.security.dto;

import com.asg.spindleserp.security.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO for User create / update / view operations.
 *
 * Notes:
 *   • password is optional on update — UserServiceImpl ignores it when blank
 *   • roleIds → resolved to Role entities by UserServiceImpl
 *   • defaultDashboard uses User.DefaultDashboard enum (all values covered)
 *   • Boolean flags use primitive boolean (not Boolean) — Builder.Default provides true/false
 *
 * Fix over uploaded version:
 *   getDefaultDashboardLabel() had null returns for ACCOUNTS, SALES,
 *   PURCHASE, HRM — now all 10 enum values return a label.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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
    // Validated only when non-blank (create = required, update = optional)

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // ── Preferences ───────────────────────────────────────────────────────────

    private User.DefaultDashboard defaultDashboard;

    // ── Account flags ─────────────────────────────────────────────────────────
    // Use @Builder.Default so the Lombok builder starts these as true.

    @Builder.Default private boolean enabled               = true;
    @Builder.Default private boolean accountNonExpired     = true;
    @Builder.Default private boolean accountNonLocked      = true;
    @Builder.Default private boolean credentialsNonExpired = true;

    // ── Roles ─────────────────────────────────────────────────────────────────

    @Builder.Default private Set<Long>   roleIds   = new HashSet<>();
    @Builder.Default private Set<String> roleNames = new HashSet<>();
    private Integer roleCount;

    // ── Audit ─────────────────────────────────────────────────────────────────

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Human-readable label for all User.DefaultDashboard enum values.
     * Fix: uploaded version returned null for ACCOUNTS, SALES, PURCHASE, HRM.
     */
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

    /** Convenience: is this a create operation? */
    public boolean isNew() {
        return id == null;
    }
}
