package com.asg.spindleserp.fixedassets.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DepreciationRunDTO — header + computed lines */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepreciationRunDTO {

    private Long   id;

    @NotNull(message = "Run date is required")
    private LocalDate runDate;

    @NotNull(message = "Period start is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end is required")
    private LocalDate periodEnd;

    /** MONTHLY | QUARTERLY | ANNUAL */
    @Builder.Default private String runType = "MONTHLY";

    /** DRAFT | PROCESSING | COMPLETED | POSTED | REVERSED */
    @Builder.Default private String status = "DRAFT";

    @Builder.Default private int        totalAssets      = 0;
    @Builder.Default private BigDecimal totalDepreciation = BigDecimal.ZERO;

    private String postedBy;
    private String postedAt;
    private String createdAt;
    private String createdBy;

    // GL journal voucher reference
    private Long journalEntryId;

    private List<LineDTO> lines;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineDTO {
        private Long       id;
        private Long       assetId;
        private String     assetCode;
        private String     assetName;
        private String     depreciationMethod;
        private BigDecimal openingBookValue;
        private BigDecimal depreciationAmount;
        private BigDecimal closingBookValue;
        private BigDecimal rateApplied;
        private BigDecimal unitsProduced;
        private String     notes;
    }
}
