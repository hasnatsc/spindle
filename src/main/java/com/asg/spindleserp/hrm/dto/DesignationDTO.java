package com.asg.spindleserp.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

// ── DesignationDTO ────────────────────────────────────────────────────────────
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DesignationDTO {
    private Long   id;

    @NotBlank(message = "Designation code is required")
    @Size(max = 50)
    private String designationCode;

    @NotBlank(message = "Designation name is required")
    @Size(max = 200)
    private String designationName;

    @Size(max = 20)  private String grade;
    @Size(max = 500) private String description;

    @Builder.Default private Boolean active = true;

    private String createdAt; private String updatedAt;
    private String createdBy; private String updatedBy;
}
