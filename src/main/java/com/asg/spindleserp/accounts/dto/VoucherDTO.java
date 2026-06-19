package com.asg.spindleserp.accounts.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * VoucherDTO — unified DTO for Journal, Payment, Receipt, and Contra vouchers.
 *
 * voucherType discriminates the form:
 *   JOURNAL_VOUCHER  → free GL lines (debit/credit balanced)
 *   PAYMENT_VOUCHER  → party + bank/cash + invoice allocations
 *   RECEIPT_VOUCHER  → party + bank/cash + invoice allocations
 *   CONTRA_VOUCHER   → bank-to-bank / cash-to-bank fund transfer
 *
 * Status lifecycle:  DRAFT → POSTED → REVERSED | CANCELLED
 *
 * Allocation lines populate acc_voucher_allocations and update
 * JournalEntryMaster.allocated_amount on source invoices/bills.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherDTO {

    private Long   id;
    private String voucherNo;

    @NotBlank(message = "Voucher type is required")
    private String voucherType;   // JOURNAL_VOUCHER | PAYMENT_VOUCHER | RECEIPT_VOUCHER | CONTRA_VOUCHER

    @NotNull(message = "Voucher date is required")
    private LocalDate voucherDate;

    private LocalDate dueDate;

    @Builder.Default
    private String voucherStatus = "DRAFT";   // DRAFT | POSTED | REVERSED | CANCELLED

    @Size(max = 100) private String referenceNo;
    @Size(max = 1000) private String narration;

    // ── Total amounts ─────────────────────────────────────────────────────────
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal totalAmount;       // convenience — equals totalDebit for PV
    private BigDecimal allocatedAmount;   // sum of settled allocations
    private BigDecimal dueAmount;         // totalAmount - allocatedAmount

    // ── Party (PAYMENT / RECEIPT) — AJAX Select2 ─────────────────────────────
    private String partyType;             // SUPPLIER | CUSTOMER | EMPLOYEE
    private Long   partyId;
    private String partyDisplay;          // "{code} — {name}"
    private BigDecimal partyBalance;      // current balance for display

    // ── Bank / Cash — AJAX Select2 ───────────────────────────────────────────
    private Long   bankAccountId;         // sub-account id
    private String bankAccountDisplay;
    private Long   cashAccountId;
    private String cashAccountDisplay;

    // ── Payment mode (for PV / RV) ────────────────────────────────────────────
    @Size(max = 30) private String paymentMode;    // BANK_TRANSFER | CHEQUE | CASH | ONLINE
    @Size(max = 50) private String chequeNumber;
    private LocalDate chequeDate;

    // ── Contra-specific ───────────────────────────────────────────────────────
    private Long   fromAccountId;          // sub-account BANK/CASH (debit side)
    private String fromAccountDisplay;
    private Long   toAccountId;            // sub-account BANK/CASH (credit side)
    private String toAccountDisplay;

    // ── Reversal ──────────────────────────────────────────────────────────────
    private Long   reversedVoucherId;
    private String reversedVoucherNo;
    @Builder.Default private Boolean reversed = false;

    // ── GL lines (Journal Voucher only) ──────────────────────────────────────
    @Valid
    private List<LineDTO> lines;

    // ── Allocation lines (Payment / Receipt) ─────────────────────────────────
    @Valid
    private List<AllocationDTO> allocations;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
    private String postedBy;
    private String postedAt;

    // ═════════════════════════════════════════════════════════════════════════
    // INNER: GL Line
    // ═════════════════════════════════════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineDTO {
        private Long    id;
        private Integer lineNumber;

        @NotNull(message = "Account is required on each line")
        private Long   accountId;
        private String accountDisplay;   // "{code} — {name}"

        private Long   subAccountId;
        private String subAccountDisplay;

        private Long   costCenterId;
        private String costCenterDisplay;

        @NotBlank(message = "Entry type is required")
        private String entryType;         // DEBIT | CREDIT

        @NotNull(message = "Amount is required")
        private BigDecimal amount;

        @Size(max = 500) private String narration;
        @Size(max = 100) private String referenceNo;
        @Size(max = 20)  private String taxCode;
        @Builder.Default private Boolean isTaxLine = false;
        @Size(max = 20)  private String currencyCode;
        private BigDecimal exchangeRate;
        private BigDecimal baseAmount;
        @Size(max = 50)  private String sourceType;
        private Long     sourceId;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // INNER: Allocation line (head-to-head settlement)
    // ═════════════════════════════════════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AllocationDTO {
        private Long   id;

        /** The source invoice / bill voucher being settled */
        @NotNull(message = "Source voucher is required for allocation")
        private Long   sourceVoucherId;
        private String sourceVoucherNo;
        private String sourceVoucherType;
        private LocalDate sourceDueDate;
        private BigDecimal sourceTotal;
        private BigDecimal sourceAlreadyAllocated;
        private BigDecimal sourceRemaining;    // populated from server

        /** Amount being allocated in this line */
        @NotNull(message = "Allocated amount is required")
        private BigDecimal allocatedAmount;

        @Builder.Default
        private BigDecimal discountAmount  = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal writeOffAmount  = BigDecimal.ZERO;

        private LocalDate allocationDate;
        @Size(max = 500) private String narration;
    }
}
