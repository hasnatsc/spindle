package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.inventory.entity.Item;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// =============================================================================
// EC PRODUCT IMAGE
// =============================================================================

@Entity
@Table(name = "ec_product_images",
        indexes = @Index(name = "idx_ec_prodimg_prod", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EcProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Column(nullable = false, length = 700)
    private String imageUrl;

    @Column(length = 700)
    private String thumbnailUrl;

    @Column(length = 255)
    private String altText;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean isPrimary = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private String createdBy;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

// =============================================================================
// EC PRODUCT VARIANT
// Each variant maps to a DISTINCT inv_items row (mandatory item_id).
// Sourced from inv_items via item_id:  sku, barcode, costPrice
// Kept as nullable physical overrides: weight/length/width/height
//   (null = use Item.weight / volume / dimensions for shipping calculator)
// Kept as storefront pricing layer: sellingPrice, comparePrice
// =============================================================================

@Entity
@Table(name = "ec_product_variants",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_variant_item",
                columnNames = {"product_id", "item_id"}),
        indexes = {
                @Index(name = "idx_ec_variant_prod",   columnList = "product_id"),
                @Index(name = "idx_ec_variant_item",   columnList = "item_id"),
                @Index(name = "idx_ec_variant_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EcProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    // MANDATORY: each variant IS a distinct inv_items row
    // item.sku, item.barcode, item.costPrice all readable from here
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(length = 50)
    private String variantCode;

    @Column(length = 200)
    private String variantName;

    // ── Visual differentiators (storefront-only) ──────────────────────────
    @Column(length = 100)
    private String color;

    @Column(length = 100)
    private String sizeName;

    // ── NULLABLE physical overrides for shipping rate calculator ──────────
    // NULL = use Item.weight / Item.volume / Item.dimensions
    @Column(precision = 12, scale = 3)
    private BigDecimal weightOverride;

    @Column(precision = 12, scale = 3)
    private BigDecimal lengthOverride;

    @Column(precision = 12, scale = 3)
    private BigDecimal widthOverride;

    @Column(precision = 12, scale = 3)
    private BigDecimal heightOverride;

    // ── Storefront pricing (may differ from Item.unitPrice) ───────────────
    @Column(precision = 18, scale = 2)
    private BigDecimal sellingPrice;    // customer-facing price

    @Column(precision = 18, scale = 2)
    private BigDecimal comparePrice;    // strikethrough/was price

    // costPrice NOT stored here — always read from Item.costPrice via item FK

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

// =============================================================================
// EC PRODUCT ATTRIBUTE VALUE
// Instance values of EcCategoryAttribute for a specific product
// =============================================================================

@Entity
@Table(name = "ec_product_attribute_values",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_prodattr",
                columnNames = {"product_id", "category_attribute_id"}),
        indexes = @Index(name = "idx_ec_prodattr_prod", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EcProductAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_attribute_id", nullable = false)
    private EcCategoryAttribute categoryAttribute;

    @Column(columnDefinition = "text")
    private String attributeValue;
}

// =============================================================================
// EC PRODUCT TAG
// =============================================================================

@Entity
@Table(name = "ec_product_tags",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_tag",
                columnNames = {"organization_id", "slug"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EcProductTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 100)
    private String tagName;

    @Column(nullable = false, length = 120)
    private String slug;
}

// =============================================================================
// EC PRODUCT TAG MAP (join table)
// =============================================================================

@Entity
@Table(name = "ec_product_tag_map",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_prod_tag",
                columnNames = {"product_id", "tag_id"}),
        indexes = @Index(name = "idx_ec_prodtag_prod", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EcProductTagMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private EcProductTag tag;
}
