// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E07  Approval Engine                                      ║
// ║  Tables: apr_configs, apr_levels, apr_requests, apr_histories,           ║
// ║           apr_delegations, apr_notifications, apr_voucher                ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: approval/entity/ApprovalConfig.java ────────────────────────────────
package com.hasnat.optimum.approval.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "apr_configs",
    uniqueConstraints = @UniqueConstraint(name = "uq_aprc_code", columnNames = "code"),
    indexes = @Index(name = "idx_aprc_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalConfig extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 1000) private String description;
    @Column(nullable = false, length = 50) private String documentType;
    @Column(nullable = false, length = 30) private String module;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String flowType = "SEQUENTIAL"; // SEQUENTIAL | PARALLEL

    @Builder.Default @Column(nullable = false) private boolean isActive            = true;
    @Builder.Default @Column(nullable = false) private boolean enableReminders      = false;
    @Builder.Default @Column(nullable = false) private boolean useReportingHierarchy= false;

    private Integer priority;
    private Integer autoEscalationHours;
    private Integer reminderIntervalHours;

    @Column(precision = 18, scale = 2) private BigDecimal minAmount;
    @Column(precision = 18, scale = 2) private BigDecimal maxAmount;

    @Builder.Default
    @OneToMany(mappedBy = "approvalConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalLevel> levels = new ArrayList<>();
}


// ── FILE: approval/entity/ApprovalLevel.java ─────────────────────────────────
package com.hasnat.optimum.approval.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "apr_levels",
    indexes = @Index(name = "idx_aprl_config", columnList = "approval_config_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalLevel extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_config_id", nullable = false)
    private ApprovalConfig approvalConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id")
    private User approverUser;

    @Column(nullable = false) private Integer levelNumber;
    @Column(nullable = false, length = 100) private String levelName;
    @Column(length = 500) private String description;
    @Column(length = 200) private String approverDescription;

    @Builder.Default @Column(nullable = false) private boolean isActive              = true;
    @Builder.Default @Column(nullable = false) private boolean canApproveWithChanges = false;
    @Builder.Default @Column(nullable = false) private boolean canDelegate           = false;
    @Builder.Default @Column(nullable = false) private boolean canForward            = false;
    @Builder.Default @Column(nullable = false) private boolean canHold               = false;
}


// ── FILE: approval/entity/ApprovalRequest.java ───────────────────────────────
package com.hasnat.optimum.approval.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_requests",
    indexes = {
        @Index(name = "idx_aprr_org",      columnList = "organization_id"),
        @Index(name = "idx_aprr_status",   columnList = "status"),
        @Index(name = "idx_aprr_ref",      columnList = "reference_id, document_type"),
        @Index(name = "idx_aprr_req",      columnList = "requester_id"),
        @Index(name = "idx_aprr_approver", columnList = "current_approver_user_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalRequest extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approval_config_id")
    private ApprovalConfig approvalConfig;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "current_approval_level_id")
    private ApprovalLevel currentApprovalLevel;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "current_approver_user_id")
    private User currentApproverUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false, length = 50)  private String documentType;
    @Column(nullable = false)               private Long   referenceId;
    @Column(nullable = false, length = 100) private String referenceNumber;
    private LocalDate documentDate;
    @Column(precision = 18, scale = 2) private BigDecimal documentAmount;
    @Column(length = 500) private String documentSummary;

    @Builder.Default @Column(nullable = false) private int currentLevelNumber = 1;
    @Builder.Default @Column(nullable = false) private int totalLevels        = 1;

    @Column(length = 200) private String requesterName;
    @Column(length = 80)  private String currentApproverRole;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Builder.Default @Column(nullable = false) private boolean isUrgent = false;
    private LocalDate dueDate;
    @Column(length = 1000) private String finalRemarks;
    @Column(length = 100)  private String finalActionBy;
    private LocalDateTime completedAt;
}


// ── FILE: approval/entity/ApprovalHistory.java ───────────────────────────────
package com.hasnat.optimum.approval.entity;

import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_histories",
    indexes = {
        @Index(name = "idx_aprh_request", columnList = "approval_request_id"),
        @Index(name = "idx_aprh_user",    columnList = "actor_user_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approval_level_id")
    private ApprovalLevel approvalLevel;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "delegated_from_user_id")
    private User delegatedFromUser;

    @Column(nullable = false)         private Integer levelNumber;
    @Column(nullable = false, length = 100) private String levelName;
    @Column(nullable = false, length = 150) private String actorName;
    @Column(length = 100) private String actorDesignation;
    @Column(length = 150) private String actorDepartment;
    @Column(nullable = false, length = 30) private String action;
    @Column(nullable = false, length = 20) private String status;
    @Column(length = 2000) private String comments;
    @Column(length = 1000) private String rejectionReason;
    @Column(length = 1000) private String returnReason;

    @Builder.Default @Column(nullable = false) private boolean isAutoAction = false;

    @Column(length = 50) private String ipAddress;
    private Long responseTimeMinutes;
    private LocalDateTime actionAt;
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}


// ── FILE: approval/entity/ApprovalDelegation.java ────────────────────────────
package com.hasnat.optimum.approval.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_delegations",
    uniqueConstraints = @UniqueConstraint(name = "uq_aprd_code", columnNames = "delegation_code"),
    indexes = {
        @Index(name = "idx_aprdel_org",       columnList = "organization_id"),
        @Index(name = "idx_aprdel_delegator", columnList = "delegator_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalDelegation extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegator_id", nullable = false)
    private User delegator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegate_id", nullable = false)
    private User delegate;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "revoked_by_id")
    private User revokedBy;

    @Column(nullable = false, unique = true, length = 50) private String delegationCode;
    @Column(length = 30) private String module;
    @Column(length = 50) private String documentType;

    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;

    @Column(precision = 18, scale = 2) private BigDecimal maxAmount;
    @Column(length = 1000) private String reason;
    @Column(length = 500)  private String revocationReason;
    private LocalDateTime revokedAt;

    @Builder.Default @Column(nullable = false, length = 20) private String  status   = "SCHEDULED";
    @Builder.Default @Column(nullable = false)              private boolean isActive  = true;
    @Builder.Default @Column(nullable = false)              private boolean notifyDelegator = false;
}


// ── FILE: approval/entity/ApprovalNotification.java ──────────────────────────
package com.hasnat.optimum.approval.entity;

import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_notifications",
    indexes = {
        @Index(name = "idx_aprnot_org",  columnList = "organization_id"),
        @Index(name = "idx_aprnot_user", columnList = "recipient_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 200) private String subject;
    @Column(columnDefinition = "text") private String body;
    @Column(length = 255) private String link;
    @Column(nullable = false, length = 20) private String notificationType;
    @Column(nullable = false, length = 30) private String reason;
    @Column(length = 200) private String recipientEmail;
    @Column(length = 50)  private String recipientPhone;

    @Builder.Default @Column(nullable = false, length = 20) private String  deliveryStatus = "PENDING";
    @Column(length = 500) private String failureReason;
    @Builder.Default @Column(nullable = false) private int     retryCount = 0;
    @Builder.Default @Column(nullable = false) private boolean isRead     = false;

    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}


// ── FILE: approval/entity/ApprovalVoucher.java ───────────────────────────────
package com.hasnat.optimum.approval.entity;

import com.hasnat.optimum.accounts.entity.JournalEntryMaster;
import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "apr_voucher")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalVoucher extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_master_id", nullable = false, unique = true)
    private JournalEntryMaster journalEntryMaster;

    @Column(nullable = false)
    private Integer approvalLevel;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String approvalStatus = "PENDING";

    @Column(length = 100) private String approverName;
    @Column(length = 100) private String approverRole;
    private LocalDate approvalDate;
    @Column(length = 1000) private String approvalRemarks;
}
