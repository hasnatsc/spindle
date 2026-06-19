package com.asg.spindleserp.inventory.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.common.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inv_item_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_icat_org_code",
                columnNames = {"organization_id", "category_code"}),
        indexes = {
                @Index(name = "idx_icat_org", columnList = "organization_id"),
                @Index(name = "idx_icat_parent", columnList = "parent_category_id"),
                @Index(name = "idx_icat_type", columnList = "item_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ItemCategory parentCategory;

    @Column(nullable = false, length = 50)
    private String categoryCode;
    @Column(nullable = false, length = 100)
    private String categoryName;
    @Column(columnDefinition = "text")
    private String description;

    // ★ Generic item type — applies to any manufacturing industry
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ItemType itemType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LayerType layerType = LayerType.ITEM;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    public enum LayerType {ROOT, GROUP, ITEM}
}
