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

    // ── Vendor / Agent info ──────────────────────────────────────────────────
    private LocalDate issueDate;
    private String bookingReference;
    private String agentVendorName;
    private String agentVendorAddress;
    private String agentVendorEmail;
    private String agentVendorMocatNo;

    @Builder.Default private String status = "ISSUED";

    private Long bookingServiceId;
    private Long airlineId;             private String airlineDisplay;
    private Long originAirportId;       private String originAirportDisplay;
    private Long destinationAirportId;  private String destinationAirportDisplay;
    private Long cabinClassId;          private String cabinClassDisplay;

    @Builder.Default
    @Valid
    private List<SegmentDTO> segments = new ArrayList<>();

    @Builder.Default
    @Valid
    private List<PassengerTicketDTO> passengerTickets = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SegmentDTO {
        private Long id;
        private Integer segmentOrder;
        private String flightNumber;
        private String aircraftModel;
        private Long airlineId;             private String airlineDisplay;
        private Long originAirportId;       private String originAirportDisplay;
        private Long destinationAirportId;  private String destinationAirportDisplay;
        private LocalDate departureDate;
        private LocalTime departureTime;
        private String departureTerminal;
        private LocalDate arrivalDate;
        private LocalTime arrivalTime;
        private String arrivalTerminal;
        private Integer flightDurationMinutes;
        private Long cabinClassId;          private String cabinClassDisplay;
        private String baggageAllowance;
    }

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
