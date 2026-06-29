package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import jakarta.persistence.*;
import lombok.*;

/**
 * EcGlAccountDefaults — per-org default GL accounts for ecommerce GL postings.
 *
 * Mirrors hrm_payroll_account_mappings pattern exactly:
 *   One row per organization (UNIQUE constraint).
 *   All account FKs reference ChartOfAccount (acc_chart_of_accounts).
 *   Sub-account reference uses ChartOfAccountSub (acc_chart_of_accounts_sub).
 *
 * EcErpBridgeService lookup priority:
 *   1. This table for org
 *   2. Fall back to acc_mapping if ECOMMERCE module_type exists
 *   3. Fall back to first matching account_type
 *
 * Voucher line mapping:
 *   SALES_VOUCHER  (ORDER_CONFIRMED):
 *     DR  salesRevenueAccount   → JournalEntryLine.account
 *     CR  accountsReceivable    → JournalEntryLine.account
 *                                  (or customer.erpSubAccount if linked)
 *     CR  vatPayableAccount     → JournalEntryLine.account
 *
 *   RECEIPT_VOUCHER (PAYMENT_SUCCESS):
 *     DR  defaultBankSubAccount → JournalEntryLine.subAccount (BankAccount STI)
 *     CR  accountsReceivable    → JournalEntryLine.account
 *
 *   CREDIT_NOTE  (RETURN_APPROVED):
 *     DR  salesReturnsAccount   → JournalEntryLine.account
 *     CR  accountsReceivable    → JournalEntryLine.account
 */
@Entity
@Table(name = "ec_gl_account_defaults",
        indexes = @Index(name = "idx_ec_gl_defaults_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcGlAccountDefaults extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Revenue (ChartOfAccount.AccountType = REVENUE) ────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_revenue_account_id")
    private ChartOfAccount salesRevenueAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_returns_account_id")
    private ChartOfAccount salesReturnsAccount;

    // ── COGS (ChartOfAccount.AccountType = EXPENSE) ───────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cogs_account_id")
    private ChartOfAccount cogsAccount;

    // ── Receivable control (ChartOfAccount.AccountType = ASSET) ──────────
    // Used when EcCustomer.erpSubAccount is NULL (no individual AR sub-account)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounts_receivable_id")
    private ChartOfAccount accountsReceivable;

    // ── Tax (ChartOfAccount.AccountType = LIABILITY) ──────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_payable_account_id")
    private ChartOfAccount vatPayableAccount;

    // ── Discount (ChartOfAccount.AccountType = EXPENSE) ──────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_expense_account_id")
    private ChartOfAccount discountExpenseAccount;

    // ── Shipping income (ChartOfAccount.AccountType = REVENUE) ───────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_income_account_id")
    private ChartOfAccount shippingIncomeAccount;

    // ── Default receiving bank/cash sub-account ───────────────────────────
    // ChartOfAccountSub STI: BankAccount or CashAccount discriminator
    // Overridden per-payment by EcPayment.receivingSubAccount
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_bank_sub_account_id")
    private ChartOfAccountSub defaultBankSubAccount;
}
