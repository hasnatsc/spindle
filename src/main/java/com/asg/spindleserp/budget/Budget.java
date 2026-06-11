package com.asg.spindleserp.budget;

import com.asg.spindleserp.approval.DocumentType;
import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.approval.ApprovalRequest;
import com.asg.spindleserp.approval.ApprovalStatus;
import com.asg.spindleserp.security.BusinessUnit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Budget — header entity.
 * <p>
 * Approval pattern:
 * - Implements ApprovalDocumentCallback
 * - Constructor registers itself with callbackRegistry
 * - @Lazy ApprovalService to break circular dependency
 * - NEVER use @RequiredArgsConstructor — Lombok ignores @Lazy on final fields
 */
@Entity
@Table(name = "bgt_budgets",
        uniqueConstraints = @UniqueConstraint(name = "uk_bgt_org_no",
                columnNames = {"organization_id", "budget_no"}),
        indexes = {
                @Index(name = "idx_bgt_org", columnList = "organization_id"),
                @Index(name = "idx_bgt_fy", columnList = "fiscal_year_id"),
                @Index(name = "idx_bgt_bu", columnList = "business_unit_id"),
                @Index(name = "idx_bgt_status", columnList = "status"),
                @Index(name = "idx_bgt_type", columnList = "budget_type"),
                @Index(name = "idx_bgt_apr", columnList = "approval_request_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Budget extends BaseOrgEntity implements ApprovalDocumentCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(name = "budget_no", nullable = false, length = 50)
    private String budgetNo;
    @Column(name = "budget_name", nullable = false, length = 200)
    private String budgetName;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type", nullable = false, length = 30)
    private BudgetType budgetType = BudgetType.ANNUAL;

    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType = "ANNUAL";  // ANNUAL|Q1|Q2|Q3|Q4|JAN..DEC|PROJECT

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(length = 3)
    private String currency = "BDT";
    @Column(name = "exchange_rate", precision = 18, scale = 4)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    // ── Running totals (maintained by fn_post_budget_actual) ──────
    @Column(name = "total_budgeted", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBudgeted = BigDecimal.ZERO;
    @Column(name = "total_revised", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRevised = BigDecimal.ZERO;
    @Column(name = "total_actual", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalActual = BigDecimal.ZERO;
    @Column(name = "total_committed", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCommitted = BigDecimal.ZERO;
    @Column(name = "total_available", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAvailable = BigDecimal.ZERO;

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
    @Column(nullable = false)
    private Integer version = 1;
    @Column(name = "is_template", nullable = false)
    private Boolean isTemplate = false;

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

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
    @Transient
    private ApprovalCallbackRegistry callbackRegistry;
    @Transient
    private ApprovalService approvalService;

    /**
     * Explicit constructor for @Lazy injection.
     * CRITICAL: Never use @RequiredArgsConstructor — Lombok ignores @Lazy.
     */
    public Budget(@org.springframework.context.annotation.Lazy ApprovalService approvalService,
                  ApprovalCallbackRegistry callbackRegistry) {
        this.approvalService = approvalService;
        this.callbackRegistry = callbackRegistry;
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

    /**
     * Called by ApprovalService when all levels approved.
     */
    @Override
    public void onApproved(Long referenceId) {
        this.status = BudgetStatus.ACTIVE;
        this.approvalStatus = ApprovalStatus.APPROVED;
    }

    /**
     * Called when any approver rejects.
     */
    @Override
    public void onRejected(Long referenceId) {
        this.status = BudgetStatus.REJECTED;
        this.approvalStatus = ApprovalStatus.REJECTED;
    }

    /**
     * Called when returned for correction.
     */
    @Override
    public void onReturned(Long referenceId) {
        this.status = BudgetStatus.RETURNED;
        this.approvalStatus = ApprovalStatus.RETURNED;
    }

    @Override
    public void onRecalled(Long referenceId) {
        this.status = BudgetStatus.DRAFT;
        this.approvalStatus = ApprovalStatus.DRAFT;
    }

    // ── Helpers ───────────────────────────────────────────────────
    public boolean isDraft() {
        return BudgetStatus.DRAFT.equals(status);
    }

    public boolean isActive() {
        return BudgetStatus.ACTIVE.equals(status);
    }

    public boolean isEditable() {
        return isDraft() || BudgetStatus.RETURNED.equals(status);
    }

    public boolean isLocked() {
        return BudgetStatus.LOCKED.equals(status) || BudgetStatus.CLOSED.equals(status);
    }

    /**
     * Submit for approval. Re-activates existing ApprovalRequest if RETURNED/REJECTED.
     */
    public void submit(String submittedBy) {
        if (!isEditable()) throw new IllegalStateException("Budget " + budgetNo + " is not editable");
        this.status = BudgetStatus.SUBMITTED;
        this.approvalStatus = ApprovalStatus.SUBMITTED;
        this.updatedBy = submittedBy;
    }

    /**
     * Recalculate header totals from all lines.
     */
    public void recalculateTotals() {
        this.totalBudgeted = lines.stream().map(BudgetLine::getOriginalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalRevised = lines.stream().map(BudgetLine::getRevisedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalActual = lines.stream().map(BudgetLine::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalCommitted = lines.stream().map(BudgetLine::getCommittedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAvailable = totalRevised.subtract(totalActual).subtract(totalCommitted);
    }
}
