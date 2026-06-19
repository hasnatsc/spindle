package com.asg.spindleserp.approval.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_requests",
        indexes = {
                @Index(name = "idx_aprr_org", columnList = "organization_id"),
                @Index(name = "idx_aprr_status", columnList = "status"),
                @Index(name = "idx_aprr_ref", columnList = "reference_id, document_type"),
                @Index(name = "idx_aprr_req", columnList = "requester_id"),
                @Index(name = "idx_aprr_approver", columnList = "current_approver_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_config_id")
    private ApprovalConfig approvalConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approval_level_id")
    private ApprovalLevel currentApprovalLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_approver_user_id")
    private User currentApproverUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false, length = 50)
    private String documentType;
    @Column(nullable = false)
    private Long referenceId;
    @Column(nullable = false, length = 100)
    private String referenceNumber;
    private LocalDate documentDate;
    @Column(precision = 18, scale = 2)
    private BigDecimal documentAmount;
    @Column(length = 500)
    private String documentSummary;

    @Builder.Default
    @Column(nullable = false)
    private int currentLevelNumber = 1;
    @Builder.Default
    @Column(nullable = false)
    private int totalLevels = 1;

    @Column(length = 200)
    private String requesterName;
    @Column(length = 80)
    private String currentApproverRole;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Builder.Default
    @Column(nullable = false)
    private boolean isUrgent = false;
    private LocalDate dueDate;
    @Column(length = 1000)
    private String finalRemarks;
    @Column(length = 100)
    private String finalActionBy;
    private LocalDateTime completedAt;
}
