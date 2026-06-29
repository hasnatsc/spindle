package com.asg.spindleserp.ecommerce.campaign;

import com.asg.spindleserp.ecommerce.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.EcProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ec_campaign_products",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_campprod",
                columnNames = {"campaign_id", "product_id", "variant_id"}),
        indexes = {
                @Index(name = "idx_ec_campprod_camp", columnList = "campaign_id"),
                @Index(name = "idx_ec_campprod_prod", columnList = "product_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCampaignProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private EcCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EcCoupon.DiscountType discountType;

    @Column(precision = 18, scale = 2)
    private BigDecimal discountValue;
    @Column(precision = 18, scale = 2)
    private BigDecimal specialPrice;
    @Column(precision = 12, scale = 3)
    private BigDecimal maximumQty;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
