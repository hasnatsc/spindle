package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvTourGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvTourGuideRepository extends JpaRepository<TrvTourGuide, Long> {

    List<TrvTourGuide> findByIsActiveTrue();
}
