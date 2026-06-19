package com.asg.spindleserp.inventory.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.dto.ItemBrandDTO;
import com.asg.spindleserp.inventory.service.ItemBrandService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ItemBrandController  /item-brands
 * JS fns: brandShow / brandEdit / brandToggle / brandDelete / brandOpenCreate
 */
@Slf4j
@Controller
@RequestMapping("/item-brands")
@RequiredArgsConstructor
public class ItemBrandController {

    private final ItemBrandService brandService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "item-brands");
        return "inventory/brand-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return brandService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", brandService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ItemBrandDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { brandService.update(dto.getId(), dto); res.put("message", "Brand updated successfully."); }
            else                     { brandService.create(dto);              res.put("message", "Brand created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            ItemBrandDTO dto = brandService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Brand " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { brandService.delete(id); res.put("success", true); res.put("message", "Brand deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<ItemBrandDTO> list = orgId != null ? brandService.findActiveByOrg(orgId)
                : brandService.findAll().stream().filter(b -> Boolean.TRUE.equals(b.getActive())).toList();
        return list.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   b.getId());
            m.put("code", b.getBrandCode());
            m.put("name", b.getBrandName());
            return m;
        }).toList();
    }
}
