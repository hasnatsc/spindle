package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.common.enums.PaymentMode;
import com.asg.spindleserp.common.enums.VoucherType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JournalEntryMaster — single table for all voucher types.
 * <p>
 * Voucher lifecycle (voucherStatus):
 * DRAFT → POSTED → REVERSED | CANCELLED
 * <p>
 * AP / AR settlement:
 * totalAmount      = invoice / payment face amount
 * allocatedAmount  = SUM of acc_voucher_allocations rows (updated atomically)
 * getDueAmount()   = totalAmount - allocatedAmount  ← @Transient, NOT persisted
 * <p>
 * Aging is computed in SQL using dueDate and the two stored columns above.
 * <p>
 * New columns added by V4__voucher_master_fields.sql migration.
 * paymentMode converted to enum + paymentAccountId added by
 * V310__payment_mode_and_payment_account.sql.
 */
@Entity
@Table(name = "acc_journal_entry_master",
        indexes = {
                @Index(name = "idx_jem_org", columnList = "organization_id"),
                @Index(name = "idx_jem_date", columnList = "voucher_date"),
                @Index(name = "idx_jem_type", columnList = "voucher_type"),
                @Index(name = "idx_jem_posted", columnList = "is_posted"),
                @Index(name = "idx_jem_no", columnList = "voucher_no"),
                @Index(name = "idx_jem_status", columnList = "voucher_status"),
                @Index(name = "idx_jem_party", columnList = "organization_id,party_type,party_id"),
                @Index(name = "idx_jem_due", columnList = "due_date"),
                @Index(name = "idx_jem_allocated", columnList = "organization_id,voucher_type,voucher_status"),
                @Index(name = "idx_jem_pay_account", columnList = "organization_id,payment_account_id"),
                @Index(name = "idx_jem_pay_mode", columnList = "organization_id,payment_mode")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Core identity ─────────────────────────────────────────────────────────

    /**
     * System-generated voucher number, e.g. JV-25-000001. Null until posted.
     */
    @Column(length = 100, unique = true)
    private String voucherNo;

    private LocalDate voucherDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VoucherType voucherType;

    // ── Status lifecycle ──────────────────────────────────────────────────────

    /**
     * DRAFT | PENDING_APPROVAL | POSTED | REJECTED | REVERSED | CANCELLED
     */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String voucherStatus = "DRAFT";

    /**
     * Legacy boolean — kept in sync with voucherStatus for backward compatibility.
     * False for DRAFT, PENDING_APPROVAL, REJECTED; true only after POSTED.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean isPosted = false;

    @Column(length = 100)
    private String postedBy;

    private LocalDateTime postedAt;

    // ── GL totals ─────────────────────────────────────────────────────────────

    @Column(precision = 18, scale = 2)
    private BigDecimal totalDebit;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalCredit;

    /**
     * Net face amount for this voucher:
     * - JOURNAL_VOUCHER  : totalDebit (= totalCredit, balanced)
     * - PAYMENT_VOUCHER  : amount paid out to party
     * - RECEIPT_VOUCHER  : amount received from party
     * - CONTRA_VOUCHER   : transfer amount
     * Used as the base for AP/AR settlement tracking.
     */
    @Column(precision = 18, scale = 2)
    private BigDecimal totalAmount;

    // ── AP / AR settlement ────────────────────────────────────────────────────

    /**
     * Running total of all allocations made against this voucher as SOURCE.
     * Updated atomically via JournalEntryMasterRepository.addAllocation() /
     * subtractAllocation() JPQL — never set directly except on initial create.
     */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    /**
     * Remaining balance = totalAmount - allocatedAmount.
     *
     * @Transient — computed on every call, never persisted.
     * Use in service layer for allocation cap checks and aging queries (via SQL).
     */
    @Transient
    public BigDecimal getDueAmount() {
        if (totalAmount == null) return BigDecimal.ZERO;
        BigDecimal alloc = (allocatedAmount != null) ? allocatedAmount : BigDecimal.ZERO;
        return totalAmount.subtract(alloc).max(BigDecimal.ZERO);
    }

    /**
     * When the invoice / bill / note is due for settlement
     */
    private LocalDate dueDate;

    // ── Party reference (AP / AR) ─────────────────────────────────────────────

    /**
     * SUPPLIER | CUSTOMER | EMPLOYEE
     */
    @Column(length = 20)
    private String partyType;

    /**
     * FK to acc_chart_of_accounts_sub.id.
     * Stored as plain Long (no @ManyToOne) to avoid lazy-init issues in
     * the accounts service layer and to keep JournalEntryMaster serializable.
     */
    private Long partyId;

    // ── Bank / Cash references (Payment / Receipt / Contra) ───────────────────

    /**
     * For PAYMENT_VOUCHER : the bank/cash account being credited (money going out).
     * For RECEIPT_VOUCHER : the bank/cash account being debited (money coming in).
     * For CONTRA_VOUCHER  : the FROM account (DR side).
     * <p>
     * LEGACY — retained for existing vouchers and reports. New code should set
     * paymentAccountId instead and let PaymentAccountResolver produce the ledger.
     */
    private Long bankAccountId;

    /**
     * For CONTRA_VOUCHER  : the TO account (CR side).
     * For CASH PAYMENT    : the cash account when mode = CASH.
     * <p>
     * LEGACY — see bankAccountId.
     */
    private Long cashAccountId;

    // ── Payment instrument details ────────────────────────────────────────────

    /**
     * CASH | BANK | BKASH | NAGAD | ROCKET | CARD | CHEQUE | ONLINE_TRANSFER | WALLET
     * <p>
     * Was a free-text String until V310; legacy values were remapped in that
     * migration ("BANK_TRANSFER" → BANK, "ONLINE" → ONLINE_TRANSFER).
     * A payment mode never identifies a ledger on its own — see paymentAccountId.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 30)
    private PaymentMode paymentMode;

    /**
     * FK to acc_payment_accounts.id — THE posting target for this voucher.
     * Stored as plain Long (no @ManyToOne) to match the partyId / bankAccountId
     * convention and keep this entity serializable.
     * <p>
     * Resolve to a ledger with PaymentAccountResolver.resolve(orgId, paymentMode,
     * paymentAccountId) rather than reading paymentMode directly. ★
     */
    @Column(name = "payment_account_id")
    private Long paymentAccountId;

    /**
     * Instrument reference: MFS TrxID, card authorisation code, online transfer
     * reference. Cheques use chequeNumber / chequeDate below.
     */
    @Column(length = 100)
    private String paymentReference;

    @Column(length = 50)
    private String chequeNumber;

    private LocalDate chequeDate;

    // ── Reversal chain ────────────────────────────────────────────────────────

    /**
     * ID of the original voucher that was reversed to create this mirror entry
     */
    private Long reversedVoucherId;

    /**
     * True when this voucher has been reversed (original side)
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean isReversed = false;

    // ── Narration / reference ─────────────────────────────────────────────────

    @Column(length = 1000)
    private String narration;

    @Column(length = 100)
    private String referenceNo;

    // ── GL lines ──────────────────────────────────────────────────────────────

    @Builder.Default
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();

    // ── Backward-compatible String accessors ──────────────────────────────────
    // Existing DTOs, Thymeleaf expressions and JSON payloads that treat
    // paymentMode as a String keep working through these two methods. ★

    /**
     * @return the enum name, or null. Safe for th:text / JSON serialisation.
     */
    @Transient
    public String getPaymentModeCode() {
        return paymentMode == null ? null : paymentMode.name();
    }

    /**
     * Accepts legacy free-text values ("BANK_TRANSFER", "ONLINE", "bkash", ...).
     * Unmappable input sets null rather than throwing — validate in the service
     * layer where a proper message can be returned to the user.
     */
    public void setPaymentModeCode(String code) {
        this.paymentMode = PaymentMode.fromCode(code);
    }
}
