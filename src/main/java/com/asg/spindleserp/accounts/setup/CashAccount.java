package com.asg.spindleserp.accounts.setup;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "acc_cash_accounts")
@DiscriminatorValue("CASH")
@Getter
@Setter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "id")
public class CashAccount extends SubAccount {

    @Column(name = "cash_account_code", nullable = false, unique = true, length = 50)
    private String cashAccountCode;
    @Column(name = "account_title", length = 200)
    private String accountTitle;
    @Column(name = "cash_account_type", nullable = false, length = 20)
    @Builder.Default
    private String cashAccountType = "PETTY_CASH"; // MAIN_CASH|PETTY_CASH|CASH_IN_HAND|CASH_DRAWER|IMPREST

    @Column(length = 100)
    private String location;
    @Column(length = 100)
    private String custodian;
    @Column(name = "custodian_phone", length = 20)
    private String custodianPhone;
    @Column(name = "custodian_email", length = 100)
    private String custodianEmail;
    @Builder.Default
    @Column(name = "maximum_limit", precision = 18, scale = 2)
    private BigDecimal maximumLimit = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "minimum_limit", precision = 18, scale = 2)
    private BigDecimal minimumLimit = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;
    @Builder.Default
    @Column(name = "approval_limit", precision = 18, scale = 2)
    private BigDecimal approvalLimit = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;
    @Column(columnDefinition = "TEXT")
    private String remarks;
}
