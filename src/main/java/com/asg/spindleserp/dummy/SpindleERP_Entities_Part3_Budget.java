// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  SpindleERP — BUDGET MODULE ENTITIES (Part 3 of 3)                     ║
// ║  12 entity classes + 3 enums + ApprovalDocumentCallback integration     ║
// ║  Package: com.asg.spindleserp                                           ║
// ║  Java 21 | Spring Boot 3.5 | JPA/Hibernate | Lombok                    ║
// ╚══════════════════════════════════════════════════════════════════════════╝
//
//  New entities (12 tables — bgt_*):
//  ─────────────────────────────────────────────────────────────────────────
//  BudgetFiscalYear        → bgt_fiscal_years
//  BudgetHead              → bgt_budget_heads
//  Budget                  → bgt_budgets          (implements ApprovalDocumentCallback)
//  BudgetLine              → bgt_budget_lines      (GENERATED available_amount)
//  BudgetRevision          → bgt_budget_revisions  (implements ApprovalDocumentCallback)
//  BudgetRevisionLine      → bgt_budget_revision_lines
//  BudgetActual            → bgt_actuals           (immutable ledger)
//  BudgetEncumbrance       → bgt_encumbrances      (GENERATED outstanding_amount)
//  BudgetTransfer          → bgt_transfers
//  BudgetAlert             → bgt_alerts
//  BudgetApprovalPolicy    → bgt_approval_policies
//  BudgetNote              → bgt_budget_notes
//
//  New enums (3):
//    BudgetType, BudgetStatus, BudgetHeadType
//
//  Updated DocumentType enum: BUDGET, BUDGET_REVISION, BUDGET_TRANSFER
//  Updated ModuleType enum  : BUDGET_MANAGEMENT
// ─────────────────────────────────────────────────────────────────────────

// ════════════════════════════════════════════════════════════════════════════
// ENUMS
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetType.java
package com.asg.spindleserp.budget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetType {
    ANNUAL("BGT",    "Annual Budget"),
    QUARTERLY("QBGT","Quarterly Budget"),
    MONTHLY("MBGT",  "Monthly Budget"),
    PROJECT("PBGT",  "Project Budget"),
    DEPARTMENTAL("DBGT","Departmental Budget"),
    CAPEX("CBGT",    "Capital Expenditure Budget"),
    ROLLING("RBGT",  "Rolling Budget");

    private final String code;
    private final String displayName;
}

// FILE: com/asg/spindleserp/budget/BudgetStatus.java
package com.asg.spindleserp.budget;

public enum BudgetStatus {
    DRAFT,
    SUBMITTED,
    IN_APPROVAL,
    APPROVED,
    ACTIVE,
    LOCKED,
    CLOSED,
    REJECTED,
    RETURNED
}

// FILE: com/asg/spindleserp/budget/BudgetHeadType.java
package com.asg.spindleserp.budget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BudgetHeadType {
    REVENUE("Revenue"),
    EXPENSE("Expense"),
    CAPEX("Capital Expenditure"),
    OPEX("Operating Expenditure"),
    PRODUCTION("Production Cost"),
    HR("Human Resources"),
    COMMERCIAL("Commercial / Trade"),
    OTHER("Other");

    private final String displayName;
}

// FILE: com/asg/spindleserp/budget/OverSpendPolicy.java
package com.asg.spindleserp.budget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OverSpendPolicy {
    ALLOW("Allow — track only; no restriction on over-spend"),
    WARN("Warn  — alert raised but transaction proceeds"),
    BLOCK("Block — transaction prevented when budget exhausted");

    private final String description;
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 1 — BudgetFiscalYear
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetFiscalYear.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bgt_fiscal_years",
    uniqueConstraints = @UniqueConstraint(name = "uk_fy_org_code",
        columnNames = {"organization_id", "year_code"}),
    indexes = {
        @Index(name = "idx_bgt_fy_org",     columnList = "organization_id"),
        @Index(name = "idx_bgt_fy_current", columnList = "organization_id,is_current")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetFiscalYear extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_code",  nullable = false, length = 20)  private String yearCode;   // FY2025-26
    @Column(name = "year_name",  nullable = false, length = 100) private String yearName;   // Financial Year 2025-2026
    @Column(name = "start_date", nullable = false)               private LocalDate startDate;
    @Column(name = "end_date",   nullable = false)               private LocalDate endDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";
    // DRAFT | ACTIVE | LOCKED | CLOSED

    @Builder.Default
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;

    @Column(name = "closed_by",  length = 100) private String closedBy;
    @Column(name = "closed_at")                private LocalDateTime closedAt;
    @Column(columnDefinition = "TEXT")         private String notes;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    @Builder.Default
    @OneToMany(mappedBy = "fiscalYear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Budget> budgets = new ArrayList<>();

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isOpen()   { return "ACTIVE".equals(status); }
    public boolean isLocked() { return "LOCKED".equals(status) || "CLOSED".equals(status); }
    public boolean covers(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 2 — BudgetHead
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetHead.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bgt_budget_heads",
    uniqueConstraints = @UniqueConstraint(name = "uk_bgt_head_org_code",
        columnNames = {"organization_id", "head_code"}),
    indexes = {
        @Index(name = "idx_bgt_head_org",    columnList = "organization_id"),
        @Index(name = "idx_bgt_head_parent", columnList = "parent_id"),
        @Index(name = "idx_bgt_head_type",   columnList = "head_type")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetHead extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BudgetHead parent;

    @Column(name = "head_code", nullable = false, length = 50)  private String headCode;
    @Column(name = "head_name", nullable = false, length = 200) private String headName;

    @Enumerated(EnumType.STRING)
    @Column(name = "head_type", nullable = false, length = 30)
    @Builder.Default
    private BudgetHeadType headType = BudgetHeadType.EXPENSE;

    @Column(columnDefinition = "TEXT") private String description;
    @Builder.Default @Column(name = "is_active",     nullable = false) private Boolean isActive     = true;
    @Builder.Default @Column(name = "display_order", nullable = false) private Integer displayOrder = 0;
    @Column(name = "created_by", length = 100) private String createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetHead> children = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "budgetHead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BudgetLine> budgetLines = new ArrayList<>();

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isLeaf()    { return children == null || children.isEmpty(); }
    public boolean isRevenue() { return BudgetHeadType.REVENUE.equals(headType); }
    public boolean isCapex()   { return BudgetHeadType.CAPEX.equals(headType); }

    public String getFullPath() {
        if (parent != null) return parent.getFullPath() + " > " + headName;
        return headName;
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 3 — Budget  (header; implements ApprovalDocumentCallback)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/Budget.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.approval.*;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Budget — header entity.
 *
 * Approval pattern:
 *  - Implements ApprovalDocumentCallback
 *  - Constructor registers itself with callbackRegistry
 *  - @Lazy ApprovalService to break circular dependency
 *  - NEVER use @RequiredArgsConstructor — Lombok ignores @Lazy on final fields
 */
@Entity
@Table(name = "bgt_budgets",
    uniqueConstraints = @UniqueConstraint(name = "uk_bgt_org_no",
        columnNames = {"organization_id", "budget_no"}),
    indexes = {
        @Index(name = "idx_bgt_org",    columnList = "organization_id"),
        @Index(name = "idx_bgt_fy",     columnList = "fiscal_year_id"),
        @Index(name = "idx_bgt_bu",     columnList = "business_unit_id"),
        @Index(name = "idx_bgt_status", columnList = "status"),
        @Index(name = "idx_bgt_type",   columnList = "budget_type"),
        @Index(name = "idx_bgt_apr",    columnList = "approval_request_id")
    })
@Getter @Setter
@NoArgsConstructor
public class Budget extends BaseOrgEntity implements ApprovalDocumentCallback {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relationships ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiscal_year_id", nullable = false)
    private BudgetFiscalYear fiscalYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_unit_id")
    private BusinessUnit businessUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    // ── Identity ──────────────────────────────────────────────────
    @Column(name = "budget_no",   nullable = false, length = 50)  private String budgetNo;
    @Column(name = "budget_name", nullable = false, length = 200) private String budgetName;
    @Column(columnDefinition = "TEXT")                            private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type", nullable = false, length = 30)
    private BudgetType budgetType = BudgetType.ANNUAL;

    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType = "ANNUAL";  // ANNUAL|Q1|Q2|Q3|Q4|JAN..DEC|PROJECT

    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end",   nullable = false) private LocalDate periodEnd;

    @Column(length = 3) private String currency = "BDT";
    @Column(name = "exchange_rate", precision = 18, scale = 4) private BigDecimal exchangeRate = BigDecimal.ONE;

    // ── Running totals (maintained by fn_post_budget_actual) ──────
    @Column(name = "total_budgeted",  nullable = false, precision = 18, scale = 2) private BigDecimal totalBudgeted  = BigDecimal.ZERO;
    @Column(name = "total_revised",   nullable = false, precision = 18, scale = 2) private BigDecimal totalRevised   = BigDecimal.ZERO;
    @Column(name = "total_actual",    nullable = false, precision = 18, scale = 2) private BigDecimal totalActual    = BigDecimal.ZERO;
    @Column(name = "total_committed", nullable = false, precision = 18, scale = 2) private BigDecimal totalCommitted = BigDecimal.ZERO;
    @Column(name = "total_available", nullable = false, precision = 18, scale = 2) private BigDecimal totalAvailable = BigDecimal.ZERO;

    // ── Status ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BudgetStatus status = BudgetStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    // ── Policy ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "over_spend_policy", nullable = false, length = 20)
    private OverSpendPolicy overSpendPolicy = OverSpendPolicy.WARN;

    @Column(name = "alert_threshold_pct", precision = 5, scale = 2)
    private BigDecimal alertThresholdPct = new BigDecimal("80");

    @Column(name = "allow_inter_line_transfer", nullable = false)
    private Boolean allowInterLineTransfer = false;

    // ── Meta ──────────────────────────────────────────────────────
    @Column(nullable = false) private Integer version = 1;
    @Column(name = "is_template", nullable = false) private Boolean isTemplate = false;

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    // ── Collections ───────────────────────────────────────────────
    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<BudgetLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("revisionNumber ASC")
    private List<BudgetRevision> revisions = new ArrayList<>();

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<BudgetNote> notes = new ArrayList<>();

    // ── Approval callback fields (injected manually) ──────────────
    @Transient private ApprovalCallbackRegistry callbackRegistry;
    @Transient private ApprovalService           approvalService;

    /**
     * Explicit constructor for @Lazy injection.
     * CRITICAL: Never use @RequiredArgsConstructor — Lombok ignores @Lazy.
     */
    public Budget(@org.springframework.context.annotation.Lazy ApprovalService approvalService,
                  ApprovalCallbackRegistry callbackRegistry) {
        this.approvalService    = approvalService;
        this.callbackRegistry   = callbackRegistry;
        callbackRegistry.register(this);
    }

    // ── ApprovalDocumentCallback implementation ───────────────────

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.BUDGET;
    }

    @Override
    public String getDocumentNumber() {
        return budgetNo;
    }

    @Override
    public BigDecimal getDocumentAmount() {
        return totalRevised != null ? totalRevised : totalBudgeted;
    }

    @Override
    public String getDocumentSummary() {
        return String.format("Budget: %s | %s | Period: %s to %s | Amount: %s %s",
            budgetNo, budgetName, periodStart, periodEnd, currency, totalRevised);
    }

    /** Called by ApprovalService when all levels approved. */
    @Override
    public void onApproved(Long referenceId) {
        this.status         = BudgetStatus.ACTIVE;
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    /** Called when any approver rejects. */
    @Override
    public void onRejected(Long referenceId) {
        this.status         = BudgetStatus.REJECTED;
        this.approvalStatus = ApprovalStatus.REJECTED;
    }

    /** Called when returned for correction. */
    @Override
    public void onReturned(Long referenceId) {
        this.status         = BudgetStatus.RETURNED;
        this.approvalStatus = ApprovalStatus.RETURNED;
    }

    @Override
    public void onRecalled(Long referenceId) {
        this.status         = BudgetStatus.DRAFT;
        this.approvalStatus = ApprovalStatus.DRAFT;
    }

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isDraft()       { return BudgetStatus.DRAFT.equals(status); }
    public boolean isActive()      { return BudgetStatus.ACTIVE.equals(status); }
    public boolean isEditable()    { return isDraft() || BudgetStatus.RETURNED.equals(status); }
    public boolean isLocked()      { return BudgetStatus.LOCKED.equals(status) || BudgetStatus.CLOSED.equals(status); }

    /** Submit for approval. Re-activates existing ApprovalRequest if RETURNED/REJECTED. */
    public void submit(String submittedBy) {
        if (!isEditable()) throw new IllegalStateException("Budget " + budgetNo + " is not editable");
        this.status         = BudgetStatus.SUBMITTED;
        this.approvalStatus = ApprovalStatus.SUBMITTED;
        this.updatedBy      = submittedBy;
    }

    /** Recalculate header totals from all lines. */
    public void recalculateTotals() {
        this.totalBudgeted  = lines.stream().map(BudgetLine::getOriginalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalRevised   = lines.stream().map(BudgetLine::getRevisedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalActual    = lines.stream().map(BudgetLine::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalCommitted = lines.stream().map(BudgetLine::getCommittedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAvailable = totalRevised.subtract(totalActual).subtract(totalCommitted);
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 4 — BudgetLine
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetLine.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.hrm.setup.Department;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bgt_budget_lines",
    uniqueConstraints = @UniqueConstraint(name = "uk_bgt_line",
        columnNames = {"budget_id", "budget_head_id", "account_id", "cost_center_id", "department_id"}),
    indexes = {
        @Index(name = "idx_bgtl_budget",  columnList = "budget_id"),
        @Index(name = "idx_bgtl_head",    columnList = "budget_head_id"),
        @Index(name = "idx_bgtl_account", columnList = "account_id"),
        @Index(name = "idx_bgtl_cc",      columnList = "cost_center_id"),
        @Index(name = "idx_bgtl_dept",    columnList = "department_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetLine implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_head_id", nullable = false)
    private BudgetHead budgetHead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "line_number", nullable = false) private Integer lineNumber;
    @Column(length = 500)                           private String  description;

    // ── Budget amounts ────────────────────────────────────────────
    @Builder.Default @Column(name = "original_amount", nullable = false, precision = 18, scale = 2) private BigDecimal originalAmount  = BigDecimal.ZERO;
    @Builder.Default @Column(name = "revised_amount",  nullable = false, precision = 18, scale = 2) private BigDecimal revisedAmount   = BigDecimal.ZERO;
    @Builder.Default @Column(name = "actual_amount",   nullable = false, precision = 18, scale = 2) private BigDecimal actualAmount    = BigDecimal.ZERO;
    @Builder.Default @Column(name = "committed_amount",nullable = false, precision = 18, scale = 2) private BigDecimal committedAmount = BigDecimal.ZERO;

    /**
     * availableAmount = revisedAmount - actualAmount - committedAmount
     * Stored as a GENERATED ALWAYS AS column in PostgreSQL.
     * In Java it is computed on @PostLoad and @PrePersist.
     */
    @Column(name = "available_amount", precision = 18, scale = 2)
    private BigDecimal availableAmount;

    // ── Monthly phasing (12 months) ───────────────────────────────
    @Builder.Default @Column(name = "jan_amount", precision = 18, scale = 2) private BigDecimal janAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "feb_amount", precision = 18, scale = 2) private BigDecimal febAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "mar_amount", precision = 18, scale = 2) private BigDecimal marAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "apr_amount", precision = 18, scale = 2) private BigDecimal aprAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "may_amount", precision = 18, scale = 2) private BigDecimal mayAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "jun_amount", precision = 18, scale = 2) private BigDecimal junAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "jul_amount", precision = 18, scale = 2) private BigDecimal julAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "aug_amount", precision = 18, scale = 2) private BigDecimal augAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "sep_amount", precision = 18, scale = 2) private BigDecimal sepAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "oct_amount", precision = 18, scale = 2) private BigDecimal octAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "nov_amount", precision = 18, scale = 2) private BigDecimal novAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "dec_amount", precision = 18, scale = 2) private BigDecimal decAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT") private String notes;

    @OneToMany(mappedBy = "budgetLine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetActual> actuals = new ArrayList<>();

    @OneToMany(mappedBy = "budgetLine", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BudgetEncumbrance> encumbrances = new ArrayList<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;

    // ── Computed helpers ──────────────────────────────────────────
    @PostLoad
    @PrePersist
    @PreUpdate
    public void computeAvailable() {
        BigDecimal actual    = actualAmount    != null ? actualAmount    : BigDecimal.ZERO;
        BigDecimal committed = committedAmount != null ? committedAmount : BigDecimal.ZERO;
        BigDecimal revised   = revisedAmount   != null ? revisedAmount   : BigDecimal.ZERO;
        this.availableAmount = revised.subtract(actual).subtract(committed);
    }

    public boolean isOverBudget() {
        return availableAmount != null && availableAmount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getUtilisationPct() {
        if (revisedAmount == null || revisedAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return actualAmount.add(committedAmount)
            .multiply(new BigDecimal("100"))
            .divide(revisedAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getMonthlyPhaseTotal() {
        BigDecimal[] months = { janAmount, febAmount, marAmount, aprAmount, mayAmount, junAmount,
                                julAmount, augAmount, sepAmount, octAmount, novAmount, decAmount };
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal m : months) if (m != null) total = total.add(m);
        return total;
    }

    /** Returns the phased budget for a given month (1=Jan … 12=Dec). */
    public BigDecimal getAmountForMonth(int month) {
        return switch (month) {
            case 1  -> janAmount; case 2  -> febAmount; case 3  -> marAmount;
            case 4  -> aprAmount; case 5  -> mayAmount; case 6  -> junAmount;
            case 7  -> julAmount; case 8  -> augAmount; case 9  -> sepAmount;
            case 10 -> octAmount; case 11 -> novAmount; case 12 -> decAmount;
            default -> BigDecimal.ZERO;
        };
    }

    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 5 — BudgetRevision  (implements ApprovalDocumentCallback)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetRevision.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.approval.*;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bgt_budget_revisions",
    uniqueConstraints = @UniqueConstraint(name = "uk_bgt_rev_budget_no",
        columnNames = {"budget_id", "revision_number"}),
    indexes = {
        @Index(name = "idx_bgt_rev_budget", columnList = "budget_id"),
        @Index(name = "idx_bgt_rev_status", columnList = "status")
    })
@Getter @Setter
@NoArgsConstructor
public class BudgetRevision implements Serializable, ApprovalDocumentCallback {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(name = "revision_no",     nullable = false, length = 50) private String revisionNo;
    @Column(name = "revision_number", nullable = false)              private Integer revisionNumber;
    @Column(name = "revision_type",   nullable = false, length = 30)
    private String revisionType = "REALLOCATION";
    // REALLOCATION | SUPPLEMENTARY | REDUCTION | TECHNICAL

    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Column(columnDefinition = "TEXT")                   private String justification;

    @Column(name = "total_increase", nullable = false, precision = 18, scale = 2) private BigDecimal totalIncrease = BigDecimal.ZERO;
    @Column(name = "total_decrease", nullable = false, precision = 18, scale = 2) private BigDecimal totalDecrease = BigDecimal.ZERO;

    @Column(nullable = false, length = 30) private String status = "DRAFT";
    // DRAFT | SUBMITTED | IN_APPROVAL | APPROVED | REJECTED

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    @Column(name = "approved_by",  length = 100) private String approvedBy;
    @Column(name = "approved_at")                private LocalDateTime approvedAt;
    @Column(name = "created_by",   length = 100) private String createdBy;
    @Column(name = "updated_by",   length = 100) private String updatedBy;

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetRevisionLine> lines = new ArrayList<>();

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;

    @Transient private ApprovalCallbackRegistry callbackRegistry;
    @Transient private ApprovalService           approvalService;

    public BudgetRevision(@org.springframework.context.annotation.Lazy ApprovalService approvalService,
                          ApprovalCallbackRegistry callbackRegistry) {
        this.approvalService  = approvalService;
        this.callbackRegistry = callbackRegistry;
        callbackRegistry.register(this);
    }

    @Override public DocumentType getDocumentType()  { return DocumentType.BUDGET_REVISION; }
    @Override public String       getDocumentNumber(){ return revisionNo; }
    @Override public BigDecimal   getDocumentAmount(){ return totalIncrease.subtract(totalDecrease); }
    @Override public String       getDocumentSummary(){
        return String.format("Revision %d for Budget %s | %s | Net change: %s",
            revisionNumber, budget != null ? budget.getBudgetNo() : "?", reason,
            totalIncrease.subtract(totalDecrease));
    }

    @Override
    public void onApproved(Long referenceId) {
        this.status         = "APPROVED";
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedAt     = LocalDateTime.now();
        // BudgetService will apply the revision lines to the budget lines
    }
    @Override public void onRejected(Long referenceId) { this.status = "REJECTED"; this.approvalStatus = ApprovalStatus.REJECTED; }
    @Override public void onReturned(Long referenceId) { this.status = "DRAFT";    this.approvalStatus = ApprovalStatus.RETURNED; }
    @Override public void onRecalled(Long referenceId) { this.status = "DRAFT";    this.approvalStatus = ApprovalStatus.DRAFT; }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 6 — BudgetRevisionLine
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetRevisionLine.java
package com.asg.spindleserp.budget;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_budget_revision_lines",
    indexes = @Index(name = "idx_bgt_revl_rev", columnList = "revision_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetRevisionLine implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private BudgetRevision revision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    /**
     * direction: '+' = increase | '-' = decrease
     */
    @Column(nullable = false, length = 1)
    private String direction;

    @Column(name = "change_amount",  nullable = false, precision = 18, scale = 2) private BigDecimal changeAmount;
    @Column(name = "opening_amount", nullable = false, precision = 18, scale = 2) private BigDecimal openingAmount;  // before
    @Column(name = "closing_amount", nullable = false, precision = 18, scale = 2) private BigDecimal closingAmount;  // after
    @Column(columnDefinition = "TEXT") private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Helper ────────────────────────────────────────────────────
    public boolean isIncrease() { return "+".equals(direction); }
    public boolean isDecrease() { return "-".equals(direction); }

    public BigDecimal getSignedAmount() {
        return isIncrease() ? changeAmount : changeAmount.negate();
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 7 — BudgetActual  (immutable ledger row)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetActual.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.accounts.journal.JournalEntryLine;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BudgetActual — immutable.
 * One row created per GL journal posting that touches a budget line.
 * Never update or delete; running totals on BudgetLine are the live balance.
 */
@Entity
@Table(name = "bgt_actuals",
    indexes = {
        @Index(name = "idx_bgt_act_budget", columnList = "budget_id"),
        @Index(name = "idx_bgt_act_line",   columnList = "budget_line_id"),
        @Index(name = "idx_bgt_act_je",     columnList = "journal_entry_id"),
        @Index(name = "idx_bgt_act_date",   columnList = "transaction_date"),
        @Index(name = "idx_bgt_act_src",    columnList = "source_document_type,source_document_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetActual implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_line_id")
    private JournalEntryLine journalEntryLine;

    // Polymorphic source reference (DocumentType + id of global_business_documents)
    @Column(name = "source_document_type", length = 50)  private String sourceDocumentType;
    @Column(name = "source_document_id")                 private Long   sourceDocumentId;
    @Column(name = "source_document_no",   length = 100) private String sourceDocumentNo;

    @Column(name = "transaction_date", nullable = false) private LocalDate transactionDate;

    @Builder.Default @Column(name = "debit_amount",  nullable = false, precision = 18, scale = 2) private BigDecimal debitAmount  = BigDecimal.ZERO;
    @Builder.Default @Column(name = "credit_amount", nullable = false, precision = 18, scale = 2) private BigDecimal creditAmount = BigDecimal.ZERO;
    @Builder.Default @Column(name = "net_amount",    nullable = false, precision = 18, scale = 2) private BigDecimal netAmount    = BigDecimal.ZERO;
    // netAmount = debitAmount - creditAmount; positive = expense; negative = income

    @Column(length = 500) private String narration;
    @Column(name = "created_by", length = 100) private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 8 — BudgetEncumbrance
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetEncumbrance.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.global.documents.BusinessDocument;
import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BudgetEncumbrance — created when a PO is approved.
 * Released (fully or partially) when the purchase invoice is posted.
 * outstandingAmount = committedAmount − releasedAmount (GENERATED in DB).
 */
@Entity
@Table(name = "bgt_encumbrances",
    indexes = {
        @Index(name = "idx_bgt_enc_budget", columnList = "budget_id"),
        @Index(name = "idx_bgt_enc_line",   columnList = "budget_line_id"),
        @Index(name = "idx_bgt_enc_doc",    columnList = "source_document_id"),
        @Index(name = "idx_bgt_enc_status", columnList = "status")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetEncumbrance implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    // Source document — typically a PURCHASE_ORDER
    @Column(name = "source_document_type", nullable = false, length = 50)  private String sourceDocumentType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_document_id", nullable = false)
    private BusinessDocument sourceDocument;
    @Column(name = "source_document_no",   nullable = false, length = 100) private String sourceDocumentNo;

    @Column(name = "committed_amount", nullable = false, precision = 18, scale = 2) private BigDecimal committedAmount;
    @Builder.Default
    @Column(name = "released_amount",  nullable = false, precision = 18, scale = 2) private BigDecimal releasedAmount  = BigDecimal.ZERO;

    /**
     * outstandingAmount = committedAmount − releasedAmount.
     * Stored as GENERATED column in DB; computed in Java on @PostLoad.
     */
    @Column(name = "outstanding_amount", precision = 18, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(name = "commitment_date",       nullable = false) private LocalDate commitmentDate;
    @Column(name = "expected_invoice_date")                   private LocalDate expectedInvoiceDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";
    // OPEN | PARTIAL | FULLY_RELEASED | CANCELLED

    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "created_by", length = 100) private String createdBy;

    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(name = "updated_at")                     private LocalDateTime updatedAt;

    @PostLoad
    @PrePersist
    @PreUpdate
    public void computeOutstanding() {
        BigDecimal committed = committedAmount != null ? committedAmount : BigDecimal.ZERO;
        BigDecimal released  = releasedAmount  != null ? releasedAmount  : BigDecimal.ZERO;
        this.outstandingAmount = committed.subtract(released);
    }

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isOpen()         { return "OPEN".equals(status); }
    public boolean isFullyReleased(){ return "FULLY_RELEASED".equals(status); }
    public boolean isCancelled()    { return "CANCELLED".equals(status); }

    public boolean isOverdue() {
        return expectedInvoiceDate != null
            && expectedInvoiceDate.isBefore(LocalDate.now())
            && isOpen();
    }

    /**
     * Release an amount (called by PurchaseInvoiceService.onApproved()).
     * @return true if fully released after this operation.
     */
    public boolean release(BigDecimal amount) {
        this.releasedAmount = this.releasedAmount.add(amount);
        if (this.releasedAmount.compareTo(this.committedAmount) >= 0) {
            this.status = "FULLY_RELEASED";
            return true;
        } else {
            this.status = "PARTIAL";
            return false;
        }
    }

    public void cancel() {
        this.status         = "CANCELLED";
        this.releasedAmount = this.committedAmount;
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 9 — BudgetTransfer
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetTransfer.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_transfers",
    uniqueConstraints = @UniqueConstraint(name = "uk_bgt_xfr_org_no",
        columnNames = {"organization_id", "transfer_no"}),
    indexes = {
        @Index(name = "idx_bgt_xfr_budget", columnList = "budget_id"),
        @Index(name = "idx_bgt_xfr_from",   columnList = "from_line_id"),
        @Index(name = "idx_bgt_xfr_to",     columnList = "to_line_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetTransfer implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(name = "transfer_no", nullable = false, length = 50) private String transferNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_line_id", nullable = false)
    private BudgetLine fromLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_line_id", nullable = false)
    private BudgetLine toLine;

    @Column(name = "transfer_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal transferAmount;

    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Column(name = "transfer_date", nullable = false)    private LocalDate transferDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
    // PENDING | APPROVED | REJECTED

    @Column(name = "approved_by",  length = 100) private String approvedBy;
    @Column(name = "approved_at")                private LocalDateTime approvedAt;
    @Column(name = "created_by",   length = 100) private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Helper ────────────────────────────────────────────────────
    public boolean isApproved() { return "APPROVED".equals(status); }

    /**
     * Apply the transfer to the two budget lines.
     * Called by BudgetService.applyTransfer() after approval.
     */
    public void apply() {
        if (!isApproved()) throw new IllegalStateException("Transfer not approved");
        // Deduct from source
        fromLine.setRevisedAmount(fromLine.getRevisedAmount().subtract(transferAmount));
        fromLine.computeAvailable();
        // Add to destination
        toLine.setRevisedAmount(toLine.getRevisedAmount().add(transferAmount));
        toLine.computeAvailable();
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 10 — BudgetAlert
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetAlert.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.security.Organization;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_alerts",
    indexes = {
        @Index(name = "idx_bgt_alert_budget", columnList = "budget_id"),
        @Index(name = "idx_bgt_alert_user",   columnList = "notify_user_id"),
        @Index(name = "idx_bgt_alert_unsent", columnList = "notification_sent,triggered_at")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetAlert implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_line_id")
    private BudgetLine budgetLine;   // NULL = entire budget

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notify_user_id")
    private User notifyUser;

    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType;
    // THRESHOLD_WARNING | OVER_BUDGET | ENCUMBRANCE_EXPIRY | BUDGET_EXPIRY

    @Column(name = "threshold_pct", precision = 5, scale = 2)
    private BigDecimal thresholdPct;

    @Column(columnDefinition = "TEXT") private String message;

    @Column(name = "triggered_at")   private LocalDateTime triggeredAt;
    @Builder.Default @Column(name = "is_resolved",        nullable = false) private Boolean isResolved        = false;
    @Column(name = "resolved_at")    private LocalDateTime resolvedAt;
    @Column(name = "resolved_by",  length = 100) private String resolvedBy;
    @Builder.Default @Column(name = "notification_sent",  nullable = false) private Boolean notificationSent  = false;
    @Column(name = "sent_at")        private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isOverBudget()       { return "OVER_BUDGET".equals(alertType); }
    public boolean isThresholdWarning() { return "THRESHOLD_WARNING".equals(alertType); }
    public boolean isPending()          { return !isResolved && !notificationSent; }

    public void markSent() {
        this.notificationSent = true;
        this.sentAt           = LocalDateTime.now();
    }

    public void resolve(String resolvedByUser) {
        this.isResolved  = true;
        this.resolvedAt  = LocalDateTime.now();
        this.resolvedBy  = resolvedByUser;
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 11 — BudgetApprovalPolicy
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetApprovalPolicy.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.approval.ApprovalConfig;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bgt_approval_policies",
    indexes = @Index(name = "idx_bgt_pol_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetApprovalPolicy extends BaseOrgEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_config_id", nullable = false)
    private ApprovalConfig approvalConfig;

    @Column(name = "policy_name",   nullable = false, length = 100) private String policyName;
    @Column(name = "budget_type",   length = 30)                    private String budgetType; // NULL = applies to all

    @Builder.Default
    @Column(name = "min_amount", precision = 18, scale = 2)
    private BigDecimal minAmount = BigDecimal.ZERO;

    @Column(name = "max_amount", precision = 18, scale = 2)
    private BigDecimal maxAmount; // NULL = no upper limit

    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "created_by", length = 100) private String createdBy;
    @Column(name = "updated_by", length = 100) private String updatedBy;

    // ── Helper ────────────────────────────────────────────────────
    public boolean appliesTo(String type, BigDecimal amount) {
        if (budgetType != null && !budgetType.equals(type)) return false;
        if (amount.compareTo(minAmount) < 0) return false;
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) return false;
        return true;
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENTITY 12 — BudgetNote
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/budget/BudgetNote.java
package com.asg.spindleserp.budget;

import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_budget_notes",
    indexes = @Index(name = "idx_bgt_notes_budget", columnList = "budget_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetNote implements Serializable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noted_by_user_id")
    private User notedByUser;

    @Column(name = "note_type", nullable = false, length = 20)
    @Builder.Default
    private String noteType = "COMMENT";
    // COMMENT | QUERY | ACTION_ITEM | APPROVAL_NOTE

    @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
    private String noteText;

    @Builder.Default @Column(name = "is_internal", nullable = false) private Boolean isInternal = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

// ════════════════════════════════════════════════════════════════════════════
// UPDATED ENUMS  (add these values to existing enum files)
// ════════════════════════════════════════════════════════════════════════════

// FILE: com/asg/spindleserp/approval/DocumentType.java  (ADD these 3 values)
//
//   /* ============================================================
//      BUDGET
//      ============================================================ */
//   BUDGET("BGT",           "Budget"),
//   BUDGET_REVISION("BGTR", "Budget Revision"),
//   BUDGET_TRANSFER("BGTT", "Budget Transfer");
//
// ─────────────────────────────────────────────────────────────────────────
// CRITICAL: DocumentType enum must be kept complete.
// Missing values cause ApprovalCallbackRegistry routing failure at runtime.
// ─────────────────────────────────────────────────────────────────────────

// FILE: com/asg/spindleserp/accounts/setup/ModuleType.java  (ADD this value)
//
//   BUDGET_MANAGEMENT("Budget Management"),

// ════════════════════════════════════════════════════════════════════════════
// SERVICE INTEGRATION NOTES
// ════════════════════════════════════════════════════════════════════════════
/*
  BudgetService  (required new service)
  ──────────────────────────────────────────────────────────────────────────
  createBudget(BudgetDTO) → generates BGT-YYMM-NNNN via CodeGeneratorService
  submitBudget(id)        → budget.submit(); creates ApprovalRequest
  applyRevision(revId)    → applies BudgetRevisionLines to BudgetLines;
                            increments budget.version
  applyTransfer(xfrId)    → calls budgetTransfer.apply()
  checkBudgetAvailability(lineId, amount) → throws BudgetExceededException
                            when overSpendPolicy = BLOCK and amount > available
  postActual(...)         → calls fn_post_budget_actual() via JDBC
  createEncumbrance(...)  → called from PurchaseOrderService.onApproved()
                            via fn_create_encumbrance() JDBC
  releaseEncumbrance(...) → called from PurchaseInvoiceService.onApproved()
                            via fn_release_encumbrance() JDBC

  ApprovalCallbackRegistry integrations (register in each service constructor):
  ──────────────────────────────────────────────────────────────────────────
  BudgetService         → registers Budget          callback (DocumentType.BUDGET)
  BudgetRevisionService → registers BudgetRevision  callback (DocumentType.BUDGET_REVISION)

  PurchaseOrderService.onApproved():
  ──────────────────────────────────────────────────────────────────────────
  After super.onApproved():
    budgetService.createEncumbrance(
        budgetLineId,       // from PO line metadata or default line lookup
        po.getId(),
        "PURCHASE_ORDER",
        po.getDocumentNo(),
        po.getTotalAmount(),
        po.getDocumentDate(),
        po.getRequiredDate()
    );

  PurchaseInvoiceService.onApproved():
  ──────────────────────────────────────────────────────────────────────────
  After super.onApproved():
    budgetService.releaseEncumbrance(encumbranceId, invoiceAmount);

  GL posting integration (JournalEntryService.post()):
  ──────────────────────────────────────────────────────────────────────────
  After journal entry is marked POSTED:
    for each JournalEntryLine line :
        BudgetLine bLine = budgetService.findMatchingLine(
            line.getAccount(), line.getCostCenter(), fiscalYearId);
        if (bLine != null)
            budgetService.postActual(je.getId(), line.getId(), bLine.getId(), ...);
*/

// ════════════════════════════════════════════════════════════════════════════
// COMPLETE BUDGET ENTITY REGISTRY
// ════════════════════════════════════════════════════════════════════════════
/*
  ┌──────────────────────────────────┬──────────────────────────┬────────────────────────────────┐
  │  Entity                          │  Table                   │  Base / Pattern                │
  ├──────────────────────────────────┼──────────────────────────┼────────────────────────────────┤
  │  BudgetFiscalYear                │  bgt_fiscal_years        │  BaseOrgEntity                 │
  │  BudgetHead                      │  bgt_budget_heads        │  BaseOrgEntity  (self-ref)     │
  │  Budget                          │  bgt_budgets             │  BaseOrgEntity +               │
  │                                  │                          │  ApprovalDocumentCallback      │
  │  BudgetLine                      │  bgt_budget_lines        │  Plain + @PostLoad computed     │
  │  BudgetRevision                  │  bgt_budget_revisions    │  Plain +                       │
  │                                  │                          │  ApprovalDocumentCallback      │
  │  BudgetRevisionLine              │  bgt_budget_revision_lines │ Plain (child)               │
  │  BudgetActual                    │  bgt_actuals             │  Plain (immutable ledger)      │
  │  BudgetEncumbrance               │  bgt_encumbrances        │  Plain + @PostLoad computed    │
  │  BudgetTransfer                  │  bgt_transfers           │  Plain                         │
  │  BudgetAlert                     │  bgt_alerts              │  Plain                         │
  │  BudgetApprovalPolicy            │  bgt_approval_policies   │  BaseOrgEntity                 │
  │  BudgetNote                      │  bgt_budget_notes        │  Plain (child)                 │
  ├──────────────────────────────────┼──────────────────────────┼────────────────────────────────┤
  │  Enums                           │                          │                                │
  │  BudgetType         (new)        │  —                       │  7 values + code/displayName   │
  │  BudgetStatus       (new)        │  —                       │  9 values                      │
  │  BudgetHeadType     (new)        │  —                       │  8 values + displayName        │
  │  OverSpendPolicy    (new)        │  —                       │  3 values + description        │
  │  DocumentType       (+3)         │  —                       │  BUDGET/REVISION/TRANSFER      │
  │  ModuleType         (+1)         │  —                       │  BUDGET_MANAGEMENT             │
  └──────────────────────────────────┴──────────────────────────┴────────────────────────────────┘

  TOTAL NEW ENTITIES  : 12
  TOTAL NEW ENUMS     :  4 new  +  2 updated
  NEW PACKAGE         : com.asg.spindleserp.budget

  Key patterns applied:
  ─────────────────────────────────────────────────────────────────────
  ✔ Budget & BudgetRevision implement ApprovalDocumentCallback
  ✔ Explicit constructors with @Lazy ApprovalService (never @RequiredArgsConstructor)
  ✔ Budget.recalculateTotals() aggregates from lines
  ✔ BudgetLine.computeAvailable() on @PostLoad/@PrePersist/@PreUpdate
  ✔ BudgetLine.getAmountForMonth(int) for monthly phasing access
  ✔ BudgetEncumbrance.release(amount) applies partial/full release
  ✔ BudgetEncumbrance.isOverdue() checks expected invoice date
  ✔ BudgetTransfer.apply() adjusts both fromLine and toLine revised amounts
  ✔ BudgetAlert.resolve()/markSent() lifecycle helpers
  ✔ BudgetApprovalPolicy.appliesTo(type, amount) for policy matching
  ✔ BudgetHead.getFullPath() builds breadcrumb from parent chain
*/
