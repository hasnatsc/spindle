package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.HsCodeDTO;
import com.asg.spindleserp.setup.entity.HsCode;

import java.util.List;

public interface HsCodeService {
    HsCodeDTO create(HsCodeDTO dto);
    HsCodeDTO update(Long id, HsCodeDTO dto);
    HsCodeDTO findById(Long id);
    List<HsCodeDTO> findAll();
    List<HsCodeDTO> findActiveByOrg(Long orgId);
    void delete(Long id);
    HsCodeDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    HsCodeDTO toDTO(HsCode entity);
}
