package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPassenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvPassengerRepository extends JpaRepository<TrvPassenger, Long> {

    List<TrvPassenger> findByBookingId(Long bookingId);
}
