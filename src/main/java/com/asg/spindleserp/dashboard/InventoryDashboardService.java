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
 * InventoryDashboardService
 *
 * Provides full data payload for GET /inventory/dashboard/summary.
 *
 * Response shape:
 * {
 *   inventory: {
 *     totalActiveItems, totalStockValue, lowStockCount, zeroStockCount,
 *     pendingTransfers, adjustmentMTD,
 *     totalWarehouses, totalCategories,
 *     itemsAddedMTD, stockInMTD, stockOutMTD
 *   },
 *   stockAlerts:       [ {itemCode, itemName, qty, reorderLevel, uom} ]  — top 15 low stock
 *   topValueItems:     [ {itemCode, itemName, stockValue, qty} ]          — top 10 by value
 *   warehouseStock:    [ {warehouseName, totalItems, totalValue} ]
 *   categoryBreakdown: [ {itemType, itemCount, stockValue} ]
 *   recentTransactions:[ last 15 stock movements ]
 *   monthlyMovement:   [ {month, stockInQty, stockOutQty} ] — 12 months
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryDashboardService {

    private final JdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> summary() {
        Long   orgId    = SecurityHelper.requireOrgId();
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            _loadInventoryKpis(result, orgId, mtdStart);
            _loadStockAlerts(result, orgId);
            _loadTopValueItems(result, orgId);
            _loadWarehouseStock(result, orgId);
            _loadCategoryBreakdown(result, orgId);
            _loadRecentTransactions(result, orgId);
            _loadMonthlyMovement(result, orgId);
        } catch (Exception e) {
            log.error("InventoryDashboard summary error orgId={}", orgId, e);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. MASTER INVENTORY KPIs
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadInventoryKpis(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT
              COUNT(DISTINCT i.id)                                          AS total_active_items,
              COALESCE(SUM(sb.quantity * sb.average_cost), 0)               AS total_stock_value,
              COUNT(DISTINCT i.id) FILTER (
                  WHERE i.reorder_level > 0 AND sb.quantity <= i.reorder_level) AS low_stock_count,
              COUNT(DISTINCT i.id) FILTER (
                  WHERE i.reorder_level > 0 AND sb.quantity <= 0)           AS zero_stock_count,
              (SELECT COUNT(*) FROM global_business_documents
               WHERE organization_id = ?
                 AND document_type   = 'STOCK_TRANSFER'
                 AND status NOT IN ('COMPLETED','CANCELLED')
                 AND is_deleted = false)                                    AS pending_transfers,
              (SELECT COUNT(*) FROM global_business_documents
               WHERE organization_id = ?
                 AND document_type   = 'STOCK_ADJUSTMENT'
                 AND document_date  >= ?::date
                 AND is_deleted = false)                                    AS adjustment_mtd,
              (SELECT COUNT(DISTINCT warehouse_id) FROM global_inventory_stock_balances sb2
               JOIN inv_items i2 ON i2.id = sb2.item_id
               WHERE i2.organization_id = ?)                               AS total_warehouses,
              (SELECT COUNT(DISTINCT category_id) FROM inv_items
               WHERE organization_id = ? AND is_active = true)             AS total_categories,
              (SELECT COUNT(*) FROM inv_items
               WHERE organization_id = ?
                 AND created_at >= ?::timestamp)                            AS items_added_mtd
            FROM inv_items i
            LEFT JOIN global_inventory_stock_balances sb ON sb.item_id = i.id
            WHERE i.organization_id = ?
              AND i.is_active = true
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                orgId, orgId, mtdStart, orgId, orgId, orgId, mtdStart, orgId);

        // MTD stock in / stock out from transactions
        String moveSql = """
            SELECT
              COALESCE(SUM(quantity) FILTER (WHERE movement_type IN
                ('PURCHASE_RECEIPT','PRODUCTION_RECEIPT','TRANSFER_IN','ADJUSTMENT_IN','RETURN_FROM_CUSTOMER')), 0)
                AS stock_in_mtd,
              COALESCE(SUM(quantity) FILTER (WHERE movement_type IN
                ('SALES_ISSUE','PRODUCTION_MATERIAL_ISSUE','TRANSFER_OUT','ADJUSTMENT_OUT','STORE_ISSUE')), 0)
                AS stock_out_mtd
            FROM global_inventory_transactions
            WHERE organization_id = ?
              AND transaction_date >= ?::date
            """;
        List<Map<String, Object>> moveRows = jdbc.queryForList(moveSql, orgId, mtdStart);

        Map<String, Object> inv = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            inv.put("totalActiveItems",   toLong(r, "total_active_items"));
            inv.put("totalStockValue",    toBD(r,   "total_stock_value"));
            inv.put("lowStockCount",      toLong(r, "low_stock_count"));
            inv.put("zeroStockCount",     toLong(r, "zero_stock_count"));
            inv.put("pendingTransfers",   toLong(r, "pending_transfers"));
            inv.put("adjustmentMTD",      toLong(r, "adjustment_mtd"));
            inv.put("totalWarehouses",    toLong(r, "total_warehouses"));
            inv.put("totalCategories",    toLong(r, "total_categories"));
            inv.put("itemsAddedMTD",      toLong(r, "items_added_mtd"));
        }
        if (!moveRows.isEmpty()) {
            Map<String, Object> r = moveRows.get(0);
            inv.put("stockInMTD",  toBD(r, "stock_in_mtd"));
            inv.put("stockOutMTD", toBD(r, "stock_out_mtd"));
        }
        result.put("inventory", inv);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. STOCK ALERTS — low / at-reorder
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadStockAlerts(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT i.item_code, i.item_name,
                   COALESCE(SUM(sb.quantity), 0)  AS qty,
                   i.reorder_level,
                   i.minimum_stock,
                   i.unit_of_measure              AS uom
            FROM inv_items i
            LEFT JOIN global_inventory_stock_balances sb ON sb.item_id = i.id
            WHERE i.organization_id = ?
              AND i.is_active = true
              AND i.reorder_level > 0
            GROUP BY i.id, i.item_code, i.item_name, i.reorder_level, i.minimum_stock, i.unit_of_measure
            HAVING COALESCE(SUM(sb.quantity), 0) <= i.reorder_level
            ORDER BY (COALESCE(SUM(sb.quantity), 0) / NULLIF(i.reorder_level, 0)) ASC
            LIMIT 15
            """;
        result.put("stockAlerts", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TOP VALUE ITEMS
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopValueItems(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT i.item_code, i.item_name,
                   COALESCE(SUM(sb.quantity), 0)                      AS qty,
                   COALESCE(SUM(sb.quantity * sb.average_cost), 0)    AS stock_value,
                   i.unit_of_measure                                   AS uom
            FROM inv_items i
            JOIN global_inventory_stock_balances sb ON sb.item_id = i.id
            WHERE i.organization_id = ?
              AND i.is_active = true
            GROUP BY i.id, i.item_code, i.item_name, i.unit_of_measure
            ORDER BY stock_value DESC
            LIMIT 10
            """;
        result.put("topValueItems", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. WAREHOUSE STOCK BREAKDOWN
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadWarehouseStock(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT w.warehouse_name,
                   COUNT(DISTINCT sb.item_id)                         AS total_items,
                   COALESCE(SUM(sb.quantity * sb.average_cost), 0)    AS total_value,
                   COALESCE(SUM(sb.quantity), 0)                      AS total_qty
            FROM org_warehouses w
            LEFT JOIN global_inventory_stock_balances sb ON sb.warehouse_id = w.id
            WHERE w.organization_id = ?
              AND w.is_active = true
            GROUP BY w.id, w.warehouse_name
            ORDER BY total_value DESC
            """;
        result.put("warehouseStock", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CATEGORY BREAKDOWN
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadCategoryBreakdown(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT i.item_type,
                   COUNT(DISTINCT i.id)                               AS item_count,
                   COALESCE(SUM(sb.quantity * sb.average_cost), 0)   AS stock_value
            FROM inv_items i
            LEFT JOIN global_inventory_stock_balances sb ON sb.item_id = i.id
            WHERE i.organization_id = ?
              AND i.is_active = true
            GROUP BY i.item_type
            ORDER BY stock_value DESC
            """;
        result.put("categoryBreakdown", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. RECENT TRANSACTIONS
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadRecentTransactions(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT t.id,
                   t.movement_type,
                   i.item_code,
                   i.item_name,
                   t.quantity,
                   t.unit_cost,
                   t.total_cost,
                   TO_CHAR(t.transaction_date, 'DD-Mon-YYYY') AS transaction_date,
                   w.warehouse_name,
                   d.document_no
            FROM global_inventory_transactions t
            JOIN inv_items i ON i.id = t.item_id
            JOIN org_warehouses w ON w.id = t.warehouse_id
            JOIN global_business_documents d ON d.id = t.business_document_id
            WHERE t.organization_id = ?
            ORDER BY t.id DESC
            LIMIT 15
            """;
        result.put("recentTransactions", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. 12-MONTH MOVEMENT TREND
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadMonthlyMovement(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT TO_CHAR(DATE_TRUNC('month', transaction_date), 'Mon-YY') AS month,
                   COALESCE(SUM(quantity) FILTER (WHERE movement_type IN
                     ('PURCHASE_RECEIPT','PRODUCTION_RECEIPT','TRANSFER_IN','ADJUSTMENT_IN')), 0) AS stock_in_qty,
                   COALESCE(SUM(quantity) FILTER (WHERE movement_type IN
                     ('SALES_ISSUE','PRODUCTION_MATERIAL_ISSUE','TRANSFER_OUT','ADJUSTMENT_OUT')), 0) AS stock_out_qty
            FROM global_inventory_transactions
            WHERE organization_id = ?
              AND transaction_date >= (CURRENT_DATE - INTERVAL '12 months')
            GROUP BY DATE_TRUNC('month', transaction_date)
            ORDER BY DATE_TRUNC('month', transaction_date)
            """;
        result.put("monthlyMovement", jdbc.queryForList(sql, orgId));
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
