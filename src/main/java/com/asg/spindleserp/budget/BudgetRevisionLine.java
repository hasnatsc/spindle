package com.asg.spindleserp.budget;

import lombok.Getter;

@Entity
@Table(name = "bgt_budget_revision_lines",
        indexes = @Index(name = "idx_bgt_revl_rev", columnList = "revision_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRevisionLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private BudgetRevision revision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    /**
     * direction: '+' = increase | '-' = decrease
     */
    @Column(nullable = false, length = 1)
    private String direction;

    @Column(name = "change_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal changeAmount;
    @Column(name = "opening_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingAmount;  // before
    @Column(name = "closing_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal closingAmount;  // after
    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Helper ────────────────────────────────────────────────────
    public boolean isIncrease() {
        return "+".equals(direction);
    }

    public boolean isDecrease() {
        return "-".equals(direction);
    }

    public BigDecimal getSignedAmount() {
        return isIncrease() ? changeAmount : changeAmount.negate();
    }
}
