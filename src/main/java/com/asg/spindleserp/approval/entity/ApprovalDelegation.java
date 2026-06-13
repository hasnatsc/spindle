package com.asg.spindleserp.approval.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apr_delegations",
        uniqueConstraints = @UniqueConstraint(name = "uq_aprd_code", columnNames = "delegation_code"),
        indexes = {
                @Index(name = "idx_aprdel_org", columnList = "organization_id"),
                @Index(name = "idx_aprdel_delegator", columnList = "delegator_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDelegation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegator_id", nullable = false)
    private User delegator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delegate_id", nullable = false)
    private User delegate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_id")
    private User revokedBy;

    @Column(nullable = false, unique = true, length = 50)
    private String delegationCode;
    @Column(length = 30)
    private String module;
    @Column(length = 50)
    private String documentType;

    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;

    @Column(precision = 18, scale = 2)
    private BigDecimal maxAmount;
    @Column(length = 1000)
    private String reason;
    @Column(length = 500)
    private String revocationReason;
    private LocalDateTime revokedAt;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "SCHEDULED";
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean notifyDelegator = false;
}
