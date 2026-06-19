package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.dto.ItemModelDTO;
import com.asg.spindleserp.inventory.entity.ItemModel;
import java.util.List;

public interface ItemModelService {
    ItemModelDTO create(ItemModelDTO dto);
    ItemModelDTO update(Long id, ItemModelDTO dto);
    ItemModelDTO findById(Long id);
    List<ItemModelDTO> findAll();
    List<ItemModelDTO> findActiveByOrg(Long orgId);
    List<ItemModelDTO> findActiveByBrand(Long brandId);
    void delete(Long id);
    ItemModelDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    ItemModelDTO toDTO(ItemModel entity);
}
