package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.setup.entity.Bank;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "acc_chart_of_accounts_sub",
        indexes = {
                @Index(name = "idx_sub_org", columnList = "organization_id"),
                @Index(name = "idx_sub_type", columnList = "sub_account_type"),
                @Index(name = "idx_sub_main", columnList = "main_account_id"),
                @Index(name = "idx_sub_bank", columnList = "bank_id")
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

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

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

    @Builder.Default
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
    @Builder.Default
    @Column(nullable = false)
    private boolean requiresApproval = false;

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
    @Builder.Default
    private Integer loyaltyPoints = 0;
    @Builder.Default
    private Boolean isExportCustomer = false;

    // ── SUPPLIER-specific ───────────────────────────────────────────────────
    @Column(length = 50)
    private String supplierCode;
    private Integer leadTimeDays;
    @Column(columnDefinition = "text")
    private String certifications;
    @Builder.Default
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
}
