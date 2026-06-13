package com.asg.spindleserp.hrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_employee_salaries",
        indexes = {
                @Index(name = "idx_sal_emp", columnList = "employee_id"),
                @Index(name = "idx_sal_current", columnList = "is_current")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate effectiveDate;
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal houseRent = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal medicalAllowance = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal transportAllowance = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal otherAllowances = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal incomeTax = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal providentFund = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;

    @Builder.Default
    @Column(nullable = false)
    private boolean isCurrent = true;
    @Column(length = 500)
    private String remarks;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
