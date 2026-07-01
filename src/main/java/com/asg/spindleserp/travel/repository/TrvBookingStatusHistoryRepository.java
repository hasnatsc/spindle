package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvBookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvBookingStatusHistoryRepository extends JpaRepository<TrvBookingStatusHistory, Long> {

    List<TrvBookingStatusHistory> findByBookingIdOrderByChangedAtDesc(Long bookingId);
}
