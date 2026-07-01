package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvHotelCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvHotelCancellationRepository extends JpaRepository<TrvHotelCancellation, Long> {

    List<TrvHotelCancellation> findByHotelBookingId(Long hotelBookingId);
}
