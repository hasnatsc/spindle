package com.asg.spindleserp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemCategoryDTO {

    private Long id;

    private Long   parentCategoryId;
    private String parentCategoryName;

    @NotBlank(message = "Category code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String categoryCode;

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String categoryName;

    private String description;

    /**
     * RAW_MATERIAL | SEMI_FINISHED | FINISHED_GOOD | SERVICE |
     * SPARE_PART | CONSUMABLE | MRO | GENERAL | FIXED_ASSET
     */
    private String itemType;

    /** ROOT | GROUP | ITEM */
    @Builder.Default
    private String layerType = "ITEM";

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
    private String createdBy;
    private String updatedBy;
}
