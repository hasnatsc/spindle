// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  SpindleERP — REMAINING ENTITIES (Part 2 of 2)                         ║
// ║  49 entity classes completing the full 115-table schema                ║
// ║  Package: com.asg.spindleserp                                           ║
// ║  Java 21 | Spring Boot 3.5 | JPA/Hibernate | Lombok                    ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
//  Coverage (tables added in this file):
//  ─────────────────────────────────────────────────────────────────────────
//  Yarn Masters    : YarnCount, YarnPly, YarnBlend (3)
//  Location        : District, City (2)
//  STP Setup       : DocumentSequence, DocumentFile (2)
//  HRM (Full)      : Department, Designation, Employee, EmployeeAddress,
//                    EmployeeDocument, EmployeeSalary, PayrollRunLine (7)
//  Finance         : OpeningBalance (1)
//  Approval        : ApprovalLevel, ApprovalRequest (moved to full defs) (2)
//  Production      : ProductionRecipeItem, ProductionRecipeItemLot (2)
//  Fixed Assets    : DepreciationRun, DepreciationRunLine,
//                    AssetDisposal, AssetTransfer (4)
//  CRM             : CrmQuotation (1)
//  Inventory       : InventoryTransaction (1)
//  eCommerce (Full): EcoTaxClass, EcoCategory, EcoAttributeGroup,
//                    EcoAttributeValue, EcoProductVariant, EcoProductImage,
//                    EcoCustomer, EcoCustomerAddress, EcoWishlist,
//                    EcoRecentlyViewed, EcoCart, EcoCartItem,
//                    EcoOrderLine, EcoOrderStatusHistory, EcoCouponUsage,
//                    EcoPaymentMethod, EcoRefund, EcoStoreCredit,
//                    EcoShippingZone, EcoShippingMethod, EcoShipmentItem,
//                    EcoShipmentTracking, EcoReturn, EcoReturnItem,
//                    EcoReview, EcoCustomerNotification,
//                    EcoCoupon (26)
//  Notifications   : EmailQueue (1)
//  Enums (new)     : Gender, BloodGroup, MaritalStatus, EmployeeType,
//                    EmployeeStatus, AddressType (6)
// ─────────────────────────────────────────────────────────────────────────

// ════════════════════════════════════════════════════════════════════════════
// YARN CLASSIFICATION MASTERS  (BaseOrgEntity)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/inventory/item/YarnCount.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "yrn_counts",
    uniqueConstraints = @UniqueConstraint(name = "uk_yrn_cnt_org_code",
        columnNames = {"organization_id","count_code"}),
    indexes = @Index(name = "idx_yrn_cnt_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class YarnCount extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "count_code", nullable = false, length = 20) private String countCode;
    @Column(name = "count_name", nullable = false, length = 100) private String countName;
    @Column(name = "count_value", nullable = false, precision = 10, scale = 2) private BigDecimal countValue;
    @Column(columnDefinition = "TEXT") private String description;

    @Builder.Default @Column(name = "is_active",   nullable = false) private Boolean isActive   = true;
    @Builder.Default @Column(name = "is_approved", nullable = false) private Boolean isApproved = false;
    @Column(name = "approved_by",    length = 100) private String approvedBy;
    @Column(name = "approval_remarks", length = 500) private String approvalRemarks;
    @Column(name = "approved_at")  private java.time.LocalDateTime approvedAt;
    @Column(name = "created_by",   length = 100) private String createdBy;
    @Column(name = "updated_by",   length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/inventory/item/YarnPly.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yrn_plies",
    uniqueConstraints = @UniqueConstraint(name = "uk_yrn_ply_org_num",
        columnNames = {"organization_id","ply_number"}),
    indexes = @Index(name = "idx_yrn_ply_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class YarnPly extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ply_number", nullable = false) private Integer plyNumber;
    @Column(name = "ply_code",   nullable = false, length = 20) private String plyCode;
    @Column(name = "ply_name",   nullable = false, length = 50) private String plyName;
    @Column(columnDefinition = "TEXT") private String description;

    @Builder.Default @Column(name = "is_active",   nullable = false) private Boolean isActive   = true;
    @Builder.Default @Column(name = "is_approved", nullable = false) private Boolean isApproved = false;
    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "approved_at") private java.time.LocalDateTime approvedAt;
    @Column(name = "created_by",  length = 100) private String createdBy;
    @Column(name = "updated_by",  length = 100) private String updatedBy;

    /** Display label used by YarnItem.buildDisplayName() */
    public String getDisplayName() {
        return plyCode != null ? plyCode : String.valueOf(plyNumber);
    }
}

// FILE: com/asg/spindleserp/inventory/item/YarnBlend.java
package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "yrn_blends",
    uniqueConstraints = @UniqueConstraint(name = "uk_yrn_bld_org_code",
        columnNames = {"organization_id","blend_code"}),
    indexes = @Index(name = "idx_yrn_bld_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class YarnBlend extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blend_code", nullable = false, length = 30)  private String blendCode;
    @Column(name = "blend_name", nullable = false, length = 200) private String blendName;
    @Column(columnDefinition = "TEXT") private String description;

    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// ════════════════════════════════════════════════════════════════════════════
// LOCATION MASTERS
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/security/locations/District.java
package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "stp_districts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class District extends BaseReferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "state_id") private State state;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "name_bn", length = 100) private String nameBn;
    @Builder.Default @Column(nullable = false) private Boolean active = true;
}

// FILE: com/asg/spindleserp/security/locations/City.java
package com.asg.spindleserp.security.locations;

import com.asg.spindleserp.common.BaseReferenceEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "stp_cities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class City extends BaseReferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "district_id", nullable = false) private District district;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "name_bn", length = 150) private String nameBn;
    @Builder.Default @Column(nullable = false) private Boolean active = true;
}

// ════════════════════════════════════════════════════════════════════════════
// SETUP / UTILITY TABLES
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/stp/DocumentSequence.java
package com.asg.spindleserp.stp;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * DocumentSequence — per-org, per-prefix, per-year-month counter.
 * Used by fn_next_doc_no() via JDBC and also accessible from JPA.
 * Always use fn_next_doc_no() stored function for thread-safe increment.
 */
@Entity
@Table(name = "stp_document_sequences",
    uniqueConstraints = @UniqueConstraint(name = "uk_doc_seq",
        columnNames = {"organization_id","prefix","year_month"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentSequence implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 20) private String prefix;
    @Column(name = "year_month", nullable = false, length = 4) private String yearMonth;  // YYMM
    @Builder.Default @Column(name = "last_seq", nullable = false) private Integer lastSeq = 0;
}

// FILE: com/asg/spindleserp/stp/DocumentFile.java
package com.asg.spindleserp.stp;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DocumentFile — polymorphic file attachment.
 * document_type + reference_id point to any entity.
 */
@Entity
@Table(name = "stp_document_files",
    indexes = @Index(name = "idx_docfile_ref", columnList = "document_type,reference_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentFile implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "document_type", nullable = false, length = 50) private String documentType;
    @Column(name = "reference_id",  nullable = false)              private Long   referenceId;
    @Column(name = "file_name",          length = 255)  private String fileName;
    @Column(name = "original_file_name", length = 255)  private String originalFileName;
    @Column(name = "file_type",          length = 50)   private String fileType;     // PDF|JPG|PNG|XLSX
    @Column(name = "file_path",          length = 1000) private String filePath;
    @Column(name = "file_size")                          private Long   fileSize;
    @Column(name = "document_category",  length = 200)  private String documentCategory;
    @Column(length = 500)                                private String remarks;
    @Column(name = "uploaded_by",        length = 100)  private String uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}

// ════════════════════════════════════════════════════════════════════════════
// HRM — COMPLETE ENTITIES
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/hrm/setup/Department.java
package com.asg.spindleserp.hrm.setup;

import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.security.ContextProvider;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "org_departments",
    uniqueConstraints = @UniqueConstraint(name = "uk_dept_org_name", columnNames = {"organization_id","name"}),
    indexes = {
        @Index(name = "idx_dept_org",    columnList = "organization_id"),
        @Index(name = "idx_dept_parent", columnList = "parent_department_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"employees","parentDepartment","departmentHead"})
public class Department implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    /**
     * Deferred circular FK: Department.head_employee_id → hrm_employees.id
     * Set AFTER employee is created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_employee_id")
    private Employee departmentHead;

    @Column(nullable = false, length = 100) private String name;
    @Column(length = 50)                    private String code;
    @Column(length = 500)                   private String description;
    @Builder.Default @Column(nullable = false) private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private Set<Employee> employees = new HashSet<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;

    @PrePersist
    public void assignOrganizationOnCreate() {
        if (organization == null) organization = ContextProvider.getOrganizationReference();
    }

    public int getEmployeeCount() { return employees != null ? employees.size() : 0; }
}

// FILE: com/asg/spindleserp/hrm/setup/Designation.java
package com.asg.spindleserp.hrm.setup;

import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.security.ContextProvider;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hrm_designations",
    uniqueConstraints = @UniqueConstraint(name = "uk_desig_org_title", columnNames = {"organization_id","title"}),
    indexes = @Index(name = "idx_desig_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"employees"})
public class Designation implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(nullable = false, length = 100) private String title;
    @Column(length = 50)                    private String code;
    @Column(length = 500)                   private String description;

    @Column(nullable = false)
    private Integer level;  // 1 = CEO (highest), 10 = entry level

    @Column(name = "min_salary", precision = 12, scale = 2) private BigDecimal minSalary;
    @Column(name = "max_salary", precision = 12, scale = 2) private BigDecimal maxSalary;
    @Builder.Default @Column(nullable = false) private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "designation", cascade = CascadeType.ALL)
    private Set<Employee> employees = new HashSet<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;

    @PrePersist
    public void assignOrganizationOnCreate() {
        if (organization == null) organization = ContextProvider.getOrganizationReference();
    }

    public int getEmployeeCount() { return employees != null ? employees.size() : 0; }
}

// FILE: com/asg/spindleserp/hrm/pims/Employee.java
package com.asg.spindleserp.hrm.pims;

import com.asg.spindleserp.hrm.attendance.Attendance;
import com.asg.spindleserp.hrm.attendance.EmployeeLeave;
import com.asg.spindleserp.hrm.payroll.EmployeeSalary;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.hrm.setup.Designation;
import com.asg.spindleserp.security.ContextProvider;
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
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hrm_employees",
    uniqueConstraints = @UniqueConstraint(name = "uk_emp_org_code",
        columnNames = {"organization_id","employee_code"}),
    indexes = {
        @Index(name = "idx_emp_org",     columnList = "organization_id"),
        @Index(name = "idx_emp_dept",    columnList = "department_id"),
        @Index(name = "idx_emp_desig",   columnList = "designation_id"),
        @Index(name = "idx_emp_status",  columnList = "status"),
        @Index(name = "idx_emp_manager", columnList = "reporting_manager_id"),
        @Index(name = "idx_emp_user",    columnList = "user_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@ToString(exclude = {"department","designation","reportingManager","addresses","documents","salaries","leaves","attendances"})
public class Employee implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id")
    private Employee reportingManager;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // ── Identity ─────────────────────────────────────────────────────────
    @Column(name = "employee_code", nullable = false, unique = true, length = 50) private String employeeCode;
    @Column(name = "first_name",    nullable = false, length = 100)               private String firstName;
    @Column(name = "middle_name",   length = 100)                                 private String middleName;
    @Column(name = "last_name",     nullable = false, length = 100)               private String lastName;
    @Column(length = 100)                                                          private String email;
    @Column(nullable = false, unique = true, length = 20)                          private String phone;
    @Column(name = "alternate_phone", length = 20)                                 private String alternatePhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;                  // MALE|FEMALE|OTHER

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", length = 5)
    private BloodGroup bloodGroup;          // A+|A-|B+|B-|O+|O-|AB+|AB-

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 20)
    private MaritalStatus maritalStatus;    // SINGLE|MARRIED|DIVORCED|WIDOWED

    @Column(name = "national_id",      unique = true, length = 50) private String nationalId;
    @Column(name = "passport_number",  unique = true, length = 50) private String passportNumber;

    // ── Employment ───────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false, length = 20)
    @Builder.Default
    private EmployeeType employeeType = EmployeeType.PERMANENT;
    // PERMANENT|CONTRACT|PART_TIME|INTERN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.PROBATION;
    // ACTIVE|PROBATION|RESIGNED|TERMINATED|ON_LEAVE|RETIRED

    @Column(name = "joining_date",        nullable = false) private LocalDate joiningDate;
    @Column(name = "confirmation_date")                     private LocalDate confirmationDate;
    @Column(name = "probation_end_date")                    private LocalDate probationEndDate;
    @Column(name = "resignation_date")                      private LocalDate resignationDate;
    @Column(name = "exit_date")                             private LocalDate exitDate;

    // ── Salary & Banking ─────────────────────────────────────────────────
    @Column(name = "basic_salary",         precision = 12, scale = 2) private BigDecimal basicSalary;
    @Column(name = "gross_salary",         precision = 12, scale = 2) private BigDecimal grossSalary;
    @Column(name = "bank_name",            length = 50) private String bankName;
    @Column(name = "bank_account_number",  length = 50) private String bankAccountNumber;
    @Column(name = "bank_branch",          length = 50) private String bankBranch;

    // ── Work ────────────────────────────────────────────────────────────
    @Column(name = "work_location", length = 50) private String workLocation;
    @Column(name = "work_shift",    length = 50) private String workShift;
    @Builder.Default @Column(name = "annual_leave_days")  private Integer annualLeaveDays  = 0;
    @Builder.Default @Column(name = "sick_leave_days")    private Integer sickLeaveDays    = 0;
    @Builder.Default @Column(name = "casual_leave_days")  private Integer casualLeaveDays  = 0;

    // ── Emergency ───────────────────────────────────────────────────────
    @Column(name = "emergency_contact_name",     length = 100) private String emergencyContactName;
    @Column(name = "emergency_contact_phone",    length = 20)  private String emergencyContactPhone;
    @Column(name = "emergency_contact_relation", length = 100) private String emergencyContactRelation;

    @Column(name = "profile_picture", length = 255) private String profilePicture;
    @Column(length = 1000)                          private String notes;

    // ── Collections ─────────────────────────────────────────────────────
    @Builder.Default
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EmployeeAddress> addresses = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    private Set<EmployeeDocument> documents = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EmployeeSalary> salaries = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EmployeeLeave> leaves = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attendance> attendances = new HashSet<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;

    @PrePersist
    public void assignOrganizationOnCreate() {
        if (organization == null) organization = ContextProvider.getOrganizationReference();
    }

    // ── Helpers ─────────────────────────────────────────────────────────
    public String getFullName() {
        StringBuilder sb = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) sb.append(" ").append(middleName);
        sb.append(" ").append(lastName);
        return sb.toString();
    }

    public boolean isActive() { return status == EmployeeStatus.ACTIVE; }

    public long getTenureInMonths() {
        return joiningDate != null ? ChronoUnit.MONTHS.between(joiningDate, LocalDate.now()) : 0;
    }

    @Transient
    public boolean getIsDepartmentHead() {
        return department != null
            && department.getDepartmentHead() != null
            && department.getDepartmentHead().getId().equals(this.id);
    }
}

// FILE: com/asg/spindleserp/hrm/pims/EmployeeAddress.java
package com.asg.spindleserp.hrm.pims;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "hrm_employee_addresses",
    indexes = @Index(name = "idx_eaddr_emp", columnList = "employee_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeAddress implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    @Builder.Default
    private AddressType addressType = AddressType.PRESENT; // PRESENT|PERMANENT

    @Column(name = "address_line1", length = 200) private String addressLine1;
    @Column(name = "address_line2", length = 200) private String addressLine2;
    @Column(length = 100) private String city;
    @Column(length = 100) private String district;
    @Column(length = 100) private String state;
    @Column(length = 100) private String country;
    @Column(name = "postal_code", length = 20) private String postalCode;
}

// FILE: com/asg/spindleserp/hrm/pims/EmployeeDocument.java
package com.asg.spindleserp.hrm.pims;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_documents",
    indexes = @Index(name = "idx_edoc_emp", columnList = "employee_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeDocument implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // NID|PASSPORT|TIN|BIRTH_CERTIFICATE|CERTIFICATE|CONTRACT

    @Column(name = "file_url",   length = 500) private String fileUrl;
    @Column(name = "expiry_date")              private LocalDate expiryDate;
    @Column(length = 500)                      private String remarks;
    @Column(name = "uploaded_by", length = 100) private String uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}

// FILE: com/asg/spindleserp/hrm/payroll/EmployeeSalary.java
package com.asg.spindleserp.hrm.payroll;

import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_salaries",
    indexes = @Index(name = "idx_sal_emp", columnList = "employee_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeSalary implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "effective_date", nullable = false) private LocalDate effectiveDate;

    @Column(name = "basic_salary",    nullable = false, precision = 12, scale = 2) private BigDecimal basicSalary;
    @Builder.Default @Column(name = "house_rent",      precision = 12, scale = 2) private BigDecimal houseRent      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "medical",         precision = 12, scale = 2) private BigDecimal medical        = BigDecimal.ZERO;
    @Builder.Default @Column(name = "transport",       precision = 12, scale = 2) private BigDecimal transport      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "other_allowances",precision = 12, scale = 2) private BigDecimal otherAllowances = BigDecimal.ZERO;
    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)    private BigDecimal grossSalary;
    @Builder.Default @Column(name = "income_tax",      precision = 12, scale = 2) private BigDecimal incomeTax      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "provident_fund",  precision = 12, scale = 2) private BigDecimal providentFund  = BigDecimal.ZERO;
    @Builder.Default @Column(name = "other_deductions",precision = 12, scale = 2) private BigDecimal otherDeductions = BigDecimal.ZERO;
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)      private BigDecimal netSalary;
    @Column(length = 500) private String remarks;
    @Column(name = "created_by", length = 100) private String createdBy;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}

// FILE: com/asg/spindleserp/hrm/payroll/PayrollRunLine.java
package com.asg.spindleserp.hrm.payroll;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.hrm.pims.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "hrm_payroll_run_lines",
    indexes = {
        @Index(name = "idx_prl_run", columnList = "payroll_run_id"),
        @Index(name = "idx_prl_emp", columnList = "employee_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRunLine implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "basic_salary",     nullable = false, precision = 12, scale = 2) private BigDecimal basicSalary;
    @Builder.Default @Column(name = "house_rent",       precision = 12, scale = 2) private BigDecimal houseRent       = BigDecimal.ZERO;
    @Builder.Default @Column(name = "medical",          precision = 12, scale = 2) private BigDecimal medical         = BigDecimal.ZERO;
    @Builder.Default @Column(name = "transport",        precision = 12, scale = 2) private BigDecimal transport       = BigDecimal.ZERO;
    @Builder.Default @Column(name = "overtime",         precision = 12, scale = 2) private BigDecimal overtime        = BigDecimal.ZERO;
    @Builder.Default @Column(name = "other_allowances", precision = 12, scale = 2) private BigDecimal otherAllowances = BigDecimal.ZERO;
    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)     private BigDecimal grossSalary;
    @Builder.Default @Column(name = "income_tax",       precision = 12, scale = 2) private BigDecimal incomeTax       = BigDecimal.ZERO;
    @Builder.Default @Column(name = "provident_fund",   precision = 12, scale = 2) private BigDecimal providentFund   = BigDecimal.ZERO;
    @Builder.Default @Column(name = "loan_deduction",   precision = 12, scale = 2) private BigDecimal loanDeduction   = BigDecimal.ZERO;
    @Builder.Default @Column(name = "other_deductions", precision = 12, scale = 2) private BigDecimal otherDeductions = BigDecimal.ZERO;
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)       private BigDecimal netSalary;
    @Column(name = "working_days") private Integer workingDays;
    @Column(name = "leave_days")   private Integer leaveDays;
    @Column(name = "absent_days")  private Integer absentDays;
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING"; // PENDING|PAID|FAILED
}

// ════════════════════════════════════════════════════════════════════════════
// FINANCE — OPENING BALANCE
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/accounts/setup/OpeningBalance.java
package com.asg.spindleserp.accounts.setup;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "acc_opening_balances",
    uniqueConstraints = @UniqueConstraint(name = "uk_ob_org_acc_yr",
        columnNames = {"organization_id","account_id","fiscal_year"}),
    indexes = {
        @Index(name = "idx_ob_org",     columnList = "organization_id"),
        @Index(name = "idx_ob_account", columnList = "account_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OpeningBalance implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "fiscal_year", nullable = false) private Integer fiscalYear;  // e.g. 2025

    @Builder.Default @Column(name = "opening_debit_balance",  nullable = false, precision = 18, scale = 2) private BigDecimal openingDebitBalance  = BigDecimal.ZERO;
    @Builder.Default @Column(name = "opening_credit_balance", nullable = false, precision = 18, scale = 2) private BigDecimal openingCreditBalance = BigDecimal.ZERO;

    @Builder.Default @Column(name = "is_posted", nullable = false) private Boolean isPosted     = false;
    @Column(name = "balance_type",  length = 20)  private String    balanceType;   // ASSET|LIABILITY|...
    @Column(name = "posted_date")                 private LocalDate postedDate;
    @Column(name = "posted_by",     length = 100) private String    postedBy;
    @Column(length = 1000)                        private String    remarks;
    @Column(name = "created_by",    length = 100) private String    createdBy;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// ════════════════════════════════════════════════════════════════════════════
// INVENTORY TRANSACTION  (immutable ledger)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/global/documents/InventoryTransaction.java
package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * InventoryTransaction — immutable append-only ledger row.
 * One row per stock movement. Never updated or deleted.
 * balanceAfter captures the running stock after this transaction.
 */
@Entity
@Table(name = "global_inventory_transactions",
    indexes = {
        @Index(name = "idx_txn_org",  columnList = "organization_id"),
        @Index(name = "idx_txn_doc",  columnList = "business_document_id"),
        @Index(name = "idx_txn_item", columnList = "item_id"),
        @Index(name = "idx_txn_wh",   columnList = "warehouse_id"),
        @Index(name = "idx_txn_lot",  columnList = "lot_id"),
        @Index(name = "idx_txn_mvt",  columnList = "movement_type"),
        @Index(name = "idx_txn_date", columnList = "transaction_date"),
        @Index(name = "idx_txn_org_date", columnList = "organization_id,transaction_date")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_document_id")
    private BusinessDocument businessDocument;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private GlobalInventoryLot lot;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    // Quantities
    @Column(nullable = false, precision = 18, scale = 3)              private BigDecimal quantity;
    @Column(name = "bale_quantity", precision = 18, scale = 3)        private BigDecimal baleQuantity;
    @Column(precision = 18, scale = 3)                                private BigDecimal bags;
    @Column(name = "bag_quantity")                                    private Integer    bagQuantity;
    @Column(name = "cones_per_bag")                                   private Integer    conesPerBag;
    @Column(precision = 18, scale = 3)                                private BigDecimal cones;
    @Column(name = "actual_weight", precision = 12, scale = 3)        private BigDecimal actualWeight;
    @Column(name = "net_weight",    precision = 12, scale = 3)        private BigDecimal netWeight;

    // Costing
    @Column(name = "unit_cost",   precision = 18, scale = 4) private BigDecimal unitCost;
    @Column(name = "total_cost",  precision = 18, scale = 2) private BigDecimal totalCost;

    /** Stock level AFTER this transaction — enables point-in-time queries */
    @Column(name = "balance_after", precision = 18, scale = 3) private BigDecimal balanceAfter;

    @Column(name = "transaction_date") private LocalDate transactionDate;
    @Column(columnDefinition = "TEXT") private String remarks;
    @Column(name = "created_by",  length = 100) private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

// ════════════════════════════════════════════════════════════════════════════
// FIXED ASSETS — DEPRECIATION & DISPOSAL
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/fixedassets/DepreciationRun.java
package com.asg.spindleserp.fixedassets;

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
@Table(name = "fa_depreciation_runs",
    indexes = @Index(name = "idx_dep_run_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepreciationRun extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(name = "run_date",     nullable = false) private LocalDate runDate;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end",   nullable = false) private LocalDate periodEnd;
    @Column(name = "run_type",     nullable = false, length = 20)
    @Builder.Default private String runType = "MONTHLY"; // MONTHLY|ANNUAL|ADHOC

    @Column(nullable = false, length = 20)
    @Builder.Default private String status = "DRAFT"; // DRAFT|POSTED|REVERSED

    @Builder.Default @Column(name = "total_assets")       private Integer    totalAssets       = 0;
    @Builder.Default @Column(name = "total_depreciation", precision = 18, scale = 2) private BigDecimal totalDepreciation = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(name = "posted_by",  length = 100) private String postedBy;
    @Column(name = "posted_at")                private LocalDateTime postedAt;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @OneToMany(mappedBy = "depreciationRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DepreciationRunLine> lines = new ArrayList<>();
}

// FILE: com/asg/spindleserp/fixedassets/DepreciationRunLine.java
package com.asg.spindleserp.fixedassets;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "fa_depreciation_run_lines",
    indexes = {
        @Index(name = "idx_dep_line_run",   columnList = "depreciation_run_id"),
        @Index(name = "idx_dep_line_asset", columnList = "asset_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepreciationRunLine implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depreciation_run_id", nullable = false)
    private DepreciationRun depreciationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(name = "depreciation_method", nullable = false, length = 30) private String depreciation_method;
    @Column(name = "opening_book_value",  nullable = false, precision = 18, scale = 2) private BigDecimal openingBookValue;
    @Column(name = "depreciation_amount", nullable = false, precision = 18, scale = 2) private BigDecimal depreciationAmount;
    @Column(name = "closing_book_value",  nullable = false, precision = 18, scale = 2) private BigDecimal closingBookValue;
    @Column(name = "rate_applied",        precision = 5,  scale = 2)                  private BigDecimal rateApplied;
    @Column(name = "units_produced",      precision = 14, scale = 3)                  private BigDecimal unitsProduced;
    @Column(columnDefinition = "TEXT")                                                 private String notes;
}

// FILE: com/asg/spindleserp/fixedassets/AssetDisposal.java
package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.hrm.setup.Department;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fa_asset_disposals",
    indexes = @Index(name = "idx_fadis_asset", columnList = "asset_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetDisposal extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(name = "disposal_date", nullable = false) private LocalDate disposalDate;
    @Column(name = "disposal_type", nullable = false, length = 30)
    private String disposalType; // SALE|WRITE_OFF|TRANSFER|SCRAP

    @Builder.Default @Column(name = "disposal_value",          precision = 18, scale = 2) private BigDecimal disposalValue         = BigDecimal.ZERO;
    @Column(name = "book_value_at_disposal",  nullable = false, precision = 18, scale = 2) private BigDecimal bookValueAtDisposal;
    @Column(name = "accumulated_dep_at_disp", nullable = false, precision = 18, scale = 2) private BigDecimal accumulatedDepAtDisp;
    @Column(name = "gain_loss",               precision = 18, scale = 2)                  private BigDecimal gainLoss;  // +ve = gain

    @Column(name = "buyer_name", length = 200) private String buyerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_dept_id")
    private Department transferToDept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_to_employee_id")
    private Employee transferToEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(columnDefinition = "TEXT") private String reason;
    @Column(name = "approved_by",  length = 100) private String approvedBy;
    @Column(name = "approved_at")                private LocalDateTime approvedAt;
    @Column(name = "created_by",   length = 100) private String createdBy;
}

// FILE: com/asg/spindleserp/fixedassets/AssetTransfer.java
package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.hrm.pims.Employee;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.inventory.setup.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fa_asset_transfers",
    indexes = @Index(name = "idx_fatrf_asset", columnList = "asset_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetTransfer extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(name = "transfer_date", nullable = false) private LocalDate transferDate;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_department_id")  private Department fromDepartment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "to_department_id")    private Department toDepartment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_cost_center_id") private CostCenter fromCostCenter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "to_cost_center_id")   private CostCenter toCostCenter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_warehouse_id")   private Warehouse  fromWarehouse;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "to_warehouse_id")     private Warehouse  toWarehouse;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_employee_id")    private Employee   fromEmployee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "to_employee_id")      private Employee   toEmployee;

    @Column(columnDefinition = "TEXT") private String reason;
    @Column(name = "approved_by", length = 100) private String approvedBy;
    @Column(name = "approved_at")               private LocalDateTime approvedAt;
    @Column(name = "created_by",  length = 100) private String createdBy;
}

// ════════════════════════════════════════════════════════════════════════════
// CRM — QUOTATION
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/crm/CrmQuotation.java
package com.asg.spindleserp.crm;

import com.asg.spindleserp.global.documents.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_quotations",
    indexes = @Index(name = "idx_quot_opp", columnList = "opportunity_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrmQuotation implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_quotation_id")
    private BusinessDocument salesQuotation;

    @Column(name = "quoted_value", precision = 18, scale = 2) private BigDecimal quotedValue;
    @Column(name = "valid_until") private LocalDate validUntil;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "DRAFT";
    // DRAFT|SENT|ACCEPTED|REJECTED|REVISED
    @Builder.Default @Column(name = "revision_number") private Integer revisionNumber = 1;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "created_by", length = 100) private String createdBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}

// ════════════════════════════════════════════════════════════════════════════
// NOTIFICATIONS — EMAIL QUEUE
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/notification/EmailQueue.java
package com.asg.spindleserp.notification;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "ntf_email_queue",
    indexes = @Index(name = "idx_email_status", columnList = "status,scheduled_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailQueue implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "to_email",  nullable = false, length = 200) private String toEmail;
    @Column(name = "to_name",   length = 200)                   private String toName;
    @Column(name = "cc_emails", columnDefinition = "TEXT")       private String ccEmails;
    @Column(name = "bcc_emails", columnDefinition = "TEXT")      private String bccEmails;
    @Column(nullable = false, length = 500)                      private String subject;
    @Column(name = "body_html", columnDefinition = "TEXT")        private String bodyHtml;
    @Column(name = "body_text", columnDefinition = "TEXT")        private String bodyText;
    @Column(name = "template_name", length = 100)                 private String templateName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_params", columnDefinition = "jsonb")
    private Map<String, Object> templateParams;

    @Column(name = "reference_type", length = 50) private String referenceType;
    @Column(name = "reference_id")                private Long   referenceId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING|SENT|FAILED|CANCELLED

    @Builder.Default @Column(nullable = false)   private Integer  attempts       = 0;
    @Column(name = "last_attempt_at")             private LocalDateTime lastAttemptAt;
    @Column(name = "sent_at")                     private LocalDateTime sentAt;
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @Column(name = "scheduled_at")
    @Builder.Default
    private LocalDateTime scheduledAt = LocalDateTime.now();

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}

// ════════════════════════════════════════════════════════════════════════════
// eCOMMERCE — ALL REMAINING ENTITIES
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/ecommerce/EcoTaxClass.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "eco_tax_classes",
    uniqueConstraints = @UniqueConstraint(name = "uk_tax_cls", columnNames = {"store_id","name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoTaxClass implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 100) private String name;
    @Builder.Default @Column(nullable = false, precision = 5, scale = 2) private BigDecimal rate = BigDecimal.ZERO;
    @Builder.Default @Column(name = "is_default", nullable = false) private Boolean isDefault = false;
}

// FILE: com/asg/spindleserp/ecommerce/EcoCategory.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eco_categories",
    uniqueConstraints = @UniqueConstraint(name = "uk_ecat_store_slug", columnNames = {"store_id","slug"}),
    indexes = {
        @Index(name = "idx_cat_store",  columnList = "store_id"),
        @Index(name = "idx_cat_parent", columnList = "parent_id"),
        @Index(name = "idx_cat_slug",   columnList = "store_id,slug")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoCategory extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private EcoCategory parent;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "name_bn", length = 200) private String nameBn;
    @Column(nullable = false, length = 200) private String slug;
    @Column(columnDefinition = "TEXT")      private String description;
    @Column(name = "image_url",   length = 500) private String imageUrl;
    @Column(name = "banner_url",  length = 500) private String bannerUrl;
    @Column(length = 100)                       private String icon;
    @Builder.Default @Column(name = "display_order",  nullable = false) private Integer displayOrder = 0;
    @Column(name = "meta_title", length = 300)  private String metaTitle;
    @Column(name = "meta_description", columnDefinition = "TEXT") private String metaDescription;
    @Builder.Default @Column(name = "is_active",   nullable = false) private Boolean isActive   = true;
    @Builder.Default @Column(name = "is_featured", nullable = false) private Boolean isFeatured = false;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoAttributeGroup.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_attribute_groups",
    uniqueConstraints = @UniqueConstraint(name = "uk_atg_store_slug", columnNames = {"store_id","slug"}),
    indexes = @Index(name = "idx_atg_store", columnList = "store_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoAttributeGroup implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 100) private String name;   // Colour | Count | Ply | Size
    @Column(name = "name_bn", length = 100) private String nameBn;
    @Column(nullable = false, length = 100) private String slug;
    @Builder.Default @Column(name = "is_visible",   nullable = false) private Boolean isVisible   = true;
    @Builder.Default @Column(name = "is_variation", nullable = false) private Boolean isVariation = true;
    @Builder.Default @Column(name = "display_order", nullable = false) private Integer displayOrder = 0;
    @OneToMany(mappedBy = "attributeGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EcoAttributeValue> values = new ArrayList<>();
}

// FILE: com/asg/spindleserp/ecommerce/EcoAttributeValue.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "eco_attribute_values",
    uniqueConstraints = @UniqueConstraint(name = "uk_atv_grp_slug", columnNames = {"attribute_group_id","slug"}),
    indexes = @Index(name = "idx_atv_group", columnList = "attribute_group_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoAttributeValue implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_group_id", nullable = false)
    private EcoAttributeGroup attributeGroup;
    @Column(nullable = false, length = 100) private String value;
    @Column(name = "value_bn", length = 100) private String valueBn;
    @Column(nullable = false, length = 100) private String slug;
    @Column(name = "color_hex",  length = 7)   private String colorHex;   // #FF0000 for colour swatches
    @Column(name = "image_url",  length = 500)  private String imageUrl;   // for image swatches
    @Builder.Default @Column(name = "display_order", nullable = false) private Integer displayOrder = 0;
}

// FILE: com/asg/spindleserp/ecommerce/EcoProductVariant.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_product_variants",
    uniqueConstraints = @UniqueConstraint(name = "uk_var_prod_sku", columnNames = {"product_id","sku"}),
    indexes = {
        @Index(name = "idx_var_product", columnList = "product_id"),
        @Index(name = "idx_var_item",    columnList = "inv_item_id"),
        @Index(name = "idx_var_sku",     columnList = "sku")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoProductVariant extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inv_item_id")
    private InventoryItem invItem;  // NULL = inherits from product's inv_item_id
    @Column(nullable = false, length = 100) private String sku;
    @Column(length = 100)                   private String barcode;
    // NULL values inherit from product
    @Column(precision = 18, scale = 4) private BigDecimal price;
    @Column(name = "sale_price",  precision = 18, scale = 4) private BigDecimal salePrice;
    @Column(name = "cost_price",  precision = 18, scale = 4) private BigDecimal costPrice;
    // Shipping
    @Column(name = "weight_kg", precision = 8, scale = 3) private BigDecimal weightKg;
    @Column(name = "length_cm", precision = 8, scale = 2) private BigDecimal lengthCm;
    @Column(name = "width_cm",  precision = 8, scale = 2) private BigDecimal widthCm;
    @Column(name = "height_cm", precision = 8, scale = 2) private BigDecimal heightCm;
    @Builder.Default @Column(name = "track_inventory", nullable = false) private Boolean trackInventory = true;
    @Builder.Default @Column(name = "allow_backorder", nullable = false) private Boolean allowBackorder = false;
    @Column(name = "image_url",     length = 500) private String imageUrl;
    @Builder.Default @Column(name = "is_active",    nullable = false) private Boolean isActive    = true;
    @Builder.Default @Column(name = "display_order",nullable = false) private Integer displayOrder = 0;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
    @ManyToMany
    @JoinTable(name = "eco_variant_attribute_values",
        joinColumns        = @JoinColumn(name = "variant_id"),
        inverseJoinColumns = @JoinColumn(name = "attribute_value_id"))
    @Builder.Default
    private List<EcoAttributeValue> attributeValues = new ArrayList<>();
}

// FILE: com/asg/spindleserp/ecommerce/EcoProductImage.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_product_images",
    indexes = @Index(name = "idx_img_product", columnList = "product_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoProductImage implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @Column(name = "image_url", nullable = false, length = 500) private String imageUrl;
    @Column(name = "alt_text",  length = 300)                   private String altText;
    @Builder.Default @Column(name = "is_primary",    nullable = false) private Boolean isPrimary    = false;
    @Builder.Default @Column(name = "display_order", nullable = false) private Integer displayOrder = 0;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoCoupon.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "eco_coupons",
    uniqueConstraints = @UniqueConstraint(name = "uk_coup_store_code", columnNames = {"store_id","code"}),
    indexes = @Index(name = "idx_coup_store", columnList = "store_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoCoupon extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 50) private String code;
    @Column(columnDefinition = "TEXT")     private String description;
    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType; // PERCENTAGE|FIXED_AMOUNT|FREE_SHIPPING|BUY_X_GET_Y
    @Column(name = "discount_value",       nullable = false, precision = 18, scale = 2) private BigDecimal discountValue;
    @Builder.Default @Column(name = "min_order_amount", precision = 18, scale = 2) private BigDecimal minOrderAmount = BigDecimal.ZERO;
    @Column(name = "max_discount_amount",  precision = 18, scale = 2) private BigDecimal maxDiscountAmount;
    @Column(name = "usage_limit_total")    private Integer usageLimitTotal;
    @Builder.Default @Column(name = "usage_limit_per_user") private Integer usageLimitPerUser = 1;
    @Builder.Default @Column(name = "used_count", nullable = false) private Integer usedCount = 0;
    @Column(name = "valid_from") private LocalDate validFrom;
    @Column(name = "valid_to")   private LocalDate validTo;
    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoCustomer.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.setup.SubAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "eco_customers",
    uniqueConstraints = @UniqueConstraint(name = "uk_ecust_store_email", columnNames = {"store_id","email"}),
    indexes = {
        @Index(name = "idx_ecust_store", columnList = "store_id"),
        @Index(name = "idx_ecust_email", columnList = "store_id,email"),
        @Index(name = "idx_ecust_sub",   columnList = "sub_account_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoCustomer extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_account_id", unique = true)
    private SubAccount subAccount;         // AR ledger link (created on first confirmed order)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name",  nullable = false, length = 100) private String lastName;
    @Column(nullable = false, length = 150)                      private String email;
    @Column(length = 20) private String phone;
    @Column(name = "date_of_birth") private LocalDate dateOfBirth;
    @Column(length = 10) private String gender;
    @Column(name = "customer_type", nullable = false, length = 20)
    @Builder.Default private String customerType = "REGISTERED"; // GUEST|REGISTERED|B2B|WHOLESALE
    @Column(name = "preferred_currency", length = 3) @Builder.Default private String preferredCurrency = "BDT";
    @Column(name = "preferred_language", length = 10) @Builder.Default private String preferredLanguage = "en";
    @Builder.Default @Column(name = "email_marketing", nullable = false) private Boolean emailMarketing = true;
    @Builder.Default @Column(name = "sms_marketing",   nullable = false) private Boolean smsMarketing   = true;
    @Builder.Default @Column(name = "total_orders", nullable = false)    private Integer totalOrders     = 0;
    @Builder.Default @Column(name = "total_spent",  nullable = false, precision = 18, scale = 2) private BigDecimal totalSpent = BigDecimal.ZERO;
    @Column(name = "last_order_date") private LocalDate lastOrderDate;
    @Column(name = "company_name", length = 200)  private String companyName;
    @Column(name = "tax_id",       length = 50)   private String taxId;
    @Column(name = "credit_limit", precision = 18, scale = 2) private BigDecimal creditLimit;
    @Column(name = "payment_terms", length = 50)  private String paymentTerms;
    @Builder.Default @Column(name = "is_active",         nullable = false) private Boolean isActive        = true;
    @Builder.Default @Column(name = "is_email_verified", nullable = false) private Boolean isEmailVerified = false;
    @Column(name = "email_verified_at") private java.time.LocalDateTime emailVerifiedAt;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoCustomerAddress.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_customer_addresses",
    indexes = @Index(name = "idx_eaddr_cust", columnList = "customer_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoCustomerAddress implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @Column(name = "address_type", nullable = false, length = 20)
    @Builder.Default private String addressType = "SHIPPING"; // SHIPPING|BILLING|BOTH
    @Builder.Default @Column(name = "is_default", nullable = false) private Boolean isDefault = false;
    @Column(name = "first_name",    length = 100) private String firstName;
    @Column(name = "last_name",     length = 100) private String lastName;
    @Column(length = 200)                         private String company;
    @Column(name = "address_line1", nullable = false, length = 300) private String addressLine1;
    @Column(name = "address_line2", length = 300) private String addressLine2;
    @Column(nullable = false, length = 100)       private String city;
    @Column(length = 100) private String district;
    @Column(length = 100) private String state;
    @Column(name = "country_code", nullable = false, length = 2) @Builder.Default private String countryCode = "BD";
    @Column(name = "postal_code",  length = 20)  private String postalCode;
    @Column(length = 20)                         private String phone;
    @Column(name = "delivery_notes", columnDefinition = "TEXT") private String deliveryNotes;
    @Column(precision = 10, scale = 7) private BigDecimal latitude;
    @Column(precision = 10, scale = 7) private BigDecimal longitude;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoWishlist.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_wishlists",
    uniqueConstraints = @UniqueConstraint(name = "uk_wish_cust_prod_var",
        columnNames = {"customer_id","product_id","variant_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoWishlist implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @CreationTimestamp @Column(name = "added_at", updatable = false) private LocalDateTime addedAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoRecentlyViewed.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity @Table(name = "eco_recently_viewed")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoRecentlyViewed implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @CreationTimestamp @Column(name = "viewed_at", updatable = false) private LocalDateTime viewedAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoCart.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_carts",
    indexes = {
        @Index(name = "idx_cart_cust",   columnList = "customer_id"),
        @Index(name = "idx_cart_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoCart extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcoCustomer customer;
    @Column(name = "session_token", length = 100) private String sessionToken;
    @Column(nullable = false, length = 20)
    @Builder.Default private String status = "ACTIVE"; // ACTIVE|ABANDONED|CONVERTED|MERGED
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coupon_id") private EcoCoupon coupon;
    @Column(name = "coupon_code", length = 50) private String couponCode;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal subtotal        = BigDecimal.ZERO;
    @Builder.Default @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "tax_amount",      nullable = false, precision = 18, scale = 2) private BigDecimal taxAmount      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "shipping_amount", nullable = false, precision = 18, scale = 2) private BigDecimal shippingAmount = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @Column(length = 3) @Builder.Default private String currency = "BDT";
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "expires_at")   private LocalDateTime expiresAt;
    @Column(name = "converted_at") private LocalDateTime convertedAt;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcoCartItem> items = new ArrayList<>();
}

// FILE: com/asg/spindleserp/ecommerce/EcoCartItem.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_cart_items",
    indexes = @Index(name = "idx_cart_item_cart", columnList = "cart_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoCartItem implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private EcoCart cart;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @Column(nullable = false, precision = 12, scale = 3)                    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4) private BigDecimal unitPrice;   // snapshot at add time
    @Column(name = "line_total", nullable = false, precision = 18, scale = 2) private BigDecimal lineTotal;
    @Builder.Default @Column(name = "discount_amount", precision = 18, scale = 2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "tax_amount",      precision = 18, scale = 2) private BigDecimal taxAmount      = BigDecimal.ZERO;
    @CreationTimestamp @Column(name = "added_at",   updatable = false) private LocalDateTime addedAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoOrderLine.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "eco_order_lines",
    uniqueConstraints = @UniqueConstraint(name = "uk_oline_bdl", columnNames = {"business_document_line_id"}),
    indexes = {
        @Index(name = "idx_oline_order", columnList = "eco_order_id"),
        @Index(name = "idx_oline_prod",  columnList = "product_id"),
        @Index(name = "idx_oline_var",   columnList = "variant_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoOrderLine implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_line_id", nullable = false, unique = true)
    private BusinessDocumentLine businessDocumentLine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @Column(name = "product_name", nullable = false, length = 300) private String productName;
    @Column(name = "variant_sku",  length = 100)                   private String variantSku;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variant_attributes", columnDefinition = "jsonb")
    private Map<String, String> variantAttributes;  // {"Colour":"White","Count":"30s"}
    @Column(name = "product_image_url", length = 500) private String productImageUrl;
    @Column(nullable = false, precision = 12, scale = 3)                             private BigDecimal quantity;
    @Column(name = "unit_price",   nullable = false, precision = 18, scale = 4)      private BigDecimal unitPrice;
    @Builder.Default @Column(name = "discount_amount", precision = 18, scale = 2)   private BigDecimal discountAmount   = BigDecimal.ZERO;
    @Builder.Default @Column(name = "tax_amount",      precision = 18, scale = 2)   private BigDecimal taxAmount        = BigDecimal.ZERO;
    @Column(name = "line_total",   nullable = false, precision = 18, scale = 2)      private BigDecimal lineTotal;
    @Builder.Default @Column(name = "fulfilled_qty",   precision = 12, scale = 3)   private BigDecimal fulfilledQty     = BigDecimal.ZERO;
    @Builder.Default @Column(name = "returned_qty",    precision = 12, scale = 3)   private BigDecimal returnedQty      = BigDecimal.ZERO;
    @Builder.Default @Column(name = "refunded_amount", precision = 18, scale = 2)   private BigDecimal refundedAmount   = BigDecimal.ZERO;
    @Builder.Default @Column(name = "is_returnable",   nullable = false)             private Boolean    isReturnable      = true;
    @Column(name = "return_deadline") private LocalDate returnDeadline;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoOrderStatusHistory.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_order_status_history",
    indexes = @Index(name = "idx_osh_order", columnList = "eco_order_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoOrderStatusHistory implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;
    @Column(name = "from_status", length = 30) private String fromStatus;
    @Column(name = "to_status",   nullable = false, length = 30) private String toStatus;
    @Column(columnDefinition = "TEXT") private String comment;
    @Builder.Default @Column(name = "notify_customer", nullable = false) private Boolean notifyCustomer = false;
    @Column(name = "changed_by", length = 100) private String changedBy;
    @CreationTimestamp @Column(name = "changed_at", updatable = false) private LocalDateTime changedAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoCouponUsage.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_coupon_usages",
    uniqueConstraints = @UniqueConstraint(name = "uk_coup_usage", columnNames = {"coupon_id","eco_order_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoCouponUsage implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "coupon_id",   nullable = false) private EcoCoupon   coupon;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "eco_order_id", nullable = false) private EcoOrder   ecoOrder;
    @ManyToOne(fetch = FetchType.LAZY)                   @JoinColumn(name = "customer_id")                   private EcoCustomer customer;
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2) private BigDecimal discountAmount;
    @CreationTimestamp @Column(name = "used_at", updatable = false) private LocalDateTime usedAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoPaymentMethod.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "eco_payment_methods",
    uniqueConstraints = @UniqueConstraint(name = "uk_pm_store_code", columnNames = {"store_id","code"}),
    indexes = @Index(name = "idx_pm_store", columnList = "store_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoPaymentMethod extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 50)  private String code;
    @Column(name = "payment_type", nullable = false, length = 30)
    private String paymentType; // CASH_ON_DELIVERY|BKASH|NAGAD|ROCKET|BANK_TRANSFER|CARD|SSL_COMMERZ|STRIPE|PAYPAL|EMI
    @Column(length = 500) private String description;
    @Column(columnDefinition = "TEXT") private String instructions;
    @Column(name = "logo_url", length = 300) private String logoUrl;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_config", columnDefinition = "jsonb")
    private Map<String, Object> gatewayConfig;
    @Column(name = "min_order_amount", precision = 18, scale = 2) private BigDecimal minOrderAmount;
    @Column(name = "max_order_amount", precision = 18, scale = 2) private BigDecimal maxOrderAmount;
    @Column(name = "extra_charge_type", length = 20) private String extraChargeType; // FIXED|PERCENTAGE|NONE
    @Builder.Default @Column(name = "extra_charge_value", precision = 18, scale = 2) private BigDecimal extraChargeValue = BigDecimal.ZERO;
    @Builder.Default @Column(name = "display_order",  nullable = false) private Integer displayOrder = 0;
    @Builder.Default @Column(name = "is_active",      nullable = false) private Boolean isActive     = true;
    @Builder.Default @Column(name = "is_test_mode",   nullable = false) private Boolean isTestMode   = false;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoRefund.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_refunds",
    indexes = @Index(name = "idx_refund_order", columnList = "eco_order_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoRefund extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "eco_order_id",             nullable = false) private EcoOrder             ecoOrder;
    @ManyToOne(fetch = FetchType.LAZY)                   @JoinColumn(name = "original_transaction_id")                   private EcoPaymentTransaction originalTransaction;
    @ManyToOne(fetch = FetchType.LAZY)                   @JoinColumn(name = "refund_transaction_id")                     private EcoPaymentTransaction refundTransaction;
    @ManyToOne(fetch = FetchType.LAZY)                   @JoinColumn(name = "journal_entry_id")                          private JournalEntry          journalEntry;
    @Column(name = "refund_reason", nullable = false, length = 30)
    private String refundReason; // CUSTOMER_REQUEST|DEFECTIVE|WRONG_ITEM|OUT_OF_STOCK|FRAUD|DUPLICATE|OTHER
    @Column(name = "reason_detail", columnDefinition = "TEXT") private String reasonDetail;
    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2) private BigDecimal refundAmount;
    @Column(name = "refund_method", nullable = false, length = 30)
    private String refundMethod; // ORIGINAL_METHOD|WALLET|BANK_TRANSFER|STORE_CREDIT|CASH
    @Column(nullable = false, length = 20) @Builder.Default private String status = "PENDING";
    // PENDING|APPROVED|PROCESSING|COMPLETED|REJECTED
    @Column(name = "requested_by", length = 100) private String requestedBy;
    @Column(name = "approved_by",  length = 100) private String approvedBy;
    @Column(name = "approved_at")                private LocalDateTime approvedAt;
    @Column(name = "processed_at")               private LocalDateTime processedAt;
    @Column(name = "created_by",   length = 100) private String createdBy;
    @Column(name = "updated_by",   length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoStoreCredit.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "eco_store_credits",
    indexes = @Index(name = "idx_credit_cust", columnList = "customer_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoStoreCredit extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "store_id",    nullable = false) private EcoStore    store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id", nullable = false) private EcoCustomer customer;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // CREDIT|DEBIT|EXPIRE
    @Column(length = 200) private String reason;
    @Column(name = "expires_at") private LocalDate expiresAt;
    @Column(name = "reference_type", length = 50) private String referenceType;
    @Column(name = "reference_id")                private Long   referenceId;
    @Column(name = "created_by", length = 100)    private String createdBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoShippingZone.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "eco_shipping_zones",
    uniqueConstraints = @UniqueConstraint(name = "uk_sz_store_name", columnNames = {"store_id","name"}),
    indexes = @Index(name = "idx_sz_store", columnList = "store_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoShippingZone implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @Column(nullable = false, length = 200) private String name;
    @Column(columnDefinition = "TEXT") private String countries;
    @Column(columnDefinition = "TEXT") private String districts;
    @Column(name = "postal_codes", columnDefinition = "TEXT") private String postalCodes;
    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
}

// FILE: com/asg/spindleserp/ecommerce/EcoShippingMethod.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eco_shipping_methods",
    indexes = @Index(name = "idx_sm_store", columnList = "store_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoShippingMethod extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private EcoShippingZone zone;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 100)                   private String carrier; // Pathao|Steadfast|Sundarban|RedX|In-House
    @Column(name = "shipping_type", nullable = false, length = 30)
    @Builder.Default private String shippingType = "FLAT_RATE"; // FLAT_RATE|FREE|WEIGHT_BASED|PRICE_BASED|PER_ITEM
    @Builder.Default @Column(name = "base_cost",          nullable = false, precision = 18, scale = 2) private BigDecimal baseCost         = BigDecimal.ZERO;
    @Builder.Default @Column(name = "per_kg_cost",        precision = 18, scale = 2)                  private BigDecimal perKgCost         = BigDecimal.ZERO;
    @Column(name = "free_shipping_above", precision = 18, scale = 2) private BigDecimal freeShippingAbove;
    @Column(name = "min_order_amount",    precision = 18, scale = 2) private BigDecimal minOrderAmount;
    @Column(name = "max_weight_kg",       precision = 8,  scale = 2) private BigDecimal maxWeightKg;
    @Column(name = "estimated_days_min")  private Integer estimatedDaysMin;
    @Column(name = "estimated_days_max")  private Integer estimatedDaysMax;
    @Builder.Default @Column(name = "is_active",     nullable = false) private Boolean isActive     = true;
    @Builder.Default @Column(name = "display_order", nullable = false) private Integer displayOrder = 0;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoShipmentItem.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import com.asg.spindleserp.global.documents.InventoryTransaction;
import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_shipment_items",
    indexes = {
        @Index(name = "idx_sitem_ship",  columnList = "shipment_id"),
        @Index(name = "idx_sitem_order", columnList = "eco_order_line_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoShipmentItem implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private EcoShipment shipment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_line_id", nullable = false)
    private EcoOrderLine ecoOrderLine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_line_id", nullable = false)
    private BusinessDocumentLine businessDocumentLine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inv_item_id", nullable = false)
    private InventoryItem invItem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private GlobalInventoryLot inventoryLot;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    /** FK to the InventoryTransaction created when stock was deducted */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_transaction_id")
    private InventoryTransaction inventoryTransaction;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoShipmentTracking.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_shipment_tracking",
    indexes = @Index(name = "idx_strack_ship", columnList = "shipment_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoShipmentTracking implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private EcoShipment shipment;
    @Column(name = "event_status",   nullable = false, length = 50) private String eventStatus;
    @Column(name = "event_location", length = 200)                  private String eventLocation;
    @Column(columnDefinition = "TEXT")                              private String description;
    @Column(name = "event_time",     nullable = false)              private LocalDateTime eventTime;
    @Column(length = 20) @Builder.Default private String source = "SYSTEM"; // SYSTEM|COURIER_API|MANUAL
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoReturn.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.global.documents.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_returns",
    uniqueConstraints = @UniqueConstraint(name = "uk_return_doc", columnNames = {"business_document_id"}),
    indexes = {
        @Index(name = "idx_return_doc",    columnList = "business_document_id"),
        @Index(name = "idx_return_order",  columnList = "eco_order_id"),
        @Index(name = "idx_return_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoReturn extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_id", nullable = false, unique = true)
    private BusinessDocument businessDocument;  // documentType = ONLINE_RETURN
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @Column(name = "return_reason", nullable = false, length = 30)
    private String returnReason;
    // WRONG_ITEM|DEFECTIVE|DAMAGED_IN_TRANSIT|NOT_AS_DESCRIBED|CHANGED_MIND|QUALITY_ISSUE|LATE_DELIVERY
    @Column(name = "reason_detail", columnDefinition = "TEXT") private String reasonDetail;
    @Column(name = "resolution_type", nullable = false, length = 20)
    @Builder.Default private String resolutionType = "REFUND"; // REFUND|REPLACEMENT|STORE_CREDIT|EXCHANGE
    @Column(nullable = false, length = 30)
    @Builder.Default private String status = "REQUESTED";
    // REQUESTED|APPROVED|REJECTED|PICKED_UP|RECEIVED|INSPECTED|RESOLVED
    @Column(name = "pickup_address", columnDefinition = "TEXT")  private String pickupAddress;
    @Column(name = "courier_tracking_no", length = 200)           private String courierTrackingNo;
    @Column(name = "received_at")                                 private LocalDateTime receivedAt;
    @Column(name = "received_by", length = 100)                   private String receivedBy;
    @Column(name = "inspection_notes", columnDefinition = "TEXT") private String inspectionNotes;
    @Builder.Default @Column(name = "stock_posted", nullable = false) private Boolean stockPosted = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id")
    private EcoRefund refund;
    @Column(name = "resolved_at")              private LocalDateTime resolvedAt;
    @Column(name = "resolved_by", length = 100) private String resolvedBy;
    @Column(name = "customer_note",  columnDefinition = "TEXT") private String customerNote;
    @Column(name = "internal_note",  columnDefinition = "TEXT") private String internalNote;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
    @OneToMany(mappedBy = "ecoReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcoReturnItem> items = new ArrayList<>();
}

// FILE: com/asg/spindleserp/ecommerce/EcoReturnItem.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import com.asg.spindleserp.global.documents.InventoryTransaction;
import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_return_items",
    indexes = @Index(name = "idx_ritem_return", columnList = "eco_return_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoReturnItem implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_return_id", nullable = false)
    private EcoReturn ecoReturn;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_line_id", nullable = false)
    private EcoOrderLine ecoOrderLine;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_document_line_id")
    private BusinessDocumentLine businessDocumentLine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inv_item_id", nullable = false)
    private InventoryItem invItem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private GlobalInventoryLot inventoryLot;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(name = "return_condition", length = 20) @Builder.Default private String returnCondition = "GOOD";
    // GOOD|DAMAGED|USED|EXPIRED
    @Builder.Default @Column(name = "restock", nullable = false) private Boolean restock = true;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_transaction_id")
    private InventoryTransaction inventoryTransaction;  // set after stock is restored
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}

// FILE: com/asg/spindleserp/ecommerce/EcoReview.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eco_reviews",
    uniqueConstraints = @UniqueConstraint(name = "uk_review_prod_cust_order",
        columnNames = {"product_id","customer_id","eco_order_id"}),
    indexes = {
        @Index(name = "idx_review_prod",   columnList = "product_id"),
        @Index(name = "idx_review_cust",   columnList = "customer_id"),
        @Index(name = "idx_review_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoReview extends BaseOrgEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "store_id",   nullable = false) private EcoStore    store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private EcoProduct  product;
    @ManyToOne(fetch = FetchType.LAZY)                   @JoinColumn(name = "variant_id")                  private EcoProductVariant variant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id",nullable = false) private EcoCustomer customer;
    @ManyToOne(fetch = FetchType.LAZY)                   @JoinColumn(name = "eco_order_id")                private EcoOrder   ecoOrder;
    @Column(nullable = false)
    private Integer rating;  // 1–5  (enforced by CHECK constraint in DB)
    @Column(length = 200) private String title;
    @Column(columnDefinition = "TEXT") private String body;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "PENDING";
    // PENDING|APPROVED|REJECTED|SPAM
    @Builder.Default @Column(name = "is_verified_purchase", nullable = false) private Boolean isVerifiedPurchase = false;
    @Builder.Default @Column(name = "helpful_count",         nullable = false) private Integer helpfulCount       = 0;
    @Builder.Default @Column(name = "not_helpful_count",     nullable = false) private Integer notHelpfulCount    = 0;
    @Column(name = "admin_reply",       columnDefinition = "TEXT") private String adminReply;
    @Column(name = "admin_replied_at")                             private LocalDateTime adminRepliedAt;
    @Column(name = "admin_replied_by", length = 100)               private String adminRepliedBy;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// FILE: com/asg/spindleserp/ecommerce/EcoCustomerNotification.java
package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_customer_notifications",
    indexes = {
        @Index(name = "idx_ecnotif_cust",   columnList = "customer_id"),
        @Index(name = "idx_ecnotif_unsent", columnList = "is_sent,created_at")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcoCustomerNotification implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id",    nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcoCustomer customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eco_order_id")
    private EcoOrder ecoOrder;
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;
    // ORDER_CONFIRMED|PAYMENT_RECEIVED|ORDER_PACKED|ORDER_SHIPPED|OUT_FOR_DELIVERY|
    // DELIVERED|REFUND_PROCESSED|RETURN_APPROVED|CART_ABANDONMENT|PRICE_DROP|BACK_IN_STOCK
    @Column(nullable = false, length = 20)
    private String channel; // EMAIL|SMS|PUSH|WHATSAPP
    @Column(length = 300) private String title;
    @Column(columnDefinition = "TEXT") private String message;
    @Column(name = "template_name", length = 100) private String templateName;
    @Builder.Default @Column(name = "is_sent", nullable = false) private Boolean isSent = false;
    @Column(name = "sent_at")      private LocalDateTime sentAt;
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}

// ════════════════════════════════════════════════════════════════════════════
// NEW ENUM TYPES
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/hrm/pims/Gender.java
package com.asg.spindleserp.hrm.pims;
public enum Gender { MALE, FEMALE, OTHER }

// FILE: com/asg/spindleserp/hrm/pims/BloodGroup.java
package com.asg.spindleserp.hrm.pims;
public enum BloodGroup { A_POS, A_NEG, B_POS, B_NEG, O_POS, O_NEG, AB_POS, AB_NEG }
// DB: A+|A-|B+|B-|O+|O-|AB+|AB- via @Enumerated with custom converter or STRING

// FILE: com/asg/spindleserp/hrm/pims/MaritalStatus.java
package com.asg.spindleserp.hrm.pims;
public enum MaritalStatus { SINGLE, MARRIED, DIVORCED, WIDOWED }

// FILE: com/asg/spindleserp/hrm/pims/EmployeeType.java
package com.asg.spindleserp.hrm.pims;
public enum EmployeeType { PERMANENT, CONTRACT, PART_TIME, INTERN }

// FILE: com/asg/spindleserp/hrm/pims/EmployeeStatus.java
package com.asg.spindleserp.hrm.pims;
public enum EmployeeStatus { ACTIVE, PROBATION, RESIGNED, TERMINATED, ON_LEAVE, RETIRED }

// FILE: com/asg/spindleserp/hrm/pims/AddressType.java
package com.asg.spindleserp.hrm.pims;
public enum AddressType { PRESENT, PERMANENT }

// ════════════════════════════════════════════════════════════════════════════
// COMPLETE ENTITY REGISTRY  (both Part 1 + Part 2)
// ════════════════════════════════════════════════════════════════════════════
/*
  ┌──────────────────────────────────────────────────────────────────────────┐
  │  SpindleERP — All 115 Tables → Spring Boot Entities                     │
  ├─────────────────────────────┬────────────────┬────────────────────────── ┤
  │  Entity                     │  Table         │  Base Class               │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 1 — CORE/SECURITY   │                │                           │
  │  Organization               │  org_orgs      │  (none — tenant root)     │
  │  BusinessUnit               │  org_bu        │  BaseOrgEntity            │
  │  User                       │  sec_users     │  (none — cross-org)       │
  │  Role                       │  sec_roles     │  (none — global)          │
  │  Permission                 │  sec_perms     │  (none — global)          │
  │  Menu                       │  sec_menus     │  (none — global)          │
  │  RoleMenu                   │  sec_role_menus│  (none — join)            │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 2 — LOCATION        │                │                           │
  │  Country, State             │  stp_*         │  BaseReferenceEntity      │
  │  District, City             │  stp_*         │  BaseReferenceEntity      │
  │  Currency, HsCode           │  stp_*         │  BaseReferenceEntity      │
  │  DocumentSequence           │  stp_doc_seqs  │  (none — utility)         │
  │  DocumentFile               │  stp_doc_files │  (none — polymorphic)     │
  │  GlobalTermsCondition       │  stp_gtc       │  BaseOrgEntity            │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 3 — INVENTORY       │                │                           │
  │  ItemCategory               │  inv_item_cats │  BaseOrgEntity            │
  │  UnitsOfMeasure             │  stp_uom       │  BaseOrgEntity            │
  │  YarnType/Count/Ply/Blend   │  yrn_*         │  BaseOrgEntity            │
  │  InventoryItem              │  inv_items     │  BaseAuditEntity          │
  │  YarnItem                   │  yarn_items    │  BaseAuditEntity          │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 4 — WAREHOUSE/STOCK │                │                           │
  │  Warehouse                  │  org_wh        │  BaseOrgEntity            │
  │  GlobalInventoryLot         │  global_inv_lots│ BaseOrgEntity            │
  │  InventoryStockBalance      │  global_inv_sb │  (none — org FK explicit) │
  │  InventoryTransaction       │  global_inv_txn│  (none — immutable)       │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 5 — FINANCE         │                │                           │
  │  Bank                       │  stp_banks     │  BaseOrgEntity            │
  │  Account                    │  acc_coa       │  BaseOrgEntity            │
  │  SubAccount (base)          │  acc_coa_sub   │  BaseOrgEntity + JOINED   │
  │  BankAccount                │  acc_ba        │  SubAccount (JOINED child)│
  │  CashAccount                │  acc_ca        │  SubAccount (JOINED child)│
  │  Customer                   │  acc_customers │  SubAccount (JOINED child)│
  │  Supplier                   │  acc_suppliers │  SubAccount (JOINED child)│
  │  OpeningBalance             │  acc_ob        │  (none — org FK explicit) │
  │  CostCenter                 │  acc_cc        │  BaseOrgEntity            │
  │  JournalEntry               │  acc_je        │  BaseOrgEntity            │
  │  JournalEntryLine           │  acc_jel       │  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 6 — APPROVAL        │                │                           │
  │  ApprovalConfig             │  apr_configs   │  BaseOrgEntity            │
  │  ApprovalLevel              │  apr_levels    │  (none — child)           │
  │  ApprovalRequest            │  apr_requests  │  (none — polymorphic)     │
  │  ApprovalHistory            │  apr_histories │  (none — immutable)       │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 7 — DOCUMENTS(MDST) │                │                           │
  │  BusinessDocument           │  global_bd     │  BaseAuditEntity          │
  │  BusinessDocumentLine       │  global_bdl    │  BaseAuditEntity          │
  │  BusinessDocumentLineLot    │  global_bdll   │  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 8 — COMMERCIAL      │                │                           │
  │  CommercialLc               │  cmr_lc        │  BaseOrgEntity            │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 9 — HRM             │                │                           │
  │  Department                 │  org_depts     │  (manual @PrePersist)     │
  │  Designation                │  hrm_desig     │  (manual @PrePersist)     │
  │  Employee                   │  hrm_employees │  (manual @PrePersist)     │
  │  EmployeeAddress            │  hrm_emp_addr  │  (none — child)           │
  │  EmployeeDocument           │  hrm_emp_docs  │  (none — child)           │
  │  EmployeeSalary             │  hrm_emp_sal   │  (none — child)           │
  │  Attendance                 │  hrm_attend    │  BaseOrgEntity            │
  │  EmployeeLeave              │  hrm_emp_leave │  BaseOrgEntity            │
  │  PayrollRun                 │  hrm_pr        │  BaseOrgEntity            │
  │  PayrollRunLine             │  hrm_prl       │  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 10 — PRODUCTION     │                │                           │
  │  ProductionOrder            │  prd_orders    │  BaseOrgEntity            │
  │  ProductionRecipe           │  prd_recipes   │  BaseOrgEntity            │
  │  ProductionRecipeItem       │  prd_rec_items │  (none — child)           │
  │  ProductionRecipeItemLot    │  prd_ril       │  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 11 — FIXED ASSETS   │                │                           │
  │  AssetCategory              │  fa_asset_cats │  BaseOrgEntity            │
  │  FixedAsset                 │  fa_assets     │  BaseOrgEntity            │
  │  DepreciationRun            │  fa_dep_runs   │  BaseOrgEntity            │
  │  DepreciationRunLine        │  fa_dep_rl     │  (none — child)           │
  │  AssetDisposal              │  fa_disposals  │  BaseOrgEntity            │
  │  AssetTransfer              │  fa_transfers  │  BaseOrgEntity            │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 12 — CRM            │                │                           │
  │  Lead                       │  crm_leads     │  BaseOrgEntity            │
  │  Contact                    │  crm_contacts  │  BaseOrgEntity            │
  │  Opportunity                │  crm_opps      │  BaseOrgEntity            │
  │  Activity                   │  crm_acts      │  BaseOrgEntity            │
  │  CrmQuotation               │  crm_quotations│  (none — child)           │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 13 — NOTIF/AUDIT    │                │                           │
  │  Notification               │  ntf_notif     │  BaseOrgEntity            │
  │  EmailQueue                 │  ntf_email_q   │  (none — utility)         │
  │  AuditLog                   │  sys_audit_log │  (none — JSONB fields)    │
  ├─────────────────────────────┼────────────────┼────────────────────────── ┤
  │  MODULE 14 — eCOMMERCE      │                │                           │
  │  EcoStore                   │  eco_stores    │  BaseOrgEntity            │
  │  EcoTaxClass                │  eco_tax_cls   │  (none — store child)     │
  │  EcoCategory                │  eco_categories│  BaseOrgEntity            │
  │  EcoAttributeGroup          │  eco_attr_grps │  (none — store child)     │
  │  EcoAttributeValue          │  eco_attr_vals │  (none — group child)     │
  │  EcoCoupon                  │  eco_coupons   │  BaseOrgEntity            │
  │  EcoProduct                 │  eco_products  │  BaseOrgEntity            │
  │  EcoProductVariant          │  eco_prod_vars │  BaseOrgEntity            │
  │  EcoProductImage            │  eco_prod_imgs │  (none — product child)   │
  │  EcoCustomer                │  eco_customers │  BaseOrgEntity            │
  │  EcoCustomerAddress         │  eco_cust_addr │  (none — customer child)  │
  │  EcoWishlist                │  eco_wishlists │  (none — customer child)  │
  │  EcoRecentlyViewed          │  eco_rv        │  (none — customer child)  │
  │  EcoCart                    │  eco_carts     │  BaseOrgEntity            │
  │  EcoCartItem                │  eco_cart_items│  (none — cart child)      │
  │  EcoOrder                   │  eco_orders    │  BaseOrgEntity            │
  │  EcoOrderLine               │  eco_order_lns │  (none — order child)     │
  │  EcoOrderStatusHistory      │  eco_ord_hist  │  (none — order child)     │
  │  EcoCouponUsage             │  eco_coup_use  │  (none — order child)     │
  │  EcoPaymentMethod           │  eco_pay_mths  │  BaseOrgEntity            │
  │  EcoPaymentTransaction      │  eco_pay_txn   │  BaseOrgEntity            │
  │  EcoRefund                  │  eco_refunds   │  BaseOrgEntity            │
  │  EcoStoreCredit             │  eco_credits   │  BaseOrgEntity            │
  │  EcoShippingZone            │  eco_ship_zones│  (none — store child)     │
  │  EcoShippingMethod          │  eco_ship_mths │  BaseOrgEntity            │
  │  EcoShipment                │  eco_shipments │  BaseOrgEntity            │
  │  EcoShipmentItem            │  eco_ship_itms │  (none — shipment child)  │
  │  EcoShipmentTracking        │  eco_ship_track│  (none — shipment child)  │
  │  EcoReturn                  │  eco_returns   │  BaseOrgEntity            │
  │  EcoReturnItem              │  eco_ret_items │  (none — return child)    │
  │  EcoReview                  │  eco_reviews   │  BaseOrgEntity            │
  │  EcoCustomerNotification    │  eco_cust_notif│  (none — customer child)  │
  └─────────────────────────────┴────────────────┴────────────────────────── ┘

  TOTAL ENTITY CLASSES  : 115  (matches SQL schema exactly)
  TOTAL ENUM TYPES      :  15
  TOTAL PACKAGES        :  22
  BASE CLASS SUMMARY    :
    BaseAuditEntity     :   4  (Spring Data auditing — most transactional entities)
    BaseOrgEntity       :  43  (Hibernate timestamps + org enforcement via @PrePersist)
    BaseReferenceEntity :   6  (global reference tables — no org column)
    SubAccount JOINED   :   4  (BankAccount, CashAccount, Customer, Supplier)
    Plain entity        :  58  (no base — explicit org FK, child entities, or immutable)
*/
