package com.asg.spindleserp.hrm.service;

import com.asg.spindleserp.accounts.entity.*;
import com.asg.spindleserp.accounts.repository.*;
import com.asg.spindleserp.common.enums.VoucherType;
import com.asg.spindleserp.hrm.entity.*;
import com.asg.spindleserp.hrm.repository.*;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PayrollJournalService
 *
 * Generates the GL journal entry when a payroll run is approved.
 *
 * Journal Entry Structure (PAYROLL_VOUCHER):
 *
 *   DEBIT  lines (Expense):
 *     ├── Basic Salary Expense         → sum of all employees' basic
 *     ├── House Rent Allowance         → sum of house rent
 *     ├── Medical Allowance            → sum of medical
 *     ├── Transport Allowance          → sum of transport
 *     ├── Overtime Expense             → sum of overtime
 *     └── Other Allowances             → sum of other allowances
 *
 *   CREDIT lines (Payable Liabilities):
 *     ├── Salary Payable               → total net salary
 *     ├── Income Tax Payable           → total income tax
 *     ├── Provident Fund Payable       → total provident fund
 *     └── Other Deductions Payable     → total other deductions
 *
 * Balanced check: totalDebit == totalCredit (gross = net + deductions)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollJournalService {

    private final PayrollRunRepository          payrollRunRepo;
    private final PayrollRunLineRepository      lineRepo;
    private final PayrollAccountMappingRepository mappingRepo;
    private final JournalEntryMasterRepository  journalRepo;
    private final JournalEntryLineRepository    journalLineRepo;
    private final JdbcTemplate                  jdbcTemplate;

    /**
     * Called from HrmServiceImpl.approvePayrollRun() after status flip.
     * Idempotent — if journal already exists it returns without recreating.
     *
     * @param payrollRun fully-loaded PayrollRun with lines
     */
    public JournalEntryMaster generatePayrollJournal(PayrollRun payrollRun) {
        log.info("Generating payroll journal for run ID: {} month: {}",
                payrollRun.getId(), payrollRun.getPayrollMonth());

        // Guard: already posted
        if (payrollRun.getJournalEntry() != null) {
            log.warn("Payroll run {} already has journal entry #{}",
                    payrollRun.getId(), payrollRun.getJournalEntry().getId());
            return payrollRun.getJournalEntry();
        }

        Long orgId = payrollRun.getOrganization().getId();

        PayrollAccountMapping mapping = mappingRepo.findByOrganizationId(orgId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payroll account mapping not configured for this organization. " +
                                "Please set up payroll GL accounts in Settings → Payroll Mapping."));

        validateMapping(mapping);

        List<PayrollRunLine> lines = lineRepo.findByPayrollRunId(payrollRun.getId());

        if (lines.isEmpty()) {
            throw new IllegalStateException("No payroll lines found for run #" + payrollRun.getId());
        }

        // Aggregate all amounts
        BigDecimal totalBasic      = sum(lines, l -> l.getBasicSalary());
        BigDecimal totalHouseRent  = sum(lines, l -> l.getHouseRent());
        BigDecimal totalMedical    = sum(lines, l -> l.getMedicalAllowance());
        BigDecimal totalTransport  = sum(lines, l -> l.getTransportAllowance());
        BigDecimal totalOvertime   = sum(lines, l -> l.getOvertime());
        BigDecimal totalOtherAllow = sum(lines, l -> l.getOtherAllowances());
        BigDecimal totalGross      = sum(lines, l -> l.getGrossSalary());
        BigDecimal totalTax        = sum(lines, l -> l.getIncomeTax());
        BigDecimal totalPF         = sum(lines, l -> l.getProvidentFund());
        BigDecimal totalLoan       = sum(lines, l -> l.getLoanDeduction());
        BigDecimal totalOtherDeduc = sum(lines, l -> l.getOtherDeductions());
        BigDecimal totalNet        = sum(lines, l -> l.getNetSalary());

        // Salary payable = net salary (what employees actually receive in hand)
        BigDecimal salaryPayable = totalNet;

        // Validate balance: gross = net + all deductions
        BigDecimal debitTotal  = totalBasic.add(totalHouseRent).add(totalMedical)
                .add(totalTransport).add(totalOvertime).add(totalOtherAllow);
        BigDecimal creditTotal = salaryPayable.add(totalTax).add(totalPF)
                .add(totalLoan).add(totalOtherDeduc);

        if (debitTotal.compareTo(creditTotal) != 0) {
            log.error("Payroll journal imbalanced: DR={} CR={}", debitTotal, creditTotal);
            throw new IllegalStateException(
                    String.format("Payroll journal is imbalanced: Debit=%.2f Credit=%.2f. " +
                                    "Difference=%.2f. Please recalculate payroll.",
                            debitTotal, creditTotal, debitTotal.subtract(creditTotal).abs()));
        }

        String voucherNo = generateVoucherNo(orgId);
        String narration = String.format("Payroll for %s — %d employees | Gross: %.2f | Net: %.2f",
                payrollRun.getPayrollMonth(), lines.size(), totalGross, totalNet);
        String user = SecurityHelper.currentUsername().orElse("system");

        // Build master
        JournalEntryMaster journal = JournalEntryMaster.builder()
                .voucherNo(voucherNo)
                .voucherDate(payrollRun.getRunDate())
                .voucherType(VoucherType.PAYROLL_VOUCHER)
                .voucherStatus("POSTED")
                .isPosted(true)
                .postedBy(user)
                .postedAt(LocalDateTime.now())
                .totalDebit(debitTotal)
                .totalCredit(creditTotal)
                .totalAmount(totalGross)
                .allocatedAmount(BigDecimal.ZERO)
                .narration(narration)
                .referenceNo(payrollRun.getPayrollMonth())
                .build();

        journal.setOrganization(payrollRun.getOrganization());
        journal.setCreatedBy(user);
        journal.setCreatedAt(LocalDateTime.now());
        journal.setUpdatedBy(user);
        journal.setUpdatedAt(LocalDateTime.now());

        JournalEntryMaster savedJournal = journalRepo.save(journal);

        // Build lines
        AtomicInteger lineNo = new AtomicInteger(1);

        // ── DEBIT lines (Expense) ──────────────────────────────────────────────
        addLine(savedJournal, mapping.getBasicSalaryAccount(),    totalBasic,      lineNo, "Basic Salary Expense — " + payrollRun.getPayrollMonth(),      true,  orgId, user);
        addLineIfNonZero(savedJournal, mapping.getHouseRentAccount(),   totalHouseRent,  lineNo, "House Rent Allowance — " + payrollRun.getPayrollMonth(),        true,  orgId, user);
        addLineIfNonZero(savedJournal, mapping.getMedicalAccount(),     totalMedical,    lineNo, "Medical Allowance — " + payrollRun.getPayrollMonth(),            true,  orgId, user);
        addLineIfNonZero(savedJournal, mapping.getTransportAccount(),   totalTransport,  lineNo, "Transport Allowance — " + payrollRun.getPayrollMonth(),          true,  orgId, user);
        addLineIfNonZero(savedJournal, mapping.getOvertimeAccount(),    totalOvertime,   lineNo, "Overtime Expense — " + payrollRun.getPayrollMonth(),             true,  orgId, user);
        addLineIfNonZero(savedJournal, mapping.getOtherAllowancesAccount(), totalOtherAllow, lineNo, "Other Allowances — " + payrollRun.getPayrollMonth(),         true,  orgId, user);

        // ── CREDIT lines (Payable) ──────────────────────────────────────────────
        addLine(savedJournal, mapping.getSalaryPayableAccount(),        salaryPayable,   lineNo, "Salary Payable — " + payrollRun.getPayrollMonth(),              false, orgId, user);
        addLineIfNonZero(savedJournal, mapping.getIncomeTaxPayableAccount(),    totalTax,   lineNo, "Income Tax Payable — " + payrollRun.getPayrollMonth(),        false, orgId, user);
        addLineIfNonZero(savedJournal, mapping.getProvidentFundPayableAccount(),totalPF,    lineNo, "Provident Fund Payable — " + payrollRun.getPayrollMonth(),    false, orgId, user);
        addLineIfNonZero(savedJournal, mapping.getOtherDeductionsPayableAccount(), totalOtherDeduc.add(totalLoan), lineNo, "Other Deductions Payable — " + payrollRun.getPayrollMonth(), false, orgId, user);

        log.info("Payroll journal {} created with {} lines for run #{}",
                voucherNo, lineNo.get() - 1, payrollRun.getId());

        return savedJournal;
    }

    private void addLine(JournalEntryMaster journal, ChartOfAccount account,
                         BigDecimal amount, AtomicInteger lineNo,
                         String narration, boolean isDebit,
                         Long orgId, String user) {
        if (account == null) {
            throw new IllegalStateException("GL account not configured for: " + narration);
        }

        JournalEntryLine line = JournalEntryLine.builder()
                .journalEntry(journal)
                .account(account)
                .lineNumber(lineNo.getAndIncrement())
                .entryType(isDebit ? JournalEntryLine.EntryType.DEBIT : JournalEntryLine.EntryType.CREDIT)
                .amount(amount)
                .narration(narration)
                .sourceType("PAYROLL")
                .sourceId(journal.getId())
                .isTaxLine(false)
                .build();
        line.setCreatedBy(user);
        line.setCreatedAt(LocalDateTime.now());
        journalLineRepo.save(line);
    }

    private void addLineIfNonZero(JournalEntryMaster journal, ChartOfAccount account,
                                  BigDecimal amount, AtomicInteger lineNo,
                                  String narration, boolean isDebit,
                                  Long orgId, String user) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return;
        addLine(journal, account, amount, lineNo, narration, isDebit, orgId, user);
    }

    private BigDecimal sum(List<PayrollRunLine> lines,
                           java.util.function.Function<PayrollRunLine, BigDecimal> getter) {
        return lines.stream()
                .map(getter)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateMapping(PayrollAccountMapping m) {
        if (m.getBasicSalaryAccount()    == null) throw new IllegalStateException("Basic Salary expense account not mapped.");
        if (m.getSalaryPayableAccount()  == null) throw new IllegalStateException("Salary Payable account not mapped.");
        if (m.getIncomeTaxPayableAccount() == null) throw new IllegalStateException("Income Tax Payable account not mapped.");
        if (m.getProvidentFundPayableAccount() == null) throw new IllegalStateException("Provident Fund Payable account not mapped.");
    }

    private String generateVoucherNo(Long orgId) {
        String yearMonth = java.time.YearMonth.now().toString().replace("-", "");
        Integer next = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(CAST(SUBSTRING(voucher_no FROM '-(\\d+)$') AS INTEGER)), 0) + 1 " +
                        "FROM acc_journal_entry_master " +
                        "WHERE organization_id = ? AND voucher_no LIKE 'PYR-%'",
                Integer.class, orgId);
        return String.format("PYR-%s-%05d", yearMonth, next != null ? next : 1);
    }
}