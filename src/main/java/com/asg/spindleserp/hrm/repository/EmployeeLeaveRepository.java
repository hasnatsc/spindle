package com.asg.spindleserp.hrm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeLeaveRepository extends JpaRepository<EmployeeLeave, Long>, JpaSpecificationExecutor<EmployeeLeave> {
    List<EmployeeLeave> findByEmployeeIdAndStatus(Long empId, EmployeeLeave.LeaveStatus status);

    List<EmployeeLeave> findByOrganizationIdAndStatus(Long orgId, EmployeeLeave.LeaveStatus status);
}
