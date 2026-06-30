package com.asg.spindleserp.ecommerce.productSupport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * EcCategoryAttribute — filterable/searchable attribute schema per storefront category.
 * Instance values are stored in EcProductAttributeValue.
 *
 * Does NOT extend BaseEntity — category attributes are owned by EcCategory
 * (CascadeType.ALL + orphanRemoval = true), no independent org audit needed.
 */
@Entity
@Table(name = "ec_category_attributes",
        indexes = @Index(name = "idx_ec_catattr_cat", columnList = "category_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private EcCategory category;

    @Column(nullable = false, length = 150)
    private String attributeName;

    @Column(length = 150)
    private String attributeLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DataType dataType;

    @Builder.Default
    @Column(nullable = false)
    private boolean isRequired = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean searchable = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean filterable = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean sortable = false;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum DataType { TEXT, NUMBER, BOOLEAN, DATE, LIST, COLOR }
}
