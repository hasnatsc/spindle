package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.repository.BusinessUnitRepository;
import com.asg.spindleserp.organization.repository.CostCenterRepository;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * CostCenterController  /cost-centers
 *
 * JS fn → endpoint:
 *   ccShow(id)   GET    /cost-centers/show/{id}
 *   ccEdit(id)   GET    /cost-centers/show/{id}
 *   ccToggle(id) POST   /cost-centers/toggle/{id}
 *   ccDelete(id) DELETE /cost-centers/delete/{id}
 *   (save)       POST   /cost-centers/save
 */
@Slf4j
@Controller
@RequestMapping("/cost-centers")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterRepository ccRepo;
    private final BusinessUnitRepository buRepo;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "cost-centers");
        return "organizations/cc-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {

        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<CostCenter> all = orgId != null
                ? ccRepo.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                : ccRepo.findAll();

        String q = search.trim().toLowerCase();
        List<CostCenter> filtered = q.isBlank() ? all : all.stream()
                .filter(c -> contains(c.getCostCenterName(), q) || contains(c.getCostCenterCode(), q))
                .toList();

        long total = ccRepo.count(), filtCount = filtered.size();
        List<CostCenter> page = filtered.stream().skip(start).limit(length).toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;

        for (CostCenter c : page) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("sl",           sl++);
            r.put("code",         esc(c.getCostCenterCode()));
            r.put("name",         esc(c.getCostCenterName()));
            r.put("type",         c.getCostCenterType() != null
                    ? "<span class='badge bg-info text-dark'>" + c.getCostCenterType().name() + "</span>" : "—");
            r.put("business_unit",c.getBusinessUnit().getName());
            r.put("parent",       c.getParentCostCenter() != null ? esc(c.getParentCostCenter().getCostCenterName()) : "—");
            r.put("manager",      c.getManagerName() != null ? esc(c.getManagerName()) : "—");
            r.put("status",       c.isActive()
                    ? "<span class='badge bg-success'>Active</span>"
                    : "<span class='badge bg-danger'>Inactive</span>");
            r.put("created_at",   c.getCreatedAt() != null ? c.getCreatedAt().toString().substring(0, 16) : "—");
            r.put("actions",      actions(c.getId(), c.isActive()));
            rows.add(r);
        }
        return DataTableResponse.of(draw, total, filtCount, rows);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            CostCenter c = ccRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", toMap(c)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long id   = longVal(body.get("id"));
            Long buId = longVal(body.get("businessUnitId"));
            String code = str(body.get("costCenterCode")).toUpperCase();
            String name = str(body.get("costCenterName"));
            if (code.isBlank() || name.isBlank() || buId == null)
                throw new IllegalArgumentException("Business Unit, Code and Name are required.");

            BusinessUnit bu = buRepo.findById(buId)
                    .orElseThrow(() -> new IllegalArgumentException("Business Unit not found."));

            CostCenter c;
            if (id == null) {
                if (ccRepo.existsByCostCenterCode(code))
                    throw new IllegalArgumentException("Code '" + code + "' already exists.");
                c = new CostCenter();
            } else {
                c = ccRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
                if (!c.getCostCenterCode().equals(code) && ccRepo.existsByCostCenterCode(code))
                    throw new IllegalArgumentException("Code '" + code + "' already exists.");
            }

            c.setBusinessUnit(bu);
            c.setCostCenterCode(code);
            c.setCostCenterName(name);
            c.setDescription(strN(body.get("description")));
            c.setManagerName(strN(body.get("managerName")));
            c.setManagerEmail(strN(body.get("managerEmail")));
            c.setActive(boolVal(body.get("active"), true));

            String typeStr = strN(body.get("costCenterType"));
            c.setCostCenterType(typeStr != null ? CostCenter.CostCenterType.valueOf(typeStr) : null);

            Long parentId = longVal(body.get("parentCostCenterId"));
            if (parentId != null && !parentId.equals(id)) {
                CostCenter parent = ccRepo.findById(parentId).orElse(null);
                c.setParentCostCenter(parent);
            } else {
                c.setParentCostCenter(null);
            }

            ccRepo.save(c);
            res.put("success", true);
            res.put("message", id == null ? "Cost Center created." : "Cost Center updated.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            CostCenter c = ccRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            c.setActive(!c.isActive());
            ccRepo.save(c);
            res.put("success", true);
            res.put("message", "Cost Center " + (c.isActive() ? "activated." : "deactivated."));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            CostCenter c = ccRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            if (ccRepo.findAll().stream().anyMatch(x -> c.equals(x.getParentCostCenter())))
                throw new IllegalArgumentException("Cannot delete: child cost centers exist.");
            ccRepo.delete(c);
            res.put("success", true); res.put("message", "Cost Center deleted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Dropdown — active cost centers for current org */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<CostCenter> list = orgId != null
                ? ccRepo.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                : ccRepo.findAll().stream().filter(CostCenter::isActive).toList();
        return list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("code", c.getCostCenterCode());
            m.put("name", c.getCostCenterName());
            m.put("businessUnitId", c.getBusinessUnit().getId());
            return m;
        }).toList();
    }

    /** Parent cost center options (all non-self active) */
    @GetMapping("/parents/all")
    @ResponseBody
    public List<Map<String, Object>> parents(@RequestParam(defaultValue = "0") Long excludeId) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<CostCenter> list = orgId != null
                ? ccRepo.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                : ccRepo.findAll().stream().filter(CostCenter::isActive).toList();
        return list.stream()
                .filter(c -> !c.getId().equals(excludeId))
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("code", c.getCostCenterCode());
                    m.put("name", c.getCostCenterName());
                    return m;
                }).toList();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(CostCenter c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("costCenterCode", c.getCostCenterCode());
        m.put("costCenterName", c.getCostCenterName());
        m.put("costCenterType", c.getCostCenterType() != null ? c.getCostCenterType().name() : "");
        m.put("description", c.getDescription());
        m.put("managerName", c.getManagerName());
        m.put("managerEmail", c.getManagerEmail());
        m.put("active", c.isActive());
        m.put("businessUnitId", c.getBusinessUnit().getId());
        m.put("businessUnitName", c.getBusinessUnit().getName());
        m.put("parentCostCenterId", c.getParentCostCenter() != null ? c.getParentCostCenter().getId() : null);
        m.put("parentCostCenterName", c.getParentCostCenter() != null ? c.getParentCostCenter().getCostCenterName() : "");
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
        m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
        m.put("createdBy", c.getCreatedBy()); m.put("updatedBy", c.getUpdatedBy());
        return m;
    }

    private String actions(Long id, boolean active) {
        String ti = active ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",   "ccShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",   "ccEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='Toggle' onclick='ccToggle(" + id + ")'>"
             + "<i class='fa " + ti + "'></i></button>"
             + btn("danger",  "fa-trash",  "Delete", "ccDelete(" + id + ")")
             + "</div>";
    }

    private String btn(String c, String i, String t, String o) {
        return "<button class='btn btn-outline-" + c + "' title='" + t + "' onclick='" + o + "'>"
             + "<i class='fa " + i + "'></i></button>";
    }

    private static String esc(String s) { return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    private boolean contains(String s, String q) { return s != null && s.toLowerCase().contains(q); }
    private String str(Object o)  { return o == null ? "" : o.toString().trim(); }
    private String strN(Object o) { String s = str(o); return s.isBlank() ? null : s; }
    private Long longVal(Object o) { return (o == null || str(o).isBlank()) ? null : Long.valueOf(str(o)); }
    private boolean boolVal(Object o, boolean def) { return o == null ? def : Boolean.parseBoolean(o.toString()); }
}
