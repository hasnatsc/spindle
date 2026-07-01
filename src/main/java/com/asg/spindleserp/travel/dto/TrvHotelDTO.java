package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelDTO {

    private Long id;

    @NotBlank @Size(max = 30)
    private String hotelCode;

    @NotBlank @Size(max = 200)
    private String hotelName;

    private String city;
    private String country;
    private String address;
    private Integer starRating;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;

    @Builder.Default
    private Boolean isActive = true;

    private Long categoryId; private String categoryDisplay;
}
