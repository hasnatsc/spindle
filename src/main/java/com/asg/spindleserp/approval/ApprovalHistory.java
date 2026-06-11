package com.asg.spindleserp.approval;

import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_histories",
        indexes = {
                @Index(name = "idx_apr_hist_req", columnList = "approval_request_id"),
                @Index(name = "idx_apr_hist_actor", columnList = "actor_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_level_id")
    private ApprovalLevel approvalLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_from_user_id")
    private User delegatedFromUser;

    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;
    @Column(name = "level_name", nullable = false, length = 100)
    private String levelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalAction action;
    // SUBMIT|APPROVE|REJECT|RETURN|RECALL|HOLD|FORWARD|DELEGATE|ESCALATE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalStatus status;

    @Column(name = "actor_name", nullable = false, length = 150)
    private String actorName;
    @Column(name = "actor_designation", length = 100)
    private String actorDesignation;
    @Column(name = "actor_department", length = 150)
    private String actorDepartment;
    @Column(columnDefinition = "TEXT")
    private String comments;
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    @Column(name = "return_reason", columnDefinition = "TEXT")
    private String returnReason;
    @Column(name = "response_minutes")
    private Long responseMinutes;

    @CreationTimestamp
    @Column(name = "action_at", nullable = false, updatable = false)
    private LocalDateTime actionAt;
}
