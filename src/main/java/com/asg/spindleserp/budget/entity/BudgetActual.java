package com.asg.spindleserp.budget.entity;

import com.asg.spindleserp.accounts.entity.JournalEntryLine;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bgt_actuals",
        indexes = {
                @Index(name = "idx_ba_budget", columnList = "budget_id"),
                @Index(name = "idx_ba_line", columnList = "budget_line_id"),
                @Index(name = "idx_ba_journal", columnList = "journal_entry_id"),
                @Index(name = "idx_ba_jel", columnList = "journal_entry_line_id"),
                @Index(name = "idx_ba_date", columnList = "transaction_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetActual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    /**
     * Voucher header — always present
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryMaster journalEntry;

    /**
     * Individual GL line — enables exact GL-to-budget matching (★ new)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_line_id")
    private JournalEntryLine journalEntryLine;

    @Column(length = 50)
    private String sourceDocumentType;
    private Long sourceDocumentId;
    @Column(length = 100)
    private String sourceDocumentNo;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String narration;
    @Column(length = 100)
    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
