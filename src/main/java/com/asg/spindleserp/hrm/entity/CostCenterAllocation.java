package com.asg.spindleserp.hrm.entity;

import com.asg.spindleserp.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hrm_cost_center_allocations",
        uniqueConstraints = @UniqueConstraint(name = "uq_hcca_emp_cc_month",
                columnNames = {"employee_id", "cost_center_id", "allocation_month"}),
        indexes = {
                @Index(name = "idx_hcca_emp", columnList = "employee_id"),
                @Index(name = "idx_hcca_cc", columnList = "cost_center_id"),
                @Index(name = "idx_hcca_month", columnList = "allocation_month"),
                @Index(name = "idx_hcca_payrun", columnList = "payroll_run_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostCenterAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    /**
     * YYYY-MM — must match the payroll run month
     */
    @Column(nullable = false, length = 7)
    private String allocationMonth;

    /**
     * Employee's gross salary for this period
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;

    /**
     * Percentage of work time spent in this cost center (0–100)
     */
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationPct = new BigDecimal("100.00");

    /**
     * grossSalary × allocationPct / 100  — labor cost charged to this cost center
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(length = 500)
    private String remarks;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
