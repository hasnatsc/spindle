package com.asg.spindleserp.hrm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {
    List<Designation> findByOrganizationIdAndIsActiveTrue(Long orgId);

    Optional<Designation> findByOrganizationIdAndDesignationCode(Long orgId, String code);

    boolean existsByOrganizationIdAndDesignationCode(Long orgId, String code);
}
