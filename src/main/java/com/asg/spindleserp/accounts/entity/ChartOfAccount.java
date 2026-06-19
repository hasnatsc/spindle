package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_chart_of_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_coa_code", columnNames = "account_code"),
                @UniqueConstraint(name = "uq_coa_org_code", columnNames = {"organization_id", "account_code"})
        },
        indexes = {
                @Index(name = "idx_coa_org", columnList = "organization_id"),
                @Index(name = "idx_coa_parent", columnList = "parent_account_id"),
                @Index(name = "idx_coa_type", columnList = "account_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartOfAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private ChartOfAccount parentAccount;

    @Column(nullable = false, unique = true, length = 50)
    private String accountCode;

    @Column(nullable = false, length = 200)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChartOfAccount.AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChartOfAccount.AccountNature accountNature;

    @Builder.Default
    @Column(nullable = false)
    private int level = 1;

    @Column(precision = 18, scale = 2)
    private BigDecimal openingBalance;
    @Column(precision = 18, scale = 2)
    private BigDecimal currentBalance;
    @Column(length = 10)
    private String currency;
    @Column(length = 1000)
    private String description;
    @Column(length = 50)
    private String taxId;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean isSystem = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isControlAccount = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean allowManualEntry = true;

    public enum AccountType {ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE}

    public enum AccountNature {DEBIT, CREDIT}
}
