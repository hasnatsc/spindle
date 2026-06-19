package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.ChartOfAccountDTO;
import com.asg.spindleserp.accounts.entity.ChartOfAccount;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;
import java.util.Map;

/**
 * ChartOfAccountService
 *
 * CRUD + DataTable + AJAX search for Chart of Accounts.
 * Mirrors the BusinessUnitService canonical pattern.
 */
public interface ChartOfAccountService {

    ChartOfAccountDTO create(ChartOfAccountDTO dto);

    ChartOfAccountDTO update(Long id, ChartOfAccountDTO dto);

    ChartOfAccountDTO findById(Long id);

    List<ChartOfAccountDTO> findAll();

    List<ChartOfAccountDTO> findActiveByOrg(Long orgId);

    ChartOfAccountDTO toggleStatus(Long id);

    void delete(Long id);

    DataTableResponse datatableList(int draw, int start, int length, String search);

    /**
     * AJAX Select2 search — returns {items:[{id,text,code,name,accountType}], hasMore}.
     * Used by ChartOfAccountSubController form and AccountsMapping form.
     */
    Map<String, Object> search(String q, int page, int pageSize);

    ChartOfAccountDTO toDTO(ChartOfAccount entity);
}
