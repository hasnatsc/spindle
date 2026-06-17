package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.dto.CostCenterDTO;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.service.CostCenterService;
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
 * CostCenterController  /cost-centers
 *
 * JS fn → endpoint mapping (must match cc-index.html):
 *   ccShow(id)   GET    /cost-centers/show/{id}
 *   ccEdit(id)   GET    /cost-centers/show/{id}   (same; form toggles mode)
 *   ccToggle(id) POST   /cost-centers/toggle/{id}
 *   ccDelete(id) DELETE /cost-centers/delete/{id}
 *   (save)       POST   /cost-centers/save
 *
 * Response envelope: { success, message, obj: { defaultData: {...} } }
 * — mirrors BusinessUnitController exactly.
 */
@Slf4j
@Controller
@RequestMapping("/cost-centers")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterService costCenterService;

    // ── Page ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "cost-centers");
        return "organizations/cc-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return costCenterService.datatableList(draw, start, length, search);
    }

    // ── Show / pre-fill ───────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            CostCenterDTO dto = costCenterService.findById(id);
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
    public Map<String, Object> save(@RequestBody @Valid CostCenterDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                costCenterService.update(dto.getId(), dto);
                res.put("message", "Cost Center updated successfully.");
            } else {
                costCenterService.create(dto);
                res.put("message", "Cost Center created successfully.");
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
            CostCenterDTO dto = costCenterService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Cost Center " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
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
            costCenterService.delete(id);
            res.put("success", true);
            res.put("message", "Cost Center deleted successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Reference data ────────────────────────────────────────────────────────

    /**
     * Active cost centers for the current org — for GL / approval form dropdowns.
     * GET /cost-centers/active
     */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<CostCenterDTO> list = orgId != null
                ? costCenterService.findActiveByOrg(orgId)
                : costCenterService.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getActive())).toList();
        return list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             c.getId());
            m.put("code",           c.getCostCenterCode());
            m.put("name",           c.getCostCenterName());
            m.put("businessUnitId", c.getBusinessUnitId());
            return m;
        }).toList();
    }

    /**
     * Parent cost center candidates for the form dropdown
     * — returns all active CCs except the record being edited.
     * GET /cost-centers/parents/all?excludeId=0
     */
    @GetMapping("/parents/all")
    @ResponseBody
    public List<Map<String, Object>> parents(@RequestParam(defaultValue = "0") Long excludeId) {
        return costCenterService.findParentCandidates(excludeId).stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",   c.getId());
                    m.put("code", c.getCostCenterCode());
                    m.put("name", c.getCostCenterName());
                    return m;
                }).toList();
    }

    /**
     * CostCenterType enum values for the form select.
     * GET /cost-centers/types
     */
    @GetMapping("/types")
    @ResponseBody
    public List<String> types() {
        return Arrays.stream(CostCenter.CostCenterType.values()).map(Enum::name).toList();
    }
}
