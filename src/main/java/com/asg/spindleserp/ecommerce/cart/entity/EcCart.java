package com.asg.spindleserp.ecommerce.cart.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ec_cart",
        indexes = {
                @Index(name = "idx_ec_cart_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_cart_session", columnList = "session_id"),
                @Index(name = "idx_ec_cart_status", columnList = "cart_status"),
                @Index(name = "idx_ec_cart_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    // For guest/anonymous carts
    @Column(length = 150)
    private String sessionId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EcCart.CartStatus cartStatus = EcCart.CartStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalItems = 0;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingCharge = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcCartItem> items = new ArrayList<>();

    public enum CartStatus {ACTIVE, ORDERED, ABANDONED, EXPIRED}
}
