package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// =============================================================================
// EC CART
// =============================================================================

@Entity
@Table(name = "ec_cart",
        indexes = {
                @Index(name = "idx_ec_cart_cust",    columnList = "customer_id"),
                @Index(name = "idx_ec_cart_session", columnList = "session_id"),
                @Index(name = "idx_ec_cart_status",  columnList = "cart_status"),
                @Index(name = "idx_ec_cart_org",     columnList = "organization_id")
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
    private CartStatus cartStatus = CartStatus.ACTIVE;

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

    public enum CartStatus { ACTIVE, ORDERED, ABANDONED, EXPIRED }
}

// =============================================================================
// EC CART ITEM
// unit_price: snapshot at add-to-cart time
// (reads ec_product_variants.sellingPrice if variant exists, else Item.unitPrice)
// =============================================================================

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
