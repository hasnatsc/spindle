package com.asg.spindleserp.travel.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trv_hotel_rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrvHotelRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", length = 30)
    private String roomNumber;

    @Column(name = "room_type_snapshot", length = 150)
    private String roomTypeSnapshot;

    @Column(name = "hotel_booking_id", nullable = false)
    private Long hotelBookingId;
}
