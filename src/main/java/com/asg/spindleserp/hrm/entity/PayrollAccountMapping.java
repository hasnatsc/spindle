package com.asg.spindleserp.hrm.entity;

import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Defines which GL accounts are used when payroll auto-posts to accounting.
 * One record per organization — seeded on first use.
 *
 * Journal Entry generated on payroll approval:
 *
 *   DR  Basic Salary Expense         basicSalaryAccount
 *   DR  House Rent Allowance Exp     houseRentAccount
 *   DR  Medical Allowance Exp        medicalAccount
 *   DR  Transport Allowance Exp      transportAccount
 *   DR  Other Allowances Exp         otherAllowancesAccount
 *   DR  Overtime Expense             overtimeAccount
 *       CR  Salary Payable              salaryPayableAccount
 *       CR  Income Tax Payable          incomeTaxPayableAccount
 *       CR  Provident Fund Payable      providentFundPayableAccount
 *       CR  Other Deductions Payable    otherDeductionsPayableAccount
 */
@Entity
@Table(name = "hrm_payroll_account_mappings", uniqueConstraints = @UniqueConstraint(name = "uq_pam_org", columnNames = "organization_id"), indexes = @Index(name = "idx_pam_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollAccountMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── EXPENSE accounts (DEBIT side) ──────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "basic_salary_account_id")
    private ChartOfAccount basicSalaryAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_rent_account_id")
    private ChartOfAccount houseRentAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_account_id")
    private ChartOfAccount medicalAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_account_id")
    private ChartOfAccount transportAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "other_allowances_account_id")
    private ChartOfAccount otherAllowancesAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overtime_account_id")
    private ChartOfAccount overtimeAccount;

    // ── PAYABLE accounts (CREDIT side) ─────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_payable_account_id")
    private ChartOfAccount salaryPayableAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_tax_payable_account_id")
    private ChartOfAccount incomeTaxPayableAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provident_fund_payable_account_id")
    private ChartOfAccount providentFundPayableAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "other_deductions_payable_account_id")
    private ChartOfAccount otherDeductionsPayableAccount;
}