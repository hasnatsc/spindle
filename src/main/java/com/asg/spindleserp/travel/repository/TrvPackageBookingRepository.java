package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPackageBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrvPackageBookingRepository extends JpaRepository<TrvPackageBooking, Long> {

    Optional<TrvPackageBooking> findByBookingServiceId(Long bookingServiceId);
}
