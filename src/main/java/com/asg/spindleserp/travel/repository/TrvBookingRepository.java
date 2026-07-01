package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrvBookingRepository extends JpaRepository<TrvBooking, Long> {

    Optional<TrvBooking> findByOrganizationIdAndBookingNo(Long organizationId, String bookingNo);

    boolean existsByOrganizationIdAndBookingNo(Long organizationId, String bookingNo);
}
