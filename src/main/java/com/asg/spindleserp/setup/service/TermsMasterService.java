package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.TermsMasterDTO;
import com.asg.spindleserp.setup.entity.TermsMaster;

import java.util.List;

public interface TermsMasterService {
    TermsMasterDTO create(TermsMasterDTO dto);
    TermsMasterDTO update(Long id, TermsMasterDTO dto);
    TermsMasterDTO findById(Long id);
    List<TermsMasterDTO> findAll();
    List<TermsMasterDTO> findActiveByDocumentType(String documentType);
    void delete(Long id);
    TermsMasterDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    TermsMasterDTO toDTO(TermsMaster entity);
}
