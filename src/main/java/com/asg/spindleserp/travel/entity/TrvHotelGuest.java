package com.asg.spindleserp.travel.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_hotel_guests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelGuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_booking_id", nullable = false)
    private Long hotelBookingId;

    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;

    @Column(name = "room_id")
    private Long roomId;

    // ── JPA object mappings ────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_booking_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvHotelBooking hotelBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvPassenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    @JsonIgnore
    private TrvHotelRoom room;
}
