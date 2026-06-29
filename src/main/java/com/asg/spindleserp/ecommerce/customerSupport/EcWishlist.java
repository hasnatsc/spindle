package com.asg.spindleserp.ecommerce.customerSupport;

import com.asg.spindleserp.ecommerce.EcCustomer;
import com.asg.spindleserp.ecommerce.EcProductCatalog;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_wishlist",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_wishlist",
                columnNames = {"customer_id", "product_id"}),
        indexes = {
                @Index(name = "idx_ec_wishlist_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_wishlist_prod", columnList = "product_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcWishlist {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
