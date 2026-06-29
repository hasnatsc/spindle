package com.asg.spindleserp.ecommerce.productSupport;

import com.asg.spindleserp.ecommerce.EcCategoryAttribute;
import com.asg.spindleserp.ecommerce.EcProductCatalog;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ec_product_attribute_values",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_prodattr",
                columnNames = {"product_id", "category_attribute_id"}),
        indexes = @Index(name = "idx_ec_prodattr_prod", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcProductAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_attribute_id", nullable = false)
    private EcCategoryAttribute categoryAttribute;

    @Column(columnDefinition = "text")
    private String attributeValue;
}
