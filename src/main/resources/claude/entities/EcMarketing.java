package com.asg.spindleserp.ecommerce.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// =============================================================================
// EC NEWSLETTER SUBSCRIBER
// =============================================================================

@Entity
@Table(name = "ec_newsletter",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_newsletter",
                columnNames = {"organization_id", "email"}),
        indexes = {
                @Index(name = "idx_ec_newsletter_status", columnList = "subscription_status"),
                @Index(name = "idx_ec_newsletter_org",    columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcNewsletter extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200) private String email;
    @Column(length = 200) private String fullName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.SUBSCRIBED;

    @Column(length = 255) private String verificationToken;
    @Builder.Default @Column(nullable = false) private boolean verified = false;
    @Builder.Default private LocalDateTime subscribedAt = LocalDateTime.now();
    private LocalDateTime unsubscribedAt;

    public enum SubscriptionStatus { SUBSCRIBED, UNSUBSCRIBED, BOUNCED }
}

// =============================================================================
// EC LOYALTY PROGRAM CONFIGURATION
// =============================================================================

@Entity
@Table(name = "ec_loyalty_programs",
        indexes = @Index(name = "idx_ec_loyalty_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcLoyaltyProgram extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200) private String programName;
    @Column(precision = 12, scale = 4) private BigDecimal earnRate;    // points per BDT spent
    @Column(precision = 12, scale = 4) private BigDecimal redeemRate;  // BDT per point
    private Integer minimumPoints;
    private Integer expiryDays;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC GIFT CARD
// =============================================================================

@Entity
@Table(name = "ec_gift_cards",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_gift_card",
                columnNames = {"organization_id", "gift_card_code"}),
        indexes = {
                @Index(name = "idx_ec_giftcard_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_giftcard_org",  columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcGiftCard extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100) private String giftCardCode;
    @Column(precision = 18, scale = 2) private BigDecimal initialAmount;
    @Column(precision = 18, scale = 2) private BigDecimal balanceAmount;
    private LocalDate expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Builder.Default @Column(nullable = false) private boolean active = true;
}

// =============================================================================
// EC AFFILIATE
// =============================================================================

@Entity
@Table(name = "ec_affiliates",
        uniqueConstraints = @UniqueConstraint(name = "uq_ec_affiliate",
                columnNames = {"organization_id", "affiliate_code"}),
        indexes = {
                @Index(name = "idx_ec_affiliate_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_affiliate_org",  columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcAffiliate extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Column(length = 100) private String affiliateCode;
    @Column(precision = 8, scale = 2) private BigDecimal commissionRate;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal totalSales      = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal totalCommission  = BigDecimal.ZERO;
    @Builder.Default @Column(precision = 18, scale = 2) private BigDecimal payableAmount    = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AffiliateStatus status = AffiliateStatus.ACTIVE;

    public enum AffiliateStatus { ACTIVE, BLOCKED, SUSPENDED }
}

// =============================================================================
// EC REFERRAL
// =============================================================================

@Entity
@Table(name = "ec_referrals",
        indexes = @Index(name = "idx_ec_referral_aff", columnList = "affiliate_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcReferral {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliate_id")
    private EcAffiliate affiliate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_customer")
    private EcCustomer referredCustomer;

    @Column(precision = 18, scale = 2) private BigDecimal referralBonus;
    @Builder.Default private LocalDateTime referralDate = LocalDateTime.now();
}

// =============================================================================
// EC CUSTOMER IN-APP NOTIFICATION
// =============================================================================

@Entity
@Table(name = "ec_notifications",
        indexes = {
                @Index(name = "idx_ec_notif_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_notif_org",  columnList = "organization_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcNotification extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationType notificationType;

    @Column(length = 300) private String title;
    @Column(columnDefinition = "text") private String message;
    @Builder.Default @Column(nullable = false) private boolean readFlag = false;
    private LocalDateTime sentAt;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    public enum NotificationType { EMAIL, SMS, PUSH, IN_APP }
}

// =============================================================================
// EC SAVE FOR LATER (already in EcCartItem.java file; moved here for clarity)
// =============================================================================

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
