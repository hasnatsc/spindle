package com.asg.spindleserp.travel.entity;

import com.asg.spindleserp.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trv_hotel_bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelBooking extends BaseEntity implements Serializable {

    public enum Status { PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW }

    public enum BookingSource { DIRECT, ONLINE_TRAVEL_AGENCY, CORPORATE, TRAVEL_AGENT, GDS, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // ── Booking source / channel ───────────────────────────────────────────
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", length = 25)
    private BookingSource bookingSource = BookingSource.DIRECT;

    @Column(name = "cancellation_policy", length = 2000)
    private String cancellationPolicy;

    @Column(name = "free_cancellation_until")
    private LocalDate freeCancellationUntil;

    @Builder.Default
    @Column(name = "deposit_amount", precision = 18, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "balance_due_date")
    private LocalDate balanceDueDate;

    @Column(name = "special_requests", length = 2000)
    private String specialRequests;

    @Builder.Default
    @Column(name = "booking_currency", nullable = false, length = 3)
    private String bookingCurrency = "BDT";

    @Builder.Default
    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_amount", precision = 18, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vendor_confirmation_received", nullable = false)
    private Boolean vendorConfirmationReceived = false;

    @Column(name = "vendor_remarks", length = 1000)
    private String vendorRemarks;

    // ── JPA object mappings ────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvHotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvRoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvMealPlan mealPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_service_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvBookingService bookingService;

    @Builder.Default
    @OneToMany(mappedBy = "hotelBooking", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TrvHotelRoom> rooms = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "hotelBooking", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TrvHotelGuest> guests = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "hotelBooking", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TrvHotelCancellation> cancellations = new ArrayList<>();
}
