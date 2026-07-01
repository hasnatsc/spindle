package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvHotelRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvHotelRoomRepository extends JpaRepository<TrvHotelRoom, Long> {

    List<TrvHotelRoom> findByHotelBookingId(Long hotelBookingId);
}
