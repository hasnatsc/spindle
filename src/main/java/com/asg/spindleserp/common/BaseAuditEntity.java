package com.asg.spindleserp.common;

import com.asg.spindleserp.security.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BaseAuditEntity — Spring Data Auditing + org isolation.
 * <p>
 * Rules:
 * 1. organization MUST be set before createdBy in all create() methods.
 * 2. @CreatedBy / @LastModifiedBy are populated by SpringSecurityAuditorAware.
 * 3. Never use @RequiredArgsConstructor when @Lazy injection is needed.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    protected void onPrePersist() {
        if (this.organization == null) {
            this.organization = ContextProvider.getOrganizationReference();
        }
    }
}
