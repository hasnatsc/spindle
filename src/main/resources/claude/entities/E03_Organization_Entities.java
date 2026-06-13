// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E03  Organization Hierarchy                               ║
// ║  Tables: org_organizations, org_business_units, org_cost_centers,        ║
// ║           org_warehouses, org_departments, user_context                  ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: organization/entity/Organization.java ──────────────────────────────
package com.hasnat.optimum.organization.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "org_organizations",
    indexes = {
        @Index(name = "idx_org_code",   columnList = "code"),
        @Index(name = "idx_org_active", columnList = "is_active")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200) private String nameBn;
    @Column(columnDefinition = "text") private String about;
    @Column(columnDefinition = "text") private String address;
    @Column(length = 100) private String city;
    @Column(length = 100) private String state;
    @Column(length = 100) private String country;
    @Column(length = 20)  private String postalCode;
    @Column(length = 20)  private String phone;
    @Column(length = 100) private String email;
    @Column(length = 255) private String website;
    @Column(length = 500) private String logoUrl;
    private LocalDate establishedDate;
    @Column(length = 50) private String taxId;
    @Column(length = 50) private String vatNo;
    @Column(length = 50) private String binNo;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}


// ── FILE: organization/entity/BusinessUnit.java ──────────────────────────────
package com.hasnat.optimum.organization.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_business_units",
    uniqueConstraints = @UniqueConstraint(name = "uq_bu_org_code",
        columnNames = {"organization_id", "code"}),
    indexes = @Index(name = "idx_bu_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessUnit extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}


// ── FILE: organization/entity/CostCenter.java ────────────────────────────────
package com.hasnat.optimum.organization.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_cost_centers",
    indexes = {
        @Index(name = "idx_cc_bu",     columnList = "business_unit_id"),
        @Index(name = "idx_cc_parent", columnList = "parent_cost_center_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CostCenter extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_unit_id", nullable = false)
    private BusinessUnit businessUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_cost_center_id")
    private CostCenter parentCostCenter;

    @Column(nullable = false, unique = true, length = 50)
    private String costCenterCode;

    @Column(nullable = false, length = 200)
    private String costCenterName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CostCenterType costCenterType;

    @Column(length = 1000) private String description;
    @Column(length = 100)  private String managerName;
    @Column(length = 100)  private String managerEmail;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    public enum CostCenterType { DEPARTMENT, PROJECT, BRANCH, DIVISION, PRODUCT, SERVICE }
}


// ── FILE: organization/entity/Warehouse.java ─────────────────────────────────
package com.hasnat.optimum.organization.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.common.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_warehouses",
    indexes = @Index(name = "idx_wh_bu", columnList = "business_unit_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Warehouse extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_unit_id", nullable = false)
    private BusinessUnit businessUnit;

    @Column(nullable = false, unique = true, length = 50)
    private String warehouseCode;

    @Column(nullable = false, length = 200)
    private String warehouseName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemType itemType;

    @Column(columnDefinition = "text") private String address;
    @Column(length = 100) private String managerName;
    @Column(length = 20)  private String contactNumber;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}


// ── FILE: organization/entity/Department.java ────────────────────────────────
package com.hasnat.optimum.organization.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "org_departments",
    indexes = {
        @Index(name = "idx_dept_org",    columnList = "organization_id"),
        @Index(name = "idx_dept_parent", columnList = "parent_department_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    // Deferred FK to hrm_employees.id — stored as plain Long to avoid circular dep
    @Column(name = "head_employee_id")
    private Long headEmployeeId;

    @Column(unique = true, length = 50)  private String code;
    @Column(nullable = false, unique = true, length = 100) private String name;
    @Column(length = 500) private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}


// ── FILE: organization/entity/UserContext.java ───────────────────────────────
package com.hasnat.optimum.organization.entity;

import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_context")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserContext {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private Long organizationId;
    private Long businessUnitId;
    private Long costCenterId;
    private Long warehouseId;

    @Column(length = 20) private String approvalDefaultView;
    private Boolean approvalDesktopNotification;
    private Boolean approvalEmailEnabled;
    private Boolean approvalPushEnabled;
    private Boolean approvalSmsEnabled;
    private Boolean approvalWhatsappEnabled;
    private Boolean approvalSoundEnabled;
    @Column(length = 20) private String approvalNotificationFrequency;
    private Integer approvalRefreshInterval;
    private Boolean showApprovalBadge;
    private Long lastViewedNotificationId;
}
