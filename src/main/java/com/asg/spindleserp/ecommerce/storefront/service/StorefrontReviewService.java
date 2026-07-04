// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontReviewService.java
package com.asg.spindleserp.ecommerce.storefront.service;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.storefront.dto.SfReviewDTO;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StorefrontReviewService — customer reviews (ec_reviews / ec_review_summary) via JDBC.
 * Storefront submissions land as PENDING; the eCommerce admin approves them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontReviewService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final JdbcTemplate jdbcTemplate;

    // ── APPROVED REVIEWS FOR PDP ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SfReviewDTO> approved(Long productId, int limit) {
        return jdbcTemplate.query("""
                SELECT r.id, r.rating, r.review_title, r.review_text, r.verified_purchase, r.created_at,
                       COALESCE(c.full_name, c.first_name, 'Customer') AS customer_name
                FROM ec_reviews r
                JOIN ec_customers c ON c.id = r.customer_id
                WHERE r.product_id = ? AND r.review_status = 'APPROVED' AND r.active = true
                ORDER BY r.created_at DESC NULLS LAST, r.id DESC
                LIMIT ?
                """,
                (rs, i) -> SfReviewDTO.builder()
                        .id(rs.getLong("id"))
                        .rating(rs.getInt("rating"))
                        .reviewTitle(rs.getString("review_title"))
                        .reviewText(rs.getString("review_text"))
                        .verifiedPurchase(rs.getBoolean("verified_purchase"))
                        .customerName(maskName(rs.getString("customer_name")))
                        .createdAt(rs.getTimestamp("created_at") != null
                                ? rs.getTimestamp("created_at").toLocalDateTime().format(DF) : "")
                        .build(),
                productId, Math.max(1, limit));
    }

    // ── SUMMARY (avg, count, star distribution %) ────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long productId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT average_rating, total_reviews, rating1, rating2, rating3, rating4, rating5 " +
                "FROM ec_review_summary WHERE product_id = ?", productId);
        Map<String, Object> src;
        if (!rows.isEmpty() && toInt(rows.getFirst().get("total_reviews")) > 0) {
            src = rows.getFirst();
        } else {
            src = jdbcTemplate.queryForMap("""
                    SELECT COALESCE(AVG(rating), 0)              AS average_rating,
                           COUNT(*)                              AS total_reviews,
                           COUNT(*) FILTER (WHERE rating = 1)    AS rating1,
                           COUNT(*) FILTER (WHERE rating = 2)    AS rating2,
                           COUNT(*) FILTER (WHERE rating = 3)    AS rating3,
                           COUNT(*) FILTER (WHERE rating = 4)    AS rating4,
                           COUNT(*) FILTER (WHERE rating = 5)    AS rating5
                    FROM ec_reviews
                    WHERE product_id = ? AND review_status = 'APPROVED' AND active = true
                    """, productId);
        }
        int total = toInt(src.get("total_reviews"));
        double avg = src.get("average_rating") != null ? ((Number) src.get("average_rating")).doubleValue() : 0.0;

        Map<String, Object> out = new HashMap<>();
        out.put("avgRating", BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue());
        out.put("avgRatingRounded", (int) Math.round(avg));
        out.put("totalReviews", total);
        for (int s = 1; s <= 5; s++) {
            int cnt = toInt(src.get("rating" + s));
            out.put("count" + s, cnt);
            out.put("pct" + s, total > 0 ? Math.round(cnt * 100.0 / total) : 0);
        }
        return out;
    }

    // ── SUBMIT (lands as PENDING) ────────────────────────────────────────────
    @Transactional
    public String submit(EcCustomer customer, Long productId, Integer rating, String title, String text) {
        if (customer == null) throw new IllegalArgumentException("Please sign in to write a review.");
        if (rating == null || rating < 1 || rating > 5)
            throw new IllegalArgumentException("Please select a rating between 1 and 5 stars.");

        Integer already = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ec_reviews WHERE product_id = ? AND customer_id = ?",
                Integer.class, productId, customer.getId());
        if (already != null && already > 0)
            throw new IllegalArgumentException("You have already reviewed this product.");

        Integer purchased = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ec_order_items oi
                JOIN ec_orders o ON o.id = oi.order_id
                WHERE o.customer_id = ? AND oi.product_id = ?
                  AND o.order_status IN ('DELIVERED', 'COMPLETED')
                """, Integer.class, customer.getId(), productId);
        boolean verified = purchased != null && purchased > 0;

        jdbcTemplate.update("""
                INSERT INTO ec_reviews
                    (organization_id, customer_id, product_id, rating, review_title, review_text,
                     review_status, verified_purchase, active,
                     helpful_count, likes_count, dislikes_count, report_count,
                     created_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, true, 0, 0, 0, 0, now(), ?)
                """,
                ContextProvider.getOrganizationId(), customer.getId(), productId, rating,
                title != null ? title.trim() : null,
                text != null ? text.trim() : null,
                verified, customer.getFullName());

        log.info("Storefront review submitted: product #{} by customer #{}", productId, customer.getId());
        return "Thanks! Your review has been submitted and will appear once approved.";
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────
    private static int toInt(Object o) { return o == null ? 0 : ((Number) o).intValue(); }

    /** "Rahim Ahmed" → "Rahim A." — a small privacy courtesy on the public PDP. */
    private static String maskName(String name) {
        if (name == null || name.isBlank()) return "Customer";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
