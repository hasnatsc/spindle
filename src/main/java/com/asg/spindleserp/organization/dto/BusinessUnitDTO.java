package com.asg.spindleserp.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessUnitDTO {

    private Long id;

    @NotNull(message = "Organisation is required")
    private Long organizationId;

    private String organizationName;
    private String createdAt;
    private String updatedBy;

    @NotBlank(message = "Business unit code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Business unit name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    // Boolean (wrapper) — accepts null from JSON without throwing.
    // Defaults to true in builder for create flow.
    @Builder.Default
    private Boolean active = true;
}
