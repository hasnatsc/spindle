package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FinancialReportService — comprehensive financial reporting suite.
 * <p>
 * All reports are organization-scoped and read-only.
 * <p>
 * Reports provided:
 * 1. dayBook(startDate, endDate)             — chronological transaction listing
 * 2. voucherRegister(voucherType, startDate, endDate) — filtered voucher log
 * 3. cashFlowStatement(startDate, endDate)   — direct-method cash flow
 * 4. bankBook(bankAccountId, startDate, endDate)  — bank transactions
 * 5. cashBook(cashAccountId, startDate, endDate)  — cash transactions
 * 6. partyLedger(partyId, partyType, startDate, endDate) — customer/supplier detailed ledger
 * 7. comparativePL(startDate, endDate, prevStartDate, prevEndDate) — period-over-period P&L
 * 8. comparativeTrialBalance(asOfDate, prevAsOfDate)  — comparative trial balance
 * 9. taxSummary(startDate, endDate)          — input/output tax summary
 * 10. financialKpis(asOfDate)                — key financial ratios
 * 11. costCenterSummary(startDate, endDate)  — transactions by cost center
 * 12. budgetVsActual(fiscalYear, period)     — budget comparison (when budget data exists)
 * 13. accountTransactionSummary(accountId, startDate, endDate) — account movement summary
 * 14. subAccountSummary(subAccountType, orgId) — party balance summary by type
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialReportService {

    private final JdbcTemplate jdbc;

    // =========================================================================
    // 1. DAY BOOK — chronological listing of ALL posted transactions
    // =========================================================================

    /**
     * Day Book — every posted journal entry line in date order.
     * Returns: { lines:[], summary:{totalDebit,totalCredit,count} }
     */
    public Map<String, Object> dayBook(String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        String sql = """
                SELECT
                    TO_CHAR(jem.voucher_date, 'DD-Mon-YYYY') AS voucher_date,
                    jem.voucher_no,
                    jem.voucher_type,
                    jem.voucher_status,
                    coa.account_code,
                    coa.account_name,
                    CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END AS debit,
                    CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END AS credit,
                    COALESCE(jel.narration, jem.narration, '—') AS narration,
                    COALESCE(jem.reference_no, '—') AS reference_no
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                JOIN acc_chart_of_accounts coa    ON coa.id = jel.account_id
                WHERE jem.organization_id = ?
                  AND jem.is_posted = true
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                ORDER BY jem.voucher_date, jem.id, jel.line_number
                """;

        List<Map<String, Object>> lines = toCamelCase(jdbc.queryForList(sql, orgId, sd, ed));

        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        for (Map<String, Object> r : lines) {
            totalDr = totalDr.add(toBD(r, "debit"));
            totalCr = totalCr.add(toBD(r, "credit"));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDebit", totalDr);
        summary.put("totalCredit", totalCr);
        summary.put("count", lines.size());
        summary.put("startDate", sd);
        summary.put("endDate", ed);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lines", lines);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 2. VOUCHER REGISTER — filtered list of vouchers by type
    // =========================================================================

    /**
     * Voucher Register — all vouchers filtered by type and date range.
     * voucherType: null/empty = all types
     */
    public Map<String, Object> voucherRegister(String voucherType, String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        String typeFilter = "";
        List<Object> params = new ArrayList<>();
        params.add(orgId);
        if (voucherType != null && !voucherType.isBlank()) {
            typeFilter = " AND jem.voucher_type = ? ";
            params.add(voucherType);
        }
        params.add(sd);
        params.add(ed);

        String sql = """
                SELECT
                    jem.id,
                    jem.voucher_no,
                    jem.voucher_type,
                    TO_CHAR(jem.voucher_date, 'DD-Mon-YYYY') AS voucher_date,
                    jem.voucher_status,
                    COALESCE(s.sub_account_code || ' — ' || s.sub_account_name, '—') AS party_name,
                    COALESCE(jem.total_amount, 0) AS total_amount,
                    COALESCE(TO_CHAR(jem.due_date, 'DD-Mon-YYYY'), '—') AS due_date,
                    COALESCE(jem.payment_mode, '—') AS payment_mode,
                    COALESCE(jem.reference_no, '—') AS reference_no,
                    COALESCE(jem.narration, '—') AS narration,
                    jem.created_by,
                    TO_CHAR(jem.created_at, 'DD-Mon-YYYY HH24:MI') AS created_at
                FROM acc_journal_entry_master jem
                LEFT JOIN acc_chart_of_accounts_sub s ON s.id = jem.party_id
                WHERE jem.organization_id = ?
                """ + typeFilter + """
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                  AND jem.voucher_status NOT IN ('CANCELLED')
                ORDER BY jem.voucher_date DESC, jem.id DESC
                """;

        List<Map<String, Object>> rows = toCamelCase(jdbc.queryForList(sql, params.toArray()));

        // Compute totals by type
        Map<String, Map<String, Object>> typeTotals = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            String vt = String.valueOf(r.getOrDefault("voucher_type", "OTHER"));
            typeTotals.putIfAbsent(vt, new LinkedHashMap<>());
            typeTotals.get(vt).merge("count", 1, (a, b) -> ((Integer) a) + 1);
            BigDecimal amt = toBD(r, "total_amount");
            typeTotals.get(vt).merge("amount", amt, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            grandTotal = grandTotal.add(amt);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalVouchers", rows.size());
        summary.put("grandTotal", grandTotal);
        summary.put("typeBreakdown", typeTotals);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vouchers", rows);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 3. CASH FLOW STATEMENT (direct method)
    // =========================================================================

    /**
     * Cash Flow Statement — direct method.
     * Categories cash movements into Operating / Investing / Financing.
     * <p>
     * Uses account codes by convention:
     * OPERATING:  1- (current assets), 2- (current liabilities), 4- (revenue), 5- (expense)
     * INVESTING:  1-FIXED, 1-NCA (non-current assets)
     * FINANCING:  2-LT (long-term liabilities), 3- (equity)
     * <p>
     * Bank & Cash accounts themselves are excluded from the movement calculation.
     */
    public Map<String, Object> cashFlowStatement(String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        // Get all posted journal entry lines grouped by account for the period
        // Exclude CASH/BANK sub-accounts (we're tracking movement TO/FROM them)
        String sql = """
                SELECT
                    coa.account_code,
                    coa.account_name,
                    coa.account_type,
                    COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END), 0) AS total_dr,
                    COALESCE(SUM(CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END), 0) AS total_cr
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                JOIN acc_chart_of_accounts coa    ON coa.id = jel.account_id
                WHERE jem.organization_id = ?
                  AND jem.is_posted = true
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                GROUP BY coa.id, coa.account_code, coa.account_name, coa.account_type
                ORDER BY coa.account_code
                """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId, sd, ed);

        // Categorize accounts into cash flow sections
        List<Map<String, Object>> operating = new ArrayList<>();
        List<Map<String, Object>> investing = new ArrayList<>();
        List<Map<String, Object>> financing = new ArrayList<>();

        BigDecimal opTotal = BigDecimal.ZERO;
        BigDecimal invTotal = BigDecimal.ZERO;
        BigDecimal finTotal = BigDecimal.ZERO;

        for (Map<String, Object> r : rows) {
            String code = String.valueOf(r.getOrDefault("account_code", ""));
            String type = String.valueOf(r.getOrDefault("account_type", ""));
            BigDecimal dr = toBD(r, "total_dr");
            BigDecimal cr = toBD(r, "total_cr");
            BigDecimal net = dr.subtract(cr);

            if (net.compareTo(BigDecimal.ZERO) == 0) continue;

            String section;
            BigDecimal sectionAmt;

            // Simple heuristic based on account type and code prefix
            if ("REVENUE".equals(type) || "EXPENSE".equals(type)) {
                section = "OPERATING";
                sectionAmt = "REVENUE".equals(type) ? cr.subtract(dr) : dr.subtract(cr); // Rev increases cash, Exp decreases
            } else if ("ASSET".equals(type) && code.startsWith("1")) {
                // Current assets (typically 1xxx range) = operating, non-current = investing
                section = code.length() >= 4 && code.charAt(0) == '1' && code.charAt(1) >= '5'
                        ? "INVESTING" : "OPERATING";
                sectionAmt = cr.subtract(dr); // Asset decrease = cash inflow
            } else if ("LIABILITY".equals(type)) {
                section = code.startsWith("3") ? "FINANCING" : "OPERATING";
                sectionAmt = dr.subtract(cr); // Liability increase = cash inflow
            } else if ("EQUITY".equals(type)) {
                section = "FINANCING";
                sectionAmt = dr.subtract(cr);
            } else {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("accountCode", code);
            item.put("accountName", r.get("account_name"));
            item.put("accountType", type);
            item.put("amount", sectionAmt);

            switch (section) {
                case "OPERATING" -> {
                    operating.add(item);
                    opTotal = opTotal.add(sectionAmt);
                }
                case "INVESTING" -> {
                    investing.add(item);
                    invTotal = invTotal.add(sectionAmt);
                }
                case "FINANCING" -> {
                    financing.add(item);
                    finTotal = finTotal.add(sectionAmt);
                }
            }
        }

        // Get opening and closing cash/bank balance for the period
        BigDecimal openingCash = getCashBankBalance(orgId, LocalDate.parse(sd).minusDays(1));
        BigDecimal closingCash = getCashBankBalance(orgId, LocalDate.parse(ed));
        BigDecimal netChange = closingCash.subtract(openingCash);

        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("operating", Map.of("items", operating, "total", opTotal));
        sections.put("investing", Map.of("items", investing, "total", invTotal));
        sections.put("financing", Map.of("items", financing, "total", finTotal));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openingCash", openingCash);
        summary.put("closingCash", closingCash);
        summary.put("netCashChange", netChange);
        summary.put("calculatedChange", opTotal.add(invTotal).add(finTotal));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sections", sections);
        result.put("summary", summary);
        return result;
    }

    /**
     * Helper: sum current_balance of BANK + CASH sub-accounts at a given date
     */
    private BigDecimal getCashBankBalance(Long orgId, LocalDate asOf) {
        String sql = """
                SELECT COALESCE(SUM(current_balance), 0) AS balance
                FROM acc_chart_of_accounts_sub
                WHERE organization_id = ?
                  AND sub_account_type IN ('BANK','CASH')
                  AND is_active = true
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId);
        return rows.isEmpty() ? BigDecimal.ZERO : toBD(rows.get(0), "balance");
    }

    // =========================================================================
    // 4. BANK BOOK
    // =========================================================================

    /**
     * Bank Book — all transactions hitting a specific bank sub-account.
     */
    public Map<String, Object> bankBook(Long bankSubAccountId, String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        // Get bank account info
        String infoSql = """
                SELECT sub_account_code, sub_account_name, account_number, bank_name, current_balance
                FROM acc_chart_of_accounts_sub
                WHERE id = ? AND organization_id = ? AND sub_account_type = 'BANK'
                """;
        List<Map<String, Object>> infoRows = toCamelCase(jdbc.queryForList(infoSql, bankSubAccountId, orgId));
        if (infoRows.isEmpty()) return Map.of("error", "Bank account not found");

        // Get opening balance before startDate
        BigDecimal openingBal = getSubAccountBalanceBefore(orgId, bankSubAccountId, sd);

        // Get transactions
        String txSql = """
                SELECT
                    TO_CHAR(jem.voucher_date, 'DD-Mon-YYYY') AS voucher_date,
                    jem.voucher_no,
                    jem.voucher_type,
                    CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END AS debit,
                    CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END AS credit,
                    COALESCE(jel.narration, jem.narration, '—') AS narration,
                    COALESCE(jem.reference_no, '—') AS reference_no,
                    COALESCE(jem.payment_mode, '—') AS payment_mode,
                    COALESCE(jem.cheque_number, '—') AS cheque_number
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                WHERE jel.organization_id = ?
                  AND jel.sub_account_id = ?
                  AND jem.is_posted = true
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                ORDER BY jem.voucher_date, jem.id, jel.line_number
                """;

        List<Map<String, Object>> lines = toCamelCase(jdbc.queryForList(txSql, orgId, bankSubAccountId, sd, ed));

        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        BigDecimal running = openingBal;
        for (Map<String, Object> r : lines) {
            BigDecimal dr = toBD(r, "debit");
            BigDecimal cr = toBD(r, "credit");
            running = running.add(dr).subtract(cr);
            r.put("balance", running);
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountInfo", infoRows.get(0));
        result.put("lines", lines);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openingBalance", openingBal);
        summary.put("totalDebit", totalDr);
        summary.put("totalCredit", totalCr);
        summary.put("closingBalance", running);
        result.put("summary", summary);

        return result;
    }

    // =========================================================================
    // 5. CASH BOOK
    // =========================================================================

    /**
     * Cash Book — all transactions hitting a specific cash sub-account.
     */
    public Map<String, Object> cashBook(Long cashSubAccountId, String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        String infoSql = """
                SELECT sub_account_code, sub_account_name, location, custodian, current_balance
                FROM acc_chart_of_accounts_sub
                WHERE id = ? AND organization_id = ? AND sub_account_type = 'CASH'
                """;
        List<Map<String, Object>> infoRows = jdbc.queryForList(infoSql, cashSubAccountId, orgId);
        if (infoRows.isEmpty()) return Map.of("error", "Cash account not found");

        BigDecimal openingBal = getSubAccountBalanceBefore(orgId, cashSubAccountId, sd);

        String txSql = """
                SELECT
                    TO_CHAR(jem.voucher_date, 'DD-Mon-YYYY') AS voucher_date,
                    jem.voucher_no,
                    jem.voucher_type,
                    CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END AS debit,
                    CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END AS credit,
                    COALESCE(jel.narration, jem.narration, '—') AS narration,
                    COALESCE(jem.reference_no, '—') AS reference_no,
                    COALESCE(jem.payment_mode, '—') AS payment_mode
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                WHERE jel.organization_id = ?
                  AND jel.sub_account_id = ?
                  AND jem.is_posted = true
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                ORDER BY jem.voucher_date, jem.id, jel.line_number
                """;

        List<Map<String, Object>> lines = toCamelCase(jdbc.queryForList(txSql, orgId, cashSubAccountId, sd, ed));

        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        BigDecimal running = openingBal;
        for (Map<String, Object> r : lines) {
            BigDecimal dr = toBD(r, "debit");
            BigDecimal cr = toBD(r, "credit");
            running = running.add(dr).subtract(cr);
            r.put("balance", running);
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountInfo", infoRows.get(0));
        result.put("lines", lines);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openingBalance", openingBal);
        summary.put("totalDebit", totalDr);
        summary.put("totalCredit", totalCr);
        summary.put("closingBalance", running);
        result.put("summary", summary);

        return result;
    }

    /**
     * Opening balance for a sub-account before a given date
     */
    private BigDecimal getSubAccountBalanceBefore(Long orgId, Long subAccountId, String beforeDate) {
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END)
                             - SUM(CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END), 0) AS ob
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                WHERE jel.organization_id = ?
                  AND jel.sub_account_id = ?
                  AND jem.is_posted = true
                  AND jem.voucher_date < ?::date
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId, subAccountId, beforeDate);
        return rows.isEmpty() ? BigDecimal.ZERO : toBD(rows.get(0), "ob");
    }

    // =========================================================================
    // 6. PARTY LEDGER (Customer / Supplier detailed)
    // =========================================================================

    /**
     * Party Ledger — all transactions for a specific customer or supplier.
     * Includes ageing info and running balance.
     */
    public Map<String, Object> partyLedger(Long partyId, String partyType, String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        // Party info
        String infoSql = """
                SELECT sub_account_code, sub_account_name, current_balance, contact_person, contact_phone, address, city
                FROM acc_chart_of_accounts_sub
                WHERE id = ? AND organization_id = ?
                """;
        List<Map<String, Object>> infoRows = jdbc.queryForList(infoSql, partyId, orgId);
        if (infoRows.isEmpty()) return Map.of("error", "Party not found");

        // Opening balance before startDate
        BigDecimal openingBal = BigDecimal.ZERO;
        String obSql = """
                SELECT COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END)
                             - SUM(CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END), 0) AS ob
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                WHERE jel.organization_id = ?
                  AND jem.party_id = ?
                  AND jem.party_type = ?
                  AND jem.is_posted = true
                  AND jem.voucher_date < ?::date
                """;
        List<Map<String, Object>> obRows = jdbc.queryForList(obSql, orgId, partyId, partyType, sd);
        if (!obRows.isEmpty()) openingBal = toBD(obRows.get(0), "ob");

        // Transactions
        String txSql = """
                SELECT
                    TO_CHAR(jem.voucher_date, 'DD-Mon-YYYY') AS voucher_date,
                    jem.voucher_no,
                    jem.voucher_type,
                    CASE WHEN jem.party_type = 'CUSTOMER' THEN
                        CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END
                        ELSE CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END
                    END AS debit,
                    CASE WHEN jem.party_type = 'CUSTOMER' THEN
                        CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END
                        ELSE CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END
                    END AS credit,
                    COALESCE(jel.narration, jem.narration, '—') AS narration,
                    COALESCE(jem.reference_no, '—') AS reference_no,
                    COALESCE(TO_CHAR(jem.due_date, 'DD-Mon-YYYY'), '—') AS due_date
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                WHERE jel.organization_id = ?
                  AND jem.party_id = ?
                  AND jem.party_type = ?
                  AND jem.is_posted = true
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                ORDER BY jem.voucher_date, jem.id, jel.line_number
                """;

        List<Map<String, Object>> lines = toCamelCase(jdbc.queryForList(txSql, orgId, partyId, partyType, sd, ed));

        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        BigDecimal running = openingBal;
        for (Map<String, Object> r : lines) {
            BigDecimal dr = toBD(r, "debit");
            BigDecimal cr = toBD(r, "credit");
            running = running.add(dr).subtract(cr);
            r.put("balance", running);
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("partyInfo", infoRows.get(0));
        result.put("lines", lines);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openingBalance", openingBal);
        summary.put("totalDebit", totalDr);
        summary.put("totalCredit", totalCr);
        summary.put("closingBalance", running);
        result.put("summary", summary);

        return result;
    }

    // =========================================================================
    // 7. COMPARATIVE PROFIT & LOSS (period-over-period)
    // =========================================================================

    /**
     * Comparative P&L — shows revenue and expense for current period
     * and previous period side by side with change %.
     */
    public Map<String, Object> comparativePL(String startDate, String endDate,
                                             String prevStartDate, String prevEndDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();
        String ps = (prevStartDate != null) ? prevStartDate : LocalDate.parse(sd).minusMonths(1).withDayOfMonth(1).toString();
        String pe = (prevEndDate != null) ? prevEndDate : LocalDate.parse(sd).minusDays(1).toString();

        String sql = """
                SELECT
                    coa.id,
                    coa.account_code,
                    coa.account_name,
                    coa.account_type,
                    COALESCE(SUM(CASE WHEN jel.entry_type='CREDIT' AND jem.voucher_date BETWEEN ?::date AND ?::date
                               THEN jel.amount ELSE 0 END)
                           - SUM(CASE WHEN jel.entry_type='DEBIT'  AND jem.voucher_date BETWEEN ?::date AND ?::date
                               THEN jel.amount ELSE 0 END), 0) AS current_amount,
                    COALESCE(SUM(CASE WHEN jel.entry_type='CREDIT' AND jem.voucher_date BETWEEN ?::date AND ?::date
                               THEN jel.amount ELSE 0 END)
                           - SUM(CASE WHEN jel.entry_type='DEBIT'  AND jem.voucher_date BETWEEN ?::date AND ?::date
                               THEN jel.amount ELSE 0 END), 0) AS previous_amount
                FROM acc_chart_of_accounts coa
                LEFT JOIN acc_journal_entry_lines jel   ON jel.account_id = coa.id
                LEFT JOIN acc_journal_entry_master jem  ON jem.id = jel.journal_entry_id AND jem.is_posted = true
                WHERE coa.organization_id = ?
                  AND coa.is_active = true
                  AND coa.account_type IN ('REVENUE','EXPENSE')
                GROUP BY coa.id, coa.account_code, coa.account_name, coa.account_type
                HAVING ABS(COALESCE(SUM(jel.amount), 0)) > 0
                ORDER BY coa.account_type, coa.account_code
                """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, sd, ed, ps, pe, orgId);

        List<Map<String, Object>> revenueLines = new ArrayList<>();
        List<Map<String, Object>> expenseLines = new ArrayList<>();
        BigDecimal totalRev = BigDecimal.ZERO, totalRevPrev = BigDecimal.ZERO;
        BigDecimal totalExp = BigDecimal.ZERO, totalExpPrev = BigDecimal.ZERO;

        for (Map<String, Object> r : rows) {
            String type = String.valueOf(r.getOrDefault("account_type", ""));
            BigDecimal cur = toBD(r, "current_amount");
            BigDecimal prev = toBD(r, "previous_amount");
            BigDecimal changePct = prev.compareTo(BigDecimal.ZERO) != 0
                    ? cur.subtract(prev).abs().multiply(BigDecimal.valueOf(100))
                      .divide(prev.abs(), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            r.put("changePercent", changePct);

            if ("REVENUE".equals(type)) {
                revenueLines.add(r);
                totalRev = totalRev.add(cur);
                totalRevPrev = totalRevPrev.add(prev);
            } else {
                expenseLines.add(r);
                totalExp = totalExp.add(cur);
                totalExpPrev = totalExpPrev.add(prev);
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("currentRevenue", totalRev);
        summary.put("previousRevenue", totalRevPrev);
        summary.put("revenueChangePct", calcChangePct(totalRev, totalRevPrev));
        summary.put("currentExpense", totalExp);
        summary.put("previousExpense", totalExpPrev);
        summary.put("expenseChangePct", calcChangePct(totalExp, totalExpPrev));
        summary.put("currentNetProfit", totalRev.subtract(totalExp));
        summary.put("previousNetProfit", totalRevPrev.subtract(totalExpPrev));
        summary.put("netProfitChangePct", calcChangePct(totalRev.subtract(totalExp), totalRevPrev.subtract(totalExpPrev)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("revenueLines", revenueLines);
        result.put("expenseLines", expenseLines);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 8. COMPARATIVE TRIAL BALANCE
    // =========================================================================

    /**
     * Comparative Trial Balance — shows balances at two dates side by side.
     */
    public Map<String, Object> comparativeTrialBalance(String asOfDate, String prevAsOfDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String cutoff = (asOfDate != null && !asOfDate.isBlank()) ? asOfDate : LocalDate.now().toString();
        String prevCutoff = (prevAsOfDate != null && !prevAsOfDate.isBlank())
                ? prevAsOfDate : LocalDate.parse(cutoff).minusMonths(1).toString();

        String sql = """
                SELECT
                    coa.account_code,
                    coa.account_name,
                    coa.account_type,
                    coa.level,
                    COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  AND jem.voucher_date <= ?::date THEN jel.amount ELSE 0 END)
                           - SUM(CASE WHEN jel.entry_type='CREDIT' AND jem.voucher_date <= ?::date THEN jel.amount ELSE 0 END), 0)
                           AS current_balance,
                    COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  AND jem.voucher_date <= ?::date THEN jel.amount ELSE 0 END)
                           - SUM(CASE WHEN jel.entry_type='CREDIT' AND jem.voucher_date <= ?::date THEN jel.amount ELSE 0 END), 0)
                           AS previous_balance
                FROM acc_chart_of_accounts coa
                LEFT JOIN acc_journal_entry_lines jel   ON jel.account_id = coa.id
                LEFT JOIN acc_journal_entry_master jem  ON jem.id = jel.journal_entry_id AND jem.is_posted = true
                WHERE coa.organization_id = ? AND coa.is_active = true
                GROUP BY coa.id, coa.account_code, coa.account_name, coa.account_type, coa.level
                HAVING ABS(COALESCE(SUM(jel.amount), 0)) > 0
                ORDER BY coa.account_type, coa.account_code
                """;

        List<Map<String, Object>> rows = toCamelCase(jdbc.queryForList(sql, cutoff, cutoff, prevCutoff, prevCutoff, orgId));

        BigDecimal totalCurDr = BigDecimal.ZERO, totalCurCr = BigDecimal.ZERO;
        BigDecimal totalPrev = BigDecimal.ZERO;

        for (Map<String, Object> r : rows) {
            BigDecimal cur = toBD(r, "current_balance");
            BigDecimal prev = toBD(r, "previous_balance");
            String type = String.valueOf(r.getOrDefault("account_type", ""));
            if ("ASSET".equals(type) || "EXPENSE".equals(type)) {
                totalCurDr = totalCurDr.add(cur);
                totalCurCr = totalCurCr.add(prev);
            } else {
                totalCurCr = totalCurCr.add(cur);
                totalPrev = totalPrev.add(prev);
            }
            r.put("changePercent", calcChangePct(cur, prev));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("currentTotalDr", totalCurDr);
        summary.put("currentTotalCr", totalCurCr);
        summary.put("accountCount", rows.size());
        summary.put("balanced", totalCurDr.compareTo(totalCurCr) == 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accounts", rows);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 9. TAX SUMMARY
    // =========================================================================

    /**
     * Tax Summary — input/output VAT/Tax by rate.
     * Uses JournalEntryLine.taxCode and isTaxLine flag.
     */
    public Map<String, Object> taxSummary(String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        String sql = """
                SELECT
                    COALESCE(jel.tax_code, 'NO-TAX') AS tax_code,
                    COALESCE(coa.account_code, '—') AS account_code,
                    COALESCE(coa.account_name, '—') AS account_name,
                    SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END) AS debit_total,
                    SUM(CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END) AS credit_total,
                    COUNT(*) AS line_count
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                LEFT JOIN acc_chart_of_accounts coa ON coa.id = jel.account_id
                WHERE jel.organization_id = ?
                  AND jem.is_posted = true
                  AND jel.is_tax_line = true
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                GROUP BY jel.tax_code, coa.account_code, coa.account_name
                ORDER BY jel.tax_code
                """;

        List<Map<String, Object>> lines = toCamelCase(jdbc.queryForList(sql, orgId, sd, ed));

        // Compute totals by tax code
        Map<String, Map<String, Object>> byCode = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Map<String, Object> r : lines) {
            String code = String.valueOf(r.getOrDefault("tax_code", "NO-TAX"));
            byCode.putIfAbsent(code, new LinkedHashMap<>());
            byCode.get(code).merge("debit", toBD(r, "debit_total"), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            byCode.get(code).merge("credit", toBD(r, "credit_total"), (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
            byCode.get(code).merge("count", 1, (a, b) -> ((Integer) a) + 1);
            grandTotal = grandTotal.add(toBD(r, "debit_total")).add(toBD(r, "credit_total"));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTaxLines", lines.size());
        summary.put("grandTotal", grandTotal);
        summary.put("taxCodeCount", byCode.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lines", lines);
        result.put("byCode", byCode);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 10. FINANCIAL KPIs
    // =========================================================================

    /**
     * Financial KPIs — key ratios and metrics as of a date.
     * <p>
     * Returns:
     * - currentRatio = currentAssets / currentLiabilities
     * - quickRatio   = (currentAssets - inventory) / currentLiabilities
     * - debtRatio    = totalLiabilities / totalAssets
     * - profitMargin = netProfit / totalRevenue (for trailing 12 months)
     * - arTurnover   = totalRevenue / avgReceivables
     * - apTurnover   = totalPurchases / avgPayables
     * - workingCapital = currentAssets - currentLiabilities
     */
    public Map<String, Object> financialKpis(String asOfDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String cutoff = (asOfDate != null && !asOfDate.isBlank()) ? asOfDate : LocalDate.now().toString();
        String yearStart = LocalDate.parse(cutoff).minusMonths(12).toString();

        // Get account balances by type
        String balSql = """
                SELECT coa.account_type,
                       COALESCE(SUM(CASE WHEN coa.account_type IN ('ASSET','EXPENSE')
                                   THEN CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END
                                          - CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END
                                   ELSE CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END
                                          - CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END
                              END), 0) AS balance
                FROM acc_chart_of_accounts coa
                LEFT JOIN acc_journal_entry_lines jel   ON jel.account_id = coa.id
                LEFT JOIN acc_journal_entry_master jem  ON jem.id = jel.journal_entry_id
                    AND jem.is_posted = true AND jem.voucher_date <= ?::date
                WHERE coa.organization_id = ? AND coa.is_active = true
                GROUP BY coa.account_type
                """;

        List<Map<String, Object>> balRows = jdbc.queryForList(balSql, cutoff, orgId);
        Map<String, BigDecimal> balances = new HashMap<>();
        for (Map<String, Object> r : balRows) {
            balances.put(String.valueOf(r.get("account_type")), toBD(r, "balance"));
        }

        // Get revenue for trailing 12 months
        String revSql = """
                SELECT COALESCE(SUM(jel.amount), 0) AS total
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                JOIN acc_chart_of_accounts coa ON coa.id = jel.account_id
                WHERE jel.organization_id = ?
                  AND jem.is_posted = true
                  AND coa.account_type = 'REVENUE'
                  AND jel.entry_type = 'CREDIT'
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                """;
        List<Map<String, Object>> revRows = jdbc.queryForList(revSql, orgId, yearStart, cutoff);
        BigDecimal totalRevenue = revRows.isEmpty() ? BigDecimal.ZERO : toBD(revRows.get(0), "total");

        // Get total purchase (expense) for trailing 12 months
        String expSql = """
                SELECT COALESCE(SUM(jel.amount), 0) AS total
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                JOIN acc_chart_of_accounts coa ON coa.id = jel.account_id
                WHERE jel.organization_id = ?
                  AND jem.is_posted = true
                  AND coa.account_type = 'EXPENSE'
                  AND jel.entry_type = 'DEBIT'
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                """;
        List<Map<String, Object>> expRows = jdbc.queryForList(expSql, orgId, yearStart, cutoff);
        BigDecimal totalExpense = expRows.isEmpty() ? BigDecimal.ZERO : toBD(expRows.get(0), "total");

        // Get inventory balance
        String invSql = """
                SELECT COALESCE(SUM(current_balance), 0) AS balance
                FROM acc_chart_of_accounts_sub
                WHERE organization_id = ? AND sub_account_type IN ('INVENTORY')
                   OR id IN (SELECT sub_account_id FROM acc_chart_of_accounts_sub
                             WHERE organization_id = ? AND main_account_id IN
                             (SELECT id FROM acc_chart_of_accounts WHERE account_code LIKE '1.1%' AND organization_id = ?))
                """;
        BigDecimal inventory = BigDecimal.ZERO;
        try {
            List<Map<String, Object>> invRows = jdbc.queryForList(invSql, orgId, orgId, orgId);
            if (!invRows.isEmpty()) inventory = toBD(invRows.get(0), "balance");
        } catch (Exception e) {
            log.warn("Inventory balance query failed (non-critical for KPIs): {}", e.getMessage());
        }

        BigDecimal currentAssets = balances.getOrDefault("ASSET", BigDecimal.ZERO);
        BigDecimal currentLiab = balances.getOrDefault("LIABILITY", BigDecimal.ZERO);
        BigDecimal equity = balances.getOrDefault("EQUITY", BigDecimal.ZERO);
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("currentRatio", safeDivide(currentAssets, currentLiab));
        kpis.put("quickRatio", safeDivide(currentAssets.subtract(inventory), currentLiab));
        kpis.put("debtRatio", safeDivide(currentLiab, currentAssets));
        kpis.put("profitMargin", safeDivide(netProfit, totalRevenue));
        kpis.put("returnOnEquity", safeDivide(netProfit, equity));
        kpis.put("workingCapital", currentAssets.subtract(currentLiab));
        kpis.put("totalRevenue12m", totalRevenue);
        kpis.put("totalExpense12m", totalExpense);
        kpis.put("netProfit12m", netProfit);
        kpis.put("totalAssets", currentAssets);
        kpis.put("totalLiabilities", currentLiab);
        kpis.put("totalEquity", equity);
        kpis.put("asOfDate", cutoff);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpis", kpis);
        return result;
    }

    // =========================================================================
    // 11. COST CENTER SUMMARY
    // =========================================================================

    /**
     * Cost Center Summary — total debits and credits grouped by cost center.
     */
    public Map<String, Object> costCenterSummary(String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        String sql = """
                SELECT
                    COALESCE(cc.cost_center_code, 'NO-CC') AS cost_center_code,
                    COALESCE(cc.name, 'No Cost Center') AS cost_center_name,
                    COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END), 0) AS total_debit,
                    COALESCE(SUM(CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END), 0) AS total_credit,
                    COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount
                                      WHEN jel.entry_type='CREDIT' THEN -jel.amount ELSE 0 END), 0) AS net_amount,
                    COUNT(DISTINCT jem.id) AS voucher_count
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                LEFT JOIN org_cost_centers cc ON cc.id = jel.cost_center_id
                WHERE jel.organization_id = ?
                  AND jem.is_posted = true
                  AND jem.voucher_date BETWEEN ?::date AND ?::date
                GROUP BY cc.id, cc.cost_center_code, cc.cost_center_name
                ORDER BY net_amount DESC
                """;

        List<Map<String, Object>> rows = toCamelCase(jdbc.queryForList(sql, orgId, sd, ed));

        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            totalDr = totalDr.add(toBD(r, "total_debit"));
            totalCr = totalCr.add(toBD(r, "total_credit"));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDebit", totalDr);
        summary.put("totalCredit", totalCr);
        summary.put("count", rows.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 12. ACCOUNT TRANSACTION SUMMARY
    // =========================================================================

    /**
     * Account Transaction Summary — shows period movement, average balance,
     * and transaction count for a single COA account.
     */
    public Map<String, Object> accountTransactionSummary(Long accountId, String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        String infoSql = "SELECT account_code, account_name, account_type FROM acc_chart_of_accounts WHERE id = ? AND organization_id = ?";
        List<Map<String, Object>> infoRows = toCamelCase(jdbc.queryForList(infoSql, accountId, orgId));
        if (infoRows.isEmpty()) return Map.of("error", "Account not found");

        // Pre-period balance
        BigDecimal openingBal = BigDecimal.ZERO;
        String obSql = """
                SELECT COALESCE(SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE -amount END), 0) AS ob
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                WHERE jel.account_id = ? AND jel.organization_id = ?
                  AND jem.is_posted = true AND jem.voucher_date < ?::date
                """;
        List<Map<String, Object>> obRows = jdbc.queryForList(obSql, accountId, orgId, sd);
        if (!obRows.isEmpty()) openingBal = toBD(obRows.get(0), "ob");

        // Period summary
        String summarySql = """
                SELECT
                    COUNT(*) AS tx_count,
                    COALESCE(SUM(CASE WHEN entry_type='DEBIT'  THEN amount ELSE 0 END), 0) AS total_debit,
                    COALESCE(SUM(CASE WHEN entry_type='CREDIT' THEN amount ELSE 0 END), 0) AS total_credit,
                    MIN(jem.voucher_date) AS first_tx_date,
                    MAX(jem.voucher_date) AS last_tx_date
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                WHERE jel.account_id = ? AND jel.organization_id = ?
                  AND jem.is_posted = true AND jem.voucher_date BETWEEN ?::date AND ?::date
                """;
        List<Map<String, Object>> sumRows = toCamelCase(jdbc.queryForList(summarySql, accountId, orgId, sd, ed));

        BigDecimal closingBal = openingBal;
        if (!sumRows.isEmpty()) {
            BigDecimal td = toBD(sumRows.get(0), "total_debit");
            BigDecimal tc = toBD(sumRows.get(0), "total_credit");
            closingBal = openingBal.add(td).subtract(tc);
        }

        Map<String, Object> summary = sumRows.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(sumRows.get(0));
        summary.put("openingBalance", openingBal);
        summary.put("closingBalance", closingBal);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountInfo", infoRows.get(0));
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 13. SUB-ACCOUNT SUMMARY (balance by type)
    // =========================================================================

    /**
     * Sub-account Summary — lists all sub-accounts of a given type
     * with current balance and contact info.
     */
    public Map<String, Object> subAccountSummary(String subAccountType) {
        Long orgId = SecurityHelper.requireOrgId();
        String type = (subAccountType != null && !subAccountType.isBlank()) ? subAccountType : "CUSTOMER";

        String sql = """
                SELECT
                    id,
                    sub_account_code,
                    sub_account_name,
                    COALESCE(current_balance, 0) AS current_balance,
                    COALESCE(opening_balance, 0) AS opening_balance,
                    contact_person,
                    contact_phone,
                    contact_email,
                    city,
                    is_active
                FROM acc_chart_of_accounts_sub
                WHERE organization_id = ?
                  AND sub_account_type = ?
                  AND is_active = true
                ORDER BY sub_account_name
                """;
        List<Map<String, Object>> rows = toCamelCase(jdbc.queryForList(sql, orgId, type));

        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal totalOpening = BigDecimal.ZERO;
        int activeCount = 0;
        for (Map<String, Object> r : rows) {
            totalBalance = totalBalance.add(toBD(r, "current_balance"));
            totalOpening = totalOpening.add(toBD(r, "opening_balance"));
            if (Boolean.TRUE.equals(r.get("is_active"))) activeCount++;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBalance", totalBalance);
        summary.put("totalOpening", totalOpening);
        summary.put("accountCount", rows.size());
        summary.put("activeCount", activeCount);
        summary.put("subAccountType", type);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accounts", rows);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 14. BUDGET VS ACTUAL REPORT
    // =========================================================================

    /**
     * Budget vs Actual — compares budget heads with actual GL postings.
     * Requires budget data to exist, gracefully returns empty if not.
     */
    public Map<String, Object> budgetVsActual(Long budgetId, String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        String sd = (startDate != null && !startDate.isBlank()) ? startDate : LocalDate.now().withDayOfMonth(1).toString();
        String ed = (endDate != null && !endDate.isBlank()) ? endDate : LocalDate.now().toString();

        // Check if budget module tables exist
        String sql = """
                SELECT bh.id AS head_id,
                       bh.head_code,
                       bh.head_name,
                       bh.head_type,
                       COALESCE(bl.apr_amount, 0) AS budget_amount,
                       COALESCE((
                           SELECT SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount
                                           WHEN jel.entry_type='CREDIT' THEN -jel.amount ELSE 0 END)
                           FROM acc_journal_entry_lines jel
                           JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                           JOIN acc_chart_of_accounts coa ON coa.id = jel.account_id
                           WHERE jem.organization_id = ?
                             AND jem.is_posted = true
                             AND jem.voucher_date BETWEEN ?::date AND ?::date
                             AND coa.account_code LIKE bh.head_code || '%'
                       ), 0) AS actual_amount
                FROM bgt_budget_heads bh
                LEFT JOIN bgt_budget_lines bl ON bl.budget_head_id = bh.id
                    AND bl.budget_id = ?
                WHERE bh.organization_id = ? AND bh.is_active = true
                ORDER BY bh.head_code
                """;

        List<Map<String, Object>> rows;
        try {
            rows = toCamelCase(jdbc.queryForList(sql, orgId, sd, ed, budgetId, orgId));
        } catch (Exception e) {
            log.warn("Budget vs Actual query failed (budget data may not exist): {}", e.getMessage());
            return Map.of("error", "Budget data not available. Ensure budget module is initialized.",
                    "rows", List.of(), "summary", Map.of("totalBudget", 0, "totalActual", 0, "variance", 0));
        }

        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            BigDecimal bud = toBD(r, "budget_amount");
            BigDecimal act = toBD(r, "actual_amount");
            totalBudget = totalBudget.add(bud);
            totalActual = totalActual.add(act);
            r.put("variance", bud.subtract(act));
            r.put("utilizationPct", bud.compareTo(BigDecimal.ZERO) > 0
                    ? act.multiply(BigDecimal.valueOf(100)).divide(bud, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBudget", totalBudget);
        summary.put("totalActual", totalActual);
        summary.put("variance", totalBudget.subtract(totalActual));
        summary.put("variancePct", calcChangePct(totalActual, totalBudget));
        summary.put("headCount", rows.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Transforms all snake_case keys in every row to camelCase.
     * Applies in-place so both snake_case and camelCase keys coexist.
     */
    private List<Map<String, Object>> toCamelCase(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            List<String> keys = new ArrayList<>(row.keySet());
            for (String key : keys) {
                if (key.contains("_")) {
                    row.put(snakeToCamel(key), row.get(key));
                }
            }
        }
        return rows;
    }

    private String snakeToCamel(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private BigDecimal calcChangePct(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBD(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
