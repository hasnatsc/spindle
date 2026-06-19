package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_policy",
        uniqueConstraints = @UniqueConstraint(name = "uq_policy_org_code",
                columnNames = {"organization_id", "policy_code"}),
        indexes = @Index(name = "idx_policy_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountsPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounts_mapping_id")
    private AccountsMapping accountsMapping;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_debit_account_id")
    private ChartOfAccount defaultDebitAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_credit_account_id")
    private ChartOfAccount defaultCreditAccount;

    @Column(nullable = false, length = 30)
    private String policyCode;
    @Column(nullable = false, length = 200)
    private String policyName;
    @Column(nullable = false, length = 30)
    private String policyType;
    @Column(length = 30)
    private String moduleType;
    @Column(length = 500)
    private String description;
    @Column(length = 20)
    private String voucherPrefix;
    private Integer nextVoucherNumber;
    @Builder.Default
    private Integer numberPadding = 6;

    @Builder.Default
    @Column(nullable = false)
    private boolean autoNumbering = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean autoPost = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean requireApproval = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean allowBackdating = false;
    private Integer backdatingDays;
    @Builder.Default
    @Column(nullable = false)
    private boolean allowFutureDating = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean allowEditAfterPost = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean allowReversal = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean allowNegativeAmount = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean allowZeroAmount = false;

    @Column(precision = 18, scale = 2)
    private BigDecimal approvalThreshold;
    @Column(precision = 18, scale = 2)
    private BigDecimal minimumAmount;
    @Column(precision = 18, scale = 2)
    private BigDecimal maximumAmount;
    @Column(length = 500)
    private String defaultNarrationTemplate;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isSystem = false;
}
