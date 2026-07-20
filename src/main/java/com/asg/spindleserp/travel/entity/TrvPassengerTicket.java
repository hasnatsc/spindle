package com.asg.spindleserp.travel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trv_passenger_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvPassengerTicket {

    public enum Status { ISSUED, VOID, CHECKED_IN, NO_SHOW, USED, CANCELLED }
    public enum CheckInStatus { NOT_CHECKED_IN, CHECKED_IN, NO_SHOW, BOARDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", length = 50)
    private String ticketNumber;

    @Column(name = "seat_number", length = 20)
    private String seatNumber;

    /** Standalone per-passenger baggage allowance (overrides segment default if set). */
    @Column(name = "baggage_allowance", length = 100)
    private String baggageAllowance;

    /** Per-passenger fare portion (for split PNR / group bookings). */
    @Builder.Default
    @Column(name = "fare_portion", precision = 18, scale = 2)
    private BigDecimal farePortion = BigDecimal.ZERO;

    /** Per-passenger tax portion. */
    @Builder.Default
    @Column(name = "tax_portion", precision = 18, scale = 2)
    private BigDecimal taxPortion = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ISSUED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_status", nullable = false, length = 20)
    private CheckInStatus checkInStatus = CheckInStatus.NOT_CHECKED_IN;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "boarding_time")
    private LocalDateTime boardingTime;

    @Column(name = "gate_number", length = 10)
    private String gateNumber;

    @Column(name = "departure_gate", length = 10)
    private String departureGate;

    @Column(name = "queue_number", length = 10)
    private String queueNumber;

    @Column(name = "air_ticket_id", nullable = false)
    private Long airTicketId;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    // ── JPA object mappings ────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "air_ticket_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvAirTicket airTicket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvPassenger passenger;
}
