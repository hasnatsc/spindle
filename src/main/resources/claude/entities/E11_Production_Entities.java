// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E11  Generic Production Module  (v2)                      ║
// ║                                                                          ║
// ║  ★ COMPLETE REWRITE — replaces yarn-specific module                     ║
// ║  ★ REMOVED: ProductionOrder (yarn), ProductionRecipe,                   ║
// ║             ProductionRecipeItem, ProductionRecipeItemLot               ║
// ║  ★ ADDED:   Bom, BomItem (reusable Bill of Materials)                  ║
// ║             Production (generic work order + cost sheet)                ║
// ║             ProductionInput  (raw materials consumed + lot tracking)    ║
// ║             ProductionOutput (finished goods produced + lot tracking)   ║
// ║                                                                          ║
// ║  Works for ANY manufacturing industry:                                   ║
// ║    Bakery, Garments, Furniture, Chemicals, Food Processing, Plastics,   ║
// ║    Electronics assembly, Pharmaceuticals, General factories             ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: production/entity/Bom.java ─────────────────────────────────────────
package com.hasnat.optimum.production.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.inventory.entity.ItemUom;
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
 *
 * A Production Order (prd_productions) may reference a BOM
 * to pre-fill its input lines, or be created without one (ad-hoc).
 */
@Entity
@Table(name = "prd_bom",
    uniqueConstraints = @UniqueConstraint(name = "uq_bom_org_code",
        columnNames = {"organization_id", "bom_code"}),
    indexes = {
        @Index(name = "idx_bom_org",  columnList = "organization_id"),
        @Index(name = "idx_bom_item", columnList = "finished_item_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bom extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** The finished product this BOM produces */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_unit_id", nullable = false)
    private ItemUom outputUnit;

    @Column(nullable = false, length = 50)  private String bomCode;
    @Column(nullable = false, length = 200) private String bomName;
    @Builder.Default @Column(nullable = false, length = 20) private String bomVersion = "1.0";

    /** How many units of finishedItem this BOM produces in one run */
    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal outputQuantity = BigDecimal.ONE;

    /** Expected output as % of raw materials consumed (e.g. 92% for bakery) */
    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal yieldPercent = new BigDecimal("100.00");

    @Builder.Default @Column(nullable = false) private boolean isActive  = true;
    @Builder.Default @Column(nullable = false) private boolean isDefault = false;

    @Column(columnDefinition = "text") private String description;
    @Column(columnDefinition = "text") private String notes;
    @Column(length = 100) private String approvedBy;
    private LocalDateTime approvedAt;

    @Builder.Default
    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomItem> items = new ArrayList<>();
}

// ── FILE: production/entity/BomItem.java ─────────────────────────────────────
package com.hasnat.optimum.production.entity;

import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.inventory.entity.ItemUom;
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
        @Index(name = "idx_bom_items_bom",  columnList = "bom_id"),
        @Index(name = "idx_bom_items_item", columnList = "raw_item_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BomItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(nullable = false) private Integer lineNumber;

    /** Quantity needed per bom.outputQuantity */
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal scrapPct = BigDecimal.ZERO;

    @Builder.Default @Column(nullable = false) private boolean isOptional = false;

    @Column(columnDefinition = "text") private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}

// ── FILE: production/entity/Production.java ──────────────────────────────────
package com.hasnat.optimum.production.entity;

import com.hasnat.optimum.accounts.entity.JournalEntryMaster;
import com.hasnat.optimum.approval.entity.ApprovalRequest;
import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.global.entity.BusinessDocument;
import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.inventory.entity.ItemUom;
import com.hasnat.optimum.organization.entity.CostCenter;
import com.hasnat.optimum.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic Production Work Order.
 *
 * Contains both the work order header AND a built-in cost sheet.
 * The cost sheet has 4 cost components:
 *   materialCost  = auto-summed from ProductionInput.totalCost when production is completed
 *   laborCost     = pulled from hrm_cost_center_allocations for this costCenter + month
 *   overheadCost  = manual entry or overhead rate allocation
 *   otherCost     = packaging, freight, utilities, etc.
 *   totalCost     = materialCost + laborCost + overheadCost + otherCost
 *   unitCost      = totalCost / producedQuantity
 *
 * unitCost is then copied to each ProductionOutput.unitCost for COGS calculation on sale.
 */
@Entity
@Table(name = "prd_productions",
    uniqueConstraints = @UniqueConstraint(name = "uq_prd2_org_no",
        columnNames = {"organization_id", "production_no"}),
    indexes = {
        @Index(name = "idx_prd2_org",    columnList = "organization_id"),
        @Index(name = "idx_prd2_status", columnList = "status"),
        @Index(name = "idx_prd2_item",   columnList = "finished_item_id"),
        @Index(name = "idx_prd2_date",   columnList = "production_date"),
        @Index(name = "idx_prd2_bom",    columnList = "bom_id"),
        @Index(name = "idx_prd2_so",     columnList = "sales_order_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Production extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** Optional — pre-fills inputs from BOM template */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id")
    private Bom bom;

    /** The finished product being manufactured */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    /** Where finished goods will be stored after production */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_warehouse_id", nullable = false)
    private Warehouse outputWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    /** Optional link to Sales Order that triggered this production */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id")
    private BusinessDocument salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    /** Journal entry created when production is posted to GL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_unit_id", nullable = false)
    private ItemUom outputUnit;

    // ── Work Order Header ──────────────────────────────────────────────────
    @Column(nullable = false, length = 50) private String productionNo;
    @Column(nullable = false)              private LocalDate productionDate;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    // ── Quantities ─────────────────────────────────────────────────────────
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal plannedQuantity;

    @Builder.Default @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal producedQuantity  = BigDecimal.ZERO;

    @Builder.Default @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal rejectedQuantity  = BigDecimal.ZERO;

    @Builder.Default @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal wasteQuantity     = BigDecimal.ZERO;

    // ── COST SHEET ─────────────────────────────────────────────────────────
    // materialCost: sum of ProductionInput.totalCost after completion
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal materialCost = BigDecimal.ZERO;

    // laborCost: from hrm_cost_center_allocations (costCenter + allocationMonth)
    // Proportional to this order's produced_quantity vs total monthly production
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal laborCost = BigDecimal.ZERO;

    // overheadCost: factory rent, electricity, depreciation — manual or allocated
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal overheadCost = BigDecimal.ZERO;

    // otherCost: packaging, transport, quality testing, etc.
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal otherCost = BigDecimal.ZERO;

    // totalCost = materialCost + laborCost + overheadCost + otherCost
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    // unitCost = totalCost / producedQuantity — used as COGS basis when goods are sold
    @Builder.Default @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    // ── Status ─────────────────────────────────────────────────────────────
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductionStatus status = ProductionStatus.DRAFT;

    @Column(length = 30) private String approvalStatus;
    @Column(columnDefinition = "text") private String remarks;

    @Builder.Default
    @OneToMany(mappedBy = "production", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionInput> inputs = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "production", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionOutput> outputs = new ArrayList<>();

    public enum ProductionStatus {
        DRAFT, SUBMITTED, APPROVED, RELEASED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED
    }
}

// ── FILE: production/entity/ProductionInput.java ─────────────────────────────
package com.hasnat.optimum.production.entity;

import com.hasnat.optimum.global.entity.InventoryLot;
import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.inventory.entity.ItemUom;
import com.hasnat.optimum.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One raw material line consumed in a Production Order.
 * Tracks actual quantity consumed, the specific lot used (for FIFO/LIFO costing),
 * the unit cost from the lot, and total cost (for cost sheet materialCost).
 *
 * Multiple lots can be used for the same raw material — one line per lot.
 */
@Entity
@Table(name = "prd_production_inputs",
    indexes = {
        @Index(name = "idx_prdi_prod", columnList = "production_id"),
        @Index(name = "idx_prdi_item", columnList = "raw_item_id"),
        @Index(name = "idx_prdi_lot",  columnList = "lot_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductionInput {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_item_id", nullable = false)
    private Item rawItem;

    /** Specific lot being consumed — null if no lot tracking for this item */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /** Reference to BOM line this input satisfies — null if ad-hoc */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_item_id")
    private BomItem bomItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ItemUom unit;

    @Column(nullable = false) private Integer lineNumber;

    /** From BOM — what was expected */
    @Column(precision = 14, scale = 3) private BigDecimal plannedQuantity;

    /** What was actually consumed */
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal actualQuantity;

    /** Unit cost from the inventory lot or standard cost */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    /** actualQuantity × unitCost — feeds into Production.materialCost */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    /** Scrap from this ingredient (for waste tracking) */
    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal scrapQuantity = BigDecimal.ZERO;

    @Column(columnDefinition = "text") private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}

// ── FILE: production/entity/ProductionOutput.java ────────────────────────────
package com.hasnat.optimum.production.entity;

import com.hasnat.optimum.global.entity.InventoryLot;
import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.inventory.entity.ItemUom;
import com.hasnat.optimum.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Finished goods produced in a Production Order.
 * unitCost is copied from Production.unitCost when the order is completed.
 * totalCost = quantity × unitCost = the COGS value used when this lot is sold.
 *
 * A new InventoryLot is created for each output line with lot.unitCost = this.unitCost,
 * so when a Sales Invoice is posted, COGS Dr = soldQty × lot.unitCost.
 */
@Entity
@Table(name = "prd_production_outputs",
    indexes = {
        @Index(name = "idx_prdo_prod", columnList = "production_id"),
        @Index(name = "idx_prdo_item", columnList = "finished_item_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductionOutput {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_id", nullable = false)
    private Production production;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    /** Lot created for this batch of finished goods */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ItemUom unit;

    @Column(nullable = false) private Integer lineNumber;

    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal quantity;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;

    /** Copied from Production.unitCost at completion — COGS basis when sold */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    /** quantity × unitCost — value entering Finished Goods Inventory */
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(length = 100) private String batchNo;
    @Column(columnDefinition = "text") private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
