package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.global.documents.InventoryTransaction;
import com.asg.spindleserp.global.documents.BusinessDocumentLine;
import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eco_shipment_items",
        indexes = {
                @Index(name = "idx_sitem_ship", columnList = "shipment_id"),
                @Index(name = "idx_sitem_order", columnList = "eco_order_line_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoShipmentItem implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private EcoShipment shipment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eco_order_line_id", nullable = false)
    private EcoOrderLine ecoOrderLine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_line_id", nullable = false)
    private BusinessDocumentLine businessDocumentLine;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inv_item_id", nullable = false)
    private InventoryItem invItem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private GlobalInventoryLot inventoryLot;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcoProductVariant variant;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    /**
     * FK to the InventoryTransaction created when stock was deducted
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_transaction_id")
    private InventoryTransaction inventoryTransaction;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
