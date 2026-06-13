// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E15  Notifications + Audit Log                            ║
// ║  Tables: ntf_notifications, sys_audit_log (partitioned)                 ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: notification/entity/Notification.java ──────────────────────────────
package com.hasnat.optimum.notification.entity;

import com.hasnat.optimum.security.entity.User;
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


// ── FILE: notification/entity/AuditLog.java ──────────────────────────────────
// NOTE: sys_audit_log is PARTITIONED BY RANGE(created_at) in PostgreSQL.
//       JPA treats it as a regular table — no special mapping needed.
//       Partitions are managed at the DB level.
package com.hasnat.optimum.notification.entity;

import com.hasnat.optimum.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sys_audit_log",
    indexes = {
        @Index(name = "idx_sal_org",     columnList = "organization_id"),
        @Index(name = "idx_sal_user",    columnList = "user_id"),
        @Index(name = "idx_sal_entity",  columnList = "entity_type, entity_id"),
        @Index(name = "idx_sal_created", columnList = "created_at"),
        @Index(name = "idx_sal_action",  columnList = "action")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100) private String username;
    @Column(nullable = false, length = 50) private String action;
    @Column(length = 100) private String entityType;
    private Long          entityId;
    @Column(length = 100) private String entityCode;
    @Column(columnDefinition = "text") private String oldValues;
    @Column(columnDefinition = "text") private String newValues;
    @Column(length = 50)  private String ipAddress;
    @Column(length = 500) private String userAgent;
    @Column(length = 100) private String sessionId;
    @Column(length = 500) private String remarks;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
