package com.asg.spindleserp.fixedassets.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** AssetDisposalDTO — records disposal / write-off / transfer */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetDisposalDTO {

    private Long      id;

    @NotNull(message = "Asset is required")
    private Long      assetId;
    private String    assetCode;
    private String    assetName;
    private BigDecimal currentBookValue;
    private BigDecimal accumulatedDepreciation;

    @NotNull(message = "Disposal date is required")
    private LocalDate disposalDate;

    /** SALE | WRITE_OFF | TRANSFER | SCRAP | DONATION */
    @NotNull(message = "Disposal type is required")
    private String    disposalType;

    @Builder.Default private BigDecimal disposalValue = BigDecimal.ZERO;
    private BigDecimal bookValueAtDisposal;
    private BigDecimal accumulatedDepAtDisposal;
    private BigDecimal gainLoss;

    private String buyerName;
    private String reason;
    private String approvedBy;
    private String approvedAt;

    // Transfer fields
    private Long   transferToDeptId;
    private String transferToDeptDisplay;
    private Long   transferToEmployeeId;
    private String transferToEmployeeDisplay;

    private Long   journalEntryId;

    private String createdBy;
    private String createdAt;
}
