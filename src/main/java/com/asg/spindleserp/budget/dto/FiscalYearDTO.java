package com.asg.spindleserp.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;

/** FiscalYearDTO — mirrors FiscalYear entity */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiscalYearDTO {
    private Long   id;

    @NotBlank(message = "Year code is required")
    @Size(max = 20)
    private String yearCode;

    @NotBlank(message = "Year name is required")
    @Size(max = 100)
    private String yearName;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /** DRAFT | ACTIVE | LOCKED | CLOSED */
    @Builder.Default private String status    = "DRAFT";
    @Builder.Default private Boolean isCurrent = false;

    private String notes;
    private String closedBy;
    private String closedAt;
    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
