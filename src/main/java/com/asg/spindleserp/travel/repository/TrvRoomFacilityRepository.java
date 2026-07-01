package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvRoomFacility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvRoomFacilityRepository extends JpaRepository<TrvRoomFacility, Long> {

    List<TrvRoomFacility> findByRoomTypeId(Long roomTypeId);
}
