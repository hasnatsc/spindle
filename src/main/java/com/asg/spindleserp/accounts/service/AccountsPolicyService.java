package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.AccountsPolicyDTO;
import com.asg.spindleserp.accounts.entity.AccountsPolicy;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;

public interface AccountsPolicyService {

    AccountsPolicyDTO create(AccountsPolicyDTO dto);

    AccountsPolicyDTO update(Long id, AccountsPolicyDTO dto);

    AccountsPolicyDTO findById(Long id);

    List<AccountsPolicyDTO> findAll();

    AccountsPolicyDTO toggleStatus(Long id);

    void delete(Long id);

    DataTableResponse datatableList(int draw, int start, int length, String search);

    AccountsPolicyDTO toDTO(AccountsPolicy entity);
}
