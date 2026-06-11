package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eco_product_variants",
        uniqueConstraints = @UniqueConstraint(name = "uk_var_prod_sku", columnNames = {"product_id", "sku"}),
        indexes = {
                @Index(name = "idx_var_product", columnList = "product_id"),
                @Index(name = "idx_var_item", columnList = "inv_item_id"),
                @Index(name = "idx_var_sku", columnList = "sku")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoProductVariant extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcoProduct product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inv_item_id")
    private InventoryItem invItem;  // NULL = inherits from product's inv_item_id
    @Column(nullable = false, length = 100)
    private String sku;
    @Column(length = 100)
    private String barcode;
    // NULL values inherit from product
    @Column(precision = 18, scale = 4)
    private BigDecimal price;
    @Column(name = "sale_price", precision = 18, scale = 4)
    private BigDecimal salePrice;
    @Column(name = "cost_price", precision = 18, scale = 4)
    private BigDecimal costPrice;
    // Shipping
    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;
    @Column(name = "length_cm", precision = 8, scale = 2)
    private BigDecimal lengthCm;
    @Column(name = "width_cm", precision = 8, scale = 2)
    private BigDecimal widthCm;
    @Column(name = "height_cm", precision = 8, scale = 2)
    private BigDecimal heightCm;
    @Builder.Default
    @Column(name = "track_inventory", nullable = false)
    private Boolean trackInventory = true;
    @Builder.Default
    @Column(name = "allow_backorder", nullable = false)
    private Boolean allowBackorder = false;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    @ManyToMany
    @JoinTable(name = "eco_variant_attribute_values",
            joinColumns = @JoinColumn(name = "variant_id"),
            inverseJoinColumns = @JoinColumn(name = "attribute_value_id"))
    @Builder.Default
    private List<EcoAttributeValue> attributeValues = new ArrayList<>();
}
