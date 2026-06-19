package com.asg.spindleserp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * ItemUomDTO — Unit of Measure CRUD transfer object.
 * Follows BusinessUnitDTO pattern: wrapper Boolean active, @Builder.Default.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemUomDTO {

    private Long id;

    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    private String symbol;

    /** One of: WEIGHT, COUNT, LENGTH, VOLUME, AREA, PACKING, UNIT */
    @NotBlank(message = "Category is required")
    private String category;

    @Builder.Default
    private Boolean isBaseUnit = false;

    @Builder.Default
    private BigDecimal conversionFactor = BigDecimal.ONE;

    @Builder.Default
    private Boolean active = true;

    private String createdAt;
    private String updatedAt;
}
