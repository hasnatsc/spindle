package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvHotelBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrvHotelBookingRepository extends JpaRepository<TrvHotelBooking, Long> {

    Optional<TrvHotelBooking> findByBookingServiceId(Long bookingServiceId);
}
