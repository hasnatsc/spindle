package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.dto.DepartmentDTO;
import com.asg.spindleserp.organization.service.DepartmentService;
import com.asg.spindleserp.security.auth.SecurityHelper;
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
 * DepartmentController  /departments
 *
 * JS fn → endpoint mapping (must match dept-index.html):
 *   deptShow(id)   GET    /departments/show/{id}
 *   deptEdit(id)   GET    /departments/show/{id}   (same; form toggles mode)
 *   deptToggle(id) POST   /departments/toggle/{id}
 *   deptDelete(id) DELETE /departments/delete/{id}
 *   (save)         POST   /departments/save
 *
 * Response envelope: { success, message, obj: { defaultData: {...} } }
 * — mirrors BusinessUnitController exactly.
 */
@Slf4j
@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    // ── Page ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "departments");
        return "organizations/dept-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return departmentService.datatableList(draw, start, length, search);
    }

    // ── Show / pre-fill ───────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            DepartmentDTO dto = departmentService.findById(id);
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
    public Map<String, Object> save(@RequestBody @Valid DepartmentDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                departmentService.update(dto.getId(), dto);
                res.put("message", "Department updated successfully.");
            } else {
                departmentService.create(dto);
                res.put("message", "Department created successfully.");
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
            DepartmentDTO dto = departmentService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Department " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
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
            departmentService.delete(id);
            res.put("success", true);
            res.put("message", "Department deleted successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Reference data ────────────────────────────────────────────────────────

    /**
     * Active departments for the current org — for HRM / approval form dropdowns.
     * GET /departments/active
     */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<DepartmentDTO> list = orgId != null
                ? departmentService.findActiveByOrg(orgId)
                : departmentService.findAll().stream().filter(d -> Boolean.TRUE.equals(d.getActive())).toList();
        return list.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             d.getId());
            m.put("code",           d.getCode());
            m.put("name",           d.getName());
            m.put("organizationId", d.getOrganizationId());
            return m;
        }).toList();
    }

    /**
     * Parent department candidates — all active departments except the record being edited.
     * GET /departments/parents/all?excludeId=0
     */
    @GetMapping("/parents/all")
    @ResponseBody
    public List<Map<String, Object>> parents(@RequestParam(defaultValue = "0") Long excludeId) {
        return departmentService.findParentCandidates(excludeId).stream()
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",   d.getId());
                    m.put("code", d.getCode());
                    m.put("name", d.getName());
                    return m;
                }).toList();
    }
}
