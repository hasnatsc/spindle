package com.asg.spindleserp.security.entity;

import com.asg.spindleserp.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "sec_users",
    indexes = {
        @Index(name = "idx_user_org",      columnList = "organization_id"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email",    columnList = "email"),
        @Index(name = "idx_user_deleted",  columnList = "deleted")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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

    @Builder.Default @Column(nullable = false) private boolean enabled                = true;
    @Builder.Default @Column(nullable = false) private boolean isEnabled              = true;
    @Builder.Default @Column(nullable = false) private boolean accountNonLocked       = true;
    @Builder.Default @Column(nullable = false) private boolean isAccountNonLocked     = true;
    @Builder.Default @Column(nullable = false) private boolean accountNonExpired      = true;
    @Builder.Default @Column(nullable = false) private boolean isAccountNonExpired    = true;
    @Builder.Default @Column(nullable = false) private boolean credentialsNonExpired  = true;
    @Builder.Default @Column(nullable = false) private boolean isCredentialsNonExpired= true;
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

    public enum DefaultDashboard { DEFAULT, ACCOUNTS, INVENTORY, PRODUCTION, SALES, PURCHASE, HRM }
}

