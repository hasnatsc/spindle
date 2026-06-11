package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_products",
        uniqueConstraints = @UniqueConstraint(name = "uk_prod_store_slug", columnNames = {"store_id", "slug"}),
        indexes = {
                @Index(name = "idx_prod_store", columnList = "store_id"),
                @Index(name = "idx_prod_item", columnList = "inv_item_id"),
                @Index(name = "idx_prod_status", columnList = "status"),
                @Index(name = "idx_prod_featured", columnList = "store_id,is_featured")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoProduct extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inv_item_id", nullable = false)
    private InventoryItem invItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EcoCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_class_id")
    private EcoTaxClass taxClass;

    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;
    @Column(name = "product_name_bn", length = 300)
    private String productNameBn;
    @Column(nullable = false, length = 300)
    private String slug;
    @Column(name = "short_description", length = 1000)
    private String shortDescription;
    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;
    @Column(name = "product_type", nullable = false, length = 30)
    @Builder.Default
    private String productType = "SIMPLE"; // SIMPLE|VARIABLE|BUNDLE|DIGITAL

    @Column(name = "base_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal basePrice;
    @Column(name = "sale_price", precision = 18, scale = 4)
    private BigDecimal salePrice;
    @Column(name = "sale_starts_at")
    private LocalDateTime saleStartsAt;
    @Column(name = "sale_ends_at")
    private LocalDateTime saleEndsAt;
    @Column(name = "cost_price", precision = 18, scale = 4)
    private BigDecimal costPrice;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;
    @Builder.Default
    @Column(name = "is_new_arrival", nullable = false)
    private Boolean isNewArrival = false;
    @Builder.Default
    @Column(name = "is_best_seller", nullable = false)
    private Boolean isBestSeller = false;
    @Builder.Default
    @Column(name = "track_inventory", nullable = false)
    private Boolean trackInventory = true;
    @Builder.Default
    @Column(name = "allow_backorder", nullable = false)
    private Boolean allowBackorder = false;
    @Builder.Default
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;
    @Builder.Default
    @Column(name = "min_order_qty", precision = 12, scale = 3)
    private BigDecimal minOrderQty = BigDecimal.ONE;
    @Builder.Default
    @Column(name = "sold_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal soldQty = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT|ACTIVE|ARCHIVED|OUT_OF_STOCK|COMING_SOON

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Builder.Default
    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EcoProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EcoProductImage> images = new ArrayList<>();
}
