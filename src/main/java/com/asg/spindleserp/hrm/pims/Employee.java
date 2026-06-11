package com.asg.spindleserp.hrm.pims;

import com.asg.spindleserp.hrm.attendance.Attendance;
import com.asg.spindleserp.hrm.attendance.EmployeeLeave;
import com.asg.spindleserp.hrm.payroll.EmployeeSalary;
import com.asg.spindleserp.hrm.setup.Department;
import com.asg.spindleserp.hrm.setup.Designation;
import com.asg.spindleserp.security.Organization;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "hrm_employees",
        uniqueConstraints = @UniqueConstraint(name = "uk_emp_org_code",
                columnNames = {"organization_id", "employee_code"}),
        indexes = {
                @Index(name = "idx_emp_org", columnList = "organization_id"),
                @Index(name = "idx_emp_dept", columnList = "department_id"),
                @Index(name = "idx_emp_desig", columnList = "designation_id"),
                @Index(name = "idx_emp_status", columnList = "status"),
                @Index(name = "idx_emp_manager", columnList = "reporting_manager_id"),
                @Index(name = "idx_emp_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"department", "designation", "reportingManager", "addresses", "documents", "salaries", "leaves", "attendances"})
public class Employee implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Column(name = "middle_name", length = 100)
    private String middleName;
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    @Column(length = 100)
    private String email;
    @Column(nullable = false, unique = true, length = 20)
    private String phone;
    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

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

    @Column(name = "national_id", unique = true, length = 50)
    private String nationalId;
    @Column(name = "passport_number", unique = true, length = 50)
    private String passportNumber;

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

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;
    @Column(name = "confirmation_date")
    private LocalDate confirmationDate;
    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;
    @Column(name = "resignation_date")
    private LocalDate resignationDate;
    @Column(name = "exit_date")
    private LocalDate exitDate;

    // ── Salary & Banking ─────────────────────────────────────────────────
    @Column(name = "basic_salary", precision = 12, scale = 2)
    private BigDecimal basicSalary;
    @Column(name = "gross_salary", precision = 12, scale = 2)
    private BigDecimal grossSalary;
    @Column(name = "bank_name", length = 50)
    private String bankName;
    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;
    @Column(name = "bank_branch", length = 50)
    private String bankBranch;

    // ── Work ────────────────────────────────────────────────────────────
    @Column(name = "work_location", length = 50)
    private String workLocation;
    @Column(name = "work_shift", length = 50)
    private String workShift;
    @Builder.Default
    @Column(name = "annual_leave_days")
    private Integer annualLeaveDays = 0;
    @Builder.Default
    @Column(name = "sick_leave_days")
    private Integer sickLeaveDays = 0;
    @Builder.Default
    @Column(name = "casual_leave_days")
    private Integer casualLeaveDays = 0;

    // ── Emergency ───────────────────────────────────────────────────────
    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;
    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;
    @Column(name = "emergency_contact_relation", length = 100)
    private String emergencyContactRelation;

    @Column(name = "profile_picture", length = 255)
    private String profilePicture;
    @Column(length = 1000)
    private String notes;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public boolean isActive() {
        return status == EmployeeStatus.ACTIVE;
    }

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
