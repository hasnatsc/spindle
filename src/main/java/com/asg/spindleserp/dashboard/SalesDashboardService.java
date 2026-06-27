package com.asg.spindleserp.dashboard;

import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

/**
 * SalesDashboardService
 *
 * Provides full data payload for GET /sales/dashboard/summary.
 *
 * Response shape:
 * {
 *   sales: {
 *     openSOCount, openSOValue, openSQCount,
 *     deliveryPendingCount, deliveryPendingValue,
 *     invoicedMTDCount, invoicedMTDValue,
 *     arOutstanding, arOverdue,
 *     unpostedInvoiceCount, cancelledMTD,
 *     activeCustomerCount, creditNoteCount, creditNoteValue,
 *     mtdSOCount, mtdSOValue, collectedMTD
 *   },
 *   arAging:  { current, d3160, d6190, d90plus },
 *   topCustomers:   [ {customerName, salesValue} ]        — top 7 MTD
 *   topOverdueAR:   [ {customerName, overdueAmount} ]     — top 7 overdue
 *   itemBreakdown:  [ {itemName, qty, value} ]            — top 8 items invoiced MTD
 *   recentDocuments:[ last 15 across SQ/SO/DO/DC/SI ]
 *   monthlyTrend:   [ {month, soValue, invoiceValue, collectedValue} ] — 12 months
 *   exceptions:     { overdueAR, pendingDelivery, unposted, cancelled }
 *   exceptionDetails: [ {priority, exceptionType, docNo, customerName, value, delayDays, actionUrl} ]
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesDashboardService {

    private final JdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> summary() {
        Long   orgId    = SecurityHelper.requireOrgId();
        String today    = LocalDate.now().toString();
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();
        String prev7    = LocalDate.now().minusDays(7).toString();

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            _loadSalesKpis(result, orgId, today, mtdStart, prev7);
            _loadArAging(result, orgId, today);
            _loadTopCustomers(result, orgId, mtdStart);
            _loadTopOverdueAR(result, orgId, today);
            _loadItemBreakdown(result, orgId, mtdStart);
            _loadRecentDocuments(result, orgId);
            _loadMonthlyTrend(result, orgId);
            _loadExceptions(result, orgId, today, prev7);
            _loadExceptionDetails(result, orgId, today, prev7);
        } catch (Exception e) {
            log.error("SalesDashboard summary error orgId={}", orgId, e);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. MASTER SALES KPIs — single conditional-aggregation pass
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadSalesKpis(Map<String, Object> result, Long orgId,
                                 String today, String mtdStart, String prev7) {
        String sql = """
            SELECT
              COUNT(*)     FILTER (WHERE document_type = 'SALES_ORDER'
                                     AND status NOT IN ('CLOSED','CANCELLED','CONVERTED'))
                                                            AS open_so_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'SALES_ORDER'
                    AND status NOT IN ('CLOSED','CANCELLED','CONVERTED')), 0)
                                                            AS open_so_value,
              COUNT(*)     FILTER (WHERE document_type = 'SALES_QUOTATION'
                                     AND status NOT IN ('CLOSED','CANCELLED','CONVERTED'))
                                                            AS open_sq_count,
              COUNT(*)     FILTER (WHERE document_type IN ('DELIVERY_ORDER','DELIVERY_CHALLAN')
                                     AND status NOT IN ('COMPLETED','CANCELLED'))
                                                            AS delivery_pending_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type IN ('DELIVERY_ORDER','DELIVERY_CHALLAN')
                    AND status NOT IN ('COMPLETED','CANCELLED')), 0)
                                                            AS delivery_pending_value,
              COUNT(*)     FILTER (WHERE document_type = 'SALES_INVOICE'
                                     AND document_date >= ?::date)
                                                            AS invoiced_mtd_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'SALES_INVOICE'
                    AND document_date >= ?::date), 0)
                                                            AS invoiced_mtd_value,
              COUNT(*)     FILTER (WHERE document_type = 'SALES_ORDER'
                                     AND document_date >= ?::date)
                                                            AS mtd_so_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'SALES_ORDER'
                    AND document_date >= ?::date), 0)
                                                            AS mtd_so_value,
              COUNT(*)     FILTER (WHERE document_type = 'SALES_INVOICE'
                                     AND stock_posted  = false
                                     AND status NOT IN ('CANCELLED'))
                                                            AS unposted_invoice_count,
              COUNT(*)     FILTER (WHERE document_type IN ('SALES_ORDER','SALES_INVOICE')
                                     AND status = 'CANCELLED'
                                     AND document_date >= ?::date)
                                                            AS cancelled_mtd,
              COUNT(*)     FILTER (WHERE document_type = 'CREDIT_NOTE'
                                     AND status NOT IN ('CANCELLED'))
                                                            AS credit_note_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'CREDIT_NOTE'
                    AND status NOT IN ('CANCELLED')), 0)
                                                            AS credit_note_value
            FROM global_business_documents
            WHERE organization_id = ?
              AND is_deleted = false
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                mtdStart, mtdStart, mtdStart, mtdStart, mtdStart, orgId);

        // AR outstanding from journal entries
        String arSql = """
            SELECT
              COALESCE(SUM(CASE WHEN COALESCE(due_date,'9999-12-31'::date) >= ?::date
                               THEN COALESCE(total_amount,0) ELSE 0 END), 0) AS ar_outstanding,
              COALESCE(SUM(CASE WHEN due_date < ?::date
                               THEN COALESCE(total_amount,0) ELSE 0 END), 0) AS ar_overdue,
              COALESCE(SUM(CASE WHEN is_posted = true
                               AND voucher_date >= ?::date
                               AND voucher_type = 'RECEIPT_VOUCHER'
                               THEN COALESCE(total_amount,0) ELSE 0 END), 0) AS collected_mtd
            FROM acc_journal_entry_master
            WHERE organization_id = ?
              AND voucher_type IN ('SALES_VOUCHER','RECEIPT_VOUCHER')
              AND voucher_status NOT IN ('CANCELLED','REVERSED')
            """;

        List<Map<String, Object>> arRows = jdbc.queryForList(arSql, today, today, mtdStart, orgId);

        // Active customer count
        Long activeCustomers = jdbc.queryForObject("""
            SELECT COUNT(*) FROM acc_chart_of_accounts_sub
            WHERE organization_id = ? AND sub_account_type = 'CUSTOMER' AND is_active = true
            """, Long.class, orgId);

        Map<String, Object> sales = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            sales.put("openSOCount",          toLong(r, "open_so_count"));
            sales.put("openSOValue",           toBD(r,   "open_so_value"));
            sales.put("openSQCount",           toLong(r, "open_sq_count"));
            sales.put("deliveryPendingCount",  toLong(r, "delivery_pending_count"));
            sales.put("deliveryPendingValue",  toBD(r,   "delivery_pending_value"));
            sales.put("invoicedMTDCount",      toLong(r, "invoiced_mtd_count"));
            sales.put("invoicedMTDValue",      toBD(r,   "invoiced_mtd_value"));
            sales.put("mtdSOCount",            toLong(r, "mtd_so_count"));
            sales.put("mtdSOValue",            toBD(r,   "mtd_so_value"));
            sales.put("unpostedInvoiceCount",  toLong(r, "unposted_invoice_count"));
            sales.put("cancelledMTD",          toLong(r, "cancelled_mtd"));
            sales.put("creditNoteCount",       toLong(r, "credit_note_count"));
            sales.put("creditNoteValue",       toBD(r,   "credit_note_value"));
        }
        if (!arRows.isEmpty()) {
            Map<String, Object> r = arRows.get(0);
            sales.put("arOutstanding",  toBD(r, "ar_outstanding"));
            sales.put("arOverdue",      toBD(r, "ar_overdue"));
            sales.put("collectedMTD",   toBD(r, "collected_mtd"));
        }
        sales.put("activeCustomerCount", activeCustomers != null ? activeCustomers : 0L);
        result.put("sales", sales);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. AR AGING
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadArAging(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT
              COALESCE(SUM(CASE WHEN validity_date >= ?::date                           THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS current_amt,
              COALESCE(SUM(CASE WHEN validity_date BETWEEN (?::date - 60) AND (?::date - 31) THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d3160,
              COALESCE(SUM(CASE WHEN validity_date BETWEEN (?::date - 90) AND (?::date - 61) THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d6190,
              COALESCE(SUM(CASE WHEN validity_date < (?::date - 90)                     THEN COALESCE(due_amount,0) ELSE 0 END), 0) AS d90plus
            FROM global_business_documents
            WHERE organization_id = ?
              AND document_type   = 'SALES_INVOICE'
              AND status NOT IN ('CANCELLED')
              AND COALESCE(due_amount,0) > 0
              AND is_deleted = false
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                today, today, today, today, today, today, orgId);
        Map<String, Object> aging = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            aging.put("current", toBD(r, "current_amt"));
            aging.put("d3160",   toBD(r, "d3160"));
            aging.put("d6190",   toBD(r, "d6190"));
            aging.put("d90plus", toBD(r, "d90plus"));
        }
        result.put("arAging", aging);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TOP CUSTOMERS (MTD invoiced value)
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopCustomers(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT s.sub_account_name AS customer_name,
                   COALESCE(SUM(d.total_amount), 0) AS sales_value
            FROM global_business_documents d
            JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'SALES_INVOICE'
              AND d.document_date  >= ?::date
              AND d.is_deleted      = false
            GROUP BY s.id, s.sub_account_name
            ORDER BY sales_value DESC
            LIMIT 7
            """;
        result.put("topCustomers", jdbc.queryForList(sql, orgId, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TOP OVERDUE AR
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopOverdueAR(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT s.sub_account_name AS customer_name,
                   COALESCE(SUM(d.due_amount), 0) AS overdue_amount
            FROM global_business_documents d
            JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'SALES_INVOICE'
              AND d.required_date   < ?::date
              AND COALESCE(d.due_amount, 0) > 0
              AND d.is_deleted      = false
            GROUP BY s.id, s.sub_account_name
            ORDER BY overdue_amount DESC
            LIMIT 7
            """;
        result.put("topOverdueAR", jdbc.queryForList(sql, orgId, today));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. ITEM BREAKDOWN — top 8 items invoiced MTD
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadItemBreakdown(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT i.item_name,
                   COALESCE(SUM(l.quantity), 0)    AS qty,
                   COALESCE(SUM(l.line_amount), 0) AS value
            FROM global_business_documents d
            JOIN global_business_document_lines l ON l.document_id = d.id
            JOIN inv_items i ON i.id = l.item_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'SALES_INVOICE'
              AND d.document_date  >= ?::date
              AND d.is_deleted      = false
            GROUP BY i.id, i.item_name
            ORDER BY value DESC
            LIMIT 8
            """;
        result.put("itemBreakdown", jdbc.queryForList(sql, orgId, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. RECENT DOCUMENTS — last 15
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadRecentDocuments(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT d.id, d.document_no, d.document_type,
                   TO_CHAR(d.document_date, 'DD-Mon-YYYY') AS document_date,
                   COALESCE(s.sub_account_name, '—')       AS customer_name,
                   COALESCE(d.total_amount, 0)              AS total_amount,
                   d.status, d.accounting_posted, d.stock_posted
            FROM global_business_documents d
            LEFT JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            WHERE d.organization_id = ?
              AND d.document_type IN ('SALES_QUOTATION','SALES_ORDER',
                                       'DELIVERY_ORDER','DELIVERY_CHALLAN',
                                       'SALES_INVOICE','CREDIT_NOTE')
              AND d.is_deleted = false
            ORDER BY d.document_date DESC, d.id DESC
            LIMIT 15
            """;
        result.put("recentDocuments", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. 12-MONTH TREND
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadMonthlyTrend(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT TO_CHAR(DATE_TRUNC('month', document_date), 'Mon-YY') AS month,
                   COALESCE(SUM(total_amount) FILTER (WHERE document_type='SALES_ORDER'),   0) AS so_value,
                   COALESCE(SUM(total_amount) FILTER (WHERE document_type='SALES_INVOICE'), 0) AS invoice_value
            FROM global_business_documents
            WHERE organization_id = ?
              AND document_type IN ('SALES_ORDER','SALES_INVOICE')
              AND document_date  >= (CURRENT_DATE - INTERVAL '12 months')
              AND is_deleted      = false
            GROUP BY DATE_TRUNC('month', document_date)
            ORDER BY DATE_TRUNC('month', document_date)
            """;
        result.put("monthlyTrend", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. EXCEPTION SUMMARY
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadExceptions(Map<String, Object> result, Long orgId, String today, String prev7) {
        String sql = """
            SELECT
              COUNT(*) FILTER (WHERE document_type='SALES_INVOICE'
                                 AND required_date < ?::date
                                 AND COALESCE(due_amount,0) > 0)  AS overdue_ar,
              COUNT(*) FILTER (WHERE document_type IN ('DELIVERY_ORDER','DELIVERY_CHALLAN')
                                 AND status NOT IN ('COMPLETED','CANCELLED')
                                 AND required_date < ?::date)      AS pending_delivery,
              COUNT(*) FILTER (WHERE document_type='SALES_INVOICE'
                                 AND stock_posted = false
                                 AND status NOT IN ('CANCELLED'))  AS unposted,
              COUNT(*) FILTER (WHERE status='CANCELLED'
                                 AND document_date >= ?::date)     AS cancelled
            FROM global_business_documents
            WHERE organization_id = ?
              AND is_deleted = false
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, today, today, prev7, orgId);
        Map<String, Object> ex = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            ex.put("overdueAR",       toLong(r, "overdue_ar"));
            ex.put("pendingDelivery", toLong(r, "pending_delivery"));
            ex.put("unposted",        toLong(r, "unposted"));
            ex.put("cancelled",       toLong(r, "cancelled"));
        }
        result.put("exceptions", ex);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. EXCEPTION DETAILS — top actionable rows
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadExceptionDetails(Map<String, Object> result, Long orgId, String today, String prev7) {
        String sql = """
            SELECT 'HIGH'                          AS priority,
                   'Overdue AR'                    AS exception_type,
                   d.document_no,
                   COALESCE(s.sub_account_name,'—') AS customer_name,
                   COALESCE(d.due_amount,0)         AS value,
                   (CURRENT_DATE - d.required_date) AS delay_days,
                   '/sales/invoices'               AS action_url
            FROM global_business_documents d
            LEFT JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'SALES_INVOICE'
              AND d.required_date   < ?::date
              AND COALESCE(d.due_amount, 0) > 0
              AND d.is_deleted      = false
            ORDER BY delay_days DESC
            LIMIT 10
            """;
        result.put("exceptionDetails", jdbc.queryForList(sql, orgId, today));
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
