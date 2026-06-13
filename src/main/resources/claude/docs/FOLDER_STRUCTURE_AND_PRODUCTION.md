# Optimum ERP v2 — Folder Structure & Updated Production Entities
## Generic Edition · com.hasnat.optimum

---

## Project Folder Structure

```
optimum/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/hasnat/optimum/
│   │   │       │
│   │   │       ├── OptimumApplication.java                  ← @SpringBootApplication
│   │   │       │
│   │   │       ├── common/                                  ← Shared across all modules
│   │   │       │   ├── entity/
│   │   │       │   │   └── BaseEntity.java                  ← @MappedSuperclass (audit fields)
│   │   │       │   ├── enums/
│   │   │       │   │   ├── DocumentType.java
│   │   │       │   │   ├── DocumentStatus.java
│   │   │       │   │   ├── ApprovalStatus.java
│   │   │       │   │   ├── ItemType.java                    ← RAW_MATERIAL, FINISHED_GOOD…
│   │   │       │   │   ├── VoucherType.java
│   │   │       │   │   ├── MovementType.java
│   │   │       │   │   └── Priority.java
│   │   │       │   ├── dto/
│   │   │       │   │   ├── ApiResponse.java                 ← {success, message, obj}
│   │   │       │   │   ├── DataTableRequest.java
│   │   │       │   │   └── DataTableResponse.java
│   │   │       │   ├── exception/
│   │   │       │   │   ├── BusinessException.java
│   │   │       │   │   └── GlobalExceptionHandler.java
│   │   │       │   └── util/
│   │   │       │       ├── DocumentNumberGenerator.java     ← {PREFIX}-{YY}-{NNNNNN}
│   │   │       │       └── DateUtils.java
│   │   │       │
│   │   │       ├── config/                                  ← Spring config
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── JpaConfig.java                       ← @EnableJpaAuditing
│   │   │       │   ├── AuditorAwareImpl.java
│   │   │       │   └── ThymeleafConfig.java
│   │   │       │
│   │   │       ├── security/                                ← Auth + RBAC
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Role.java
│   │   │       │   │   ├── Permission.java
│   │   │       │   │   ├── User.java
│   │   │       │   │   ├── AppMenu.java
│   │   │       │   │   └── RoleMenuAccess.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   │   ├── UserService.java
│   │   │       │   │   ├── RoleService.java
│   │   │       │   │   └── MenuService.java
│   │   │       │   ├── controller/
│   │   │       │   │   └── api/
│   │   │       │   ├── dto/
│   │   │       │   └── init/
│   │   │       │       └── SecurityDataInitializer.java     ← Seeds roles/permissions
│   │   │       │
│   │   │       ├── organization/                            ← Org hierarchy
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Organization.java
│   │   │       │   │   ├── BusinessUnit.java
│   │   │       │   │   ├── CostCenter.java
│   │   │       │   │   ├── Warehouse.java
│   │   │       │   │   ├── Department.java
│   │   │       │   │   └── UserContext.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   └── controller/
│   │   │       │
│   │   │       ├── setup/                                   ← Reference masters
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Currency.java
│   │   │       │   │   ├── Bank.java
│   │   │       │   │   ├── DocumentSequence.java
│   │   │       │   │   ├── TermsMaster.java
│   │   │       │   │   ├── DocumentFile.java
│   │   │       │   │   └── Country.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   └── controller/
│   │   │       │
│   │   │       ├── inventory/                               ← Item masters, UOM, brands
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Item.java                        ← Generic (no fiber columns)
│   │   │       │   │   ├── ItemCategory.java                ← Generic item_type enum
│   │   │       │   │   ├── ItemUom.java
│   │   │       │   │   ├── ItemBrand.java
│   │   │       │   │   └── ItemModel.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   │   └── ItemService.java
│   │   │       │   └── controller/
│   │   │       │       └── api/ItemController.java
│   │   │       │
│   │   │       ├── global/                                  ← Lots, Documents, Stock
│   │   │       │   ├── entity/
│   │   │       │   │   ├── InventoryLot.java                ← Generic (no QC fiber cols)
│   │   │       │   │   ├── BusinessDocument.java
│   │   │       │   │   ├── BusinessDocumentLine.java
│   │   │       │   │   ├── BusinessDocumentLineLot.java
│   │   │       │   │   ├── InventoryStockBalance.java
│   │   │       │   │   └── InventoryTransaction.java
│   │   │       │   ├── repository/
│   │   │       │   └── service/
│   │   │       │       ├── BusinessDocumentService.java
│   │   │       │       └── StockService.java
│   │   │       │
│   │   │       ├── purchase/                                ← Purchase cycle
│   │   │       │   ├── service/
│   │   │       │   │   └── PurchaseService.java             ← PR→RFQ→PO→GRN→Invoice→Payment
│   │   │       │   └── controller/
│   │   │       │       └── api/PurchaseController.java
│   │   │       │
│   │   │       ├── sales/                                   ← Sales cycle
│   │   │       │   ├── service/
│   │   │       │   │   └── SalesService.java                ← Quotation→SO→Delivery→Invoice→Receipt
│   │   │       │   └── controller/
│   │   │       │       └── api/SalesController.java
│   │   │       │
│   │   │       ├── accounts/                                ← Finance / GL
│   │   │       │   ├── entity/
│   │   │       │   │   ├── ChartOfAccount.java
│   │   │       │   │   ├── ChartOfAccountSub.java           ← STI: BANK|CASH|LC|CUSTOMER|SUPPLIER…
│   │   │       │   │   ├── BankAccount.java
│   │   │       │   │   ├── CashAccount.java
│   │   │       │   │   ├── LetterOfCredit.java
│   │   │       │   │   ├── CustomerAccount.java
│   │   │       │   │   ├── SupplierAccount.java
│   │   │       │   │   ├── AccountingPeriod.java
│   │   │       │   │   ├── OpeningBalance.java
│   │   │       │   │   ├── JournalEntryMaster.java          ← + PRODUCTION_VOUCHER type
│   │   │       │   │   ├── JournalEntryLine.java            ← ★ Critical GL lines
│   │   │       │   │   ├── AccountsMapping.java
│   │   │       │   │   ├── AccountsMappingDetail.java
│   │   │       │   │   └── AccountsPolicy.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   │   ├── JournalEntryService.java         ← post() hooks budget
│   │   │       │   │   ├── AccountsMappingEngine.java
│   │   │       │   │   └── AccountsPolicyService.java
│   │   │       │   └── controller/
│   │   │       │
│   │   │       ├── approval/                                ← Approval engine
│   │   │       │   ├── entity/
│   │   │       │   │   ├── ApprovalConfig.java
│   │   │       │   │   ├── ApprovalLevel.java
│   │   │       │   │   ├── ApprovalRequest.java
│   │   │       │   │   ├── ApprovalHistory.java
│   │   │       │   │   ├── ApprovalDelegation.java
│   │   │       │   │   └── ApprovalVoucher.java
│   │   │       │   ├── repository/
│   │   │       │   └── service/
│   │   │       │       └── ApprovalService.java
│   │   │       │
│   │   │       ├── production/                              ← ★ GENERIC PRODUCTION MODULE
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Bom.java                         ← Bill of Materials master
│   │   │       │   │   ├── BomItem.java                     ← BOM lines
│   │   │       │   │   ├── Production.java                  ← Work order + cost sheet
│   │   │       │   │   ├── ProductionInput.java             ← Raw materials consumed
│   │   │       │   │   └── ProductionOutput.java            ← Finished goods produced
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   │   ├── BomService.java
│   │   │       │   │   ├── ProductionService.java           ← Core orchestration
│   │   │       │   │   └── ProductionCostService.java       ← Cost sheet calculation
│   │   │       │   └── controller/
│   │   │       │       └── api/ProductionController.java
│   │   │       │
│   │   │       ├── hrm/                                     ← Human Resources
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Designation.java
│   │   │       │   │   ├── Employee.java
│   │   │       │   │   ├── EmployeeAddress.java
│   │   │       │   │   ├── EmployeeDocument.java
│   │   │       │   │   ├── Attendance.java
│   │   │       │   │   ├── EmployeeLeave.java
│   │   │       │   │   ├── EmployeeSalary.java
│   │   │       │   │   ├── PayrollRun.java
│   │   │       │   │   ├── PayrollRunLine.java
│   │   │       │   │   └── CostCenterAllocation.java        ← ★ NEW: labor→production cost
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   │   ├── EmployeeService.java
│   │   │       │   │   ├── PayrollService.java
│   │   │       │   │   └── CostCenterAllocationService.java ← ★ NEW
│   │   │       │   └── controller/
│   │   │       │
│   │   │       ├── fixedassets/                             ← Fixed Assets
│   │   │       │   ├── entity/
│   │   │       │   │   ├── AssetCategory.java
│   │   │       │   │   ├── Asset.java
│   │   │       │   │   ├── DepreciationRun.java
│   │   │       │   │   ├── DepreciationRunLine.java
│   │   │       │   │   └── AssetDisposal.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   │   ├── AssetService.java
│   │   │       │   │   └── DepreciationService.java         ← @Scheduled monthly
│   │   │       │   └── controller/
│   │   │       │
│   │   │       ├── budget/                                  ← Budget & planning
│   │   │       │   ├── entity/
│   │   │       │   │   ├── FiscalYear.java
│   │   │       │   │   ├── BudgetHead.java
│   │   │       │   │   ├── Budget.java
│   │   │       │   │   ├── BudgetLine.java
│   │   │       │   │   ├── BudgetActual.java
│   │   │       │   │   ├── Encumbrance.java
│   │   │       │   │   └── BudgetNote.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   │   └── BudgetService.java               ← postActual() called from JournalEntryService
│   │   │       │   └── controller/
│   │   │       │
│   │   │       ├── commercial/                              ← LC & Trade
│   │   │       │   ├── entity/
│   │   │       │   │   ├── HsCode.java
│   │   │       │   │   ├── CommercialInvoice.java
│   │   │       │   │   ├── CommercialInvoiceItem.java
│   │   │       │   │   ├── DocumentTerm.java
│   │   │       │   │   ├── LcDocumentMapping.java
│   │   │       │   │   └── LcSettlement.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   └── controller/
│   │   │       │
│   │   │       ├── crm/                                     ← CRM
│   │   │       │   ├── entity/
│   │   │       │   │   ├── Lead.java
│   │   │       │   │   ├── Opportunity.java
│   │   │       │   │   ├── CrmActivity.java
│   │   │       │   │   ├── Contact.java
│   │   │       │   │   └── CustomerFeedback.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   └── controller/
│   │   │       │
│   │   │       └── notification/                            ← Notifications & Audit
│   │   │           ├── entity/
│   │   │           │   ├── Notification.java
│   │   │           │   └── AuditLog.java
│   │   │           ├── repository/
│   │   │           └── service/
│   │   │               └── NotificationService.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── db/migration/
│   │       │   ├── V1__optimum_complete_schema_v2.sql       ← 103 tables
│   │       │   └── V2__menu_permission_seed_v2.sql          ← menus + permissions
│   │       ├── static/
│   │       │   ├── css/
│   │       │   ├── js/
│   │       │   │   └── application.js                       ← secureFetch(), hsResetForm(), etc.
│   │       │   └── assets/
│   │       └── templates/
│   │           ├── layout/
│   │           │   ├── base.html                            ← Thymeleaf layout
│   │           │   ├── head.html
│   │           │   ├── topMenuHeader.html
│   │           │   ├── topMenu.html
│   │           │   └── breadcrumb.html
│   │           ├── dashboard/
│   │           ├── purchase/
│   │           ├── sales/
│   │           ├── inventory/
│   │           ├── production/
│   │           │   ├── bom/
│   │           │   │   ├── list.html
│   │           │   │   └── form.html
│   │           │   ├── orders/
│   │           │   │   ├── list.html
│   │           │   │   └── form.html
│   │           │   └── cost-sheets/
│   │           │       └── view.html
│   │           ├── accounts/
│   │           ├── hrm/
│   │           ├── fixedassets/
│   │           ├── budget/
│   │           ├── crm/
│   │           ├── commercial/
│   │           ├── reports/
│   │           └── settings/
│   │
│   └── test/
│       └── java/com/hasnat/optimum/
│           └── production/
│               └── ProductionServiceTest.java
│
└── pom.xml
```

---

## Production Module — Updated Entities (Generic)

```java
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_unit_id", nullable = false)
    private ItemUom outputUnit;

    @Column(nullable = false, length = 50)  private String bomCode;
    @Column(nullable = false, length = 200) private String bomName;
    @Builder.Default @Column(length = 20)   private String bomVersion = "1.0";

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal outputQuantity = BigDecimal.ONE;

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
    @Column(nullable = false, precision = 14, scale = 4) private BigDecimal quantity;

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
// Generic Work Order + Cost Sheet
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

@Entity
@Table(name = "prd_productions",
    uniqueConstraints = @UniqueConstraint(name = "uq_prd2_org_no",
        columnNames = {"organization_id", "production_no"}),
    indexes = {
        @Index(name = "idx_prd2_org",    columnList = "organization_id"),
        @Index(name = "idx_prd2_status", columnList = "status"),
        @Index(name = "idx_prd2_item",   columnList = "finished_item_id"),
        @Index(name = "idx_prd2_date",   columnList = "production_date")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Production extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bom_id")
    private Bom bom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finished_item_id", nullable = false)
    private Item finishedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_warehouse_id", nullable = false)
    private Warehouse outputWarehouse;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sales_order_id")
    private BusinessDocument salesOrder;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "output_unit_id", nullable = false)
    private ItemUom outputUnit;

    @Column(nullable = false, length = 50) private String productionNo;
    @Column(nullable = false) private LocalDate productionDate;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;

    // Quantities
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal plannedQuantity;
    @Builder.Default @Column(nullable = false, precision = 14, scale = 3) private BigDecimal producedQuantity  = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 14, scale = 3) private BigDecimal rejectedQuantity  = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 14, scale = 3) private BigDecimal wasteQuantity     = BigDecimal.ZERO;

    // ★ COST SHEET — direct COGS source
    // materialCost  ← auto-summed from ProductionInput.totalCost after completion
    // laborCost     ← fetched from HrmCostCenterAllocation for this cost center + month
    // overheadCost  ← manual or from overhead allocation
    // otherCost     ← packaging, utilities, freight, etc.
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal materialCost  = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal laborCost     = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal overheadCost  = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal otherCost     = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalCost     = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 4) private BigDecimal unitCost      = BigDecimal.ZERO;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private InventoryLot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_item_id")
    private BomItem bomItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private ItemUom unit;

    @Column(nullable = false) private Integer lineNumber;

    @Column(precision = 14, scale = 3) private BigDecimal plannedQuantity;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal actualQuantity;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;  // actualQuantity × unitCost

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

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Builder.Default
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal rejectedQuantity = BigDecimal.ZERO;

    // unitCost = production.unitCost (copied at completion time)
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    // totalCost = quantity × unitCost → this is the COGS value for this output
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(length = 100) private String batchNo;
    @Column(columnDefinition = "text") private String remarks;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
```

---

## Production Service — Accounting Flow

```java
// ProductionService.complete(productionId) sequence:
//
// 1. Calculate material_cost = sum(inputs.totalCost)
// 2. Look up labor_cost from hrm_cost_center_allocations
//    (cost_center_id, allocation_month = production_date YYYY-MM)
//    → proportional to produced_quantity / total_monthly_production
// 3. overhead_cost = manually entered or factory overhead allocation
// 4. total_cost = material + labor + overhead + other
// 5. unit_cost = total_cost / produced_quantity
// 6. Copy unit_cost to each ProductionOutput.unitCost
// 7. Post stock movements:
//    - For each input: global_inventory_transactions (PRODUCTION_MATERIAL_ISSUE, qty--)
//    - For each output: global_inventory_transactions (PRODUCTION_RECEIPT, qty++)
//    - Update global_inventory_stock_balances
// 8. Post journal via acc_mapping:
//    DR  WIP Inventory   = material_cost (each input)
//    CR  Raw Material    = material_cost (each input)
//
//    DR  WIP Inventory   = labor_cost
//    CR  Factory Payroll Allocation = labor_cost
//
//    DR  WIP Inventory   = overhead_cost + other_cost
//    CR  Factory Overhead Control   = overhead_cost + other_cost
//
//    DR  Finished Goods Inventory = total_cost
//    CR  WIP Inventory            = total_cost
```

---

## Item Type Enum (Updated — Generic)

```java
public enum ItemType {
    RAW_MATERIAL,    // Direct material (cotton→flour, steel, fabric, chemicals)
    SEMI_FINISHED,   // WIP (dough, cut pieces, sub-assemblies)
    FINISHED_GOOD,   // Sellable output (biscuits, garments, furniture)
    SERVICE,         // Non-physical (consulting, transport)
    SPARE_PART,      // Machine parts
    CONSUMABLE,      // Low-value non-inventory (gloves, tape, oil)
    MRO,             // Maintenance, Repair & Operations
    GENERAL,         // Uncategorized
    FIXED_ASSET      // Capitalized equipment
}
```

---

## COA Recommended Structure for Manufacturing

```
ASSETS
├── 1100  Current Assets
│   ├── 1110  Raw Material Inventory
│   ├── 1120  WIP Inventory
│   ├── 1130  Finished Goods Inventory
│   ├── 1140  Accounts Receivable
│   └── 1150  Cash & Bank
│
LIABILITIES
├── 2100  Current Liabilities
│   └── 2110  Accounts Payable
│
REVENUE
├── 4000  Sales Revenue
│
EXPENSES
├── 5000  Cost of Goods Sold (COGS)
├── 5100  Production Expenses
│   ├── 5110  Factory Payroll Allocation
│   ├── 5120  Factory Rent
│   ├── 5130  Factory Electricity
│   ├── 5140  Factory Maintenance
│   └── 5150  Factory Depreciation
│       └── → Factory Overhead Control Account
├── 5200  Operating Expenses
│   ├── 5210  Admin Salaries
│   ├── 5220  Office Rent
│   └── 5230  Marketing
```

---

## Accounting: What acc_mapping entries to create

| Mapping Code | Module | Transaction | DR Account | CR Account |
|---|---|---|---|---|
| PRD_MAT_CONSUME | PRODUCTION | MATERIAL_CONSUMPTION | WIP Inventory | Raw Material Inventory |
| PRD_LABOR_ALLOC | PRODUCTION | LABOR_ALLOCATION | WIP Inventory | Factory Payroll Allocation |
| PRD_OVERHEAD | PRODUCTION | OVERHEAD_ALLOCATION | WIP Inventory | Factory Overhead Control |
| PRD_FG_RECEIVE | PRODUCTION | FINISHED_GOODS_RECEIVE | Finished Goods Inventory | WIP Inventory |
| SALES_COGS | SALES | COGS_POST | Cost of Goods Sold | Finished Goods Inventory |
| PO_RECEIPT | PURCHASE | GOODS_RECEIPT | Raw Material Inventory | Accounts Payable |
| SALES_INVOICE | SALES | INVOICE_POST | Accounts Receivable | Sales Revenue |
