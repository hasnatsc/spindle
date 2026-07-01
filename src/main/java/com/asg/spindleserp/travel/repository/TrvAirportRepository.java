package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvAirport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvAirportRepository extends JpaRepository<TrvAirport, Long> {

    List<TrvAirport> findByAirportNameContainingIgnoreCaseOrAirportCodeContainingIgnoreCase(
            String name, String code);
}
