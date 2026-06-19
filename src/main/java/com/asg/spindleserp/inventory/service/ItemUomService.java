// ─────────────────────────────────────────────────────────────────────────────
// FILE: ItemUomService.java
// ─────────────────────────────────────────────────────────────────────────────
package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.dto.ItemUomDTO;
import com.asg.spindleserp.inventory.entity.ItemUom;
import java.util.List;

public interface ItemUomService {
    ItemUomDTO create(ItemUomDTO dto);
    ItemUomDTO update(Long id, ItemUomDTO dto);
    ItemUomDTO findById(Long id);
    List<ItemUomDTO> findAll();
    List<ItemUomDTO> findActiveByOrg(Long orgId);
    void delete(Long id);
    ItemUomDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    ItemUomDTO toDTO(ItemUom entity);
}
