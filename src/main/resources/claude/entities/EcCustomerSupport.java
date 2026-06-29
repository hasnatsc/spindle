package com.asg.spindleserp.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// =============================================================================
// EC CUSTOMER ADDRESS
// =============================================================================

@Entity
@Table(name = "ec_customer_addresses",
        indexes = @Index(name = "idx_ec_custaddr_cust", columnList = "customer_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCustomerAddress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AddressType addressType;

    @Column(length = 200) private String contactPerson;
    @Column(length = 30)  private String contactPhone;

    @Builder.Default
    @Column(length = 100) private String country = "Bangladesh";

    @Column(length = 100) private String division;
    @Column(length = 100) private String district;
    @Column(length = 100) private String upazila;
    @Column(length = 20)  private String postCode;
    @Column(length = 150) private String area;
    @Column(length = 300) private String addressLine1;
    @Column(length = 300) private String addressLine2;
    @Column(length = 200) private String landmark;

    @Column(precision = 12, scale = 8) private BigDecimal latitude;
    @Column(precision = 12, scale = 8) private BigDecimal longitude;

    @Builder.Default @Column(nullable = false) private boolean defaultShipping = false;
    @Builder.Default @Column(nullable = false) private boolean defaultBilling  = false;
    @Builder.Default @Column(nullable = false) private boolean active = true;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum AddressType { HOME, OFFICE, BILLING, SHIPPING, OTHER }
}

// =============================================================================
// EC CUSTOMER WALLET  (immutable ledger — never UPDATE, only INSERT)
// Single source of truth for wallet balance.
// Balance = SUM(CASE WHEN transaction_type='CREDIT' THEN amount ELSE -amount END)
// balance_after is a snapshot for O(1) last-known-balance lookup.
// =============================================================================

@Entity
@Table(name = "ec_customer_wallet",
        indexes = {
                @Index(name = "idx_ec_wallet_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_wallet_ref",  columnList = "reference_type,reference_id"),
                @Index(name = "idx_ec_wallet_time", columnList = "created_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCustomerWallet {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType transactionType;

    // 'ORDER' | 'REFUND' | 'TOPUP' | 'ADJUSTMENT' | 'REWARD_REDEEM'
    @Column(length = 50) private String referenceType;
    private Long referenceId;

    @Column(length = 500) private String narration;

    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal balanceAfter;

    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
    @Column(length = 100) private String createdBy;

    public enum TransactionType { CREDIT, DEBIT }
}

// =============================================================================
// EC REWARD POINTS LEDGER
// =============================================================================

@Entity
@Table(name = "ec_reward_points",
        indexes = @Index(name = "idx_ec_reward_cust", columnList = "customer_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcRewardPoints {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @Column(length = 50) private String referenceType;
    private Long referenceId;

    @Column(nullable = false) private Integer points;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TransactionType transactionType;

    @Column(length = 500) private String remarks;

    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType { EARN, REDEEM, ADJUST }
}

// =============================================================================
// EC WISHLIST
// =============================================================================

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

// =============================================================================
// EC RECENTLY VIEWED
// customer_id nullable: supports anonymous browsing tracking via session_id
// =============================================================================

@Entity
@Table(name = "ec_recently_viewed",
        indexes = {
                @Index(name = "idx_ec_recent_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_recent_prod", columnList = "product_id"),
                @Index(name = "idx_ec_recent_time", columnList = "viewed_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcRecentlyViewed {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private EcProductCatalog product;

    @Builder.Default private LocalDateTime viewedAt = LocalDateTime.now();
    @Column(length = 50)  private String ipAddress;
    @Column(length = 255) private String deviceInfo;
}

// =============================================================================
// EC CUSTOMER LOGIN HISTORY
// =============================================================================

@Entity
@Table(name = "ec_customer_login_history",
        indexes = {
                @Index(name = "idx_ec_login_cust", columnList = "customer_id"),
                @Index(name = "idx_ec_login_time", columnList = "login_time")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCustomerLoginHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    @Column(length = 50)  private String ipAddress;
    @Column(length = 150) private String browser;
    @Column(length = 150) private String operatingSystem;
    @Column(length = 150) private String device;
    @Column(length = 20)  private String loginStatus;   // SUCCESS | FAILED
    @Column(length = 50)  private String loginSource;   // WEB | ANDROID | IOS
}

// =============================================================================
// EC CUSTOMER OTP
// customer_id nullable: OTP may be issued before customer row exists (registration)
// =============================================================================

@Entity
@Table(name = "ec_customer_otp",
        indexes = {
                @Index(name = "idx_ec_otp_cust",  columnList = "customer_id"),
                @Index(name = "idx_ec_otp_phone", columnList = "phone"),
                @Index(name = "idx_ec_otp_email", columnList = "email")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCustomerOtp {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private EcCustomer customer;

    @Column(length = 30)  private String phone;
    @Column(length = 200) private String email;
    @Column(length = 10)  private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OtpType otpType;

    private LocalDateTime expiryTime;

    @Builder.Default @Column(nullable = false) private boolean verified = false;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    public enum OtpType { LOGIN, REGISTER, FORGOT_PASSWORD, VERIFY_PHONE, VERIFY_EMAIL }
}

// =============================================================================
// EC CUSTOMER DEVICE  (push notification targets)
// =============================================================================

@Entity
@Table(name = "ec_customer_devices",
        indexes = @Index(name = "idx_ec_device_cust", columnList = "customer_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EcCustomerDevice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private EcCustomer customer;

    @Column(length = 200) private String deviceUuid;
    @Column(length = 200) private String deviceName;
    @Column(length = 50)  private String deviceType;
    @Column(length = 100) private String osName;
    @Column(length = 50)  private String appVersion;
    @Column(columnDefinition = "text") private String pushToken;
    private LocalDateTime lastActive;

    @Builder.Default @Column(nullable = false) private boolean active = true;
}
