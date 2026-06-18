package com.asg.spindleserp.setup.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.setup.dto.CountryDTO;
import com.asg.spindleserp.setup.entity.Country;

import java.util.List;

public interface CountryService {
    CountryDTO create(CountryDTO dto);
    CountryDTO update(Long id, CountryDTO dto);
    CountryDTO findById(Long id);
    List<CountryDTO> findAll();
    List<CountryDTO> findActive();
    void delete(Long id);
    CountryDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    CountryDTO toDTO(Country entity);
}
