// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E08  Global Documents + Stock  (v2 Generic Edition)       ║
// ║  ★ UPDATED: InventoryLot — removed yarn QC (micronaire/staple_length…)  ║
// ║  ★ UPDATED: InventoryLot.itemType → generic enum                        ║
// ║  ★ UPDATED: InventoryLot.productionRecipeId → productionOrderId          ║
// ║  ★ UPDATED: BusinessDocument.documentType → added PRODUCTION_ORDER etc.  ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: global/entity/InventoryLot.java ────────────────────────────────────
package com.hasnat.optimum.global.entity;

import com.hasnat.optimum.common.enums.ItemType;
import com.hasnat.optimum.inventory.entity.Item;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_inv_lots",
    indexes = {
        @Index(name = "idx_lot_item",    columnList = "item_id"),
        @Index(name = "idx_lot_org",     columnList = "organization_id"),
        @Index(name = "idx_lot_status",  columnList = "status"),
        @Index(name = "idx_lot_number",  columnList = "lot_number"),
        @Index(name = "idx_lot_deleted", columnList = "deleted")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryLot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Soft FKs (resolved at service layer to avoid circular deps)
    private Long countryOfOriginId;
    private Long bankId;
    private Long supplierId;           // → acc_chart_of_accounts_sub
    private Long productionOrderId;    // ★ replaces productionRecipeId → prd_productions (DEFERRED)

    @Version
    @Column(name = "version")
    private Long version;              // optimistic lock

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, length = 100)
    private String lotNumber;

    // ★ Generic item type (no FIBER/YARN/FABRICS)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemType itemType = ItemType.GENERAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private LotStatus status = LotStatus.AVAILABLE;

    // ── Generic lot attributes ─────────────────────────────────────────────
    @Column(length = 100) private String batchNo;
    @Column(length = 100) private String manufacturerBatchNo;
    @Column(length = 100) private String serialNo;
    @Column(length = 100) private String binLocation;
    @Column(length = 100) private String shelfLocation;
    @Column(length = 100) private String warehouseLocation;

    // ── Dates ─────────────────────────────────────────────────────────────
    private LocalDate receivedDate;
    private LocalDate manufacturingDate;
    private LocalDate productionDate;
    private LocalDate expiryDate;

    // ★ GENERIC QC attributes (replaces fiber-specific micronaire/staple_length/denier/avg_*)
    @Column(length = 50)  private String  qcGrade;    // A, B, C / PASS / FAIL / etc.
    @Column(columnDefinition = "text") private String qcRemarks;
    private Boolean qcPassed;
    private LocalDate qcDate;
    @Column(length = 100) private String qcBy;

    // ── Physical ──────────────────────────────────────────────────────────
    @Column(precision = 12, scale = 3) private BigDecimal grossWeight;
    @Column(precision = 12, scale = 3) private BigDecimal netWeight;
    @Column(precision = 18, scale = 4) private BigDecimal unitCost;  // cost per unit when received

    @Column(columnDefinition = "text") private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(length = 100) private String createdBy;
    @Column(length = 100) private String updatedBy;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum LotStatus { AVAILABLE, RESERVED, BLOCKED, QC_HOLD, EXPIRED, CONSUMED }
}

// ── FILE: global/entity/BusinessDocument.java ────────────────────────────────
package com.hasnat.optimum.global.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.approval.entity.ApprovalRequest;
import com.hasnat.optimum.common.enums.DocumentType;
import com.hasnat.optimum.organization.entity.Department;
import com.hasnat.optimum.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_business_documents",
    indexes = {
        @Index(name = "idx_gbd_org",     columnList = "organization_id"),
        @Index(name = "idx_gbd_type",    columnList = "document_type"),
        @Index(name = "idx_gbd_status",  columnList = "status"),
        @Index(name = "idx_gbd_party",   columnList = "party_id"),
        @Index(name = "idx_gbd_parent",  columnList = "parent_document_id"),
        @Index(name = "idx_gbd_wh",      columnList = "warehouse_id"),
        @Index(name = "idx_gbd_date",    columnList = "document_date"),
        @Index(name = "idx_gbd_no",      columnList = "document_no"),
        @Index(name = "idx_gbd_deleted", columnList = "is_deleted")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private com.hasnat.optimum.organization.entity.Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_document_id")
    private BusinessDocument parentDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private ChartOfAccountSub party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(nullable = false, unique = true, length = 100) private String documentNo;
    @Column(length = 100) private String documentNoManual;
    @Column(nullable = false)               private LocalDate documentDate;

    // ★ UPDATED: includes PRODUCTION_ORDER, PRODUCTION_MATERIAL_ISSUE, FINISHED_GOODS_RECEIVE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType documentType;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(length = 30) private String approvalStatus;
    @Column(length = 20) private String currency;
    @Column(precision = 18, scale = 4) private BigDecimal exchangeRate;
    @Column(precision = 18, scale = 2) private BigDecimal subtotalAmount;
    @Column(precision = 18, scale = 2) private BigDecimal discountAmount;
    @Column(precision = 18, scale = 2) private BigDecimal taxAmount;
    @Column(precision = 18, scale = 2) private BigDecimal otherCharges;
    @Column(precision = 18, scale = 2) private BigDecimal totalAmount;
    @Column(precision = 18, scale = 2) private BigDecimal paidAmount;
    @Column(precision = 18, scale = 2) private BigDecimal dueAmount;

    @Builder.Default @Column(nullable = false) private boolean stockPosted      = false;
    @Builder.Default @Column(nullable = false) private boolean accountingPosted = false;

    @Column(length = 100) private String referenceNo;
    // Shipping fields
    @Column(length = 50)  private String incoterms;
    @Column(length = 50)  private String portOfLoading;
    @Column(length = 50)  private String portOfDischarge;
    @Column(length = 100) private String vesselName;
    @Column(length = 100) private String blNumber;
    @Column(length = 100) private String containerNumber;
    // Delivery fields
    @Column(length = 100) private String challanNo;
    @Column(length = 100) private String vehicleNumber;
    @Column(length = 100) private String driverName;
    @Column(length = 500) private String deliveryAddress;
    private LocalDate deliveryDate;
    private LocalDate requiredDate;
    private LocalDate validityDate;
    @Column(length = 100) private String contactPerson;
    @Column(length = 20)  private String contactNumber;
    @Column(columnDefinition = "text") private String termsAndConditions;
    @Column(columnDefinition = "text") private String remarks;

    @Builder.Default @Column(nullable = false) private boolean isDeleted = false;
    private LocalDateTime deletedAt;
    @Column(length = 100) private String deletedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(length = 100) private String createdBy;
    @Column(length = 100) private String updatedBy;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Builder.Default
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessDocumentLine> lines = new ArrayList<>();
}

// ── FILE: global/entity/BusinessDocumentLine.java ────────────────────────────
package com.hasnat.optimum.global.entity;

import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "global_business_document_lines",
    indexes = {
        @Index(name = "idx_gbdl_doc",  columnList = "document_id"),
        @Index(name = "idx_gbdl_item", columnList = "item_id"),
        @Index(name = "idx_gbdl_lot",  columnList = "inventory_lot_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessDocumentLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private BusinessDocument document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_lot_id")
    private InventoryLot inventoryLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_line_id")
    private BusinessDocumentLine sourceLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(nullable = false) private Integer lineNumber;

    @Column(length = 100) private String itemCode;
    @Column(length = 500) private String itemName;
    @Column(length = 1000) private String description;
    @Column(length = 20)  private String unitCode;

    @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity;
    @Column(precision = 18, scale = 3) private BigDecimal deliveredQty;
    @Column(precision = 18, scale = 3) private BigDecimal receivedQty;
    @Column(precision = 18, scale = 3) private BigDecimal acceptedQty;
    @Column(precision = 18, scale = 3) private BigDecimal rejectedQty;
    @Column(precision = 18, scale = 4) private BigDecimal unitPrice;
    @Column(precision = 18, scale = 2) private BigDecimal discountAmount;
    @Column(precision = 18, scale = 2) private BigDecimal taxAmount;
    @Column(precision = 18, scale = 2) private BigDecimal lineAmount;

    @Column(length = 100) private String batchNumber;
    private LocalDate expectedDate;
    @Column(length = 30)  private String qualityStatus;
    @Column(columnDefinition = "text") private String qualityRemarks;
    @Column(columnDefinition = "text") private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(length = 100) private String createdBy;
    @Column(length = 100) private String updatedBy;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    @Builder.Default
    @OneToMany(mappedBy = "documentLine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessDocumentLineLot> lots = new ArrayList<>();
}

// ── FILE: global/entity/BusinessDocumentLineLot.java ─────────────────────────
package com.hasnat.optimum.global.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_business_document_line_lots",
    indexes = {
        @Index(name = "idx_gbdll_line", columnList = "document_line_id"),
        @Index(name = "idx_gbdll_lot",  columnList = "lot_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessDocumentLineLot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_line_id", nullable = false)
    private BusinessDocumentLine documentLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private InventoryLot lot;

    @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity;
    @Column(precision = 12, scale = 3) private BigDecimal grossWeight;
    @Column(precision = 12, scale = 3) private BigDecimal netWeight;
    @Column(precision = 18, scale = 4) private BigDecimal unitCost;
    @Column(precision = 18, scale = 2) private BigDecimal totalCost;
    @Column(columnDefinition = "text") private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}

// ── FILE: global/entity/InventoryStockBalance.java ───────────────────────────
package com.hasnat.optimum.global.entity;

import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_inventory_stock_balances",
    uniqueConstraints = @UniqueConstraint(name = "uq_stock_item_wh_lot",
        columnNames = {"item_id", "warehouse_id", "lot_id"}),
    indexes = {
        @Index(name = "idx_stock_item", columnList = "item_id"),
        @Index(name = "idx_stock_wh",   columnList = "warehouse_id"),
        @Index(name = "idx_stock_lot",  columnList = "lot_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryStockBalance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity         = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 3) private BigDecimal reservedQuantity = BigDecimal.ZERO;
    @Column(precision = 12, scale = 3) private BigDecimal grossWeight;
    @Column(precision = 12, scale = 3) private BigDecimal netWeight;
    @Column(precision = 18, scale = 4) private BigDecimal averageCost;
    @Column(precision = 18, scale = 2) private BigDecimal stockValue;
    private LocalDateTime lastTransactionTime;
}

// ── FILE: global/entity/InventoryTransaction.java ────────────────────────────
package com.hasnat.optimum.global.entity;

import com.hasnat.optimum.common.enums.MovementType;
import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.organization.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "global_inventory_transactions",
    indexes = {
        @Index(name = "idx_invtx_item", columnList = "item_id"),
        @Index(name = "idx_invtx_wh",   columnList = "warehouse_id"),
        @Index(name = "idx_invtx_doc",  columnList = "business_document_id"),
        @Index(name = "idx_invtx_date", columnList = "transaction_date"),
        @Index(name = "idx_invtx_org",  columnList = "organization_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_document_id", nullable = false)
    private BusinessDocument businessDocument;

    @Column(nullable = false, length = 50) private String documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MovementType movementType;

    private LocalDate transactionDate;

    @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity;
    @Column(precision = 12, scale = 3) private BigDecimal grossWeight;
    @Column(precision = 12, scale = 3) private BigDecimal netWeight;
    @Column(precision = 18, scale = 4) private BigDecimal unitCost;
    @Column(precision = 18, scale = 2) private BigDecimal totalCost;
    @Column(precision = 18, scale = 3) private BigDecimal balanceAfter;

    @Column(length = 255) private String remarks;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
