package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.organization.dto.WarehouseDTO;
import com.asg.spindleserp.organization.service.WarehouseService;
import com.asg.spindleserp.security.auth.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WarehouseController  /warehouses
 *
 * JS fn → endpoint mapping (must match wh-index.html):
 *   whShow(id)   GET    /warehouses/show/{id}
 *   whEdit(id)   GET    /warehouses/show/{id}   (same; form toggles mode)
 *   whToggle(id) POST   /warehouses/toggle/{id}
 *   whDelete(id) DELETE /warehouses/delete/{id}
 *   (save)       POST   /warehouses/save
 *
 * Response envelope: { success, message, obj: { defaultData: {...} } }
 * — mirrors BusinessUnitController exactly.
 */
@Slf4j
@Controller
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    // ── Page ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "warehouses");
        return "organizations/wh-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return warehouseService.datatableList(draw, start, length, search);
    }

    // ── Show / pre-fill ───────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            WarehouseDTO dto = warehouseService.findById(id);
            res.put("success", true);
            res.put("obj", Map.of("defaultData", dto));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Save (create / update) ────────────────────────────────────────────────

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody @Valid WarehouseDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                warehouseService.update(dto.getId(), dto);
                res.put("message", "Warehouse updated successfully.");
            } else {
                warehouseService.create(dto);
                res.put("message", "Warehouse created successfully.");
            }
            res.put("success", true);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Toggle status ─────────────────────────────────────────────────────────

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            WarehouseDTO dto = warehouseService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Warehouse " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            warehouseService.delete(id);
            res.put("success", true);
            res.put("message", "Warehouse deleted successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Reference data ────────────────────────────────────────────────────────

    /**
     * Active warehouses for the current org — for stock/transfer form dropdowns.
     * GET /warehouses/active
     */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<WarehouseDTO> list = orgId != null
                ? warehouseService.findActiveByOrg(orgId)
                : warehouseService.findAll().stream().filter(w -> Boolean.TRUE.equals(w.getActive())).toList();
        return list.stream().map(w -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             w.getId());
            m.put("code",           w.getWarehouseCode());
            m.put("name",           w.getWarehouseName());
            m.put("itemType",       w.getItemType());
            m.put("businessUnitId", w.getBusinessUnitId());
            return m;
        }).toList();
    }

    /**
     * ItemType enum values for the form select.
     * GET /warehouses/item-types
     */
    @GetMapping("/item-types")
    @ResponseBody
    public List<String> itemTypes() {
        return Arrays.stream(ItemType.values()).map(Enum::name).toList();
    }
}
