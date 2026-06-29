package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// =============================================================================
// EC RETURN
// GL bridge: CREDIT_NOTE on APPROVED transition
//   DR Sales Returns (revenue reversal)
//   CR Accounts Receivable (customer AR sub-account)
// =============================================================================

@Entity
@Table(name = "ec_returns",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_return_no",
                columnNames = {"organization_id", "return_no"}),
        indexes = {
                @Index(name = "idx_ec_return_order",  columnList = "order_id"),
                @Index(name = "idx_ec_return_status", columnList = "return_status"),
                @Index(name = "idx_ec_return_gl",     columnList = "journal_entry_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcReturn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Column(nullable = false, length = 50)
    private String returnNo;

    @Builder.Default
    private LocalDateTime returnDate = LocalDateTime.now();

    @Column(columnDefinition = "text")
    private String returnReason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus returnStatus = ReturnStatus.REQUESTED;

    @Column(precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(columnDefinition = "text")
    private String remarks;

    // GL bridge: CREDIT_NOTE (JournalEntryMaster)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @Builder.Default
    @OneToMany(mappedBy = "ecReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcReturnItem> returnItems = new ArrayList<>();

    public enum ReturnStatus {
        REQUESTED, APPROVED, REJECTED, RECEIVED, REFUNDED, COMPLETED
    }
}

// =============================================================================
// EC RETURN ITEM
// orderItem → EcOrderItem → item → Item for stock re-receipt movement
// =============================================================================

@Entity
@Table(name = "ec_return_items",
        indexes = @Index(name = "idx_ec_returnitem_ret", columnList = "return_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private EcReturn ecReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private EcOrderItem orderItem;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ConditionStatus conditionStatus;

    @Column(precision = 12, scale = 3)
    private BigDecimal approvedQty;

    public enum ConditionStatus { GOOD, DAMAGED, USED, DEFECTIVE }
}

// =============================================================================
// EC REFUND
// GL bridge: PAYMENT_VOUCHER on disbursement
//   DR Accounts Receivable (customer AR sub-account)
//   CR Bank / Cash / MFS  (payment method sub-account)
// =============================================================================

@Entity
@Table(name = "ec_refunds",
        indexes = {
                @Index(name = "idx_ec_refund_return", columnList = "return_id"),
                @Index(name = "idx_ec_refund_gl",     columnList = "journal_entry_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcRefund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private EcReturn ecReturn;

    @Column(length = 50)
    private String refundNo;

    @Builder.Default
    private LocalDateTime refundDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RefundMethod refundMethod;

    @Column(precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 150)
    private String transactionReference;

    @Column(columnDefinition = "text")
    private String remarks;

    // GL bridge: PAYMENT_VOUCHER (JournalEntryMaster)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    public enum RefundMethod { BKASH, NAGAD, ROCKET, CARD, BANK, WALLET, CASH }
}
