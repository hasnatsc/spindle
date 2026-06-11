package com.asg.spindleserp.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "sec_menus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menu implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_code", nullable = false, unique = true, length = 50)
    private String menuCode;

    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    @Column(name = "menu_name_bn", length = 200)
    private String menuNameBn;
    @Column(name = "menu_url", length = 255)
    private String menuUrl;
    @Column(length = 50)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_menu_id")
    private Menu parentMenu;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(length = 255)
    private String description;
    @Column(name = "required_permission", length = 100)
    private String requiredPermission;
    @Column(length = 50)
    private String module;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;
    @Column(length = 20)
    @Builder.Default
    private String target = "_self";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
