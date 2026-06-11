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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRunLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

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
    @Column(name = "overtime", precision = 12, scale = 2)
    private BigDecimal overtime = BigDecimal.ZERO;
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
    @Column(name = "loan_deduction", precision = 12, scale = 2)
    private BigDecimal loanDeduction = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "other_deductions", precision = 12, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary;
    @Column(name = "working_days")
    private Integer workingDays;
    @Column(name = "leave_days")
    private Integer leaveDays;
    @Column(name = "absent_days")
    private Integer absentDays;
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING"; // PENDING|PAID|FAILED
}
