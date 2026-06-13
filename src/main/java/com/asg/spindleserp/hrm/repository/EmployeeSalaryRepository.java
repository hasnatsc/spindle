package com.asg.spindleserp.hrm.repository;

import com.asg.spindleserp.hrm.entity.EmployeeSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary, Long> {
    Optional<EmployeeSalary> findByEmployeeIdAndIsCurrentTrue(Long empId);

    List<EmployeeSalary> findByEmployeeIdOrderByEffectiveDateDesc(Long empId);
}
