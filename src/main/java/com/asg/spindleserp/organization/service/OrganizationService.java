package com.asg.spindleserp.organization.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.dto.OrganizationDTO;
import com.asg.spindleserp.organization.entity.Organization;

import java.util.List;

/**
 * OrganizationService
 *
 * Contract for Organization CRUD and DataTable operations.
 * Pattern mirrors BusinessUnitService.
 */
public interface OrganizationService {

    OrganizationDTO create(OrganizationDTO dto);

    OrganizationDTO update(Long id, OrganizationDTO dto);

    OrganizationDTO findById(Long id);

    List<OrganizationDTO> findAll();

    List<OrganizationDTO> findActive();

    void delete(Long id);

    OrganizationDTO toggleStatus(Long id);

    DataTableResponse datatableList(int draw, int start, int length, String search);

    OrganizationDTO toDTO(Organization entity);
}
