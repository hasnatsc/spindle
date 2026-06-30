package com.asg.spindleserp.ecommerce.marketing.entity;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "ec_saved_for_later",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_saved",
                columnNames = {"customer_id", "product_id", "variant_id"}),
        indexes = @Index(name = "idx_ec_saved_cust", columnList = "customer_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcSavedForLater {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcProductVariant variant;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ONE;

    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
