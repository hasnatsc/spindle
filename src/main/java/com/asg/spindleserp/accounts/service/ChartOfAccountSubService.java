package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.ChartOfAccountSubDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccountSub;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;
import java.util.Map;

/**
 * ChartOfAccountSubService
 *
 * Unified CRUD for all ChartOfAccountSub sub-types.
 * Sub-type is determined by dto.getSubAccountType().
 */
public interface ChartOfAccountSubService {

    ChartOfAccountSubDTO create(ChartOfAccountSubDTO dto);

    ChartOfAccountSubDTO update(Long id, ChartOfAccountSubDTO dto);

    ChartOfAccountSubDTO findById(Long id);

    List<ChartOfAccountSubDTO> findAll();

    List<ChartOfAccountSubDTO> findByType(String subAccountType);

    ChartOfAccountSubDTO toggleStatus(Long id);

    void delete(Long id);

    DataTableResponse datatableList(String subAccountType, int draw, int start, int length, String search);

    /**
     * AJAX Select2 search across all active sub-accounts (optionally filtered by type).
     * Returns {items:[{id,text,code,name,subAccountType}], hasMore}
     */
    Map<String, Object> search(String q, String subAccountType, int page, int pageSize);

    ChartOfAccountSubDTO toDTO(ChartOfAccountSub entity);
}
