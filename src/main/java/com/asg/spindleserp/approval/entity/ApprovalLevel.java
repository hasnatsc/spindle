package com.asg.spindleserp.approval.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "apr_levels",
        indexes = @Index(name = "idx_aprl_config", columnList = "approval_config_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalLevel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_config_id", nullable = false)
    private ApprovalConfig approvalConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_user_id")
    private User approverUser;

    @Column(nullable = false)
    private Integer levelNumber;
    @Column(nullable = false, length = 100)
    private String levelName;
    @Column(length = 500)
    private String description;
    @Column(length = 200)
    private String approverDescription;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean canApproveWithChanges = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean canDelegate = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean canForward = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean canHold = false;
}
