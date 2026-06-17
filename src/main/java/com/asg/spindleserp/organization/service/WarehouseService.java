package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.dto.WarehouseDTO;
import com.asg.spindleserp.organization.entity.Warehouse;

import java.util.List;

/**
 * WarehouseService
 *
 * Contract for Warehouse CRUD and DataTable operations.
 * Pattern mirrors BusinessUnitService.
 */
public interface WarehouseService {

    WarehouseDTO create(WarehouseDTO dto);

    WarehouseDTO update(Long id, WarehouseDTO dto);

    WarehouseDTO findById(Long id);

    List<WarehouseDTO> findAll();

    List<WarehouseDTO> findActiveByOrg(Long orgId);

    List<WarehouseDTO> findActiveByBusinessUnit(Long buId);

    void delete(Long id);

    WarehouseDTO toggleStatus(Long id);

    DataTableResponse datatableList(int draw, int start, int length, String search);

    WarehouseDTO toDTO(Warehouse entity);
}
