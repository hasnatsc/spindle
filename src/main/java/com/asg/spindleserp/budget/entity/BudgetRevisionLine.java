package com.asg.spindleserp.budget.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_budget_revision_lines",
        indexes = {
                @Index(name = "idx_bbrl_rev", columnList = "revision_id"),
                @Index(name = "idx_bbrl_line", columnList = "budget_line_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRevisionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private BudgetRevision revision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    @Column(nullable = false, length = 1)
    private String direction; // '+' or '-'
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal changeAmount;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal openingAmount;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal closingAmount;
    @Column(columnDefinition = "text")
    private String reason;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
