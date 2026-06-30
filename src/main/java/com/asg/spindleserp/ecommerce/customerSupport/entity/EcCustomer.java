package com.asg.spindleserp.ecommerce.customerSupport.entity;

import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * EcCustomer — B2C portal registrations.
 *
 * Does NOT extend BaseEntity.  Reason: customers self-register from the
 * storefront — there is no ERP session context at registration time.
 * Mirrors the ItemUom / InventoryLot pattern: organizationId stored as
 * plain Long, set server-side from SecurityHelper.requireOrgId().
 *
 * erp_sub_account_id (nullable):
 *   NULL = portal-only customer (no ERP AR sub-ledger yet)
 *   Set  = CustomerAccount (discriminator "CUSTOMER") linked for AR posting.
 *   Populated by EcCustomerService.linkToErp() after B2B onboarding.
 *
 * wallet_balance NOT stored here — single source of truth is ec_customer_wallet.
 * Service query: SELECT SUM(CASE WHEN transaction_type='CREDIT'
 *                               THEN amount ELSE -amount END)
 *                FROM ec_customer_wallet WHERE customer_id = ?
 *
 * total_orders / total_purchase: cached counters, updated on each order save.
 * reward_points: cached counter, updated on each ec_reward_points insert.
 */
@Entity
@Table(name = "ec_customers",
        uniqueConstraints =
            @UniqueConstraint(name = "uq_ec_customer_code",
                    columnNames = {"organization_id", "customer_code"}),
        indexes = {
                @Index(name = "idx_ec_cust_org",    columnList = "organization_id"),
                @Index(name = "idx_ec_cust_email",  columnList = "email"),
                @Index(name = "idx_ec_cust_phone",  columnList = "phone"),
                @Index(name = "idx_ec_cust_status", columnList = "account_status"),
                @Index(name = "idx_ec_cust_sub",    columnList = "erp_sub_account_id"),
                @Index(name = "idx_ec_cust_user",   columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Set server-side: SecurityHelper.requireOrgId()
    @Builder.Default
    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId = ContextProvider.getOrganizationId();

    // Bridge to ERP sec_users (B2B portal SSO)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Bridge to ERP AR sub-ledger (CustomerAccount discriminator)
    // acc_chart_of_accounts_sub where sub_account_type = 'CUSTOMER'
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "erp_sub_account_id")
    private ChartOfAccountSub erpSubAccount;

    // ── Identity ──────────────────────────────────────────────────────────
    @Column(nullable = false, length = 50)
    private String customerCode;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 200)
    private String fullName;

    @Column(length = 200)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    private LocalDate dateOfBirth;

    @Column(length = 500)
    private String profileImage;

    // ── B2B fields ────────────────────────────────────────────────────────
    @Column(length = 200)
    private String companyName;

    @Column(length = 100)
    private String taxNumber;

    @Column(length = 100)
    private String nationalId;

    // ── Verification ──────────────────────────────────────────────────────
    @Builder.Default
    @Column(nullable = false)
    private Boolean emailVerified = Boolean.FALSE;

    @Builder.Default
    @Column(nullable = false)
    private Boolean phoneVerified = Boolean.FALSE;

    // ── Status ────────────────────────────────────────────────────────────
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.PENDING;

    @Column(length = 50)
    private String customerGroup;

    // ── Cached counters (updated by service, not triggers) ────────────────
    @Builder.Default
    @Column(nullable = false)
    private Integer totalOrders = 0;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPurchase = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private Integer rewardPoints = 0;

    // wallet_balance is NOT stored — computed from ec_customer_wallet ledger

    // ── Timestamps ────────────────────────────────────────────────────────
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── Enums ─────────────────────────────────────────────────────────────

    public enum Gender { MALE, FEMALE, OTHER }

    public enum AccountStatus { ACTIVE, BLOCKED, PENDING, DELETED }
}
