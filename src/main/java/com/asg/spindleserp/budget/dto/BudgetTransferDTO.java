package com.asg.spindleserp.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** BudgetTransferDTO — inter-line budget transfer within same budget */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetTransferDTO {
    private Long   id;
    private Long   budgetId;
    private String budgetName;
    private String transferNo;          // auto-generated

    @NotNull(message = "Source line is required")
    private Long   fromLineId;
    private String fromLineDisplay;     // head + account

    @NotNull(message = "Destination line is required")
    private Long   toLineId;
    private String toLineDisplay;

    @NotNull(message = "Transfer amount is required")
    private BigDecimal transferAmount;

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotNull(message = "Transfer date is required")
    private LocalDate transferDate;

    /** PENDING | APPROVED | REJECTED */
    @Builder.Default private String status = "PENDING";

    private String approvedBy;
    private String approvedAt;
    private String createdBy;
    private String createdAt;
}
