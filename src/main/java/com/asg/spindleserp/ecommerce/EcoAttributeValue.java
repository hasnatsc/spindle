package com.asg.spindleserp.ecommerce;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "eco_attribute_values",
        uniqueConstraints = @UniqueConstraint(name = "uk_atv_grp_slug", columnNames = {"attribute_group_id", "slug"}),
        indexes = @Index(name = "idx_atv_group", columnList = "attribute_group_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoAttributeValue implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_group_id", nullable = false)
    private EcoAttributeGroup attributeGroup;
    @Column(nullable = false, length = 100)
    private String value;
    @Column(name = "value_bn", length = 100)
    private String valueBn;
    @Column(nullable = false, length = 100)
    private String slug;
    @Column(name = "color_hex", length = 7)
    private String colorHex;   // #FF0000 for colour swatches
    @Column(name = "image_url", length = 500)
    private String imageUrl;   // for image swatches
    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
