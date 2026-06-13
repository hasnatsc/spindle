package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acc_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uq_mapping_org_code",
                columnNames = {"organization_id", "mapping_code"}),
        indexes = @Index(name = "idx_mapping_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountsMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 30)
    private String mappingCode;
    @Column(nullable = false, length = 200)
    private String mappingName;
    @Column(nullable = false, length = 50)
    private String moduleType;
    @Column(nullable = false, length = 50)
    private String transactionType;
    @Column(length = 500)
    private String description;
    @Column(length = 30)
    private String defaultVoucherType;
    @Column(length = 30)
    private String voucherType;
    @Column(length = 20)
    private String voucherPrefix;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String debitControlType = "NONE";
    @Builder.Default
    @Column(nullable = false, length = 30)
    private String creditControlType = "NONE";

    @Column(length = 500)
    private String defaultNarrationTemplate;

    @Builder.Default
    @Column(nullable = false)
    private boolean useSubLedger = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean updateSubAccountBalance = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean allowPartialPosting = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean autoPost = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean consolidateEntries = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean createReversingEntry = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean requireApproval = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_debit_account_id")
    private ChartOfAccount defaultDebitAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_credit_account_id")
    private ChartOfAccount defaultCreditAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_account_id")
    private ChartOfAccount discountAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freight_account_id")
    private ChartOfAccount freightAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "input_vat_account_id")
    private ChartOfAccount inputVatAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_vat_account_id")
    private ChartOfAccount outputVatAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forex_gain_account_id")
    private ChartOfAccount forexGainAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forex_loss_account_id")
    private ChartOfAccount forexLossAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tds_account_id")
    private ChartOfAccount tdsAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ait_account_id")
    private ChartOfAccount aitAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rounding_account_id")
    private ChartOfAccount roundingAccount;

    @Builder.Default
    @OneToMany(mappedBy = "accountsMapping", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountsMappingDetail> details = new ArrayList<>();
}
