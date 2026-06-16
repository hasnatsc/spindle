package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * OrganizationController  /organizations
 *
 * JS fn → endpoint:
 *   orgShow(id)   GET    /organizations/show/{id}
 *   orgEdit(id)   GET    /organizations/show/{id}
 *   orgToggle(id) POST   /organizations/toggle/{id}
 *   orgDelete(id) DELETE /organizations/delete/{id}
 *   (save)        POST   /organizations/save
 */
@Slf4j
@Controller
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository orgRepo;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // ── Index ─────────────────────────────────────────────────────────────────
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

        List<Organization> all = orgRepo.findAll();
        String q = search.trim().toLowerCase();
        List<Organization> filtered = q.isBlank() ? all : all.stream()
                .filter(o -> contains(o.getName(), q) || contains(o.getCode(), q)
                          || contains(o.getEmail(), q) || contains(o.getPhone(), q))
                .toList();

        long total = all.size(), filtCount = filtered.size();
        List<Organization> page = filtered.stream().skip(start).limit(length).toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;

        for (Organization o : page) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("sl",         sl++);
            r.put("code",       esc(o.getCode()));
            r.put("name",       esc(o.getName()));
            r.put("name_bn",    o.getNameBn() != null ? esc(o.getNameBn()) : "—");
            r.put("email",      o.getEmail() != null ? esc(o.getEmail()) : "—");
            r.put("phone",      o.getPhone() != null ? esc(o.getPhone()) : "—");
            r.put("country",    o.getCountry() != null ? esc(o.getCountry()) : "—");
            r.put("status",     o.isActive()
                    ? "<span class='badge bg-success'>Active</span>"
                    : "<span class='badge bg-danger'>Inactive</span>");
            r.put("created_at", o.getCreatedAt() != null ? o.getCreatedAt().format(DT) : "—");
            r.put("actions",    actions(o.getId(), o.isActive()));
            rows.add(r);
        }
        return DataTableResponse.of(draw, total, filtCount, rows);
    }

    // ── Show ──────────────────────────────────────────────────────────────────
    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Organization o = orgRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Organization #" + id + " not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", toMap(o)));
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Save ─────────────────────────────────────────────────────────────────
    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long id = longVal(body.get("id"));
            String code = str(body.get("code")).toUpperCase();
            String name = str(body.get("name"));
            if (code.isBlank() || name.isBlank())
                throw new IllegalArgumentException("Code and Name are required.");

            Organization o;
            if (id == null) {
                if (orgRepo.existsByCode(code))
                    throw new IllegalArgumentException("Code '" + code + "' already exists.");
                o = new Organization();
                o.setCreatedAt(LocalDateTime.now());
                o.setCreatedBy(actor());
            } else {
                o = orgRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
                if (!o.getCode().equals(code) && orgRepo.existsByCode(code))
                    throw new IllegalArgumentException("Code '" + code + "' already exists.");
            }
            o.setCode(code);
            o.setName(name);
            o.setNameBn(strN(body.get("nameBn")));
            o.setAbout(strN(body.get("about")));
            o.setAddress(strN(body.get("address")));
            o.setCity(strN(body.get("city")));
            o.setState(strN(body.get("state")));
            o.setCountry(strN(body.get("country")));
            o.setPostalCode(strN(body.get("postalCode")));
            o.setPhone(strN(body.get("phone")));
            o.setEmail(strN(body.get("email")));
            o.setWebsite(strN(body.get("website")));
            o.setTaxId(strN(body.get("taxId")));
            o.setVatNo(strN(body.get("vatNo")));
            o.setBinNo(strN(body.get("binNo")));
            o.setActive(boolVal(body.get("active"), true));
            o.setUpdatedAt(LocalDateTime.now());
            o.setUpdatedBy(actor());
            orgRepo.save(o);

            res.put("success", true);
            res.put("message", id == null ? "Organization created." : "Organization updated.");
        } catch (Exception e) {
            res.put("success", false); res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Toggle ────────────────────────────────────────────────────────────────
    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Organization o = orgRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Not found."));
            o.setActive(!o.isActive());
            o.setUpdatedAt(LocalDateTime.now()); o.setUpdatedBy(actor());
            orgRepo.save(o);
            res.put("success", true);
            res.put("message", "Organization " + (o.isActive() ? "activated." : "deactivated."));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Organization o = orgRepo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Not found."));
            orgRepo.delete(o);
            res.put("success", true); res.put("message", "Organization deleted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    // ── Dropdown (for BU / CC / WH pickers) ──────────────────────────────────
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        return orgRepo.findByIsActiveTrue().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId()); m.put("code", o.getCode()); m.put("name", o.getName());
            return m;
        }).toList();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(Organization o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId()); m.put("code", o.getCode()); m.put("name", o.getName());
        m.put("nameBn", o.getNameBn()); m.put("about", o.getAbout());
        m.put("address", o.getAddress()); m.put("city", o.getCity());
        m.put("state", o.getState()); m.put("country", o.getCountry());
        m.put("postalCode", o.getPostalCode()); m.put("phone", o.getPhone());
        m.put("email", o.getEmail()); m.put("website", o.getWebsite());
        m.put("taxId", o.getTaxId()); m.put("vatNo", o.getVatNo()); m.put("binNo", o.getBinNo());
        m.put("active", o.isActive());
        m.put("logoUrl", o.getLogoUrl());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
        m.put("updatedAt", o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : "");
        m.put("createdBy", o.getCreatedBy()); m.put("updatedBy", o.getUpdatedBy());
        return m;
    }

    private String actions(Long id, boolean active) {
        String ti = active ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        String tt = active ? "Deactivate" : "Activate";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",   "orgShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",   "orgEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='" + tt + "' onclick='orgToggle(" + id + ")'>"
             + "<i class='fa " + ti + "'></i></button>"
             + btn("danger",  "fa-trash",  "Delete", "orgDelete(" + id + ")")
             + "</div>";
    }

    private String btn(String c, String i, String t, String o) {
        return "<button class='btn btn-outline-" + c + "' title='" + t + "' onclick='" + o + "'>"
             + "<i class='fa " + i + "'></i></button>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private boolean contains(String s, String q) { return s != null && s.toLowerCase().contains(q); }
    private String str(Object o)  { return o == null ? "" : o.toString().trim(); }
    private String strN(Object o) { String s = str(o); return s.isBlank() ? null : s; }
    private Long   longVal(Object o) { return (o == null || str(o).isBlank()) ? null : Long.valueOf(str(o)); }
    private boolean boolVal(Object o, boolean def) {
        return o == null ? def : Boolean.parseBoolean(o.toString());
    }
    private String actor() {
        try { var a = SecurityContextHolder.getContext().getAuthentication();
              return a != null ? a.getName() : "system"; } catch (Exception e) { return "system"; }
    }
}
