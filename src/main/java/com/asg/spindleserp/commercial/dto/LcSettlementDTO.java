package com.asg.spindleserp.commercial.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * LcSettlementDTO — LC payment settlement record.
 *
 * SettlementType   = SIGHT | USANCE | LOAN_ADJUSTMENT
 * SettlementStatus = PENDING | PARTIAL | SETTLED | REVERSED
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LcSettlementDTO {
    private Long   id;

    // LC account — AJAX Select2
    @NotNull(message = "LC is required")
    private Long   lcId;
    private String lcDisplay;

    // Linked document (optional)
    private Long   documentId;
    private String documentDisplay;

    @NotNull(message = "Settlement date is required")
    private LocalDate settlementDate;

    /** SIGHT | USANCE | LOAN_ADJUSTMENT */
    private String settlementType;
    /** PENDING | PARTIAL | SETTLED | REVERSED */
    @Builder.Default private String status = "PENDING";

    @Builder.Default private BigDecimal exchangeRate = BigDecimal.ONE;
    private BigDecimal amountUsd;
    private BigDecimal amountBdt;
    private BigDecimal marginUsed;
    private BigDecimal charges;
    private BigDecimal commission;
    private BigDecimal interest;
    private BigDecimal loanAmount;
}
