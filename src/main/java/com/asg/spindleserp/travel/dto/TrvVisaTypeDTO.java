package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvVisaTypeDTO {

    private Long id;

    @NotBlank private String country;
    @NotBlank private String visaCategory;
    private Integer processingDays;
    @Builder.Default private BigDecimal feeAmount = BigDecimal.ZERO;
    @Builder.Default private String currency = "BDT";
    private String description;
}
