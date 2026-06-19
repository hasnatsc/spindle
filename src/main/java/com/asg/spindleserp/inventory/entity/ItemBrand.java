package com.asg.spindleserp.inventory.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inv_item_brands",
        uniqueConstraints = @UniqueConstraint(name = "uq_brand_org_code",
                columnNames = {"organization_id", "brand_code"}),
        indexes = @Index(name = "idx_brand_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemBrand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String brandCode;
    @Column(nullable = false, length = 150)
    private String brandName;
    @Column(columnDefinition = "text")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
