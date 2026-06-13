package com.asg.spindleserp.approval.entity;

import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "apr_histories",
        indexes = {
                @Index(name = "idx_aprh_request", columnList = "approval_request_id"),
                @Index(name = "idx_aprh_user", columnList = "actor_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_level_id")
    private ApprovalLevel approvalLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_from_user_id")
    private User delegatedFromUser;

    @Column(nullable = false)
    private Integer levelNumber;
    @Column(nullable = false, length = 100)
    private String levelName;
    @Column(nullable = false, length = 150)
    private String actorName;
    @Column(length = 100)
    private String actorDesignation;
    @Column(length = 150)
    private String actorDepartment;
    @Column(nullable = false, length = 30)
    private String action;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(length = 2000)
    private String comments;
    @Column(length = 1000)
    private String rejectionReason;
    @Column(length = 1000)
    private String returnReason;

    @Builder.Default
    @Column(nullable = false)
    private boolean isAutoAction = false;

    @Column(length = 50)
    private String ipAddress;
    private Long responseTimeMinutes;
    private LocalDateTime actionAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
