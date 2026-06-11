package com.asg.spindleserp.ecommerce;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.dummy.EcoCartItem;
import com.asg.spindleserp.dummy.EcoCoupon;
import com.asg.spindleserp.dummy.EcoCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "eco_carts",
        indexes = {
                @Index(name = "idx_cart_cust", columnList = "customer_id"),
                @Index(name = "idx_cart_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoCart extends BaseOrgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private EcoStore store;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcoCustomer customer;
    @Column(name = "session_token", length = 100)
    private String sessionToken;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE|ABANDONED|CONVERTED|MERGED
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private EcoCoupon coupon;
    @Column(name = "coupon_code", length = 50)
    private String couponCode;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "shipping_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;
    @Column(length = 3)
    @Builder.Default
    private String currency = "BDT";
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Column(name = "converted_at")
    private LocalDateTime convertedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EcoCartItem> items = new ArrayList<>();
}
