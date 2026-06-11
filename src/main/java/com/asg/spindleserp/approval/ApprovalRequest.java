package com.asg.spindleserp.approval;

import com.asg.spindleserp.security.Organization;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_ar_doc", columnNames = {"document_type", "reference_id"}),
        indexes = {
                @Index(name = "idx_apr_req_org", columnList = "organization_id"),
                @Index(name = "idx_apr_req_dtype", columnList = "document_type,reference_id"),
                @Index(name = "idx_apr_req_status", columnList = "status"),
                @Index(name = "idx_apr_req_user", columnList = "current_approver_user_id"),
                @Index(name = "idx_apr_req_role", columnList = "current_approver_role"),
                @Index(name = "idx_apr_req_requester", columnList = "requester_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_config_id")
    private ApprovalConfig approvalConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approval_level_id")
    private ApprovalLevel currentApprovalLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver_user_id")
    private User currentApproverUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // Polymorphic document reference
    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;
    @Column(name = "reference_number", nullable = false, length = 100)
    private String referenceNumber;
    @Column(name = "document_date")
    private LocalDate documentDate;
    @Column(name = "document_amount", precision = 18, scale = 2)
    private BigDecimal documentAmount;
    @Column(name = "document_summary", length = 500)
    private String documentSummary;

    @Builder.Default
    @Column(name = "total_levels", nullable = false)
    private Integer totalLevels = 1;
    @Builder.Default
    @Column(name = "current_level_number", nullable = false)
    private Integer currentLevelNumber = 1;
    @Column(name = "current_approver_role", length = 80)
    private String currentApproverRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    @Column(name = "requester_name", length = 200)
    private String requesterName;
    @Builder.Default
    @Column(name = "is_urgent", nullable = false)
    private Boolean isUrgent = false;
    @Column(name = "due_date")
    private LocalDate dueDate;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("actionAt ASC")
    @Builder.Default
    private List<ApprovalHistory> histories = new ArrayList<>();

    public boolean isPending() {
        return status == ApprovalStatus.SUBMITTED || status == ApprovalStatus.IN_APPROVAL;
    }

    public boolean isLastLevel() {
        return currentLevelNumber != null && totalLevels != null && currentLevelNumber >= totalLevels;
    }

    public boolean isCompleted() {
        return status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED || status == ApprovalStatus.CANCELLED;
    }
}
