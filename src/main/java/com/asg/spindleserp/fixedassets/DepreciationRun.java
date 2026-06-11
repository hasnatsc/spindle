package com.asg.spindleserp.fixedassets;

import com.asg.spindleserp.accounts.journal.JournalEntry;
import com.asg.spindleserp.common.BaseOrgEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fa_depreciation_runs",
        indexes = @Index(name = "idx_dep_run_org", columnList = "organization_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationRun extends BaseOrgEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Column(name = "run_type", nullable = false, length = 20)
    @Builder.Default
    private String runType = "MONTHLY"; // MONTHLY|ANNUAL|ADHOC

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT|POSTED|REVERSED

    @Builder.Default
    @Column(name = "total_assets")
    private Integer totalAssets = 0;
    @Builder.Default
    @Column(name = "total_depreciation", precision = 18, scale = 2)
    private BigDecimal totalDepreciation = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(name = "posted_by", length = 100)
    private String postedBy;
    @Column(name = "posted_at")
    private LocalDateTime postedAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @OneToMany(mappedBy = "depreciationRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DepreciationRunLine> lines = new ArrayList<>();
}
