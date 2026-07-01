package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvTourBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrvTourBookingRepository extends JpaRepository<TrvTourBooking, Long> {

    Optional<TrvTourBooking> findByBookingServiceId(Long bookingServiceId);
}
