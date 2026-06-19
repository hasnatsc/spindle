package com.asg.spindleserp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemModelDTO {

    private Long id;

    /** Optional — null means model is brand-independent */
    private Long   brandId;
    private String brandName;

    @NotBlank(message = "Model code is required")
    @Size(max = 30, message = "Code must not exceed 30 characters")
    private String modelCode;

    @NotBlank(message = "Model name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String modelName;

    private String description;

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
