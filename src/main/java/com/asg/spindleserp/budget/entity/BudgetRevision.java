package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.approval.entity.ApprovalRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_budget_revisions",
        uniqueConstraints = @UniqueConstraint(name = "uq_bbr_budget_rev",
                columnNames = {"budget_id", "revision_number"}),
        indexes = @Index(name = "idx_bbr_budget", columnList = "budget_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRevision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @Column(nullable = false, length = 50)
    private String revisionNo;
    @Column(nullable = false)
    private Integer revisionNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BudgetRevision.RevisionType revisionType = BudgetRevision.RevisionType.REALLOCATION;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;
    @Column(columnDefinition = "text")
    private String justification;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalIncrease = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDecrease = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BudgetRevision.RevisionStatus status = BudgetRevision.RevisionStatus.DRAFT;

    @Column(length = 100)
    private String approvedBy;
    private LocalDateTime approvedAt;

    public enum RevisionType {REALLOCATION, SUPPLEMENTARY, REDUCTION, TECHNICAL}

    public enum RevisionStatus {DRAFT, SUBMITTED, IN_APPROVAL, APPROVED, REJECTED}
}
