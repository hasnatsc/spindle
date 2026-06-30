package com.asg.spindleserp.ecommerce.productSupport.entity;

import com.asg.spindleserp.inventory.entity.Item;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// =============================================================================
// EC PRODUCT IMAGE
// =============================================================================

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
public class EcProductVariant {

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

