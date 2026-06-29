package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// =============================================================================
// EC COUPON
// =============================================================================

@Entity
@Table(name = "ec_coupon",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_coupon",
                columnNames = {"organization_id", "coupon_code"}),
        indexes = {
                @Index(name = "idx_ec_coupon_code", columnList = "coupon_code"),
                @Index(name = "idx_ec_coupon_org",  columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String couponCode;

    @Column(length = 200)
    private String couponName;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DiscountType discountType;

    @Column(precision = 18, scale = 2) private BigDecimal discountValue;
    @Column(precision = 18, scale = 2) private BigDecimal minimumOrder;
    @Column(precision = 18, scale = 2) private BigDecimal maximumDiscount;

    private Integer usageLimit;

    @Builder.Default
    private Integer usagePerCustomer = 1;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public enum DiscountType { PERCENTAGE, FIXED }
}

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

@Entity
@Table(name = "ec_campaigns",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_campaign",
                columnNames = {"organization_id", "campaign_code"}),
        indexes = {
                @Index(name = "idx_ec_campaign_org",    columnList = "organization_id"),
                @Index(name = "idx_ec_campaign_dates",  columnList = "start_date,end_date"),
                @Index(name = "idx_ec_campaign_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCampaign extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String campaignCode;

    @Column(length = 200)
    private String campaignName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CampaignType campaignType;

    @Column(length = 700)
    private String bannerImage;

    @Column(columnDefinition = "text")
    private String description;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder.Default
    private Integer priority = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcCampaignProduct> campaignProducts = new ArrayList<>();

    public enum CampaignType {
        FLASH_SALE, DISCOUNT, BUY_X_GET_Y, FREE_SHIPPING, SEASONAL, CLEARANCE
    }
}

// =============================================================================
// EC CAMPAIGN PRODUCT MAPPING
// =============================================================================

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

    @Column(precision = 18, scale = 2) private BigDecimal discountValue;
    @Column(precision = 18, scale = 2) private BigDecimal specialPrice;
    @Column(precision = 12, scale = 3) private BigDecimal maximumQty;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}

// =============================================================================
// EC ABANDONED CART
// recovered_order_id: soft FK (Long) — mirrors JournalEntryMaster.partyId pattern
// =============================================================================

@Entity
@Table(name = "ec_abandoned_cart",
        indexes = {
                @Index(name = "idx_ec_abandoned_cust",  columnList = "customer_id"),
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
