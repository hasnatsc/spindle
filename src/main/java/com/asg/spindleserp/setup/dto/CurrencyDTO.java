package com.asg.spindleserp.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CurrencyDTO {

    private Long id;

    @NotBlank(message = "Code is required")
    @Size(max = 3, message = "Code must not exceed 3 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 10, message = "Symbol must not exceed 10 characters")
    private String symbol;

    @Builder.Default
    private Integer decimalPlaces = 2;

    @Builder.Default
    private Boolean active = true;
}
