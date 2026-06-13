package com.asg.spindleserp.notification.entity;

import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ntf_notifications",
    indexes = {
        @Index(name = "idx_ntf_org",       columnList = "organization_id"),
        @Index(name = "idx_ntf_recipient", columnList = "recipient_id"),
        @Index(name = "idx_ntf_read",      columnList = "is_read"),
        @Index(name = "idx_ntf_created",   columnList = "created_at")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType notificationType = NotificationType.IN_APP;

    @Column(length = 50)  private String category;
    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "text") private String message;
    @Column(length = 500) private String link;
    @Column(length = 50)  private String referenceType;
    private Long          referenceId;

    @Builder.Default @Column(nullable = false) private boolean isRead = false;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;

    @Builder.Default @Column(nullable = false, length = 20) private String deliveryStatus = "PENDING";
    @Column(length = 500) private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum NotificationType { IN_APP, EMAIL, SMS, PUSH, WHATSAPP }
}