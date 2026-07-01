package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvVisaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvVisaTypeRepository extends JpaRepository<TrvVisaType, Long> {

    List<TrvVisaType> findByCountryIgnoreCase(String country);
}
