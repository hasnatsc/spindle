package com.asg.spindleserp.commercial.entity;

import com.asg.spindleserp.inventory.entity.Item;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "com_commercial_invoice_item",
        indexes = @Index(name = "idx_cii_invoice", columnList = "invoice_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommercialInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private CommercialInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;
    @Column(length = 500)
    private String description;
    @Column(length = 20)
    private String unit;
    private Long deliveryDetailId;   // stub FK
}
