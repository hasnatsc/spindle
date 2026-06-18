package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.CurrencyDTO;
import com.asg.spindleserp.setup.entity.Currency;

import java.util.List;

public interface CurrencyService {
    CurrencyDTO create(CurrencyDTO dto);
    CurrencyDTO update(Long id, CurrencyDTO dto);
    CurrencyDTO findById(Long id);
    List<CurrencyDTO> findAll();
    List<CurrencyDTO> findActive();
    void delete(Long id);
    CurrencyDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    CurrencyDTO toDTO(Currency entity);
}
