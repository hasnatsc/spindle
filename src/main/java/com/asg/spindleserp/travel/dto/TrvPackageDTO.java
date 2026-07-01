package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPackageDTO {

    private Long id;

    @NotBlank private String packageCode;
    @NotBlank private String packageName;
    private String destination;
    private String category;
    private Integer durationDays;
    private Integer durationNights;

    @Builder.Default private BigDecimal basePrice = BigDecimal.ZERO;
    @Builder.Default private String currency = "BDT";
    private String description;
    @Builder.Default private Boolean isActive = true;

    @Builder.Default
    private List<ItineraryDayDTO> itineraryDays = new ArrayList<>();

    @Builder.Default
    private List<InclusionDTO> inclusions = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItineraryDayDTO {
        private Long id;
        private Integer dayNumber;
        private String title;
        private String description;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InclusionDTO {
        private Long id;
        @Builder.Default private String inclusionType = "INCLUDED";
        private String description;
    }
}
