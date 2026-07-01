// Path: com/asg/spindleserp/ecommerce/dashboard/EcDashboardService.java
package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EcDashboardService — assembles every metric the eCommerce dashboard needs.
 *
 * All queries are read-only JDBC (no JPA) to match the existing dashboard
 * service pattern (SalesDashboardService, ProductionModuleDashboardService, etc.)
 * and to keep the summary endpoint fast under load.
 *
 * Data sections returned to the controller:
 *   1. kpi           — headline counts + monetary totals (today / MTD / all-time)
 *   2. orderStatus   — order count breakdown by status
 *   3. revenueChart  — 12-month revenue + order-count trend (line chart)
 *   4. topProducts   — top 10 products by MTD revenue (bar chart)
 *   5. topCustomers  — top 10 customers by all-time spend
 *   6. categoryRevenue  — revenue share by storefront category (doughnut)
 *   7. paymentMethod — payment method breakdown (doughnut)
 *   8. exceptions    — pending returns, unreviewed reviews, active abandoned carts,
 *                      unfulfilled orders, unposted payments
 *   9. recentOrders  — last 15 orders (table)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcDashboardService {

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        String f  = orgId != null ? " AND o.organization_id = " + orgId : "";
        String fc = orgId != null ? " AND c.organization_id = " + orgId : "";
        String fp = orgId != null ? " AND p.organization_id = " + orgId : "";

        String today    = LocalDate.now().toString();
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();

        Map<String, Object> res = new LinkedHashMap<>();
        _loadKpi(res, orgId, today, mtdStart);
        _loadOrderStatus(res, orgId);
        _loadRevenueChart(res, orgId);
        _loadTopProducts(res, orgId, mtdStart);
        _loadTopCustomers(res, orgId);
        _loadCategoryRevenue(res, orgId, mtdStart);
        _loadPaymentMethod(res, orgId, mtdStart);
        _loadExceptions(res, orgId, today);
        _loadRecentOrders(res, orgId);
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. KPI HEADLINE CARDS
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadKpi(Map<String, Object> res, Long orgId, String today, String mtdStart) {
        String f = orgId != null ? " AND organization_id = " + orgId : "";

        // Orders
        String orderSql = """
            SELECT
                COUNT(*) FILTER (WHERE DATE(created_at) = ?::date
                                   AND order_status NOT IN ('CANCELLED'))   AS orders_today,
                COUNT(*) FILTER (WHERE DATE(created_at) >= ?::date
                                   AND order_status NOT IN ('CANCELLED'))   AS orders_mtd,
                COUNT(*) FILTER (WHERE order_status = 'PENDING')            AS orders_pending,
                COUNT(*) FILTER (WHERE order_status = 'PROCESSING')         AS orders_processing,
                COUNT(*) FILTER (WHERE order_status = 'DELIVERED'
                                    OR order_status = 'COMPLETED')          AS orders_completed,
                COUNT(*) FILTER (WHERE order_status = 'CANCELLED')          AS orders_cancelled,
                COALESCE(SUM(grand_total) FILTER (
                    WHERE DATE(created_at) = ?::date
                      AND order_status NOT IN ('CANCELLED')), 0)            AS revenue_today,
                COALESCE(SUM(grand_total) FILTER (
                    WHERE DATE(created_at) >= ?::date
                      AND order_status NOT IN ('CANCELLED')), 0)            AS revenue_mtd,
                COALESCE(SUM(grand_total) FILTER (
                    WHERE order_status NOT IN ('CANCELLED')), 0)            AS revenue_total
            FROM ec_orders WHERE 1=1
            """ + f;
        List<Map<String, Object>> orderRows = jdbc.queryForList(orderSql, today, mtdStart, today, mtdStart);
        Map<String, Object> ok = orderRows.isEmpty() ? Map.of() : orderRows.get(0);

        // Customers
        String custSql = """
            SELECT
                COUNT(*) FILTER (WHERE account_status = 'ACTIVE')  AS active_customers,
                COUNT(*) FILTER (WHERE DATE(created_at) >= ?::date) AS new_customers_mtd,
                COUNT(*)                                            AS total_customers
            FROM ec_customers WHERE 1=1
            """ + (orgId != null ? " AND organization_id = " + orgId : "");
        List<Map<String, Object>> custRows = jdbc.queryForList(custSql, mtdStart);
        Map<String, Object> ck = custRows.isEmpty() ? Map.of() : custRows.get(0);

        // Products
        String prodSql = """
            SELECT
                COUNT(*) FILTER (WHERE published = true AND active = true)  AS published_products,
                COUNT(*) FILTER (WHERE active = true)                       AS active_products,
                COUNT(*)                                                    AS total_products
            FROM ec_product_catalog WHERE 1=1
            """ + (orgId != null ? " AND organization_id = " + orgId : "");
        List<Map<String, Object>> prodRows = jdbc.queryForList(prodSql);
        Map<String, Object> pk = prodRows.isEmpty() ? Map.of() : prodRows.get(0);

        // Average order value (MTD, non-cancelled)
        String avgSql = """
            SELECT COALESCE(AVG(grand_total), 0) AS avg_order_value
            FROM ec_orders
            WHERE DATE(created_at) >= ?::date
              AND order_status NOT IN ('CANCELLED')
            """ + (orgId != null ? " AND organization_id = " + orgId : "");
        BigDecimal avgOrder = jdbc.queryForObject(avgSql, BigDecimal.class, mtdStart);

        // Reviews pending moderation
        String revSql = """
            SELECT COUNT(*) AS pending_reviews
            FROM ec_reviews
            WHERE review_status = 'PENDING'
            """ + (orgId != null ? " AND organization_id = " + orgId : "");
        Long pendingReviews = jdbc.queryForObject(revSql, Long.class);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("ordersToday",       toLong(ok, "orders_today"));
        kpi.put("ordersMtd",         toLong(ok, "orders_mtd"));
        kpi.put("ordersPending",      toLong(ok, "orders_pending"));
        kpi.put("ordersProcessing",   toLong(ok, "orders_processing"));
        kpi.put("ordersCompleted",    toLong(ok, "orders_completed"));
        kpi.put("ordersCancelled",    toLong(ok, "orders_cancelled"));
        kpi.put("revenueToday",       toBD(ok.get("revenue_today")));
        kpi.put("revenueMtd",         toBD(ok.get("revenue_mtd")));
        kpi.put("revenueTotal",       toBD(ok.get("revenue_total")));
        kpi.put("avgOrderValue",      avgOrder != null ? avgOrder : BigDecimal.ZERO);
        kpi.put("activeCustomers",    toLong(ck, "active_customers"));
        kpi.put("newCustomersMtd",    toLong(ck, "new_customers_mtd"));
        kpi.put("totalCustomers",     toLong(ck, "total_customers"));
        kpi.put("publishedProducts",  toLong(pk, "published_products"));
        kpi.put("activeProducts",     toLong(pk, "active_products"));
        kpi.put("totalProducts",      toLong(pk, "total_products"));
        kpi.put("pendingReviews",     pendingReviews != null ? pendingReviews : 0L);
        res.put("kpi", kpi);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. ORDER STATUS BREAKDOWN
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadOrderStatus(Map<String, Object> res, Long orgId) {
        String sql = """
            SELECT order_status, COUNT(*) AS cnt
            FROM ec_orders
            """ + (orgId != null ? " WHERE organization_id = " + orgId +" " : "") + """
            GROUP BY order_status
            ORDER BY cnt DESC
            """;
        res.put("orderStatus", jdbc.queryForList(sql));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. 12-MONTH REVENUE TREND (line chart)
    // Uses generate_series to fill gaps — every month appears even with no data.
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadRevenueChart(Map<String, Object> res, Long orgId) {
        String sql = """
            WITH months AS (
              SELECT TO_CHAR(m, 'Mon YY')              AS month,
                     DATE_TRUNC('month', m)             AS month_start
              FROM generate_series(
                     DATE_TRUNC('month', CURRENT_DATE - INTERVAL '11 months'),
                     DATE_TRUNC('month', CURRENT_DATE),
                     INTERVAL '1 month') AS m
            ),
            order_agg AS (
              SELECT DATE_TRUNC('month', created_at)                           AS order_month,
                     COUNT(*) FILTER (WHERE order_status NOT IN ('CANCELLED')) AS order_count,
                     COALESCE(SUM(grand_total) FILTER (
                       WHERE order_status NOT IN ('CANCELLED')), 0)            AS revenue
              FROM ec_orders
              WHERE 1=1
            """ + (orgId != null ? "  AND organization_id = " + orgId : "") + """
              GROUP BY 1
            )
            SELECT m.month,
                   COALESCE(a.order_count, 0)  AS order_count,
                   COALESCE(a.revenue,     0)  AS revenue
            FROM months m
            LEFT JOIN order_agg a ON a.order_month = m.month_start
            ORDER BY m.month_start
            """;
        res.put("revenueChart", jdbc.queryForList(sql));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TOP 10 PRODUCTS BY MTD REVENUE (bar chart)
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopProducts(Map<String, Object> res, Long orgId, String mtdStart) {
        String sql = """
            SELECT p.product_title,
                   COUNT(DISTINCT oi.order_id) AS order_count,
                   SUM(oi.quantity)             AS units_sold,
                   COALESCE(SUM(oi.line_total), 0) AS revenue
            FROM ec_order_items oi
            JOIN ec_product_catalog p ON p.id = oi.product_id
            JOIN ec_orders o ON o.id = oi.order_id
            WHERE o.order_status NOT IN ('CANCELLED')
              AND DATE(o.created_at) >= ?::date
            """ + (orgId != null ? "  AND o.organization_id = " + orgId + " " : "") + """
            GROUP BY p.id, p.product_title
            ORDER BY revenue DESC
            LIMIT 10
            """;
        res.put("topProducts", jdbc.queryForList(sql, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. TOP 10 CUSTOMERS BY ALL-TIME SPEND
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadTopCustomers(Map<String, Object> res, Long orgId) {
        String sql = """
            SELECT COALESCE(c.full_name, 'Guest') AS customer_name,
                   COALESCE(c.phone, '—')          AS phone,
                   COUNT(o.id)                     AS total_orders,
                   COALESCE(SUM(o.grand_total), 0) AS total_spent
            FROM ec_orders o
            LEFT JOIN ec_customers c ON c.id = o.customer_id
            WHERE o.order_status NOT IN ('CANCELLED')
            """ + (orgId != null ? "  AND o.organization_id = " + orgId : "") + """
            GROUP BY c.id, c.full_name, c.phone
            ORDER BY total_spent DESC
            LIMIT 10
            """;
        res.put("topCustomers", jdbc.queryForList(sql));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. REVENUE BY STOREFRONT CATEGORY (MTD, doughnut)
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadCategoryRevenue(Map<String, Object> res, Long orgId, String mtdStart) {
        String sql = """
            SELECT COALESCE(cat.category_name, 'Uncategorised') AS category_name,
                   COALESCE(SUM(oi.line_total), 0)              AS revenue
            FROM ec_order_items oi
            JOIN ec_product_catalog p   ON p.id   = oi.product_id
            LEFT JOIN ec_categories cat ON cat.id = p.category_id
            JOIN ec_orders o ON o.id = oi.order_id
            WHERE o.order_status NOT IN ('CANCELLED')
              AND DATE(o.created_at) >= ?::date
            """ + (orgId != null ? "  AND o.organization_id = " + orgId : "") + """
            GROUP BY cat.id, cat.category_name
            ORDER BY revenue DESC
            LIMIT 8
            """;
        res.put("categoryRevenue", jdbc.queryForList(sql, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. PAYMENT METHOD BREAKDOWN (MTD, doughnut)
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadPaymentMethod(Map<String, Object> res, Long orgId, String mtdStart) {
        String sql = """
            SELECT COALESCE(pay.payment_method, 'COD / Unknown') AS payment_method,
                   COUNT(*)                                       AS txn_count,
                   COALESCE(SUM(pay.paid_amount), 0)             AS total_paid
            FROM ec_payments pay
            JOIN ec_orders o ON o.id = pay.order_id
            WHERE DATE(pay.created_at) >= ?::date
              AND pay.payment_status = 'SUCCESS'
            """ + (orgId != null ? "  AND pay.organization_id = " + orgId : "") + """
            GROUP BY pay.payment_method
            ORDER BY total_paid DESC
            """;
        res.put("paymentMethod", jdbc.queryForList(sql, mtdStart));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. EXCEPTIONS / ALERTS (action-required items)
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadExceptions(Map<String, Object> res, Long orgId, String today) {
        String f = orgId != null ? " AND organization_id = " + orgId : "";

        // Orders needing attention
        String orderExSql = """
            SELECT
                COUNT(*) FILTER (WHERE order_status = 'PENDING'
                                   AND DATE(created_at) < ?::date - INTERVAL '1 day')
                                                                AS stale_pending,
                COUNT(*) FILTER (WHERE payment_status = 'PENDING'
                                   AND order_status NOT IN ('CANCELLED'))
                                                                AS unpaid_orders,
                COUNT(*) FILTER (WHERE shipping_status = 'PENDING'
                                   AND order_status = 'CONFIRMED')
                                                                AS unshipped_confirmed
            FROM ec_orders WHERE 1=1
            """ + f;
        List<Map<String, Object>> oeRows = jdbc.queryForList(orderExSql, today);
        Map<String, Object> oe = oeRows.isEmpty() ? Map.of() : oeRows.get(0);

        // Return requests
        String retSql = """
            SELECT COUNT(*) AS pending_returns
            FROM ec_returns WHERE return_status = 'REQUESTED'
            """ + f;
        Long pendingReturns = jdbc.queryForObject(retSql, Long.class);

        // Reviews awaiting moderation
        String revSql = """
            SELECT COUNT(*) AS pending_reviews
            FROM ec_reviews WHERE review_status = 'PENDING'
            """ + f;
        Long pendingReviews = jdbc.queryForObject(revSql, Long.class);

        // Abandoned carts (active for > 2 hours)
        String cartSql = """
            SELECT COUNT(*) AS abandoned_carts
            FROM ec_cart
            WHERE cart_status = 'ACTIVE'
              AND created_at < NOW() - INTERVAL '2 hours'
            """ + f;
        Long abandonedCarts = jdbc.queryForObject(cartSql, Long.class);

        // Coupons expiring in next 3 days
        String couponSql = """
            SELECT COUNT(*) AS expiring_soon
            FROM ec_coupon
            WHERE active = true
              AND valid_to IS NOT NULL
              AND valid_to BETWEEN NOW() AND NOW() + INTERVAL '3 days'
            """ + (orgId != null ? " AND organization_id = " + orgId : "");
        Long expiringSoonCoupons = jdbc.queryForObject(couponSql, Long.class);

        Map<String, Object> ex = new LinkedHashMap<>();
        ex.put("stalePendingOrders",   toLong(oe, "stale_pending"));
        ex.put("unpaidOrders",         toLong(oe, "unpaid_orders"));
        ex.put("unshippedConfirmed",   toLong(oe, "unshipped_confirmed"));
        ex.put("pendingReturns",       pendingReturns  != null ? pendingReturns  : 0L);
        ex.put("pendingReviews",       pendingReviews  != null ? pendingReviews  : 0L);
        ex.put("abandonedCarts",       abandonedCarts  != null ? abandonedCarts  : 0L);
        ex.put("expiringSoonCoupons",  expiringSoonCoupons != null ? expiringSoonCoupons : 0L);
        res.put("exceptions", ex);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. RECENT ORDERS (last 15)
    // ─────────────────────────────────────────────────────────────────────────
    private void _loadRecentOrders(Map<String, Object> res, Long orgId) {
        String sql = """
            SELECT o.id, o.order_no,
                   COALESCE(c.full_name, 'Guest')              AS customer_name,
                   COALESCE(c.phone, '—')                      AS phone,
                   TO_CHAR(o.grand_total, 'FM৳ 999,999,999.00') AS grand_total,
                   o.order_status, o.payment_status, o.shipping_status,
                   TO_CHAR(o.created_at, 'DD-Mon-YYYY HH24:MI') AS order_date
            FROM ec_orders o
            LEFT JOIN ec_customers c ON c.id = o.customer_id
            WHERE 1=1
            """ + (orgId != null ? "  AND o.organization_id = " + orgId : "") + """
            ORDER BY o.id DESC
            LIMIT 15
            """;
        res.put("recentOrders", jdbc.queryForList(sql));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private static Long toLong(Map<String, Object> row, String col) {
        Object v = row.get(col);
        return v == null ? 0L : Long.parseLong(v.toString());
    }
    private static BigDecimal toBD(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
