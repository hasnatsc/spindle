package com.asg.spindleserp.security.entity;

import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * sec_users
 * <p>
 * Added over the previous version:
 * • organizations      — @ManyToMany via sec_user_organizations
 * (which orgs this user is PERMITTED to access)
 * • allowedBusinessUnits — via sec_user_org_business_units
 * • allowedCostCenters   — via sec_user_org_cost_centers
 * • allowedWarehouses    — via sec_user_warehouses
 * <p>
 * These are the ALLOWED scopes the user can switch between via the top menu.
 * The ACTIVE selection is stored in user_context (UserContext entity).
 * <p>
 * All four sets use FetchType.LAZY intentionally — they are only needed
 * when building the context at login (one query via findByUsernameWithAllContext)
 * and when populating the top-menu switcher. They must NOT be EAGER because
 * that would load potentially large sets on every security filter check.
 */
@Entity
@Table(
        name = "sec_users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_deleted", columnList = "deleted")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_username", columnNames = "username"),
                @UniqueConstraint(name = "uq_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uq_user_phone", columnNames = "phone")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Primary organization ───────────────────────────────────────────────
    // The org the user was created under. EAGER so it serializes into the
    // Spring Session without a ByteBuddy proxy.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // ── Identity ──────────────────────────────────────────────────────────
    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 200)
    private String fullName;

    // ── UserDetails flags ─────────────────────────────────────────────────
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean accountNonLocked = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean accountNonExpired = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean credentialsNonExpired = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    // ── Preferences ───────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "default_dashboard", length = 35)
    private DefaultDashboard defaultDashboard;

    // ── Security roles (EAGER — needed at every request for DynamicAuthorizationManager) ──
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "sec_user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new LinkedHashSet<>();

    // ── Allowed scope collections (LAZY — loaded only at login / context switch) ──────────

    /**
     * Organizations this user is allowed to operate in (multi-org support).
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sec_user_organizations", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "organization_id"))
    private Set<Organization> organizations = new LinkedHashSet<>();

    /**
     * Business units this user is allowed to select in the top-menu switcher.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sec_user_org_business_units", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "business_unit_id"))
    private Set<BusinessUnit> allowedBusinessUnits = new LinkedHashSet<>();

    /**
     * Cost centers this user is allowed to select.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sec_user_org_cost_centers", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "cost_center_id"))
    private Set<CostCenter> allowedCostCenters = new LinkedHashSet<>();

    /**
     * Warehouses this user is allowed to select.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sec_user_warehouses", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "warehouse_id"))
    private Set<Warehouse> allowedWarehouses = new LinkedHashSet<>();

    // ── Audit ─────────────────────────────────────────────────────────────
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Enum ──────────────────────────────────────────────────────────────
    public enum DefaultDashboard {
        DEFAULT, ACCOUNTS, INVENTORY, PRODUCTION,
        SALES, PURCHASE, HRM, COMMERCIAL, HR, FINANCE
    }
}
