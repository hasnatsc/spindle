package com.asg.spindleserp.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OpportunityDTO {
    private Long   id;
    private String opportunityNo;      // auto-generated

    // Customer — AJAX Select2
    private Long   customerId;
    private String customerDisplay;

    // Lead link (optional)
    private Long   leadId;
    private String leadDisplay;

    // Assigned user — AJAX Select2
    private Long   assignedToId;
    private String assignedToDisplay;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    private String description;

    /** PROSPECT | QUALIFIED | PROPOSAL | NEGOTIATION | WON | LOST */
    @Builder.Default private String opportunityStage = "PROSPECT";

    @Builder.Default private BigDecimal probability    = BigDecimal.ZERO;
    private BigDecimal estimatedValue;
    @Builder.Default private String currency = "BDT";

    private LocalDate expectedCloseDate;
    private LocalDate actualCloseDate;
    @Size(max = 500) private String lostReason;
    private String remarks;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
