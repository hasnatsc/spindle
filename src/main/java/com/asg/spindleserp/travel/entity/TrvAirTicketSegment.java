package com.asg.spindleserp.travel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A single flight segment within an air ticket.
 * Follows the soft-FK pattern (no JPA relationship annotations).
 */
@Entity
@Table(name = "trv_air_ticket_segments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvAirTicketSegment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "air_ticket_id", nullable = false)
    private Long airTicketId;

    @Column(name = "segment_order", nullable = false)
    private Integer segmentOrder;

    @Column(name = "flight_number", length = 20)
    private String flightNumber;

    @Column(name = "aircraft_model", length = 100)
    private String aircraftModel;

    @Column(name = "airline_id")
    private Long airlineId;

    @Column(name = "origin_airport_id")
    private Long originAirportId;

    @Column(name = "destination_airport_id")
    private Long destinationAirportId;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "departure_terminal", length = 50)
    private String departureTerminal;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "arrival_terminal", length = 50)
    private String arrivalTerminal;

    @Column(name = "flight_duration_minutes")
    private Integer flightDurationMinutes;

    @Column(name = "cabin_class_id")
    private Long cabinClassId;

    @Column(name = "baggage_allowance", length = 100)
    private String baggageAllowance;

    // ── JPA object mappings (read-only for writes) ──────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "air_ticket_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvAirTicket airTicket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvAirline airline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_airport_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvAirport originAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_airport_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvAirport destinationAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cabin_class_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvCabinClass cabinClass;
}
