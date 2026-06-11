package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eco_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_ecat_store_slug", columnNames = {"store_id", "slug"}),
        indexes = {
                @Index(name = "idx_cat_store", columnList = "store_id"),
                @Index(name = "idx_cat_parent", columnList = "parent_id"),
                @Index(name = "idx_cat_slug", columnList = "store_id,slug")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoCategory extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private EcoCategory parent;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "name_bn", length = 200)
    private String nameBn;
    @Column(nullable = false, length = 200)
    private String slug;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @Column(name = "banner_url", length = 500)
    private String bannerUrl;
    @Column(length = 100)
    private String icon;
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    @Column(name = "meta_title", length = 300)
    private String metaTitle;
    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
