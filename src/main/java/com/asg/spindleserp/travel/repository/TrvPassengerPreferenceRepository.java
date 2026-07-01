package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPassengerPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrvPassengerPreferenceRepository extends JpaRepository<TrvPassengerPreference, Long> {

    Optional<TrvPassengerPreference> findByPassengerId(Long passengerId);
}
