package com.asg.spindleserp.global.entity;

import com.asg.spindleserp.common.enums.MovementType;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_inventory_transactions",
    indexes = {
        @Index(name = "idx_invtx_item", columnList = "item_id"),
        @Index(name = "idx_invtx_wh",   columnList = "warehouse_id"),
        @Index(name = "idx_invtx_doc",  columnList = "business_document_id"),
        @Index(name = "idx_invtx_date", columnList = "transaction_date"),
        @Index(name = "idx_invtx_org",  columnList = "organization_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_id", nullable = false)
    private BusinessDocument businessDocument;

    @Column(nullable = false, length = 50) private String documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MovementType movementType;

    private LocalDate transactionDate;

    @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity;
    @Column(precision = 12, scale = 3) private BigDecimal grossWeight;
    @Column(precision = 12, scale = 3) private BigDecimal netWeight;
    @Column(precision = 18, scale = 4) private BigDecimal unitCost;
    @Column(precision = 18, scale = 2) private BigDecimal totalCost;
    @Column(precision = 18, scale = 3) private BigDecimal balanceAfter;

    @Column(length = 255) private String remarks;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
