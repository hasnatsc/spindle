package com.asg.spindleserp.inventory.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.inventory.dto.ItemCategoryDTO;
import com.asg.spindleserp.inventory.entity.ItemCategory;
import com.asg.spindleserp.inventory.service.ItemCategoryService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ItemCategoryController  /item-categories
 * JS fns: catShow / catEdit / catToggle / catDelete / catOpenCreate
 */
@Slf4j
@Controller
@RequestMapping("/item-categories")
@RequiredArgsConstructor
public class ItemCategoryController {

    private final ItemCategoryService categoryService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "item-categories");
        return "inventory/category-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return categoryService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", categoryService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ItemCategoryDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { categoryService.update(dto.getId(), dto); res.put("message", "Category updated successfully."); }
            else                     { categoryService.create(dto);              res.put("message", "Category created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            ItemCategoryDTO dto = categoryService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Category " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { categoryService.delete(id); res.put("success", true); res.put("message", "Category deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Active categories for Item form dropdown */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<ItemCategoryDTO> list = orgId != null ? categoryService.findActiveByOrg(orgId)
                : categoryService.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getActive())).toList();
        return list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       c.getId());
            m.put("code",     c.getCategoryCode());
            m.put("name",     c.getCategoryName());
            m.put("itemType", c.getItemType() != null ? c.getItemType() : "");
            return m;
        }).toList();
    }

    /** Parent candidates (exclude self) */
    @GetMapping("/parents/all")
    @ResponseBody
    public List<Map<String, Object>> parents(@RequestParam(defaultValue = "0") Long excludeId) {
        return categoryService.findParentCandidates(excludeId).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   c.getId());
            m.put("code", c.getCategoryCode());
            m.put("name", c.getCategoryName());
            return m;
        }).toList();
    }

    /** ItemType enum values for the form select */
    @GetMapping("/item-types")
    @ResponseBody
    public List<String> itemTypes() {
        return Arrays.stream(ItemType.values()).map(Enum::name).toList();
    }

    /** LayerType enum values for the form select */
    @GetMapping("/layer-types")
    @ResponseBody
    public List<String> layerTypes() {
        return Arrays.stream(ItemCategory.LayerType.values()).map(Enum::name).toList();
    }
}
