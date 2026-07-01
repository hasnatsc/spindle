package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvGlAccountDefaults;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrvGlAccountDefaultsRepository extends JpaRepository<TrvGlAccountDefaults, Long> {

    Optional<TrvGlAccountDefaults> findByOrganizationId(Long organizationId);
}
