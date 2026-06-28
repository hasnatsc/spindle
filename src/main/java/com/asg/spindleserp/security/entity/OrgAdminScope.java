package com.asg.spindleserp.security.entity;

import com.asg.spindleserp.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * OrgAdminScope — designates a user as an org-level administrator.
 *
 * ══════════════════════════════════════════════════════════════════════
 * PURPOSE
 * ══════════════════════════════════════════════════════════════════════
 *
 * A user with ROLE_ORG_ADMIN can:
 *   • Create / update / delete users within their org only.
 *   • Assign any Role whose masterRole is their org's role prefix OR
 *     any role that is listed in sec_org_module_roles for their org.
 *   • NOT grant permissions for modules their org doesn't have access to.
 *   • NOT touch other orgs' data.
 *
 * Super admin grants org-admin status via the Org Admin management UI.
 * One user can be admin of multiple orgs (rare but supported).
 *
 * Table: sec_org_admin_scopes
 */
@Entity
@Table(
    name = "sec_org_admin_scopes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_oas_user_org",
        columnNames = {"user_id", "organization_id"}
    ),
    indexes = {
        @Index(name = "idx_oas_user", columnList = "user_id"),
        @Index(name = "idx_oas_org",  columnList = "organization_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrgAdminScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Whether the admin grant is currently active. */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "granted_by",  length = 100) private String grantedBy;
    @Column(name = "granted_at")                private LocalDateTime grantedAt;

    @Column(length = 500)                       private String notes;
}
