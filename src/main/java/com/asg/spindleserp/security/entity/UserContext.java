package com.asg.spindleserp.security.entity;

import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * user_context
 *
 * One row per user (PK = user_id via @MapsId).
 * Stores the CURRENTLY ACTIVE selection — the org/BU/CC/WH the user
 * is working in RIGHT NOW. Written by UserContextController.switch*()
 * and read by UserContextService.loadContext() at login.
 *
 * How it works with the top menu:
 *   1. Admin assigns allowed organizations/BUs/CCs/WHs to the user
 *      (via users-form → sec_user_organizations etc.)
 *   2. Admin optionally sets a default via users-form
 *      (UserService calls UserContextService.saveDefaultContext())
 *   3. On login, LoginSuccessHandler calls UserContextService.loadContext()
 *      which reads this row + the allowed sets → fills UserContextHolder
 *   4. User clicks a different org in the top menu → POST /user-context/switch/organization/{id}
 *      → updates this row → reloads the session holder
 *   5. All modules read via ContextProvider.getOrganizationId() etc. — zero DB
 *
 * DDL (Hibernate auto-creates; run manually if needed):
 *   CREATE TABLE IF NOT EXISTS user_context (
 *       user_id          BIGINT PRIMARY KEY REFERENCES sec_users(id) ON DELETE CASCADE,
 *       organization_id  BIGINT REFERENCES org_organizations(id)   ON DELETE SET NULL,
 *       business_unit_id BIGINT REFERENCES org_business_units(id)  ON DELETE SET NULL,
 *       cost_center_id   BIGINT REFERENCES org_cost_centers(id)    ON DELETE SET NULL,
 *       warehouse_id     BIGINT REFERENCES org_warehouses(id)      ON DELETE SET NULL,
 *       approval_notification_frequency VARCHAR(20) DEFAULT 'IMMEDIATE',
 *       approval_email_enabled    BOOLEAN DEFAULT TRUE,
 *       approval_sms_enabled      BOOLEAN DEFAULT FALSE,
 *       approval_push_enabled     BOOLEAN DEFAULT TRUE,
 *       approval_whatsapp_enabled BOOLEAN DEFAULT FALSE,
 *       approval_default_view     VARCHAR(20) DEFAULT 'PENDING',
 *       approval_refresh_interval INTEGER DEFAULT 60,
 *       approval_sound_enabled    BOOLEAN DEFAULT TRUE,
 *       approval_desktop_notification BOOLEAN DEFAULT TRUE,
 *       show_approval_badge       BOOLEAN DEFAULT TRUE,
 *       last_viewed_notification_id BIGINT
 *   );
 */
@Entity
@Table(name = "user_context")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── Active working context ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id")
    private BusinessUnit businessUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    // ── Approval preferences ───────────────────────────────────────────────
    @Column(name = "approval_notification_frequency", length = 20)
    @Builder.Default private String  approvalNotificationFrequency = "IMMEDIATE";
    @Column(name = "approval_email_enabled")
    @Builder.Default private Boolean approvalEmailEnabled          = Boolean.TRUE;
    @Column(name = "approval_sms_enabled")
    @Builder.Default private Boolean approvalSmsEnabled            = Boolean.FALSE;
    @Column(name = "approval_push_enabled")
    @Builder.Default private Boolean approvalPushEnabled           = Boolean.TRUE;
    @Column(name = "approval_whatsapp_enabled")
    @Builder.Default private Boolean approvalWhatsappEnabled       = Boolean.FALSE;
    @Column(name = "approval_default_view", length = 20)
    @Builder.Default private String  approvalDefaultView           = "PENDING";
    @Column(name = "approval_refresh_interval")
    @Builder.Default private Integer approvalRefreshInterval       = 60;
    @Column(name = "approval_sound_enabled")
    @Builder.Default private Boolean approvalSoundEnabled          = Boolean.TRUE;
    @Column(name = "approval_desktop_notification")
    @Builder.Default private Boolean approvalDesktopNotification   = Boolean.TRUE;
    @Column(name = "show_approval_badge")
    @Builder.Default private Boolean showApprovalBadge             = Boolean.TRUE;
    @Column(name = "last_viewed_notification_id")
    private Long lastViewedNotificationId;

    // ── Convenience ────────────────────────────────────────────────────────
    public Long getOrganizationId()  { return organization  != null ? organization.getId()              : null; }
    public Long getBusinessUnitId()  { return businessUnit  != null ? businessUnit.getId()               : null; }
    public Long getCostCenterId()    { return costCenter    != null ? costCenter.getId()                 : null; }
    public Long getWarehouseId()     { return warehouse     != null ? warehouse.getId()                  : null; }
}
