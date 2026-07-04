// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontProductService.java
package com.asg.spindleserp.ecommerce.storefront.service;

import com.asg.spindleserp.common.util.CommonUtils;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcCategory;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductImage;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcCategoryRepository;
import com.asg.spindleserp.ecommerce.productSupport.repository.EcProductCatalogRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfProductCardDTO;
import com.asg.spindleserp.ecommerce.storefront.dto.SfProductDetailDTO;
import com.asg.spindleserp.security.auth.ContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * StorefrontProductService — v2.
 * Changes vs v1:
 *  - browse(): variant LEFT JOIN removed (duplicated rows); compare_price now a MIN() subselect;
 *    rating/count read from ec_review_summary instead of per-row AVG subqueries.
 *  - Shared CARD column list + mapCardRow() reused by browse / section / wishlist / recently-viewed.
 *  - findBySlug(): real avgRating/reviewCount from ec_review_summary; attribute label fixed to
 *    come from the category attribute definition (★ getter names — see note below).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontProductService {

    private final EcProductCatalogRepository productRepository;
    private final EcCategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Shared card projection — every card query selects exactly these columns. */
    private static final String CARD_COLS = """
                p.id, p.product_title, p.slug,
                (SELECT img.image_url FROM ec_product_images img
                  WHERE img.product_id = p.id
                  ORDER BY img.is_primary DESC, img.display_order NULLS LAST, img.id LIMIT 1) AS primary_image,
                i.unit_price AS selling_price,
                (SELECT MIN(v.compare_price) FROM ec_product_variants v
                  WHERE v.product_id = p.id AND v.active = true AND v.compare_price IS NOT NULL) AS compare_price,
                c.category_name, c.slug AS category_slug,
                p.featured, p.best_seller, p.new_arrival,
                COALESCE(rs.average_rating, 0)   AS avg_rating,
                COALESCE(rs.total_reviews, 0) AS review_count
            """;

    private static final String CARD_FROM = """
            FROM ec_product_catalog p
            JOIN inv_items i ON i.id = p.item_id
            LEFT JOIN ec_categories c ON c.id = p.category_id
            LEFT JOIN ec_review_summary rs ON rs.product_id = p.id
            """;

    private static final String PUBLISHED_FILTER =
            "p.published = true AND p.active = true AND p.deleted = false";

    // ── GRID / LISTING (JDBC for performance — storefront traffic is read-heavy) ─
    @Transactional(readOnly = true)
    public Map<String, Object> browse(Long categoryId, String search, String sort, int page, int pageSize) {
        Long orgId = ContextProvider.getOrganizationId();
        int offset = Math.max(0, page - 1) * pageSize;

        StringBuilder where = new StringBuilder(
            "WHERE p.organization_id = " + orgId + " AND " + PUBLISHED_FILTER
        );
        if (categoryId != null) where.append(" AND p.category_id = ").append(categoryId);
        if (search != null && !search.isBlank())
            where.append(CommonUtils.searchILike(search, List.of("p.product_title", "i.item_code", "i.sku")));

        String orderBy = switch (sort == null ? "" : sort) {
            case "price_asc"  -> "i.unit_price ASC";
            case "price_desc" -> "i.unit_price DESC";
            case "newest"     -> "p.created_at DESC";
            case "best_seller"-> "p.best_seller DESC, p.created_at DESC";
            default           -> "p.featured DESC, p.created_at DESC";
        };

        String sql = String.format("""
            SELECT COUNT(*) OVER () AS full_count,
            %s
            %s
            %s
            ORDER BY %s
            OFFSET %d LIMIT %d
            """, CARD_COLS, CARD_FROM, where, orderBy, offset, pageSize);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long total = rows.isEmpty() ? 0L : CommonUtils.toLong(rows.getFirst().get("full_count"));

        List<SfProductCardDTO> cards = rows.stream().map(this::mapCardRow).toList();

        return Map.of("items", cards, "total", total, "page", page, "pageSize", pageSize,
                "totalPages", (long) Math.ceil(total / (double) pageSize));
    }

    @Transactional(readOnly = true)
    public List<SfProductCardDTO> featured(int limit) {
        return (List<SfProductCardDTO>) browse(null, null, "featured", 1, limit).get("items");
    }

    @Transactional(readOnly = true)
    public List<SfProductCardDTO> newArrivals(int limit) {
        return (List<SfProductCardDTO>) browse(null, null, "newest", 1, limit).get("items");
    }

    // ── DYNAMIC HOME SECTION PRODUCTS (ec_home_section_products) ─────────────
    @Transactional(readOnly = true)
    public List<SfProductCardDTO> cardsForSection(Long sectionId, int limit) {
        Long orgId = ContextProvider.getOrganizationId();
        String sql = String.format("""
            SELECT %s
            %s
            JOIN ec_home_section_products hsp ON hsp.product_id = p.id
            WHERE p.organization_id = ? AND %s AND hsp.section_id = ?
            ORDER BY hsp.display_order NULLS LAST, hsp.id
            LIMIT %d
            """, CARD_COLS, CARD_FROM, PUBLISHED_FILTER, limit);
        return jdbcTemplate.queryForList(sql, orgId, sectionId).stream().map(this::mapCardRow).toList();
    }

    // ── WISHLIST CARDS ───────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SfProductCardDTO> cardsForWishlist(Long customerId) {
        Long orgId = ContextProvider.getOrganizationId();
        String sql = String.format("""
            SELECT %s
            %s
            JOIN ec_wishlist w ON w.product_id = p.id
            WHERE p.organization_id = ? AND %s AND w.customer_id = ?
            ORDER BY w.id DESC
            """, CARD_COLS, CARD_FROM, PUBLISHED_FILTER);
        return jdbcTemplate.queryForList(sql, orgId, customerId).stream().map(this::mapCardRow).toList();
    }

    // ── RECENTLY VIEWED CARDS ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SfProductCardDTO> cardsRecentlyViewed(Long customerId, int limit, Long excludeProductId) {
        Long orgId = ContextProvider.getOrganizationId();
        String sql = String.format("""
            SELECT %s
            %s
            JOIN (SELECT product_id, MAX(viewed_at) AS last_seen
                    FROM ec_recently_viewed WHERE customer_id = ?
                    GROUP BY product_id) rv ON rv.product_id = p.id
            WHERE p.organization_id = ? AND %s AND p.id <> COALESCE(?, -1)
            ORDER BY rv.last_seen DESC
            LIMIT %d
            """, CARD_COLS, CARD_FROM, PUBLISHED_FILTER, limit);
        return jdbcTemplate.queryForList(sql, customerId, orgId, excludeProductId)
                .stream().map(this::mapCardRow).toList();
    }

    private SfProductCardDTO mapCardRow(Map<String, Object> r) {
        BigDecimal sell = toBD(r.get("selling_price"));
        BigDecimal cmp  = toBD(r.get("compare_price"));
        Integer disc = (cmp != null && sell != null && cmp.compareTo(BigDecimal.ZERO) > 0 && cmp.compareTo(sell) > 0)
                ? cmp.subtract(sell).multiply(BigDecimal.valueOf(100)).divide(cmp, 0, RoundingMode.HALF_UP).intValue()
                : null;
        return SfProductCardDTO.builder()
                .id(CommonUtils.toLong(r.get("id")))
                .productTitle((String) r.get("product_title"))
                .slug((String) r.get("slug"))
                .primaryImageUrl((String) r.get("primary_image"))
                .sellingPrice(sell)
                .comparePrice(cmp)
                .discountPercent(disc)
                .categoryName((String) r.get("category_name"))
                .categorySlug((String) r.get("category_slug"))
                .inStock(true)
                .featured(Boolean.TRUE.equals(r.get("featured")))
                .newArrival(Boolean.TRUE.equals(r.get("new_arrival")))
                .bestSeller(Boolean.TRUE.equals(r.get("best_seller")))
                .avgRating(r.get("avg_rating") != null ? ((Number) r.get("avg_rating")).doubleValue() : 0.0)
                .reviewCount(r.get("review_count") != null ? ((Number) r.get("review_count")).intValue() : 0)
                .build();
    }

    // ── DETAIL PAGE ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public SfProductDetailDTO findBySlug(String slug) {
        Long orgId = ContextProvider.getOrganizationId();
        EcProductCatalog p = productRepository.findByOrganizationIdAndSlug(orgId, slug)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        if (!p.isPublished() || !p.isActive() || p.isDeleted())
            throw new IllegalArgumentException("This product is no longer available.");

        List<String> images = p.getImages().stream()
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder() != null ? a.getDisplayOrder() : 0,
                        b.getDisplayOrder() != null ? b.getDisplayOrder() : 0))
                .map(EcProductImage::getImageUrl).toList();

        List<SfProductDetailDTO.VariantDTO> variants = p.getVariants().stream().map(v ->
            SfProductDetailDTO.VariantDTO.builder()
                .id(v.getId())
                .variantName(v.getVariantName())
                .colorCode(v.getColor())
                .sizeLabel(v.getSizeName())
                .sellingPrice(v.getSellingPrice() != null ? v.getSellingPrice() : p.getItem().getUnitPrice())
                .inStock(true)
                .build()
        ).toList();

        // ★ Attribute label now comes from the attribute definition, not the value.
        //   Adjust getCategoryAttribute()/getAttributeLabel() if your entity names differ.
        List<SfProductDetailDTO.AttrDTO> attrs = p.getAttributeValues().stream().map(av ->
            SfProductDetailDTO.AttrDTO.builder()
                .attributeLabel(av.getCategoryAttribute() != null
                        ? av.getCategoryAttribute().getAttributeLabel() : null)
                .value(av.getAttributeValue())
                .build()
        ).toList();

        BigDecimal sellPrice = p.getItem().getUnitPrice() != null ? p.getItem().getUnitPrice() : BigDecimal.ZERO;

        List<SfProductCardDTO> related = p.getCategory() != null
                ? (List<SfProductCardDTO>) browse(p.getCategory().getId(), null, "featured", 1, 5).get("items")
                : List.of();

        // Real review stats from summary table
        double avgRating = 0.0; int reviewCount = 0;
        List<Map<String, Object>> rs = jdbcTemplate.queryForList("SELECT average_rating AS avg_rating, total_reviews FROM ec_review_summary WHERE product_id = ?", p.getId());
        if (!rs.isEmpty()) {
            Object ar = rs.getFirst().get("avg_rating");
            Object tr = rs.getFirst().get("total_reviews");
            avgRating   = ar != null ? ((Number) ar).doubleValue() : 0.0;
            reviewCount = tr != null ? ((Number) tr).intValue() : 0;
        }

        return SfProductDetailDTO.builder()
                .id(p.getId())
                .productTitle(p.getProductTitle())
                .slug(p.getSlug())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .itemCode(p.getItem().getItemCode())
                .sku(p.getItem().getSku())
                .brandName(p.getItem().getBrand() != null ? p.getItem().getBrand().getBrandName() : null)
                .sellingPrice(sellPrice)
                .salesUnitCode(p.getItem().getSalesUnitCode())
                .availableStock(null)
                .inStock(true)
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)
                .metaTitle(p.getProductTitle())
                .metaDescription(p.getDescription())
                .avgRating(avgRating)
                .reviewCount(reviewCount)
                .imageUrls(images)
                .variants(variants)
                .attributes(attrs)
                .relatedProducts(related.stream().filter(r -> !r.getId().equals(p.getId())).limit(4).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<EcCategory> activeCategories() {
        Long orgId = ContextProvider.getOrganizationId();
        return categoryRepository.findActiveByOrg(orgId).stream()
                .filter(c -> c.getParentCategory() == null)
                .toList();
    }

    private BigDecimal toBD(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }
}
