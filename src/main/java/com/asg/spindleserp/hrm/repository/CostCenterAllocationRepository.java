package com.asg.spindleserp.hrm.repository;

import com.asg.spindleserp.hrm.entity.CostCenterAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ★ NEW in v2: Labor cost → production cost center linking
 */
@Repository
public interface CostCenterAllocationRepository extends JpaRepository<CostCenterAllocation, Long> {
    List<CostCenterAllocation> findByCostCenterIdAndAllocationMonth(Long costCenterId, String month);

    List<CostCenterAllocation> findByPayrollRunId(Long payrollRunId);

    List<CostCenterAllocation> findByEmployeeIdAndAllocationMonth(Long empId, String month);

    // Total labor cost for a cost center in a given month — used by ProductionCostService
    @Query("SELECT COALESCE(SUM(a.allocatedAmount), 0) FROM CostCenterAllocation a " +
            "WHERE a.costCenter.id = :costCenterId AND a.allocationMonth = :month")
    java.math.BigDecimal sumAllocatedAmountByCostCenterAndMonth(
            @Param("costCenterId") Long costCenterId,
            @Param("month") String month);
}
