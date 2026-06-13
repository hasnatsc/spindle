package com.asg.spindleserp.budget.repository;

import com.asg.spindleserp.budget.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    Optional<FiscalYear> findByOrganizationIdAndYearCode(Long orgId, String code);

    Optional<FiscalYear> findByOrganizationIdAndIsCurrentTrue(Long orgId);

    List<FiscalYear> findByOrganizationIdAndStatus(Long orgId, FiscalYear.FiscalYearStatus status);
}
