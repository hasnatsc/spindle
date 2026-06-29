package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * EcCategory — storefront/CMS product category tree.
 *
 * SEPARATE from inv_item_categories (inventory classification).
 * inv_item_categories = stock type / costing classification
 * ec_categories       = customer-facing SEO + navigation hierarchy
 *
 * Linked implicitly via: EcProductCatalog → inv_items → inv_item_categories
 */
@Entity
@Table(name = "ec_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ec_cat_code", columnNames = {"organization_id", "category_code"}),
                @UniqueConstraint(name = "uq_ec_cat_slug", columnNames = {"organization_id", "slug"})
        },
        indexes = {
                @Index(name = "idx_ec_cat_org",      columnList = "organization_id"),
                @Index(name = "idx_ec_cat_parent",   columnList = "parent_category_id"),
                @Index(name = "idx_ec_cat_slug",     columnList = "slug"),
                @Index(name = "idx_ec_cat_active",   columnList = "active"),
                @Index(name = "idx_ec_cat_featured", columnList = "is_featured")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private EcCategory parentCategory;

    @Column(nullable = false, length = 50)
    private String categoryCode;

    @Column(nullable = false, length = 200)
    private String categoryName;

    @Column(nullable = false, length = 250)
    private String slug;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 700)
    private String imageUrl;

    @Column(length = 700)
    private String bannerUrl;

    @Column(length = 100)
    private String icon;

    // ── SEO ───────────────────────────────────────────────────────────────
    @Column(length = 255)
    private String metaTitle;

    @Column(length = 500)
    private String metaKeywords;

    @Column(length = 1000)
    private String metaDescription;

    // ── Display ───────────────────────────────────────────────────────────
    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    private Integer levelNo = 1;

    @Builder.Default
    @Column(nullable = false)
    private boolean isMenu = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean isFeatured = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    // ── Children (for tree rendering) ─────────────────────────────────────
    @Builder.Default
    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcCategory> children = new ArrayList<>();

    // ── Category attribute definitions ────────────────────────────────────
    @Builder.Default
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcCategoryAttribute> attributes = new ArrayList<>();
}
