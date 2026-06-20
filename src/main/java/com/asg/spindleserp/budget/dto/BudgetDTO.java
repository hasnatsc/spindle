package com.asg.spindleserp.budget.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetDTO {
    private Long   id;
    private String budgetNo;         // auto-generated

    // Fiscal Year — AJAX Select2
    @NotNull(message = "Fiscal year is required")
    private Long   fiscalYearId;
    private String fiscalYearDisplay;

    private Long   businessUnitId;
    private String businessUnitDisplay;

    @NotBlank(message = "Budget name is required")
    @Size(max = 200)
    private String budgetName;

    private String description;

    /** ANNUAL | QUARTERLY | MONTHLY | PROJECT | DEPARTMENTAL | CAPEX | ROLLING */
    @Builder.Default private String budgetType = "ANNUAL";
    /** MONTHLY | QUARTERLY | ANNUAL */
    @Builder.Default private String periodType = "ANNUAL";

    @NotNull(message = "Period start is required")
    private LocalDate periodStart;
    @NotNull(message = "Period end is required")
    private LocalDate periodEnd;

    @Builder.Default private String     currency     = "BDT";
    @Builder.Default private BigDecimal exchangeRate = BigDecimal.ONE;

    // Totals (computed server-side, read-only on form)
    @Builder.Default private BigDecimal totalBudgeted  = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalRevised   = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalActual    = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalCommitted = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalAvailable = BigDecimal.ZERO;

    /** DRAFT | SUBMITTED | IN_APPROVAL | APPROVED | ACTIVE | LOCKED | CLOSED | REJECTED | RETURNED */
    @Builder.Default private String status         = "DRAFT";
    private String approvalStatus;

    /** ALLOW | WARN | BLOCK */
    @Builder.Default private String     overSpendPolicy     = "WARN";
    @Builder.Default private BigDecimal alertThresholdPct   = new BigDecimal("80.00");
    @Builder.Default private Boolean    allowInterLineTransfer = false;
    @Builder.Default private Integer    version              = 1;
    @Builder.Default private Boolean    isTemplate           = false;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;

    @NotEmpty(message = "At least one budget line is required")
    @Valid
    private List<LineDTO> lines;

    // ── Line DTO ──────────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LineDTO {
        private Long    id;
        private Integer lineNumber;

        // Budget Head — AJAX Select2
        @NotNull(message = "Budget head is required on each line")
        private Long   budgetHeadId;
        private String budgetHeadDisplay;

        // COA Account — AJAX Select2 (optional)
        private Long   accountId;
        private String accountDisplay;

        // Cost Center — AJAX Select2 (optional)
        private Long   costCenterId;
        private String costCenterDisplay;

        // Department (optional)
        private Long   departmentId;
        private String departmentDisplay;

        private String description;

        @Builder.Default private BigDecimal originalAmount  = BigDecimal.ZERO;
        @Builder.Default private BigDecimal revisedAmount   = BigDecimal.ZERO;
        @Builder.Default private BigDecimal actualAmount    = BigDecimal.ZERO;
        @Builder.Default private BigDecimal committedAmount = BigDecimal.ZERO;
        // availableAmount = revisedAmount - actualAmount - committedAmount (computed)
        private BigDecimal availableAmount;

        // Monthly phasing (Jan–Dec)
        @Builder.Default private BigDecimal janAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal febAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal marAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal aprAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal mayAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal junAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal julAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal augAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal sepAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal octAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal novAmount = BigDecimal.ZERO;
        @Builder.Default private BigDecimal decAmount = BigDecimal.ZERO;

        private String notes;
    }
}
