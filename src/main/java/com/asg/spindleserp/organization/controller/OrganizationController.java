package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.dto.OrganizationDTO;
import com.asg.spindleserp.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OrganizationController  /organizations
 *
 * JS fn → endpoint mapping (must match org-index.html):
 *   orgShow(id)   GET    /organizations/show/{id}
 *   orgEdit(id)   GET    /organizations/show/{id}   (same; form toggles mode)
 *   orgToggle(id) POST   /organizations/toggle/{id}
 *   orgDelete(id) DELETE /organizations/delete/{id}
 *   (save)        POST   /organizations/save
 *
 * Response envelope: { success, message, obj: { defaultData: {...} } }
 * — mirrors BusinessUnitController exactly.
 */
@Slf4j
@Controller
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    // ── Page ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "organizations");
        return "organizations/org-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return organizationService.datatableList(draw, start, length, search);
    }

    // ── Show / pre-fill ───────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            OrganizationDTO dto = organizationService.findById(id);
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
    public Map<String, Object> save(@RequestBody @Valid OrganizationDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                organizationService.update(dto.getId(), dto);
                res.put("message", "Organization updated successfully.");
            } else {
                organizationService.create(dto);
                res.put("message", "Organization created successfully.");
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
            OrganizationDTO dto = organizationService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Organization " + (Boolean.TRUE.equals(dto.getActive()) ? "activated" : "deactivated") + " successfully.");
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
            organizationService.delete(id);
            res.put("success", true);
            res.put("message", "Organization deleted successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Reference data (dropdown pickers) ────────────────────────────────────

    /**
     * Active organizations for BU / Dept / CC pickers.
     * GET /organizations/active
     */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        return organizationService.findActive().stream()
                .map(dto -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id",   dto.getId());
                    m.put("code", dto.getCode());
                    m.put("name", dto.getName());
                    return m;
                })
                .toList();
    }
}
