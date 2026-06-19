package com.asg.spindleserp.inventory.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.inventory.dto.ItemDTO;
import com.asg.spindleserp.inventory.service.ItemService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ItemController  /items
 * JS fns: itemShow / itemEdit / itemToggle / itemDelete / itemOpenCreate
 */
@Slf4j
@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "items");
        return "inventory/item-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return itemService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", itemService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ItemDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { itemService.update(dto.getId(), dto); res.put("message", "Item updated successfully."); }
            else                     { itemService.create(dto);              res.put("message", "Item created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            ItemDTO dto = itemService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Item " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { itemService.delete(id); res.put("success", true); res.put("message", "Item deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** All active items for current org */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.requireOrgId();
        return itemService.findActiveByOrg(orgId).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       i.getId());
            m.put("code",     i.getItemCode());
            m.put("name",     i.getItemName());
            m.put("itemType", i.getItemType());
            m.put("uom",      i.getUnitOfMeasure());
            return m;
        }).toList();
    }

    /** Production BOM inputs: RAW_MATERIAL, SEMI_FINISHED, CONSUMABLE, MRO */
    @GetMapping("/production-inputs")
    @ResponseBody
    public List<Map<String, Object>> productionInputs() {
        Long orgId = SecurityHelper.requireOrgId();
        return itemService.findProductionInputs(orgId).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       i.getId());
            m.put("code",     i.getItemCode());
            m.put("name",     i.getItemName());
            m.put("itemType", i.getItemType());
            m.put("uom",      i.getUnitOfMeasure());
            return m;
        }).toList();
    }

    /** FINISHED_GOOD and SEMI_FINISHED — for sales / production output */
    @GetMapping("/finished-goods")
    @ResponseBody
    public List<Map<String, Object>> finishedGoods() {
        Long orgId = SecurityHelper.requireOrgId();
        return itemService.findFinishedGoods(orgId).stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       i.getId());
            m.put("code",     i.getItemCode());
            m.put("name",     i.getItemName());
            m.put("uom",      i.getUnitOfMeasure());
            m.put("unitPrice", i.getUnitPrice());
            return m;
        }).toList();
    }

    /** ItemType enum values */
    @GetMapping("/item-types")
    @ResponseBody
    public List<String> itemTypes() {
        return Arrays.stream(ItemType.values()).map(Enum::name).toList();
    }
}
