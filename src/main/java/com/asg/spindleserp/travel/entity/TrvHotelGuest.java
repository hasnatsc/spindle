package com.asg.spindleserp.travel.entity;

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
}
