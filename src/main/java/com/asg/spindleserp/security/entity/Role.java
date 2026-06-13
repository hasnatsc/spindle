package com.asg.spindleserp.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * FIX: implements Serializable
 *
 * User.roles is Set<Role>. When Java serializes User it must also
 * serialize every Role in that set — and every Permission in each Role.
 * Without Serializable on Role the chain breaks.
 *
 * Also removed the duplicate 'active' / 'isActive' boolean — same problem
 * as User: Lombok would generate isActive() AND isIsActive(), causing
 * confusion and potential Spring issues.
 */
@Entity
@Table(name = "sec_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role implements Serializable {   // ✅ FIX

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 250)
    private String nameBn;

    @Column(length = 255)
    private String description;

    @Column(length = 60)
    private String masterRole;

    // ✅ Keep only ONE boolean field (Lombok generates isActive() getter)
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "sec_role_permissions",
        joinColumns        = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new LinkedHashSet<>();
}
