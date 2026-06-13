package com.asg.spindleserp.accounts.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.organization.entity.CostCenter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_journal_entry_lines",
        indexes = {
                @Index(name = "idx_jel_journal", columnList = "journal_entry_id"),
                @Index(name = "idx_jel_account", columnList = "account_id"),
                @Index(name = "idx_jel_sub", columnList = "sub_account_id"),
                @Index(name = "idx_jel_cc", columnList = "cost_center_id"),
                @Index(name = "idx_jel_org", columnList = "organization_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private JournalEntryLine.EntryType entryType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String narration;
    @Column(length = 100)
    private String referenceNo;
    @Column(length = 20)
    private String taxCode;

    @Builder.Default
    @Column(nullable = false)
    private boolean isTaxLine = false;

    public enum EntryType {DEBIT, CREDIT}
}
