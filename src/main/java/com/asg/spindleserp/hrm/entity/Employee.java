package com.asg.spindleserp.hrm.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.organization.entity.Department;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hrm_employees",
        uniqueConstraints = @UniqueConstraint(name = "uq_emp_org_code",
                columnNames = {"organization_id", "employee_code"}),
        indexes = {
                @Index(name = "idx_emp_org", columnList = "organization_id"),
                @Index(name = "idx_emp_dept", columnList = "department_id"),
                @Index(name = "idx_emp_desig", columnList = "designation_id"),
                @Index(name = "idx_emp_mgr", columnList = "reporting_manager_id"),
                @Index(name = "idx_emp_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false, unique = true, length = 50)
    private String employeeCode;
    @Column(nullable = false, length = 100)
    private String firstName;
    @Column(nullable = false, length = 100)
    private String lastName;
    @Column(length = 100)
    private String email;
    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Employee.Gender gender = Employee.Gender.MALE;

    @Column(nullable = false)
    private LocalDate dateOfBirth;
    @Column(length = 10)
    private String bloodGroup;
    @Column(length = 20)
    private String maritalStatus;
    @Column(unique = true, length = 50)
    private String nationalId;
    @Column(unique = true, length = 50)
    private String passportNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Employee.EmployeeType employeeType = Employee.EmployeeType.PERMANENT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Employee.EmployeeStatus status = Employee.EmployeeStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate joiningDate;
    private LocalDate confirmationDate;
    private LocalDate probationEndDate;
    private LocalDate resignationDate;
    private LocalDate exitDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal basicSalary;
    @Column(precision = 12, scale = 2)
    private BigDecimal grossSalary;
    @Column(length = 50)
    private String bankName;
    @Column(length = 50)
    private String bankAccountNumber;
    @Column(length = 50)
    private String bankBranch;
    @Column(length = 50)
    private String workLocation;
    @Column(length = 50)
    private String workShift;

    @Builder.Default
    private int annualLeaveDays = 0;
    @Builder.Default
    private int sickLeaveDays = 0;
    @Builder.Default
    private int casualLeaveDays = 0;

    @Column(length = 100)
    private String emergencyContactName;
    @Column(length = 20)
    private String emergencyContactPhone;
    @Column(length = 100)
    private String emergencyContactRelation;
    @Column(length = 255)
    private String profilePicture;
    @Column(length = 1000)
    private String notes;

    public enum Gender {MALE, FEMALE, OTHER}

    public enum EmployeeType {PERMANENT, CONTRACT, TEMPORARY, INTERN, PART_TIME, CONSULTANT}

    public enum EmployeeStatus {ACTIVE, INACTIVE, ON_LEAVE, SUSPENDED, TERMINATED, RESIGNED, RETIRED}

    @Transient
    public String getFullName() {
        return ((firstName != null ? firstName : "") + " " +
                (lastName != null ? lastName : "")).trim();
    }
}
