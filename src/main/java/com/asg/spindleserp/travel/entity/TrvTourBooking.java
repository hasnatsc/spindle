package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trv_tour_bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvTourBooking extends BaseEntity implements Serializable {

    public enum Status { PENDING, CONFIRMED, CANCELLED, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tour_date")
    private LocalDate tourDate;

    @Builder.Default
    @Column(name = "pax_count", nullable = false)
    private Integer paxCount = 1;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "confirmation_number", length = 100)
    private String confirmationNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "booking_service_id", nullable = false)
    private Long bookingServiceId;

    @Column(name = "tour_id", nullable = false)
    private Long tourId;

    @Column(name = "guide_id")
    private Long guideId;
}
