// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontContentService.java
package com.asg.spindleserp.ecommerce.storefront.service;

import com.asg.spindleserp.ecommerce.storefront.dto.SfBannerDTO;
import com.asg.spindleserp.ecommerce.storefront.dto.SfHomeSectionDTO;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * StorefrontContentService — CMS-ish storefront content via JDBC:
 * home slider banners (ec_banners), dynamic home sections (ec_home_sections),
 * static pages (ec_pages), newsletter (ec_newsletter), recently viewed tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontContentService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final JdbcTemplate jdbcTemplate;
    private final StorefrontProductService productService;

    // ── HOME SLIDER BANNERS ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SfBannerDTO> homeSliderBanners() {
        Long orgId = ContextProvider.getOrganizationId();
        return jdbcTemplate.query("""
                SELECT id, title, sub_title, description, image_url, mobile_image_url,
                       button_text, button_url, open_in_new_tab
                FROM ec_banners
                WHERE organization_id = ? AND active = true AND banner_type = 'HOME_SLIDER'
                  AND (start_date IS NULL OR start_date <= now())
                  AND (end_date   IS NULL OR end_date   >= now())
                ORDER BY display_order NULLS LAST, id
                """,
                (rs, i) -> SfBannerDTO.builder()
                        .id(rs.getLong("id"))
                        .title(rs.getString("title"))
                        .subTitle(rs.getString("sub_title"))
                        .description(rs.getString("description"))
                        .imageUrl(rs.getString("image_url"))
                        .mobileImageUrl(rs.getString("mobile_image_url"))
                        .buttonText(rs.getString("button_text"))
                        .buttonUrl(rs.getString("button_url"))
                        .openInNewTab(rs.getBoolean("open_in_new_tab"))
                        .build(),
                orgId);
    }

    // ── DYNAMIC HOME SECTIONS ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SfHomeSectionDTO> homeSections() {
        Long orgId = ContextProvider.getOrganizationId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, section_code, section_name, section_title, section_subtitle, max_products
                FROM ec_home_sections
                WHERE organization_id = ? AND active = true
                ORDER BY display_order NULLS LAST, id
                """, orgId);

        List<SfHomeSectionDTO> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Long id = ((Number) r.get("id")).longValue();
            int max = r.get("max_products") != null ? ((Number) r.get("max_products")).intValue() : 8;
            var products = productService.cardsForSection(id, Math.min(Math.max(max, 1), 24));
            if (products.isEmpty()) continue;   // don't render empty shelves
            out.add(SfHomeSectionDTO.builder()
                    .id(id)
                    .sectionCode((String) r.get("section_code"))
                    .sectionName((String) r.get("section_name"))
                    .sectionTitle((String) r.get("section_title"))
                    .sectionSubtitle((String) r.get("section_subtitle"))
                    .products(products)
                    .build());
        }
        return out;
    }

    // ── STATIC PAGES (ec_pages) ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> pageBySlug(String slug) {
        Long orgId = ContextProvider.getOrganizationId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT page_title, page_content, seo_title, seo_description
                FROM ec_pages
                WHERE organization_id = ? AND slug = ? AND published = true
                """, orgId, slug);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    // ── NEWSLETTER ───────────────────────────────────────────────────────────
    @Transactional
    public String subscribeNewsletter(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches())
            throw new IllegalArgumentException("Please enter a valid email address.");
        Long orgId = ContextProvider.getOrganizationId();
        jdbcTemplate.update("""
                INSERT INTO ec_newsletter
                    (organization_id, email, subscribed_at, subscription_status, verified, created_at)
                VALUES (?, ?, now(), 'SUBSCRIBED', false, now())
                ON CONFLICT (organization_id, email)
                DO UPDATE SET subscription_status = 'SUBSCRIBED', unsubscribed_at = NULL,
                              subscribed_at = now(), updated_at = now()
                """, orgId, email.trim().toLowerCase());
        return "You're in the loop — thanks for subscribing!";
    }

    // ── RECENTLY VIEWED ──────────────────────────────────────────────────────
    @Transactional
    public void recordRecentlyViewed(Long customerId, Long productId, String ip) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO ec_recently_viewed (customer_id, product_id, ip_address, viewed_at) VALUES (?, ?, ?, now())",
                    customerId, productId, ip);
        } catch (Exception e) {
            log.warn("recordRecentlyViewed failed: {}", e.getMessage());   // never break the PDP over analytics
        }
    }
}
