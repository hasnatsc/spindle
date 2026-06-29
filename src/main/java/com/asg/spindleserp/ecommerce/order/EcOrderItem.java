package com.asg.spindleserp.ecommerce.order;

import com.asg.spindleserp.ecommerce.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.EcProductVariant;
import com.asg.spindleserp.global.entity.InventoryLot;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ec_order_items",
        indexes = {
                @Index(name = "idx_ec_orderitem_order", columnList = "order_id"),
                @Index(name = "idx_ec_orderitem_prod", columnList = "product_id"),
                @Index(name = "idx_ec_orderitem_item", columnList = "item_id"),
                @Index(name = "idx_ec_orderitem_lot", columnList = "inventory_lot_id"),
                @Index(name = "idx_ec_orderitem_wh", columnList = "warehouse_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private EcOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcProductVariant variant;

    // MANDATORY: direct link to ERP Item master
    // Service enforces: if variant != null → item must = variant.item
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Set on READY_TO_SHIP / SHIPPED transition
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    // Set when item.hasLotTracking = true (populated by EcInventoryService)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private InventoryLot inventoryLot;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    // SNAPSHOTS — never re-derive from Item master after order save
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;       // what customer paid

    @Column(precision = 18, scale = 2)
    private BigDecimal costPrice;       // Item.costPrice at time of sale (COGS basis)

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    // Computed at order close: (unitPrice - costPrice) * quantity
    @Column(precision = 18, scale = 2)
    private BigDecimal profitAmount;

    @Column(length = 500)
    private String remarks;
}
