package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.dto.ItemBrandDTO;
import com.asg.spindleserp.inventory.entity.ItemBrand;
import java.util.List;

public interface ItemBrandService {
    ItemBrandDTO create(ItemBrandDTO dto);
    ItemBrandDTO update(Long id, ItemBrandDTO dto);
    ItemBrandDTO findById(Long id);
    List<ItemBrandDTO> findAll();
    List<ItemBrandDTO> findActiveByOrg(Long orgId);
    void delete(Long id);
    ItemBrandDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    ItemBrandDTO toDTO(ItemBrand entity);
}
