// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E13  Budget                                               ║
// ║  Tables: bgt_fiscal_years, bgt_budget_heads, bgt_budgets,               ║
// ║           bgt_budget_lines, bgt_budget_revisions,                       ║
// ║           bgt_budget_revision_lines, bgt_actuals,                       ║
// ║           bgt_encumbrances, bgt_transfers, bgt_alerts,                  ║
// ║           bgt_approval_policies, bgt_budget_notes                       ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: budget/entity/FiscalYear.java ──────────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_fiscal_years",
    uniqueConstraints = @UniqueConstraint(name = "uq_bfy_org_code",
        columnNames = {"organization_id", "year_code"}),
    indexes = {
        @Index(name = "idx_bfy_org",     columnList = "organization_id"),
        @Index(name = "idx_bfy_current", columnList = "is_current"),
        @Index(name = "idx_bfy_status",  columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiscalYear extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @Column(nullable = false, length = 20)  private String yearCode;
    @Column(nullable = false, length = 100) private String yearName;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FiscalYearStatus status = FiscalYearStatus.DRAFT;

    @Builder.Default @Column(nullable = false) private boolean isCurrent = false;

    @Column(length = 100) private String closedBy;
    private LocalDateTime closedAt;
    @Column(columnDefinition = "text") private String notes;

    public enum FiscalYearStatus { DRAFT, ACTIVE, LOCKED, CLOSED }
}


// ── FILE: budget/entity/BudgetHead.java ──────────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bgt_budget_heads",
    uniqueConstraints = @UniqueConstraint(name = "uq_bbh_org_code",
        columnNames = {"organization_id", "head_code"}),
    indexes = {
        @Index(name = "idx_bbh_org",    columnList = "organization_id"),
        @Index(name = "idx_bbh_parent", columnList = "parent_id"),
        @Index(name = "idx_bbh_type",   columnList = "head_type")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetHead extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BudgetHead parent;

    @Column(nullable = false, length = 50)  private String headCode;
    @Column(nullable = false, length = 200) private String headName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HeadType headType = HeadType.EXPENSE;

    @Column(columnDefinition = "text") private String description;
    @Builder.Default @Column(nullable = false) private boolean isActive      = true;
    @Builder.Default @Column(nullable = false) private int     displayOrder  = 0;

    public enum HeadType { REVENUE, EXPENSE, CAPEX, OPEX, PRODUCTION, HR, COMMERCIAL, OTHER }
}


// ── FILE: budget/entity/Budget.java ──────────────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.approval.entity.ApprovalRequest;
import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bgt_budgets",
    uniqueConstraints = @UniqueConstraint(name = "uq_bgt_org_no",
        columnNames = {"organization_id", "budget_no"}),
    indexes = {
        @Index(name = "idx_bgt_org",    columnList = "organization_id"),
        @Index(name = "idx_bgt_fy",     columnList = "fiscal_year_id"),
        @Index(name = "idx_bgt_status", columnList = "status"),
        @Index(name = "idx_bgt_bu",     columnList = "business_unit_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Budget extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;
    private Long businessUnitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private FiscalYear fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(nullable = false, length = 50)  private String budgetNo;
    @Column(nullable = false, length = 200) private String budgetName;
    @Column(columnDefinition = "text")      private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BudgetType budgetType = BudgetType.ANNUAL;

    @Column(nullable = false, length = 20) private String  periodType;
    @Column(nullable = false)              private LocalDate periodStart;
    @Column(nullable = false)              private LocalDate periodEnd;

    @Builder.Default @Column(nullable = false, length = 3)  private String  currency     = "BDT";
    @Builder.Default @Column(nullable = false, precision = 18, scale = 4) private BigDecimal exchangeRate = BigDecimal.ONE;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalBudgeted  = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalRevised   = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalActual    = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalCommitted = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalAvailable = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BudgetStatus status = BudgetStatus.DRAFT;

    @Column(length = 30) private String approvalStatus;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OverSpendPolicy overSpendPolicy = OverSpendPolicy.WARN;

    @Builder.Default @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal alertThresholdPct = new BigDecimal("80.00");

    @Builder.Default @Column(nullable = false) private boolean allowInterLineTransfer = false;
    @Builder.Default @Column(nullable = false) private int     version                = 1;
    @Builder.Default @Column(nullable = false) private boolean isTemplate             = false;

    public enum BudgetType    { ANNUAL, QUARTERLY, MONTHLY, PROJECT, DEPARTMENTAL, CAPEX, ROLLING }
    public enum BudgetStatus  { DRAFT, SUBMITTED, IN_APPROVAL, APPROVED, ACTIVE, LOCKED, CLOSED, REJECTED, RETURNED }
    public enum OverSpendPolicy { ALLOW, WARN, BLOCK }
}


// ── FILE: budget/entity/BudgetLine.java ──────────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.accounts.entity.ChartOfAccount;
import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.organization.entity.CostCenter;
import com.hasnat.optimum.organization.entity.Department;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bgt_budget_lines",
    indexes = {
        @Index(name = "idx_bbl_budget", columnList = "budget_id"),
        @Index(name = "idx_bbl_head",   columnList = "budget_head_id"),
        @Index(name = "idx_bbl_acct",   columnList = "account_id"),
        @Index(name = "idx_bbl_cc",     columnList = "cost_center_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetLine extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_head_id", nullable = false)
    private BudgetHead budgetHead;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id")     private ChartOfAccount account;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cost_center_id") private CostCenter     costCenter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id")  private Department     department;

    @Column(nullable = false) private Integer lineNumber;
    @Column(length = 500)     private String  description;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal originalAmount  = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal revisedAmount   = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal actualAmount    = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal committedAmount = BigDecimal.ZERO;

    // availableAmount = revisedAmount - actualAmount - committedAmount
    // GENERATED ALWAYS AS STORED in DB — NOT mapped here. Read via DTO or @Formula.

    // Monthly phasing
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal janAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal febAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal marAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal aprAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal mayAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal junAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal julAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal augAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal sepAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal octAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal novAmount = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal decAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "text") private String notes;
}


// ── FILE: budget/entity/BudgetRevision.java ──────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.approval.entity.ApprovalRequest;
import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_budget_revisions",
    uniqueConstraints = @UniqueConstraint(name = "uq_bbr_budget_rev",
        columnNames = {"budget_id", "revision_number"}),
    indexes = @Index(name = "idx_bbr_budget", columnList = "budget_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetRevision extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(nullable = false, length = 50) private String  revisionNo;
    @Column(nullable = false)              private Integer revisionNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RevisionType revisionType = RevisionType.REALLOCATION;

    @Column(nullable = false, columnDefinition = "text") private String reason;
    @Column(columnDefinition = "text")                   private String justification;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalIncrease = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal totalDecrease = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RevisionStatus status = RevisionStatus.DRAFT;

    @Column(length = 100) private String approvedBy;
    private LocalDateTime approvedAt;

    public enum RevisionType   { REALLOCATION, SUPPLEMENTARY, REDUCTION, TECHNICAL }
    public enum RevisionStatus { DRAFT, SUBMITTED, IN_APPROVAL, APPROVED, REJECTED }
}


// ── FILE: budget/entity/BudgetRevisionLine.java ──────────────────────────────
package com.hasnat.optimum.budget.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_budget_revision_lines",
    indexes = {
        @Index(name = "idx_bbrl_rev",  columnList = "revision_id"),
        @Index(name = "idx_bbrl_line", columnList = "budget_line_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetRevisionLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private BudgetRevision revision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    @Column(nullable = false, length = 1) private String direction; // '+' or '-'
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal changeAmount;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal openingAmount;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal closingAmount;
    @Column(columnDefinition = "text") private String reason;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}


// ── FILE: budget/entity/BudgetActual.java ────────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.accounts.entity.JournalEntryLine;
import com.hasnat.optimum.accounts.entity.JournalEntryMaster;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_actuals",
    indexes = {
        @Index(name = "idx_ba_budget",  columnList = "budget_id"),
        @Index(name = "idx_ba_line",    columnList = "budget_line_id"),
        @Index(name = "idx_ba_journal", columnList = "journal_entry_id"),
        @Index(name = "idx_ba_jel",     columnList = "journal_entry_line_id"),
        @Index(name = "idx_ba_date",    columnList = "transaction_date")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetActual {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    /** Voucher header — always present */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryMaster journalEntry;

    /** Individual GL line — enables exact GL-to-budget matching (★ new) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_line_id")
    private JournalEntryLine journalEntryLine;

    @Column(length = 50)  private String sourceDocumentType;
    private Long          sourceDocumentId;
    @Column(length = 100) private String sourceDocumentNo;

    @Column(nullable = false) private LocalDate transactionDate;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal debitAmount  = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal creditAmount = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal netAmount    = BigDecimal.ZERO;

    @Column(length = 500) private String narration;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}


// ── FILE: budget/entity/Encumbrance.java ─────────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.global.entity.BusinessDocument;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_encumbrances",
    indexes = {
        @Index(name = "idx_be_budget", columnList = "budget_id"),
        @Index(name = "idx_be_line",   columnList = "budget_line_id"),
        @Index(name = "idx_be_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Encumbrance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_document_id", nullable = false)
    private BusinessDocument sourceDocument;

    @Column(nullable = false, length = 50)  private String sourceDocumentType;
    @Column(nullable = false, length = 100) private String sourceDocumentNo;

    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal committedAmount;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2) private BigDecimal releasedAmount = BigDecimal.ZERO;

    // outstandingAmount = committedAmount - releasedAmount
    // GENERATED ALWAYS AS STORED in DB — NOT mapped here. Read via DTO.

    @Column(nullable = false) private LocalDate commitmentDate;
    private LocalDate expectedInvoiceDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EncumbranceStatus status = EncumbranceStatus.OPEN;

    @Column(columnDefinition = "text") private String notes;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum EncumbranceStatus { OPEN, PARTIAL, FULLY_RELEASED, CANCELLED }
}


// ── FILE: budget/entity/BudgetTransfer.java ──────────────────────────────────
package com.hasnat.optimum.budget.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_transfers",
    uniqueConstraints = @UniqueConstraint(name = "uq_bt_org_no",
        columnNames = {"organization_id", "transfer_no"}),
    indexes = @Index(name = "idx_bt_budget", columnList = "budget_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetTransfer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_line_id", nullable = false)
    private BudgetLine fromLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_line_id", nullable = false)
    private BudgetLine toLine;

    @Column(nullable = false, length = 50)  private String     transferNo;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal transferAmount;
    @Column(nullable = false, columnDefinition = "text") private String     reason;
    @Column(nullable = false)              private LocalDate  transferDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(length = 100) private String approvedBy;
    private LocalDateTime approvedAt;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum TransferStatus { PENDING, APPROVED, REJECTED }
}


// ── FILE: budget/entity/BudgetAlert.java ─────────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_alerts",
    indexes = {
        @Index(name = "idx_bal_budget", columnList = "budget_id"),
        @Index(name = "idx_bal_type",   columnList = "alert_type"),
        @Index(name = "idx_bal_user",   columnList = "notify_user_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetAlert {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "budget_line_id") private BudgetLine budgetLine;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "notify_user_id") private User       notifyUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType alertType;

    @Column(precision = 5, scale = 2) private BigDecimal thresholdPct;
    @Column(columnDefinition = "text") private String message;
    private LocalDateTime triggeredAt;

    @Builder.Default @Column(nullable = false) private boolean isResolved        = false;
    private LocalDateTime resolvedAt;
    @Column(length = 100) private String resolvedBy;

    @Builder.Default @Column(nullable = false) private boolean notificationSent = false;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum AlertType { THRESHOLD_WARNING, OVER_BUDGET, ENCUMBRANCE_EXPIRY, BUDGET_EXPIRY }
}


// ── FILE: budget/entity/BudgetApprovalPolicy.java ────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.approval.entity.ApprovalConfig;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_approval_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetApprovalPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false) private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approval_config_id")
    private ApprovalConfig approvalConfig;

    @Column(nullable = false, length = 30) private String budgetType;
    @Column(precision = 18, scale = 2)     private BigDecimal minAmount;
    @Column(precision = 18, scale = 2)     private BigDecimal maxAmount;

    @Builder.Default @Column(nullable = false) private boolean requireCfo = false;
    @Builder.Default @Column(nullable = false) private boolean requireMd  = false;
    @Builder.Default @Column(nullable = false) private boolean isActive   = true;

    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}


// ── FILE: budget/entity/BudgetNote.java ──────────────────────────────────────
package com.hasnat.optimum.budget.entity;

import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_budget_notes",
    indexes = @Index(name = "idx_bbn_budget", columnList = "budget_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetNote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, columnDefinition = "text") private String noteText;
    @Builder.Default @Column(nullable = false) private boolean isInternal = true;
    @Column(length = 500) private String attachmentUrl;
    @Column(length = 100) private String createdBy;
    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
