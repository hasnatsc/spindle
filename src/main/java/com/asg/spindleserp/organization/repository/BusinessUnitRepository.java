package com.asg.spindleserp.organization.repository;

import com.asg.spindleserp.organization.entity.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessUnitRepository
        extends JpaRepository<BusinessUnit, Long>,
                JpaSpecificationExecutor<BusinessUnit> {

    List<BusinessUnit> findByOrganizationIdAndIsActiveTrue(Long orgId);

    List<BusinessUnit> findByIsActiveTrue();

    Optional<BusinessUnit> findByOrganizationIdAndCode(Long orgId, String code);

    boolean existsByOrganizationIdAndCode(Long orgId, String code);

    /** Used in update — skip the record being edited */
    boolean existsByOrganizationIdAndCodeAndIdNot(Long orgId, String code, Long id);
}
