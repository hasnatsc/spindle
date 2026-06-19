package com.asg.spindleserp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemBrandDTO {

    private Long id;

    @NotBlank(message = "Brand code is required")
    @Size(max = 30, message = "Code must not exceed 30 characters")
    private String brandCode;

    @NotBlank(message = "Brand name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String brandName;

    private String description;

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
