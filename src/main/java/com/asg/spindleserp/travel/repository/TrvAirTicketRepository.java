package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvAirTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrvAirTicketRepository extends JpaRepository<TrvAirTicket, Long> {

    Optional<TrvAirTicket> findByBookingServiceId(Long bookingServiceId);
}
