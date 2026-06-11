package com.asg.spindleserp.budget;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_transfers",
        uniqueConstraints = @UniqueConstraint(name = "uk_bgt_xfr_org_no",
                columnNames = {"organization_id", "transfer_no"}),
        indexes = {
                @Index(name = "idx_bgt_xfr_budget", columnList = "budget_id"),
                @Index(name = "idx_bgt_xfr_from", columnList = "from_line_id"),
                @Index(name = "idx_bgt_xfr_to", columnList = "to_line_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetTransfer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(name = "transfer_no", nullable = false, length = 50)
    private String transferNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_line_id", nullable = false)
    private BudgetLine fromLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_line_id", nullable = false)
    private BudgetLine toLine;

    @Column(name = "transfer_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal transferAmount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
    // PENDING | APPROVED | REJECTED

    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Helper ────────────────────────────────────────────────────
    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    /**
     * Apply the transfer to the two budget lines.
     * Called by BudgetService.applyTransfer() after approval.
     */
    public void apply() {
        if (!isApproved()) throw new IllegalStateException("Transfer not approved");
        // Deduct from source
        fromLine.setRevisedAmount(fromLine.getRevisedAmount().subtract(transferAmount));
        fromLine.computeAvailable();
        // Add to destination
        toLine.setRevisedAmount(toLine.getRevisedAmount().add(transferAmount));
        toLine.computeAvailable();
    }
}
