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
 * FIXES applied in this version:
 *
 * FIX 1 — implements Serializable:
 *   Spring Session JDBC serializes SecurityContext → Authentication → CustomUserDetails → User
 *   into the SPRING_SESSION_ATTRIBUTES table as a byte[].
 *   Without Serializable the write throws NotSerializableException.
 *
 * FIX 2 — Organization is now FetchType.EAGER:
 *   The original LAZY fetch causes Hibernate to wrap Organization in a
 *   ByteBuddy/CGLib proxy. That proxy is NOT serializable, so even after
 *   adding implements Serializable the error reappears.
 *   For the sec_users → org_organizations join (always 1 row), EAGER is correct.
 *
 * FIX 3 — Duplicate boolean fields REMOVED:
 *   The original entity had both:
 *     private boolean enabled     ← Lombok generates isEnabled() getter
 *     private boolean isEnabled   ← Lombok generates isIsEnabled() getter
 *                                   AND Spring Security calls isEnabled() which
 *                                   finds the first field → confusion + warnings
 *   Solution: keep only ONE field per boolean property (without "is" prefix).
 *   Lombok's @Getter will generate the correct isXxx() getter automatically.
 */
@Entity
@Table(name = "sec_users",
    indexes = {
        @Index(name = "idx_user_org",      columnList = "organization_id"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email",    columnList = "email"),
        @Index(name = "idx_user_deleted",  columnList = "deleted")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User implements Serializable {   // ✅ FIX 1

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ FIX 2: EAGER so Hibernate doesn't wrap in an unserializable proxy
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

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

    // ✅ FIX 3: ONE field per boolean — Lombok generates isEnabled(), isAccountNonLocked(), etc.
    // Removed the duplicate isEnabled, isAccountNonLocked, isAccountNonExpired, isCredentialsNonExpired
    // Spring Security's UserDetails interface calls isEnabled() — Lombok generates this from 'enabled'
    @Builder.Default @Column(nullable = false) private boolean enabled                = true;
    @Builder.Default @Column(nullable = false) private boolean accountNonLocked       = true;
    @Builder.Default @Column(nullable = false) private boolean accountNonExpired      = true;
    @Builder.Default @Column(nullable = false) private boolean credentialsNonExpired  = true;
    @Builder.Default @Column(nullable = false) private boolean deleted                = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DefaultDashboard defaultDashboard;

    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(length = 100) private String createdBy;
    @Column(length = 100) private String updatedBy;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "sec_user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new LinkedHashSet<>();

    public enum DefaultDashboard {
        DEFAULT, ACCOUNTS, INVENTORY, PRODUCTION, SALES, PURCHASE, HRM
    }
}
