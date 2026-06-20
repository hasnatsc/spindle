package com.asg.spindleserp.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetHeadDTO {
    private Long   id;
    private Long   parentId;
    private String parentName;

    @NotBlank(message = "Head code is required")
    @Size(max = 50)
    private String headCode;

    @NotBlank(message = "Head name is required")
    @Size(max = 200)
    private String headName;

    /** REVENUE | EXPENSE | CAPEX | OPEX | PRODUCTION | HR | COMMERCIAL | OTHER */
    @Builder.Default private String headType = "EXPENSE";

    private String description;
    @Builder.Default private Boolean active = true;
    @Builder.Default private Integer displayOrder = 0;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
