package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvRoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvRoomTypeRepository extends JpaRepository<TrvRoomType, Long> {

    List<TrvRoomType> findByHotelIdAndIsActiveTrue(Long hotelId);
}
