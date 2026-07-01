package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvBookingService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvBookingServiceRepository extends JpaRepository<TrvBookingService, Long> {

    List<TrvBookingService> findByBookingId(Long bookingId);
}
