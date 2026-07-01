package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvHotelGuest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvHotelGuestRepository extends JpaRepository<TrvHotelGuest, Long> {

    List<TrvHotelGuest> findByHotelBookingId(Long hotelBookingId);
}
