package com.asg.spindleserp.global.documents;

import com.asg.spindleserp.common.BaseAuditEntity;
import com.asg.spindleserp.dummy.BusinessDocumentLineLot;
import com.asg.spindleserp.global.lot.GlobalInventoryLot;
import com.asg.spindleserp.inventory.item.InventoryItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_business_document_lines",
        indexes = {
                @Index(name = "idx_gdl_doc", columnList = "document_id"),
                @Index(name = "idx_gdl_item", columnList = "item_id"),
                @Index(name = "idx_gdl_lot", columnList = "inventory_lot_id"),
                @Index(name = "idx_gdl_source", columnList = "source_line_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessDocumentLine extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private BusinessDocument document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private GlobalInventoryLot inventoryLot;

    /**
     * Source line from upstream document.
     * MUST use EntityManager.getReference() — never new BDLine() with only id set.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_line_id")
    private BusinessDocumentLine sourceLine;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    @Column(name = "item_code", length = 100)
    private String itemCode;
    @Column(name = "item_name", length = 500)
    private String itemName;
    @Column(length = 100)
    private String sku;
    @Column(length = 1000)
    private String description;
    @Column(name = "unit_code", length = 20)
    private String unitCode;

    // Quantities
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;
    @Column(name = "delivered_quantity", precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal deliveredQuantity = BigDecimal.ZERO;
    @Column(name = "received_quantity", precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal receivedQuantity = BigDecimal.ZERO;
    @Column(name = "accepted_quantity", precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal acceptedQuantity = BigDecimal.ZERO;
    @Column(name = "rejected_quantity", precision = 18, scale = 3)
    @Builder.Default
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;
    @Column(name = "bale_quantity", precision = 12, scale = 2)
    private BigDecimal baleQuantity;
    @Column(precision = 10, scale = 0)
    private BigDecimal bags;

    // Pricing
    @Column(name = "unit_price", precision = 18, scale = 4)
    private BigDecimal unitPrice;
    @Builder.Default
    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "line_amount", precision = 18, scale = 2)
    private BigDecimal lineAmount = BigDecimal.ZERO;

    // Lot / quality
    @Column(name = "batch_number", length = 100)
    private String batchNumber;
    @Column(name = "quality_status", length = 30)
    private String qualityStatus; // PENDING|PASSED|FAILED|QUARANTINE
    @Column(name = "quality_remarks", columnDefinition = "TEXT")
    private String qualityRemarks;
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(columnDefinition = "TEXT")
    private String remarks; // JSON-packed for PRQ/PMI

    @OneToMany(mappedBy = "documentLine", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BusinessDocumentLineLot> lots = new ArrayList<>();

    public void calculateAmount() {
        if (unitPrice != null && quantity != null) {
            BigDecimal gross = unitPrice.multiply(quantity);
            BigDecimal disc = discountAmount != null ? discountAmount : BigDecimal.ZERO;
            BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
            this.lineAmount = gross.subtract(disc).add(tax);
        }
    }
}
