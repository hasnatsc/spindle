package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvTourDTO {

    private Long id;

    @NotBlank private String tourCode;
    @NotBlank private String tourName;
    private String destination;
    private BigDecimal durationHours;
    @Builder.Default private BigDecimal basePrice = BigDecimal.ZERO;
    @Builder.Default private String currency = "BDT";
    private String description;
    @Builder.Default private Boolean isActive = true;
}
