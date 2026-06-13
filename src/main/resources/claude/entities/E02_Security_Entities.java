// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E02  Security / Auth                                       ║
// ║  Tables: sec_roles, sec_permissions, sec_users, sec_user_roles,           ║
// ║           sec_role_permissions, app_menus, sec_mrole_menus               ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: security/entity/Role.java ──────────────────────────────────────────
package com.hasnat.optimum.security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "sec_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 250)
    private String nameBn;

    @Column(length = 255)
    private String description;

    @Column(length = 60)
    private String masterRole;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sec_role_permissions",
        joinColumns        = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new LinkedHashSet<>();
}


// ── FILE: security/entity/Permission.java ────────────────────────────────────
package com.hasnat.optimum.security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sec_permissions",
    indexes = {
        @Index(name = "idx_perm_name",   columnList = "name"),
        @Index(name = "idx_perm_module", columnList = "module")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String urlPattern;

    @Column(length = 10)
    private String httpMethod;

    @Column(length = 50)
    private String category;

    @Column(length = 80)
    private String module;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Module {
        CORE_SECURITY, HRM, SALES_CUSTOMER_OPERATIONS, PURCHASE_SUPPLIER,
        INVENTORY_WAREHOUSE, FINANCE_ACCOUNTS, PRODUCTION, PRODUCT_CATALOG_ECOMMERCE,
        POS, CRM, COMMUNICATION_NOTIFICATION, COMMERCIAL, REPORTS_ANALYTICS,
        BUDGET, FIXED_ASSETS
    }
}


// ── FILE: security/entity/User.java ──────────────────────────────────────────
package com.hasnat.optimum.security.entity;

import com.hasnat.optimum.organization.entity.Organization;
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


// ── FILE: security/entity/AppMenu.java ───────────────────────────────────────
package com.hasnat.optimum.security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_menus",
    indexes = {
        @Index(name = "idx_menu_parent", columnList = "parent_id"),
        @Index(name = "idx_menu_order",  columnList = "display_order"),
        @Index(name = "idx_menu_active", columnList = "active, deleted")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppMenu {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Builder.Default @Column(length = 20)   private String target  = "_self";
    @Builder.Default @Column(nullable = false) private boolean active  = true;
    @Builder.Default @Column(nullable = false) private boolean visible = true;
    @Builder.Default @Column(nullable = false) private boolean deleted = false;

    @Column(length = 100) private String createdBy;
    @Column(length = 100) private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


// ── FILE: security/entity/RoleMenuAccess.java ────────────────────────────────
package com.hasnat.optimum.security.entity;

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
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleMenuAccess {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private AppMenu menu;

    @Builder.Default @Column(nullable = false) private boolean canView   = true;
    @Builder.Default @Column(nullable = false) private boolean canCreate = false;
    @Builder.Default @Column(nullable = false) private boolean canEdit   = false;
    @Builder.Default @Column(nullable = false) private boolean canDelete = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
