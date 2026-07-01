package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPackageItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvPackageItineraryDayRepository extends JpaRepository<TrvPackageItineraryDay, Long> {

    List<TrvPackageItineraryDay> findByPackageEntityIdOrderByDayNumber(Long packageId);
}
