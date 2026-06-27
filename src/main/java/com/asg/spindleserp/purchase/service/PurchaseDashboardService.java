package com.asg.spindleserp.purchase.service;

import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * PurchaseDashboardService
 *
 * Provides the full data payload for GET /purchase/dashboard/summary.
 *
 * Response shape (all keys consumed by purchase-dashboard.html JS):
 * {
 *   purchase: {
 *     openPOCount, openPOValue, openPOConfirmed, openPODraft,
 *     grnPendingCount, grnPendingValue, grnOverdue7d, grnOverdueValue,
 *     mtdGRNCount, mtdGRNValue,
 *     apOutstanding, apOverdue, apDue7d,
 *     openInvoiceCount, unpostedInvoiceCount, unpostedInvoiceValue,
 *     mtdInvoiceCount, mtdInvoiceValue, voucherOverdueCount,
 *     mtdPOCount, mtdPOValue, cancelledMTD,
 *     debitNoteCount, debitNoteValue, debitNotePending, mtdDebitNoteValue,
 *     prPendingCount, prPendingValue, prOverdue5d,
 *     rfqOpenCount, csPendingCount,
 *     closedPOCount, closedPOValue, avgLeadDays,
 *     activeSupplierCount, suppliersWithOpenPO, suppliersWithOverdueAP,
 *     totalQtyReceivedMTD, uniqueItemsReceivedMTD
 *   },
 *   apAging:  { current, d3160, d6190, d90plus },
 *   exceptions: { grnOverdue, apOverdue, unposted, prPending, cancelled },
 *   exceptionDetails: [ {priority, exceptionType, docNo, supplierName, value, delayDays, actionUrl} ],
 *   topSuppliers:     [ {supplierName, purchaseValue} ]  — top 7 by MTD PO value
 *   topOverdueAP:     [ {supplierName, overdueAmount} ]  — top 7 by overdue AP
 *   categoryBreakdown:[ {category, value} ]              — item_type breakdown (GRN MTD)
 *   recentDocuments:  [ last 15 across PO/GRN/PI/DN ]
 *   monthlyTrend:     [ {month, poValue, grnValue, apValue} ] — 12 months
 * }
 *
 * SQL STRATEGY
 * ────────────
 * One master CTE covers all document-level KPIs in a single pass over
 * global_business_documents using conditional aggregation (FILTER clause).
 * Supplementary queries handle per-supplier rankings, AP aging, recent docs,
 * exception detail rows, and monthly trend — each hitting dedicated indexes.
 *
 * Indexes relied upon (all pre-existing on schema):
 *   idx_gbd_org, idx_gbd_type, idx_gbd_status, idx_gbd_date, idx_gbd_deleted
 *   idx_gbd_party  → supplier aggregations
 *   idx_gbdl_doc   → line-level qty/value joins
 *   idx_jem_org, idx_jem_status, idx_jem_due  → voucher overdue
 *   idx_sub_org, idx_sub_type  → supplier counts
 *   idx_stock_item → qty received
 *   idx_invtx_org, idx_invtx_date → inventory transactions MTD
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseDashboardService {

    private final JdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> summary() {
        Long   orgId    = SecurityHelper.requireOrgId();
        String today    = LocalDate.now().toString();
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();
        String prev7    = LocalDate.now().minusDays(7).toString();
        String prev5    = LocalDate.now().minusDays(5).toString();

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            _loadPurchaseKpis(result, orgId, today, mtdStart, prev7, prev5);
            _loadApAging(result, orgId, today);
            _loadExceptions(result, orgId, today, prev7, prev5);
            _loadExceptionDetails(result, orgId, today, prev7);
            _loadTopSuppliers(result, orgId, mtdStart);
            _loadTopOverdueAP(result, orgId, today);
            _loadCategoryBreakdown(result, orgId, mtdStart);
            _loadRecentDocuments(result, orgId);
            _loadMonthlyTrend(result, orgId);
        } catch (Exception e) {
            log.error("PurchaseDashboard summary error orgId={}", orgId, e);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. MASTER PURCHASE KPIs  — single conditional-aggregation pass
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadPurchaseKpis(Map<String, Object> result, Long orgId, String today, String mtdStart, String prev7, String prev5) {
        String sql = """
            SELECT
              /* ── PURCHASE REQUISITIONS ─────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_REQUISITION'
                                     AND status NOT IN ('COMPLETED','CANCELLED','CONVERTED'))
                                                            AS pr_pending_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'PURCHASE_REQUISITION'
                    AND status NOT IN ('COMPLETED','CANCELLED','CONVERTED')), 0)
                                                            AS pr_pending_value,
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_REQUISITION'
                                     AND status NOT IN ('COMPLETED','CANCELLED','CONVERTED')
                                     AND document_date  < ?::date)
                                                            AS pr_overdue_5d,

              /* ── RFQ ────────────────────────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'REQUEST_FOR_QUOTATION'
                                     AND status NOT IN ('COMPLETED','CANCELLED','CLOSED'))
                                                            AS rfq_open_count,

              /* ── COMPARATIVE STATEMENT ──────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'COMPARATIVE_STATEMENT'
                                     AND status NOT IN ('COMPLETED','CANCELLED','CLOSED'))
                                                            AS cs_pending_count,

              /* ── PURCHASE ORDERS (OPEN) ─────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_ORDER'
                                     AND status NOT IN ('CLOSED','CANCELLED'))
                                                            AS po_open_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'PURCHASE_ORDER'
                    AND status NOT IN ('CLOSED','CANCELLED')), 0)
                                                            AS po_open_value,
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_ORDER'
                                     AND status = 'CONFIRMED')
                                                            AS po_open_confirmed,
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_ORDER'
                                     AND status = 'DRAFT')
                                                            AS po_open_draft,

              /* ── PURCHASE ORDERS (MTD) ──────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_ORDER'
                                     AND document_date >= ?::date
                                     AND status NOT IN ('CANCELLED'))
                                                            AS po_mtd_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'PURCHASE_ORDER'
                    AND document_date >= ?::date
                    AND status NOT IN ('CANCELLED')), 0)    AS po_mtd_value,
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_ORDER'
                                     AND status = 'CANCELLED'
                                     AND document_date >= ?::date)
                                                            AS po_cancelled_mtd,

              /* ── PURCHASE ORDERS (CLOSED MTD) ───────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_ORDER'
                                     AND status = 'CLOSED'
                                     AND document_date >= ?::date)
                                                            AS po_closed_mtd_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'PURCHASE_ORDER'
                    AND status = 'CLOSED'
                    AND document_date >= ?::date), 0)       AS po_closed_mtd_value,

              /* ── GRN PENDING ─────────────────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'GOODS_RECEIPT_NOTE'
                                     AND status NOT IN ('CONFIRMED','CANCELLED'))
                                                            AS grn_pending_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'GOODS_RECEIPT_NOTE'
                    AND status NOT IN ('CONFIRMED','CANCELLED')), 0)
                                                            AS grn_pending_value,
              COUNT(*)     FILTER (WHERE document_type = 'GOODS_RECEIPT_NOTE'
                                     AND status NOT IN ('CONFIRMED','CANCELLED')
                                     AND document_date < ?::date)
                                                            AS grn_overdue_7d,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'GOODS_RECEIPT_NOTE'
                    AND status NOT IN ('CONFIRMED','CANCELLED')
                    AND document_date < ?::date), 0)        AS grn_overdue_value,

              /* ── GRN MTD (CONFIRMED) ────────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'GOODS_RECEIPT_NOTE'
                                     AND status = 'CONFIRMED'
                                     AND document_date >= ?::date)
                                                            AS grn_mtd_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'GOODS_RECEIPT_NOTE'
                    AND status = 'CONFIRMED'
                    AND document_date >= ?::date), 0)       AS grn_mtd_value,

              /* ── PURCHASE INVOICE (OPEN / UNPOSTED) ─────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_INVOICE'
                                     AND status NOT IN ('CANCELLED'))
                                                            AS pi_open_count,
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_INVOICE'
                                     AND accounting_posted = false
                                     AND status NOT IN ('CANCELLED'))
                                                            AS pi_unposted_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'PURCHASE_INVOICE'
                    AND accounting_posted = false
                    AND status NOT IN ('CANCELLED')), 0)    AS pi_unposted_value,

              /* ── PURCHASE INVOICE (MTD) ─────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'PURCHASE_INVOICE'
                                     AND document_date >= ?::date
                                     AND status NOT IN ('CANCELLED'))
                                                            AS pi_mtd_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'PURCHASE_INVOICE'
                    AND document_date >= ?::date
                    AND status NOT IN ('CANCELLED')), 0)    AS pi_mtd_value,

              /* ── AP OUTSTANDING (from invoices) ─────────────────── */
              COALESCE(SUM(total_amount - COALESCE(paid_amount,0)) FILTER (
                  WHERE document_type = 'PURCHASE_INVOICE'
                    AND status NOT IN ('CANCELLED')
                    AND COALESCE(total_amount,0) > COALESCE(paid_amount,0)), 0)
                                                            AS ap_outstanding,
              COALESCE(SUM(due_amount) FILTER (
                  WHERE document_type = 'PURCHASE_INVOICE'
                    AND COALESCE(due_amount,0) > 0
                    AND required_date < ?::date), 0)        AS ap_overdue,
              COALESCE(SUM(due_amount) FILTER (
                  WHERE document_type = 'PURCHASE_INVOICE'
                    AND COALESCE(due_amount,0) > 0
                    AND required_date BETWEEN ?::date AND (?::date + INTERVAL '7 days')), 0)
                                                            AS ap_due_7d,

              /* ── DEBIT NOTES (RETURNS) ──────────────────────────── */
              COUNT(*)     FILTER (WHERE document_type = 'DEBIT_NOTE'
                                     AND status NOT IN ('CANCELLED'))
                                                            AS dn_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'DEBIT_NOTE'
                    AND status NOT IN ('CANCELLED')), 0)    AS dn_value,
              COUNT(*)     FILTER (WHERE document_type = 'DEBIT_NOTE'
                                     AND status = 'DRAFT')
                                                            AS dn_pending,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'DEBIT_NOTE'
                    AND document_date >= ?::date
                    AND status NOT IN ('CANCELLED')), 0)    AS dn_mtd_value

            FROM global_business_documents
            WHERE organization_id = ?
              AND is_deleted       = false
            """;

        // Parameter order matches the ? placeholders above
        List<Map<String, Object>> rows = jdbc.queryForList(sql,
            prev5,                    // pr_overdue_5d
            mtdStart, mtdStart,       // po_mtd (count + value)
            mtdStart,                 // po_cancelled_mtd
            mtdStart, mtdStart,       // po_closed_mtd (count + value)
            prev7, prev7,             // grn_overdue_7d, grn_overdue_value
            mtdStart, mtdStart,       // grn_mtd (count + value)
            mtdStart, mtdStart,       // pi_mtd (count + value)
            today,                    // ap_overdue
            today, today,             // ap_due_7d range
            mtdStart,                 // dn_mtd_value
            orgId                     // organization_id
        );

        // Average lead time (PO confirm date → first GRN date) — separate query
        String leadSql = """
            SELECT COALESCE(AVG(
                EXTRACT(DAY FROM (grn.document_date - po.document_date))
            ), 0)::int AS avg_lead_days
            FROM global_business_documents po
            JOIN global_business_documents grn
              ON grn.parent_document_id = po.id
             AND grn.document_type      = 'GOODS_RECEIPT_NOTE'
             AND grn.status             = 'CONFIRMED'
             AND grn.is_deleted         = false
            WHERE po.organization_id = ?
              AND po.document_type   = 'PURCHASE_ORDER'
              AND po.status          = 'CLOSED'
              AND po.document_date  >= (CURRENT_DATE - INTERVAL '3 months')
              AND po.is_deleted      = false
            """;
        int avgLead = 0;
        try {
            List<Map<String, Object>> lr = jdbc.queryForList(leadSql, orgId);
            if (!lr.isEmpty()) avgLead = toLong(lr.getFirst(), "avg_lead_days").intValue();
        } catch (Exception ignored) {}

        // Active supplier count + suppliers with open PO + suppliers with overdue AP
        String suppSql = """
            SELECT
              COUNT(DISTINCT s.id)                                          AS active_supplier_count,
              COUNT(DISTINCT po.party_id) FILTER (WHERE po.id IS NOT NULL)  AS suppliers_with_open_po,
              COUNT(DISTINCT ov.party_id) FILTER (WHERE ov.id IS NOT NULL)  AS suppliers_with_overdue_ap
            FROM acc_chart_of_accounts_sub s
            LEFT JOIN global_business_documents po
              ON po.party_id      = s.id
             AND po.document_type = 'PURCHASE_ORDER'
             AND po.status NOT IN ('CLOSED','CANCELLED')
             AND po.is_deleted    = false
            LEFT JOIN global_business_documents ov
              ON ov.party_id      = s.id
             AND ov.document_type = 'PURCHASE_INVOICE'
             AND ov.required_date < ?::date
             AND COALESCE(ov.due_amount,0) > 0
             AND ov.is_deleted    = false
            WHERE s.organization_id   = ?
              AND s.sub_account_type  = 'SUPPLIER'
              AND s.is_active         = true
            """;
        List<Map<String, Object>> suppRows = jdbc.queryForList(suppSql, today, orgId);

        // Items received MTD (from inventory transactions)
        String rcvdSql = """
            SELECT
              COALESCE(SUM(quantity),  0)::bigint AS total_qty_mtd,
              COUNT(DISTINCT item_id)             AS unique_items_mtd
            FROM global_inventory_transactions
            WHERE organization_id  = ?
              AND movement_type    = 'PURCHASE_RECEIPT'
              AND transaction_date >= ?::date
            """;
        List<Map<String, Object>> rcvdRows = jdbc.queryForList(rcvdSql, orgId, mtdStart);

        // Overdue vouchers count
        String vchSql = """
            SELECT COUNT(*) AS overdue_count
            FROM acc_journal_entry_master
            WHERE organization_id = ?
              AND voucher_type     = 'PURCHASE_VOUCHER'
              AND is_posted        = false
              AND due_date         < ?::date
              AND voucher_status NOT IN ('CANCELLED','REVERSED')
            """;
        long vchOverdue = 0;
        try {
            List<Map<String, Object>> vr = jdbc.queryForList(vchSql, orgId, today);
            if (!vr.isEmpty()) vchOverdue = toLong(vr.getFirst(), "overdue_count");
        } catch (Exception ignored) {}

        // Assemble purchase map
        Map<String, Object> p = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.getFirst();
            p.put("prPendingCount",       toLong(r, "pr_pending_count"));
            p.put("prPendingValue",       toBD(r, "pr_pending_value"));
            p.put("prOverdue5d",          toLong(r, "pr_overdue_5d"));
            p.put("rfqOpenCount",         toLong(r, "rfq_open_count"));
            p.put("csPendingCount",       toLong(r, "cs_pending_count"));
            p.put("openPOCount",          toLong(r, "po_open_count"));
            p.put("openPOValue",          toBD(r, "po_open_value"));
            p.put("openPOConfirmed",      toLong(r, "po_open_confirmed"));
            p.put("openPODraft",          toLong(r, "po_open_draft"));
            p.put("mtdPOCount",           toLong(r, "po_mtd_count"));
            p.put("mtdPOValue",           toBD(r, "po_mtd_value"));
            p.put("cancelledMTD",         toLong(r, "po_cancelled_mtd"));
            p.put("closedPOCount",        toLong(r, "po_closed_mtd_count"));
            p.put("closedPOValue",        toBD(r, "po_closed_mtd_value"));
            p.put("grnPendingCount",      toLong(r, "grn_pending_count"));
            p.put("grnPendingValue",      toBD(r, "grn_pending_value"));
            p.put("grnOverdue7d",         toLong(r, "grn_overdue_7d"));
            p.put("grnOverdueValue",      toBD(r, "grn_overdue_value"));
            p.put("mtdGRNCount",          toLong(r, "grn_mtd_count"));
            p.put("mtdGRNValue",          toBD(r, "grn_mtd_value"));
            p.put("openInvoiceCount",     toLong(r, "pi_open_count"));
            p.put("unpostedInvoiceCount", toLong(r, "pi_unposted_count"));
            p.put("unpostedInvoiceValue", toBD(r, "pi_unposted_value"));
            p.put("mtdInvoiceCount",      toLong(r, "pi_mtd_count"));
            p.put("mtdInvoiceValue",      toBD(r, "pi_mtd_value"));
            p.put("apOutstanding",        toBD(r, "ap_outstanding"));
            p.put("apOverdue",            toBD(r, "ap_overdue"));
            p.put("apDue7d",              toBD(r, "ap_due_7d"));
            p.put("debitNoteCount",       toLong(r, "dn_count"));
            p.put("debitNoteValue",       toBD(r, "dn_value"));
            p.put("debitNotePending",     toLong(r, "dn_pending"));
            p.put("mtdDebitNoteValue",    toBD(r, "dn_mtd_value"));
        }
        p.put("avgLeadDays",           avgLead);
        p.put("voucherOverdueCount",   vchOverdue);
        if (!suppRows.isEmpty()) {
            p.put("activeSupplierCount",   toLong(suppRows.getFirst(), "active_supplier_count"));
            p.put("suppliersWithOpenPO",   toLong(suppRows.getFirst(), "suppliers_with_open_po"));
            p.put("suppliersWithOverdueAP",toLong(suppRows.getFirst(), "suppliers_with_overdue_ap"));
        }
        if (!rcvdRows.isEmpty()) {
            p.put("totalQtyReceivedMTD",  toLong(rcvdRows.getFirst(), "total_qty_mtd"));
            p.put("uniqueItemsReceivedMTD",toLong(rcvdRows.getFirst(), "unique_items_mtd"));
        }

        result.put("purchase", p);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. AP AGING  — bucket outstanding payables by age
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadApAging(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT
              COALESCE(SUM(due_amount) FILTER (
                  WHERE required_date >= (CURRENT_DATE - 30) OR required_date IS NULL), 0)
                                                    AS current_bal,
              COALESCE(SUM(due_amount) FILTER (
                  WHERE required_date BETWEEN (CURRENT_DATE - 60) AND (CURRENT_DATE - 31)), 0)
                                                    AS d31_60,
              COALESCE(SUM(due_amount) FILTER (
                  WHERE required_date BETWEEN (CURRENT_DATE - 90) AND (CURRENT_DATE - 61)), 0)
                                                    AS d61_90,
              COALESCE(SUM(due_amount) FILTER (
                  WHERE required_date < (CURRENT_DATE - 90)), 0)
                                                    AS d90_plus
            FROM global_business_documents
            WHERE organization_id = ?
              AND document_type   = 'PURCHASE_INVOICE'
              AND status NOT IN ('CANCELLED')
              AND COALESCE(due_amount, 0) > 0
              AND is_deleted       = false
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId);
        Map<String, Object> ag = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.getFirst();
            ag.put("current", toBD(r, "current_bal"));
            ag.put("d3160",   toBD(r, "d31_60"));
            ag.put("d6190",   toBD(r, "d61_90"));
            ag.put("d90plus", toBD(r, "d90_plus"));
        }
        result.put("apAging", ag);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. EXCEPTION SUMMARY COUNTS
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadExceptions(Map<String, Object> result, Long orgId,
                                  String today, String prev7, String prev5) {
        String sql = """
            SELECT
              COUNT(*) FILTER (WHERE document_type = 'GOODS_RECEIPT_NOTE'
                                 AND status NOT IN ('CONFIRMED','CANCELLED')
                                 AND document_date < ?::date)           AS grn_overdue,
              COUNT(*) FILTER (WHERE document_type = 'PURCHASE_INVOICE'
                                 AND required_date < ?::date
                                 AND COALESCE(due_amount,0) > 0)         AS ap_overdue,
              COUNT(*) FILTER (WHERE document_type = 'PURCHASE_INVOICE'
                                 AND accounting_posted = false
                                 AND status NOT IN ('CANCELLED'))         AS unposted,
              COUNT(*) FILTER (WHERE document_type = 'PURCHASE_REQUISITION'
                                 AND status NOT IN ('COMPLETED','CANCELLED','CONVERTED')
                                 AND document_date < ?::date)            AS pr_pending_5d,
              COUNT(*) FILTER (WHERE document_type IN (
                                   'PURCHASE_ORDER','GOODS_RECEIPT_NOTE','PURCHASE_INVOICE')
                                 AND status = 'CANCELLED'
                                 AND document_date >= (CURRENT_DATE - INTERVAL '30 days'))
                                                                         AS cancelled_30d
            FROM global_business_documents
            WHERE organization_id = ?
              AND is_deleted       = false
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, prev7, today, prev5, orgId);
        Map<String, Object> ex = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            ex.put("grnOverdue", toLong(r, "grn_overdue"));
            ex.put("apOverdue",  toLong(r, "ap_overdue"));
            ex.put("unposted",   toLong(r, "unposted"));
            ex.put("prPending",  toLong(r, "pr_pending_5d"));
            ex.put("cancelled",  toLong(r, "cancelled_30d"));
        }
        result.put("exceptions", ex);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. EXCEPTION DETAIL ROWS (for the table — max 20 rows)
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadExceptionDetails(Map<String, Object> result, Long orgId,
                                        String today, String prev7) {
        String sql = """
            SELECT priority, exception_type, doc_no, supplier_name,
                   value, delay_days, action_url
            FROM (
              /* GRN overdue > 7 days */
              SELECT 'CRITICAL'           AS priority,
                     'GRN Overdue (>7d)'  AS exception_type,
                     d.document_no        AS doc_no,
                     COALESCE(s.sub_account_name, '—') AS supplier_name,
                     COALESCE(d.total_amount, 0)        AS value,
                     (CURRENT_DATE - d.document_date)   AS delay_days,
                     '/purchase/grns'                   AS action_url
              FROM global_business_documents d
              LEFT JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
              WHERE d.organization_id = ?
                AND d.document_type   = 'GOODS_RECEIPT_NOTE'
                AND d.status NOT IN   ('CONFIRMED','CANCELLED')
                AND d.document_date   < ?::date
                AND d.is_deleted      = false

              UNION ALL
              /* AP Overdue invoices */
              SELECT 'CRITICAL',
                     'AP Overdue Invoice',
                     d.document_no,
                     COALESCE(s.sub_account_name, '—'),
                     COALESCE(d.due_amount, 0),
                     (CURRENT_DATE - d.required_date),
                     '/purchase/invoices'
              FROM global_business_documents d
              LEFT JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
              WHERE d.organization_id = ?
                AND d.document_type   = 'PURCHASE_INVOICE'
                AND d.required_date   < ?::date
                AND COALESCE(d.due_amount, 0) > 0
                AND d.is_deleted      = false

              UNION ALL
              /* Unposted invoices */
              SELECT 'HIGH',
                     'Invoice Unposted',
                     d.document_no,
                     COALESCE(s.sub_account_name, '—'),
                     COALESCE(d.total_amount, 0),
                     (CURRENT_DATE - d.document_date),
                     '/purchase/invoices'
              FROM global_business_documents d
              LEFT JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
              WHERE d.organization_id  = ?
                AND d.document_type    = 'PURCHASE_INVOICE'
                AND d.accounting_posted = false
                AND d.status NOT IN    ('CANCELLED')
                AND d.is_deleted       = false
                AND d.document_date    < (CURRENT_DATE - INTERVAL '2 days')

              UNION ALL
              /* PR pending > 5 days */
              SELECT 'MEDIUM',
                     'PR Pending >5d',
                     d.document_no,
                     '—',
                     COALESCE(d.total_amount, 0),
                     (CURRENT_DATE - d.document_date),
                     '/purchase/requisitions'
              FROM global_business_documents d
              WHERE d.organization_id = ?
                AND d.document_type   = 'PURCHASE_REQUISITION'
                AND d.status NOT IN   ('COMPLETED','CANCELLED','CONVERTED')
                AND d.document_date   < (CURRENT_DATE - INTERVAL '5 days')
                AND d.is_deleted      = false
            ) x
            ORDER BY
              CASE priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 ELSE 3 END,
              delay_days DESC
            LIMIT 20
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql,
            orgId, prev7,     // GRN overdue
            orgId, today,     // AP overdue
            orgId,            // unposted
            orgId             // PR pending
        );
        result.put("exceptionDetails", rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. TOP SUPPLIERS BY MTD PURCHASE VALUE
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadTopSuppliers(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT COALESCE(s.sub_account_name, '—') AS supplier_name,
                   COALESCE(SUM(d.total_amount), 0)   AS purchase_value
            FROM global_business_documents d
            JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'PURCHASE_ORDER'
              AND d.document_date  >= ?::date
              AND d.status NOT IN   ('CANCELLED')
              AND d.is_deleted      = false
            GROUP BY s.id, s.sub_account_name
            ORDER BY purchase_value DESC
            LIMIT 7
            """;
        result.put("topSuppliers", jdbc.queryForList(sql, orgId, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. TOP OVERDUE AP BY SUPPLIER
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadTopOverdueAP(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT COALESCE(s.sub_account_name, '—')  AS supplier_name,
                   COALESCE(SUM(d.due_amount), 0)      AS overdue_amount
            FROM global_business_documents d
            JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'PURCHASE_INVOICE'
              AND d.required_date   < ?::date
              AND COALESCE(d.due_amount, 0) > 0
              AND d.is_deleted      = false
            GROUP BY s.id, s.sub_account_name
            ORDER BY overdue_amount DESC
            LIMIT 7
            """;
        result.put("topOverdueAP", jdbc.queryForList(sql, orgId, today));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. CATEGORY BREAKDOWN (GRN lines MTD by item_type)
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadCategoryBreakdown(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT COALESCE(i.item_type, 'OTHER')   AS category,
                   COALESCE(SUM(l.line_amount), 0)  AS value
            FROM global_business_documents d
            JOIN global_business_document_lines l ON l.document_id = d.id
            JOIN inv_items i ON i.id = l.item_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'GOODS_RECEIPT_NOTE'
              AND d.status          = 'CONFIRMED'
              AND d.document_date  >= ?::date
              AND d.is_deleted      = false
            GROUP BY i.item_type
            ORDER BY value DESC
            LIMIT 8
            """;
        result.put("categoryBreakdown", jdbc.queryForList(sql, orgId, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. RECENT DOCUMENTS (last 15 across all purchase types)
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadRecentDocuments(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT d.id,
                   d.document_no,
                   d.document_type,
                   TO_CHAR(d.document_date, 'DD-Mon-YYYY')                  AS document_date,
                   COALESCE(s.sub_account_name, '—')                        AS supplier_name,
                   COALESCE(w.warehouse_name, '—')                          AS warehouse_name,
                   COALESCE(d.total_amount, 0)                               AS total_amount,
                   d.status,
                   d.accounting_posted,
                   d.stock_posted
            FROM global_business_documents d
            LEFT JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            LEFT JOIN org_warehouses w ON w.id = d.warehouse_id
            WHERE d.organization_id = ?
              AND d.document_type IN (
                    'PURCHASE_REQUISITION','REQUEST_FOR_QUOTATION',
                    'COMPARATIVE_STATEMENT','PURCHASE_ORDER',
                    'GOODS_RECEIPT_NOTE','PURCHASE_INVOICE','DEBIT_NOTE')
              AND d.is_deleted = false
            ORDER BY d.id DESC
            LIMIT 15
            """;
        result.put("recentDocuments", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. 12-MONTH TREND (PO value, GRN value, AP outstanding)
    // ─────────────────────────────────────────────────────────────────────────

    private void _loadMonthlyTrend(Map<String, Object> result, Long orgId) {
        String sql = """
            WITH months AS (
              SELECT to_char(m, 'Mon YY')              AS month,
                     date_trunc('month', m)             AS month_start
              FROM generate_series(
                     date_trunc('month', CURRENT_DATE - INTERVAL '11 months'),
                     date_trunc('month', CURRENT_DATE),
                     INTERVAL '1 month') AS m
            ),
            doc_agg AS (
              SELECT date_trunc('month', document_date) AS doc_month,
                     SUM(CASE WHEN document_type = 'PURCHASE_ORDER'
                              THEN COALESCE(total_amount, 0) ELSE 0 END)  AS po_value,
                     SUM(CASE WHEN document_type = 'GOODS_RECEIPT_NOTE'
                              AND status = 'CONFIRMED'
                              THEN COALESCE(total_amount, 0) ELSE 0 END)  AS grn_value,
                     SUM(CASE WHEN document_type = 'PURCHASE_INVOICE'
                              THEN COALESCE(total_amount, 0)
                                 - COALESCE(paid_amount, 0)
                              ELSE 0 END)                                  AS ap_value
              FROM global_business_documents
              WHERE organization_id = ?
                AND document_type IN ('PURCHASE_ORDER','GOODS_RECEIPT_NOTE','PURCHASE_INVOICE')
                AND status NOT IN ('CANCELLED')
                AND is_deleted = false
                AND document_date >= (CURRENT_DATE - INTERVAL '12 months')
              GROUP BY 1
            )
            SELECT m.month,
                   COALESCE(d.po_value,  0) AS po_value,
                   COALESCE(d.grn_value, 0) AS grn_value,
                   COALESCE(d.ap_value,  0) AS ap_value
            FROM months m
            LEFT JOIN doc_agg d ON d.doc_month = m.month_start
            ORDER BY m.month_start
            """;
        result.put("monthlyTrend", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static Long toLong(Map<String, Object> row, String col) {
        Object v = row.get(col);
        return v == null ? 0L : CommonUtils.toLong(v);
    }

    private static double toBD(Map<String, Object> row, String col) {
        Object v = row.get(col);
        if (v == null) return 0.0;
        try { return Double.parseDouble(v.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
