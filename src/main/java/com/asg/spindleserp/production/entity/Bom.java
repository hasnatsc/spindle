package com.asg.spindleserp.production.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.inventory.entity.Item;
import com.asg.spindleserp.inventory.entity.ItemUom;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bill of Materials — reusable master template.
 * One BOM per finished product (e.g. "Chocolate Biscuit 200g").
 * Defines which raw materials and quantities to consume to produce
 * a given output_quantity of the finished item.
 * <p>
 * A Production Order (prd_productions) may reference a BOM
 * to pre-fill its input lines, or be created without one (ad-hoc).
 */
@Entity
@Table(name = "prd_bom",
        uniqueConstraints = @UniqueConstraint(name = "uq_bom_org_code",
                columnNames = {"organization_id", "bom_code"}),
        indexes = {
                @Index(name = "idx_bom_org", columnList = "organization_id"),
                @Index(name = "idx_bom_item", columnList = "finished_item_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * The finished product this BOM produces
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_unit_id", nullable = false)
    private ItemUom outputUnit;

    @Column(nullable = false, length = 50)
    private String bomCode;
    @Column(nullable = false, length = 200)
    private String bomName;
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String bomVersion = "1.0";

    /**
     * How many units of finishedItem this BOM produces in one run
     */
    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal outputQuantity = BigDecimal.ONE;

    /**
     * Expected output as % of raw materials consumed (e.g. 92% for bakery)
     */
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal yieldPercent = new BigDecimal("100.00");

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;

    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(length = 100)
    private String approvedBy;
    private LocalDateTime approvedAt;

    @Builder.Default
    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomItem> items = new ArrayList<>();
}
