package com.asg.spindleserp.ecommerce.campaign.entity;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.cart.entity.EcCart;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ec_abandoned_cart",
        indexes = {
                @Index(name = "idx_ec_abandoned_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_abandoned_order", columnList = "recovered_order_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcAbandonedCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private EcCart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    private LocalDateTime abandonedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean reminderSent = Boolean.FALSE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean recovered = Boolean.FALSE;

    // Soft FK — set after order is created; avoids circular JPA dependency
    private Long recoveredOrderId;
}
