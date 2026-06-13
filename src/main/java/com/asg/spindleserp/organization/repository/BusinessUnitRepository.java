package com.asg.spindleserp.organization.repository;

import com.asg.spindleserp.organization.entity.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, Long> {
    List<BusinessUnit> findByOrganizationIdAndIsActiveTrue(Long orgId);

    Optional<BusinessUnit> findByOrganizationIdAndCode(Long orgId, String code);

    boolean existsByOrganizationIdAndCode(Long orgId, String code);
}
