package com.asg.spindleserp.approval;

import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_levels",
        indexes = {
                @Index(name = "idx_apr_lvl_cfg", columnList = "approval_config_id"),
                @Index(name = "idx_apr_lvl_user", columnList = "approver_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalLevel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_config_id", nullable = false)
    private ApprovalConfig approvalConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id")
    private User approverUser;

    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;
    @Column(name = "level_name", nullable = false, length = 100)
    private String levelName;
    @Column(length = 500)
    private String description;
    @Column(name = "approver_description", length = 200)
    private String approverDescription;

    @Builder.Default
    @Column(name = "can_delegate", nullable = false)
    private Boolean canDelegate = true;
    @Builder.Default
    @Column(name = "can_hold", nullable = false)
    private Boolean canHold = false;
    @Builder.Default
    @Column(name = "can_forward", nullable = false)
    private Boolean canForward = false;
    @Builder.Default
    @Column(name = "can_approve_with_changes", nullable = false)
    private Boolean canApproveWithChanges = false;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
}
