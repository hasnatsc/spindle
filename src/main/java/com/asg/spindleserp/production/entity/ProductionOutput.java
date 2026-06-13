package com.asg.spindleserp.production.entity;

import com.asg.spindleserp.global.entity.InventoryLot;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.inventory.entity.ItemUom;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * Finished goods produced in a Production Order.
 * unitCost is copied from Production.unitCost when the order is completed.
 * totalCost = quantity × unitCost = the COGS value used when this lot is sold.
 *
 * A new InventoryLot is created for each output line with lot.unitCost = this.unitCost,
 * so when a Sales Invoice is posted, COGS Dr = soldQty × lot.unitCost.
 */
@Entity
@Table(name = "prd_production_outputs",
    indexes = {
        @Index(name = "idx_prdo_prod", columnList = "production_id"),
        @Index(name = "idx_prdo_item", columnList = "finished_item_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductionOutput {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    /** Lot created for this batch of finished goods */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ItemUom unit;

    @Column(nullable = false) private Integer lineNumber;

    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal quantity;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;

    /** Copied from Production.unitCost at completion — COGS basis when sold */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    /** quantity × unitCost — value entering Finished Goods Inventory */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(length = 100) private String batchNo;
    @Column(columnDefinition = "text") private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
