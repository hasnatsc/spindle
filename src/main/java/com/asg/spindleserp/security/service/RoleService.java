package com.asg.spindleserp.security.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.security.dto.RoleDTO;

import java.util.List;
import java.util.Optional;

public interface RoleService {

    RoleDTO create(RoleDTO dto);
    Optional<RoleDTO> findById(Long id);
    RoleDTO update(Long id, RoleDTO dto);
    void delete(Long id);
    RoleDTO toggleStatus(Long id);

    /** All active roles (super-admin sees all; org-admin sees module-filtered). */
    List<RoleDTO> findAll();

    /**
     * Roles visible to the current user for assignment in the users form.
     * Super-admin: all roles.
     * Org-admin: only roles whose permissions belong to modules active for their org.
     */
    List<RoleDTO> findAllForCurrentOrg();

    DataTableResponse datatableList(int draw, int start, int length, String search);

    RoleDTO toDTO(com.asg.spindleserp.security.entity.Role role);
}
