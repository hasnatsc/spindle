package com.asg.spindleserp.production.entity;

import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.inventory.entity.ItemUom;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One raw material line in a Bill of Materials.
 * quantity = how much of rawItem is needed per BOM outputQuantity.
 * scrapPct = expected waste % for this ingredient (e.g. 5% for flour trimming).
 */
@Entity
@Table(name = "prd_bom_items",
        indexes = {
                @Index(name = "idx_bom_items_bom", columnList = "bom_id"),
                @Index(name = "idx_bom_items_item", columnList = "raw_item_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id", nullable = false)
    private Bom bom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_item_id", nullable = false)
    private Item rawItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ItemUom unit;

    @Column(nullable = false)
    private Integer lineNumber;

    /**
     * Quantity needed per bom.outputQuantity
     */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal scrapPct = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private boolean isOptional = false;

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
