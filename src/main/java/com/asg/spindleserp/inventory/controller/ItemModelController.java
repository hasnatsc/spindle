package com.asg.spindleserp.inventory.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.inventory.dto.ItemModelDTO;
import com.asg.spindleserp.inventory.service.ItemModelService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ItemModelController  /item-models
 * JS fns: modelShow / modelEdit / modelToggle / modelDelete / modelOpenCreate
 */
@Slf4j
@Controller
@RequestMapping("/item-models")
@RequiredArgsConstructor
public class ItemModelController {

    private final ItemModelService modelService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "item-models");
        return "inventory/model-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return modelService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { res.put("success", true); res.put("obj", Map.of("defaultData", modelService.findById(id))); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid ItemModelDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) { modelService.update(dto.getId(), dto); res.put("message", "Model updated successfully."); }
            else                     { modelService.create(dto);              res.put("message", "Model created successfully."); }
            res.put("success", true);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            ItemModelDTO dto = modelService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Model " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try { modelService.delete(id); res.put("success", true); res.put("message", "Model deleted successfully."); }
        catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** All active models for current org */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<ItemModelDTO> list = orgId != null ? modelService.findActiveByOrg(orgId)
                : modelService.findAll().stream().filter(m -> Boolean.TRUE.equals(m.getActive())).toList();
        return list.stream().map(m -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id",       m.getId());
            r.put("code",     m.getModelCode());
            r.put("name",     m.getModelName());
            r.put("brandId",  m.getBrandId());
            return r;
        }).toList();
    }

    /** Models filtered by brand — called from Item form when brand changes */
    @GetMapping("/by-brand/{brandId}")
    @ResponseBody
    public List<Map<String, Object>> byBrand(@PathVariable Long brandId) {
        return modelService.findActiveByBrand(brandId).stream().map(m -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id",   m.getId());
            r.put("code", m.getModelCode());
            r.put("name", m.getModelName());
            return r;
        }).toList();
    }
}
