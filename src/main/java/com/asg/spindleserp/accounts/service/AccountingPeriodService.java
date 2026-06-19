package com.asg.spindleserp.accounts.service;

import com.asg.spindleserp.accounts.dto.AccountingPeriodDTO;
import com.asg.spindleserp.accounts.entity.AccountingPeriod;
import com.asg.spindleserp.common.dto.DataTableResponse;

import java.util.List;

/**
 * AccountingPeriodService — mirrors BusinessUnitService canonical pattern.
 */
public interface AccountingPeriodService {

    AccountingPeriodDTO create(AccountingPeriodDTO dto);

    AccountingPeriodDTO update(Long id, AccountingPeriodDTO dto);

    AccountingPeriodDTO findById(Long id);

    List<AccountingPeriodDTO> findAll();

    List<AccountingPeriodDTO> findActiveByOrg(Long orgId);

    AccountingPeriodDTO toggleStatus(Long id);

    /** Close a period — sets isClosed=true, records closedBy + closedDate. */
    AccountingPeriodDTO closePeriod(Long id);

    void delete(Long id);

    DataTableResponse datatableList(int draw, int start, int length, String search);

    AccountingPeriodDTO toDTO(AccountingPeriod entity);
}
