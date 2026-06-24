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
 * ProductionModuleDashboardService
 *
 * Provides full data payload for GET /production/dashboard/summary.
 * (Separate from the stub in ProductionServiceImpl which is used by production-dashboard.html)
 *
 * Response shape:
 * {
 *   production: {
 *     inProgressCount, completedMTD, mtdOutputValue, mtdMaterialCost,
 *     pendingApprovalCount, activeBOMCount, totalBOMCount,
 *     draftCount, submittedCount, rejectedMTD, cancelledMTD
 *   },
 *   statusBreakdown: [ {status, count} ]
 *   topFinishedItems:[ {itemName, qty, value} ]   — top 10 produced MTD
 *   topRawMaterials: [ {itemName, qty, cost} ]    — top 10 consumed MTD
 *   recentOrders:    [ last 15 production orders ]
 *   monthlyOutput:   [ {month, completedCount, outputValue, materialCost} ] — 12 months
 *   bomList:         [ {bomCode, bomName, finishedItem, version, isDefault} ]  — active BOMs
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionModuleDashboardService {

    private final JdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> summary() {
        Long   orgId    = SecurityHelper.requireOrgId();
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            _loadProductionKpis(result, orgId, mtdStart);
            _loadStatusBreakdown(result, orgId);
            _loadTopFinishedItems(result, orgId, mtdStart);
            _loadTopRawMaterials(result, orgId, mtdStart);
            _loadRecentOrders(result, orgId);
            _loadMonthlyOutput(result, orgId);
            _loadActiveBoms(result, orgId);
        } catch (Exception e) {
            log.error("ProductionDashboard summary error orgId={}", orgId, e);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. MASTER PRODUCTION KPIs
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadProductionKpis(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT
              COUNT(*) FILTER (WHERE status IN ('RELEASED','IN_PROGRESS'))         AS in_progress_count,
              COUNT(*) FILTER (WHERE status = 'COMPLETED' AND production_date >= ?::date) AS completed_mtd,
              COALESCE(SUM(total_cost)    FILTER (WHERE status = 'COMPLETED' AND production_date >= ?::date), 0) AS mtd_output_value,
              COALESCE(SUM(material_cost) FILTER (WHERE status = 'COMPLETED' AND production_date >= ?::date), 0) AS mtd_material_cost,
              COUNT(*) FILTER (WHERE status IN ('DRAFT','SUBMITTED'))               AS pending_approval_count,
              COUNT(*) FILTER (WHERE status = 'DRAFT')                             AS draft_count,
              COUNT(*) FILTER (WHERE status = 'SUBMITTED')                         AS submitted_count,
              COUNT(*) FILTER (WHERE status = 'REJECTED' AND production_date >= ?::date) AS rejected_mtd,
              COUNT(*) FILTER (WHERE status = 'CANCELLED' AND production_date >= ?::date) AS cancelled_mtd
            FROM prd_productions
            WHERE organization_id = ?
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql,
                mtdStart, mtdStart, mtdStart, mtdStart, mtdStart, orgId);

        Long activeBoms = jdbc.queryForObject(
                "SELECT COUNT(*) FROM prd_bom WHERE organization_id = ? AND is_active = true",
                Long.class, orgId);
        Long totalBoms = jdbc.queryForObject(
                "SELECT COUNT(*) FROM prd_bom WHERE organization_id = ?",
                Long.class, orgId);

        Map<String, Object> prd = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            prd.put("inProgressCount",      toLong(r, "in_progress_count"));
            prd.put("completedMTD",         toLong(r, "completed_mtd"));
            prd.put("mtdOutputValue",       toBD(r,   "mtd_output_value"));
            prd.put("mtdMaterialCost",      toBD(r,   "mtd_material_cost"));
            prd.put("pendingApprovalCount", toLong(r, "pending_approval_count"));
            prd.put("draftCount",           toLong(r, "draft_count"));
            prd.put("submittedCount",       toLong(r, "submitted_count"));
            prd.put("rejectedMTD",          toLong(r, "rejected_mtd"));
            prd.put("cancelledMTD",         toLong(r, "cancelled_mtd"));
        }
        prd.put("activeBOMCount", activeBoms != null ? activeBoms : 0L);
        prd.put("totalBOMCount",  totalBoms  != null ? totalBoms  : 0L);
        result.put("production", prd);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. STATUS BREAKDOWN
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadStatusBreakdown(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT status, COUNT(*) AS count
            FROM prd_productions
            WHERE organization_id = ?
            GROUP BY status
            ORDER BY count DESC
            """;
        result.put("statusBreakdown", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TOP FINISHED ITEMS PRODUCED MTD
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopFinishedItems(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT i.item_name,
                   COALESCE(SUM(p.produced_quantity), 0) AS qty,
                   COALESCE(SUM(p.total_cost), 0)        AS value
            FROM prd_productions p
            JOIN inv_items i ON i.id = p.finished_item_id
            WHERE p.organization_id = ?
              AND p.status         = 'COMPLETED'
              AND p.production_date >= ?::date
            GROUP BY i.id, i.item_name
            ORDER BY qty DESC
            LIMIT 10
            """;
        result.put("topFinishedItems", jdbc.queryForList(sql, orgId, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TOP RAW MATERIALS CONSUMED MTD
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopRawMaterials(Map<String, Object> result, Long orgId, String mtdStart) {
        String sql = """
            SELECT i.item_name,
                   COALESCE(SUM(pi.actual_quantity), 0) AS qty,
                   COALESCE(SUM(pi.total_cost), 0)      AS cost
            FROM prd_production_inputs pi
            JOIN prd_productions p ON p.id = pi.production_id
            JOIN inv_items i ON i.id = pi.raw_item_id
            WHERE p.organization_id = ?
              AND p.status         = 'COMPLETED'
              AND p.production_date >= ?::date
            GROUP BY i.id, i.item_name
            ORDER BY cost DESC
            LIMIT 10
            """;
        result.put("topRawMaterials", jdbc.queryForList(sql, orgId, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. RECENT ORDERS
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadRecentOrders(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT p.id, p.production_no,
                   TO_CHAR(p.production_date, 'DD-Mon-YYYY') AS production_date,
                   i.item_name AS finished_item,
                   p.planned_quantity, p.produced_quantity,
                   COALESCE(p.total_cost, 0) AS total_cost,
                   p.status
            FROM prd_productions p
            JOIN inv_items i ON i.id = p.finished_item_id
            WHERE p.organization_id = ?
            ORDER BY p.id DESC
            LIMIT 15
            """;
        result.put("recentOrders", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. 12-MONTH OUTPUT TREND
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadMonthlyOutput(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT TO_CHAR(DATE_TRUNC('month', production_date), 'Mon-YY') AS month,
                   COUNT(*) FILTER (WHERE status='COMPLETED')  AS completed_count,
                   COALESCE(SUM(total_cost)    FILTER (WHERE status='COMPLETED'), 0) AS output_value,
                   COALESCE(SUM(material_cost) FILTER (WHERE status='COMPLETED'), 0) AS material_cost
            FROM prd_productions
            WHERE organization_id = ?
              AND production_date >= (CURRENT_DATE - INTERVAL '12 months')
            GROUP BY DATE_TRUNC('month', production_date)
            ORDER BY DATE_TRUNC('month', production_date)
            """;
        result.put("monthlyOutput", jdbc.queryForList(sql, orgId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. ACTIVE BOMs
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadActiveBoms(Map<String, Object> result, Long orgId) {
        String sql = """
            SELECT b.bom_code, b.bom_name, b.bom_version,
                   i.item_name AS finished_item,
                   b.output_quantity,
                   u.symbol AS output_unit,
                   b.yield_percent,
                   b.is_default,
                   (SELECT COUNT(*) FROM prd_bom_items WHERE bom_id = b.id) AS raw_material_count
            FROM prd_bom b
            JOIN inv_items i ON i.id = b.finished_item_id
            JOIN inv_item_uom u ON u.id = b.output_unit_id
            WHERE b.organization_id = ?
              AND b.is_active = true
            ORDER BY b.is_default DESC, b.bom_code
            """;
        result.put("bomList", jdbc.queryForList(sql, orgId));
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
