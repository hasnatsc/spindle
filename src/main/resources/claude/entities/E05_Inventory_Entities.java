// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E05  Inventory Masters  (v2 Generic Edition)              ║
// ║  ★ REMOVED: YarnType, YarnCount, YarnProduct, YarnBlendItem              ║
// ║  ★ UPDATED: Item.itemType → generic enum (no fiber/yarn specifics)       ║
// ║  ★ REMOVED: fiber QC columns from Item (micronaire, staple_length, etc.) ║
// ║  ★ KEPT:    yield_percent, process_loss_pct (generic production fields)  ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: inventory/entity/ItemCategory.java ─────────────────────────────────
package com.hasnat.optimum.inventory.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.common.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inv_item_categories",
    uniqueConstraints = @UniqueConstraint(name = "uq_icat_org_code",
        columnNames = {"organization_id", "category_code"}),
    indexes = {
        @Index(name = "idx_icat_org",    columnList = "organization_id"),
        @Index(name = "idx_icat_parent", columnList = "parent_category_id"),
        @Index(name = "idx_icat_type",   columnList = "item_type")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemCategory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ItemCategory parentCategory;

    @Column(nullable = false, length = 50)  private String categoryCode;
    @Column(nullable = false, length = 100) private String categoryName;
    @Column(columnDefinition = "text")      private String description;

    // ★ Generic item type — applies to any manufacturing industry
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ItemType itemType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LayerType layerType = LayerType.ITEM;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    public enum LayerType { ROOT, GROUP, ITEM }
}

// ── FILE: inventory/entity/ItemUom.java ──────────────────────────────────────
package com.hasnat.optimum.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inv_item_uom",
    uniqueConstraints = @UniqueConstraint(name = "uq_uom_org_code",
        columnNames = {"organization_id", "code"}),
    indexes = @Index(name = "idx_uom_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemUom {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 20)  private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 20) private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UomCategory category;

    @Builder.Default @Column(nullable = false) private boolean isBaseUnit = false;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @Builder.Default @Column(nullable = false) private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum UomCategory { WEIGHT, COUNT, LENGTH, VOLUME, AREA, PACKING, UNIT }
}

// ── FILE: inventory/entity/ItemBrand.java ────────────────────────────────────
package com.hasnat.optimum.inventory.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inv_item_brands",
    uniqueConstraints = @UniqueConstraint(name = "uq_brand_org_code",
        columnNames = {"organization_id", "brand_code"}),
    indexes = @Index(name = "idx_brand_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemBrand extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 30)  private String brandCode;
    @Column(nullable = false, length = 150) private String brandName;
    @Column(columnDefinition = "text")      private String description;

    @Builder.Default @Column(nullable = false) private boolean isActive = true;
}

// ── FILE: inventory/entity/ItemModel.java ────────────────────────────────────
package com.hasnat.optimum.inventory.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inv_item_models",
    uniqueConstraints = @UniqueConstraint(name = "uq_model_org_brand_code",
        columnNames = {"organization_id", "brand_id", "model_code"}),
    indexes = @Index(name = "idx_model_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemModel extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private ItemBrand brand;

    @Column(nullable = false, length = 30)  private String modelCode;
    @Column(nullable = false, length = 150) private String modelName;
    @Column(columnDefinition = "text")      private String description;

    @Builder.Default @Column(nullable = false) private boolean isActive = true;
}

// ── FILE: inventory/entity/Item.java ─────────────────────────────────────────
// ★ GENERIC — no fiber/yarn columns. Suitable for any manufacturing industry.
package com.hasnat.optimum.inventory.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.common.enums.ItemType;
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
        @Index(name = "idx_item_org",    columnList = "organization_id"),
        @Index(name = "idx_item_type",   columnList = "item_type"),
        @Index(name = "idx_item_cat",    columnList = "category_id"),
        @Index(name = "idx_item_active", columnList = "is_active")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Item extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

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
    @Column(nullable = false, length = 50)  private String itemCode;
    @Column(nullable = false, length = 200) private String itemName;
    @Column(length = 200) private String itemNameBn;

    // ★ GENERIC item type (replaces FIBER/YARN/FABRICS)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemType itemType = ItemType.GENERAL;

    @Column(length = 100) private String sku;
    @Column(length = 100) private String barcode;

    // Display codes (denormalized for performance)
    @Column(nullable = false, length = 20) private String unitOfMeasure;
    @Column(nullable = false, length = 20) private String purchaseUnitCode;
    @Column(nullable = false, length = 20) private String salesUnitCode;

    // ── Costing ───────────────────────────────────────────────────────────
    @Column(precision = 12, scale = 4) private BigDecimal costPrice;
    @Column(precision = 12, scale = 4) private BigDecimal unitPrice;
    @Column(precision = 5,  scale = 2) private BigDecimal taxRate;
    @Column(precision = 12, scale = 4) private BigDecimal standardCost; // COGS basis fallback

    // ── Stock control ─────────────────────────────────────────────────────
    @Column(precision = 12, scale = 3) private BigDecimal minimumStock;
    @Column(precision = 12, scale = 3) private BigDecimal maximumStock;
    @Column(precision = 12, scale = 3) private BigDecimal reorderLevel;

    // ── Generic production fields ─────────────────────────────────────────
    @Column(precision = 5, scale = 2) private BigDecimal yieldPercent;     // expected output %
    @Column(precision = 5, scale = 2) private BigDecimal processLossPct;   // expected waste %

    // ── Physical specs ────────────────────────────────────────────────────
    @Column(precision = 12, scale = 4) private BigDecimal weight;    // kg per unit
    @Column(precision = 12, scale = 4) private BigDecimal volume;    // litres per unit
    @Column(length = 100) private String dimensions;                  // e.g. "30x20x10 cm"

    // ── Shelf life ────────────────────────────────────────────────────────
    private Integer shelfLifeDays;
    private LocalDate expiryDate;
    @Builder.Default @Column(nullable = false) private boolean hasLotTracking = false;
    @Builder.Default @Column(nullable = false) private boolean hasSerial       = false;

    // ── Asset-specific (FIXED_ASSET type only) ────────────────────────────
    @Column(length = 100) private String serialNumber;
    @Column(length = 100) private String modelName;
    @Column(length = 100) private String manufacturer;
    private Integer warrantyMonths;
    @Column(precision = 5, scale = 2) private BigDecimal depreciationRate;

    // ── Chemical / hazardous (CONSUMABLE / MRO type) ─────────────────────
    @Column(length = 50)  private String  casNumber;
    @Builder.Default @Column(nullable = false) private boolean isHazardous      = false;
    @Column(length = 255) private String  safetyDataSheet;

    // ── Descriptions ──────────────────────────────────────────────────────
    @Column(columnDefinition = "text") private String description;
    @Column(columnDefinition = "text") private String internalNotes;

    // ── Status ────────────────────────────────────────────────────────────
    @Builder.Default @Column(nullable = false) private boolean isActive   = true;
    @Builder.Default @Column(nullable = false) private boolean isApproved = false;
    @Column(length = 100) private String approvedBy;
    private LocalDateTime approvedAt;
}
