package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvBookingReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvBookingReceiptRepository extends JpaRepository<TrvBookingReceipt, Long> {
    List<TrvBookingReceipt> findByBookingId(Long bookingId);
    void deleteByBookingId(Long bookingId);
}
