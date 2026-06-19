package com.asg.spindleserp.inventory.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.common.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inv_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_item_org_code", columnNames = {"organization_id", "item_code"}),
                @UniqueConstraint(name = "uq_item_org_name", columnNames = {"organization_id", "item_name"})
        },
        indexes = {
                @Index(name = "idx_item_org", columnList = "organization_id"),
                @Index(name = "idx_item_type", columnList = "item_type"),
                @Index(name = "idx_item_cat", columnList = "category_id"),
                @Index(name = "idx_item_active", columnList = "is_active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_unit_id", nullable = false)
    private ItemUom purchaseUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_unit_id", nullable = false)
    private ItemUom salesUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_unit_id", nullable = false)
    private ItemUom operationUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private ItemBrand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private ItemModel model;

    // Soft FKs — resolved at service layer
    private Long hsCodeId;
    private Long originId;   // → stp_location_countries

    // ── Core identification ────────────────────────────────────────────────
    @Column(nullable = false, length = 50)
    private String itemCode;
    @Column(nullable = false, length = 200)
    private String itemName;
    @Column(length = 200)
    private String itemNameBn;

    // ★ GENERIC item type (replaces FIBER/YARN/FABRICS)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemType itemType = ItemType.GENERAL;

    @Column(length = 100)
    private String sku;
    @Column(length = 100)
    private String barcode;

    // Display codes (denormalized for performance)
    @Column(nullable = false, length = 20)
    private String unitOfMeasure;
    @Column(nullable = false, length = 20)
    private String purchaseUnitCode;
    @Column(nullable = false, length = 20)
    private String salesUnitCode;

    // ── Costing ───────────────────────────────────────────────────────────
    @Column(precision = 12, scale = 4)
    private BigDecimal costPrice;
    @Column(precision = 12, scale = 4)
    private BigDecimal unitPrice;
    @Column(precision = 5, scale = 2)
    private BigDecimal taxRate;
    @Column(precision = 12, scale = 4)
    private BigDecimal standardCost; // COGS basis fallback

    // ── Stock control ─────────────────────────────────────────────────────
    @Column(precision = 12, scale = 3)
    private BigDecimal minimumStock;
    @Column(precision = 12, scale = 3)
    private BigDecimal maximumStock;
    @Column(precision = 12, scale = 3)
    private BigDecimal reorderLevel;

    // ── Generic production fields ─────────────────────────────────────────
    @Column(precision = 5, scale = 2)
    private BigDecimal yieldPercent;     // expected output %
    @Column(precision = 5, scale = 2)
    private BigDecimal processLossPct;   // expected waste %

    // ── Physical specs ────────────────────────────────────────────────────
    @Column(precision = 12, scale = 4)
    private BigDecimal weight;    // kg per unit
    @Column(precision = 12, scale = 4)
    private BigDecimal volume;    // litres per unit
    @Column(length = 100)
    private String dimensions;                  // e.g. "30x20x10 cm"

    // ── Shelf life ────────────────────────────────────────────────────────
    private Integer shelfLifeDays;
    private LocalDate expiryDate;
    @Builder.Default
    @Column(nullable = false)
    private boolean hasLotTracking = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean hasSerial = false;

    // ── Asset-specific (FIXED_ASSET type only) ────────────────────────────
    @Column(length = 100)
    private String serialNumber;
    @Column(length = 100)
    private String modelName;
    @Column(length = 100)
    private String manufacturer;
    private Integer warrantyMonths;
    @Column(precision = 5, scale = 2)
    private BigDecimal depreciationRate;

    // ── Chemical / hazardous (CONSUMABLE / MRO type) ─────────────────────
    @Column(length = 50)
    private String casNumber;
    @Builder.Default
    @Column(nullable = false)
    private boolean isHazardous = false;
    @Column(length = 255)
    private String safetyDataSheet;

    // ── Descriptions ──────────────────────────────────────────────────────
    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String internalNotes;

    // ── Status ────────────────────────────────────────────────────────────
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean isApproved = false;
    @Column(length = 100)
    private String approvedBy;
    private LocalDateTime approvedAt;
}
