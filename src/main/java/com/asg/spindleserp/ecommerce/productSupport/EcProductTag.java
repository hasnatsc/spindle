package com.asg.spindleserp.ecommerce.productSupport;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_product_tags",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_tag",
                columnNames = {"organization_id", "slug"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EcProductTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 100)
    private String tagName;

    @Column(nullable = false, length = 120)
    private String slug;
}
