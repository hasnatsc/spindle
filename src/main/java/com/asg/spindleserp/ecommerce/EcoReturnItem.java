package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.global.documents.InventoryTransaction;
import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eco_return_items",
        indexes = @Index(name = "idx_ritem_return", columnList = "eco_return_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoReturnItem implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_return_id", nullable = false)
    private EcoReturn ecoReturn;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_line_id", nullable = false)
    private EcoOrderLine ecoOrderLine;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_document_line_id")
    private BusinessDocumentLine businessDocumentLine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inv_item_id", nullable = false)
    private InventoryItem invItem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private GlobalInventoryLot inventoryLot;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    @Column(name = "return_condition", length = 20)
    @Builder.Default
    private String returnCondition = "GOOD";
    // GOOD|DAMAGED|USED|EXPIRED
    @Builder.Default
    @Column(name = "restock", nullable = false)
    private Boolean restock = true;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_transaction_id")
    private InventoryTransaction inventoryTransaction;  // set after stock is restored
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
