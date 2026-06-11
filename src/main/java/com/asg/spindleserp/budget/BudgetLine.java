package com.asg.spindleserp.budget;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.hrm.setup.Department;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bgt_budget_lines",
        uniqueConstraints = @UniqueConstraint(name = "uk_bgt_line",
                columnNames = {"budget_id", "budget_head_id", "account_id", "cost_center_id", "department_id"}),
        indexes = {
                @Index(name = "idx_bgtl_budget", columnList = "budget_id"),
                @Index(name = "idx_bgtl_head", columnList = "budget_head_id"),
                @Index(name = "idx_bgtl_account", columnList = "account_id"),
                @Index(name = "idx_bgtl_cc", columnList = "cost_center_id"),
                @Index(name = "idx_bgtl_dept", columnList = "department_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetLine implements Serializable {

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
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    @Column(length = 500)
    private String description;

    // ── Budget amounts ────────────────────────────────────────────
    @Builder.Default
    @Column(name = "original_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "revised_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal revisedAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "actual_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal actualAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "committed_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal committedAmount = BigDecimal.ZERO;

    /**
     * availableAmount = revisedAmount - actualAmount - committedAmount
     * Stored as a GENERATED ALWAYS AS column in PostgreSQL.
     * In Java it is computed on @PostLoad and @PrePersist.
     */
    @Column(name = "available_amount", precision = 18, scale = 2)
    private BigDecimal availableAmount;

    // ── Monthly phasing (12 months) ───────────────────────────────
    @Builder.Default
    @Column(name = "jan_amount", precision = 18, scale = 2)
    private BigDecimal janAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "feb_amount", precision = 18, scale = 2)
    private BigDecimal febAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "mar_amount", precision = 18, scale = 2)
    private BigDecimal marAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "apr_amount", precision = 18, scale = 2)
    private BigDecimal aprAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "may_amount", precision = 18, scale = 2)
    private BigDecimal mayAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "jun_amount", precision = 18, scale = 2)
    private BigDecimal junAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "jul_amount", precision = 18, scale = 2)
    private BigDecimal julAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "aug_amount", precision = 18, scale = 2)
    private BigDecimal augAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "sep_amount", precision = 18, scale = 2)
    private BigDecimal sepAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "oct_amount", precision = 18, scale = 2)
    private BigDecimal octAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "nov_amount", precision = 18, scale = 2)
    private BigDecimal novAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "dec_amount", precision = 18, scale = 2)
    private BigDecimal decAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "budgetLine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetActual> actuals = new ArrayList<>();

    @OneToMany(mappedBy = "budgetLine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetEncumbrance> encumbrances = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Computed helpers ──────────────────────────────────────────
    @PostLoad
    @PrePersist
    @PreUpdate
    public void computeAvailable() {
        BigDecimal actual = actualAmount != null ? actualAmount : BigDecimal.ZERO;
        BigDecimal committed = committedAmount != null ? committedAmount : BigDecimal.ZERO;
        BigDecimal revised = revisedAmount != null ? revisedAmount : BigDecimal.ZERO;
        this.availableAmount = revised.subtract(actual).subtract(committed);
    }

    public boolean isOverBudget() {
        return availableAmount != null && availableAmount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getUtilisationPct() {
        if (revisedAmount == null || revisedAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return actualAmount.add(committedAmount)
                .multiply(new BigDecimal("100"))
                .divide(revisedAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getMonthlyPhaseTotal() {
        BigDecimal[] months = {janAmount, febAmount, marAmount, aprAmount, mayAmount, junAmount,
                julAmount, augAmount, sepAmount, octAmount, novAmount, decAmount};
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal m : months) if (m != null) total = total.add(m);
        return total;
    }

    /**
     * Returns the phased budget for a given month (1=Jan … 12=Dec).
     */
    public BigDecimal getAmountForMonth(int month) {
        return switch (month) {
            case 1 -> janAmount;
            case 2 -> febAmount;
            case 3 -> marAmount;
            case 4 -> aprAmount;
            case 5 -> mayAmount;
            case 6 -> junAmount;
            case 7 -> julAmount;
            case 8 -> augAmount;
            case 9 -> sepAmount;
            case 10 -> octAmount;
            case 11 -> novAmount;
            case 12 -> decAmount;
            default -> BigDecimal.ZERO;
        };
    }

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
