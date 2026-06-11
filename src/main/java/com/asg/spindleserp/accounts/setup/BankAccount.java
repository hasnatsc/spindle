package com.asg.spindleserp.accounts.setup;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "acc_bank_accounts")
@DiscriminatorValue("BANK")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class BankAccount extends SubAccount {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    @Column(name = "bank_account_code", nullable = false, unique = true, length = 50)
    private String bankAccountCode;
    @Column(name = "branch_name", nullable = false, length = 100)
    private String branchName;
    @Column(name = "branch_code", length = 10)
    private String branchCode;
    @Column(name = "branch_address", length = 200)
    private String branchAddress;
    @Column(name = "account_number", nullable = false, unique = true, length = 100)
    private String accountNumber;
    @Column(name = "account_title", length = 200)
    private String accountTitle;
    @Column(name = "bank_account_type", length = 30)
    private String bankAccountType;
    // SAVINGS|CURRENT|FIXED_DEPOSIT|OVERDRAFT|LOAN|ESCROW|NOSTRO|VOSTRO|CREDIT_CARD

    @Column(name = "opening_date")
    private LocalDate openingDate;
    @Column(name = "closing_date")
    private LocalDate closingDate;
    @Column(name = "credit_limit", precision = 18, scale = 2)
    private BigDecimal creditLimit;
    @Column(name = "overdraft_limit", precision = 18, scale = 2)
    private BigDecimal overdraftLimit;
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Builder.Default
    @Column(name = "supports_lc")
    private Boolean supportsLc = false;
    @Builder.Default
    @Column(name = "supports_import_lc")
    private Boolean supportsImportLc = false;
    @Builder.Default
    @Column(name = "supports_export_lc")
    private Boolean supportsExportLc = false;
    @Builder.Default
    @Column(name = "supports_btb_lc")
    private Boolean supportsBtbLc = false;
    @Builder.Default
    @Column(name = "requires_approval")
    private Boolean requiresApproval = false;
    @Column(name = "approval_limit", precision = 18, scale = 2)
    private BigDecimal approvalLimit;
    @Builder.Default
    @Column(name = "is_default_payment_account")
    private Boolean isDefaultPaymentAccount = false;
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;
}
