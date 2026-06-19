package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.dto.ItemCategoryDTO;
import com.asg.spindleserp.inventory.entity.ItemCategory;
import java.util.List;

public interface ItemCategoryService {
    ItemCategoryDTO create(ItemCategoryDTO dto);
    ItemCategoryDTO update(Long id, ItemCategoryDTO dto);
    ItemCategoryDTO findById(Long id);
    List<ItemCategoryDTO> findAll();
    List<ItemCategoryDTO> findActiveByOrg(Long orgId);
    List<ItemCategoryDTO> findParentCandidates(Long excludeId);
    void delete(Long id);
    ItemCategoryDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    ItemCategoryDTO toDTO(ItemCategory entity);
}
