package com.asg.spindleserp.budget;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.accounts.journal.JournalEntryLine;
import com.asg.spindleserp.security.Organization;
import lombok.Getter;

/**
 * BudgetActual — immutable.
 * One row created per GL journal posting that touches a budget line.
 * Never update or delete; running totals on BudgetLine are the live balance.
 */
@Entity
@Table(name = "bgt_actuals",
        indexes = {
                @Index(name = "idx_bgt_act_budget", columnList = "budget_id"),
                @Index(name = "idx_bgt_act_line", columnList = "budget_line_id"),
                @Index(name = "idx_bgt_act_je", columnList = "journal_entry_id"),
                @Index(name = "idx_bgt_act_date", columnList = "transaction_date"),
                @Index(name = "idx_bgt_act_src", columnList = "source_document_type,source_document_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetActual implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_line_id", nullable = false)
    private BudgetLine budgetLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_line_id")
    private JournalEntryLine journalEntryLine;

    // Polymorphic source reference (DocumentType + id of global_business_documents)
    @Column(name = "source_document_type", length = 50)
    private String sourceDocumentType;
    @Column(name = "source_document_id")
    private Long sourceDocumentId;
    @Column(name = "source_document_no", length = 100)
    private String sourceDocumentNo;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Builder.Default
    @Column(name = "debit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "credit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "net_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;
    // netAmount = debitAmount - creditAmount; positive = expense; negative = income

    @Column(length = 500)
    private String narration;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
