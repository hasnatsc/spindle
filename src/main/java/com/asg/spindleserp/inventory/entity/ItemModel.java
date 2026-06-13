package com.asg.spindleserp.inventory.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inv_item_models",
        uniqueConstraints = @UniqueConstraint(name = "uq_model_org_brand_code",
                columnNames = {"organization_id", "brand_id", "model_code"}),
        indexes = @Index(name = "idx_model_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemModel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private ItemBrand brand;

    @Column(nullable = false, length = 30)
    private String modelCode;
    @Column(nullable = false, length = 150)
    private String modelName;
    @Column(columnDefinition = "text")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
