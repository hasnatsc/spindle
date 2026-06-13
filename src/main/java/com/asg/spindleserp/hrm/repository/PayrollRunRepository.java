package com.asg.spindleserp.hrm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long>, JpaSpecificationExecutor<PayrollRun> {
    Optional<PayrollRun> findByOrganizationIdAndPayrollMonth(Long orgId, String month);

    List<PayrollRun> findByOrganizationIdAndStatus(Long orgId, PayrollRun.PayrollStatus status);
}
