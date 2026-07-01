package com.asg.spindleserp.travel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelCancellationDTO {

    private Long id;

    @NotNull private LocalDate cancellationDate;
    private String reason;

    @Builder.Default private BigDecimal penaltyAmount = BigDecimal.ZERO;
    @Builder.Default private BigDecimal refundAmount = BigDecimal.ZERO;
    @Builder.Default private String status = "REQUESTED";

    private Long hotelBookingId;
}
