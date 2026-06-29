package com.asg.spindleserp.ecommerce.cms;

import com.asg.spindleserp.ecommerce.EcProductCatalog;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_home_section_products",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_home_prod",
                columnNames = {"section_id", "product_id"}),
        indexes = @Index(name = "idx_ec_homeprod_sec", columnList = "section_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcHomeSectionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private EcHomeSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Builder.Default
    private Integer displayOrder = 0;
}
