package com.asg.spindleserp.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * AccountingPeriodDTO — canonical pattern.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountingPeriodDTO {

    private Long id;

    @NotBlank(message = "Period name is required")
    @Size(max = 50)
    private String periodName;

    /** DAILY | WEEKLY | MONTHLY | QUARTERLY | YEARLY | CUSTOM */
    @NotBlank(message = "Period type is required")
    private String periodType;

    @NotNull(message = "Fiscal year is required")
    private Integer fiscalYear;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Size(max = 1000)
    private String description;

    @Builder.Default private Boolean active = true;
    @Builder.Default private Boolean closed = false;

    private String closedBy;
    private LocalDate closedDate;

    // audit
    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
