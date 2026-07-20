package com.asg.spindleserp.travel.dto;

import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ── Ticket-level fields ──────────────────────────────────────────────────
    /** The actual airline ticket / e-ticket number. */
    private String ticketNumber;

    /** Validating carrier IATA code. */
    private String validatingCarrier;

    /** Fare basis code (e.g. Y, Q14NR). */
    private String fareBasis;

    @Builder.Default private BigDecimal commissionAmount = BigDecimal.ZERO;
    private BigDecimal commissionRate;
    @Builder.Default private BigDecimal netFare = BigDecimal.ZERO;
    @Builder.Default private BigDecimal serviceFeeAmount = BigDecimal.ZERO;

    /** Tour code / IATA package identifier. */
    private String tourCode;

    /** Endorsements / restrictions. */
    private String endorsementRestrictions;

    /** Ticket time limit (by when the ticket must be issued). */
    private LocalDateTime ticketTimeLimit;

    /** Additional collection for reissue / exchange. */
    @Builder.Default private BigDecimal additionalCollection = BigDecimal.ZERO;

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
        @Builder.Default private String checkInStatus = "NOT_CHECKED_IN";
        private LocalDateTime checkInTime;
        private LocalDateTime boardingTime;
        private String gateNumber;
        private String departureGate;
        @Builder.Default private BigDecimal farePortion = BigDecimal.ZERO;
        @Builder.Default private BigDecimal taxPortion = BigDecimal.ZERO;
    }
}
