package com.asg.spindleserp.organization.repository;

import com.asg.spindleserp.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository
        extends JpaRepository<Department, Long>,
                JpaSpecificationExecutor<Department> {

    List<Department> findByOrganizationIdAndActiveTrue(Long orgId);

    Optional<Department> findByCode(String code);

    Optional<Department> findByName(String name);

    List<Department> findByParentDepartmentIdIsNullAndOrganizationId(Long orgId);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByParentDepartmentId(Long parentId);
}
