package com.asg.spindleserp.ecommerce.analytics;

import com.asg.spindleserp.ecommerce.EcProductCatalog;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ec_product_analytics",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_prodana",
                columnNames = {"product_id", "analytics_date"}),
        indexes = {
                @Index(name = "idx_ec_prodana_prod", columnList = "product_id"),
                @Index(name = "idx_ec_prodana_date", columnList = "analytics_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcProductAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Column(nullable = false)
    private LocalDate analyticsDate;

    @Builder.Default
    @Column(nullable = false)
    private Integer productViews = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer wishlistCount = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer cartAdditions = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer purchases = 0;
    @Builder.Default
    @Column(nullable = false)
    private Integer returns = 0;
    @Builder.Default
    @Column(precision = 8, scale = 2)
    private BigDecimal conversionRate = BigDecimal.ZERO;
    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;
}
