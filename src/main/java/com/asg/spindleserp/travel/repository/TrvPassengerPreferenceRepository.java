package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvPassengerPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * DROP-IN REPLACEMENT — findByPassengerId is unchanged; deleteByPassengerId is
 * new and is required so deleting a passenger cleans up its preference row
 * before the FK bites.
 */
public interface TrvPassengerPreferenceRepository extends JpaRepository<TrvPassengerPreference, Long> {

    Optional<TrvPassengerPreference> findByPassengerId(Long passengerId);

    void deleteByPassengerId(Long passengerId);
}
