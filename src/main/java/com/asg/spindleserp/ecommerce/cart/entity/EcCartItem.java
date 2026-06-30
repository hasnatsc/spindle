package com.asg.spindleserp.ecommerce.cart.entity;

import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductCatalog;
import com.asg.spindleserp.ecommerce.productSupport.entity.EcProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ec_cart_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_cart_product",
                columnNames = {"cart_id", "product_id", "variant_id"}),
        indexes = {
                @Index(name = "idx_ec_cartitem_cart", columnList = "cart_id"),
                @Index(name = "idx_ec_cartitem_prod", columnList = "product_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private EcCart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private EcProductVariant variant;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    // Price snapshot at add-to-cart
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;

    @Column(length = 500)
    private String remarks;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
