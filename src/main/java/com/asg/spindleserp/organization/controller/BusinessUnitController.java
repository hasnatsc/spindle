package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.BusinessUnitRepository;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * BusinessUnitController  /business-units
 *
 * JS fn → endpoint:
 *   buShow(id)   GET    /business-units/show/{id}
 *   buEdit(id)   GET    /business-units/show/{id}
 *   buToggle(id) POST   /business-units/toggle/{id}
 *   buDelete(id) DELETE /business-units/delete/{id}
 *   (save)       POST   /business-units/save
 */
@Slf4j
@Controller
@RequestMapping("/business-units")
@RequiredArgsConstructor
public class BusinessUnitController {

    private final BusinessUnitRepository buRepo;
    private final OrganizationRepository orgRepo;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "business-units");
        return "organizations/bu-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {

        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<BusinessUnit> all = orgId != null ? buRepo.findByOrganizationIdAndIsActiveTrue(orgId)   // scoped to current org
                : buRepo.findAll();

        String q = search.trim().toLowerCase();
        List<BusinessUnit> filtered = q.isBlank() ? all : all.stream()
                .filter(b -> contains(b.getName(), q) || contains(b.getCode(), q))
                .toList();

        long total = buRepo.count(), filtCount = filtered.size();
        List<BusinessUnit> page = filtered.stream().skip(start).limit(length).toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;

        for (BusinessUnit b : page) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("sl",           sl++);
            r.put("code",         esc(b.getCode()));
            r.put("name",         esc(b.getName()));
            r.put("organization", esc(b.getOrganization().getName()));
            r.put("description",  b.getDescription() != null ? esc(b.getDescription()) : "—");
            r.put("status",       b.isActive()
                    ? "<span class='badge bg-success'>Active</span>"
                    : "<span class='badge bg-danger'>Inactive</span>");
            r.put("created_at",   b.getCreatedAt() != null ? b.getCreatedAt().toString().substring(0, 16) : "—");
            r.put("actions",      actions(b.getId(), b.isActive()));
            rows.add(r);
        }
        return DataTableResponse.of(draw, total, filtCount, rows);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BusinessUnit b = buRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", toMap(b)));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long id    = longVal(body.get("id"));
            Long orgId = longVal(body.get("organizationId"));
            String code = str(body.get("code")).toUpperCase();
            String name = str(body.get("name"));
            if (code.isBlank() || name.isBlank() || orgId == null)
                throw new IllegalArgumentException("Organization, Code and Name are required.");

            Organization org = orgRepo.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found."));

            BusinessUnit b;
            if (id == null) {
                if (buRepo.existsByOrganizationIdAndCode(orgId, code))
                    throw new IllegalArgumentException("Code '" + code + "' already used in this organization.");
                b = new BusinessUnit();
                b.setOrganization(org);
            } else {
                b = buRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
                if (!b.getCode().equals(code) && buRepo.existsByOrganizationIdAndCode(orgId, code))
                    throw new IllegalArgumentException("Code '" + code + "' already used in this organization.");
                b.setOrganization(org);
            }
            b.setCode(code);
            b.setName(name);
            b.setDescription(strN(body.get("description")));
            b.setActive(boolVal(body.get("active"), true));
            buRepo.save(b);
            res.put("success", true);
            res.put("message", id == null ? "Business Unit created." : "Business Unit updated.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BusinessUnit b = buRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            b.setActive(!b.isActive());
            buRepo.save(b);
            res.put("success", true);
            res.put("message", "Business Unit " + (b.isActive() ? "activated." : "deactivated."));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            BusinessUnit b = buRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            buRepo.delete(b);
            res.put("success", true); res.put("message", "Business Unit deleted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Dropdown: business units for current org */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<BusinessUnit> list = orgId != null
                ? buRepo.findByOrganizationIdAndIsActiveTrue(orgId)
                : buRepo.findAll().stream().filter(BusinessUnit::isActive).toList();
        return list.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId()); m.put("code", b.getCode()); m.put("name", b.getName());
            m.put("organizationId", b.getOrganization().getId());
            return m;
        }).toList();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(BusinessUnit b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId()); m.put("code", b.getCode()); m.put("name", b.getName());
        m.put("description", b.getDescription()); m.put("active", b.isActive());
        m.put("organizationId", b.getOrganization().getId());
        m.put("organizationName", b.getOrganization().getName());
        m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : "");
        m.put("updatedAt", b.getUpdatedAt() != null ? b.getUpdatedAt().toString() : "");
        m.put("createdBy", b.getCreatedBy()); m.put("updatedBy", b.getUpdatedBy());
        return m;
    }

    private String actions(Long id, boolean active) {
        String ti = active ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",   "buShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",   "buEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='Toggle' onclick='buToggle(" + id + ")'>"
             + "<i class='fa " + ti + "'></i></button>"
             + btn("danger",  "fa-trash",  "Delete", "buDelete(" + id + ")")
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
