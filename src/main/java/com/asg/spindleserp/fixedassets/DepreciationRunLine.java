package com.asg.spindleserp.fixedassets;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "fa_depreciation_run_lines",
        indexes = {
                @Index(name = "idx_dep_line_run", columnList = "depreciation_run_id"),
                @Index(name = "idx_dep_line_asset", columnList = "asset_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationRunLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depreciation_run_id", nullable = false)
    private DepreciationRun depreciationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private FixedAsset asset;

    @Column(name = "depreciation_method", nullable = false, length = 30)
    private String depreciation_method;
    @Column(name = "opening_book_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingBookValue;
    @Column(name = "depreciation_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal depreciationAmount;
    @Column(name = "closing_book_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal closingBookValue;
    @Column(name = "rate_applied", precision = 5, scale = 2)
    private BigDecimal rateApplied;
    @Column(name = "units_produced", precision = 14, scale = 3)
    private BigDecimal unitsProduced;
    @Column(columnDefinition = "TEXT")
    private String notes;
}
