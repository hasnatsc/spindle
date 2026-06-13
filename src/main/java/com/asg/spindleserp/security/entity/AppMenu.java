package com.asg.spindleserp.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_menus",
        indexes = {
                @Index(name = "idx_menu_parent", columnList = "parent_id"),
                @Index(name = "idx_menu_order", columnList = "display_order"),
                @Index(name = "idx_menu_active", columnList = "active, deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String menuCode;

    @Column(nullable = false, length = 120)
    private String menuName;

    @Column(length = 300)
    private String menuUrl;

    @Column(length = 100)
    private String icon;

    @Column(name = "parent_id")
    private Long parentId;

    @Builder.Default
    @Column(nullable = false)
    private int displayOrder = 0;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String menuType = "LEAF";   // MODULE | GROUP | LEAF

    @Column(length = 80)
    private String moduleName;

    @Column(length = 120)
    private String requiredPermission;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(length = 20)
    private String target = "_self";
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean visible = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(length = 100)
    private String createdBy;
    @Column(length = 100)
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
