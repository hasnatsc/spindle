package com.asg.spindleserp.organization.repository;

import com.asg.spindleserp.organization.entity.CostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterRepository
        extends JpaRepository<CostCenter, Long>,
                JpaSpecificationExecutor<CostCenter> {

    Optional<CostCenter> findByCostCenterCode(String code);

    List<CostCenter> findByBusinessUnitIdAndIsActiveTrueOrderByCostCenterName(Long buId);

    List<CostCenter> findByBusinessUnitOrganizationIdAndIsActiveTrue(Long orgId);

    List<CostCenter> findByParentCostCenterIdIsNullAndBusinessUnitOrganizationId(Long orgId);

    boolean existsByCostCenterCode(String code);

    boolean existsByCostCenterCodeAndIdNot(String code, Long id);

    boolean existsByParentCostCenterId(Long parentId);
}
