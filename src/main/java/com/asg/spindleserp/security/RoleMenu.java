package com.asg.spindleserp.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "sec_mrole_menus",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_menu", columnNames = {"role_id", "menu_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleMenu implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Builder.Default
    @Column(name = "can_view", nullable = false)
    private Boolean canView = true;
    @Builder.Default
    @Column(name = "can_create", nullable = false)
    private Boolean canCreate = false;
    @Builder.Default
    @Column(name = "can_edit", nullable = false)
    private Boolean canEdit = false;
    @Builder.Default
    @Column(name = "can_delete", nullable = false)
    private Boolean canDelete = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
