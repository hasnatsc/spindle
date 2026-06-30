package com.asg.spindleserp.ecommerce.productSupport.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.inventory.entity.Item;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * EcProductCatalog — storefront/marketing layer on top of inv_items (Item).
 *
 * Relationship: ONE EcProductCatalog per ONE Item (enforced by UNIQUE constraint).
 *
 * What this entity adds over Item:
 *   • Storefront slug + SEO meta
 *   • Marketing product title (may differ from Item.itemName)
 *   • Rich CMS content (youtube, return policy, shipping info)
 *   • Merchandising flags (featured, trending, new_arrival …)
 *   • Publishing workflow (published, publishDate)
 *   • Storefront category FK (separate from inv_item_categories)
 *   • Per-order quantity overrides (null = inherit from Item.minimumStock)
 *
 * What this entity does NOT duplicate from Item (read via item FK):
 *   • itemCode, itemName, sku, barcode    → item.itemCode / sku / barcode
 *   • costPrice, unitPrice, standardCost  → item.costPrice / unitPrice
 *   • weight, volume, dimensions          → item.weight / volume / dimensions
 *   • minimumStock, maximumStock          → item.minimumStock / maximumStock
 *   • brand, model                        → item.brand / item.model
 *   • itemType, hasLotTracking            → item.itemType / hasLotTracking
 *   • purchaseUnit, salesUnit             → item.salesUnit
 *
 * total_sales / total_views REMOVED — computed from ec_product_analytics.
 */
@Entity
@Table(name = "ec_product_catalog",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ec_prod_slug", columnNames = {"organization_id", "slug"}),
                @UniqueConstraint(name = "uq_ec_prod_item", columnNames = {"organization_id", "item_id"})
        },
        indexes = {
                @Index(name = "idx_ec_prod_org",      columnList = "organization_id"),
                @Index(name = "idx_ec_prod_item",     columnList = "item_id"),
                @Index(name = "idx_ec_prod_cat",      columnList = "category_id"),
                @Index(name = "idx_ec_prod_slug",     columnList = "slug"),
                @Index(name = "idx_ec_prod_featured", columnList = "featured"),
                @Index(name = "idx_ec_prod_best",     columnList = "best_seller"),
                @Index(name = "idx_ec_prod_trending", columnList = "trending"),
                @Index(name = "idx_ec_prod_pub",      columnList = "published,publish_date"),
                @Index(name = "idx_ec_prod_active",   columnList = "active,deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcProductCatalog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Core ERP link (1:1 with inv_items) ────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Storefront category (separate from inv_item_categories)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EcCategory category;

    // ── Storefront identity ────────────────────────────────────────────────
    @Column(nullable = false, length = 250)
    private String slug;

    @Column(nullable = false, length = 300)
    private String productTitle;   // marketing name — may differ from Item.itemName

    // ── Rich content ──────────────────────────────────────────────────────
    @Column(length = 1000)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    // ── SEO (storefront-only) ─────────────────────────────────────────────
    @Column(length = 255)
    private String seoTitle;

    @Column(length = 500)
    private String seoKeywords;

    @Column(length = 1000)
    private String seoDescription;

    // ── CMS content ───────────────────────────────────────────────────────
    @Column(length = 500)
    private String youtubeVideo;

    @Column(length = 500)
    private String warrantyInformation;

    @Column(columnDefinition = "text")
    private String returnPolicy;

    @Column(columnDefinition = "text")
    private String shippingInformation;

    // ── Order quantity overrides ───────────────────────────────────────────
    // NULL = inherit from Item.minimumStock / maximumStock
    @Column(precision = 12, scale = 2)
    private BigDecimal minimumOrderQty;

    @Column(precision = 12, scale = 2)
    private BigDecimal maximumOrderQty;

    // ── Merchandising flags ────────────────────────────────────────────────
    @Builder.Default
    @Column(nullable = false)
    private boolean featured = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean bestSeller = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean trending = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean newArrival = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean recommended = false;

    // ── Publishing ────────────────────────────────────────────────────────
    @Builder.Default
    @Column(nullable = false)
    private boolean published = false;

    private LocalDateTime publishDate;

    // ── Status ────────────────────────────────────────────────────────────
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    // ── Collections ───────────────────────────────────────────────────────
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcProductImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcProductVariant> variants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcProductAttributeValue> attributeValues = new ArrayList<>();
}
