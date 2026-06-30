package com.asg.spindleserp.ecommerce.campaign.entity;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// =============================================================================
// EC COUPON
// =============================================================================

// =============================================================================
// EC COUPON USAGE
// order_id: DEFERRABLE FK — coupon applied before order is committed in same tx
// =============================================================================

@Entity
@Table(name = "ec_coupon_usage",
        indexes = {
                @Index(name = "idx_ec_couponuse_coupon", columnList = "coupon_id"),
                @Index(name = "idx_ec_couponuse_cust",   columnList = "customer_id"),
                @Index(name = "idx_ec_couponuse_order",  columnList = "order_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private EcCoupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    // Soft FK — resolved at service layer (avoids circular dep with EcOrder)
    // order_id set after order is persisted in EcOrderService.confirmOrder()
    private Long orderId;

    @Column(precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Builder.Default
    private LocalDateTime usedAt = LocalDateTime.now();
}

// =============================================================================
// EC PROMOTION CAMPAIGN  (price-discount, flash-sale etc.)
// DISTINCT from ec_email_campaigns (marketing email blasts)
// =============================================================================

// =============================================================================
// EC CAMPAIGN PRODUCT MAPPING
// =============================================================================

// =============================================================================
// EC ABANDONED CART
// recovered_order_id: soft FK (Long) — mirrors JournalEntryMaster.partyId pattern
// =============================================================================

