package com.asg.spindleserp.inventory.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.inventory.dto.ItemDTO;
import com.asg.spindleserp.inventory.entity.Item;
import java.util.List;

public interface ItemService {
    ItemDTO create(ItemDTO dto);
    ItemDTO update(Long id, ItemDTO dto);
    ItemDTO findById(Long id);
    List<ItemDTO> findAll();
    List<ItemDTO> findActiveByOrg(Long orgId);
    List<ItemDTO> findByOrgAndType(Long orgId, ItemType type);
    List<ItemDTO> findProductionInputs(Long orgId);
    List<ItemDTO> findFinishedGoods(Long orgId);
    void delete(Long id);
    ItemDTO toggleStatus(Long id);
    DataTableResponse datatableList(int draw, int start, int length, String search);
    ItemDTO toDTO(Item entity);
}
