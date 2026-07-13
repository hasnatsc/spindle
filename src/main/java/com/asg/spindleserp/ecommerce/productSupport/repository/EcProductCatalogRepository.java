// Path: com/asg/spindleserp/ecommerce/productSupport/repository/EcProductCatalogRepository.java
package com.asg.spindleserp.ecommerce.productSupport.repository;

import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * EcProductCatalogRepository.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * ONE METHOD ADDED — findStorefrontProduct(...)
 * ══════════════════════════════════════════════════════════════════════════
 * Every existing method here is correctly org-scoped
 * (findByOrganizationIdAndSlug, findByOrganizationIdAndActiveTrue…). There was
 * exactly one gap, and StorefrontCartService walked straight into it:
 *
 *     EcProductCatalog product = productRepository.findById(productId)
 *             .orElseThrow(() -> new IllegalArgumentException("Product not found."));
 *
 * findById() on a JpaRepository is a lookup by PRIMARY KEY. No org filter, no
 * published filter, no active filter, no deleted filter. POST /cart/add is
 * reachable by anyone — it is permitAll, guests are allowed to build a cart —
 * and the body is {"productId": N}. So any anonymous visitor could:
 *
 *   • add ANOTHER ORGANISATION'S product to their cart (cross-tenant read: the
 *     cart response echoes back product_title, slug, image URL and unit_price
 *     for whatever id you name — enumerate 1..N and you have dumped the entire
 *     multi-tenant catalogue, including every other tenant's pricing);
 *   • add an UNPUBLISHED product — a draft, an embargoed launch, a
 *     deliberately-hidden SKU — and then buy it;
 *   • add a SOFT-DELETED product;
 *   • add an INACTIVE product.
 *
 * The browse/listing queries all enforce
 * "p.published = true AND p.active = true AND p.deleted = false" plus the org
 * filter. The add-to-cart path enforced none of it. The listing was doing the
 * access control, and the listing is not an access control — it is a UI.
 *
 * findStorefrontProduct() is the same predicate the storefront's own
 * PUBLISHED_FILTER uses, expressed once, as a query, so the cart cannot diverge
 * from the catalogue again.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Repository
public interface EcProductCatalogRepository
        extends JpaRepository<EcProductCatalog, Long>,
                JpaSpecificationExecutor<EcProductCatalog> {

    boolean existsByOrganizationIdAndSlug(Long orgId, String slug);
    boolean existsByOrganizationIdAndSlugAndIdNot(Long orgId, String slug, Long id);
    boolean existsByOrganizationIdAndItemId(Long orgId, Long itemId);
    boolean existsByOrganizationIdAndItemIdAndIdNot(Long orgId, Long itemId, Long id);

    Optional<EcProductCatalog> findByOrganizationIdAndSlug(Long orgId, String slug);

    List<EcProductCatalog> findByOrganizationIdAndActiveTrue(Long orgId);
    List<EcProductCatalog> findByOrganizationIdAndActiveTrueAndPublishedTrue(Long orgId);
    List<EcProductCatalog> findByOrganizationIdAndActiveTrueAndFeaturedTrue(Long orgId);

    /**
     * ✅ NEW — the ONLY product lookup any storefront write path may use.
     *
     * Mirrors StorefrontProductService.PUBLISHED_FILTER exactly:
     *     p.published = true AND p.active = true AND p.deleted = false
     * …plus the organisation scope that findById() had no way to apply.
     *
     * If a product is not visible in the catalogue, it cannot be put in a cart.
     */
    Optional<EcProductCatalog> findByIdAndOrganizationIdAndPublishedTrueAndActiveTrueAndDeletedFalse(
            Long id, Long organizationId);
}
