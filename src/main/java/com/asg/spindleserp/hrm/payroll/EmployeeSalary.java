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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSalary implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;
    @Builder.Default
    @Column(name = "house_rent", precision = 12, scale = 2)
    private BigDecimal houseRent = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "medical", precision = 12, scale = 2)
    private BigDecimal medical = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "transport", precision = 12, scale = 2)
    private BigDecimal transport = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "other_allowances", precision = 12, scale = 2)
    private BigDecimal otherAllowances = BigDecimal.ZERO;
    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;
    @Builder.Default
    @Column(name = "income_tax", precision = 12, scale = 2)
    private BigDecimal incomeTax = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "provident_fund", precision = 12, scale = 2)
    private BigDecimal providentFund = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "other_deductions", precision = 12, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;
    @Column(length = 500)
    private String remarks;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
