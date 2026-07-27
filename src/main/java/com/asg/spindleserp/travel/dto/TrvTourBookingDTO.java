package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvTourBookingDTO {

    private Long id;

    private LocalDate tourDate;
    @Builder.Default private Integer paxCount = 1;
    @Builder.Default private BigDecimal totalAmount = BigDecimal.ZERO;
    private String confirmationNumber;
    @Builder.Default private String status = "PENDING";

    @NotNull(message = "Booking service line is required.")
    private Long bookingServiceId;
    @NotNull(message = "Tour is required.")
    private Long tourId;
    private String tourDisplay;
    private Long guideId; private String guideDisplay;
}
