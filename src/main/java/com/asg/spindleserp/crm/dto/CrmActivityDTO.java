package com.asg.spindleserp.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrmActivityDTO {
    private Long   id;

    // Optional links
    private Long   opportunityId;   private String opportunityDisplay;
    private Long   leadId;          private String leadDisplay;
    private Long   customerId;      private String customerDisplay;
    private Long   assignedToId;    private String assignedToDisplay;

    /** CALL | EMAIL | MEETING | VISIT | SAMPLE_SENT | QUOTATION | FOLLOW_UP | NOTE | OTHER */
    @NotBlank(message = "Activity type is required")
    private String activityType;

    @NotBlank(message = "Subject is required")
    @Size(max = 200)
    private String subject;

    private String description;

    @NotNull(message = "Activity date is required")
    private LocalDate activityDate;

    private Integer durationMinutes;

    @Size(max = 500) private String outcome;
    @Size(max = 500) private String nextAction;
    private LocalDate nextActionDate;

    /** PLANNED | COMPLETED | CANCELLED */
    @Builder.Default private String status = "PLANNED";

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
