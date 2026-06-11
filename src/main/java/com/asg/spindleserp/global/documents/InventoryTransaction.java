package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import com.asg.spindleserp.inventory.setup.Warehouse;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * InventoryTransaction — immutable append-only ledger row.
 * One row per stock movement. Never updated or deleted.
 * balanceAfter captures the running stock after this transaction.
 */
@Entity
@Table(name = "global_inventory_transactions",
        indexes = {
                @Index(name = "idx_txn_org", columnList = "organization_id"),
                @Index(name = "idx_txn_doc", columnList = "business_document_id"),
                @Index(name = "idx_txn_item", columnList = "item_id"),
                @Index(name = "idx_txn_wh", columnList = "warehouse_id"),
                @Index(name = "idx_txn_lot", columnList = "lot_id"),
                @Index(name = "idx_txn_mvt", columnList = "movement_type"),
                @Index(name = "idx_txn_date", columnList = "transaction_date"),
                @Index(name = "idx_txn_org_date", columnList = "organization_id,transaction_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_document_id")
    private BusinessDocument businessDocument;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private GlobalInventoryLot lot;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    // Quantities
    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;
    @Column(name = "bale_quantity", precision = 18, scale = 3)
    private BigDecimal baleQuantity;
    @Column(precision = 18, scale = 3)
    private BigDecimal bags;
    @Column(name = "bag_quantity")
    private Integer bagQuantity;
    @Column(name = "cones_per_bag")
    private Integer conesPerBag;
    @Column(precision = 18, scale = 3)
    private BigDecimal cones;
    @Column(name = "actual_weight", precision = 12, scale = 3)
    private BigDecimal actualWeight;
    @Column(name = "net_weight", precision = 12, scale = 3)
    private BigDecimal netWeight;

    // Costing
    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;
    @Column(name = "total_cost", precision = 18, scale = 2)
    private BigDecimal totalCost;

    /**
     * Stock level AFTER this transaction — enables point-in-time queries
     */
    @Column(name = "balance_after", precision = 18, scale = 3)
    private BigDecimal balanceAfter;

    @Column(name = "transaction_date")
    private LocalDate transactionDate;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
