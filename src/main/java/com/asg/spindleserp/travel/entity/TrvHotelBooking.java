package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "trv_hotel_bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelBooking extends BaseEntity implements Serializable {

    public enum Status { PENDING, CONFIRMED, CANCELLED, COMPLETED }

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "nights")
    private Integer nights;

    @Builder.Default
    @Column(name = "rooms_count", nullable = false)
    private Integer roomsCount = 1;

    @Builder.Default
    @Column(name = "adults", nullable = false)
    private Integer adults = 1;

    @Builder.Default
    @Column(name = "children", nullable = false)
    private Integer children = 0;

    @Column(name = "rate_per_night", precision = 18, scale = 2)
    private BigDecimal ratePerNight;

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "confirmation_number", length = 100)
    private String confirmationNumber;

    @Column(name = "supplier_reference", length = 100)
    private String supplierReference;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    /** Soft FK → trv_booking_services (the line this hotel stay belongs to). */
    @Column(name = "booking_service_id", nullable = false)
    private Long bookingServiceId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Column(name = "meal_plan_id")
    private Long mealPlanId;
}
