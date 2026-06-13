package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.entity.Department;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bgt_budget_lines",
        indexes = {
                @Index(name = "idx_bbl_budget", columnList = "budget_id"),
                @Index(name = "idx_bbl_head", columnList = "budget_head_id"),
                @Index(name = "idx_bbl_acct", columnList = "account_id"),
                @Index(name = "idx_bbl_cc", columnList = "cost_center_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_head_id", nullable = false)
    private BudgetHead budgetHead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private ChartOfAccount account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    private Integer lineNumber;
    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal originalAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal revisedAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal actualAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal committedAmount = BigDecimal.ZERO;

    // availableAmount = revisedAmount - actualAmount - committedAmount
    // GENERATED ALWAYS AS STORED in DB — NOT mapped here. Read via DTO or @Formula.

    // Monthly phasing
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal janAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal febAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal marAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal aprAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal mayAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal junAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal julAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal augAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal sepAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal octAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal novAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal decAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String notes;
}
