package com.asg.spindleserp.dashboard;

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
 * AccountsDashboardService
 *
 * Provides full data payload for GET /accounts/dashboard/summary.
 *
 * Response shape:
 * {
 *   accounts: {
 *     totalReceivable, overdueReceivable,
 *     totalPayable, dueSoonPayable,
 *     cashBankBalance, unpostedVoucherCount, overdueVoucherCount,
 *     mtdRevenuePosted, mtdExpensePosted,
 *     openJournalCount, reversedVoucherMTD,
 *     totalCoaAccounts, totalSubAccounts
 *   },
 *   arAging:         { current, d3160, d6190, d90plus },
 *   apAging:         { current, d3160, d6190, d90plus },
 *   cashBankAccounts:[ {accountTitle, balance, accountType} ]
 *   topReceivables:  [ {partyName, balance} ] — top 7
 *   topPayables:     [ {partyName, balance} ] — top 7
 *   recentVouchers:  [ last 15 JEM entries ]
 *   monthlyPL:       [ {month, revenue, expense, net} ] — 12 months
 *   voucherTypes:    [ {voucherType, count, total_amount} ]
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountsDashboardService {

    private final JdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> summary() {
        Long   orgId    = SecurityHelper.requireOrgId();
        String today    = LocalDate.now().toString();
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();
        String due7days = LocalDate.now().plusDays(7).toString();

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            _loadAccountsKpis(result, orgId, today, mtdStart, due7days);
            _loadArAging(result, orgId, today);
            _loadApAging(result, orgId, today);
            _loadCashBankAccounts(result, orgId);
            _loadTopReceivables(result, orgId);
            _loadTopPayables(result, orgId);
            _loadRecentVouchers(result, orgId);
            _loadMonthlyPL(result, orgId);
            _loadVoucherTypeBreakdown(result, orgId, mtdStart);
        } catch (Exception e) {
            log.error("AccountsDashboard summary error orgId={}", orgId, e);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. MASTER ACCOUNTS KPIs
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadAccountsKpis(Map<String, Object> result, Long orgId,
                                    String today, String mtdStart, String due7days) {
        String sql = """
            SELECT
              -- AR / AP from sub-account current_balance
              COALESCE((SELECT SUM(current_balance) FROM acc_chart_of_accounts_sub
               WHERE organization_id = ? AND sub_account_type = 'CUSTOMER'
                 AND is_active = true AND current_balance > 0), 0) AS total_receivable,
              COALESCE((SELECT SUM(current_balance) FROM acc_chart_of_accounts_sub
               WHERE organization_id = ? AND sub_account_type = 'SUPPLIER'
                 AND is_active = true AND current_balance < 0), 0) AS total_payable,

              -- Overdue AR — sales invoices past due date with outstanding due_amount
              COALESCE((SELECT SUM(due_amount) FROM global_business_documents
               WHERE organization_id = ? AND document_type = 'SALES_INVOICE'
                 AND required_date < ?::date AND COALESCE(due_amount,0) > 0
                 AND is_deleted = false), 0)                        AS overdue_receivable,

              -- Payable due within 7 days
              COALESCE((SELECT SUM(due_amount) FROM global_business_documents
               WHERE organization_id = ? AND document_type = 'PURCHASE_INVOICE'
                 AND required_date BETWEEN ?::date AND ?::date
                 AND COALESCE(due_amount,0) > 0
                 AND is_deleted = false), 0)                        AS due_soon_payable,

              -- Cash + Bank balance
              COALESCE((SELECT SUM(current_balance) FROM acc_chart_of_accounts_sub
               WHERE organization_id = ? AND sub_account_type IN ('BANK','CASH')
                 AND is_active = true), 0)                          AS cash_bank_balance,

              -- Unposted vouchers
              (SELECT COUNT(*) FROM acc_journal_entry_master
               WHERE organization_id = ? AND is_posted = false
                 AND voucher_status NOT IN ('CANCELLED','REVERSED'))  AS unposted_voucher_count,

              -- Overdue unposted vouchers
              (SELECT COUNT(*) FROM acc_journal_entry_master
               WHERE organization_id = ? AND is_posted = false
                 AND due_date < ?::date
                 AND voucher_status NOT IN ('CANCELLED','REVERSED'))  AS overdue_voucher_count,

              -- Revenue posted MTD
              COALESCE((SELECT SUM(jel.amount) FROM acc_journal_entry_lines jel
               JOIN acc_chart_of_accounts coa ON coa.id = jel.account_id
               JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
               WHERE jel.organization_id = ? AND jem.is_posted = true
                 AND coa.account_type = 'REVENUE' AND jel.entry_type = 'CREDIT'
                 AND jem.voucher_date >= ?::date), 0)                AS mtd_revenue,

              -- Expense posted MTD
              COALESCE((SELECT SUM(jel.amount) FROM acc_journal_entry_lines jel
               JOIN acc_chart_of_accounts coa ON coa.id = jel.account_id
               JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
               WHERE jel.organization_id = ? AND jem.is_posted = true
                 AND coa.account_type = 'EXPENSE' AND jel.entry_type = 'DEBIT'
                 AND jem.voucher_date >= ?::date), 0)                AS mtd_expense,

              -- Open journals (draft/pending vouchers)
              (SELECT COUNT(*) FROM acc_journal_entry_master
               WHERE organization_id = ? AND voucher_status = 'DRAFT') AS open_journal_count,

              -- Reversed MTD
              (SELECT COUNT(*) FROM acc_journal_entry_master
               WHERE organization_id = ? AND is_reversed = true
                 AND voucher_date >= ?::date)                         AS reversed_mtd,

              -- CoA counts
              (SELECT COUNT(*) FROM acc_chart_of_accounts WHERE organization_id = ?) AS total_coa,
              (SELECT COUNT(*) FROM acc_chart_of_accounts_sub WHERE organization_id = ? AND is_active = true) AS total_sub
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                orgId, orgId,
                orgId, today,
                orgId, today, due7days,
                orgId,
                orgId,
                orgId, today,
                orgId, mtdStart,
                orgId, mtdStart,
                orgId, orgId, mtdStart,
                orgId, orgId);

        Map<String, Object> acc = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            acc.put("totalReceivable",    toBD(r,   "total_receivable"));
            acc.put("overdueReceivable",  toBD(r,   "overdue_receivable"));
            acc.put("totalPayable",       toBD(r,   "total_payable"));
            acc.put("dueSoonPayable",     toBD(r,   "due_soon_payable"));
            acc.put("cashBankBalance",    toBD(r,   "cash_bank_balance"));
            acc.put("unpostedVoucherCount", toLong(r, "unposted_voucher_count"));
            acc.put("overdueVoucherCount",  toLong(r, "overdue_voucher_count"));
            acc.put("mtdRevenuePosted",   toBD(r,   "mtd_revenue"));
            acc.put("mtdExpensePosted",   toBD(r,   "mtd_expense"));
            acc.put("openJournalCount",   toLong(r, "open_journal_count"));
            acc.put("reversedVoucherMTD", toLong(r, "reversed_mtd"));
            acc.put("totalCoaAccounts",   toLong(r, "total_coa"));
            acc.put("totalSubAccounts",   toLong(r, "total_sub"));
        }
        result.put("accounts", acc);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. AR AGING
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadArAging(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT
              COALESCE(SUM(CASE WHEN required_date >= ?::date                                  THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS current_amt,
              COALESCE(SUM(CASE WHEN required_date BETWEEN (?::date-60) AND (?::date-31)       THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d3160,
              COALESCE(SUM(CASE WHEN required_date BETWEEN (?::date-90) AND (?::date-61)       THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d6190,
              COALESCE(SUM(CASE WHEN required_date < (?::date-90)                              THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d90plus
            FROM global_business_documents
            WHERE organization_id = ? AND document_type = 'SALES_INVOICE'
              AND COALESCE(due_amount,0) > 0 AND is_deleted = false
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                today, today, today, today, today, today, today, orgId);
        Map<String, Object> m = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            m.put("current", toBD(r, "current_amt"));
            m.put("d3160",   toBD(r, "d3160"));
            m.put("d6190",   toBD(r, "d6190"));
            m.put("d90plus", toBD(r, "d90plus"));
        }
        result.put("arAging", m);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. AP AGING
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadApAging(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT
              COALESCE(SUM(CASE WHEN required_date >= ?::date                                  THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS current_amt,
              COALESCE(SUM(CASE WHEN required_date BETWEEN (?::date-60) AND (?::date-31)       THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d3160,
              COALESCE(SUM(CASE WHEN required_date BETWEEN (?::date-90) AND (?::date-61)       THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d6190,
              COALESCE(SUM(CASE WHEN required_date < (?::date-90)                              THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d90plus
            FROM global_business_documents
            WHERE organization_id = ? AND document_type = 'PURCHASE_INVOICE'
              AND COALESCE(due_amount,0) > 0 AND is_deleted = false
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                today, today, today, today, today, today, today, orgId);
        Map<String, Object> m = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            m.put("current", toBD(r, "current_amt"));
            m.put("d3160",   toBD(r, "d3160"));
            m.put("d6190",   toBD(r, "d6190"));
            m.put("d90plus", toBD(r, "d90plus"));
        }
        result.put("apAging", m);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. CASH & BANK ACCOUNTS
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadCashBankAccounts(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT sub_account_name AS account_title,
                   sub_account_type AS account_type,
                   COALESCE(current_balance, 0) AS balance,
                   currency
            FROM acc_chart_of_accounts_sub
            WHERE organization_id = ?
              AND sub_account_type IN ('BANK','CASH')
              AND is_active = true
            ORDER BY sub_account_type, balance DESC
            """;
        result.put("cashBankAccounts", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. TOP RECEIVABLES
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopReceivables(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT sub_account_name AS party_name, current_balance AS balance
            FROM acc_chart_of_accounts_sub
            WHERE organization_id = ? AND sub_account_type = 'CUSTOMER'
              AND is_active = true AND current_balance > 0
            ORDER BY balance DESC
            LIMIT 7
            """;
        result.put("topReceivables", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. TOP PAYABLES
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopPayables(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT sub_account_name AS party_name, ABS(current_balance) AS balance
            FROM acc_chart_of_accounts_sub
            WHERE organization_id = ? AND sub_account_type = 'SUPPLIER'
              AND is_active = true AND current_balance < 0
            ORDER BY ABS(current_balance) DESC
            LIMIT 7
            """;
        result.put("topPayables", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. RECENT VOUCHERS — last 15
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadRecentVouchers(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT id, voucher_no, voucher_type,
                   TO_CHAR(voucher_date, 'DD-Mon-YYYY') AS voucher_date,
                   COALESCE(total_amount, 0) AS total_amount,
                   voucher_status, is_posted, narration
            FROM acc_journal_entry_master
            WHERE organization_id = ?
              AND voucher_status NOT IN ('CANCELLED')
            ORDER BY id DESC
            LIMIT 15
            """;
        result.put("recentVouchers", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. 12-MONTH P&L TREND
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadMonthlyPL(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT TO_CHAR(DATE_TRUNC('month', jem.voucher_date), 'Mon-YY') AS month,
                   COALESCE(SUM(jel.amount) FILTER (
                     WHERE coa.account_type='REVENUE' AND jel.entry_type='CREDIT'), 0) AS revenue,
                   COALESCE(SUM(jel.amount) FILTER (
                     WHERE coa.account_type='EXPENSE' AND jel.entry_type='DEBIT'),   0) AS expense
            FROM acc_journal_entry_lines jel
            JOIN acc_chart_of_accounts coa ON coa.id = jel.account_id
            JOIN acc_journal_entry_master jem ON jem.id = jel.journal_entry_id
            WHERE jel.organization_id = ?
              AND jem.is_posted = true
              AND jem.voucher_date >= (CURRENT_DATE - INTERVAL '12 months')
            GROUP BY DATE_TRUNC('month', jem.voucher_date)
            ORDER BY DATE_TRUNC('month', jem.voucher_date)
            """;
        result.put("monthlyPL", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. VOUCHER TYPE BREAKDOWN (MTD)
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadVoucherTypeBreakdown(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT voucher_type,
                   COUNT(*) AS count,
                   COALESCE(SUM(total_amount), 0) AS total_amount
            FROM acc_journal_entry_master
            WHERE organization_id = ?
              AND voucher_date >= ?::date
              AND voucher_status NOT IN ('CANCELLED')
            GROUP BY voucher_type
            ORDER BY count DESC
            """;
        result.put("voucherTypes", jdbc.queryForList(sql, orgId, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    private Long toLong(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return 0L;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }
    private BigDecimal toBD(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
