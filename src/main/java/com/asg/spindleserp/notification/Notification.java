package com.asg.spindleserp.notification;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ntf_notifications",
        indexes = {
                @Index(name = "idx_ntf_user", columnList = "user_id"),
                @Index(name = "idx_ntf_unread", columnList = "user_id,is_read")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 300)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;
    @Column(name = "reference_type", length = 50)
    private String referenceType;
    @Column(name = "reference_id")
    private Long referenceId;
    @Column(length = 10)
    @Builder.Default
    private String priority = "NORMAL";
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    @Column(name = "read_at")
    private LocalDateTime readAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "action_url", length = 500)
    private String actionUrl;
    @Column(name = "created_by", length = 100)
    private String createdBy;
}
