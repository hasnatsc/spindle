package com.asg.spindleserp.accounts.journal;

import com.asg.spindleserp.common.BaseOrgEntity;
import com.asg.spindleserp.dummy.JournalEntryLine;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "acc_journal_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_je_org_no", columnNames = {"organization_id", "entry_no"}),
        indexes = {
                @Index(name = "idx_je_org", columnList = "organization_id"),
                @Index(name = "idx_je_date", columnList = "entry_date"),
                @Index(name = "idx_je_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_no", nullable = false, length = 50)
    private String entryNo;
    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;
    // JOURNAL|PAYMENT|RECEIPT|CONTRA|OPENING
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;
    @Column(name = "fiscal_year")
    private Integer fiscalYear;
    @Column(name = "reference_no", length = 100)
    private String referenceNo;
    @Column(columnDefinition = "TEXT")
    private String narration;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT|POSTED|REVERSED

    @Builder.Default
    @Column(name = "is_auto_entry", nullable = false)
    private Boolean isAutoEntry = false;
    @Column(name = "source_module", length = 50)
    private String sourceModule;
    @Column(name = "source_doc_id")
    private Long sourceDocId;

    @Builder.Default
    @Column(name = "total_debit", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDebit = BigDecimal.ZERO;
    @Builder.Default
    @Column(name = "total_credit", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "posted_by", length = 100)
    private String postedBy;
    @Column(name = "posted_at")
    private LocalDateTime postedAt;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();
}
