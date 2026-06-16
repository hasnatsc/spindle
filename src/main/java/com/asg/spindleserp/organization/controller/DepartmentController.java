package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.entity.Department;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.DepartmentRepository;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * DepartmentController  /departments
 *
 * JS fn → endpoint:
 *   deptShow(id)   GET    /departments/show/{id}
 *   deptEdit(id)   GET    /departments/show/{id}
 *   deptToggle(id) POST   /departments/toggle/{id}
 *   deptDelete(id) DELETE /departments/delete/{id}
 *   (save)         POST   /departments/save
 */
@Slf4j
@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository deptRepo;
    private final OrganizationRepository orgRepo;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "departments");
        return "organizations/dept-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {

        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<Department> all = orgId != null
                ? deptRepo.findByOrganizationIdAndActiveTrue(orgId)
                : deptRepo.findAll();

        String q = search.trim().toLowerCase();
        List<Department> filtered = q.isBlank() ? all : all.stream()
                .filter(d -> contains(d.getName(), q) || contains(d.getCode(), q))
                .toList();

        long total = deptRepo.count(), filtCount = filtered.size();
        List<Department> page = filtered.stream().skip(start).limit(length).toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;

        for (Department d : page) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("sl",           sl++);
            r.put("code",         d.getCode() != null ? esc(d.getCode()) : "—");
            r.put("name",         esc(d.getName()));
            r.put("organization", esc(d.getOrganization().getName()));
            r.put("parent",       d.getParentDepartment() != null ? esc(d.getParentDepartment().getName()) : "—");
            r.put("description",  d.getDescription() != null ? esc(d.getDescription()) : "—");
            r.put("status",       d.isActive()
                    ? "<span class='badge bg-success'>Active</span>"
                    : "<span class='badge bg-danger'>Inactive</span>");
            r.put("created_at",   d.getCreatedAt() != null ? d.getCreatedAt().toString().substring(0, 16) : "—");
            r.put("actions",      actions(d.getId(), d.isActive()));
            rows.add(r);
        }
        return DataTableResponse.of(draw, total, filtCount, rows);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Department d = deptRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", toMap(d)));
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
            String name = str(body.get("name"));
            if (name.isBlank() || orgId == null)
                throw new IllegalArgumentException("Organization and Name are required.");

            Organization org = orgRepo.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found."));

            String code = strN(body.get("code"));

            Department d;
            if (id == null) {
                if (code != null && deptRepo.existsByCode(code))
                    throw new IllegalArgumentException("Code '" + code + "' already exists.");
                d = new Department();
                d.setOrganization(org);
            } else {
                d = deptRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
                if (code != null && !code.equals(d.getCode()) && deptRepo.existsByCode(code))
                    throw new IllegalArgumentException("Code '" + code + "' already exists.");
                d.setOrganization(org);
            }
            d.setCode(code);
            d.setName(name);
            d.setDescription(strN(body.get("description")));
            d.setActive(boolVal(body.get("active"), true));

            Long parentId = longVal(body.get("parentDepartmentId"));
            if (parentId != null && !parentId.equals(id)) {
                d.setParentDepartment(deptRepo.findById(parentId).orElse(null));
            } else {
                d.setParentDepartment(null);
            }
            deptRepo.save(d);
            res.put("success", true);
            res.put("message", id == null ? "Department created." : "Department updated.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Department d = deptRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            d.setActive(!d.isActive());
            deptRepo.save(d);
            res.put("success", true);
            res.put("message", "Department " + (d.isActive() ? "activated." : "deactivated."));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Department d = deptRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            if (deptRepo.findAll().stream().anyMatch(x -> d.equals(x.getParentDepartment())))
                throw new IllegalArgumentException("Cannot delete: child departments exist.");
            deptRepo.delete(d);
            res.put("success", true); res.put("message", "Department deleted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Dropdown for HRM forms */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<Department> list = orgId != null
                ? deptRepo.findByOrganizationIdAndActiveTrue(orgId)
                : deptRepo.findAll().stream().filter(Department::isActive).toList();
        return list.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId()); m.put("code", d.getCode()); m.put("name", d.getName());
            m.put("organizationId", d.getOrganization().getId());
            return m;
        }).toList();
    }

    @GetMapping("/parents/all")
    @ResponseBody
    public List<Map<String, Object>> parents(@RequestParam(defaultValue = "0") Long excludeId) {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<Department> list = orgId != null
                ? deptRepo.findByOrganizationIdAndActiveTrue(orgId)
                : deptRepo.findAll().stream().filter(Department::isActive).toList();
        return list.stream().filter(d -> !d.getId().equals(excludeId)).map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId()); m.put("code", d.getCode()); m.put("name", d.getName());
            return m;
        }).toList();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(Department d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId()); m.put("code", d.getCode()); m.put("name", d.getName());
        m.put("description", d.getDescription()); m.put("active", d.isActive());
        m.put("organizationId", d.getOrganization().getId());
        m.put("organizationName", d.getOrganization().getName());
        m.put("parentDepartmentId", d.getParentDepartment() != null ? d.getParentDepartment().getId() : null);
        m.put("parentDepartmentName", d.getParentDepartment() != null ? d.getParentDepartment().getName() : "");
        m.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : "");
        m.put("updatedAt", d.getUpdatedAt() != null ? d.getUpdatedAt().toString() : "");
        m.put("createdBy", d.getCreatedBy()); m.put("updatedBy", d.getUpdatedBy());
        return m;
    }

    private String actions(Long id, boolean active) {
        String ti = active ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",   "deptShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",   "deptEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='Toggle' onclick='deptToggle(" + id + ")'>"
             + "<i class='fa " + ti + "'></i></button>"
             + btn("danger",  "fa-trash",  "Delete", "deptDelete(" + id + ")")
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
