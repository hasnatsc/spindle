package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.BankDTO;
import com.asg.spindleserp.setup.entity.Bank;

import java.util.List;

public interface BankService {
    BankDTO create(BankDTO dto);
    BankDTO update(Long id, BankDTO dto);
    BankDTO findById(Long id);
    List<BankDTO> findAll();
    List<BankDTO> findActiveByOrg(Long orgId);
    List<BankDTO> findLcBanksByOrg(Long orgId);
    void delete(Long id);
    BankDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    BankDTO toDTO(Bank entity);
}
