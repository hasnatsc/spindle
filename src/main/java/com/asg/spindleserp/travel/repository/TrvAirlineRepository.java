package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvAirline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvAirlineRepository extends JpaRepository<TrvAirline, Long> {

    List<TrvAirline> findByIsActiveTrue();
}
