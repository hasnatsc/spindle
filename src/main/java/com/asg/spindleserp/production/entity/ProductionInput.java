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
 * One raw material line consumed in a Production Order.
 * Tracks actual quantity consumed, the specific lot used (for FIFO/LIFO costing),
 * the unit cost from the lot, and total cost (for cost sheet materialCost).
 * <p>
 * Multiple lots can be used for the same raw material — one line per lot.
 */
@Entity
@Table(name = "prd_production_inputs",
        indexes = {
                @Index(name = "idx_prdi_prod", columnList = "production_id"),
                @Index(name = "idx_prdi_item", columnList = "raw_item_id"),
                @Index(name = "idx_prdi_lot", columnList = "lot_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductionInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_item_id", nullable = false)
    private Item rawItem;

    /**
     * Specific lot being consumed — null if no lot tracking for this item
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /**
     * Reference to BOM line this input satisfies — null if ad-hoc
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_item_id")
    private BomItem bomItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ItemUom unit;

    @Column(nullable = false)
    private Integer lineNumber;

    /**
     * From BOM — what was expected
     */
    @Column(precision = 14, scale = 3)
    private BigDecimal plannedQuantity;

    /**
     * What was actually consumed
     */
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal actualQuantity;

    /**
     * Unit cost from the inventory lot or standard cost
     */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    /**
     * actualQuantity × unitCost — feeds into Production.materialCost
     */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    /**
     * Scrap from this ingredient (for waste tracking)
     */
    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal scrapQuantity = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String remarks;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
