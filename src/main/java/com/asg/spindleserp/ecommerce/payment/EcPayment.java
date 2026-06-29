package com.asg.spindleserp.ecommerce.payment;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import com.asg.spindleserp.ecommerce.order.EcOrder;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * EcPayment — one row per payment attempt against an order.
 *
 * Multiple EcPayment rows per EcOrder are allowed (partial payments).
 *
 * GL bridge (mirrors DepreciationRun.journalEntry / Production.journalEntry):
 *   journalEntry populated on paymentStatus → SUCCESS by EcPaymentJournalService
 *   VoucherType = RECEIPT_VOUCHER
 *   DR  receivingSubAccount (BankAccount / CashAccount discriminator)
 *   CR  Accounts Receivable control  OR  customer.erpSubAccount (CustomerAccount)
 *
 * receivingSubAccount: BankAccount for BANK/CARD/SSLCOMMERZ/MFS,
 *                      CashAccount for COD/CASH
 * bank_name / account_number NOT stored here — read from
 *   receivingSubAccount (ChartOfAccountSub.bankName / .accountNumber via STI)
 */
@Entity
@Table(name = "ec_payments",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_payment_no",
                columnNames = {"organization_id", "payment_no"}),
        indexes = {
                @Index(name = "idx_ec_payment_order",  columnList = "order_id"),
                @Index(name = "idx_ec_payment_status", columnList = "payment_status"),
                @Index(name = "idx_ec_payment_method", columnList = "payment_method"),
                @Index(name = "idx_ec_payment_gl",     columnList = "journal_entry_id"),
                @Index(name = "idx_ec_payment_sub",    columnList = "receiving_sub_account_id"),
                @Index(name = "idx_ec_payment_org",    columnList = "organization_id"),
                @Index(name = "idx_ec_payment_date",   columnList = "payment_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @Column(nullable = false, length = 50)
    private String paymentNo;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime paymentDate;

    // Gateway-specific references
    @Column(length = 200) private String transactionReference;
    @Column(length = 200) private String gatewayTransactionId;

    // bank_name / account_number NOT here — read from receivingSubAccount STI fields:
    //   BankAccount.bankName / BankAccount.accountNumber

    @Builder.Default
    @Column(length = 10)
    private String currency = "BDT";

    @Builder.Default
    @Column(precision = 18, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal paidAmount;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String remarks;

    // ── GL BRIDGE ─────────────────────────────────────────────────────────
    // RECEIPT_VOUCHER: DR receiving account  CR AR sub-account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    // Bank/Cash/MFS sub-account receiving this payment
    // BankAccount (BANK/CARD/SSLCOMMERZ) or CashAccount (COD/CASH)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_sub_account_id")
    private ChartOfAccountSub receivingSubAccount;

    @Builder.Default
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcPaymentTransaction> transactions = new ArrayList<>();

    // ── Enums ─────────────────────────────────────────────────────────────
    public enum PaymentMethod {
        BKASH, NAGAD, ROCKET, SSLCOMMERZ, STRIPE, PAYPAL, BANK, COD, WALLET, CASH
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, CANCELLED, REFUNDED, PARTIAL
    }
}

// =============================================================================
// EC PAYMENT TRANSACTION  (gateway raw request/response audit — immutable)
// JSONB matches existing ec_shipping_api_logs pattern
// =============================================================================

