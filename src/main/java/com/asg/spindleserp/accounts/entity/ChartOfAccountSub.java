package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.setup.entity.Bank;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "acc_chart_of_accounts_sub",
        indexes = {
                @Index(name = "idx_sub_org", columnList = "organization_id"),
                @Index(name = "idx_sub_type", columnList = "sub_account_type"),
                @Index(name = "idx_sub_main", columnList = "main_account_id"),
                @Index(name = "idx_sub_bank", columnList = "bank_id"),
                @Index(name = "idx_sub_org_type", columnList = "organization_id,sub_account_type")
        })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "sub_account_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class ChartOfAccountSub extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "main_account_id", nullable = false)
    private ChartOfAccount mainAccount;

    @Column(nullable = false, unique = true, length = 50)
    private String subAccountCode;

    @Column(nullable = false, length = 200)
    private String subAccountName;

    @Column(precision = 18, scale = 2)
    private BigDecimal openingBalance;
    @Column(precision = 18, scale = 2)
    private BigDecimal currentBalance;
    @Column(length = 20)
    private String currency;
    @Column(length = 1000)
    private String description;
    @Column(length = 200)
    private String contactPerson;
    @Column(length = 20)
    private String contactPhone;
    @Column(length = 100)
    private String contactEmail;
    @Column(length = 500)
    private String address;
    @Column(length = 50)
    private String city;
    @Column(length = 50)
    private String state;
    @Column(length = 50)
    private String country;
    @Column(length = 20)
    private String postalCode;
    @Column(length = 50)
    private String taxId;
    @Column(length = 50)
    private String vatRegistrationNo;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(length = 1000)
    private String remarks;

    // ── BANK-specific ───────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;
    @Column(unique = true, length = 50)
    private String bankAccountCode;
    @Column(unique = true, length = 50)
    private String accountNumber;
    @Column(length = 200)
    private String accountTitle;
    @Column(length = 200)
    private String bankName;
    @Column(length = 20)
    private String bankAccountType;
    @Column(length = 100)
    private String branchName;
    @Column(length = 10)
    private String branchCode;
    @Column(length = 200)
    private String branchAddress;
    @Column(length = 20)
    private String branchPhone;
    @Column(length = 9)
    private String routingNumber;
    @Column(length = 11)
    private String swiftCode;
    @Column(length = 34)
    private String ibanNumber;
    @Column(precision = 8, scale = 4)
    private BigDecimal interestRate;
    @Column(precision = 18, scale = 2)
    private BigDecimal overdraftLimit;
    @Column(precision = 8, scale = 4)
    private BigDecimal overdraftInterestRate;
    @Column(name = "bank_account_ledger_id")
    private Long bankAccountLedgerId;

    // ── CASH-specific ───────────────────────────────────────────────────────
    @Column(unique = true, length = 50)
    private String cashAccountCode;
    @Column(length = 20)
    private String cashAccountType;
    @Column(length = 100)
    private String location;
    @Column(length = 100)
    private String custodian;
    @Column(length = 100)
    private String custodianEmail;
    @Column(length = 20)
    private String custodianPhone;
    @Column(precision = 18, scale = 2)
    private BigDecimal maximumLimit;
    @Column(precision = 18, scale = 2)
    private BigDecimal minimumLimit;
    @Column(precision = 18, scale = 2)
    private BigDecimal approvalLimit;
    @Column(nullable = false)
    private boolean requiresApproval = false;

    // ── MOBILE_BANKING-specific (bKash / Nagad / Rocket / Upay) ─────────────
    /** BKASH | NAGAD | ROCKET | UPAY | TAP — free text so new MFS need no code change. */
    @Column(length = 30)
    private String mfsProvider;
    /** Registered MSISDN, e.g. 01XXXXXXXXX. */
    @Column(length = 20)
    private String mfsAccountNumber;
    /** PERSONAL | AGENT | MERCHANT | DISBURSEMENT. */
    @Column(length = 20)
    private String mfsAccountType;
    /** Merchant / short code assigned by the MFS provider. */
    @Column(length = 50)
    private String merchantNumber;
    @Column(length = 20)
    private String mfsShortCode;
    /** Cash-out / merchant charge as a percentage, e.g. 1.4900. */
    @Column(precision = 8, scale = 4)
    private BigDecimal mfsChargeRate;
    @Column(precision = 18, scale = 2)
    private BigDecimal dailyTransactionLimit;

    // ── CARD-specific (POS acquiring) ───────────────────────────────────────
    /** VISA | MASTERCARD | AMEX | NEXUS | UNIONPAY. */
    @Column(length = 30)
    private String cardNetwork;
    /** Acquiring bank — stub FK, resolved at application layer. */
    @Column(name = "card_acquirer_bank_id")
    private Long cardAcquirerBankId;
    @Column(length = 50)
    private String terminalId;
    @Column(length = 50)
    private String merchantId;
    @Column(length = 50)
    private String posSerialNumber;
    /** T+N days until the acquirer settles into the bank account. */
    private Integer settlementDays;
    /** Merchant Discount Rate as a percentage, e.g. 2.5000. */
    @Column(precision = 8, scale = 4)
    private BigDecimal mdrRate;

    // ── WALLET-specific (closed-loop / store credit / gift card) ────────────
    @Column(length = 50)
    private String walletProvider;
    @Column(length = 100)
    private String walletIdentifier;
    /** PREPAID | STORE_CREDIT | LOYALTY | GIFT_CARD. */
    @Column(length = 30)
    private String walletType;

    /**
     * Bank sub-account this instrument finally settles into (CARD acquiring,
     * MFS merchant sweep). Stub FK to acc_chart_of_accounts_sub.id — resolved
     * at application layer, mirroring the existing LC-block convention.
     */
    @Column(name = "settlement_account_id")
    private Long settlementAccountId;

    // ── CUSTOMER-specific ───────────────────────────────────────────────────
    @Column(length = 50)
    private String customerCode;
    @Column(precision = 18, scale = 2)
    private BigDecimal creditLimit;
    @Column(length = 100)
    private String paymentTerms;
    private Integer creditDays;
    @Column(length = 100)
    private String salesRepresentative;
    @Column(length = 50)
    private String customerGroup;
    private Integer loyaltyPoints = 0;
    private Boolean isExportCustomer = false;

    // ── SUPPLIER-specific ───────────────────────────────────────────────────
    @Column(length = 50)
    private String supplierCode;
    private Integer leadTimeDays;
    @Column(columnDefinition = "text")
    private String certifications;
    private Boolean isImportSupplier = false;
    @Column(length = 3)
    private String preferredCurrency;

    // ── LC-specific ─────────────────────────────────────────────────────────
    @Column(unique = true, length = 100)
    private String lcNumber;
    @Column(length = 100)
    private String manualLcNumber;
    @Column(length = 30)
    private String lcType;
    @Column(length = 30)
    private String lcStatus;
    @Column(length = 20)
    private String transactionCurrency;
    @Column(precision = 18, scale = 2)
    private BigDecimal lcAmount;
    @Column(precision = 18, scale = 4)
    private BigDecimal exchangeRate;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private LocalDate shipmentDate;
    private LocalDate receivingDate;
    private Integer tenureDays;
    @Column(length = 100)
    private String masterLcNo;
    @Column(length = 100)
    private String btbLcNo;
    private LocalDate masterLcDate;
    @Column(length = 30)
    private String paymentTerm;
    @Column(length = 20)
    private String shipmentMode;
    private Boolean partialShipmentAllowed;
    private Boolean btmaCertificateRequired;
    @Column(columnDefinition = "text")
    private String termsCondition;
    @Column(name = "margin_account_id")
    private Long marginAccountId;
    @Column(name = "beneficiary_bank_id")
    private Long beneficiaryBankId;
    @Column(name = "buyer_bank_id")
    private Long buyerBankId;
    @Column(name = "foreign_bank_id")
    private Long foreignBankId;
    @Column(name = "beneficiary_bank_account_id")
    private Long beneficiaryBankAccountId;
    @Column(name = "buyer_bank_account_id")
    private Long buyerBankAccountId;
    // stub FKs — resolved at application layer
    private Long customerId;
    private Long supplierId;

    /**
     * The discriminator column mapped read-only. Two reasons this matters:
     * <ol>
     *   <li>It makes sub_account_type usable in JPQL and Spring Data derived
     *       queries — {@code findByOrganizationIdAndSubAccountType("MOBILE_BANKING")} —
     *       without a polymorphic {@code TYPE()} expression.</li>
     *   <li>Reading the type off a LAZY proxy via {@code getClass()} returns the
     *       proxy subclass, which carries no {@code @DiscriminatorValue}. This
     *       column always holds the truth.</li>
     * </ol>
     * Length 31 matches Hibernate's default {@code @DiscriminatorColumn} width.
     */
    @Column(name = "sub_account_type", insertable = false, updatable = false, length = 31)
    private String subAccountType;

    /**
     * Discriminator value for this row. Prefers the persisted column; falls back
     * to the class annotation for entities not yet flushed to the DB.
     */
    @Transient
    public String getSubAccountTypeCode() {
        if (subAccountType != null && !subAccountType.isBlank()) {
            return subAccountType;
        }
        DiscriminatorValue dv = this.getClass().getAnnotation(DiscriminatorValue.class);
        return dv != null ? dv.value() : null;
    }

    /**
     * Sub-ledger partitions. Names are the literal {@code @DiscriminatorValue}s
     * stored in {@code acc_chart_of_accounts_sub.sub_account_type}.
     * <p>
     * GENERAL and INTER_COMPANY were already in use as discriminator values
     * (GeneralSubAccount, InterCompanyAccount) but were missing from this enum —
     * added here so the set is complete.
     * <p>
     * The first five names match {@code PaymentMode.AccountCategory} 1:1.
     */
    @Getter
    public enum SubAccountType {

        CASH           ("Cash In Hand"),
        BANK           ("Bank Accounts"),
        MOBILE_BANKING ("Mobile Banking"),
        CARD           ("Card Accounts"),
        WALLET         ("Wallets"),
        CUSTOMER       ("Customers"),
        SUPPLIER       ("Suppliers"),
        EMPLOYEE       ("Employees"),
        LC             ("Letters of Credit"),
        GENERAL        ("General"),
        INTER_COMPANY  ("Inter Company");

        private final String label;

        SubAccountType(String label) {
            this.label = label;
        }

        public static SubAccountType fromCode(String code) {
            if (code == null) return null;
            String k = code.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            for (SubAccountType t : values()) {
                if (t.name().equals(k)) return t;
            }
            return null;
        }

        /** True for the partitions a PaymentAccount is allowed to point at. */
        public boolean isPaymentPartition() {
            return this == CASH || this == BANK || this == MOBILE_BANKING
                    || this == CARD || this == WALLET;
        }
    }
}
