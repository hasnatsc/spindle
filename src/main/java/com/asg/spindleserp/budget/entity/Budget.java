package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.approval.entity.ApprovalRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bgt_budgets",
        uniqueConstraints = @UniqueConstraint(name = "uq_bgt_org_no",
                columnNames = {"organization_id", "budget_no"}),
        indexes = {
                @Index(name = "idx_bgt_org", columnList = "organization_id"),
                @Index(name = "idx_bgt_fy", columnList = "fiscal_year_id"),
                @Index(name = "idx_bgt_status", columnList = "status"),
                @Index(name = "idx_bgt_bu", columnList = "business_unit_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    private Long businessUnitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(nullable = false, length = 50)
    private String budgetNo;
    @Column(nullable = false, length = 200)
    private String budgetName;
    @Column(columnDefinition = "text")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Budget.BudgetType budgetType = Budget.BudgetType.ANNUAL;

    @Column(nullable = false, length = 20)
    private String periodType;
    @Column(nullable = false)
    private LocalDate periodStart;
    @Column(nullable = false)
    private LocalDate periodEnd;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "BDT";
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBudgeted = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRevised = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalActual = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCommitted = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAvailable = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Budget.BudgetStatus status = Budget.BudgetStatus.DRAFT;

    @Column(length = 30)
    private String approvalStatus;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Budget.OverSpendPolicy overSpendPolicy = Budget.OverSpendPolicy.WARN;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal alertThresholdPct = new BigDecimal("80.00");

    @Builder.Default
    @Column(nullable = false)
    private boolean allowInterLineTransfer = false;
    @Builder.Default
    @Column(nullable = false)
    private int version = 1;
    @Builder.Default
    @Column(nullable = false)
    private boolean isTemplate = false;

    public enum BudgetType {ANNUAL, QUARTERLY, MONTHLY, PROJECT, DEPARTMENTAL, CAPEX, ROLLING}

    public enum BudgetStatus {DRAFT, SUBMITTED, IN_APPROVAL, APPROVED, ACTIVE, LOCKED, CLOSED, REJECTED, RETURNED}

    public enum OverSpendPolicy {ALLOW, WARN, BLOCK}
}
