package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trv_hotel_cancellations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelCancellation {

    public enum Status { REQUESTED, APPROVED, REJECTED, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "cancellation_date", nullable = false)
    private LocalDate cancellationDate;

    @Column(name = "reason", length = 500)
    private String reason;

    @Builder.Default
    @Column(name = "penalty_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.REQUESTED;

    @Column(name = "hotel_booking_id", nullable = false)
    private Long hotelBookingId;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
