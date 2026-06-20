package com.asg.spindleserp.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/** BudgetRevisionDTO — covers supplementary / reallocation / reduction revisions */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetRevisionDTO {
    private Long   id;
    private Long   budgetId;
    private String budgetNo;
    private String budgetName;

    private String revisionNo;          // auto-generated
    private Integer revisionNumber;

    /** REALLOCATION | SUPPLEMENTARY | REDUCTION | TECHNICAL */
    @Builder.Default private String revisionType = "REALLOCATION";

    @NotBlank(message = "Reason is required")
    private String reason;
    private String justification;

    @Builder.Default private BigDecimal totalIncrease = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalDecrease = BigDecimal.ZERO;

    /** DRAFT | SUBMITTED | IN_APPROVAL | APPROVED | REJECTED */
    @Builder.Default private String status = "DRAFT";

    private String approvedBy;
    private String approvedAt;
    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;

    private List<LineDTO> lines;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineDTO {
        private Long   id;

        @NotNull(message = "Budget line is required")
        private Long   budgetLineId;
        private String budgetHeadDisplay;
        private String accountDisplay;

        /** '+' increase or '-' decrease */
        @NotBlank(message = "Direction is required")
        private String direction;

        @NotNull(message = "Change amount is required")
        private BigDecimal changeAmount;

        private BigDecimal openingAmount;
        private BigDecimal closingAmount;
        private String reason;
    }
}
