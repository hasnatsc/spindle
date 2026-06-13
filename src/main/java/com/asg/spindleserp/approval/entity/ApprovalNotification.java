package com.asg.spindleserp.approval.entity;

import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "apr_notifications",
        indexes = {
                @Index(name = "idx_aprnot_org", columnList = "organization_id"),
                @Index(name = "idx_aprnot_user", columnList = "recipient_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 200)
    private String subject;
    @Column(columnDefinition = "text")
    private String body;
    @Column(length = 255)
    private String link;
    @Column(nullable = false, length = 20)
    private String notificationType;
    @Column(nullable = false, length = 30)
    private String reason;
    @Column(length = 200)
    private String recipientEmail;
    @Column(length = 50)
    private String recipientPhone;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String deliveryStatus = "PENDING";
    @Column(length = 500)
    private String failureReason;
    @Builder.Default
    @Column(nullable = false)
    private int retryCount = 0;
    @Builder.Default
    @Column(nullable = false)
    private boolean isRead = false;

    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
