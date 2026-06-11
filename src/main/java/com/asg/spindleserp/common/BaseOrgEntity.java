package com.asg.spindleserp.common;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BaseOrgEntity — lightweight base without Spring Data Auditing.
 * Suitable for entities that manage createdBy/updatedBy manually.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseOrgEntity implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void enforceOrganization() {
        Long ctxOrgId = ContextProvider.getOrganizationId();
        if (ctxOrgId == null) throw new IllegalStateException("No organization in security context");
        if (this.organization == null) {
            this.organization = ContextProvider.getOrganizationReference();
        } else if (!this.organization.getId().equals(ctxOrgId)) {
            throw new IllegalStateException("Cross-organization write is not allowed");
        }
    }
}
