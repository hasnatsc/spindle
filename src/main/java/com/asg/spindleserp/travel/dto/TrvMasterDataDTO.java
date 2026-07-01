package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Shared DTO shapes for small master-data lookups (category, meal plan, airline,
 * airport, cabin class, room facility). Kept in one file since each is a
 * 2-3 field lookup table — split out only if a screen needs more than CRUD.
 */
public class TrvMasterDataDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HotelCategoryDTO {
        private Long id;
        @NotBlank private String categoryName;
        private String description;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MealPlanDTO {
        private Long id;
        @NotBlank private String planCode;
        @NotBlank private String planName;
        private String description;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AirlineDTO {
        private Long id;
        @NotBlank private String airlineCode;
        @NotBlank private String airlineName;
        @Builder.Default private Boolean isActive = true;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AirportDTO {
        private Long id;
        @NotBlank private String airportCode;
        @NotBlank private String airportName;
        private String city;
        private String country;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CabinClassDTO {
        private Long id;
        @NotBlank private String classCode;
        @NotBlank private String className;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoomFacilityDTO {
        private Long id;
        @NotBlank private String facilityName;
        private Long roomTypeId;
    }
}
