package com.asg.spindleserp.hrm.repository;

import com.asg.spindleserp.hrm.entity.PayrollRunLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRunLineRepository extends JpaRepository<PayrollRunLine, Long> {
    List<PayrollRunLine> findByPayrollRunId(Long runId);

    Optional<PayrollRunLine> findByPayrollRunIdAndEmployeeId(Long runId, Long empId);
}
