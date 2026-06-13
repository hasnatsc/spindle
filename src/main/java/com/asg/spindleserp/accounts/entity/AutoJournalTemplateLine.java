package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_auto_journal_template_lines",
        indexes = @Index(name = "idx_ajtl_template", columnList = "auto_journal_template_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoJournalTemplateLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auto_journal_template_id", nullable = false)
    private AutoJournalTemplate autoJournalTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private ChartOfAccount account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_account_id")
    private ChartOfAccount controlAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(nullable = false)
    private Integer lineNumber;
    @Column(length = 100)
    private String lineName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JournalEntryLine.EntryType entryType;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String amountMode = "FIELD_REFERENCE";
    @Column(precision = 8, scale = 4)
    private BigDecimal amountPercentage;
    @Column(precision = 18, scale = 2)
    private BigDecimal fixedAmount;
    @Column(length = 500)
    private String formula;
    @Column(length = 100)
    private String fieldReference;
    @Column(length = 30)
    private String controlAccountType;
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
    private boolean skipIfZero = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isTaxLine = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isOptional = false;
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
