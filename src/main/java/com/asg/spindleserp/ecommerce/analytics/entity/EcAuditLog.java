package com.asg.spindleserp.ecommerce.analytics.entity;

import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_audit_logs",
        indexes = {
                @Index(name = "idx_ec_audit_entity", columnList = "entity_name,entity_id"),
                @Index(name = "idx_ec_audit_user", columnList = "user_id"),
                @Index(name = "idx_ec_audit_org", columnList = "organization_id"),
                @Index(name = "idx_ec_audit_time", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    // ERP staff user performing the action (null for automated system actions)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100)
    private String entityName;
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EcAuditLog.AuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String newData;

    @Column(length = 50)
    private String ipAddress;
    @Column(columnDefinition = "text")
    private String userAgent;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AuditAction {INSERT, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT, IMPORT}
}
