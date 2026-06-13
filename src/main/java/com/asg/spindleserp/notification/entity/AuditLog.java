package com.asg.spindleserp.notification.entity;

import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_audit_log",
        indexes = {
                @Index(name = "idx_sal_org", columnList = "organization_id"),
                @Index(name = "idx_sal_user", columnList = "user_id"),
                @Index(name = "idx_sal_entity", columnList = "entity_type, entity_id"),
                @Index(name = "idx_sal_created", columnList = "created_at"),
                @Index(name = "idx_sal_action", columnList = "action")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100)
    private String username;
    @Column(nullable = false, length = 50)
    private String action;
    @Column(length = 100)
    private String entityType;
    private Long entityId;
    @Column(length = 100)
    private String entityCode;
    @Column(columnDefinition = "text")
    private String oldValues;
    @Column(columnDefinition = "text")
    private String newValues;
    @Column(length = 50)
    private String ipAddress;
    @Column(length = 500)
    private String userAgent;
    @Column(length = 100)
    private String sessionId;
    @Column(length = 500)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
