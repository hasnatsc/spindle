package com.asg.spindleserp.dashboard;

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
 * DashboardService
 *
 * Provides the full ERP summary for the main dashboard.
 *
 * STRATEGY:
 *   • One optimised PostgreSQL CTE query returns all module KPIs in a single round-trip.
 *   • A few small supplementary queries fetch list data (approvals, stock alerts, top AR/AP).
 *   • Everything is scoped by organizationId from the session — no full-table scans.
 *   • MTD date range derived server-side — never trusted from the client.
 *
 * INDEXES relied upon (all pre-existing on the schema):
 *   idx_gbd_org, idx_gbd_type, idx_gbd_status, idx_gbd_date
 *   idx_jem_org, idx_jem_status, idx_jem_type, idx_jel_org
 *   idx_jel_account (sub-account type filter)
 *   idx_emp_org, idx_emp_status
 *   idx_att_org, idx_att_date
 *   idx_stock_item, idx_stock_wh
 *   idx_aprr_org, idx_aprr_status, idx_aprr_approver
 *   idx_prd2_org, idx_prd2_status
 *   idx_bgt_org, idx_bgt_status
 *   idx_fa_org, idx_fa_status
 *   idx_crl_org, idx_crl_status
 *   idx_cro_org, idx_cro_stage
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final JdbcTemplate jdbc;

    // ── Document-type constants (mirror DocumentType enum) ────────────────────
    private static final String PR   = "'PURCHASE_REQUISITION'";
    private static final String RFQ  = "'REQUEST_FOR_QUOTATION'";
    private static final String CS   = "'COMPARATIVE_STATEMENT'";
    private static final String PO   = "'PURCHASE_ORDER'";
    private static final String GRN  = "'GOODS_RECEIPT_NOTE'";
    private static final String PINV = "'PURCHASE_INVOICE'";
    private static final String SQ   = "'SALES_QUOTATION'";
    private static final String SO   = "'SALES_ORDER'";
    private static final String DO   = "'DELIVERY_ORDER'";
    private static final String DC   = "'DELIVERY_CHALLAN'";
    private static final String SINV = "'SALES_INVOICE'";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> erpSummary() {
        Long orgId = SecurityHelper.requireOrgId();

        LocalDate today     = LocalDate.now();
        LocalDate mtdStart  = today.withDayOfMonth(1);
        String todayStr     = today.toString();          // yyyy-MM-dd
        String mtdStartStr  = mtdStart.toString();

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // ── 1. BIG CTE — all document/voucher KPIs in one query ───────────
            _loadCteKpis(result, orgId, todayStr, mtdStartStr);

            // ── 2. Inventory stock value + low stock ──────────────────────────
            _loadInventoryKpis(result, orgId);

            // ── 3. Accounts AR/AP balances ────────────────────────────────────
            _loadAccountsKpis(result, orgId, todayStr);

            // ── 4. HRM — active employees, attendance, leaves ─────────────────
            _loadHrmKpis(result, orgId, todayStr);

            // ── 5. Production orders ──────────────────────────────────────────
            _loadProductionKpis(result, orgId, mtdStartStr);

            // ── 6. Commercial — LCs ───────────────────────────────────────────
            _loadCommercialKpis(result, orgId);

            // ── 7. CRM leads + opportunities ──────────────────────────────────
            _loadCrmKpis(result, orgId);

            // ── 8. Budget + Fixed Assets ──────────────────────────────────────
            _loadBudgetFaKpis(result, orgId);

            // ── 9. Approvals (pending inbox items — capped at 8 rows) ─────────
            _loadApprovalItems(result, orgId);

            // ── 10. Stock alerts (low / at-reorder — capped at 8) ────────────
            _loadStockAlerts(result, orgId);

            // ── 11. Exceptions cross-module ───────────────────────────────────
            _loadExceptions(result, orgId, todayStr);

            // ── 12. Top AR / AP parties ───────────────────────────────────────
            _loadTopParties(result, orgId);

            // ── 13. 12-month sales vs purchase trend ──────────────────────────
            _loadMonthlyTrend(result, orgId);

        } catch (Exception e) {
            log.error("Dashboard erpSummary error orgId={}", orgId, e);
            result.put("error", "Failed to load dashboard data: " + e.getMessage());
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — CTE KPIs (document counts + values)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Single CTE query covering all global_business_documents counts.
     * Uses conditional aggregation — one pass over the indexed table.
     *
     * Covering index used: (organization_id, document_type, status, document_date, is_deleted)
     * → idx_gbd_org + idx_gbd_type + idx_gbd_status + idx_gbd_deleted
     */
    private void _loadCteKpis(Map<String, Object> result,
                               Long orgId, String today, String mtdStart) {
        String sql = """
            SELECT
              -- ── PURCHASE LIFECYCLE ──────────────────────────────────
              COUNT(*) FILTER (WHERE document_type = 'PURCHASE_REQUISITION'
                              AND status NOT IN ('COMPLETED','CANCELLED'))
                                                         AS pr_pending_count,
              COUNT(*) FILTER (WHERE document_type = 'REQUEST_FOR_QUOTATION'
                              AND status NOT IN ('COMPLETED','CANCELLED'))
                                                         AS rfq_open_count,
              COUNT(*) FILTER (WHERE document_type = 'COMPARATIVE_STATEMENT'
                              AND status NOT IN ('COMPLETED','CANCELLED'))
                                                         AS cs_open_count,
              COUNT(*) FILTER (WHERE document_type = 'PURCHASE_ORDER'
                              AND status NOT IN ('COMPLETED','CANCELLED','CLOSED'))
                                                         AS po_open_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'PURCHASE_ORDER'
                    AND status NOT IN ('COMPLETED','CANCELLED','CLOSED')), 0)
                                                         AS po_open_value,
              COUNT(*) FILTER (WHERE document_type = 'GOODS_RECEIPT_NOTE'
                              AND status NOT IN ('POSTED','COMPLETED','CANCELLED'))
                                                         AS grn_pending_count,
              COUNT(*) FILTER (WHERE document_type = 'PURCHASE_INVOICE'
                              AND (accounting_posted = false OR stock_posted = false)
                              AND status NOT IN ('CANCELLED'))
                                                         AS pinv_unposted_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'PURCHASE_INVOICE'
                    AND document_date >= ?::date
                    AND status NOT IN ('CANCELLED')), 0)
                                                         AS purch_mtd_value,

              -- ── SALES LIFECYCLE ───────────────────────────────────────
              COUNT(*) FILTER (WHERE document_type = 'SALES_QUOTATION'
                              AND status NOT IN ('CONVERTED','CANCELLED','CLOSED'))
                                                         AS sq_open_count,
              COUNT(*) FILTER (WHERE document_type = 'SALES_ORDER'
                              AND status NOT IN ('COMPLETED','CANCELLED','CLOSED'))
                                                         AS so_open_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'SALES_ORDER'
                    AND status NOT IN ('COMPLETED','CANCELLED','CLOSED')), 0)
                                                         AS so_open_value,
              COUNT(*) FILTER (WHERE document_type = 'DELIVERY_ORDER'
                              AND status NOT IN ('COMPLETED','CANCELLED'))
                                                         AS do_pending_count,
              COUNT(*) FILTER (WHERE document_type = 'DELIVERY_CHALLAN'
                              AND status NOT IN ('COMPLETED','CANCELLED'))
                                                         AS dc_pending_count,
              COUNT(*) FILTER (WHERE document_type = 'SALES_INVOICE'
                              AND (accounting_posted = false OR stock_posted = false)
                              AND status NOT IN ('CANCELLED'))
                                                         AS sinv_unposted_count,
              COALESCE(SUM(total_amount) FILTER (
                  WHERE document_type = 'SALES_INVOICE'
                    AND document_date >= ?::date
                    AND status NOT IN ('CANCELLED')), 0)
                                                         AS sales_mtd_value,
              COALESCE(SUM(paid_amount) FILTER (
                  WHERE document_type = 'SALES_INVOICE'
                    AND document_date >= ?::date
                    AND status NOT IN ('CANCELLED')), 0)
                                                         AS sales_mtd_paid,

              -- ── DEBIT / CREDIT NOTES ──────────────────────────────────
              COUNT(*) FILTER (WHERE document_type = 'DEBIT_NOTE'
                              AND status NOT IN ('POSTED','CANCELLED'))
                                                         AS debit_note_open,
              COUNT(*) FILTER (WHERE document_type = 'CREDIT_NOTE'
                              AND status NOT IN ('POSTED','CANCELLED'))
                                                         AS credit_note_open

            FROM global_business_documents
            WHERE organization_id = ?
              AND is_deleted = false
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                mtdStart, mtdStart, mtdStart, orgId);

        Map<String, Object> purchase = new LinkedHashMap<>();
        Map<String, Object> sales    = new LinkedHashMap<>();
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        Map<String, Object> lcPurch  = new LinkedHashMap<>();
        Map<String, Object> lcSales  = new LinkedHashMap<>();

        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);

            // Purchase
            purchase.put("prPendingCount",        toLong(r, "pr_pending_count"));
            purchase.put("rfqOpenCount",          toLong(r, "rfq_open_count"));
            purchase.put("csOpenCount",           toLong(r, "cs_open_count"));
            purchase.put("openPOCount",           toLong(r, "po_open_count"));
            purchase.put("openPOValue",           toBD(r,  "po_open_value"));
            purchase.put("grnPendingCount",       toLong(r, "grn_pending_count"));
            purchase.put("invoiceUnpostedCount",  toLong(r, "pinv_unposted_count"));
            purchase.put("mtdPurchaseValue",      toBD(r,  "purch_mtd_value"));

            // Sales
            sales.put("openSQCount",              toLong(r, "sq_open_count"));
            sales.put("openSOCount",              toLong(r, "so_open_count"));
            sales.put("openSOValue",              toBD(r,  "so_open_value"));
            sales.put("deliveryPendingCount",     toLong(r, "do_pending_count") + toLong(r, "dc_pending_count"));
            sales.put("invoiceUnpostedCount",     toLong(r, "sinv_unposted_count"));
            sales.put("mtdSalesValue",            toBD(r,  "sales_mtd_value"));
            sales.put("mtdSalesPaid",             toBD(r,  "sales_mtd_paid"));

            // Lifecycle flows
            lcPurch.put("PR",  toLong(r, "pr_pending_count"));
            lcPurch.put("RFQ", toLong(r, "rfq_open_count"));
            lcPurch.put("CS",  toLong(r, "cs_open_count"));
            lcPurch.put("PO",  toLong(r, "po_open_count"));
            lcPurch.put("GRN", toLong(r, "grn_pending_count"));
            lcPurch.put("PI",  toLong(r, "pinv_unposted_count"));

            lcSales.put("SQ",   toLong(r, "sq_open_count"));
            lcSales.put("SO",   toLong(r, "so_open_count"));
            lcSales.put("DO",   toLong(r, "do_pending_count"));
            lcSales.put("DC",   toLong(r, "dc_pending_count"));
            lcSales.put("INV",  toLong(r, "sinv_unposted_count"));
            lcSales.put("PAID", toLong(r, "sales_mtd_paid") > 0 ? 1 : 0); // flag
        }

        result.put("purchase",  purchase);
        result.put("sales",     sales);
        lifecycle.put("purchase", lcPurch);
        lifecycle.put("sales",    lcSales);
        result.put("lifecycle", lifecycle);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Inventory KPIs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stock value = SUM(quantity * average_cost) from global_inventory_stock_balances.
     * Low stock = items where current qty ≤ reorder_level and reorder_level > 0.
     * Uses indexes: idx_stock_item (join), idx_item_org, idx_item_active
     */
    private void _loadInventoryKpis(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT
              COUNT(DISTINCT i.id)                                       AS total_active_items,
              COALESCE(SUM(sb.quantity * sb.average_cost), 0)            AS total_stock_value,
              COUNT(DISTINCT i.id) FILTER (
                  WHERE i.reorder_level > 0
                    AND sb.quantity <= i.reorder_level)                  AS low_stock_count,
              COUNT(DISTINCT i.id) FILTER (
                  WHERE i.reorder_level > 0
                    AND sb.quantity <= 0)                                AS zero_stock_count,
              (SELECT COUNT(*) FROM global_business_documents
               WHERE organization_id = ?
                 AND document_type   = 'STOCK_TRANSFER'
                 AND status NOT IN ('COMPLETED','CANCELLED')
                 AND is_deleted = false)                                 AS pending_transfers,
              (SELECT COUNT(*) FROM global_business_documents
               WHERE organization_id = ?
                 AND document_type   = 'STOCK_ADJUSTMENT'
                 AND EXTRACT(YEAR  FROM document_date) = EXTRACT(YEAR  FROM CURRENT_DATE)
                 AND EXTRACT(MONTH FROM document_date) = EXTRACT(MONTH FROM CURRENT_DATE)
                 AND is_deleted = false)                                 AS adjustment_mtd
            FROM inv_items i
            JOIN global_inventory_stock_balances sb ON sb.item_id = i.id
            WHERE i.organization_id = ?
              AND i.is_active = true
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId, orgId, orgId);
        Map<String, Object> inv = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            inv.put("totalActiveItems", toLong(r, "total_active_items"));
            inv.put("totalStockValue",  toBD(r,  "total_stock_value"));
            inv.put("lowStockCount",    toLong(r, "low_stock_count"));
            inv.put("zeroStockCount",   toLong(r, "zero_stock_count"));
            inv.put("pendingTransfers", toLong(r, "pending_transfers"));
            inv.put("adjustmentMTD",    toLong(r, "adjustment_mtd"));
        }
        result.put("inventory", inv);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Accounts AR/AP KPIs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * AR  = SUM of credit-balance sub-accounts (CUSTOMER type) with positive outstanding.
     * AP  = SUM of debit-balance sub-accounts  (SUPPLIER type) with positive outstanding.
     * Due ≤7d = vouchers with due_date between today and today+7.
     * Cash/Bank balance = SUM current_balance for BANK + CASH sub-accounts.
     *
     * Uses indexes: idx_jem_org, idx_jem_status, idx_jem_due, idx_jem_party
     */
    private void _loadAccountsKpis(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT
              -- AR: unposted SALES_INVOICE total minus paid_amount
              COALESCE((
                SELECT SUM(d.total_amount - COALESCE(d.paid_amount,0))
                FROM global_business_documents d
                WHERE d.organization_id = ?
                  AND d.document_type   = 'SALES_INVOICE'
                  AND d.status NOT IN ('CANCELLED','REVERSED')
                  AND COALESCE(d.total_amount,0) > COALESCE(d.paid_amount,0)
                  AND d.is_deleted = false
              ), 0)                                                       AS total_receivable,

              -- Overdue AR: due_amount > 0 and required_date < today
              COALESCE((
                SELECT SUM(COALESCE(d.due_amount,0))
                FROM global_business_documents d
                WHERE d.organization_id = ?
                  AND d.document_type   = 'SALES_INVOICE'
                  AND d.required_date   < ?::date
                  AND COALESCE(d.due_amount,0) > 0
                  AND d.is_deleted = false
              ), 0)                                                       AS overdue_receivable,

              -- AP: unposted PURCHASE_INVOICE outstanding
              COALESCE((
                SELECT SUM(d.total_amount - COALESCE(d.paid_amount,0))
                FROM global_business_documents d
                WHERE d.organization_id = ?
                  AND d.document_type   = 'PURCHASE_INVOICE'
                  AND d.status NOT IN ('CANCELLED','REVERSED')
                  AND COALESCE(d.total_amount,0) > COALESCE(d.paid_amount,0)
                  AND d.is_deleted = false
              ), 0)                                                       AS total_payable,

              -- AP due within 7 days
              COALESCE((
                SELECT SUM(COALESCE(d.due_amount,0))
                FROM global_business_documents d
                WHERE d.organization_id = ?
                  AND d.document_type   = 'PURCHASE_INVOICE'
                  AND d.required_date   BETWEEN ?::date AND (?::date + INTERVAL '7 days')
                  AND COALESCE(d.due_amount,0) > 0
                  AND d.is_deleted = false
              ), 0)                                                       AS due_soon_payable,

              -- Unposted vouchers (journal entries not posted)
              (SELECT COUNT(*) FROM acc_journal_entry_master
               WHERE organization_id = ?
                 AND is_posted = false
                 AND voucher_status NOT IN ('CANCELLED','REVERSED'))      AS unposted_voucher_count,

              -- Overdue vouchers
              (SELECT COUNT(*) FROM acc_journal_entry_master
               WHERE organization_id = ?
                 AND is_posted = false
                 AND due_date  < ?::date
                 AND voucher_status NOT IN ('CANCELLED','REVERSED'))      AS overdue_voucher_count,

              -- Cash + Bank balance (from sub-accounts)
              COALESCE((
                SELECT SUM(s.current_balance)
                FROM acc_chart_of_accounts_sub s
                WHERE s.organization_id   = ?
                  AND s.sub_account_type IN ('BANK','CASH')
                  AND s.is_active = true
              ), 0)                                                       AS cash_bank_balance
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                orgId, orgId, today,
                orgId, orgId, today, today,
                orgId, orgId, today,
                orgId);

        Map<String, Object> acc = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            acc.put("totalReceivable",    toBD(r,  "total_receivable"));
            acc.put("overdueReceivable",  toBD(r,  "overdue_receivable"));
            acc.put("totalPayable",       toBD(r,  "total_payable"));
            acc.put("dueSoonPayable",     toBD(r,  "due_soon_payable"));
            acc.put("unpostedVoucherCount", toLong(r, "unposted_voucher_count"));
            acc.put("overdueVoucherCount",  toLong(r, "overdue_voucher_count"));
            acc.put("cashBankBalance",    toBD(r,  "cash_bank_balance"));
        }
        result.put("accounts", acc);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — HRM KPIs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uses indexes: idx_emp_org, idx_emp_status, idx_att_org, idx_att_date, idx_leave_emp
     */
    private void _loadHrmKpis(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT
              -- Active employees
              (SELECT COUNT(*) FROM hrm_employees
               WHERE organization_id = ?
                 AND status = 'ACTIVE')                                  AS active_employees,

              -- Distinct departments with active employees
              (SELECT COUNT(DISTINCT department_id) FROM hrm_employees
               WHERE organization_id = ?
                 AND status = 'ACTIVE')                                  AS dept_count,

              -- Present today
              (SELECT COUNT(*) FROM hrm_attendances a
               JOIN hrm_employees e ON e.id = a.employee_id
               WHERE e.organization_id = ?
                 AND a.att_date  = ?::date
                 AND a.status    = 'PRESENT')                            AS present_today,

              -- On leave today (approved)
              (SELECT COUNT(*) FROM hrm_employee_leaves l
               JOIN hrm_employees e ON e.id = l.employee_id
               WHERE e.organization_id = ?
                 AND l.status    = 'APPROVED'
                 AND ?::date BETWEEN l.start_date AND l.end_date)        AS on_leave_today,

              -- Pending leave requests
              (SELECT COUNT(*) FROM hrm_employee_leaves l
               JOIN hrm_employees e ON e.id = l.employee_id
               WHERE e.organization_id = ?
                 AND l.status = 'PENDING')                               AS pending_leaves,

              -- Current payroll run status
              (SELECT COALESCE(
                 (SELECT status FROM hrm_payroll_runs
                  WHERE organization_id = ?
                  ORDER BY run_date DESC LIMIT 1), 'NONE'))              AS current_payroll_status
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                orgId, orgId, orgId, today, orgId, today, orgId, orgId);

        Map<String, Object> hrm = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            hrm.put("activeEmployees",      toLong(r, "active_employees"));
            hrm.put("deptCount",            toLong(r, "dept_count"));
            hrm.put("presentToday",         toLong(r, "present_today"));
            hrm.put("onLeaveToday",         toLong(r, "on_leave_today"));
            hrm.put("pendingLeaves",        toLong(r, "pending_leaves"));
            hrm.put("currentPayrollStatus", Objects.toString(r.get("current_payroll_status"), "NONE"));
        }
        result.put("hrm", hrm);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Production KPIs
    // ─────────────────────────────────────────────────────────────────────────

    /** Uses indexes: idx_prd2_org, idx_prd2_status, idx_bom_org */
    private void _loadProductionKpis(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM prd_productions
               WHERE organization_id = ?
                 AND status IN ('RELEASED','IN_PROGRESS'))               AS in_progress_count,

              (SELECT COUNT(*) FROM prd_productions
               WHERE organization_id = ?
                 AND status = 'COMPLETED'
                 AND production_date >= ?::date)                         AS completed_mtd,

              COALESCE((
               SELECT SUM(total_cost) FROM prd_productions
               WHERE organization_id = ?
                 AND status = 'COMPLETED'
                 AND production_date >= ?::date), 0)                     AS mtd_output_value,

              (SELECT COUNT(*) FROM prd_productions
               WHERE organization_id = ?
                 AND status IN ('DRAFT','SUBMITTED'))                    AS pending_approval_count,

              (SELECT COUNT(*) FROM prd_bom
               WHERE organization_id = ?
                 AND is_active = true)                                   AS active_bom_count
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                orgId, orgId, mtdStart, orgId, mtdStart, orgId, orgId);

        Map<String, Object> prd = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            prd.put("inProgressCount",      toLong(r, "in_progress_count"));
            prd.put("completedMTD",         toLong(r, "completed_mtd"));
            prd.put("mtdOutputValue",       toBD(r,   "mtd_output_value"));
            prd.put("pendingApprovalCount", toLong(r, "pending_approval_count"));
            prd.put("activeBOMCount",       toLong(r, "active_bom_count"));
        }
        result.put("production", prd);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Commercial KPIs (LCs)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * LC data lives in acc_chart_of_accounts_sub (sub_account_type = 'LC').
     * Uses indexes: idx_sub_org, idx_sub_type
     */
    private void _loadCommercialKpis(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT
              COUNT(*) FILTER (WHERE lc_status IN ('ACTIVE','ACCEPTED','TRANSFERRED')
                              AND lc_type     = 'EXPORT')                AS export_lc_count,
              COUNT(*) FILTER (WHERE lc_status IN ('ACTIVE','ACCEPTED','TRANSFERRED')
                              AND lc_type     = 'IMPORT')                AS import_lc_count,
              COUNT(*) FILTER (WHERE lc_status IN ('ACTIVE','ACCEPTED','TRANSFERRED'))
                                                                         AS active_lc_count,
              COALESCE(SUM(lc_amount) FILTER (
                  WHERE lc_status IN ('ACTIVE','ACCEPTED','TRANSFERRED')), 0)
                                                                         AS active_lc_value,
              COUNT(*) FILTER (WHERE lc_status = 'EXPIRED')             AS expired_lc_count,
              -- PI pending to LC: EXPORT_PROFORMA_INVOICE not yet converted
              (SELECT COUNT(*) FROM global_business_documents
               WHERE organization_id = ?
                 AND document_type   = 'EXPORT_PROFORMA_INVOICE'
                 AND status NOT IN ('CONVERTED','CANCELLED','CLOSED')
                 AND is_deleted = false)                                  AS pi_pending_count
            FROM acc_chart_of_accounts_sub
            WHERE organization_id    = ?
              AND sub_account_type   = 'LC'
              AND is_active          = true
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId, orgId);

        Map<String, Object> com = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            com.put("exportLCCount",  toLong(r, "export_lc_count"));
            com.put("importLCCount",  toLong(r, "import_lc_count"));
            com.put("activeLCCount",  toLong(r, "active_lc_count"));
            com.put("activeLCValue",  toBD(r,   "active_lc_value"));
            com.put("expiredLCCount", toLong(r, "expired_lc_count"));
            com.put("piPendingCount", toLong(r, "pi_pending_count"));
        }
        result.put("commercial", com);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — CRM KPIs
    // ─────────────────────────────────────────────────────────────────────────

    /** Uses indexes: idx_crl_org, idx_crl_status, idx_cro_org, idx_cro_stage */
    private void _loadCrmKpis(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM crm_leads
               WHERE organization_id = ?
                 AND status NOT IN ('CONVERTED','LOST','DORMANT'))        AS active_leads_count,

              (SELECT COUNT(*) FROM crm_opportunities
               WHERE organization_id = ?
                 AND stage NOT IN ('WON','LOST'))                         AS open_opportunity_count,

              COALESCE((
               SELECT SUM(estimated_value) FROM crm_opportunities
               WHERE organization_id = ?
                 AND stage NOT IN ('WON','LOST')), 0)                    AS pipeline_value
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId, orgId, orgId);
        Map<String, Object> crm = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            crm.put("activeLeadsCount",       toLong(r, "active_leads_count"));
            crm.put("openOpportunityCount",   toLong(r, "open_opportunity_count"));
            crm.put("pipelineValue",          toBD(r,   "pipeline_value"));
        }
        result.put("crm", crm);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Budget + Fixed Assets
    // ─────────────────────────────────────────────────────────────────────────

    /** Uses indexes: idx_bgt_org, idx_bgt_status, idx_fa_org, idx_fa_status */
    private void _loadBudgetFaKpis(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM bgt_budgets
               WHERE organization_id = ?
                 AND status IN ('ACTIVE','APPROVED'))                     AS active_budget_count,

              (SELECT COUNT(*) FROM bgt_budget_lines bl
               JOIN bgt_budgets b ON b.id = bl.budget_id
               WHERE b.organization_id = ?
                 AND b.status IN ('ACTIVE','APPROVED')
                 AND bl.actual_amount > bl.revised_amount
                 AND bl.revised_amount > 0)                              AS over_budget_line_count,

              (SELECT COUNT(*) FROM fa_assets
               WHERE organization_id = ?
                 AND status = 'ACTIVE')                                  AS active_asset_count
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId, orgId, orgId);
        Map<String, Object> bgt = new LinkedHashMap<>();
        Map<String, Object> fa  = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            bgt.put("activeBudgetCount",    toLong(r, "active_budget_count"));
            bgt.put("overBudgetLineCount",  toLong(r, "over_budget_line_count"));
            fa.put("activeAssetCount",      toLong(r, "active_asset_count"));
        }
        result.put("budget",      bgt);
        result.put("fixedAssets", fa);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Approval Items
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pending approvals assigned to the current user (or pending globally if super-admin).
     * Capped at 8 rows for the mini-panel. Uses idx_aprr_org, idx_aprr_status.
     */
    private void _loadApprovalItems(Map<String, Object> result, Long orgId) {
        // Summary counts
        String countSql = """
            SELECT
              COUNT(*) FILTER (WHERE status = 'PENDING')                 AS pending_count,
              COUNT(*) FILTER (WHERE status = 'PENDING'
                              AND due_date < CURRENT_DATE)               AS overdue_count
            FROM apr_requests
            WHERE organization_id = ?
              AND status IN ('PENDING','IN_REVIEW')
            """;

        List<Map<String, Object>> cntRows = jdbc.queryForList(countSql, orgId);
        Map<String, Object> aprSummary = new LinkedHashMap<>();
        if (!cntRows.isEmpty()) {
            aprSummary.put("pendingCount",  toLong(cntRows.get(0), "pending_count"));
            aprSummary.put("overdueCount",  toLong(cntRows.get(0), "overdue_count"));
        }
        result.put("approvals", aprSummary);

        // Item list
        String itemSql = """
            SELECT ar.id,
                   ar.reference_number,
                   ar.document_type,
                   ar.document_summary,
                   ar.document_amount,
                   ar.is_urgent,
                   ar.current_level_number,
                   ar.due_date
            FROM apr_requests ar
            WHERE ar.organization_id = ?
              AND ar.status IN ('PENDING','IN_REVIEW')
            ORDER BY ar.is_urgent DESC, ar.due_date ASC NULLS LAST
            LIMIT 8
            """;

        List<Map<String, Object>> items = jdbc.queryForList(itemSql, orgId);
        result.put("pendingApprovalItems", items);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Stock Alerts
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Items where quantity ≤ reorder_level (and reorder_level > 0).
     * Ordered by severity (qty/reorder_level ratio). Capped at 8.
     * Uses idx_stock_item JOIN idx_item_org.
     */
    private void _loadStockAlerts(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT i.item_name,
                   i.item_code,
                   COALESCE(SUM(sb.quantity), 0)        AS quantity,
                   i.reorder_level,
                   i.minimum_stock,
                   i.unit_of_measure
            FROM inv_items i
            LEFT JOIN global_inventory_stock_balances sb ON sb.item_id = i.id
            WHERE i.organization_id = ?
              AND i.is_active        = true
              AND i.reorder_level    > 0
            GROUP BY i.id, i.item_name, i.item_code, i.reorder_level,
                     i.minimum_stock, i.unit_of_measure
            HAVING COALESCE(SUM(sb.quantity), 0) <= i.reorder_level
            ORDER BY (COALESCE(SUM(sb.quantity), 0) / NULLIF(i.reorder_level, 0)) ASC
            LIMIT 8
            """;

        List<Map<String, Object>> alerts = jdbc.queryForList(sql, orgId);
        result.put("stockAlerts", alerts);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Cross-Module Exceptions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Assembles a prioritised exception list from multiple modules.
     * Each module contributes its own exception type; we UNION ALL them.
     * The final ORDER is: CRITICAL > HIGH > MEDIUM.
     */
    private void _loadExceptions(Map<String, Object> result, Long orgId, String today) {
        String sql = """
            SELECT priority, module, exception_type, cnt AS count, total_value AS value, action_url
            FROM (
              -- Expired LCs
              SELECT 'CRITICAL' AS priority, 'COMMERCIAL' AS module,
                     'Expired LC' AS exception_type,
                     COUNT(*)::bigint AS cnt,
                     COALESCE(SUM(lc_amount),0) AS total_value,
                     '/commercial/lcs' AS action_url
              FROM acc_chart_of_accounts_sub
              WHERE organization_id = ? AND sub_account_type = 'LC'
                AND lc_status = 'EXPIRED' AND is_active = true

              UNION ALL
              -- Overdue Sales Invoices
              SELECT 'CRITICAL', 'SALES', 'Overdue Invoice',
                     COUNT(*), COALESCE(SUM(due_amount),0), '/sales/invoices'
              FROM global_business_documents
              WHERE organization_id = ? AND document_type = 'SALES_INVOICE'
                AND required_date   < ?::date
                AND COALESCE(due_amount,0) > 0
                AND is_deleted = false

              UNION ALL
              -- GRN Pending > 7 days
              SELECT 'HIGH', 'PURCHASE', 'GRN Overdue (>7d)',
                     COUNT(*), COALESCE(SUM(total_amount),0), '/purchase/grns'
              FROM global_business_documents
              WHERE organization_id = ? AND document_type = 'GOODS_RECEIPT_NOTE'
                AND status NOT IN ('POSTED','COMPLETED','CANCELLED')
                AND document_date   < (?::date - INTERVAL '7 days')
                AND is_deleted = false

              UNION ALL
              -- Overdue Approvals
              SELECT 'CRITICAL', 'APPROVALS', 'Overdue Approval',
                     COUNT(*), NULL, '/approval/inbox'
              FROM apr_requests
              WHERE organization_id = ? AND status IN ('PENDING','IN_REVIEW')
                AND due_date        < ?::date

              UNION ALL
              -- Unposted vouchers > 3 days old
              SELECT 'HIGH', 'ACCOUNTS', 'Unposted Vouchers (>3d)',
                     COUNT(*), COALESCE(SUM(total_amount),0), '/accounts/journals'
              FROM acc_journal_entry_master
              WHERE organization_id = ? AND is_posted = false
                AND voucher_status NOT IN ('CANCELLED','REVERSED')
                AND voucher_date    < (?::date - INTERVAL '3 days')

              UNION ALL
              -- Low / Zero stock
              SELECT 'MEDIUM', 'INVENTORY', 'Low / Zero Stock Items',
                     COUNT(DISTINCT i.id), NULL, '/inventory/items'
              FROM inv_items i
              LEFT JOIN global_inventory_stock_balances sb ON sb.item_id = i.id
              WHERE i.organization_id = ? AND i.is_active = true
                AND i.reorder_level   > 0
              GROUP BY 1,2,3,6
              HAVING COALESCE(SUM(sb.quantity),0) <= i.reorder_level

              UNION ALL
              -- Delivery overdue
              SELECT 'HIGH', 'SALES', 'Delivery Overdue',
                     COUNT(*), COALESCE(SUM(total_amount),0), '/sales/deliveries'
              FROM global_business_documents
              WHERE organization_id = ? AND document_type IN ('DELIVERY_ORDER','DELIVERY_CHALLAN')
                AND status NOT IN ('COMPLETED','CANCELLED')
                AND required_date   < ?::date
                AND is_deleted = false
            ) x
            WHERE cnt > 0
            ORDER BY
              CASE priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 ELSE 3 END,
              cnt DESC
            LIMIT 20
            """;

        List<Map<String, Object>> exc = jdbc.queryForList(sql,
                orgId,
                orgId, today,
                orgId, today,
                orgId, today,
                orgId, today,
                orgId,
                orgId, today);

        result.put("exceptions", exc);

        // Build summary counts
        long critical = exc.stream().filter(r -> "CRITICAL".equals(r.get("priority"))).count();
        long high     = exc.stream().filter(r -> "HIGH".equals(r.get("priority"))).count();
        long medium   = exc.stream().filter(r -> "MEDIUM".equals(r.get("priority"))).count();

        // SLA breach count from approvals
        String slaSql = """
            SELECT COUNT(*) AS sla_breach
            FROM apr_requests
            WHERE organization_id = ?
              AND status IN ('PENDING','IN_REVIEW')
              AND due_date < CURRENT_DATE
            """;
        long slaBreach = 0;
        try {
            List<Map<String, Object>> slaRows = jdbc.queryForList(slaSql, orgId);
            if (!slaRows.isEmpty()) slaBreach = toLong(slaRows.get(0), "sla_breach");
        } catch (Exception ignored) {}

        Map<String, Object> excSum = new LinkedHashMap<>();
        excSum.put("critical",  critical);
        excSum.put("high",      high);
        excSum.put("medium",    medium);
        excSum.put("slaBreach", slaBreach);
        result.put("exceptionSummary", excSum);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Top AR / AP Parties
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Top 5 customers by outstanding receivable.
     * Top 5 suppliers by outstanding payable.
     * Uses idx_sub_org, idx_sub_type, idx_gbd_party.
     */
    private void _loadTopParties(Map<String, Object> result, Long orgId) {
        // Top Receivables
        String arSql = """
            SELECT s.sub_account_name AS party_name,
                   COALESCE(SUM(d.total_amount - COALESCE(d.paid_amount,0)), 0) AS balance
            FROM global_business_documents d
            JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'SALES_INVOICE'
              AND d.status NOT IN ('CANCELLED','REVERSED')
              AND COALESCE(d.total_amount,0) > COALESCE(d.paid_amount,0)
              AND d.is_deleted = false
              AND s.sub_account_type = 'CUSTOMER'
            GROUP BY s.id, s.sub_account_name
            ORDER BY balance DESC
            LIMIT 5
            """;

        // Top Payables
        String apSql = """
            SELECT s.sub_account_name AS party_name,
                   COALESCE(SUM(d.total_amount - COALESCE(d.paid_amount,0)), 0) AS balance
            FROM global_business_documents d
            JOIN acc_chart_of_accounts_sub s ON s.id = d.party_id
            WHERE d.organization_id = ?
              AND d.document_type   = 'PURCHASE_INVOICE'
              AND d.status NOT IN ('CANCELLED','REVERSED')
              AND COALESCE(d.total_amount,0) > COALESCE(d.paid_amount,0)
              AND d.is_deleted = false
              AND s.sub_account_type = 'SUPPLIER'
            GROUP BY s.id, s.sub_account_name
            ORDER BY balance DESC
            LIMIT 5
            """;

        result.put("topReceivables", jdbc.queryForList(arSql, orgId));
        result.put("topPayables",    jdbc.queryForList(apSql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — 12-Month Monthly Trend
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns last 12 months of Sales + Purchase totals for the trend chart.
     * Uses generate_series to guarantee all 12 months appear even if no data.
     * Uses indexes: idx_gbd_org, idx_gbd_type, idx_gbd_date.
     */
    private void _loadMonthlyTrend(Map<String, Object> result, Long orgId) {
        String sql = """
            WITH months AS (
              SELECT to_char(m, 'Mon YY') AS month_label,
                     date_trunc('month', m)          AS month_start,
                     date_trunc('month', m) + INTERVAL '1 month' - INTERVAL '1 day' AS month_end
              FROM generate_series(
                     date_trunc('month', CURRENT_DATE - INTERVAL '11 months'),
                     date_trunc('month', CURRENT_DATE),
                     INTERVAL '1 month') AS m
            ),
            doc_agg AS (
              SELECT date_trunc('month', document_date) AS doc_month,
                     SUM(CASE WHEN document_type = 'SALES_INVOICE'
                              THEN COALESCE(total_amount,0) ELSE 0 END)    AS sales_value,
                     SUM(CASE WHEN document_type = 'PURCHASE_INVOICE'
                              THEN COALESCE(total_amount,0) ELSE 0 END)    AS purchase_value,
                     SUM(CASE WHEN document_type = 'SALES_INVOICE'
                              THEN COALESCE(total_amount,0) - COALESCE(paid_amount,0)
                              ELSE 0 END)                                   AS outstanding_ar
              FROM global_business_documents
              WHERE organization_id = ?
                AND document_type IN ('SALES_INVOICE','PURCHASE_INVOICE')
                AND status NOT IN ('CANCELLED','REVERSED')
                AND is_deleted = false
                AND document_date >= (CURRENT_DATE - INTERVAL '12 months')
              GROUP BY 1
            )
            SELECT m.month_label                                   AS month,
                   COALESCE(d.sales_value,    0)                   AS sales_value,
                   COALESCE(d.purchase_value, 0)                   AS purchase_value,
                   COALESCE(d.outstanding_ar, 0)                   AS outstanding_ar
            FROM months m
            LEFT JOIN doc_agg d ON d.doc_month = m.month_start
            ORDER BY m.month_start
            """;

        List<Map<String, Object>> trend = jdbc.queryForList(sql, orgId);
        result.put("monthlyTrend", trend);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Helper converters
    // ─────────────────────────────────────────────────────────────────────────

    private static long toLong(Map<String, Object> row, String col) {
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
