package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPassengerTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvPassengerTicketRepository extends JpaRepository<TrvPassengerTicket, Long> {

    List<TrvPassengerTicket> findByAirTicketId(Long airTicketId);

    List<TrvPassengerTicket> findByPassengerId(Long passengerId);
}
