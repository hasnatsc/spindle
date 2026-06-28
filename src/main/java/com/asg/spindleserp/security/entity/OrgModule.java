package com.asg.spindleserp.security.entity;

import com.asg.spindleserp.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * OrgModule — one row per "this organization has access to this module".
 *
 * ══════════════════════════════════════════════════════════════════════
 * DESIGN
 * ══════════════════════════════════════════════════════════════════════
 *
 * Super admin controls which modules (HRM, SALES, PURCHASE, INVENTORY …)
 * each organization may use. When a module is disabled at org level:
 *   1. DynamicAuthorizationManager rejects all URL patterns for that module.
 *   2. MenuService hides the module's navigation entries.
 *   3. Org admins cannot grant permissions for that module to their users.
 *
 * Table: sec_org_modules
 *
 * Columns:
 *   organization_id  FK → org_organizations.id
 *   module_key       enum string, e.g. "HRM", "SALES", "PURCHASE"
 *   active           true = module is ON for this org
 *   granted_by       username of the superadmin who enabled it
 *   granted_at       timestamp
 *   revoked_by       username of superadmin who last disabled it (nullable)
 *   revoked_at       timestamp (nullable)
 *   notes            free text (nullable)
 */
@Entity
@Table(
    name = "sec_org_modules",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_org_module",
        columnNames = {"organization_id", "module_key"}
    ),
    indexes = {
        @Index(name = "idx_om_org",    columnList = "organization_id"),
        @Index(name = "idx_om_module", columnList = "module_key"),
        @Index(name = "idx_om_active", columnList = "active")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Matches Permission.module values exactly (case-insensitive compare at runtime).
     * Canonical set defined in ModuleKey enum below.
     */
    @Column(name = "module_key", nullable = false, length = 60)
    private String moduleKey;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "granted_by", length = 100)
    private String grantedBy;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "revoked_by", length = 100)
    private String revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(length = 500)
    private String notes;

    // ── Canonical module keys ──────────────────────────────────────────────────
    // These must match the `module` column in sec_permissions exactly.
    // DynamicAuthorizationManager filters by these keys at request time.
    public enum ModuleKey {
        CORE_SECURITY,
        HRM,
        SALES_CUSTOMER_OPERATIONS,
        PURCHASE_SUPPLIER,
        INVENTORY_WAREHOUSE,
        FINANCE_ACCOUNTS,
        PRODUCTION,
        PRODUCT_CATALOG_ECOMMERCE,
        POS,
        CRM,
        COMMUNICATION_NOTIFICATION,
        COMMERCIAL,
        REPORTS_ANALYTICS,
        BUDGET,
        FIXED_ASSETS
    }
}
