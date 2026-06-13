package com.asg.spindleserp.hrm.entity;

import com.asg.spindleserp.organization.entity.CostCenter;
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
