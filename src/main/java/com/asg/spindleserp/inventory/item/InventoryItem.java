package com.asg.spindleserp.inventory.item;

import com.asg.spindleserp.common.BaseAuditEntity;
import com.asg.spindleserp.security.locations.Country;
import com.asg.spindleserp.stp.HsCode;
import com.asg.spindleserp.inventory.setup.ItemType;
import com.asg.spindleserp.inventory.setup.UnitsOfMeasure;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inv_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inv_item_code", columnNames = {"organization_id", "item_code"}),
                @UniqueConstraint(name = "uk_inv_item_name", columnNames = {"organization_id", "item_name"})
        },
        indexes = {
                @Index(name = "idx_inv_item_org", columnList = "organization_id"),
                @Index(name = "idx_inv_item_type", columnList = "item_type"),
                @Index(name = "idx_inv_item_code", columnList = "item_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── References ──────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_unit_id", nullable = false)
    private UnitsOfMeasure operationUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_unit_id")
    private UnitsOfMeasure purchaseUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_unit_id")
    private UnitsOfMeasure salesUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hs_code_id")
    private HsCode hsCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id")
    private Country origin;

    // ── Identity ─────────────────────────────────────────────────────────────
    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    @Column(name = "item_name_bn", length = 200)
    private String itemNameBn;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(length = 100)
    private String barcode;
    @Column(length = 100)
    private String sku;
    @Column(name = "origin_name", length = 100)
    private String originName;

    // ── Discriminator ────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    @Builder.Default
    private ItemType itemType = ItemType.GENERAL;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;
    @Column(name = "purchase_unit_code", nullable = false, length = 20)
    private String purchaseUnitCode;
    @Column(name = "sales_unit_code", nullable = false, length = 20)
    private String salesUnitCode;

    // ── Stock control ────────────────────────────────────────────────────────
    @Column(name = "reorder_level", precision = 12, scale = 3)
    private BigDecimal reorderLevel;
    @Column(name = "minimum_stock", precision = 12, scale = 3)
    private BigDecimal minimumStock;
    @Column(name = "maximum_stock", precision = 12, scale = 3)
    private BigDecimal maximumStock;
    @Column(name = "unit_price", precision = 12, scale = 4)
    private BigDecimal unitPrice;
    @Column(name = "cost_price", precision = 12, scale = 4)
    private BigDecimal costPrice;
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    // ── Fiber/Cotton fields (item_type IN FIBER, RAW_COTTON) ─────────────────
    @Column(name = "fiber_type", length = 30)
    private String fiberType;
    @Column(length = 50)
    private String grade;
    @Column(name = "staple_length", precision = 8, scale = 2)
    private BigDecimal stapleLength;
    @Column(precision = 8, scale = 2)
    private BigDecimal micronaire;
    @Column(precision = 8, scale = 2)
    private BigDecimal strength;
    @Column(precision = 8, scale = 2)
    private BigDecimal moisture;
    @Column(precision = 5, scale = 2)
    private BigDecimal trash;
    @Column(precision = 5, scale = 2)
    private BigDecimal purity;

    // ── Chemical/Dye fields (item_type IN CHEMICALS, DYES) ──────────────────
    @Column(name = "chemical_formula", length = 50)
    private String chemicalFormula;
    @Column(name = "cas_number", length = 50)
    private String casNumber;
    @Builder.Default
    @Column(name = "is_hazardous")
    private Boolean isHazardous = false;
    @Column(name = "safety_data_sheet", length = 100)
    private String safetyDataSheet;
    @Column(precision = 8, scale = 2)
    private BigDecimal concentration;
    @Column(name = "chem_expiry_date")
    private LocalDateTime chemExpiryDate;

    // ── Fixed Asset fields (item_type = FIXED_ASSET) ─────────────────────────
    @Column(length = 100)
    private String manufacturer;
    @Column(length = 100)
    private String model;
    @Column(name = "serial_number", length = 50)
    private String serialNumber;
    @Column(name = "warranty_months")
    private Integer warrantyMonths;
    @Column(name = "asset_value", precision = 15, scale = 2)
    private BigDecimal assetValue;
    @Column(name = "depreciation_rate", precision = 5, scale = 2)
    private BigDecimal depreciationRate;

    // ── Production & costing ─────────────────────────────────────────────────
    @Column(name = "yield_percent", precision = 5, scale = 2)
    private BigDecimal yieldPercent;
    @Column(name = "standard_cost_per_kg", precision = 12, scale = 2)
    private BigDecimal standardCostPerKg;
    @Column(name = "selling_price_per_kg", precision = 12, scale = 2)
    private BigDecimal sellingPricePerKg;
    @Column(name = "process_loss_percent", precision = 5, scale = 2)
    private BigDecimal processLossPercent;

    // ── Approval & status ────────────────────────────────────────────────────
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // ── 1:1 yarn extension ───────────────────────────────────────────────────
    @OneToOne(mappedBy = "item", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private YarnItem yarnItem;
}
