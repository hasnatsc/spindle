package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvHotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrvHotelRepository extends JpaRepository<TrvHotel, Long> {

    List<TrvHotel> findByOrganizationIdAndIsActiveTrue(Long organizationId);

    Optional<TrvHotel> findByOrganizationIdAndHotelCode(Long organizationId, String hotelCode);

    List<TrvHotel> findByOrganizationIdAndCityIgnoreCase(Long organizationId, String city);
}
