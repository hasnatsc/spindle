// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E12  Fixed Assets                                         ║
// ║  Tables: fa_asset_categories, fa_assets, fa_depreciation_runs,          ║
// ║           fa_depreciation_run_lines, fa_asset_disposals                 ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: fixedassets/entity/AssetCategory.java ──────────────────────────────
package com.hasnat.optimum.fixedassets.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccount;
import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "fa_asset_categories",
    uniqueConstraints = @UniqueConstraint(name = "uq_fac_org_code",
        columnNames = {"organization_id", "code"}),
    indexes = {
        @Index(name = "idx_fac_org",    columnList = "organization_id"),
        @Index(name = "idx_fac_parent", columnList = "parent_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetCategory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AssetCategory parent;

    @Column(nullable = false, length = 50)  private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(columnDefinition = "text")      private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DepreciationMethod defaultDepMethod = DepreciationMethod.STRAIGHT_LINE;

    private Integer     defaultUsefulLifeYears;
    @Column(precision = 5, scale = 2) private BigDecimal defaultDepRate;
    @Column(precision = 5, scale = 2) private BigDecimal defaultResidualPct;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "gl_asset_account_id")      private ChartOfAccount glAssetAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "gl_dep_exp_account_id")    private ChartOfAccount glDepExpAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "gl_accum_dep_account_id")  private ChartOfAccount glAccumDepAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "gl_disposal_account_id")   private ChartOfAccount glDisposalAccount;

    @Builder.Default @Column(nullable = false) private boolean isActive = true;

    public enum DepreciationMethod { STRAIGHT_LINE, DECLINING_BALANCE, UNITS_OF_PRODUCTION }
}


// ── FILE: fixedassets/entity/Asset.java ──────────────────────────────────────
package com.hasnat.optimum.fixedassets.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccountSub;
import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.global.entity.BusinessDocument;
import com.hasnat.optimum.hrm.entity.Employee;
import com.hasnat.optimum.inventory.entity.Item;
import com.hasnat.optimum.organization.entity.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fa_assets",
    uniqueConstraints = @UniqueConstraint(name = "uq_fa_org_code",
        columnNames = {"organization_id", "asset_code"}),
    indexes = {
        @Index(name = "idx_fa_org",    columnList = "organization_id"),
        @Index(name = "idx_fa_cat",    columnList = "asset_category_id"),
        @Index(name = "idx_fa_status", columnList = "status"),
        @Index(name = "idx_fa_dept",   columnList = "department_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asset extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_category_id", nullable = false)
    private AssetCategory assetCategory;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id")             private Department       department;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cost_center_id")            private CostCenter       costCenter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "warehouse_id")              private Warehouse        warehouse;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "linked_item_id")            private Item             linkedItem;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "linked_grn_id")             private BusinessDocument linkedGrn;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "linked_po_id")              private BusinessDocument linkedPo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "responsible_employee_id")   private Employee         responsibleEmployee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id")               private ChartOfAccountSub supplier;

    @Column(nullable = false, length = 50)  private String assetCode;
    @Column(nullable = false, length = 200) private String assetName;
    @Column(columnDefinition = "text")      private String description;
    @Column(length = 100) private String serialNumber;
    @Column(length = 100) private String model;
    @Column(length = 100) private String manufacturer;

    @Column(nullable = false) private LocalDate acquisitionDate;
    private LocalDate         capitalisationDate;

    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal purchaseCost;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal installationCost = BigDecimal.ZERO;

    // totalCost = purchaseCost + installationCost
    // GENERATED ALWAYS AS STORED in DB — NOT mapped here.
    // Access via: @Formula("purchase_cost + installation_cost") or native query.

    @Builder.Default @Column(nullable = false, length = 3) private String currency = "BDT";
    @Builder.Default @Column(nullable = false, precision = 18, scale = 4) private BigDecimal exchangeRate = BigDecimal.ONE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetCategory.DepreciationMethod depreciationMethod
        = AssetCategory.DepreciationMethod.STRAIGHT_LINE;

    private Integer usefulLifeYears;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2) private BigDecimal depreciationRate;
    private LocalDate depreciationStartDate;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2) private BigDecimal currentBookValue;
    private LocalDate lastDepRunDate;

    @Column(length = 200) private String location;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetStatus status = AssetStatus.ACTIVE;

    @Builder.Default @Column(length = 20) private String condition = "GOOD";

    private LocalDate warrantyExpiryDate;
    @Column(length = 100) private String insurancePolicyNo;
    private LocalDate insuranceExpiryDate;
    @Column(length = 100) private String barcode;
    @Column(columnDefinition = "text") private String notes;

    public enum AssetStatus {
        ACTIVE, DISPOSED, TRANSFERRED, SOLD, WRITTEN_OFF, UNDER_MAINTENANCE
    }
}


// ── FILE: fixedassets/entity/DepreciationRun.java ────────────────────────────
package com.hasnat.optimum.fixedassets.entity;

import com.hasnat.optimum.accounts.entity.JournalEntryMaster;
import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fa_depreciation_runs",
    indexes = {
        @Index(name = "idx_fdr_org",    columnList = "organization_id"),
        @Index(name = "idx_fdr_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepreciationRun extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @Column(nullable = false) private LocalDate runDate;
    @Column(nullable = false) private LocalDate periodStart;
    @Column(nullable = false) private LocalDate periodEnd;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunType runType = RunType.MONTHLY;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status = RunStatus.DRAFT;

    @Builder.Default @Column(nullable = false) private int totalAssets = 0;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDepreciation = BigDecimal.ZERO;

    @Column(length = 100) private String postedBy;
    private LocalDateTime postedAt;

    @Builder.Default
    @OneToMany(mappedBy = "depreciationRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepreciationRunLine> lines = new ArrayList<>();

    public enum RunType   { MONTHLY, QUARTERLY, ANNUAL }
    public enum RunStatus { DRAFT, PROCESSING, COMPLETED, POSTED, REVERSED }
}


// ── FILE: fixedassets/entity/DepreciationRunLine.java ────────────────────────
package com.hasnat.optimum.fixedassets.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fa_depreciation_run_lines",
    indexes = {
        @Index(name = "idx_fdrl_run",   columnList = "depreciation_run_id"),
        @Index(name = "idx_fdrl_asset", columnList = "asset_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepreciationRunLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depreciation_run_id", nullable = false)
    private DepreciationRun depreciationRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, length = 30) private String depreciationMethod;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal openingBookValue;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal depreciationAmount;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal closingBookValue;
    @Column(precision = 5,  scale = 2) private BigDecimal rateApplied;
    @Column(precision = 14, scale = 3) private BigDecimal unitsProduced;
    @Column(columnDefinition = "text") private String notes;

    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}


// ── FILE: fixedassets/entity/AssetDisposal.java ──────────────────────────────
package com.hasnat.optimum.fixedassets.entity;

import com.hasnat.optimum.accounts.entity.JournalEntryMaster;
import com.hasnat.optimum.hrm.entity.Employee;
import com.hasnat.optimum.organization.entity.Department;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fa_asset_disposals",
    indexes = {
        @Index(name = "idx_fad_org",   columnList = "organization_id"),
        @Index(name = "idx_fad_asset", columnList = "asset_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetDisposal {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transfer_to_dept_id")
    private Department transferToDept;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transfer_to_employee_id")
    private Employee transferToEmployee;

    @Column(nullable = false) private LocalDate disposalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisposalType disposalType;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal disposalValue              = BigDecimal.ZERO;
    @Column(nullable = false, precision = 18, scale = 2)                   private BigDecimal bookValueAtDisposal;
    @Column(nullable = false, precision = 18, scale = 2)                   private BigDecimal accumulatedDepAtDisposal;
    @Column(precision = 18, scale = 2)                                     private BigDecimal gainLoss;
    @Column(length = 200) private String buyerName;
    @Column(columnDefinition = "text") private String reason;
    @Column(length = 100) private String approvedBy;
    private LocalDateTime approvedAt;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum DisposalType { SALE, WRITE_OFF, TRANSFER, SCRAP, DONATION }
}
