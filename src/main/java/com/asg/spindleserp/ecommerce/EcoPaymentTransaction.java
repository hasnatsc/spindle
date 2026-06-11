package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.accounts.setup.BankAccount;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "eco_payment_transactions",
        indexes = {
                @Index(name = "idx_ptxn_order", columnList = "eco_order_id"),
                @Index(name = "idx_ptxn_status", columnList = "status"),
                @Index(name = "idx_ptxn_ref", columnList = "transaction_ref"),
                @Index(name = "idx_ptxn_method", columnList = "payment_method_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoPaymentTransaction extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_id", nullable = false)
    private EcoOrder ecoOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private EcoPaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by_user_id")
    private User collectedBy;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;
    // PAYMENT|PARTIAL_PAYMENT|REFUND|PARTIAL_REFUND|CHARGEBACK|COD_COLLECTION

    @Column(name = "transaction_ref", length = 200)
    private String transactionRef;
    @Column(name = "gateway_order_id", length = 200)
    private String gatewayOrderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gateway_response", columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(length = 3)
    @Builder.Default
    private String currency = "BDT";
    @Builder.Default
    @Column(name = "exchange_rate", precision = 18, scale = 4)
    private BigDecimal exchangeRate = BigDecimal.ONE;
    @Builder.Default
    @Column(name = "gateway_fee", precision = 18, scale = 2)
    private BigDecimal gatewayFee = BigDecimal.ZERO;
    @Column(name = "net_amount", precision = 18, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING";
    // PENDING|INITIATED|SUCCESS|FAILED|CANCELLED|EXPIRED|REFUNDED

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    @Column(name = "payer_name", length = 200)
    private String payerName;
    @Column(name = "payer_mobile", length = 20)
    private String payerMobile;
    @Column(name = "payer_account", length = 100)
    private String payerAccount;
    @Column(name = "collected_at")
    private LocalDateTime collectedAt;
    @Column(name = "collection_notes", columnDefinition = "TEXT")
    private String collectionNotes;
    @Builder.Default
    @Column(name = "is_reconciled", nullable = false)
    private Boolean isReconciled = false;
    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;
    @Column(name = "reconciled_by", length = 100)
    private String reconciledBy;
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
