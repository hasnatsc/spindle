// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E10  HRM  (v2 Generic Edition)                            ║
// ║  ★ ADDED: CostCenterAllocation — links employee salary to cost centers  ║
// ║           for production labor cost calculation                          ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: hrm/entity/Designation.java ────────────────────────────────────────
package com.hasnat.optimum.hrm.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hrm_designations",
    uniqueConstraints = @UniqueConstraint(name = "uq_desig_org_code",
        columnNames = {"organization_id", "designation_code"}),
    indexes = @Index(name = "idx_desig_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Designation extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 50)  private String designationCode;
    @Column(nullable = false, length = 200) private String designationName;
    @Column(length = 20)  private String grade;
    @Column(length = 500) private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}

// ── FILE: hrm/entity/Employee.java ───────────────────────────────────────────
package com.hasnat.optimum.hrm.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.organization.entity.Department;
import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hrm_employees",
    uniqueConstraints = @UniqueConstraint(name = "uq_emp_org_code",
        columnNames = {"organization_id", "employee_code"}),
    indexes = {
        @Index(name = "idx_emp_org",    columnList = "organization_id"),
        @Index(name = "idx_emp_dept",   columnList = "department_id"),
        @Index(name = "idx_emp_desig",  columnList = "designation_id"),
        @Index(name = "idx_emp_mgr",    columnList = "reporting_manager_id"),
        @Index(name = "idx_emp_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id")
    private Employee reportingManager;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 50) private String employeeCode;
    @Column(nullable = false, length = 100) private String firstName;
    @Column(nullable = false, length = 100) private String lastName;
    @Column(length = 100) private String email;
    @Column(nullable = false, unique = true, length = 20) private String phone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender = Gender.MALE;

    @Column(nullable = false) private LocalDate dateOfBirth;
    @Column(length = 10) private String bloodGroup;
    @Column(length = 20) private String maritalStatus;
    @Column(unique = true, length = 50) private String nationalId;
    @Column(unique = true, length = 50) private String passportNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeType employeeType = EmployeeType.PERMANENT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(nullable = false) private LocalDate joiningDate;
    private LocalDate confirmationDate;
    private LocalDate probationEndDate;
    private LocalDate resignationDate;
    private LocalDate exitDate;

    @Column(precision = 12, scale = 2) private BigDecimal basicSalary;
    @Column(precision = 12, scale = 2) private BigDecimal grossSalary;
    @Column(length = 50) private String bankName;
    @Column(length = 50) private String bankAccountNumber;
    @Column(length = 50) private String bankBranch;
    @Column(length = 50) private String workLocation;
    @Column(length = 50) private String workShift;

    @Builder.Default private int annualLeaveDays = 0;
    @Builder.Default private int sickLeaveDays   = 0;
    @Builder.Default private int casualLeaveDays = 0;

    @Column(length = 100) private String emergencyContactName;
    @Column(length = 20)  private String emergencyContactPhone;
    @Column(length = 100) private String emergencyContactRelation;
    @Column(length = 255) private String profilePicture;
    @Column(length = 1000) private String notes;

    public enum Gender         { MALE, FEMALE, OTHER }
    public enum EmployeeType   { PERMANENT, CONTRACT, TEMPORARY, INTERN, PART_TIME, CONSULTANT }
    public enum EmployeeStatus { ACTIVE, INACTIVE, ON_LEAVE, SUSPENDED, TERMINATED, RESIGNED, RETIRED }
}

// ── FILE: hrm/entity/EmployeeAddress.java ────────────────────────────────────
package com.hasnat.optimum.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_addresses",
    indexes = @Index(name = "idx_hea_emp", columnList = "employee_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeAddress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressType addressType;

    @Column(length = 200) private String addressLine1;
    @Column(length = 200) private String addressLine2;
    @Column(length = 100) private String city;
    @Column(length = 100) private String district;
    @Builder.Default @Column(length = 100) private String country = "Bangladesh";
    @Column(length = 20) private String postalCode;
    @Builder.Default @Column(nullable = false) private boolean isDefault = false;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum AddressType { PRESENT, PERMANENT, OFFICE }
}

// ── FILE: hrm/entity/Attendance.java ─────────────────────────────────────────
package com.hasnat.optimum.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "hrm_attendances",
    uniqueConstraints = @UniqueConstraint(name = "uq_att_emp_date",
        columnNames = {"employee_id", "att_date"}),
    indexes = {
        @Index(name = "idx_att_emp",  columnList = "employee_id"),
        @Index(name = "idx_att_date", columnList = "att_date"),
        @Index(name = "idx_att_org",  columnList = "organization_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false) private LocalDate attDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    @Column(precision = 5, scale = 2) private BigDecimal workingHours;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Builder.Default @Column(length = 20) private String source = "MANUAL";
    @Column(length = 500) private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum AttendanceStatus { PRESENT, ABSENT, LATE, HALF_DAY, HOLIDAY, LEAVE, WEEKEND }
}

// ── FILE: hrm/entity/EmployeeLeave.java ──────────────────────────────────────
package com.hasnat.optimum.hrm.entity;

import com.hasnat.optimum.approval.entity.ApprovalRequest;
import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_leaves",
    indexes = {
        @Index(name = "idx_leave_emp",    columnList = "employee_id"),
        @Index(name = "idx_leave_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeLeave extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(nullable = false, length = 30) private String leaveType;
    @Column(nullable = false)              private LocalDate startDate;
    @Column(nullable = false)              private LocalDate endDate;
    @Column(nullable = false, precision = 5, scale = 1) private BigDecimal totalDays;
    @Column(columnDefinition = "text")     private String reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(length = 100) private String approvedBy;
    private LocalDateTime approvedAt;

    public enum LeaveStatus { PENDING, APPROVED, REJECTED, CANCELLED }
}

// ── FILE: hrm/entity/EmployeeSalary.java ─────────────────────────────────────
package com.hasnat.optimum.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_salaries",
    indexes = {
        @Index(name = "idx_sal_emp",     columnList = "employee_id"),
        @Index(name = "idx_sal_current", columnList = "is_current")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeSalary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false) private LocalDate effectiveDate;
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal basicSalary;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal houseRent          = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal medicalAllowance   = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal transportAllowance = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal otherAllowances    = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal grossSalary;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal incomeTax          = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal providentFund      = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal otherDeductions    = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal netSalary;

    @Builder.Default @Column(nullable = false) private boolean isCurrent = true;
    @Column(length = 500) private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}

// ── FILE: hrm/entity/PayrollRun.java ─────────────────────────────────────────
package com.hasnat.optimum.hrm.entity;

import com.hasnat.optimum.accounts.entity.JournalEntryMaster;
import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hrm_payroll_runs",
    uniqueConstraints = @UniqueConstraint(name = "uq_pr_org_month",
        columnNames = {"organization_id", "payroll_month"}),
    indexes = {
        @Index(name = "idx_pr_org",    columnList = "organization_id"),
        @Index(name = "idx_pr_month",  columnList = "payroll_month"),
        @Index(name = "idx_pr_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRun extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @Column(nullable = false, length = 7) private String payrollMonth;  // YYYY-MM
    @Column(nullable = false)             private LocalDate runDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalGross      = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalDeductions = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalNet        = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false) private int employeeCount = 0;

    @Column(length = 100) private String approvedBy;
    private LocalDateTime approvedAt;
    @Column(columnDefinition = "text") private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollRunLine> lines = new ArrayList<>();

    public enum PayrollStatus { DRAFT, PROCESSING, COMPLETED, APPROVED, PAID, CANCELLED }
}

// ── FILE: hrm/entity/PayrollRunLine.java ─────────────────────────────────────
package com.hasnat.optimum.hrm.entity;

import com.hasnat.optimum.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_payroll_run_lines",
    indexes = {
        @Index(name = "idx_prl_run", columnList = "payroll_run_id"),
        @Index(name = "idx_prl_emp", columnList = "employee_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRunLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal basicSalary        = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal houseRent          = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal medicalAllowance   = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal transportAllowance = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal overtime           = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal otherAllowances    = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal grossSalary        = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal incomeTax          = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal providentFund      = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal loanDeduction      = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal otherDeductions    = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 12, scale = 2) private BigDecimal netSalary          = BigDecimal.ZERO;

    private Integer workingDays;
    private Integer leaveDays;
    private Integer absentDays;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum PaymentStatus { PENDING, PAID, CANCELLED }
}

// ── FILE: hrm/entity/CostCenterAllocation.java ───────────────────────────────
// ★ NEW in v2: Links employee payroll to cost centers for production labor costing
// Usage:
//   When payroll is run, for each production employee, create an allocation:
//     employee X spent 80% of their time in PRODUCTION cost center this month
//     allocated_amount = gross_salary × (80/100) = 24,000 BDT
//   ProductionCostService sums allocated_amount for the production cost center
//   and proportion it across production orders completed that month.
package com.hasnat.optimum.hrm.entity;

import com.hasnat.optimum.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_cost_center_allocations",
    uniqueConstraints = @UniqueConstraint(name = "uq_hcca_emp_cc_month",
        columnNames = {"employee_id", "cost_center_id", "allocation_month"}),
    indexes = {
        @Index(name = "idx_hcca_emp",    columnList = "employee_id"),
        @Index(name = "idx_hcca_cc",     columnList = "cost_center_id"),
        @Index(name = "idx_hcca_month",  columnList = "allocation_month"),
        @Index(name = "idx_hcca_payrun", columnList = "payroll_run_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CostCenterAllocation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_center_id", nullable = false)
    private CostCenter costCenter;

    /** YYYY-MM — must match the payroll run month */
    @Column(nullable = false, length = 7)
    private String allocationMonth;

    /** Employee's gross salary for this period */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;

    /** Percentage of work time spent in this cost center (0–100) */
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationPct = new BigDecimal("100.00");

    /** grossSalary × allocationPct / 100  — labor cost charged to this cost center */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(length = 500) private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
