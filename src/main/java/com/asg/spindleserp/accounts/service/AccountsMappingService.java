package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.AccountsMappingDTO;
import com.asg.spindleserp.accounts.entity.AccountsMapping;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;
import java.util.Map;

public interface AccountsMappingService {

    AccountsMappingDTO create(AccountsMappingDTO dto);

    AccountsMappingDTO update(Long id, AccountsMappingDTO dto);

    AccountsMappingDTO findById(Long id);

    List<AccountsMappingDTO> findAll();

    AccountsMappingDTO toggleStatus(Long id);

    void delete(Long id);

    DataTableResponse datatableList(int draw, int start, int length, String search);

    /** AJAX Select2 — returns {items:[{id,text,code,name}], hasMore} */
    Map<String, Object> search(String q, int page, int pageSize);

    AccountsMappingDTO toDTO(AccountsMapping entity);
}
