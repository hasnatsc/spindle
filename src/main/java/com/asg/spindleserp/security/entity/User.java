package com.asg.spindleserp.security.entity;

import com.asg.spindleserp.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  sec_users                                                       ║
 * ║                                                                  ║
 * ║  Three fixes applied over the uploaded version:                  ║
 * ║                                                                  ║
 * ║  FIX 1 — implements Serializable                                ║
 * ║    Spring Session JDBC serializes the full SecurityContext       ║
 * ║    (CustomUserDetails → User → Organization → Role → Permission)║
 * ║    into SPRING_SESSION_ATTRIBUTES as a byte[].                   ║
 * ║    Every class in that graph must be Serializable.               ║
 * ║                                                                  ║
 * ║  FIX 2 — Organization FetchType.EAGER                           ║
 * ║    LAZY creates a ByteBuddy proxy that is NOT serializable.      ║
 * ║    Organization is always needed (1 row, 1 join) so EAGER        ║
 * ║    is correct and cheap.                                         ║
 * ║                                                                  ║
 * ║  FIX 3 — ONE boolean per flag (no "is" prefix on field names)   ║
 * ║    Lombok @Getter on `boolean enabled` generates isEnabled().    ║
 * ║    A second field `boolean isEnabled` would generate             ║
 * ║    isIsEnabled() causing ambiguity with Spring Security.         ║
 * ║                                                                  ║
 * ║  FIX 4 — @PrePersist / @PreUpdate for audit timestamps          ║
 * ║    The uploaded version had no lifecycle callbacks; createdAt    ║
 * ║    and updatedAt were never set automatically.                   ║
 * ║                                                                  ║
 * ║  Soft-delete: set deleted = true; never physically remove rows. ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(
    name = "sec_users",
    indexes = {
        @Index(name = "idx_user_org",      columnList = "organization_id"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email",    columnList = "email"),
        @Index(name = "idx_user_phone",    columnList = "phone"),
        @Index(name = "idx_user_deleted",  columnList = "deleted")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uq_user_email",    columnNames = "email"),
        @UniqueConstraint(name = "uq_user_phone",    columnNames = "phone")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements Serializable {   // ✅ FIX 1

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Organization ──────────────────────────────────────────────────────────
    // ✅ FIX 2: EAGER — avoids unserializable ByteBuddy/CGLIB proxy
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // ── Identity ──────────────────────────────────────────────────────────────

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Optional — used as alternative login identifier. */
    @Column(nullable = false, unique = true, length = 30)
    private String phone;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 200)
    private String fullName;

    // ── UserDetails flags ─────────────────────────────────────────────────────
    // ✅ FIX 3: ONE field per boolean. Lombok generates:
    //   isEnabled(), isAccountNonLocked(), isAccountNonExpired(), isCredentialsNonExpired()
    // Spring Security's UserDetails interface expects exactly those method names.

    @Builder.Default @Column(nullable = false) private boolean enabled                = true;
    @Builder.Default @Column(nullable = false) private boolean accountNonLocked       = true;
    @Builder.Default @Column(nullable = false) private boolean accountNonExpired      = true;
    @Builder.Default @Column(nullable = false) private boolean credentialsNonExpired  = true;
    @Builder.Default @Column(nullable = false) private boolean deleted                = false;

    // ── Preferences ───────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "default_dashboard", length = 35)
    private DefaultDashboard defaultDashboard;

    // ── Roles ─────────────────────────────────────────────────────────────────
    // EAGER: Spring Security builds the GrantedAuthority set at login.
    // Keep role count small (1–2 per user) to keep this safe.

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "sec_user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new LinkedHashSet<>();

    // ── Audit ─────────────────────────────────────────────────────────────────

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

    // ── Lifecycle hooks ───────────────────────────────────────────────────────
    // ✅ FIX 4: ensure timestamps are set automatically

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum DefaultDashboard {
        DEFAULT,
        ACCOUNTS,
        INVENTORY,
        PRODUCTION,
        SALES,
        PURCHASE,
        HRM,
        COMMERCIAL,
        HR,
        FINANCE
    }
}
