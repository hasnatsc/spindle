package com.asg.spindleserp.travel.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPackageBookingDTO {

    private Long id;

    private LocalDate travelDate;
    @Builder.Default private Integer paxCount = 1;
    @Builder.Default private BigDecimal totalAmount = BigDecimal.ZERO;
    private String confirmationNumber;
    private String supplierReference;
    @Builder.Default private String status = "PENDING";

    private Long bookingServiceId;
    private Long packageId; private String packageDisplay;
}
