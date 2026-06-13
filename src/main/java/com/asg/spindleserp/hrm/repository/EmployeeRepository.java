package com.asg.spindleserp.hrm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmployeeCode(String code);

    Optional<Employee> findByPhone(String phone);

    Optional<Employee> findByNationalId(String nationalId);

    Optional<Employee> findByUserId(Long userId);

    List<Employee> findByOrganizationIdAndStatus(Long orgId, Employee.EmployeeStatus status);

    List<Employee> findByOrganizationIdAndDepartmentId(Long orgId, Long deptId);

    List<Employee> findByReportingManagerId(Long managerId);

    long countByOrganizationIdAndStatus(Long orgId, Employee.EmployeeStatus status);

    boolean existsByEmployeeCode(String code);

    @Query("SELECT e FROM Employee e WHERE e.organizationId = :orgId AND e.status = 'ACTIVE' " +
            "AND (LOWER(e.firstName) LIKE LOWER(CONCAT('%',:q,'%')) " +
            "OR   LOWER(e.lastName)  LIKE LOWER(CONCAT('%',:q,'%')) " +
            "OR   LOWER(e.employeeCode) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Employee> search(@Param("orgId") Long orgId, @Param("q") String q, Pageable p);
}
