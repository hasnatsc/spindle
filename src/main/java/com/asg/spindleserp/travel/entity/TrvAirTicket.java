package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trv_air_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvAirTicket extends BaseEntity implements Serializable {

    public enum Status { ISSUED, VOID, CANCELLED, REFUNDED, EXCHANGED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pnr", length = 20)
    private String pnr;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Builder.Default
    @Column(name = "fare_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal fareAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "supplier_reference", length = 100)
    private String supplierReference;

    // ── Ticket-level fields ──────────────────────────────────────────────────
    /** The actual airline ticket / e-ticket number (concessionary serial). */
    @Column(name = "ticket_number", length = 30)
    private String ticketNumber;

    /** IATA code of the validating carrier whose ticket stock is used. */
    @Column(name = "validating_carrier", length = 3)
    private String validatingCarrier;

    /** Fare basis / booking code (e.g. Y, Q14NR, H7NR). */
    @Column(name = "fare_basis", length = 20)
    private String fareBasis;

    /** Travel agency commission amount. */
    @Builder.Default
    @Column(name = "commission_amount", precision = 18, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    /** Commission rate percentage (e.g. 5.00 for 5%). */
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    /** Net fare after commission = fareAmount - commissionAmount. */
    @Builder.Default
    @Column(name = "net_fare", precision = 18, scale = 2)
    private BigDecimal netFare = BigDecimal.ZERO;

    /** Service fee charged by the travel agency. */
    @Builder.Default
    @Column(name = "service_fee_amount", precision = 18, scale = 2)
    private BigDecimal serviceFeeAmount = BigDecimal.ZERO;

    /** Tour code / IATA package identifier. */
    @Column(name = "tour_code", length = 50)
    private String tourCode;

    /** Endorsements / restrictions printed on the ticket. */
    @Column(name = "endorsement_restrictions", length = 500)
    private String endorsementRestrictions;

    /** Date/time by which the ticket must be issued before the reservation auto-cancels. */
    @Column(name = "ticket_time_limit")
    private LocalDateTime ticketTimeLimit;

    /** Additional collection amount (for reissue / exchange). */
    @Builder.Default
    @Column(name = "additional_collection", precision = 18, scale = 2)
    private BigDecimal additionalCollection = BigDecimal.ZERO;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "booking_reference", length = 100)
    private String bookingReference;

    @Column(name = "agent_vendor_name", length = 200)
    private String agentVendorName;

    @Column(name = "agent_vendor_address", length = 500)
    private String agentVendorAddress;

    @Column(name = "agent_vendor_email", length = 150)
    private String agentVendorEmail;

    @Column(name = "agent_vendor_mocat_no", length = 50)
    private String agentVendorMocatNo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ISSUED;

    /** Soft FK → trv_booking_services. */
    @Column(name = "booking_service_id", nullable = false)
    private Long bookingServiceId;

    @Column(name = "airline_id")
    private Long airlineId;

    @Column(name = "origin_airport_id")
    private Long originAirportId;

    @Column(name = "destination_airport_id")
    private Long destinationAirportId;

    @Column(name = "cabin_class_id")
    private Long cabinClassId;

    // ── JPA object mappings (read-only for writes; xxxId fields handle writes) ──

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_service_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvBookingService bookingService;

    @Builder.Default
    @OneToMany(mappedBy = "airTicket", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TrvAirTicketSegment> segments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "airTicket", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TrvPassengerTicket> passengerTickets = new ArrayList<>();
}
