package com.asg.spindleserp.travel.dto;

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

    private Long bookingServiceId;
    private Long tourId; private String tourDisplay;
    private Long guideId; private String guideDisplay;
}
