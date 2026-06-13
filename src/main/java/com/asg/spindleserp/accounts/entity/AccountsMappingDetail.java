package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_mapping_details",
        indexes = @Index(name = "idx_amd_mapping", columnList = "accounts_mapping_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountsMappingDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accounts_mapping_id", nullable = false)
    private AccountsMapping accountsMapping;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private ChartOfAccount account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(nullable = false)
    private Integer lineNumber;
    @Column(length = 100)
    private String entryName;
    @Column(length = 500)
    private String entryDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JournalEntryLine.EntryType entryType;

    @Column(nullable = false, length = 30)
    private String amountType;
    @Column(precision = 8, scale = 4)
    private BigDecimal percentage;
    @Column(precision = 18, scale = 2)
    private BigDecimal fixedAmount;
    @Column(length = 500)
    private String formula;
    @Column(length = 100)
    private String fieldReference;
    @Column(length = 500)
    private String condition;
    @Column(length = 20)
    private String conditionOperator;
    @Column(length = 200)
    private String conditionValue;
    @Builder.Default
    @Column(length = 30)
    private String controlAccountType = "NONE";
    @Column(length = 500)
    private String lineNarration;
    @Column(length = 20)
    private String taxCode;
    @Column(precision = 8, scale = 4)
    private BigDecimal taxRate;

    @Builder.Default
    @Column(nullable = false)
    private int sortOrder = 0;
    @Builder.Default
    @Column(nullable = false)
    private boolean negateAmount = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean roundAmount = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean skipIfZero = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isTaxEntry = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isOptional = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
    @Builder.Default
    @Column(nullable = false)
    private boolean inheritCostCenter = false;
}
