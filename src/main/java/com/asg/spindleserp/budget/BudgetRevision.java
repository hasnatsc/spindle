package com.asg.spindleserp.budget;

import com.asg.spindleserp.approval.ApprovalRequest;
import com.asg.spindleserp.approval.ApprovalStatus;
import com.asg.spindleserp.approval.DocumentType;
import com.asg.spindleserp.security.Organization;
import lombok.Getter;

@Entity
@Table(name = "bgt_budget_revisions",
        uniqueConstraints = @UniqueConstraint(name = "uk_bgt_rev_budget_no",
                columnNames = {"budget_id", "revision_number"}),
        indexes = {
                @Index(name = "idx_bgt_rev_budget", columnList = "budget_id"),
                @Index(name = "idx_bgt_rev_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class BudgetRevision implements Serializable, ApprovalDocumentCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "revision_no", nullable = false, length = 50)
    private String revisionNo;
    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;
    @Column(name = "revision_type", nullable = false, length = 30)
    private String revisionType = "REALLOCATION";
    // REALLOCATION | SUPPLEMENTARY | REDUCTION | TECHNICAL

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(name = "total_increase", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalIncrease = BigDecimal.ZERO;
    @Column(name = "total_decrease", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDecrease = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";
    // DRAFT | SUBMITTED | IN_APPROVAL | APPROVED | REJECTED

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30)
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetRevisionLine> lines = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private ApprovalCallbackRegistry callbackRegistry;
    @Transient
    private ApprovalService approvalService;

    public BudgetRevision(@org.springframework.context.annotation.Lazy ApprovalService approvalService,
                          ApprovalCallbackRegistry callbackRegistry) {
        this.approvalService = approvalService;
        this.callbackRegistry = callbackRegistry;
        callbackRegistry.register(this);
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.BUDGET_REVISION;
    }

    @Override
    public String getDocumentNumber() {
        return revisionNo;
    }

    @Override
    public BigDecimal getDocumentAmount() {
        return totalIncrease.subtract(totalDecrease);
    }

    @Override
    public String getDocumentSummary() {
        return String.format("Revision %d for Budget %s | %s | Net change: %s",
                revisionNumber, budget != null ? budget.getBudgetNo() : "?", reason,
                totalIncrease.subtract(totalDecrease));
    }

    @Override
    public void onApproved(Long referenceId) {
        this.status = "APPROVED";
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        // BudgetService will apply the revision lines to the budget lines
    }

    @Override
    public void onRejected(Long referenceId) {
        this.status = "REJECTED";
        this.approvalStatus = ApprovalStatus.REJECTED;
    }

    @Override
    public void onReturned(Long referenceId) {
        this.status = "DRAFT";
        this.approvalStatus = ApprovalStatus.RETURNED;
    }

    @Override
    public void onRecalled(Long referenceId) {
        this.status = "DRAFT";
        this.approvalStatus = ApprovalStatus.DRAFT;
    }
}
