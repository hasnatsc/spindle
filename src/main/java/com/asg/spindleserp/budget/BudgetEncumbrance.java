package com.asg.spindleserp.budget;

import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BudgetEncumbrance — created when a PO is approved.
 * Released (fully or partially) when the purchase invoice is posted.
 * outstandingAmount = committedAmount − releasedAmount (GENERATED in DB).
 */
@Entity
@Table(name = "bgt_encumbrances",
        indexes = {
                @Index(name = "idx_bgt_enc_budget", columnList = "budget_id"),
                @Index(name = "idx_bgt_enc_line", columnList = "budget_line_id"),
                @Index(name = "idx_bgt_enc_doc", columnList = "source_document_id"),
                @Index(name = "idx_bgt_enc_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetEncumbrance implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    // Source document — typically a PURCHASE_ORDER
    @Column(name = "source_document_type", nullable = false, length = 50)
    private String sourceDocumentType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_document_id", nullable = false)
    private BusinessDocument sourceDocument;
    @Column(name = "source_document_no", nullable = false, length = 100)
    private String sourceDocumentNo;

    @Column(name = "committed_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal committedAmount;
    @Builder.Default
    @Column(name = "released_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    /**
     * outstandingAmount = committedAmount − releasedAmount.
     * Stored as GENERATED column in DB; computed in Java on @PostLoad.
     */
    @Column(name = "outstanding_amount", precision = 18, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(name = "commitment_date", nullable = false)
    private LocalDate commitmentDate;
    @Column(name = "expected_invoice_date")
    private LocalDate expectedInvoiceDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";
    // OPEN | PARTIAL | FULLY_RELEASED | CANCELLED

    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PostLoad
    @PrePersist
    @PreUpdate
    public void computeOutstanding() {
        BigDecimal committed = committedAmount != null ? committedAmount : BigDecimal.ZERO;
        BigDecimal released = releasedAmount != null ? releasedAmount : BigDecimal.ZERO;
        this.outstandingAmount = committed.subtract(released);
    }

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isOpen() {
        return "OPEN".equals(status);
    }

    public boolean isFullyReleased() {
        return "FULLY_RELEASED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public boolean isOverdue() {
        return expectedInvoiceDate != null
                && expectedInvoiceDate.isBefore(LocalDate.now())
                && isOpen();
    }

    /**
     * Release an amount (called by PurchaseInvoiceService.onApproved()).
     *
     * @return true if fully released after this operation.
     */
    public boolean release(BigDecimal amount) {
        this.releasedAmount = this.releasedAmount.add(amount);
        if (this.releasedAmount.compareTo(this.committedAmount) >= 0) {
            this.status = "FULLY_RELEASED";
            return true;
        } else {
            this.status = "PARTIAL";
            return false;
        }
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.releasedAmount = this.committedAmount;
    }
}
