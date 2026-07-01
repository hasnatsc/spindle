package com.asg.spindleserp.travel.dto;

import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvAirTicketDTO {

    private Long id;

    private String pnr;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalDate arrivalDate;
    private LocalTime arrivalTime;

    @Builder.Default private BigDecimal fareAmount = BigDecimal.ZERO;
    @Builder.Default private BigDecimal taxAmount = BigDecimal.ZERO;
    @Builder.Default private BigDecimal totalAmount = BigDecimal.ZERO;
    private String supplierReference;

    @Builder.Default private String status = "ISSUED";

    private Long bookingServiceId;
    private Long airlineId;             private String airlineDisplay;
    private Long originAirportId;       private String originAirportDisplay;
    private Long destinationAirportId;  private String destinationAirportDisplay;
    private Long cabinClassId;          private String cabinClassDisplay;

    @Builder.Default
    @Valid
    private List<PassengerTicketDTO> passengerTickets = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PassengerTicketDTO {
        private Long id;
        private Long passengerId; private String passengerName;
        private String ticketNumber;
        private String seatNumber;
        private String baggageAllowance;
        @Builder.Default private String status = "ISSUED";
    }
}
