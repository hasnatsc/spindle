package com.asg.spindleserp.inventory.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.dto.ItemUomDTO;
import com.asg.spindleserp.inventory.entity.ItemUom;
import com.asg.spindleserp.inventory.service.ItemUomService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ItemUomController  /item-uoms
 * JS fns: uomShow / uomEdit / uomToggle / uomDelete / uomOpenCreate
 */
@Slf4j
@Controller
@RequestMapping("/item-uoms")
@RequiredArgsConstructor
public class ItemUomController {

    private final ItemUomService uomService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "item-uoms");
        return "inventory/uom-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return uomService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", uomService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ItemUomDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            // org is always from session — never trust client
            if (dto.getId() != null) { uomService.update(dto.getId(), dto); res.put("message", "UOM updated successfully."); }
            else                     { uomService.create(dto);              res.put("message", "UOM created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            ItemUomDTO dto = uomService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "UOM " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { uomService.delete(id); res.put("success", true); res.put("message", "UOM deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Active UOMs for Item form dropdowns */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<ItemUomDTO> list = orgId != null ? uomService.findActiveByOrg(orgId)
                : uomService.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getActive())).toList();
        return list.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",       u.getId());
            m.put("code",     u.getCode());
            m.put("name",     u.getName());
            m.put("symbol",   u.getSymbol() != null ? u.getSymbol() : "");
            m.put("category", u.getCategory());
            return m;
        }).toList();
    }

    /** UomCategory enum values for the form select */
    @GetMapping("/categories")
    @ResponseBody
    public List<String> categories() {
        return Arrays.stream(ItemUom.UomCategory.values()).map(Enum::name).toList();
    }
}
