// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  OPTIMUM ERP — E06  Accounts / Finance                                   ║
// ║  Tables: acc_chart_of_accounts, acc_chart_of_accounts_sub (STI),         ║
// ║           acc_periods, acc_opening_balances,                             ║
// ║           acc_journal_entry_master, acc_journal_entry_lines (★ new),     ║
// ║           acc_mapping, acc_mapping_details, acc_policy,                  ║
// ║           acc_auto_journal_templates, acc_auto_journal_template_lines    ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ── FILE: accounts/entity/ChartOfAccount.java ────────────────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "acc_chart_of_accounts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_coa_code",     columnNames = "account_code"),
        @UniqueConstraint(name = "uq_coa_org_code", columnNames = {"organization_id","account_code"})
    },
    indexes = {
        @Index(name = "idx_coa_org",    columnList = "organization_id"),
        @Index(name = "idx_coa_parent", columnList = "parent_account_id"),
        @Index(name = "idx_coa_type",   columnList = "account_type")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChartOfAccount extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private ChartOfAccount parentAccount;

    @Column(nullable = false, unique = true, length = 50)
    private String accountCode;

    @Column(nullable = false, length = 200)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountNature accountNature;

    @Builder.Default @Column(nullable = false) private int level = 1;

    @Column(precision = 18, scale = 2) private BigDecimal openingBalance;
    @Column(precision = 18, scale = 2) private BigDecimal currentBalance;
    @Column(length = 10) private String currency;
    @Column(length = 1000) private String description;
    @Column(length = 50) private String taxId;

    @Builder.Default @Column(nullable = false) private boolean isActive         = true;
    @Builder.Default @Column(nullable = false) private boolean isSystem         = false;
    @Builder.Default @Column(nullable = false) private boolean isControlAccount = false;
    @Builder.Default @Column(nullable = false) private boolean allowManualEntry = true;

    public enum AccountType   { ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE }
    public enum AccountNature { DEBIT, CREDIT }
}


// ── FILE: accounts/entity/ChartOfAccountSub.java ─────────────────────────────
// Single-Table Inheritance — discriminator: sub_account_type
// Subtypes: BankAccount, CashAccount, LetterOfCredit, CustomerAccount,
//            SupplierAccount, EmployeeAccount, GeneralSubAccount, InterCompanyAccount
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.setup.entity.Bank;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "acc_chart_of_accounts_sub",
    indexes = {
        @Index(name = "idx_sub_org",  columnList = "organization_id"),
        @Index(name = "idx_sub_type", columnList = "sub_account_type"),
        @Index(name = "idx_sub_main", columnList = "main_account_id"),
        @Index(name = "idx_sub_bank", columnList = "bank_id")
    })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "sub_account_type", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public abstract class ChartOfAccountSub extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(precision = 18, scale = 2) private BigDecimal openingBalance;
    @Column(precision = 18, scale = 2) private BigDecimal currentBalance;
    @Column(length = 20) private String currency;
    @Column(length = 1000) private String description;
    @Column(length = 200) private String contactPerson;
    @Column(length = 20)  private String contactPhone;
    @Column(length = 100) private String contactEmail;
    @Column(length = 500) private String address;
    @Column(length = 50)  private String city;
    @Column(length = 50)  private String state;
    @Column(length = 50)  private String country;
    @Column(length = 20)  private String postalCode;
    @Column(length = 50)  private String taxId;
    @Column(length = 50)  private String vatRegistrationNo;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @Column(length = 1000) private String remarks;

    // ── BANK-specific ───────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bank_id") private Bank bank;
    @Column(unique = true, length = 50) private String bankAccountCode;
    @Column(unique = true, length = 50) private String accountNumber;
    @Column(length = 200) private String accountTitle;
    @Column(length = 200) private String bankName;
    @Column(length = 20)  private String bankAccountType;
    @Column(length = 100) private String branchName;
    @Column(length = 10)  private String branchCode;
    @Column(length = 200) private String branchAddress;
    @Column(length = 20)  private String branchPhone;
    @Column(length = 9)   private String routingNumber;
    @Column(length = 11)  private String swiftCode;
    @Column(length = 34)  private String ibanNumber;
    @Column(precision = 8, scale = 4) private BigDecimal interestRate;
    @Column(precision = 18, scale = 2) private BigDecimal overdraftLimit;
    @Column(precision = 8, scale = 4) private BigDecimal overdraftInterestRate;
    @Column(name = "bank_account_ledger_id") private Long bankAccountLedgerId;

    // ── CASH-specific ───────────────────────────────────────────────────────
    @Column(unique = true, length = 50) private String cashAccountCode;
    @Column(length = 20)  private String cashAccountType;
    @Column(length = 100) private String location;
    @Column(length = 100) private String custodian;
    @Column(length = 100) private String custodianEmail;
    @Column(length = 20)  private String custodianPhone;
    @Column(precision = 18, scale = 2) private BigDecimal maximumLimit;
    @Column(precision = 18, scale = 2) private BigDecimal minimumLimit;
    @Column(precision = 18, scale = 2) private BigDecimal approvalLimit;
    @Builder.Default @Column(nullable = false) private boolean requiresApproval = false;

    // ── CUSTOMER-specific ───────────────────────────────────────────────────
    @Column(length = 50) private String customerCode;
    @Column(precision = 18, scale = 2) private BigDecimal creditLimit;
    @Column(length = 100) private String paymentTerms;
    private Integer creditDays;
    @Column(length = 100) private String salesRepresentative;
    @Column(length = 50)  private String customerGroup;
    @Builder.Default private Integer loyaltyPoints = 0;
    @Builder.Default private Boolean isExportCustomer = false;

    // ── SUPPLIER-specific ───────────────────────────────────────────────────
    @Column(length = 50) private String supplierCode;
    private Integer leadTimeDays;
    @Column(columnDefinition = "text") private String certifications;
    @Builder.Default private Boolean isImportSupplier = false;
    @Column(length = 3) private String preferredCurrency;

    // ── LC-specific ─────────────────────────────────────────────────────────
    @Column(unique = true, length = 100) private String lcNumber;
    @Column(length = 100) private String manualLcNumber;
    @Column(length = 30) private String lcType;
    @Column(length = 30) private String lcStatus;
    @Column(length = 20) private String transactionCurrency;
    @Column(precision = 18, scale = 2) private BigDecimal lcAmount;
    @Column(precision = 18, scale = 4) private BigDecimal exchangeRate;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private LocalDate shipmentDate;
    private LocalDate receivingDate;
    private Integer tenureDays;
    @Column(length = 100) private String masterLcNo;
    @Column(length = 100) private String btbLcNo;
    private LocalDate masterLcDate;
    @Column(length = 30) private String paymentTerm;
    @Column(length = 20) private String shipmentMode;
    private Boolean partialShipmentAllowed;
    private Boolean btmaCertificateRequired;
    @Column(columnDefinition = "text") private String termsCondition;
    @Column(name = "margin_account_id") private Long marginAccountId;
    @Column(name = "beneficiary_bank_id") private Long beneficiaryBankId;
    @Column(name = "buyer_bank_id") private Long buyerBankId;
    @Column(name = "foreign_bank_id") private Long foreignBankId;
    @Column(name = "beneficiary_bank_account_id") private Long beneficiaryBankAccountId;
    @Column(name = "buyer_bank_account_id")       private Long buyerBankAccountId;
    // stub FKs — resolved at application layer
    private Long customerId;
    private Long supplierId;
}

// ── STI subclasses ─────────────────────────────────────────────────────────
@Entity @DiscriminatorValue("BANK")
class BankAccount extends ChartOfAccountSub {
    @Builder public BankAccount() { super(); }
}

@Entity @DiscriminatorValue("CASH")
class CashAccount extends ChartOfAccountSub {
    @Builder public CashAccount() { super(); }
}

@Entity @DiscriminatorValue("LC")
class LetterOfCredit extends ChartOfAccountSub {
    @Builder public LetterOfCredit() { super(); }
}

@Entity @DiscriminatorValue("CUSTOMER")
class CustomerAccount extends ChartOfAccountSub {
    @Builder public CustomerAccount() { super(); }
}

@Entity @DiscriminatorValue("SUPPLIER")
class SupplierAccount extends ChartOfAccountSub {
    @Builder public SupplierAccount() { super(); }
}

@Entity @DiscriminatorValue("EMPLOYEE")
class EmployeeSubAccount extends ChartOfAccountSub {
    @Builder public EmployeeSubAccount() { super(); }
}

@Entity @DiscriminatorValue("GENERAL")
class GeneralSubAccount extends ChartOfAccountSub {
    @Builder public GeneralSubAccount() { super(); }
}

@Entity @DiscriminatorValue("INTER_COMPANY")
class InterCompanyAccount extends ChartOfAccountSub {
    @Builder public InterCompanyAccount() { super(); }
}


// ── FILE: accounts/entity/AccountingPeriod.java ──────────────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "acc_periods",
    indexes = @Index(name = "idx_period_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountingPeriod extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, unique = true, length = 50)
    private String periodName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodType periodType;

    @Column(nullable = false) private int fiscalYear;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(length = 1000) private String description;

    @Builder.Default @Column(nullable = false) private boolean isActive = true;
    @Builder.Default @Column(nullable = false) private boolean isClosed = false;

    @Column(length = 100) private String closedBy;
    private LocalDate closedDate;

    public enum PeriodType { DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY, CUSTOM }
}


// ── FILE: accounts/entity/OpeningBalance.java ────────────────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "acc_opening_balances",
    indexes = @Index(name = "idx_ob_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OpeningBalance extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accounting_period_id", nullable = false)
    private AccountingPeriod accountingPeriod;

    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal openingDebitBalance  = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal openingCreditBalance = BigDecimal.ZERO;

    @Column(length = 50) private String balanceType;
    private Boolean isActive;

    @Builder.Default @Column(nullable = false) private boolean isPosted = false;
    @Column(length = 100) private String postedBy;
    private LocalDate postedDate;
    @Column(length = 1000) private String remarks;
}


// ── FILE: accounts/entity/JournalEntryMaster.java ────────────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.common.enums.VoucherType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acc_journal_entry_master",
    indexes = {
        @Index(name = "idx_jem_org",    columnList = "organization_id"),
        @Index(name = "idx_jem_date",   columnList = "voucher_date"),
        @Index(name = "idx_jem_type",   columnList = "voucher_type"),
        @Index(name = "idx_jem_posted", columnList = "is_posted"),
        @Index(name = "idx_jem_no",     columnList = "voucher_no")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntryMaster extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(length = 100)   private String voucherNo;
    private LocalDate       voucherDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)    private VoucherType voucherType;

    @Column(precision = 18, scale = 2) private BigDecimal totalDebit;
    @Column(precision = 18, scale = 2) private BigDecimal totalCredit;
    @Column(length = 1000)  private String narration;
    @Column(length = 100)   private String referenceNo;

    @Builder.Default @Column(nullable = false) private boolean isPosted = false;
    @Column(length = 100)   private String postedBy;
    private LocalDateTime   postedAt;

    @Builder.Default
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines = new ArrayList<>();
}


// ── FILE: accounts/entity/JournalEntryLine.java (★ NEW — was missing) ────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "acc_journal_entry_lines",
    indexes = {
        @Index(name = "idx_jel_journal", columnList = "journal_entry_id"),
        @Index(name = "idx_jel_account", columnList = "account_id"),
        @Index(name = "idx_jel_sub",     columnList = "sub_account_id"),
        @Index(name = "idx_jel_cc",      columnList = "cost_center_id"),
        @Index(name = "idx_jel_org",     columnList = "organization_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntryLine extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryMaster journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_account_id")
    private ChartOfAccountSub subAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(nullable = false)
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType entryType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 500) private String narration;
    @Column(length = 100) private String referenceNo;
    @Column(length = 20)  private String taxCode;

    @Builder.Default @Column(nullable = false) private boolean isTaxLine = false;

    public enum EntryType { DEBIT, CREDIT }
}


// ── FILE: accounts/entity/AccountsMapping.java ───────────────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acc_mapping",
    uniqueConstraints = @UniqueConstraint(name = "uq_mapping_org_code",
        columnNames = {"organization_id","mapping_code"}),
    indexes = @Index(name = "idx_mapping_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountsMapping extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 30)  private String mappingCode;
    @Column(nullable = false, length = 200) private String mappingName;
    @Column(nullable = false, length = 50)  private String moduleType;
    @Column(nullable = false, length = 50)  private String transactionType;
    @Column(length = 500) private String description;
    @Column(length = 30)  private String defaultVoucherType;
    @Column(length = 30)  private String voucherType;
    @Column(length = 20)  private String voucherPrefix;

    @Builder.Default @Column(nullable = false, length = 30) private String debitControlType  = "NONE";
    @Builder.Default @Column(nullable = false, length = 30) private String creditControlType = "NONE";

    @Column(length = 500) private String defaultNarrationTemplate;

    @Builder.Default @Column(nullable = false) private boolean useSubLedger            = false;
    @Builder.Default @Column(nullable = false) private boolean updateSubAccountBalance  = false;
    @Builder.Default @Column(nullable = false) private boolean allowPartialPosting      = false;
    @Builder.Default @Column(nullable = false) private boolean autoPost                 = false;
    @Builder.Default @Column(nullable = false) private boolean consolidateEntries        = false;
    @Builder.Default @Column(nullable = false) private boolean createReversingEntry      = false;
    @Builder.Default @Column(nullable = false) private boolean requireApproval           = false;
    @Builder.Default @Column(nullable = false) private boolean isActive                  = true;
    @Builder.Default @Column(nullable = false) private boolean isDefault                 = false;
    @Builder.Default @Column(nullable = false) private boolean isSystem                  = false;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "default_debit_account_id")  private ChartOfAccount defaultDebitAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "default_credit_account_id") private ChartOfAccount defaultCreditAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "discount_account_id")       private ChartOfAccount discountAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "freight_account_id")        private ChartOfAccount freightAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "input_vat_account_id")      private ChartOfAccount inputVatAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "output_vat_account_id")     private ChartOfAccount outputVatAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "forex_gain_account_id")     private ChartOfAccount forexGainAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "forex_loss_account_id")     private ChartOfAccount forexLossAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "tds_account_id")            private ChartOfAccount tdsAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ait_account_id")            private ChartOfAccount aitAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rounding_account_id")       private ChartOfAccount roundingAccount;

    @Builder.Default
    @OneToMany(mappedBy = "accountsMapping", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountsMappingDetail> details = new ArrayList<>();
}


// ── FILE: accounts/entity/AccountsMappingDetail.java ─────────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "acc_mapping_details",
    indexes = @Index(name = "idx_amd_mapping", columnList = "accounts_mapping_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountsMappingDetail extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accounts_mapping_id", nullable = false)
    private AccountsMapping accountsMapping;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id")    private ChartOfAccount account;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cost_center_id") private CostCenter costCenter;

    @Column(nullable = false) private Integer lineNumber;
    @Column(length = 100) private String entryName;
    @Column(length = 500) private String entryDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JournalEntryLine.EntryType entryType;

    @Column(nullable = false, length = 30) private String amountType;
    @Column(precision = 8, scale = 4) private BigDecimal percentage;
    @Column(precision = 18, scale = 2) private BigDecimal fixedAmount;
    @Column(length = 500) private String formula;
    @Column(length = 100) private String fieldReference;
    @Column(length = 500) private String condition;
    @Column(length = 20)  private String conditionOperator;
    @Column(length = 200) private String conditionValue;
    @Builder.Default @Column(length = 30) private String controlAccountType = "NONE";
    @Column(length = 500) private String lineNarration;
    @Column(length = 20)  private String taxCode;
    @Column(precision = 8, scale = 4) private BigDecimal taxRate;

    @Builder.Default @Column(nullable = false) private int     sortOrder         = 0;
    @Builder.Default @Column(nullable = false) private boolean negateAmount       = false;
    @Builder.Default @Column(nullable = false) private boolean roundAmount        = false;
    @Builder.Default @Column(nullable = false) private boolean skipIfZero         = false;
    @Builder.Default @Column(nullable = false) private boolean isTaxEntry         = false;
    @Builder.Default @Column(nullable = false) private boolean isOptional         = false;
    @Builder.Default @Column(nullable = false) private boolean isActive           = true;
    @Builder.Default @Column(nullable = false) private boolean inheritCostCenter  = false;
}


// ── FILE: accounts/entity/AccountsPolicy.java ────────────────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "acc_policy",
    uniqueConstraints = @UniqueConstraint(name = "uq_policy_org_code",
        columnNames = {"organization_id","policy_code"}),
    indexes = @Index(name = "idx_policy_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountsPolicy extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "accounts_mapping_id")
    private AccountsMapping accountsMapping;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "default_debit_account_id")
    private ChartOfAccount defaultDebitAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "default_credit_account_id")
    private ChartOfAccount defaultCreditAccount;

    @Column(nullable = false, length = 30)  private String policyCode;
    @Column(nullable = false, length = 200) private String policyName;
    @Column(nullable = false, length = 30)  private String policyType;
    @Column(length = 30)  private String moduleType;
    @Column(length = 500) private String description;
    @Column(length = 20)  private String voucherPrefix;
    private Integer nextVoucherNumber;
    @Builder.Default private Integer numberPadding = 6;

    @Builder.Default @Column(nullable = false) private boolean autoNumbering        = true;
    @Builder.Default @Column(nullable = false) private boolean autoPost             = false;
    @Builder.Default @Column(nullable = false) private boolean requireApproval      = false;
    @Builder.Default @Column(nullable = false) private boolean allowBackdating      = false;
    private Integer backdatingDays;
    @Builder.Default @Column(nullable = false) private boolean allowFutureDating    = false;
    @Builder.Default @Column(nullable = false) private boolean allowEditAfterPost   = false;
    @Builder.Default @Column(nullable = false) private boolean allowReversal        = true;
    @Builder.Default @Column(nullable = false) private boolean allowNegativeAmount  = false;
    @Builder.Default @Column(nullable = false) private boolean allowZeroAmount      = false;

    @Column(precision = 18, scale = 2) private BigDecimal approvalThreshold;
    @Column(precision = 18, scale = 2) private BigDecimal minimumAmount;
    @Column(precision = 18, scale = 2) private BigDecimal maximumAmount;
    @Column(length = 500) private String defaultNarrationTemplate;

    @Builder.Default @Column(nullable = false) private boolean isActive  = true;
    @Builder.Default @Column(nullable = false) private boolean isDefault = false;
    @Builder.Default @Column(nullable = false) private boolean isSystem  = false;
}


// ── FILE: accounts/entity/AutoJournalTemplate.java ───────────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acc_auto_journal_templates",
    uniqueConstraints = @UniqueConstraint(name = "uq_ajt_org_code",
        columnNames = {"organization_id","template_code"}),
    indexes = @Index(name = "idx_ajt_org", columnList = "organization_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AutoJournalTemplate extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "accounts_policy_id")
    private AccountsPolicy accountsPolicy;

    @Column(nullable = false, length = 30)  private String templateCode;
    @Column(nullable = false, length = 200) private String templateName;
    @Column(nullable = false, length = 30)  private String moduleType;
    @Column(nullable = false, length = 50)  private String transactionType;
    @Column(length = 30)  private String voucherType;
    @Builder.Default @Column(nullable = false, length = 30) private String triggerMode = "MANUAL";
    @Column(length = 50)  private String triggerEvent;
    @Column(length = 500) private String narrationTemplate;
    @Column(length = 500) private String description;

    @Builder.Default @Column(nullable = false) private boolean autoPost       = false;
    @Builder.Default @Column(nullable = false) private boolean validateBalance = false;
    @Builder.Default @Column(nullable = false) private boolean isActive        = true;
    @Builder.Default @Column(nullable = false) private boolean isSystem        = false;

    private LocalDateTime lastUsedAt;
    @Builder.Default @Column(nullable = false) private int usageCount = 0;

    @Builder.Default
    @OneToMany(mappedBy = "autoJournalTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AutoJournalTemplateLine> lines = new ArrayList<>();
}


// ── FILE: accounts/entity/AutoJournalTemplateLine.java ───────────────────────
package com.hasnat.optimum.accounts.entity;

import com.hasnat.optimum.common.entity.BaseEntity;
import com.hasnat.optimum.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "acc_auto_journal_template_lines",
    indexes = @Index(name = "idx_ajtl_template", columnList = "auto_journal_template_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AutoJournalTemplateLine extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auto_journal_template_id", nullable = false)
    private AutoJournalTemplate autoJournalTemplate;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id")         private ChartOfAccount account;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "control_account_id") private ChartOfAccount controlAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cost_center_id")     private CostCenter costCenter;

    @Column(nullable = false) private Integer lineNumber;
    @Column(length = 100) private String lineName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JournalEntryLine.EntryType entryType;

    @Builder.Default @Column(nullable = false, length = 30) private String amountMode = "FIELD_REFERENCE";
    @Column(precision = 8, scale = 4) private BigDecimal amountPercentage;
    @Column(precision = 18, scale = 2) private BigDecimal fixedAmount;
    @Column(length = 500) private String formula;
    @Column(length = 100) private String fieldReference;
    @Column(length = 30)  private String controlAccountType;
    @Column(length = 500) private String lineNarration;
    @Column(length = 20)  private String taxCode;
    @Column(precision = 8, scale = 4) private BigDecimal taxRate;

    @Builder.Default @Column(nullable = false) private int     sortOrder   = 0;
    @Builder.Default @Column(nullable = false) private boolean negateAmount = false;
    @Builder.Default @Column(nullable = false) private boolean skipIfZero  = false;
    @Builder.Default @Column(nullable = false) private boolean isTaxLine   = false;
    @Builder.Default @Column(nullable = false) private boolean isOptional  = false;
    @Builder.Default @Column(nullable = false) private boolean isActive    = true;
}
