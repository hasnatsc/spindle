package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvRoomTypeDTO {

    private Long id;

    @NotBlank
    private String roomTypeName;

    private Integer maxOccupancy;
    private BigDecimal basePrice;
    private String currency;

    @Builder.Default
    private Boolean isActive = true;

    private Long hotelId; private String hotelDisplay;

    @Builder.Default
    private List<String> facilities = new ArrayList<>();
}
