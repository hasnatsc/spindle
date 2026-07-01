package com.asg.spindleserp.travel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelBookingDTO {

    private Long id;

    @NotNull private LocalDate checkInDate;
    @NotNull private LocalDate checkOutDate;
    private Integer nights;

    @Builder.Default private Integer roomsCount = 1;
    @Builder.Default private Integer adults = 1;
    @Builder.Default private Integer children = 0;

    private BigDecimal ratePerNight;
    private BigDecimal totalAmount;
    private String confirmationNumber;
    private String supplierReference;

    @Builder.Default
    private String status = "PENDING";

    private Long bookingServiceId;
    private Long hotelId;        private String hotelDisplay;
    private Long roomTypeId;     private String roomTypeDisplay;
    private Long mealPlanId;     private String mealPlanDisplay;

    @Builder.Default
    @Valid
    private List<RoomDTO> rooms = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoomDTO {
        private Long id;
        private String roomNumber;
        private String roomTypeSnapshot;
        @Builder.Default
        private List<Long> guestPassengerIds = new ArrayList<>();
    }
}
