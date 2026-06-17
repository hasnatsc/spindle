package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.dto.DepartmentDTO;
import com.asg.spindleserp.organization.entity.Department;

import java.util.List;

/**
 * DepartmentService
 *
 * Contract for Department CRUD and DataTable operations.
 * Pattern mirrors BusinessUnitService.
 */
public interface DepartmentService {

    DepartmentDTO create(DepartmentDTO dto);

    DepartmentDTO update(Long id, DepartmentDTO dto);

    DepartmentDTO findById(Long id);

    List<DepartmentDTO> findAll();

    List<DepartmentDTO> findActiveByOrg(Long orgId);

    /** All departments except the given id — for parent dropdown */
    List<DepartmentDTO> findParentCandidates(Long excludeId);

    void delete(Long id);

    DepartmentDTO toggleStatus(Long id);

    DataTableResponse datatableList(int draw, int start, int length, String search);

    DepartmentDTO toDTO(Department entity);
}
