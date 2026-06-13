package com.asg.spindleserp.global.entity;

import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_inventory_stock_balances",
        uniqueConstraints = @UniqueConstraint(name = "uq_stock_item_wh_lot",
                columnNames = {"item_id", "warehouse_id", "lot_id"}),
        indexes = {
                @Index(name = "idx_stock_item", columnList = "item_id"),
                @Index(name = "idx_stock_wh", columnList = "warehouse_id"),
                @Index(name = "idx_stock_lot", columnList = "lot_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryStockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;
    @Column(precision = 12, scale = 3)
    private BigDecimal grossWeight;
    @Column(precision = 12, scale = 3)
    private BigDecimal netWeight;
    @Column(precision = 18, scale = 4)
    private BigDecimal averageCost;
    @Column(precision = 18, scale = 2)
    private BigDecimal stockValue;
    private LocalDateTime lastTransactionTime;
}
