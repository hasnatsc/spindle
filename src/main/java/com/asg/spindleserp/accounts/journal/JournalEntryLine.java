package com.asg.spindleserp.accounts.journal;

import com.asg.spindleserp.accounts.setup.Account;
import com.asg.spindleserp.accounts.setup.CostCenter;
import com.asg.spindleserp.accounts.setup.SubAccount;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "acc_journal_entry_lines",
        indexes = {
                @Index(name = "idx_jel_entry", columnList = "journal_entry_id"),
                @Index(name = "idx_jel_account", columnList = "account_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_account_id")
    private SubAccount subAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;
    @Column(length = 500)
    private String description;
    @Builder.Default
    @Column(name = "debit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "credit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;
    @Column(length = 3)
    @Builder.Default
    private String currency = "BDT";
    @Builder.Default
    @Column(name = "exchange_rate", precision = 18, scale = 4)
    private BigDecimal exchangeRate = BigDecimal.ONE;
}
