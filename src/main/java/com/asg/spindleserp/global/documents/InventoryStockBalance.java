package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_inventory_stock_balances",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_balance",
                columnNames = {"item_id", "warehouse_id", "lot_id"}),
        indexes = {
                @Index(name = "idx_sb_item", columnList = "item_id"),
                @Index(name = "idx_sb_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_sb_lot", columnList = "lot_id"),
                @Index(name = "idx_sb_org", columnList = "organization_id")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private GlobalInventoryLot lot;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;
    @Column(name = "bale_quantity", precision = 12, scale = 3)
    private BigDecimal baleQuantity;
    @Column
    private Integer bags;
    @Column(name = "bag_quantity")
    private Integer bagQuantity;
    @Column(name = "cones_per_bag")
    private Integer conesPerBag;
    @Column(name = "cone_quantity")
    private Integer coneQuantity;
    @Column(name = "actual_weight", precision = 12, scale = 3)
    private BigDecimal actualWeight;
    @Column(name = "net_weight", precision = 12, scale = 3)
    private BigDecimal netWeight;
    @Builder.Default
    @Column(name = "average_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal averageCost = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "stock_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal stockValue = BigDecimal.ZERO;
    @Column(name = "last_transaction_time")
    private LocalDateTime lastTransactionTime;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
