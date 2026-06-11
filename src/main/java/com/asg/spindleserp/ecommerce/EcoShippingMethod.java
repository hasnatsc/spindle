package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.dummy.EcoShippingZone;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eco_shipping_methods",
        indexes = @Index(name = "idx_sm_store", columnList = "store_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoShippingMethod extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private EcoShippingZone zone;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 100)
    private String carrier; // Pathao|Steadfast|Sundarban|RedX|In-House
    @Column(name = "shipping_type", nullable = false, length = 30)
    @Builder.Default
    private String shippingType = "FLAT_RATE"; // FLAT_RATE|FREE|WEIGHT_BASED|PRICE_BASED|PER_ITEM
    @Builder.Default
    @Column(name = "base_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseCost = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "per_kg_cost", precision = 18, scale = 2)
    private BigDecimal perKgCost = BigDecimal.ZERO;
    @Column(name = "free_shipping_above", precision = 18, scale = 2)
    private BigDecimal freeShippingAbove;
    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;
    @Column(name = "max_weight_kg", precision = 8, scale = 2)
    private BigDecimal maxWeightKg;
    @Column(name = "estimated_days_min")
    private Integer estimatedDaysMin;
    @Column(name = "estimated_days_max")
    private Integer estimatedDaysMax;
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
}
