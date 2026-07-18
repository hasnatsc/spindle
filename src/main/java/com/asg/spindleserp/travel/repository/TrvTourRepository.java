package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvTour;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrvTourRepository extends JpaRepository<TrvTour, Long> {
    List<TrvTour> findByIsActiveTrue();
}
