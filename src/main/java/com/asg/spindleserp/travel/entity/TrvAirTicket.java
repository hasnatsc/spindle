package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trv_air_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvAirTicket extends BaseEntity implements Serializable {

    public enum Status { ISSUED, CANCELLED, REFUNDED }

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
