package com.asg.spindleserp.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * UserAccessScope
 *
 * A single row = "this user is allowed to operate in this scope".
 * scopeType: ORGANIZATION | BUSINESS_UNIT | COST_CENTER | WAREHOUSE
 * referenceId: the PK of the corresponding org entity.
 *
 * Empty table for a given user = no restriction (superadmin behaviour).
 * Used by the UserContext session bean to filter dropdown data.
 *
 * Table: sec_user_access_scopes
 */
@Entity
@Table(name = "sec_user_access_scopes",
        uniqueConstraints = @UniqueConstraint(name = "uq_uas_user_scope_ref",
                columnNames = {"user_id", "scope_type", "reference_id"}),
        indexes = {
                @Index(name = "idx_uas_user",  columnList = "user_id"),
                @Index(name = "idx_uas_scope", columnList = "scope_type"),
                @Index(name = "idx_uas_ref",   columnList = "reference_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccessScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private ScopeType scopeType;

    /** FK to org_organizations / org_business_units / org_cost_centers / org_warehouses */
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum ScopeType {
        ORGANIZATION,
        BUSINESS_UNIT,
        COST_CENTER,
        WAREHOUSE
    }
}
