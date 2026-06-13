package com.asg.spindleserp.fixedassets.entity;

import com.asg.spindleserp.BaseEntity;
import com.asg.spindleserp.accounts.entity.JournalEntryMaster;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fa_depreciation_runs",
        indexes = {
                @Index(name = "idx_fdr_org", columnList = "organization_id"),
                @Index(name = "idx_fdr_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntryMaster journalEntry;

    @Column(nullable = false)
    private LocalDate runDate;
    @Column(nullable = false)
    private LocalDate periodStart;
    @Column(nullable = false)
    private LocalDate periodEnd;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepreciationRun.RunType runType = DepreciationRun.RunType.MONTHLY;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepreciationRun.RunStatus status = DepreciationRun.RunStatus.DRAFT;

    @Builder.Default
    @Column(nullable = false)
    private int totalAssets = 0;
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDepreciation = BigDecimal.ZERO;

    @Column(length = 100)
    private String postedBy;
    private LocalDateTime postedAt;

    @Builder.Default
    @OneToMany(mappedBy = "depreciationRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepreciationRunLine> lines = new ArrayList<>();

    public enum RunType {MONTHLY, QUARTERLY, ANNUAL}

    public enum RunStatus {DRAFT, PROCESSING, COMPLETED, POSTED, REVERSED}
}
