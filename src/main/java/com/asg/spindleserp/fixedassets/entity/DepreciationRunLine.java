package com.asg.spindleserp.fixedassets.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fa_depreciation_run_lines",
    indexes = {
        @Index(name = "idx_fdrl_run",   columnList = "depreciation_run_id"),
        @Index(name = "idx_fdrl_asset", columnList = "asset_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepreciationRunLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depreciation_run_id", nullable = false)
    private DepreciationRun depreciationRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false, length = 30) private String depreciationMethod;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal openingBookValue;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal depreciationAmount;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal closingBookValue;
    @Column(precision = 5,  scale = 2) private BigDecimal rateApplied;
    @Column(precision = 14, scale = 3) private BigDecimal unitsProduced;
    @Column(columnDefinition = "text") private String notes;

    private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}

