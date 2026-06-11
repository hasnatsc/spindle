package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eco_refunds",
        indexes = @Index(name = "idx_refund_order", columnList = "eco_order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoRefund extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_transaction_id")
    private EcoPaymentTransaction originalTransaction;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_transaction_id")
    private EcoPaymentTransaction refundTransaction;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;
    @Column(name = "refund_reason", nullable = false, length = 30)
    private String refundReason; // CUSTOMER_REQUEST|DEFECTIVE|WRONG_ITEM|OUT_OF_STOCK|FRAUD|DUPLICATE|OTHER
    @Column(name = "reason_detail", columnDefinition = "TEXT")
    private String reasonDetail;
    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;
    @Column(name = "refund_method", nullable = false, length = 30)
    private String refundMethod; // ORIGINAL_METHOD|WALLET|BANK_TRANSFER|STORE_CREDIT|CASH
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
    // PENDING|APPROVED|PROCESSING|COMPLETED|REJECTED
    @Column(name = "requested_by", length = 100)
    private String requestedBy;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
