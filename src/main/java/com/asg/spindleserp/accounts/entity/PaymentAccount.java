package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.common.enums.PaymentMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * PaymentAccount — the single thing every module (Sales, Purchase, Payroll, POS,
 * Travel, HRM) references when money moves. It is the stable seam between
 * "how it was paid" and "which ledger gets hit".
 *
 * <pre>
 *   PaymentMode → PaymentAccount → ChartOfAccount (ledger)
 *                                → ChartOfAccountSub (sub-ledger, optional)
 * </pre>
 * <p>
 * Adding "bKash Personal" or a third POS terminal is a data row, not a release.
 * <p>
 * Multi-tenant: scoped by organization_id from BaseEntity; unique on
 * (organization_id, payment_account_code).
 */
@Entity
@Table(name = "acc_payment_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uq_pa_org_code",
                columnNames = {"organization_id", "payment_account_code"}),
        indexes = {
                @Index(name = "idx_pa_org", columnList = "organization_id"),
                @Index(name = "idx_pa_mode", columnList = "organization_id,payment_mode"),
                @Index(name = "idx_pa_category", columnList = "organization_id,account_category"),
                @Index(name = "idx_pa_ledger", columnList = "ledger_id"),
                @Index(name = "idx_pa_sub_ledger", columnList = "sub_ledger_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identity ──────────────────────────────────────────────────────────────

    /** Org-unique business key, e.g. PA-CASH-HO, PA-BKASH-MERCHANT. */
    @Column(name = "payment_account_code", nullable = false, length = 50)
    private String paymentAccountCode;

    /** Display name shown in every module's payment picker, e.g. "bKash Merchant". */
    @Column(nullable = false, length = 200)
    private String paymentAccountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMode paymentMode;

    /**
     * Denormalised from {@link #paymentMode} so DataTable SQL can filter on
     * category without a CASE expression. Kept in sync by {@link #normalize()}
     * and the overridden setter below — never set it independently.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_category", nullable = false, length = 30)
    private PaymentMode.AccountCategory accountCategory;

    // ── Posting targets ───────────────────────────────────────────────────────

    /** The GL head that gets debited/credited. Mandatory — this is the whole point. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private ChartOfAccount ledger;

    /**
     * Optional sub-ledger row (the specific bank A/C, cash box, MFS wallet, POS
     * terminal). Its discriminator must belong to the same AccountCategory —
     * enforced in PaymentAccountResolver, not by the DB.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_ledger_id")
    private ChartOfAccountSub subLedger;

    /** Where MDR / cash-out / MFS charges are expensed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_account_id")
    private ChartOfAccount chargeAccount;

    /**
     * Bank PaymentAccount that a CARD / MOBILE_BANKING balance sweeps into on
     * settlement. Plain Long — self-referencing FK resolved at service layer.
     */
    @Column(name = "settlement_payment_account_id")
    private Long settlementPaymentAccountId;

    // ── Instrument metadata (denormalised for fast display) ───────────────────

    /** "bKash", "DBBL", "Visa" — free text so new providers need no code change. */
    @Column(length = 100)
    private String providerName;

    /** MSISDN / bank A/C number / terminal ID, whichever applies to the mode. */
    @Column(length = 100)
    private String accountIdentifier;

    @Column(length = 50)
    private String merchantNumber;

    @Builder.Default
    @Column(length = 10)
    private String currency = "BDT";

    // ── Balances ──────────────────────────────────────────────────────────────

    @Column(precision = 18, scale = 2)
    private BigDecimal openingBalance;

    @Column(precision = 18, scale = 2)
    private BigDecimal currentBalance;

    // ── Controls ──────────────────────────────────────────────────────────────

    /** Charge/MDR percentage applied by the provider, e.g. 1.4900. */
    @Column(precision = 8, scale = 4)
    private BigDecimal chargeRate;

    @Column(precision = 18, scale = 2)
    private BigDecimal perTransactionLimit;

    @Column(precision = 18, scale = 2)
    private BigDecimal dailyLimit;

    @Column(precision = 18, scale = 2)
    private BigDecimal approvalThreshold;

    /** T+N days until funds land in the settlement account. */
    private Integer settlementDays;

    @Builder.Default
    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(length = 500)
    private String description;

    // ── Flags ─────────────────────────────────────────────────────────────────

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    /** One default per (organization_id, payment_mode) — enforced by partial unique index. */
    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean isSystem = false;

    /** May be used on RECEIPT_VOUCHER (money in). */
    @Builder.Default
    @Column(nullable = false)
    private boolean allowReceipt = true;

    /** May be used on PAYMENT_VOUCHER (money out). */
    @Builder.Default
    @Column(nullable = false)
    private boolean allowPayment = true;

    /** Force an instrument reference (TrxID / cheque no / auth code) before POST. */
    @Builder.Default
    @Column(nullable = false)
    private boolean requireReference = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean requireApproval = false;

    // ── Invariants ────────────────────────────────────────────────────────────

    /**
     * Keeps {@link #accountCategory} derived from {@link #paymentMode}.
     * The @Builder path bypasses setters, so the service layer must call
     * {@code normalize()} before every save. ★ wired in PaymentAccountResolver.save()
     */
    public PaymentAccount normalize() {
        this.accountCategory = (paymentMode == null) ? null : paymentMode.getAccountCategory();
        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "BDT";
        }
        if (this.paymentAccountCode != null) {
            this.paymentAccountCode = this.paymentAccountCode.trim().toUpperCase();
        }
        return this;
    }

    /** Overrides Lombok's generated setter so category can never drift from mode. */
    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
        this.accountCategory = (paymentMode == null) ? null : paymentMode.getAccountCategory();
    }

    /** Label for pickers: "bKash Merchant (bKash)". */
    @Transient
    public String getDisplayLabel() {
        if (paymentMode == null) return paymentAccountName;
        return paymentAccountName + " (" + paymentMode.getLabel() + ")";
    }
}
