// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  SpindleERP — Complete Spring Boot Entity Layer                         ║
// ║  Package: com.asg.spindleserp                                           ║
// ║  Java 21 | Spring Boot 3.5 | JPA/Hibernate | Lombok                    ║
// ║  All entities extend BaseAuditEntity or BaseOrgEntity.                  ║
// ║  Multi-tenancy enforced via organization_id on every org-scoped table.  ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ════════════════════════════════════════════════════════════════════════════
// FILE 1:  com/asg/spindleserp/common/BaseAuditEntity.java
// ════════════════════════════════════════════════════════════════════════════
package com.asg.spindleserp.common;

import com.asg.spindleserp.security.ContextProvider;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BaseAuditEntity — Spring Data Auditing + org isolation.
 *
 * Rules:
 *  1. organization MUST be set before createdBy in all create() methods.
 *  2. @CreatedBy / @LastModifiedBy are populated by SpringSecurityAuditorAware.
 *  3. Never use @RequiredArgsConstructor when @Lazy injection is needed.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onPrePersist() {
        if (this.organization == null) {
            this.organization = ContextProvider.getOrganizationReference();
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// FILE 2:  com/asg/spindleserp/common/BaseOrgEntity.java
// ════════════════════════════════════════════════════════════════════════════
package com.asg.spindleserp.common;

import com.asg.spindleserp.security.ContextProvider;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BaseOrgEntity — lightweight base without Spring Data Auditing.
 * Suitable for entities that manage createdBy/updatedBy manually.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseOrgEntity implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void enforceOrganization() {
        Long ctxOrgId = ContextProvider.getOrganizationId();
        if (ctxOrgId == null) throw new IllegalStateException("No organization in security context");
        if (this.organization == null) {
            this.organization = ContextProvider.getOrganizationReference();
        } else if (!this.organization.getId().equals(ctxOrgId)) {
            throw new IllegalStateException("Cross-organization write is not allowed");
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// FILE 3:  com/asg/spindleserp/common/BaseReferenceEntity.java
// ════════════════════════════════════════════════════════════════════════════
package com.asg.spindleserp.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BaseReferenceEntity — for global reference/lookup tables that are NOT
 * organisation-scoped (countries, currencies, HS codes, etc.).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseReferenceEntity implements Serializable {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 1 — CORE / SECURITY
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/security/Organization.java
package com.asg.spindleserp.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "org_organizations",
    indexes = {
        @Index(name = "idx_org_active", columnList = "is_active")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"businessUnits"})
public class Organization implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "name_bn", length = 200)
    private String nameBn;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "vat_registration_no", length = 50)
    private String vatRegistrationNo;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "BDT";

    @Column(length = 100)
    @Builder.Default
    private String country = "Bangladesh";

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String website;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "fiscal_year_start")
    private LocalDate fiscalYearStart;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    @Builder.Default
    private List<BusinessUnit> businessUnits = new ArrayList<>();
}

// FILE: com/asg/spindleserp/security/BusinessUnit.java
package com.asg.spindleserp.security;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_business_units",
    uniqueConstraints = @UniqueConstraint(name = "uk_bu_org_code", columnNames = {"organization_id","code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessUnit extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

// FILE: com/asg/spindleserp/security/User.java
package com.asg.spindleserp.security;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sec_users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"roles","organizations","allowedBusinessUnits","allowedWarehouses","allowedCostCenters"})
public class User implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "first_name", length = 100) private String firstName;
    @Column(name = "last_name",  length = 100) private String lastName;
    @Column(length = 100) private String email;
    @Column(length = 20)  private String phone;
    @Column(name = "profile_picture", length = 500) private String profilePicture;

    @Builder.Default @Column(nullable = false) private Boolean enabled = true;
    @Builder.Default @Column(name = "account_non_locked",       nullable = false) private Boolean accountNonLocked      = true;
    @Builder.Default @Column(name = "account_non_expired",      nullable = false) private Boolean accountNonExpired      = true;
    @Builder.Default @Column(name = "credentials_non_expired",  nullable = false) private Boolean credentialsNonExpired  = true;
    @Builder.Default @Column(name = "must_change_password",     nullable = false) private Boolean mustChangePassword     = false;

    @Column(name = "last_login_at")      private LocalDateTime lastLoginAt;
    @Column(name = "password_changed_at") private LocalDateTime passwordChangedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "sec_user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "sec_user_organizations",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "organization_id"))
    @Builder.Default
    private Set<Organization> organizations = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "sec_user_business_units",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "business_unit_id"))
    @Builder.Default
    private Set<BusinessUnit> allowedBusinessUnits = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "sec_user_warehouses",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "warehouse_id"))
    @Builder.Default
    private Set<Warehouse> allowedWarehouses = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "sec_user_cost_centers",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "cost_center_id"))
    @Builder.Default
    private Set<CostCenter> allowedCostCenters = new HashSet<>();

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// FILE: com/asg/spindleserp/security/Role.java
package com.asg.spindleserp.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sec_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "name_bn", length = 250)  private String nameBn;
    @Column(length = 255)                     private String description;
    @Column(name = "master_role", length = 30) private String masterRole; // ADMIN|MANAGER|USER|VIEWER

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sec_role_permissions",
        joinColumns        = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// FILE: com/asg/spindleserp/security/Permission.java
package com.asg.spindleserp.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "sec_permissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "url_pattern", nullable = false, length = 255)
    private String urlPattern;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(length = 255) private String description;
    @Column(length = 50)  private String category;
    @Column(length = 50)  private String module;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// FILE: com/asg/spindleserp/security/Menu.java
package com.asg.spindleserp.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "sec_menus")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Menu implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_code", nullable = false, unique = true, length = 50)
    private String menuCode;

    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    @Column(name = "menu_name_bn", length = 200) private String menuNameBn;
    @Column(name = "menu_url",     length = 255) private String menuUrl;
    @Column(length = 50)                         private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_menu_id")
    private Menu parentMenu;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(length = 255)                              private String description;
    @Column(name = "required_permission", length = 100) private String requiredPermission;
    @Column(length = 50)                               private String module;

    @Builder.Default @Column(name = "is_active",  nullable = false) private Boolean isActive  = true;
    @Builder.Default @Column(name = "is_visible", nullable = false) private Boolean isVisible = true;
    @Column(length = 20) @Builder.Default private String target = "_self";

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// FILE: com/asg/spindleserp/security/RoleMenu.java
package com.asg.spindleserp.security;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "sec_mrole_menus",
    uniqueConstraints = @UniqueConstraint(name = "uk_role_menu", columnNames = {"role_id","menu_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleMenu implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Builder.Default @Column(name = "can_view",   nullable = false) private Boolean canView   = true;
    @Builder.Default @Column(name = "can_create", nullable = false) private Boolean canCreate = false;
    @Builder.Default @Column(name = "can_edit",   nullable = false) private Boolean canEdit   = false;
    @Builder.Default @Column(name = "can_delete", nullable = false) private Boolean canDelete = false;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 2 — LOCATION & REFERENCE MASTERS
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/security/locations/Country.java
package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "stp_countries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Country extends BaseReferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 2, nullable = false, unique = true)    private String code;
    @Column(nullable = false, length = 100)                 private String name;
    @Builder.Default @Column(nullable = false)              private Boolean active = true;
}

// FILE: com/asg/spindleserp/security/locations/State.java
package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "stp_states")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class State extends BaseReferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "country_id", nullable = false) private Country country;
    @Column(length = 10)  private String code;
    @Column(nullable = false, length = 100) private String name;
    @Builder.Default @Column(nullable = false) private Boolean active = true;
}

// FILE: com/asg/spindleserp/stp/Currency.java
package com.asg.spindleserp.stp;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "stp_currencies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Currency extends BaseReferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 3, nullable = false, unique = true)    private String code;
    @Column(nullable = false, length = 100)                 private String name;
    @Column(length = 10)                                    private String symbol;
    @Builder.Default @Column(name = "decimal_places", nullable = false) private Integer decimalPlaces = 2;
    @Builder.Default @Column(nullable = false)              private Boolean active = true;
}

// FILE: com/asg/spindleserp/stp/HsCode.java
package com.asg.spindleserp.stp;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity @Table(name = "stp_hs_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HsCode extends BaseReferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)                  private Long id;
    @Column(nullable = false, unique = true, length = 20)                    private String code;
    @Column(length = 500)                                                    private String description;
    @Column(name = "duty_rate", precision = 5, scale = 2)                   private BigDecimal dutyRate;
    @Builder.Default @Column(name = "is_active", nullable = false)           private Boolean isActive = true;
}

// FILE: com/asg/spindleserp/stp/GlobalTermsCondition.java
package com.asg.spindleserp.stp;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.approval.DocumentType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stp_global_terms_conditions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GlobalTermsCondition extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "terms_text", columnDefinition = "TEXT")
    private String termsText;

    @Builder.Default @Column(name = "is_default", nullable = false) private Boolean isDefault = false;
    @Builder.Default @Column(name = "is_active",  nullable = false) private Boolean isActive  = true;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 3 — INVENTORY MASTERS & ITEMS
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/inventory/item/ItemCategory.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inv_item_categories",
    uniqueConstraints = @UniqueConstraint(name = "uk_item_cat_org_code", columnNames = {"organization_id","code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemCategory extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(columnDefinition = "TEXT") private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ItemCategory parent;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", length = 100) private String createdBy;
}

// FILE: com/asg/spindleserp/inventory/setup/UnitsOfMeasure.java
package com.asg.spindleserp.inventory.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stp_units_of_measure",
    uniqueConstraints = @UniqueConstraint(name = "uk_uom_org_code", columnNames = {"organization_id","code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitsOfMeasure extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 20) private String symbol;
    @Column(name = "uom_category", nullable = false, length = 30) private String uomCategory; // WEIGHT|COUNT|LENGTH|VOLUME|AREA
    @Builder.Default @Column(name = "is_base_unit", nullable = false) private Boolean isBaseUnit = false;
    @Builder.Default @Column(name = "conversion_factor", nullable = false, precision = 12, scale = 6) private BigDecimal conversionFactor = BigDecimal.ONE;
    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
}

// FILE: com/asg/spindleserp/inventory/item/YarnType.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yrn_types",
    uniqueConstraints = @UniqueConstraint(name = "uk_yrn_type_org_code", columnNames = {"organization_id","type_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class YarnType extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "type_code", nullable = false, length = 30)   private String typeCode;
    @Column(name = "type_name", nullable = false, length = 100)  private String typeName;
    @Column(name = "type_name_short", length = 30)               private String typeNameShort;
    @Column(columnDefinition = "TEXT")                           private String description;
    @Builder.Default @Column(name = "is_active",   nullable = false) private Boolean isActive   = true;
    @Builder.Default @Column(name = "is_approved", nullable = false) private Boolean isApproved = false;
    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "created_by",  length = 100) private String createdBy;
    @Column(name = "updated_by",  length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/inventory/item/YarnCount.java  (same pattern — condensed)
// FILE: com/asg/spindleserp/inventory/item/YarnPly.java
// FILE: com/asg/spindleserp/inventory/item/YarnBlend.java
// [These follow identical pattern to YarnType with their own fields]

// FILE: com/asg/spindleserp/inventory/item/InventoryItem.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseAuditEntity;
import com.asg.spindleserp.inventory.setup.ItemType;
import com.asg.spindleserp.security.locations.Country;
import com.asg.spindleserp.stp.HsCode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inv_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inv_item_code", columnNames = {"organization_id","item_code"}),
        @UniqueConstraint(name = "uk_inv_item_name", columnNames = {"organization_id","item_name"})
    },
    indexes = {
        @Index(name = "idx_inv_item_org",  columnList = "organization_id"),
        @Index(name = "idx_inv_item_type", columnList = "item_type"),
        @Index(name = "idx_inv_item_code", columnList = "item_code")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryItem extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── References ──────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_unit_id", nullable = false)
    private UnitsOfMeasure operationUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_unit_id")
    private UnitsOfMeasure purchaseUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_unit_id")
    private UnitsOfMeasure salesUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hs_code_id")
    private HsCode hsCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id")
    private Country origin;

    // ── Identity ─────────────────────────────────────────────────────────────
    @Column(name = "item_code", nullable = false, length = 50) private String itemCode;
    @Column(name = "item_name", nullable = false, length = 200) private String itemName;
    @Column(name = "item_name_bn", length = 200) private String itemNameBn;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 100) private String barcode;
    @Column(length = 100) private String sku;
    @Column(name = "origin_name", length = 100) private String originName;

    // ── Discriminator ────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private ItemType itemType = ItemType.GENERAL;

    @Column(name = "unit_of_measure",    nullable = false, length = 20) private String unitOfMeasure;
    @Column(name = "purchase_unit_code", nullable = false, length = 20) private String purchaseUnitCode;
    @Column(name = "sales_unit_code",    nullable = false, length = 20) private String salesUnitCode;

    // ── Stock control ────────────────────────────────────────────────────────
    @Column(name = "reorder_level", precision = 12, scale = 3) private BigDecimal reorderLevel;
    @Column(name = "minimum_stock", precision = 12, scale = 3) private BigDecimal minimumStock;
    @Column(name = "maximum_stock", precision = 12, scale = 3) private BigDecimal maximumStock;
    @Column(name = "unit_price",    precision = 12, scale = 4) private BigDecimal unitPrice;
    @Column(name = "cost_price",    precision = 12, scale = 4) private BigDecimal costPrice;
    @Column(name = "tax_rate",      precision = 5,  scale = 2) private BigDecimal taxRate;

    // ── Fiber/Cotton fields (item_type IN FIBER, RAW_COTTON) ─────────────────
    @Column(name = "fiber_type", length = 30) private String fiberType;
    @Column(length = 50) private String grade;
    @Column(name = "staple_length", precision = 8, scale = 2) private BigDecimal stapleLength;
    @Column(precision = 8, scale = 2) private BigDecimal micronaire;
    @Column(precision = 8, scale = 2) private BigDecimal strength;
    @Column(precision = 8, scale = 2) private BigDecimal moisture;
    @Column(precision = 5, scale = 2) private BigDecimal trash;
    @Column(precision = 5, scale = 2) private BigDecimal purity;

    // ── Chemical/Dye fields (item_type IN CHEMICALS, DYES) ──────────────────
    @Column(name = "chemical_formula", length = 50)  private String chemicalFormula;
    @Column(name = "cas_number",       length = 50)  private String casNumber;
    @Builder.Default @Column(name = "is_hazardous") private Boolean isHazardous = false;
    @Column(name = "safety_data_sheet", length = 100) private String safetyDataSheet;
    @Column(precision = 8, scale = 2) private BigDecimal concentration;
    @Column(name = "chem_expiry_date") private LocalDateTime chemExpiryDate;

    // ── Fixed Asset fields (item_type = FIXED_ASSET) ─────────────────────────
    @Column(length = 100) private String manufacturer;
    @Column(length = 100) private String model;
    @Column(name = "serial_number",   length = 50) private String serialNumber;
    @Column(name = "warranty_months") private Integer warrantyMonths;
    @Column(name = "asset_value",        precision = 15, scale = 2) private BigDecimal assetValue;
    @Column(name = "depreciation_rate",  precision = 5,  scale = 2) private BigDecimal depreciationRate;

    // ── Production & costing ─────────────────────────────────────────────────
    @Column(name = "yield_percent",         precision = 5,  scale = 2) private BigDecimal yieldPercent;
    @Column(name = "standard_cost_per_kg",  precision = 12, scale = 2) private BigDecimal standardCostPerKg;
    @Column(name = "selling_price_per_kg",  precision = 12, scale = 2) private BigDecimal sellingPricePerKg;
    @Column(name = "process_loss_percent",  precision = 5,  scale = 2) private BigDecimal processLossPercent;

    // ── Approval & status ────────────────────────────────────────────────────
    @Builder.Default @Column(name = "is_active",   nullable = false) private Boolean isActive   = true;
    @Builder.Default @Column(name = "is_approved", nullable = false) private Boolean isApproved = false;
    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "approved_at")               private LocalDateTime approvedAt;

    // ── 1:1 yarn extension ───────────────────────────────────────────────────
    @OneToOne(mappedBy = "item", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private YarnItem yarnItem;
}

// FILE: com/asg/spindleserp/inventory/item/YarnItem.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yarn_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class YarnItem extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_type_id") private YarnType  yarnType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_count_id") private YarnCount yarnCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_ply_id")   private YarnPly  yarnPly;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_blend_id") private YarnBlend yarnBlend;

    @Column(name = "quality_grade", length = 50) private String qualityGrade;
    @Column(name = "display_name",  length = 500) private String displayName;

    /** Builds display name: "30/2 Combed 100% Cotton" */
    @PostLoad @PostPersist @PostUpdate
    public void buildDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (yarnCount != null && yarnPly != null)
            sb.append(yarnCount.getCountName()).append("/").append(yarnPly.getPlyNumber());
        else if (yarnCount != null) sb.append(yarnCount.getCountName());
        if (yarnType  != null && !yarnType.getTypeName().isBlank()) sb.append(" ").append(yarnType.getTypeName());
        if (yarnBlend != null && !yarnBlend.getBlendName().isBlank()) sb.append(" ").append(yarnBlend.getBlendName());
        this.displayName = sb.toString().trim();
    }
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 4 — WAREHOUSE & STOCK
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/inventory/setup/Warehouse.java
package com.asg.spindleserp.inventory.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_warehouses",
    uniqueConstraints = @UniqueConstraint(name = "uk_wh_org_code", columnNames = {"organization_id","warehouse_code"}),
    indexes = {
        @Index(name = "idx_wh_org",  columnList = "organization_id"),
        @Index(name = "idx_wh_type", columnList = "item_type")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Warehouse extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id", nullable = false)
    private BusinessUnit businessUnit;

    @Column(name = "warehouse_code", nullable = false, length = 50) private String warehouseCode;
    @Column(name = "warehouse_name", nullable = false, length = 200) private String warehouseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private ItemType itemType = ItemType.GENERAL;

    @Column(columnDefinition = "TEXT") private String address;
    @Column(name = "manager_name",   length = 100) private String managerName;
    @Column(name = "contact_number", length = 20)  private String contactNumber;
    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/global/lot/GlobalInventoryLot.java
package com.asg.spindleserp.global.lot;

import com.asg.spindleserp.accounts.setup.Bank;
import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Certification;
import com.asg.spindleserp.inventory.setup.ColorGrade;
import com.asg.spindleserp.inventory.setup.InventoryLotStatus;
import com.asg.spindleserp.inventory.setup.ItemType;
import com.asg.spindleserp.security.locations.Country;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "global_inv_lots",
    uniqueConstraints = @UniqueConstraint(name = "uk_inv_lot_org_no", columnNames = {"organization_id","lot_number"}),
    indexes = {
        @Index(name = "idx_lot_item",     columnList = "item_id"),
        @Index(name = "idx_lot_status",   columnList = "status"),
        @Index(name = "idx_lot_expiry",   columnList = "expiry_date"),
        @Index(name = "idx_lot_received", columnList = "received_date")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GlobalInventoryLot extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(name = "lot_number", nullable = false, length = 100) private String lotNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private ItemType itemType = ItemType.GENERAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private SubAccount supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_of_origin_id")
    private Country countryOfOrigin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    // Dates
    @Column(name = "received_date")      private LocalDate receivedDate;
    @Column(name = "production_date")    private LocalDate productionDate;
    @Column(name = "expiry_date")        private LocalDate expiryDate;
    @Column(name = "manufacturing_date") private LocalDate manufacturingDate;

    // Classification
    @Enumerated(EnumType.STRING) @Column(length = 50) private Certification certification;
    @Enumerated(EnumType.STRING) @Column(name = "color_grade", length = 50) private ColorGrade colorGrade;

    // Fiber quality
    @Column(name = "avg_staple_length",  precision = 8, scale = 3) private BigDecimal avgStapleLength;
    @Column(name = "avg_micronaire",     precision = 8, scale = 3) private BigDecimal avgMicronaire;
    @Column(name = "avg_moisture",       precision = 8, scale = 3) private BigDecimal avgMoisture;
    @Column(name = "avg_trash_percent",  precision = 5, scale = 3) private BigDecimal avgTrashPercent;
    @Column(name = "avg_purity",         precision = 5, scale = 3) private BigDecimal avgPurity;
    @Column(precision = 8, scale = 3)                              private BigDecimal denier;

    // Location
    @Column(name = "warehouse_location", length = 100) private String warehouseLocation;
    @Column(name = "shelf_location",     length = 100) private String shelfLocation;
    @Column(name = "bin_location",       length = 100) private String binLocation;

    // Batch
    @Column(name = "batch_no",              length = 100) private String batchNo;
    @Column(name = "manufacturer_batch_no", length = 100) private String manufacturerBatchNo;
    @Column(precision = 8, scale = 3)                     private BigDecimal concentration;
    @Column(name = "chemical_grade",        length = 50)  private String chemicalGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InventoryLotStatus status = InventoryLotStatus.AVAILABLE;

    @Builder.Default @Column(nullable = false) private Boolean active = true;
    @Column(columnDefinition = "TEXT") private String remarks;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/global/documents/InventoryStockBalance.java
package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_inventory_stock_balances",
    uniqueConstraints = @UniqueConstraint(name = "uk_stock_balance",
        columnNames = {"item_id","warehouse_id","lot_id"}),
    indexes = {
        @Index(name = "idx_sb_item",      columnList = "item_id"),
        @Index(name = "idx_sb_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_sb_lot",       columnList = "lot_id"),
        @Index(name = "idx_sb_org",       columnList = "organization_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryStockBalance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private GlobalInventoryLot lot;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity        = BigDecimal.ZERO;
    @Column(name = "bale_quantity", precision = 12, scale = 3)            private BigDecimal baleQuantity;
    @Column                                                                private Integer    bags;
    @Column(name = "bag_quantity")                                         private Integer    bagQuantity;
    @Column(name = "cones_per_bag")                                        private Integer    conesPerBag;
    @Column(name = "cone_quantity")                                        private Integer    coneQuantity;
    @Column(name = "actual_weight", precision = 12, scale = 3)            private BigDecimal actualWeight;
    @Column(name = "net_weight",    precision = 12, scale = 3)            private BigDecimal netWeight;
    @Builder.Default @Column(name = "average_cost", nullable = false, precision = 18, scale = 4) private BigDecimal averageCost = BigDecimal.ZERO;
    @Builder.Default @Column(name = "stock_value",  nullable = false, precision = 18, scale = 2) private BigDecimal stockValue  = BigDecimal.ZERO;
    @Column(name = "last_transaction_time") private LocalDateTime lastTransactionTime;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 5 — FINANCE & ACCOUNTS
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/accounts/setup/Bank.java
package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stp_banks",
    uniqueConstraints = @UniqueConstraint(name = "uk_bank_org_code", columnNames = {"organization_id","bank_code"}),
    indexes = @Index(name = "idx_bank_swift", columnList = "swift_code"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bank extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "bank_code",       nullable = false, length = 20)  private String bankCode;
    @Column(name = "bank_name",       nullable = false, length = 200) private String bankName;
    @Column(name = "bank_name_local", length = 200)                   private String bankNameLocal;
    @Column(name = "short_name",      length = 50)                    private String shortName;
    @Column(name = "swift_code",      length = 11)                    private String swiftCode;
    @Column(name = "head_office_address", length = 500) private String headOfficeAddress;
    @Column(name = "head_office_country", length = 100) @Builder.Default private String headOfficeCountry = "Bangladesh";

    @Builder.Default @Column(name = "supports_lc",         nullable = false) private Boolean supportsLc         = false;
    @Builder.Default @Column(name = "supports_import_lc",  nullable = false) private Boolean supportsImportLc   = false;
    @Builder.Default @Column(name = "supports_export_lc",  nullable = false) private Boolean supportsExportLc   = false;
    @Builder.Default @Column(name = "supports_btb_lc",     nullable = false) private Boolean supportsBtbLc      = false;
    @Builder.Default @Column(name = "is_active",           nullable = false) private Boolean isActive           = true;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @OneToMany(mappedBy = "bank", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BankAccount> bankAccounts = new ArrayList<>();
}

// FILE: com/asg/spindleserp/accounts/setup/Account.java
package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_chart_of_accounts",
    uniqueConstraints = @UniqueConstraint(name = "uk_coa_org_code", columnNames = {"organization_id","account_code"}),
    indexes = {
        @Index(name = "idx_coa_org",    columnList = "organization_id"),
        @Index(name = "idx_coa_type",   columnList = "account_type"),
        @Index(name = "idx_coa_parent", columnList = "parent_account_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;

    @Column(name = "account_code", nullable = false, length = 50) private String accountCode;
    @Column(name = "account_name", nullable = false, length = 200) private String accountName;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;     // ASSET|LIABILITY|EQUITY|REVENUE|EXPENSE

    @Column(name = "account_nature", nullable = false, length = 10)
    private String accountNature;   // DEBIT|CREDIT

    @Builder.Default @Column(nullable = false) private Integer level = 1;
    @Builder.Default @Column(name = "opening_balance", nullable = false, precision = 18, scale = 2) private BigDecimal openingBalance = BigDecimal.ZERO;
    @Builder.Default @Column(name = "current_balance", nullable = false, precision = 18, scale = 2) private BigDecimal currentBalance = BigDecimal.ZERO;
    @Column(length = 3) @Builder.Default private String currency = "BDT";
    @Column(columnDefinition = "TEXT") private String description;

    @Builder.Default @Column(name = "is_active",           nullable = false) private Boolean isActive          = true;
    @Builder.Default @Column(name = "is_system",           nullable = false) private Boolean isSystem          = false;
    @Builder.Default @Column(name = "is_control_account",  nullable = false) private Boolean isControlAccount  = false;
    @Builder.Default @Column(name = "allow_manual_entry",  nullable = false) private Boolean allowManualEntry  = true;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/accounts/setup/SubAccount.java
package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * SubAccount — JOINED TABLE INHERITANCE base.
 * sub_account_type discriminates: BANK | CASH | CUSTOMER | SUPPLIER | GENERAL
 * Child tables: BankAccount, CashAccount, Customer (acc_customers), Supplier (acc_suppliers)
 */
@Entity
@Table(name = "acc_chart_of_accounts_sub",
    uniqueConstraints = @UniqueConstraint(name = "uk_sub_org_code", columnNames = {"organization_id","sub_account_code"}),
    indexes = {
        @Index(name = "idx_sub_org",  columnList = "organization_id"),
        @Index(name = "idx_sub_type", columnList = "sub_account_type")
    })
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "sub_account_type", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SubAccount extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_account_id", nullable = false)
    private Account mainAccount;

    @Column(name = "sub_account_code", nullable = false, length = 50) private String subAccountCode;
    @Column(name = "sub_account_name", nullable = false, length = 200) private String subAccountName;

    @Builder.Default
    @Column(name = "opening_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(length = 20) @Builder.Default private String currency = "BDT";
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "contact_person", length = 200) private String contactPerson;
    @Column(name = "contact_phone",  length = 20)  private String contactPhone;
    @Column(name = "contact_email",  length = 100) private String contactEmail;
    @Column(length = 500) private String address;
    @Column(length = 50)  private String city;
    @Column(length = 50)  private String country;
    @Column(name = "tax_id",                length = 50) private String taxId;
    @Column(name = "vat_registration_no",   length = 50) private String vatRegistrationNo;
    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/accounts/setup/BankAccount.java
package com.asg.spindleserp.accounts.setup;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "acc_bank_accounts")
@DiscriminatorValue("BANK")
@Getter @Setter @NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class BankAccount extends SubAccount {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    @Column(name = "bank_account_code", nullable = false, unique = true, length = 50) private String bankAccountCode;
    @Column(name = "branch_name",       nullable = false, length = 100)               private String branchName;
    @Column(name = "branch_code",       length = 10)                                  private String branchCode;
    @Column(name = "branch_address",    length = 200)                                 private String branchAddress;
    @Column(name = "account_number",    nullable = false, unique = true, length = 100) private String accountNumber;
    @Column(name = "account_title",     length = 200)                                  private String accountTitle;
    @Column(name = "bank_account_type", length = 30)                                   private String bankAccountType;
    // SAVINGS|CURRENT|FIXED_DEPOSIT|OVERDRAFT|LOAN|ESCROW|NOSTRO|VOSTRO|CREDIT_CARD

    @Column(name = "opening_date")    private LocalDate openingDate;
    @Column(name = "closing_date")    private LocalDate closingDate;
    @Column(name = "credit_limit",    precision = 18, scale = 2) private BigDecimal creditLimit;
    @Column(name = "overdraft_limit", precision = 18, scale = 2) private BigDecimal overdraftLimit;
    @Column(name = "interest_rate",   precision = 5,  scale = 2) private BigDecimal interestRate;

    @Builder.Default @Column(name = "supports_lc")        private Boolean supportsLc       = false;
    @Builder.Default @Column(name = "supports_import_lc") private Boolean supportsImportLc = false;
    @Builder.Default @Column(name = "supports_export_lc") private Boolean supportsExportLc = false;
    @Builder.Default @Column(name = "supports_btb_lc")    private Boolean supportsBtbLc    = false;
    @Builder.Default @Column(name = "requires_approval")  private Boolean requiresApproval = false;
    @Column(name = "approval_limit", precision = 18, scale = 2) private BigDecimal approvalLimit;
    @Builder.Default @Column(name = "is_default_payment_account") private Boolean isDefaultPaymentAccount = false;
    @Builder.Default @Column(name = "is_active") private Boolean isActive = true;
}

// FILE: com/asg/spindleserp/accounts/setup/CashAccount.java
package com.asg.spindleserp.accounts.setup;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_cash_accounts")
@DiscriminatorValue("CASH")
@Getter @Setter @NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class CashAccount extends SubAccount {

    @Column(name = "cash_account_code", nullable = false, unique = true, length = 50) private String cashAccountCode;
    @Column(name = "account_title",     length = 200) private String accountTitle;
    @Column(name = "cash_account_type", nullable = false, length = 20)
    @Builder.Default
    private String cashAccountType = "PETTY_CASH"; // MAIN_CASH|PETTY_CASH|CASH_IN_HAND|CASH_DRAWER|IMPREST

    @Column(length = 100) private String location;
    @Column(length = 100) private String custodian;
    @Column(name = "custodian_phone", length = 20)  private String custodianPhone;
    @Column(name = "custodian_email", length = 100) private String custodianEmail;
    @Builder.Default @Column(name = "maximum_limit", precision = 18, scale = 2) private BigDecimal maximumLimit = BigDecimal.ZERO;
    @Builder.Default @Column(name = "minimum_limit", precision = 18, scale = 2) private BigDecimal minimumLimit = BigDecimal.ZERO;
    @Builder.Default @Column(name = "requires_approval", nullable = false) private Boolean requiresApproval = false;
    @Builder.Default @Column(name = "approval_limit",    precision = 18, scale = 2) private BigDecimal approvalLimit = BigDecimal.ZERO;
    @Builder.Default @Column(name = "is_active") private Boolean isActive = true;
    @Column(columnDefinition = "TEXT") private String remarks;
}

// FILE: com/asg/spindleserp/accounts/setup/Customer.java
package com.asg.spindleserp.accounts.setup;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_customers",
    uniqueConstraints = @UniqueConstraint(name = "uk_customer_org_code", columnNames = {"organization_id","customer_code"}))
@DiscriminatorValue("CUSTOMER")
@Getter @Setter @NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Customer extends SubAccount {

    @Column(name = "customer_code", nullable = false, length = 50) private String customerCode;
    @Column(name = "credit_limit",  precision = 18, scale = 2)     private BigDecimal creditLimit;
    @Column(name = "payment_terms", length = 100)                  private String paymentTerms;
    @Column(name = "credit_days")                                  private Integer creditDays;
    @Column(name = "sales_representative", length = 100)           private String salesRepresentative;
    @Column(name = "preferred_contact",    length = 50)            private String preferredContact;
    @Column(name = "customer_group",       length = 50)            private String customerGroup;
    @Builder.Default @Column(name = "loyalty_points")             private Integer loyaltyPoints     = 0;
    @Builder.Default @Column(name = "is_export_customer")         private Boolean isExportCustomer  = false;
}

// FILE: com/asg/spindleserp/accounts/setup/Supplier.java
package com.asg.spindleserp.accounts.setup;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_suppliers",
    uniqueConstraints = @UniqueConstraint(name = "uk_supplier_org_code", columnNames = {"organization_id","supplier_code"}))
@DiscriminatorValue("SUPPLIER")
@Getter @Setter @NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class Supplier extends SubAccount {

    @Column(name = "supplier_code",    nullable = false, length = 50) private String supplierCode;
    @Column(name = "payment_terms",    length = 100)                  private String paymentTerms;
    @Column(name = "lead_time_days")                                  private Integer leadTimeDays;
    @Column(columnDefinition = "TEXT")                                private String certifications;
    @Builder.Default @Column(name = "is_import_supplier") private Boolean isImportSupplier = false;
    @Column(name = "preferred_currency", length = 3)                  private String preferredCurrency;
}

// FILE: com/asg/spindleserp/accounts/setup/CostCenter.java
package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "acc_cost_centers",
    uniqueConstraints = @UniqueConstraint(name = "uk_cc_org_code", columnNames = {"organization_id","code"}),
    indexes = {
        @Index(name = "idx_cc_org",    columnList = "organization_id"),
        @Index(name = "idx_cc_parent", columnList = "parent_cost_center_id"),
        @Index(name = "idx_cc_bu",     columnList = "business_unit_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CostCenter extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id")
    private BusinessUnit businessUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_cost_center_id")
    private CostCenter parent;

    @Column(nullable = false, length = 50)  private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(columnDefinition = "TEXT")      private String description;
    @Builder.Default @Column(nullable = false) private Boolean active = true;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/accounts/journal/JournalEntry.java
package com.asg.spindleserp.accounts.journal;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acc_journal_entries",
    uniqueConstraints = @UniqueConstraint(name = "uk_je_org_no", columnNames = {"organization_id","entry_no"}),
    indexes = {
        @Index(name = "idx_je_org",    columnList = "organization_id"),
        @Index(name = "idx_je_date",   columnList = "entry_date"),
        @Index(name = "idx_je_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntry extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(name = "entry_no", nullable = false, length = 50) private String entryNo;
    @Column(name = "entry_type", nullable = false, length = 30) private String entryType;
    // JOURNAL|PAYMENT|RECEIPT|CONTRA|OPENING
    @Column(name = "entry_date", nullable = false) private LocalDate entryDate;
    @Column(name = "fiscal_year")                  private Integer fiscalYear;
    @Column(name = "reference_no", length = 100)   private String referenceNo;
    @Column(columnDefinition = "TEXT")             private String narration;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT|POSTED|REVERSED

    @Builder.Default @Column(name = "is_auto_entry", nullable = false) private Boolean isAutoEntry = false;
    @Column(name = "source_module", length = 50) private String sourceModule;
    @Column(name = "source_doc_id")              private Long sourceDocId;

    @Builder.Default @Column(name = "total_debit",  nullable = false, precision = 18, scale = 2) private BigDecimal totalDebit  = BigDecimal.ZERO;
    @Builder.Default @Column(name = "total_credit", nullable = false, precision = 18, scale = 2) private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "posted_by",  length = 100) private String postedBy;
    @Column(name = "posted_at")                private LocalDateTime postedAt;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();
}

// FILE: com/asg/spindleserp/accounts/journal/JournalEntryLine.java
package com.asg.spindleserp.accounts.journal;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.accounts.setup.SubAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_journal_entry_lines",
    indexes = {
        @Index(name = "idx_jel_entry",   columnList = "journal_entry_id"),
        @Index(name = "idx_jel_account", columnList = "account_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntryLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_account_id")
    private SubAccount subAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "line_no", nullable = false) private Integer lineNo;
    @Column(length = 500) private String description;
    @Builder.Default @Column(name = "debit_amount",  nullable = false, precision = 18, scale = 2) private BigDecimal debitAmount  = BigDecimal.ZERO;
    @Builder.Default @Column(name = "credit_amount", nullable = false, precision = 18, scale = 2) private BigDecimal creditAmount = BigDecimal.ZERO;
    @Column(length = 3) @Builder.Default private String currency = "BDT";
    @Builder.Default @Column(name = "exchange_rate", precision = 18, scale = 4) private BigDecimal exchangeRate = BigDecimal.ONE;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 6 — APPROVAL ENGINE
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/approval/ApprovalConfig.java
package com.asg.spindleserp.approval;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "apr_configs",
    uniqueConstraints = @UniqueConstraint(name = "uk_apr_cfg", columnNames = {"organization_id","document_type","code"}),
    indexes = @Index(name = "idx_apr_cfg_dtype", columnList = "document_type"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalConfig extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false, length = 50)  private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(columnDefinition = "TEXT")      private String description;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(nullable = false, length = 50) private String module;
    @Column(name = "flow_type", nullable = false, length = 20)
    @Builder.Default
    private String flowType = "SEQUENTIAL"; // SEQUENTIAL|PARALLEL

    @Column(name = "min_amount", precision = 18, scale = 2) private BigDecimal minAmount;
    @Column(name = "max_amount", precision = 18, scale = 2) private BigDecimal maxAmount;
    @Builder.Default @Column(name = "use_reporting_hierarchy", nullable = false) private Boolean useReportingHierarchy = false;
    @Column(name = "auto_escalation_hours")  private Integer autoEscalationHours;
    @Builder.Default @Column(name = "enable_reminders", nullable = false) private Boolean enableReminders = true;
    @Builder.Default @Column(name = "reminder_interval_hours") private Integer reminderIntervalHours = 24;
    @Builder.Default @Column(nullable = false) private Integer priority = 100;
    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @OneToMany(mappedBy = "approvalConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("levelNumber ASC")
    @Builder.Default
    private List<ApprovalLevel> approvalLevels = new ArrayList<>();

    public int getTotalLevels() { return approvalLevels.size(); }
}

// FILE: com/asg/spindleserp/approval/ApprovalLevel.java
package com.asg.spindleserp.approval;

import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_levels",
    indexes = {
        @Index(name = "idx_apr_lvl_cfg",  columnList = "approval_config_id"),
        @Index(name = "idx_apr_lvl_user", columnList = "approver_user_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalLevel implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_config_id", nullable = false)
    private ApprovalConfig approvalConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id")
    private User approverUser;

    @Column(name = "level_number", nullable = false) private Integer levelNumber;
    @Column(name = "level_name",   nullable = false, length = 100) private String levelName;
    @Column(length = 500) private String description;
    @Column(name = "approver_description", length = 200) private String approverDescription;

    @Builder.Default @Column(name = "can_delegate",             nullable = false) private Boolean canDelegate           = true;
    @Builder.Default @Column(name = "can_hold",                 nullable = false) private Boolean canHold               = false;
    @Builder.Default @Column(name = "can_forward",              nullable = false) private Boolean canForward             = false;
    @Builder.Default @Column(name = "can_approve_with_changes", nullable = false) private Boolean canApproveWithChanges  = false;
    @Builder.Default @Column(name = "is_active",                nullable = false) private Boolean isActive               = true;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// FILE: com/asg/spindleserp/approval/ApprovalRequest.java
package com.asg.spindleserp.approval;

import com.asg.spindleserp.security.Organization;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "apr_requests",
    uniqueConstraints = @UniqueConstraint(name = "uk_ar_doc", columnNames = {"document_type","reference_id"}),
    indexes = {
        @Index(name = "idx_apr_req_org",       columnList = "organization_id"),
        @Index(name = "idx_apr_req_dtype",     columnList = "document_type,reference_id"),
        @Index(name = "idx_apr_req_status",    columnList = "status"),
        @Index(name = "idx_apr_req_user",      columnList = "current_approver_user_id"),
        @Index(name = "idx_apr_req_role",      columnList = "current_approver_role"),
        @Index(name = "idx_apr_req_requester", columnList = "requester_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalRequest implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_config_id")
    private ApprovalConfig approvalConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approval_level_id")
    private ApprovalLevel currentApprovalLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver_user_id")
    private User currentApproverUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // Polymorphic document reference
    @Column(name = "document_type", nullable = false, length = 50) private String documentType;
    @Column(name = "reference_id",  nullable = false)              private Long   referenceId;
    @Column(name = "reference_number", nullable = false, length = 100) private String referenceNumber;
    @Column(name = "document_date")                                private LocalDate documentDate;
    @Column(name = "document_amount", precision = 18, scale = 2)  private BigDecimal documentAmount;
    @Column(name = "document_summary", length = 500)              private String documentSummary;

    @Builder.Default @Column(name = "total_levels",        nullable = false) private Integer totalLevels       = 1;
    @Builder.Default @Column(name = "current_level_number", nullable = false) private Integer currentLevelNumber = 1;
    @Column(name = "current_approver_role", length = 80)  private String currentApproverRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    @Column(name = "requester_name", length = 200) private String requesterName;
    @Builder.Default @Column(name = "is_urgent", nullable = false) private Boolean isUrgent = false;
    @Column(name = "due_date")       private LocalDate   dueDate;
    @Column(name = "completed_at")   private LocalDateTime completedAt;
    @Column(name = "completed_by", length = 100) private String completedBy;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("actionAt ASC")
    @Builder.Default
    private List<ApprovalHistory> histories = new ArrayList<>();

    public boolean isPending()   { return status == ApprovalStatus.SUBMITTED || status == ApprovalStatus.IN_APPROVAL; }
    public boolean isLastLevel() { return currentLevelNumber != null && totalLevels != null && currentLevelNumber >= totalLevels; }
    public boolean isCompleted() { return status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED || status == ApprovalStatus.CANCELLED; }
}

// FILE: com/asg/spindleserp/approval/ApprovalHistory.java
package com.asg.spindleserp.approval;

import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_histories",
    indexes = {
        @Index(name = "idx_apr_hist_req",   columnList = "approval_request_id"),
        @Index(name = "idx_apr_hist_actor", columnList = "actor_user_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalHistory implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_level_id")
    private ApprovalLevel approvalLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_from_user_id")
    private User delegatedFromUser;

    @Column(name = "level_number", nullable = false) private Integer levelNumber;
    @Column(name = "level_name",   nullable = false, length = 100) private String levelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalAction action;
    // SUBMIT|APPROVE|REJECT|RETURN|RECALL|HOLD|FORWARD|DELEGATE|ESCALATE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalStatus status;

    @Column(name = "actor_name",        nullable = false, length = 150) private String actorName;
    @Column(name = "actor_designation", length = 100)                  private String actorDesignation;
    @Column(name = "actor_department",  length = 150)                  private String actorDepartment;
    @Column(columnDefinition = "TEXT") private String comments;
    @Column(name = "rejection_reason", columnDefinition = "TEXT") private String rejectionReason;
    @Column(name = "return_reason",    columnDefinition = "TEXT") private String returnReason;
    @Column(name = "response_minutes") private Long responseMinutes;

    @CreationTimestamp
    @Column(name = "action_at", nullable = false, updatable = false)
    private LocalDateTime actionAt;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 7 — GLOBAL BUSINESS DOCUMENTS (MDST)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/global/documents/BusinessDocument.java
package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.approval.ApprovalRequest;
import com.asg.spindleserp.approval.DocumentType;
import com.asg.spindleserp.commercial.CommercialLc;
import com.asg.spindleserp.common.BaseAuditEntity;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.inventory.setup.ItemType;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_business_documents",
    uniqueConstraints = @UniqueConstraint(name = "uk_gbd_no", columnNames = {"organization_id","document_no"}),
    indexes = {
        @Index(name = "idx_gbd_org",        columnList = "organization_id"),
        @Index(name = "idx_gbd_type",       columnList = "document_type"),
        @Index(name = "idx_gbd_status",     columnList = "status"),
        @Index(name = "idx_gbd_party",      columnList = "party_id"),
        @Index(name = "idx_gbd_parent",     columnList = "parent_document_id"),
        @Index(name = "idx_gbd_warehouse",  columnList = "warehouse_id"),
        @Index(name = "idx_gbd_date",       columnList = "document_date"),
        @Index(name = "idx_gbd_type_status",columnList = "document_type,status"),
        @Index(name = "idx_gbd_org_type",   columnList = "organization_id,document_type")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessDocument extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identity ─────────────────────────────────────────────────────────────
    @Column(name = "document_no",        nullable = false, unique = true, length = 100) private String documentNo;
    @Column(name = "document_no_manual", length = 100) private String documentNoManual;
    @Column(name = "reference_no",       length = 100) private String referenceNo;
    @Column(name = "reference_doc_id")                 private Long   referenceDocId;
    @Column(name = "document_date",      nullable = false)            private LocalDate documentDate;
    @Column(name = "validity_date")                                   private LocalDate validityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private ItemType itemType = ItemType.GENERAL;

    // ── Status ───────────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BusinessDocumentStatus status = BusinessDocumentStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String priority = "NORMAL"; // NORMAL|URGENT

    // ── Relationships ─────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private SubAccount party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_document_id")
    private BusinessDocument parentDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lc_id")
    private CommercialLc lc;

    // ── Currency ─────────────────────────────────────────────────────────────
    @Column(length = 3) @Builder.Default private String currency = "BDT";
    @Column(name = "exchange_rate", precision = 18, scale = 4) @Builder.Default private BigDecimal exchangeRate = BigDecimal.ONE;

    // ── Dates ────────────────────────────────────────────────────────────────
    @Column(name = "required_date") private LocalDate requiredDate;
    @Column(name = "delivery_date") private LocalDate deliveryDate;

    // ── Amounts ──────────────────────────────────────────────────────────────
    @Builder.Default @Column(name = "subtotal_amount", nullable = false, precision = 18, scale = 2) private BigDecimal subtotalAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount  = BigDecimal.ZERO;
    @Builder.Default @Column(name = "tax_amount",      nullable = false, precision = 18, scale = 2) private BigDecimal taxAmount       = BigDecimal.ZERO;
    @Builder.Default @Column(name = "shipping_amount", nullable = false, precision = 18, scale = 2) private BigDecimal shippingAmount  = BigDecimal.ZERO;
    @Builder.Default @Column(name = "total_amount",    nullable = false, precision = 18, scale = 2) private BigDecimal totalAmount     = BigDecimal.ZERO;
    @Builder.Default @Column(name = "paid_amount",     nullable = false, precision = 18, scale = 2) private BigDecimal paidAmount      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "due_amount",      nullable = false, precision = 18, scale = 2) private BigDecimal dueAmount       = BigDecimal.ZERO;

    // ── Logistics ─────────────────────────────────────────────────────────────
    @Column(name = "delivery_address",  length = 500) private String deliveryAddress;
    @Column(name = "contact_person",    length = 100) private String contactPerson;
    @Column(name = "contact_number",    length = 20)  private String contactNumber;
    @Column(name = "vehicle_number",    length = 100) private String vehicleNumber;
    @Column(name = "driver_name",       length = 100) private String driverName;
    @Column(name = "challan_no",        length = 100) private String challanNo;

    // ── Export / Import ───────────────────────────────────────────────────────
    @Column(name = "export_lc_number",   length = 100) private String exportLcNumber;
    @Column(name = "bl_number",          length = 100) private String blNumber;
    @Column(name = "vessel_name",        length = 100) private String vesselName;
    @Column(name = "container_number",   length = 100) private String containerNumber;
    @Column(length = 50)                               private String incoterms;
    @Column(name = "port_of_loading",    length = 100) private String portOfLoading;
    @Column(name = "port_of_discharge",  length = 100) private String portOfDischarge;

    // ── Text & Flags ─────────────────────────────────────────────────────────
    @Column(name = "terms_and_conditions", columnDefinition = "TEXT") private String termsAndConditions;
    @Column(columnDefinition = "TEXT")                                private String remarks;
    @Builder.Default @Column(name = "stock_posted", nullable = false) private Boolean stockPosted = false;
    @Builder.Default @Column(name = "is_deleted",   nullable = false) private Boolean isDeleted   = false;

    // ── Lines ─────────────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<BusinessDocumentLine> lines = new ArrayList<>();

    public void calculateTotals() {
        this.subtotalAmount = lines.stream()
            .map(l -> l.getLineAmount() != null ? l.getLineAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = subtotalAmount
            .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
            .add(taxAmount      != null ? taxAmount      : BigDecimal.ZERO)
            .add(shippingAmount != null ? shippingAmount : BigDecimal.ZERO);
        this.dueAmount = totalAmount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }
}

// FILE: com/asg/spindleserp/global/documents/BusinessDocumentLine.java
package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.common.BaseAuditEntity;
import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_business_document_lines",
    indexes = {
        @Index(name = "idx_gdl_doc",    columnList = "document_id"),
        @Index(name = "idx_gdl_item",   columnList = "item_id"),
        @Index(name = "idx_gdl_lot",    columnList = "inventory_lot_id"),
        @Index(name = "idx_gdl_source", columnList = "source_line_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessDocumentLine extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private BusinessDocument document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private GlobalInventoryLot inventoryLot;

    /**
     * Source line from upstream document.
     * MUST use EntityManager.getReference() — never new BDLine() with only id set.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_line_id")
    private BusinessDocumentLine sourceLine;

    @Column(name = "line_number",  nullable = false)  private Integer lineNumber;
    @Column(name = "item_code",    length = 100)       private String  itemCode;
    @Column(name = "item_name",    length = 500)       private String  itemName;
    @Column(length = 100)                              private String  sku;
    @Column(length = 1000)                             private String  description;
    @Column(name = "unit_code",    length = 20)        private String  unitCode;

    // Quantities
    @Builder.Default @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity           = BigDecimal.ZERO;
    @Column(name = "delivered_quantity", precision = 18, scale = 3) @Builder.Default private BigDecimal deliveredQuantity = BigDecimal.ZERO;
    @Column(name = "received_quantity",  precision = 18, scale = 3) @Builder.Default private BigDecimal receivedQuantity  = BigDecimal.ZERO;
    @Column(name = "accepted_quantity",  precision = 18, scale = 3) @Builder.Default private BigDecimal acceptedQuantity  = BigDecimal.ZERO;
    @Column(name = "rejected_quantity",  precision = 18, scale = 3) @Builder.Default private BigDecimal rejectedQuantity  = BigDecimal.ZERO;
    @Column(name = "bale_quantity",      precision = 12, scale = 2) private BigDecimal baleQuantity;
    @Column(precision = 10, scale = 0)                              private BigDecimal bags;

    // Pricing
    @Column(name = "unit_price",      precision = 18, scale = 4) private BigDecimal unitPrice;
    @Builder.Default @Column(name = "discount_amount", precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "tax_amount",      precision = 18, scale = 2) private BigDecimal taxAmount      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "line_amount",     precision = 18, scale = 2) private BigDecimal lineAmount     = BigDecimal.ZERO;

    // Lot / quality
    @Column(name = "batch_number",  length = 100) private String batchNumber;
    @Column(name = "quality_status",length = 30)  private String qualityStatus; // PENDING|PASSED|FAILED|QUARANTINE
    @Column(name = "quality_remarks", columnDefinition = "TEXT") private String qualityRemarks;
    @Column(name = "expected_delivery_date") private LocalDate expectedDeliveryDate;

    @Column(columnDefinition = "TEXT") private String remarks; // JSON-packed for PRQ/PMI

    @OneToMany(mappedBy = "documentLine", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BusinessDocumentLineLot> lots = new ArrayList<>();

    public void calculateAmount() {
        if (unitPrice != null && quantity != null) {
            BigDecimal gross = unitPrice.multiply(quantity);
            BigDecimal disc  = discountAmount != null ? discountAmount : BigDecimal.ZERO;
            BigDecimal tax   = taxAmount      != null ? taxAmount      : BigDecimal.ZERO;
            this.lineAmount  = gross.subtract(disc).add(tax);
        }
    }
}

// FILE: com/asg/spindleserp/global/documents/BusinessDocumentLineLot.java
package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_business_document_line_lots",
    indexes = {
        @Index(name = "idx_gdll_line", columnList = "document_line_id"),
        @Index(name = "idx_gdll_lot",  columnList = "lot_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessDocumentLineLot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_line_id", nullable = false)
    private BusinessDocumentLine documentLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private GlobalInventoryLot lot;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity     = BigDecimal.ZERO;
    @Column(name = "bale_quantity",  precision = 12, scale = 3)           private BigDecimal baleQuantity;
    @Column                                                               private Integer    bags;
    @Column(name = "bag_capacity",   length = 20)
    @Builder.Default private String bagCapacity = "CUSTOM"; // CONE_25|CONE_50|CUSTOM
    @Column(name = "bag_weight",     precision = 12, scale = 3)  private BigDecimal bagWeight;
    @Column(name = "bag_quantity")                               private Integer    bagQuantity;
    @Column(name = "cones_per_bag")                              private Integer    conesPerBag;
    @Column(name = "cone_quantity")                              private Integer    coneQuantity;
    @Column(name = "actual_weight",  precision = 12, scale = 3)  private BigDecimal actualWeight;
    @Column(name = "net_weight",     precision = 12, scale = 3)  private BigDecimal netWeight;
    @Column(name = "unit_cost",      precision = 18, scale = 4)  private BigDecimal unitCost;
    @Column(name = "total_cost",     precision = 18, scale = 2)  private BigDecimal totalCost;
    @Column(name = "quality_remarks", columnDefinition = "TEXT") private String qualityRemarks;
    @Column(columnDefinition = "TEXT")                           private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 8 — COMMERCIAL (LC)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/commercial/CommercialLc.java
package com.asg.spindleserp.commercial;

import com.asg.spindleserp.accounts.setup.BankAccount;
import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.accounts.setup.Bank;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cmr_letters_of_credit",
    uniqueConstraints = @UniqueConstraint(name = "uk_lc_org_no", columnNames = {"organization_id","lc_number"}),
    indexes = {
        @Index(name = "idx_lc_org",    columnList = "organization_id"),
        @Index(name = "idx_lc_type",   columnList = "lc_type"),
        @Index(name = "idx_lc_expiry", columnList = "expiry_date"),
        @Index(name = "idx_lc_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommercialLc extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuing_bank_id")
    private Bank issuingBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advising_bank_id")
    private Bank advisingBank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private SubAccount beneficiary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuing_bank_account_id")
    private BankAccount issuingBankAccount;

    @Column(name = "lc_number", nullable = false, length = 100) private String lcNumber;
    @Column(name = "lc_type",   nullable = false, length = 30)
    private String lcType; // EXPORT|IMPORT|BACK_TO_BACK|INLAND

    @Column(name = "lc_amount", precision = 18, scale = 2) private BigDecimal lcAmount;
    @Column(length = 3) private String currency;
    @Column(name = "expiry_date")          private LocalDate expiryDate;
    @Column(name = "last_shipment_date")   private LocalDate lastShipmentDate;
    @Column(name = "presentation_period")  private Integer   presentationPeriod;

    @Column(name = "partial_shipment", length = 20) @Builder.Default private String partialShipment = "NOT_ALLOWED";
    @Column(name = "transhipment",     length = 20) @Builder.Default private String transhipment    = "NOT_ALLOWED";
    @Column(name = "port_of_loading",  length = 100)                 private String portOfLoading;
    @Column(name = "port_of_discharge",length = 100)                 private String portOfDischarge;
    @Column(length = 50)                                             private String incoterms;

    @Column(name = "tolerance_plus",  precision = 4, scale = 2) private BigDecimal tolerancePlus;
    @Column(name = "tolerance_minus", precision = 4, scale = 2) private BigDecimal toleranceMinus;
    @Column(name = "description_of_goods", columnDefinition = "TEXT") private String descriptionOfGoods;
    @Column(name = "special_conditions",   columnDefinition = "TEXT") private String specialConditions;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT|ACTIVE|AMENDED|UTILIZED|EXPIRED|CANCELLED

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 9 — HRM
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/hrm/setup/Department.java  (already full in project knowledge)
// FILE: com/asg/spindleserp/hrm/setup/Designation.java (already full in project knowledge)
// FILE: com/asg/spindleserp/hrm/pims/Employee.java     (already full in project knowledge — shown below condensed with new fields)

// FILE: com/asg/spindleserp/hrm/attendance/Attendance.java
package com.asg.spindleserp.hrm.attendance;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "hrm_attendances",
    uniqueConstraints = @UniqueConstraint(name = "uk_att_emp_date", columnNames = {"employee_id","att_date"}),
    indexes = {
        @Index(name = "idx_att_emp_date", columnList = "employee_id,att_date"),
        @Index(name = "idx_att_org_date", columnList = "organization_id,att_date")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "att_date", nullable = false) private LocalDate attDate;
    @Column(name = "check_in")                   private LocalTime checkIn;
    @Column(name = "check_out")                  private LocalTime checkOut;
    @Column(name = "working_hours", precision = 5, scale = 2) private BigDecimal workingHours;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ABSENT"; // PRESENT|ABSENT|LATE|HALF_DAY|HOLIDAY|LEAVE
    @Column(length = 500) private String remarks;
    @Column(name = "created_by", length = 100) private String createdBy;
}

// FILE: com/asg/spindleserp/hrm/attendance/EmployeeLeave.java
package com.asg.spindleserp.hrm.attendance;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_leaves",
    indexes = {
        @Index(name = "idx_leave_emp",    columnList = "employee_id"),
        @Index(name = "idx_leave_status", columnList = "status"),
        @Index(name = "idx_leave_dates",  columnList = "start_date,end_date")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeLeave extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "leave_type", nullable = false, length = 30)
    private String leaveType; // ANNUAL|SICK|CASUAL|MATERNITY|PATERNITY|UNPAID|COMPENSATORY

    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date",   nullable = false) private LocalDate endDate;
    @Column(name = "total_days", nullable = false, precision = 5, scale = 1) private BigDecimal totalDays;
    @Column(columnDefinition = "TEXT") private String reason;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "PENDING";
    // PENDING|APPROVED|REJECTED|CANCELLED
    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "approved_at")               private LocalDateTime approvedAt;
    @Column(columnDefinition = "TEXT")          private String remarks;
    @Column(name = "created_by", length = 100) private String createdBy;
}

// FILE: com/asg/spindleserp/hrm/payroll/PayrollRun.java
package com.asg.spindleserp.hrm.payroll;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hrm_payroll_runs",
    uniqueConstraints = @UniqueConstraint(name = "uk_payroll_org_month", columnNames = {"organization_id","payroll_month"}),
    indexes = @Index(name = "idx_payroll_month", columnList = "organization_id,payroll_month"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRun extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(name = "payroll_month", nullable = false, length = 7) private String payrollMonth; // YYYY-MM
    @Column(name = "run_date", nullable = false) private LocalDate runDate;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "DRAFT";

    @Builder.Default @Column(name = "total_gross",      precision = 18, scale = 2) private BigDecimal totalGross      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "total_deductions", precision = 18, scale = 2) private BigDecimal totalDeductions = BigDecimal.ZERO;
    @Builder.Default @Column(name = "total_net",        precision = 18, scale = 2) private BigDecimal totalNet        = BigDecimal.ZERO;
    @Builder.Default @Column(name = "employee_count")                              private Integer    employeeCount    = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "approved_at")               private LocalDateTime approvedAt;
    @Column(name = "created_by",  length = 100) private String createdBy;
    @Column(name = "updated_by",  length = 100) private String updatedBy;

    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PayrollRunLine> lines = new ArrayList<>();
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 10 — PRODUCTION
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/production/order/ProductionOrder.java
package com.asg.spindleserp.production.order;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.approval.ApprovalRequest;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "prd_orders",
    uniqueConstraints = @UniqueConstraint(name = "uk_prd_org_no", columnNames = {"organization_id","order_no"}),
    indexes = {
        @Index(name = "idx_prd_org",    columnList = "organization_id"),
        @Index(name = "idx_prd_status", columnList = "status"),
        @Index(name = "idx_prd_yarn",   columnList = "yarn_product_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductionOrder extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_management_id", nullable = false)
    private BusinessDocument orderManagement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_product_id", nullable = false)
    private InventoryItem yarnProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "order_no",           nullable = false, length = 50) private String     orderNo;
    @Column(name = "planned_quantity",   nullable = false, precision = 14, scale = 3) private BigDecimal plannedQuantity;
    @Builder.Default @Column(name = "produced_quantity", nullable = false, precision = 14, scale = 3) private BigDecimal producedQuantity = BigDecimal.ZERO;
    @Builder.Default @Column(name = "waste_quantity",    nullable = false, precision = 14, scale = 3) private BigDecimal wasteQuantity    = BigDecimal.ZERO;
    @Builder.Default @Column(name = "total_bags_packed", nullable = false)                            private Long       totalBagsPacked   = 0L;

    @Column(name = "planned_start_date") private LocalDate plannedStartDate;
    @Column(name = "planned_end_date")   private LocalDate plannedEndDate;
    @Column(name = "actual_start_date")  private LocalDate actualStartDate;
    @Column(name = "actual_end_date")    private LocalDate actualEndDate;

    @Column(name = "planned_cost_per_kg", precision = 14, scale = 4) private BigDecimal plannedCostPerKg;
    @Column(name = "actual_cost_per_kg",  precision = 14, scale = 4) private BigDecimal actualCostPerKg;
    @Column(name = "total_planned_cost",  precision = 18, scale = 2) private BigDecimal totalPlannedCost;
    @Column(name = "total_actual_cost",   precision = 18, scale = 2) private BigDecimal totalActualCost;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";
    // DRAFT|SUBMITTED|APPROVED|IN_PROGRESS|COMPLETED|CANCELLED|REJECTED

    @Column(name = "approval_status", length = 30)
    @Builder.Default
    private String approvalStatus = "DRAFT";

    @Column(columnDefinition = "TEXT") private String remarks;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/production/order/ProductionRecipe.java
package com.asg.spindleserp.production.order;

import com.asg.spindleserp.approval.ApprovalRequest;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prd_recipes",
    uniqueConstraints = @UniqueConstraint(name = "uk_recipe_org_code", columnNames = {"organization_id","recipe_code"}),
    indexes = @Index(name = "idx_recipe_prd", columnList = "production_order_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductionRecipe extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(name = "recipe_code", nullable = false, length = 50)  private String recipeCode;
    @Column(name = "recipe_name", nullable = false, length = 200) private String recipeName;
    @Builder.Default @Column(nullable = false) private Boolean active  = true;
    @Builder.Default @Column(nullable = false) private Boolean deleted = false;
    @Column(name = "approval_status", length = 30) @Builder.Default private String approvalStatus = "DRAFT";
    @Column(columnDefinition = "TEXT") private String remarks;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<ProductionRecipeItem> items = new ArrayList<>();

    /** Sum of all item percentages must equal 100 */
    public void validateBlend() {
        double total = items.stream()
            .mapToDouble(i -> i.getPercentage() != null ? i.getPercentage().doubleValue() : 0)
            .sum();
        if (Math.abs(total - 100.0) > 0.001) {
            throw new RuntimeException("Recipe blend percentages must sum to 100%");
        }
    }
}

// FILE: com/asg/spindleserp/production/order/ProductionRecipeItem.java
package com.asg.spindleserp.production.order;

import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prd_recipe_items",
    indexes = {
        @Index(name = "idx_ritem_recipe", columnList = "yarn_recipe_id"),
        @Index(name = "idx_ritem_raw",    columnList = "raw_material_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductionRecipeItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yarn_recipe_id", nullable = false)
    private ProductionRecipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_material_id", nullable = false)
    private InventoryItem rawMaterial;

    @Column(name = "line_number",  nullable = false)               private Integer    lineNumber;
    @Column(nullable = false, precision = 5, scale = 2)             private BigDecimal percentage;
    @Column(columnDefinition = "TEXT")                              private String     remarks;

    @OneToMany(mappedBy = "recipeItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductionRecipeItemLot> lots = new ArrayList<>();
}

// FILE: com/asg/spindleserp/production/order/ProductionRecipeItemLot.java
package com.asg.spindleserp.production.order;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "prd_recipe_item_lots",
    indexes = @Index(name = "idx_rlot_item", columnList = "production_recipe_item_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductionRecipeItemLot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_recipe_item_id", nullable = false)
    private ProductionRecipeItem recipeItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_lot_id", nullable = false)
    private GlobalInventoryLot inventoryLot;

    @Column(name = "line_number",           nullable = false)               private Integer    lineNumber;
    @Column(nullable = false, precision = 5, scale = 2)                     private BigDecimal percentage;
    @Builder.Default @Column(name = "buffer_percent",       precision = 5, scale = 2) private BigDecimal bufferPercent      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "waste_factor_percent", precision = 5, scale = 2) private BigDecimal wasteFactorPercent = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT") private String remarks;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 11 — FIXED ASSETS
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/fixedassets/AssetCategory.java
package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fa_asset_categories",
    uniqueConstraints = @UniqueConstraint(name = "uk_fa_cat_org_code", columnNames = {"organization_id","code"}),
    indexes = @Index(name = "idx_facat_parent", columnList = "parent_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetCategory extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AssetCategory parent;

    // GL account links
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "gl_asset_account_id")       private Account glAssetAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "gl_dep_exp_account_id")     private Account glDepExpAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "gl_accum_dep_account_id")   private Account glAccumDepAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "gl_disposal_account_id")    private Account glDisposalAccount;

    @Column(nullable = false, length = 50)  private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "default_dep_method", length = 30) @Builder.Default private String defaultDepMethod   = "STRAIGHT_LINE";
    @Column(name = "default_useful_life")                               private Integer defaultUsefulLife;
    @Column(name = "default_dep_rate", precision = 5, scale = 2)        private BigDecimal defaultDepRate;
    @Builder.Default @Column(name = "is_active", nullable = false)      private Boolean isActive           = true;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/fixedassets/FixedAsset.java
package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fa_assets",
    uniqueConstraints = @UniqueConstraint(name = "uk_fa_org_code", columnNames = {"organization_id","asset_code"}),
    indexes = {
        @Index(name = "idx_fa_org",      columnList = "organization_id"),
        @Index(name = "idx_fa_category", columnList = "asset_category_id"),
        @Index(name = "idx_fa_status",   columnList = "status"),
        @Index(name = "idx_fa_dept",     columnList = "department_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FixedAsset extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_category_id", nullable = false)
    private AssetCategory assetCategory;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id")            private Department  department;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cost_center_id")           private CostCenter  costCenter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "warehouse_id")             private Warehouse   warehouse;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "linked_item_id")           private InventoryItem linkedItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "linked_grn_id")            private BusinessDocument linkedGrn;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "linked_po_id")             private BusinessDocument linkedPo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "responsible_employee_id")  private Employee responsibleEmployee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id")              private SubAccount supplier;

    @Column(name = "asset_code", nullable = false, length = 50)  private String assetCode;
    @Column(name = "asset_name", nullable = false, length = 200) private String assetName;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "serial_number", length = 100) private String serialNumber;
    @Column(length = 100) private String model;
    @Column(length = 100) private String manufacturer;

    @Column(name = "acquisition_date",    nullable = false) private LocalDate acquisitionDate;
    @Column(name = "capitalisation_date")                  private LocalDate capitalisationDate;
    @Column(name = "purchase_cost", nullable = false, precision = 18, scale = 2)  private BigDecimal purchaseCost;
    @Builder.Default @Column(name = "installation_cost", precision = 18, scale = 2) private BigDecimal installationCost = BigDecimal.ZERO;
    @Column(length = 3) @Builder.Default private String currency = "BDT";

    @Column(name = "depreciation_method", nullable = false, length = 30)
    @Builder.Default
    private String depreciationMethod = "STRAIGHT_LINE"; // STRAIGHT_LINE|DECLINING_BALANCE|UNITS_OF_PRODUCTION

    @Column(name = "useful_life_years")                                              private Integer    usefulLifeYears;
    @Builder.Default @Column(name = "residual_value", precision = 18, scale = 2)    private BigDecimal residualValue          = BigDecimal.ZERO;
    @Column(name = "depreciation_rate",        precision = 5, scale = 2)            private BigDecimal depreciationRate;
    @Column(name = "depreciation_start_date")                                        private LocalDate  depreciationStartDate;
    @Builder.Default @Column(name = "accumulated_depreciation", nullable = false, precision = 18, scale = 2) private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;
    @Column(name = "current_book_value",       precision = 18, scale = 2)            private BigDecimal currentBookValue;
    @Column(name = "last_dep_run_date")                                              private LocalDate  lastDepRunDate;

    @Column(length = 200) private String location;
    @Column(nullable = false, length = 30) @Builder.Default
    private String status = "ACTIVE"; // ACTIVE|DISPOSED|TRANSFERRED|SOLD|WRITTEN_OFF|UNDER_MAINTENANCE

    @Column(length = 20) @Builder.Default private String condition = "GOOD";
    @Column(name = "warranty_expiry_date")   private LocalDate warrantyExpiryDate;
    @Column(name = "insurance_policy_no",  length = 100) private String insurancePolicyNo;
    @Column(name = "insurance_expiry_date")  private LocalDate insuranceExpiryDate;
    @Column(length = 100) private String barcode;
    @Column(name = "qr_code", length = 100) private String qrCode;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    /** Computed book value = total cost − accumulated depreciation */
    @PostLoad
    public void computeBookValue() {
        BigDecimal total = (purchaseCost != null ? purchaseCost : BigDecimal.ZERO)
            .add(installationCost != null ? installationCost : BigDecimal.ZERO);
        this.currentBookValue = total.subtract(
            accumulatedDepreciation != null ? accumulatedDepreciation : BigDecimal.ZERO);
    }
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 12 — CRM
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/crm/Lead.java
package com.asg.spindleserp.crm;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.User;
import com.asg.spindleserp.security.locations.Country;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_leads",
    indexes = {
        @Index(name = "idx_lead_org",      columnList = "organization_id"),
        @Index(name = "idx_lead_status",   columnList = "status"),
        @Index(name = "idx_lead_assigned", columnList = "assigned_to_user_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lead extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @Column(name = "full_name", nullable = false, length = 200) private String fullName;
    @Column(length = 200) private String company;
    @Column(name = "job_title", length = 100) private String jobTitle;
    @Column(length = 100) private String email;
    @Column(length = 20)  private String phone;
    @Column(length = 20)  private String mobile;
    @Column(length = 200) private String website;
    @Column(columnDefinition = "TEXT") private String address;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String source = "UNKNOWN";
    // WEBSITE|REFERRAL|TRADE_SHOW|COLD_CALL|SOCIAL_MEDIA|EMAIL_CAMPAIGN|WALK_IN|OTHER

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "NEW";
    // NEW|CONTACTED|QUALIFIED|DISQUALIFIED|CONVERTED

    @Builder.Default @Column(name = "lead_score")                                  private Integer    leadScore      = 0;
    @Column(name = "estimated_value", precision = 18, scale = 2)                   private BigDecimal estimatedValue;
    @Column(length = 100)                                                           private String     industry;
    @Column(name = "product_interest", columnDefinition = "TEXT")                  private String     productInterest;
    @Column(columnDefinition = "TEXT")                                              private String     description;
    @Column(name = "disqualify_reason", columnDefinition = "TEXT")                 private String     disqualifyReason;
    @Column(name = "converted_at")                                                 private LocalDateTime convertedAt;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/crm/Opportunity.java
package com.asg.spindleserp.crm;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "crm_opportunities",
    indexes = {
        @Index(name = "idx_opp_org",      columnList = "organization_id"),
        @Index(name = "idx_opp_stage",    columnList = "stage"),
        @Index(name = "idx_opp_customer", columnList = "customer_id"),
        @Index(name = "idx_opp_close",    columnList = "expected_close_date")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Opportunity extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private SubAccount customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_sq_id")
    private BusinessDocument convertedSq;

    @Column(nullable = false, length = 300)  private String title;
    @Column(columnDefinition = "TEXT")       private String description;
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String stage = "PROSPECT";
    // PROSPECT|PROPOSAL|NEGOTIATION|WON|LOST|ON_HOLD

    @Column(name = "estimated_value", precision = 18, scale = 2) private BigDecimal estimatedValue;
    @Column(length = 3)               @Builder.Default private String currency = "BDT";
    @Column(precision = 5, scale = 2)                            private BigDecimal probability;
    @Column(name = "expected_close_date")                        private LocalDate  expectedCloseDate;
    @Column(name = "actual_close_date")                          private LocalDate  actualCloseDate;
    @Column(name = "loss_reason",        columnDefinition = "TEXT") private String lossReason;
    @Column(length = 200)                                         private String competitor;
    @Column(length = 50)                                          private String source;
    @Column(name = "product_interest",   columnDefinition = "TEXT") private String productInterest;
    @Column(name = "next_action",        columnDefinition = "TEXT") private String nextAction;
    @Column(name = "next_action_date")                             private LocalDate nextActionDate;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/crm/Activity.java
package com.asg.spindleserp.crm;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_activities",
    indexes = {
        @Index(name = "idx_act_opp",    columnList = "opportunity_id"),
        @Index(name = "idx_act_type",   columnList = "activity_type"),
        @Index(name = "idx_act_status", columnList = "status"),
        @Index(name = "idx_act_due",    columnList = "due_date")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Activity extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "opportunity_id")      private Opportunity opportunity;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id")             private Lead        lead;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "contact_id")          private Contact     contact;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_user_id") private User        assignedTo;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;
    // CALL|EMAIL|MEETING|DEMO|FOLLOW_UP|TASK|NOTE|SITE_VISIT|PRESENTATION

    @Column(nullable = false, length = 300) private String subject;
    @Column(columnDefinition = "TEXT")      private String description;
    @Column(name = "due_date")              private LocalDateTime dueDate;
    @Column(name = "completed_at")          private LocalDateTime completedAt;
    @Column(columnDefinition = "TEXT")      private String outcome;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN"; // OPEN|COMPLETED|CANCELLED|OVERDUE

    // Call-specific
    @Column(name = "call_duration_mins") private Integer callDurationMins;
    @Column(name = "call_direction",   length = 10) private String callDirection;

    // Meeting-specific
    @Column(length = 300) private String location;
    @Column(columnDefinition = "TEXT") private String attendees;
    @Column(name = "meeting_notes",   columnDefinition = "TEXT") private String meetingNotes;

    @Column(name = "follow_up_date")                            private LocalDate followUpDate;
    @Builder.Default @Column(name = "follow_up_required")       private Boolean   followUpRequired = false;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/crm/Contact.java
package com.asg.spindleserp.crm;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_contacts",
    indexes = {
        @Index(name = "idx_con_org",      columnList = "organization_id"),
        @Index(name = "idx_con_customer", columnList = "customer_id"),
        @Index(name = "idx_con_lead",     columnList = "lead_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contact extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private SubAccount customer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id")     private Lead       lead;

    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name",  nullable = false, length = 100) private String lastName;
    @Column(length = 100) private String email;
    @Column(length = 20)  private String phone;
    @Column(length = 20)  private String mobile;
    @Column(name = "job_title",  length = 100) private String jobTitle;
    @Column(length = 100)                      private String department;
    @Builder.Default @Column(name = "is_primary_contact", nullable = false) private Boolean isPrimaryContact = false;
    @Column(name = "linkedin_url", length = 300) private String linkedinUrl;
    @Column(columnDefinition = "TEXT")           private String notes;
    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 13 — NOTIFICATIONS & AUDIT
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/notification/Notification.java
package com.asg.spindleserp.notification;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ntf_notifications",
    indexes = {
        @Index(name = "idx_ntf_user",   columnList = "user_id"),
        @Index(name = "idx_ntf_unread", columnList = "user_id,is_read")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 300) private String title;
    @Column(columnDefinition = "TEXT")      private String message;
    @Column(name = "notification_type", nullable = false, length = 50) private String notificationType;
    @Column(name = "reference_type", length = 50)  private String referenceType;
    @Column(name = "reference_id")                 private Long   referenceId;
    @Column(length = 10)                           @Builder.Default private String priority = "NORMAL";
    @Builder.Default @Column(name = "is_read", nullable = false) private Boolean isRead    = false;
    @Column(name = "read_at")     private LocalDateTime readAt;
    @Column(name = "expires_at")  private LocalDateTime expiresAt;
    @Column(name = "action_url",  length = 500) private String actionUrl;
    @Column(name = "created_by",  length = 100) private String createdBy;
}

// FILE: com/asg/spindleserp/audit/AuditLog.java
package com.asg.spindleserp.audit;

import com.asg.spindleserp.security.Organization;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "sys_audit_log",
    indexes = {
        @Index(name = "idx_audit_org",    columnList = "organization_id"),
        @Index(name = "idx_audit_user",   columnList = "user_id"),
        @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_audit_date",   columnList = "created_at")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "organization_id") private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")         private User         user;

    @Column(length = 100) private String username;
    @Column(name = "ip_address",  length = 50)  private String ipAddress;
    @Column(name = "user_agent",  length = 500) private String userAgent;
    @Column(nullable = false, length = 50)       private String action;       // CREATE|UPDATE|DELETE|VIEW|LOGIN|LOGOUT
    @Column(name = "entity_type", nullable = false, length = 100) private String entityType;
    @Column(name = "entity_id")                  private Long   entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", columnDefinition = "jsonb")
    private Map<String, Object> changes;

    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 50)               private String module;
    @Column(length = 20) @Builder.Default private String status = "SUCCESS";
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @Column(name = "duration_ms")  private Integer durationMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

// ════════════════════════════════════════════════════════════════════════════
// MODULE 14 — eCOMMERCE
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/ecommerce/EcoStore.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.security.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eco_stores",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_store_org_code", columnNames = {"organization_id","store_code"}),
        @UniqueConstraint(name = "uk_store_slug",     columnNames = {"store_slug"})
    },
    indexes = {
        @Index(name = "idx_store_org",  columnList = "organization_id"),
        @Index(name = "idx_store_slug", columnList = "store_slug")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoStore extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "business_unit_id")    private BusinessUnit businessUnit;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "warehouse_id")        private Warehouse    warehouse;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ar_account_id")       private Account      arAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "revenue_account_id")  private Account      revenueAccount;

    @Column(name = "store_code",  nullable = false, length = 50)  private String storeCode;
    @Column(name = "store_name",  nullable = false, length = 200) private String storeName;
    @Column(name = "store_name_bn", length = 200)                 private String storeNameBn;
    @Column(name = "store_slug",  nullable = false, length = 100) private String storeSlug;
    @Column(name = "store_type",  nullable = false, length = 30)
    @Builder.Default private String storeType = "B2C"; // B2C|B2B|WHOLESALE|MARKETPLACE
    @Column(length = 300) private String domain;
    @Column(name = "logo_url",    length = 500) private String logoUrl;
    @Column(name = "banner_url",  length = 500) private String bannerUrl;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "contact_email",   length = 100) private String contactEmail;
    @Column(name = "contact_phone",   length = 20)  private String contactPhone;
    @Column(name = "default_currency",nullable = false, length = 3)
    @Builder.Default private String defaultCurrency = "BDT";
    @Column(name = "default_language", nullable = false, length = 10)
    @Builder.Default private String defaultLanguage = "en";
    @Builder.Default @Column(name = "tax_inclusive",  nullable = false) private Boolean taxInclusive   = false;
    @Builder.Default @Column(name = "is_active",      nullable = false) private Boolean isActive       = true;
    @Builder.Default @Column(name = "is_maintenance", nullable = false) private Boolean isMaintenance  = false;
    @Column(name = "maintenance_message", columnDefinition = "TEXT") private String maintenanceMessage;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoProduct.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_products",
    uniqueConstraints = @UniqueConstraint(name = "uk_prod_store_slug", columnNames = {"store_id","slug"}),
    indexes = {
        @Index(name = "idx_prod_store",    columnList = "store_id"),
        @Index(name = "idx_prod_item",     columnList = "inv_item_id"),
        @Index(name = "idx_prod_status",   columnList = "status"),
        @Index(name = "idx_prod_featured", columnList = "store_id,is_featured")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoProduct extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inv_item_id", nullable = false)
    private InventoryItem invItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EcoCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_class_id")
    private EcoTaxClass taxClass;

    @Column(name = "product_name",   nullable = false, length = 300) private String productName;
    @Column(name = "product_name_bn", length = 300)                  private String productNameBn;
    @Column(nullable = false, length = 300)                          private String slug;
    @Column(name = "short_description", length = 1000)               private String shortDescription;
    @Column(name = "long_description",  columnDefinition = "TEXT")   private String longDescription;
    @Column(name = "product_type",  nullable = false, length = 30)
    @Builder.Default private String productType = "SIMPLE"; // SIMPLE|VARIABLE|BUNDLE|DIGITAL

    @Column(name = "base_price",   nullable = false, precision = 18, scale = 4) private BigDecimal basePrice;
    @Column(name = "sale_price",   precision = 18, scale = 4)                   private BigDecimal salePrice;
    @Column(name = "sale_starts_at")                                             private LocalDateTime saleStartsAt;
    @Column(name = "sale_ends_at")                                               private LocalDateTime saleEndsAt;
    @Column(name = "cost_price",   precision = 18, scale = 4)                   private BigDecimal costPrice;

    @Builder.Default @Column(name = "is_featured",      nullable = false) private Boolean isFeatured     = false;
    @Builder.Default @Column(name = "is_new_arrival",   nullable = false) private Boolean isNewArrival    = false;
    @Builder.Default @Column(name = "is_best_seller",   nullable = false) private Boolean isBestSeller    = false;
    @Builder.Default @Column(name = "track_inventory",  nullable = false) private Boolean trackInventory  = true;
    @Builder.Default @Column(name = "allow_backorder",  nullable = false) private Boolean allowBackorder  = false;
    @Builder.Default @Column(name = "low_stock_threshold")                private Integer lowStockThreshold = 5;
    @Builder.Default @Column(name = "min_order_qty",    precision = 12, scale = 3) private BigDecimal minOrderQty = BigDecimal.ONE;
    @Builder.Default @Column(name = "sold_qty",         nullable = false, precision = 18, scale = 3) private BigDecimal soldQty = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT|ACTIVE|ARCHIVED|OUT_OF_STOCK|COMING_SOON

    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Builder.Default @Column(name = "avg_rating",    precision = 3, scale = 2) private BigDecimal avgRating    = BigDecimal.ZERO;
    @Builder.Default @Column(name = "review_count") private Integer            reviewCount = 0;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EcoProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EcoProductImage> images = new ArrayList<>();
}

// FILE: com/asg/spindleserp/ecommerce/EcoOrder.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.crm.Opportunity;
import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_orders",
    uniqueConstraints = @UniqueConstraint(name = "uk_eco_order_doc", columnNames = {"business_document_id"}),
    indexes = {
        @Index(name = "idx_order_doc",        columnList = "business_document_id"),
        @Index(name = "idx_order_store",      columnList = "store_id"),
        @Index(name = "idx_order_customer",   columnList = "customer_id"),
        @Index(name = "idx_order_status",     columnList = "order_status"),
        @Index(name = "idx_order_pay_status", columnList = "payment_status"),
        @Index(name = "idx_order_date",       columnList = "created_at")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoOrder extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    /** Header stored in global_business_documents (documentType = ONLINE_ORDER) */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_id", nullable = false, unique = true)
    private BusinessDocument businessDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcoCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private EcoCart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private EcoPaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_method_id")
    private EcoShippingMethod shippingMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private EcoCoupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_doc_id")
    private BusinessDocument salesInvoice;

    @Column(nullable = false, length = 30) @Builder.Default
    private String channel = "WEB"; // WEB|MOBILE|API|POS|WHATSAPP|PHONE

    @Column(name = "order_status", nullable = false, length = 30) @Builder.Default
    private String orderStatus = "PENDING";
    // PENDING|CONFIRMED|PROCESSING|PACKED|SHIPPED|OUT_FOR_DELIVERY|DELIVERED|CANCELLED|REFUNDED

    // Shipping address (snapshot)
    @Column(name = "shipping_address1", nullable = false, length = 300) private String shippingAddress1;
    @Column(name = "shipping_address2", length = 300) private String shippingAddress2;
    @Column(name = "shipping_city",     nullable = false, length = 100) private String shippingCity;
    @Column(name = "shipping_district", length = 100)                   private String shippingDistrict;
    @Column(name = "shipping_state",    length = 100)                   private String shippingState;
    @Column(name = "shipping_country",  nullable = false, length = 2) @Builder.Default private String shippingCountry = "BD";
    @Column(name = "shipping_postal_code", length = 20) private String shippingPostalCode;
    @Column(name = "shipping_phone",    length = 20)  private String shippingPhone;
    @Column(name = "shipping_email",    length = 150) private String shippingEmail;
    @Column(name = "delivery_notes",    columnDefinition = "TEXT") private String deliveryNotes;

    // Amounts
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal subtotal        = BigDecimal.ZERO;
    @Column(name = "coupon_code", length = 50) private String couponCode;
    @Builder.Default @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "tax_amount",      nullable = false, precision = 18, scale = 2) private BigDecimal taxAmount      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "shipping_amount", nullable = false, precision = 18, scale = 2) private BigDecimal shippingAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "cod_charge",      nullable = false, precision = 18, scale = 2) private BigDecimal codCharge      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "total_amount",    nullable = false, precision = 18, scale = 2) private BigDecimal totalAmount    = BigDecimal.ZERO;
    @Builder.Default @Column(name = "refunded_amount", nullable = false, precision = 18, scale = 2) private BigDecimal refundedAmount = BigDecimal.ZERO;
    @Column(length = 3) @Builder.Default private String currency = "BDT";

    @Column(name = "payment_method_name", length = 100)  private String paymentMethodName;
    @Column(name = "shipping_method_name", length = 100) private String shippingMethodName;
    @Column(name = "estimated_delivery_date")             private LocalDate estimatedDeliveryDate;
    @Column(name = "actual_delivery_date")                private LocalDate actualDeliveryDate;

    @Column(name = "payment_status", nullable = false, length = 30) @Builder.Default
    private String paymentStatus = "PENDING";
    // PENDING|PAID|PARTIALLY_PAID|FAILED|REFUNDED|PARTIALLY_REFUNDED|COD_COLLECTED

    @Column(name = "packed_at")  private LocalDateTime packedAt;
    @Column(name = "packed_by",  length = 100) private String packedBy;
    @Column(name = "customer_note",  columnDefinition = "TEXT") private String customerNote;
    @Column(name = "internal_note",  columnDefinition = "TEXT") private String internalNote;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @OneToMany(mappedBy = "ecoOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcoOrderLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "ecoOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    @Builder.Default
    private List<EcoOrderStatusHistory> statusHistory = new ArrayList<>();
}

// FILE: com/asg/spindleserp/ecommerce/EcoPaymentTransaction.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.setup.BankAccount;
import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "eco_payment_transactions",
    indexes = {
        @Index(name = "idx_ptxn_order",  columnList = "eco_order_id"),
        @Index(name = "idx_ptxn_status", columnList = "status"),
        @Index(name = "idx_ptxn_ref",    columnList = "transaction_ref"),
        @Index(name = "idx_ptxn_method", columnList = "payment_method_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoPaymentTransaction extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private EcoPaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by_user_id")
    private User collectedBy;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;
    // PAYMENT|PARTIAL_PAYMENT|REFUND|PARTIAL_REFUND|CHARGEBACK|COD_COLLECTION

    @Column(name = "transaction_ref",    length = 200) private String transactionRef;
    @Column(name = "gateway_order_id",   length = 200) private String gatewayOrderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse;

    @Column(nullable = false, precision = 18, scale = 2)                          private BigDecimal amount;
    @Column(length = 3) @Builder.Default                                           private String     currency    = "BDT";
    @Builder.Default @Column(name = "exchange_rate", precision = 18, scale = 4)   private BigDecimal exchangeRate = BigDecimal.ONE;
    @Builder.Default @Column(name = "gateway_fee",   precision = 18, scale = 2)   private BigDecimal gatewayFee   = BigDecimal.ZERO;
    @Column(name = "net_amount", precision = 18, scale = 2)                       private BigDecimal netAmount;

    @Column(nullable = false, length = 30) @Builder.Default
    private String status = "PENDING";
    // PENDING|INITIATED|SUCCESS|FAILED|CANCELLED|EXPIRED|REFUNDED

    @Column(name = "initiated_at")   private LocalDateTime initiatedAt;
    @Column(name = "completed_at")   private LocalDateTime completedAt;
    @Column(name = "failed_at")      private LocalDateTime failedAt;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
    @Column(name = "payer_name",    length = 200) private String payerName;
    @Column(name = "payer_mobile",  length = 20)  private String payerMobile;
    @Column(name = "payer_account", length = 100) private String payerAccount;
    @Column(name = "collected_at")                private LocalDateTime collectedAt;
    @Column(name = "collection_notes", columnDefinition = "TEXT") private String collectionNotes;
    @Builder.Default @Column(name = "is_reconciled", nullable = false) private Boolean isReconciled = false;
    @Column(name = "reconciled_at")  private LocalDateTime reconciledAt;
    @Column(name = "reconciled_by",  length = 100) private String reconciledBy;
    @Column(name = "ip_address",     length = 50)  private String ipAddress;
    @Column(name = "created_by",     length = 100) private String createdBy;
    @Column(name = "updated_by",     length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoShipment.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "eco_shipments",
    uniqueConstraints = @UniqueConstraint(name = "uk_shipment_doc", columnNames = {"business_document_id"}),
    indexes = {
        @Index(name = "idx_ship_doc",      columnList = "business_document_id"),
        @Index(name = "idx_ship_order",    columnList = "eco_order_id"),
        @Index(name = "idx_ship_status",   columnList = "status"),
        @Index(name = "idx_ship_tracking", columnList = "tracking_number")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoShipment extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    /** Header in global_business_documents (documentType = ONLINE_SHIPMENT) */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_id", nullable = false, unique = true)
    private BusinessDocument businessDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_method_id")
    private EcoShippingMethod shippingMethod;

    @Column(name = "tracking_number", length = 200) private String trackingNumber;
    @Column(length = 100)                            private String carrier;
    @Column(name = "courier_order_id", length = 200) private String courierOrderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "courier_response", columnDefinition = "jsonb")
    private Map<String, Object> courierResponse;

    @Column(name = "shipping_label_url", length = 500) private String shippingLabelUrl;
    @Column(name = "invoice_url",        length = 500) private String invoiceUrl;

    @Column(nullable = false, length = 30) @Builder.Default
    private String status = "PENDING";
    // PENDING|PACKED|DISPATCHED|IN_TRANSIT|OUT_FOR_DELIVERY|DELIVERED|FAILED|RETURNED

    @Column(name = "packed_at")      private LocalDateTime packedAt;
    @Column(name = "packed_by",      length = 100) private String packedBy;
    @Column(name = "dispatched_at")  private LocalDateTime dispatchedAt;
    @Column(name = "dispatched_by",  length = 100) private String dispatchedBy;
    @Column(name = "delivered_at")   private LocalDateTime deliveredAt;
    @Column(name = "delivery_confirmed_by", length = 100) private String deliveryConfirmedBy;

    @Column(name = "actual_weight_kg", precision = 8, scale = 3)  private BigDecimal actualWeightKg;
    @Column(name = "shipping_cost",    precision = 18, scale = 2)  private BigDecimal shippingCost;
    @Column(columnDefinition = "TEXT")                             private String notes;
    @Builder.Default @Column(name = "stock_posted", nullable = false) private Boolean stockPosted = false;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcoShipmentItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("eventTime ASC")
    @Builder.Default
    private List<EcoShipmentTracking> trackingEvents = new ArrayList<>();
}

// ════════════════════════════════════════════════════════════════════════════
// ENUM CLASSES (cross-cutting)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/inventory/setup/ItemType.java
package com.asg.spindleserp.inventory.setup;
public enum ItemType {
    YARN, FIBER, RAW_COTTON, CHEMICALS, DYES, GENERAL,
    SPARE_PARTS, FIXED_ASSET, PACKAGING, MRO, FABRICS, WORK_IN_PROGRESS
}

// FILE: com/asg/spindleserp/inventory/setup/InventoryLotStatus.java
package com.asg.spindleserp.inventory.setup;
public enum InventoryLotStatus { AVAILABLE, QUARANTINE, EXPIRED, CONSUMED, DISPOSED, RESERVED }

// FILE: com/asg/spindleserp/inventory/setup/Certification.java
package com.asg.spindleserp.inventory.setup;
public enum Certification { ORGANIC, GOTS, BCI, OEKO_TEX, NONE }

// FILE: com/asg/spindleserp/inventory/setup/ColorGrade.java
package com.asg.spindleserp.inventory.setup;
public enum ColorGrade { PREMIUM, GRADE_A, GRADE_B, GRADE_C }

// FILE: com/asg/spindleserp/global/documents/MovementType.java
package com.asg.spindleserp.global.documents;
public enum MovementType {
    PURCHASE_RECEIPT, PRODUCTION_RECEIPT, SALE_ISSUE, STORE_ISSUE,
    PRODUCTION_MATERIAL_ISSUE, TRANSFER_IN, TRANSFER_OUT,
    ADJUSTMENT_IN, ADJUSTMENT_OUT, CUSTOMER_RETURN, SUPPLIER_RETURN,
    ECOM_SALE_ISSUE, ECOM_RETURN_RECEIPT
}

// FILE: com/asg/spindleserp/global/documents/BusinessDocumentStatus.java
package com.asg.spindleserp.global.documents;
public enum BusinessDocumentStatus {
    DRAFT, SUBMITTED, IN_APPROVAL, APPROVED, REJECTED, RETURNED,
    PROCESSING, PARTIAL, PARTIALLY_CONVERTED, CONVERTED, COMPLETED,
    CANCELLED, SENT
}

// FILE: com/asg/spindleserp/approval/ApprovalStatus.java
package com.asg.spindleserp.approval;
public enum ApprovalStatus {
    DRAFT, SUBMITTED, IN_APPROVAL, APPROVED, AUTO_APPROVED,
    REJECTED, RETURNED, RECALLED, CANCELLED, CLOSED
}

// FILE: com/asg/spindleserp/approval/ApprovalAction.java
package com.asg.spindleserp.approval;
public enum ApprovalAction {
    SUBMIT, APPROVE, REJECT, RETURN, RECALL, HOLD,
    FORWARD, DELEGATE, ESCALATE
}

// FILE: com/asg/spindleserp/approval/DocumentType.java
package com.asg.spindleserp.approval;
/**
 * DocumentType enum — MUST remain complete.
 * Missing values cause ApprovalCallbackRegistry routing failure at runtime.
 */
public enum DocumentType {
    // Procurement
    STORE_REQUISITION, PURCHASE_REQUISITION, REQUEST_FOR_QUOTATION,
    COMPARATIVE_STATEMENT, PURCHASE_ORDER, GOODS_RECEIPT_NOTE,
    PURCHASE_INVOICE, DEBIT_NOTE,
    // Sales
    SALES_QUOTATION, SALES_ORDER, DELIVERY_ORDER,
    DELIVERY_CHALLAN, SALES_INVOICE, CREDIT_NOTE,
    // Production
    PRODUCTION_REQUISITION, PRODUCTION_MATERIAL_ISSUE, FINISHED_GOODS_RECEIVE,
    // Commercial
    EXPORT_PROFORMA_INVOICE, IMPORT_PROFORMA_INVOICE,
    LETTER_OF_CREDIT, COMMERCIAL_INVOICE,
    // Inventory
    MATERIAL_ISSUE, STOCK_TRANSFER, STOCK_ADJUSTMENT,
    // Finance
    JOURNAL_VOUCHER, PAYMENT_VOUCHER, RECEIPT_VOUCHER,
    // eCommerce
    ONLINE_ORDER, ONLINE_SHIPMENT, ONLINE_RETURN, ONLINE_INVOICE
}

// ════════════════════════════════════════════════════════════════════════════
// SPRING CONFIGURATION
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/config/SpringSecurityAuditorAware.java
package com.asg.spindleserp.config;

import com.asg.spindleserp.security.ContextProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAware")
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        String username = ContextProvider.getCurrentUsername();
        return Optional.ofNullable(username != null ? username : "system");
    }
}

// FILE: com/asg/spindleserp/config/JpaAuditingConfig.java
package com.asg.spindleserp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig { }

// ════════════════════════════════════════════════════════════════════════════
// ENTITY SUMMARY
// ════════════════════════════════════════════════════════════════════════════
/*
  Base Classes (MappedSuperclass):
  ─────────────────────────────────────────────────────────────────────────
  BaseAuditEntity    extends nothing   — Spring Data Auditing + org isolation
                                         Use for: most transactional entities
  BaseOrgEntity      extends nothing   — Hibernate timestamps + org isolation
                                         Use for: reference/master data with manual audit
  BaseReferenceEntity extends nothing  — Timestamps only (no org column)
                                         Use for: global lookups (Country, Currency, HsCode)

  Module Entities and their base class:
  ─────────────────────────────────────────────────────────────────────────
  MODULE 1 — Core/Security
    Organization         (none — tenant root, no org FK)
    BusinessUnit         → BaseOrgEntity
    User                 (none — cross-org, owns security context)
    Role, Permission     (none — global)
    Menu, RoleMenu       (none — global)

  MODULE 2 — Location/Masters
    Country, State       → BaseReferenceEntity
    Currency, HsCode     → BaseReferenceEntity
    GlobalTermsCondition → BaseOrgEntity

  MODULE 3 — Inventory Masters
    ItemCategory         → BaseOrgEntity
    UnitsOfMeasure       → BaseOrgEntity
    YarnType/Count/Ply/Blend → BaseOrgEntity
    InventoryItem        → BaseAuditEntity   (Spring Data audit)
    YarnItem             → BaseAuditEntity

  MODULE 4 — Warehouse & Stock
    Warehouse            → BaseOrgEntity
    GlobalInventoryLot   → BaseOrgEntity
    InventoryStockBalance (plain entity — no base, org field explicit)
    InventoryTransaction  (plain entity — immutable, no updated_at)

  MODULE 5 — Finance
    Bank, Account, SubAccount → BaseOrgEntity
    BankAccount, CashAccount, Customer, Supplier → SubAccount (JOINED)
    CostCenter           → BaseOrgEntity
    JournalEntry         → BaseOrgEntity
    JournalEntryLine     (plain — child of JournalEntry)

  MODULE 6 — Approval
    ApprovalConfig       → BaseOrgEntity
    ApprovalLevel        (plain — child, no org column)
    ApprovalRequest      (plain — polymorphic, org field explicit)
    ApprovalHistory      (plain — immutable audit)

  MODULE 7 — Documents (MDST)
    BusinessDocument     → BaseAuditEntity   (Spring Data audit)
    BusinessDocumentLine → BaseAuditEntity
    BusinessDocumentLineLot (plain — child, immutable)

  MODULE 8 — Commercial
    CommercialLc         → BaseOrgEntity

  MODULE 9 — HRM
    Department, Designation → BaseOrgEntity (manual @PrePersist)
    Employee             (plain — manual org assignment)
    Attendance           → BaseOrgEntity
    EmployeeLeave        → BaseOrgEntity
    PayrollRun           → BaseOrgEntity

  MODULE 10 — Production
    ProductionOrder      → BaseOrgEntity
    ProductionRecipe     → BaseOrgEntity
    ProductionRecipeItem, ProductionRecipeItemLot (plain — children)

  MODULE 11 — Fixed Assets
    AssetCategory        → BaseOrgEntity
    FixedAsset           → BaseOrgEntity

  MODULE 12 — CRM
    Lead, Opportunity, Activity, Contact → BaseOrgEntity

  MODULE 13 — Notifications/Audit
    Notification         → BaseOrgEntity
    AuditLog             (plain — explicit org FK, JSONB columns)

  MODULE 14 — eCommerce
    EcoStore             → BaseOrgEntity
    EcoProduct           → BaseOrgEntity
    EcoOrder             → BaseOrgEntity    (1:1 with BusinessDocument)
    EcoShipment          → BaseOrgEntity    (1:1 with BusinessDocument)
    EcoPaymentTransaction → BaseOrgEntity
    [EcoCategory, EcoCustomer, EcoCart, etc. → BaseOrgEntity]
*/
