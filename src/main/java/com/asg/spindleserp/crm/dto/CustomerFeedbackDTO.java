package com.asg.spindleserp.crm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerFeedbackDTO {
    private Long   id;

    @NotNull(message = "Customer is required")
    private Long   customerId;
    private String customerDisplay;

    private Long   businessDocumentId;
    private String businessDocumentDisplay;

    @Builder.Default private LocalDate feedbackDate = LocalDate.now();

    /** GENERAL | COMPLAINT | QUALITY | DELIVERY | PRICING | SERVICE | APPRECIATION */
    @Builder.Default private String feedbackType = "GENERAL";

    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 200) private String subject;
    private String description;
    private String resolution;
    @Size(max = 100) private String resolvedBy;
    private String resolvedAt;

    /** OPEN | IN_PROGRESS | RESOLVED | CLOSED */
    @Builder.Default private String status = "OPEN";

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
