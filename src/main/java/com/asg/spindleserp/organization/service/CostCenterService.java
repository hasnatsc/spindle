package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.dto.CostCenterDTO;
import com.asg.spindleserp.organization.entity.CostCenter;

import java.util.List;

/**
 * CostCenterService
 *
 * Contract for CostCenter CRUD and DataTable operations.
 * Pattern mirrors BusinessUnitService.
 */
public interface CostCenterService {

    CostCenterDTO create(CostCenterDTO dto);

    CostCenterDTO update(Long id, CostCenterDTO dto);

    CostCenterDTO findById(Long id);

    List<CostCenterDTO> findAll();

    List<CostCenterDTO> findActiveByOrg(Long orgId);

    List<CostCenterDTO> findActiveByBusinessUnit(Long buId);

    /** All cost centers except the given id — for parent dropdown */
    List<CostCenterDTO> findParentCandidates(Long excludeId);

    void delete(Long id);

    CostCenterDTO toggleStatus(Long id);

    DataTableResponse datatableList(int draw, int start, int length, String search);

    CostCenterDTO toDTO(CostCenter entity);
}
