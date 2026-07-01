package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvVisaApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrvVisaApplicationRepository extends JpaRepository<TrvVisaApplication, Long> {

    Optional<TrvVisaApplication> findByBookingServiceId(Long bookingServiceId);

    List<TrvVisaApplication> findByPassengerId(Long passengerId);
}
