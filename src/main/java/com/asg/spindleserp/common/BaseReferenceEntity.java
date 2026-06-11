package com.asg.spindleserp.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BaseReferenceEntity — for global reference/lookup tables that are NOT
 * organisation-scoped (countries, currencies, HS codes, etc.).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseReferenceEntity implements Serializable {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
