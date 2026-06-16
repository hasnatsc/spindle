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

    List<RoleDTO> findAll();
    DataTableResponse datatableList(int draw, int start, int length, String search);

    RoleDTO toDTO(com.asg.spindleserp.security.entity.Role role);
}
