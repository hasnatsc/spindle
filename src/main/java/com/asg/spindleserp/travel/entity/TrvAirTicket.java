package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

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
}
