package com.asg.spindleserp.ecommerce.productSupport;

import com.asg.spindleserp.ecommerce.EcProductCatalog;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_product_tag_map",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_prod_tag",
                columnNames = {"product_id", "tag_id"}),
        indexes = @Index(name = "idx_ec_prodtag_prod", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class EcProductTagMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private EcProductTag tag;
}
