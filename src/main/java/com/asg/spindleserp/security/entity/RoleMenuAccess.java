package com.asg.spindleserp.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sec_mrole_menus",
        uniqueConstraints = @UniqueConstraint(name = "uq_rma_role_menu",
                columnNames = {"role_id", "menu_id"}),
        indexes = {
                @Index(name = "idx_rma_role", columnList = "role_id"),
                @Index(name = "idx_rma_menu", columnList = "menu_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleMenuAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private AppMenu menu;

    @Builder.Default
    @Column(nullable = false)
    private boolean canView = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean canCreate = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean canEdit = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean canDelete = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
