package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.dto.BusinessUnitDTO;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.organization.service.BusinessUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BusinessUnitController  /business-units
 *
 * JS fn → endpoint mapping (must match business-unit-index.html):
 *   buShow(id)   GET    /business-units/show/{id}
 *   buEdit(id)   GET    /business-units/show/{id}   (same; form toggles mode)
 *   buToggle(id) POST   /business-units/toggle/{id}
 *   buDelete(id) DELETE /business-units/delete/{id}
 *   (save)       POST   /business-units/save
 *
 * Response envelope: { success, message, obj: { defaultData: {...} } }
 */
@Slf4j
@Controller
@RequestMapping("/business-units")
@RequiredArgsConstructor
public class BusinessUnitController {

    private final BusinessUnitService businessUnitService;
    private final OrganizationRepository organizationRepository;

    // ── Page ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "businessUnits");
        return "organizations/business-unit-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return businessUnitService.datatableList(draw, start, length, search);
    }

    // ── Show / pre-fill ───────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BusinessUnitDTO dto = businessUnitService.findById(id);
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
    public Map<String, Object> save(@RequestBody @Valid BusinessUnitDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                businessUnitService.update(dto.getId(), dto);
                res.put("message", "Business Unit updated successfully.");
            } else {
                businessUnitService.create(dto);
                res.put("message", "Business Unit created successfully.");
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
            BusinessUnitDTO dto = businessUnitService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Business Unit " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
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
            businessUnitService.delete(id);
            res.put("success", true);
            res.put("message", "Business Unit deleted successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Reference data ────────────────────────────────────────────────────────

    /**
     * Organizations dropdown for the BU form.
     * GET /business-units/organizations/all
     */
    @GetMapping("/organizations/all")
    @ResponseBody
    public List<Map<String, Object>> allOrganizations() {
        return organizationRepository.findByIsActiveTrue().stream()
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",   o.getId());
                    m.put("code", o.getCode());
                    m.put("name", o.getName());
                    return m;
                })
                .toList();
    }

    /**
     * Active business units — for Warehouse / Cost Center / User scope dropdowns.
     * GET /business-units/active-list
     */
    @GetMapping("/active-list")
    @ResponseBody
    public List<Map<String, Object>> activeList() {
        return businessUnitService.findAll().stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getActive()))
                .map(dto -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",             dto.getId());
                    m.put("code",           dto.getCode());
                    m.put("name",           dto.getName());
                    m.put("organizationId", dto.getOrganizationId());
                    return m;
                })
                .toList();
    }
}
