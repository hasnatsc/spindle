package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * GlReportService — General Ledger, Trial Balance, P&L, Balance Sheet.
 *
 * All four GL reports in one service; each method is independent.
 *
 * Endpoints wired from GlReportController:
 *   GET /accounts/ledger/data?accountId=&startDate=&endDate=
 *   GET /accounts/trial-balance/data?asOfDate=&showZeroBalance=
 *   GET /accounts/profit-loss/data?startDate=&endDate=&compareStartDate=&compareEndDate=
 *   GET /accounts/balance-sheet/data?asOfDate=&compareDate=
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlReportService {

    private final JdbcTemplate jdbc;

    // =========================================================================
    // 1. GENERAL LEDGER
    //    Returns: { accountName, lines:[], summary:{openingBalance,totalDebit,totalCredit,closingBalance} }
    // =========================================================================

    public Map<String, Object> generalLedger(Long accountId, String startDate, String endDate) {
        Long orgId = SecurityHelper.requireOrgId();
        Map<String, Object> result = new LinkedHashMap<>();

        // Account name
        String nameSql = "SELECT account_code || ' — ' || account_name AS display_name FROM acc_chart_of_accounts WHERE id = ? AND organization_id = ?";
        List<Map<String, Object>> nameRows = jdbc.queryForList(nameSql, accountId, orgId);
        result.put("accountName", nameRows.isEmpty() ? "" : nameRows.getFirst().get("display_name"));

        // Opening balance: SUM of all posted lines for this account BEFORE startDate
        BigDecimal openingBalance = BigDecimal.ZERO;
        if (startDate != null && !startDate.isBlank()) {
            String obSql = """
                SELECT
                  COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END)
                         - SUM(CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END), 0) AS ob
                FROM acc_journal_entry_lines jel
                JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
                WHERE jel.account_id = ? AND jel.organization_id = ?
                  AND jem.is_posted = true AND jem.voucher_date < ?::date
                """;
            List<Map<String, Object>> obRows = jdbc.queryForList(obSql, accountId, orgId, startDate);
            if (!obRows.isEmpty() && obRows.getFirst().get("ob") != null) {
                openingBalance = toBD(obRows.getFirst(), "ob");
            }
        }

        // Ledger lines
        String where = "WHERE jel.account_id = ? AND jel.organization_id = ? AND jem.is_posted = true";
        Object[] params;
        if (startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank()) {
            where += " AND jem.voucher_date BETWEEN ?::date AND ?::date ";
            params = new Object[]{accountId, orgId, startDate, endDate};
        } else if (startDate != null && !startDate.isBlank()) {
            where += " AND jem.voucher_date >= ?::date ";
            params = new Object[]{accountId, orgId, startDate};
        } else if (endDate != null && !endDate.isBlank()) {
            where += " AND jem.voucher_date <= ?::date ";
            params = new Object[]{accountId, orgId, endDate};
        } else {
            params = new Object[]{accountId, orgId};
        }

        String linesSql = """
            SELECT
                TO_CHAR(jem.voucher_date, 'DD-Mon-YYYY') AS voucher_date,
                jem.voucher_no,
                jem.voucher_type,
                CASE WHEN jel.entry_type = 'DEBIT'  THEN jel.amount ELSE 0 END AS debit,
                CASE WHEN jel.entry_type = 'CREDIT' THEN jel.amount ELSE 0 END AS credit,
                jel.narration,
                COALESCE(jem.reference_no, '—') AS reference_no
            FROM acc_journal_entry_lines jel
            JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
            """ + where + """
            ORDER BY jem.voucher_date, jem.id, jel.line_number
            """;

        List<Map<String, Object>> lines = jdbc.queryForList(linesSql, params);

        // Compute running balance
        BigDecimal running = openingBalance;
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        for (Map<String, Object> line : lines) {
            BigDecimal dr = toBD(line, "debit");
            BigDecimal cr = toBD(line, "credit");
            running = running.add(dr).subtract(cr);
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
            line.put("running_balance", running);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openingBalance",  openingBalance);
        summary.put("totalDebit",      totalDr);
        summary.put("totalCredit",     totalCr);
        summary.put("closingBalance",  running);

        result.put("lines",   lines);
        result.put("summary", summary);
        return result;
    }

    // =========================================================================
    // 2. TRIAL BALANCE
    //    Returns: { accounts:[], summary:{totalDebit, totalCredit, accountCount} }
    // =========================================================================

    public Map<String, Object> trialBalance(String asOfDate, boolean showZeroBalance) {
        Long orgId = SecurityHelper.requireOrgId();
        String cutoff = (asOfDate != null && !asOfDate.isBlank()) ? asOfDate : LocalDate.now().toString();

        String sql = """
            SELECT
                coa.account_code,
                coa.account_name,
                coa.account_type,
                coa.level,
                COALESCE(SUM(CASE WHEN jel.entry_type = 'DEBIT'  THEN jel.amount ELSE 0 END), 0) AS total_debit,
                COALESCE(SUM(CASE WHEN jel.entry_type = 'CREDIT' THEN jel.amount ELSE 0 END), 0) AS total_credit
            FROM acc_chart_of_accounts coa
            LEFT JOIN acc_journal_entry_lines jel   ON jel.account_id = coa.id
            LEFT JOIN acc_journal_entry_master jem  ON jem.id = jel.journal_entry_id
                AND jem.is_posted = true
                AND jem.voucher_date <= ?::date
            WHERE coa.organization_id = ? AND coa.is_active = true
            GROUP BY coa.id, coa.account_code, coa.account_name, coa.account_type, coa.level
            """ + (showZeroBalance ? "" : "HAVING COALESCE(SUM(jel.amount), 0) > 0 ") + """
            ORDER BY coa.account_type, coa.account_code
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, cutoff, orgId);

        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        for (Map<String, Object> r : rows) {
            totalDr = totalDr.add(toBD(r, "total_debit"));
            totalCr = totalCr.add(toBD(r, "total_credit"));
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDebit",    totalDr);
        summary.put("totalCredit",   totalCr);
        summary.put("accountCount",  rows.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accounts", rows);
        result.put("summary",  summary);
        return result;
    }

    // =========================================================================
    // 3. PROFIT & LOSS
    //    Returns: { sections:[], summary:{totalRevenue, totalExpense, compareRevenue, compareExpense} }
    // =========================================================================

    public Map<String, Object> profitAndLoss(String startDate, String endDate, String cmpStart,  String cmpEnd) {
        Long orgId = SecurityHelper.requireOrgId();

        List<Map<String, Object>> sections = _plQuery(orgId, startDate, endDate, cmpStart, cmpEnd, false);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal cmpRevenue   = BigDecimal.ZERO;
        BigDecimal cmpExpense   = BigDecimal.ZERO;
        for (Map<String, Object> r : sections) {
            String type = String.valueOf(r.getOrDefault("account_type", ""));
            BigDecimal amt  = toBD(r, "amount");
            BigDecimal cAmt = toBD(r, "compare_amount");
            if ("REVENUE".equals(type)) { totalRevenue = totalRevenue.add(amt); cmpRevenue = cmpRevenue.add(cAmt); }
            if ("EXPENSE".equals(type)) { totalExpense = totalExpense.add(amt); cmpExpense = cmpExpense.add(cAmt); }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRevenue",  totalRevenue);
        summary.put("totalExpense",  totalExpense);
        summary.put("compareRevenue", cmpRevenue);
        summary.put("compareExpense", cmpExpense);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sections", sections);
        result.put("summary",  summary);
        return result;
    }

    // =========================================================================
    // 4. BALANCE SHEET
    //    Returns: { accounts:[], summary:{totalAssets, totalLiabilities, totalEquity,
    //               compareAssets, compareLiabilities, compareEquity} }
    // =========================================================================

    public Map<String, Object> balanceSheet(String asOfDate, String compareDate) {
        Long   orgId  = SecurityHelper.requireOrgId();
        String cutoff = (asOfDate != null && !asOfDate.isBlank()) ? asOfDate : LocalDate.now().toString();

        List<Map<String, Object>> accounts = _bsQuery(orgId, cutoff, compareDate);

        BigDecimal totalAssets = BigDecimal.ZERO, totalLiab = BigDecimal.ZERO, totalEquity = BigDecimal.ZERO;
        BigDecimal cmpAssets   = BigDecimal.ZERO, cmpLiab   = BigDecimal.ZERO, cmpEquity   = BigDecimal.ZERO;
        for (Map<String, Object> r : accounts) {
            String type = String.valueOf(r.getOrDefault("account_type", ""));
            BigDecimal amt  = toBD(r, "amount");
            BigDecimal cAmt = toBD(r, "compare_amount");
            switch (type) {
                case "ASSET"     -> { totalAssets = totalAssets.add(amt); cmpAssets = cmpAssets.add(cAmt); }
                case "LIABILITY" -> { totalLiab   = totalLiab.add(amt);   cmpLiab   = cmpLiab.add(cAmt); }
                case "EQUITY"    -> { totalEquity  = totalEquity.add(amt);  cmpEquity  = cmpEquity.add(cAmt); }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAssets",         totalAssets);
        summary.put("totalLiabilities",    totalLiab);
        summary.put("totalEquity",         totalEquity);
        summary.put("compareAssets",       cmpAssets);
        summary.put("compareLiabilities",  cmpLiab);
        summary.put("compareEquity",       cmpEquity);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accounts", accounts);
        result.put("summary",  summary);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private List<Map<String, Object>> _plQuery(Long orgId, String s, String e,
                                                String cs, String ce, boolean bsMode) {
        String typeFilter = bsMode ? "('ASSET','LIABILITY','EQUITY')" : "('REVENUE','EXPENSE')";
        boolean hasCompare = cs != null && !cs.isBlank() && ce != null && !ce.isBlank();

        String sql = """
            SELECT
                coa.account_code,
                coa.account_name,
                coa.account_type,
                coa.level,
                COALESCE(SUM(CASE WHEN jel.entry_type='CREDIT' AND coa.account_type='REVENUE' THEN jel.amount
                                  WHEN jel.entry_type='DEBIT'  AND coa.account_type='EXPENSE' THEN jel.amount
                                  ELSE 0 END) FILTER (WHERE jem.voucher_date BETWEEN ?::date AND ?::date), 0) AS amount,
                """ + (hasCompare ? """
                COALESCE(SUM(CASE WHEN jel.entry_type='CREDIT' AND coa.account_type='REVENUE' THEN jel.amount
                                  WHEN jel.entry_type='DEBIT'  AND coa.account_type='EXPENSE' THEN jel.amount
                                  ELSE 0 END) FILTER (WHERE jem.voucher_date BETWEEN ?::date AND ?::date), 0) AS compare_amount
                """ : " 0 AS compare_amount ") + """
            FROM acc_chart_of_accounts coa
            LEFT JOIN acc_journal_entry_lines jel   ON jel.account_id = coa.id
            LEFT JOIN acc_journal_entry_master jem  ON jem.id = jel.journal_entry_id AND jem.is_posted = true
            WHERE coa.organization_id = ? AND coa.is_active = true
              AND coa.account_type IN """ + typeFilter + """
            GROUP BY coa.id, coa.account_code, coa.account_name, coa.account_type, coa.level
            HAVING COALESCE(SUM(jel.amount), 0) > 0
            ORDER BY coa.account_type, coa.account_code
            """;

        List<Object> args = new ArrayList<>(Arrays.asList(s, e));
        if (hasCompare) { args.add(cs); args.add(ce); }
        args.add(orgId);
        return jdbc.queryForList(sql, args.toArray());
    }

    private List<Map<String, Object>> _bsQuery(Long orgId, String cutoff, String cmpDate) {
        boolean hasCompare = cmpDate != null && !cmpDate.isBlank();
        String sql = """
            SELECT
                coa.account_code,
                coa.account_name,
                coa.account_type,
                coa.level,
                COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END
                           - CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END)
                    FILTER (WHERE jem.voucher_date <= ?::date), 0) AS amount,
                """ + (hasCompare ? """
                COALESCE(SUM(CASE WHEN jel.entry_type='DEBIT'  THEN jel.amount ELSE 0 END
                           - CASE WHEN jel.entry_type='CREDIT' THEN jel.amount ELSE 0 END)
                    FILTER (WHERE jem.voucher_date <= ?::date), 0) AS compare_amount\n
                """ : "0 AS compare_amount\n") + """
                FROM acc_chart_of_accounts coa
            LEFT JOIN acc_journal_entry_lines jel   ON jel.account_id = coa.id
            LEFT JOIN acc_journal_entry_master jem  ON jem.id = jel.journal_entry_id AND jem.is_posted = true
            WHERE coa.organization_id = ? AND coa.is_active = true
              AND coa.account_type IN ('ASSET','LIABILITY','EQUITY')
            GROUP BY coa.id, coa.account_code, coa.account_name, coa.account_type, coa.level
            HAVING ABS(COALESCE(SUM(jel.amount), 0)) > 0
            ORDER BY coa.account_type, coa.account_code
            """;

        List<Object> args = new ArrayList<>(Collections.singletonList(cutoff));
        if (hasCompare) { args.add(cmpDate); }
        args.add(orgId);
        return jdbc.queryForList(sql, args.toArray());
    }

    private BigDecimal toBD(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n)    return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
