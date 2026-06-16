package com.asg.spindleserp.organization.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.common.enums.ItemType;
import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.Warehouse;
import com.asg.spindleserp.organization.repository.BusinessUnitRepository;
import com.asg.spindleserp.organization.repository.WarehouseRepository;
import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * WarehouseController  /warehouses
 *
 * JS fn → endpoint:
 *   whShow(id)   GET    /warehouses/show/{id}
 *   whEdit(id)   GET    /warehouses/show/{id}
 *   whToggle(id) POST   /warehouses/toggle/{id}
 *   whDelete(id) DELETE /warehouses/delete/{id}
 *   (save)       POST   /warehouses/save
 */
@Slf4j
@Controller
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseRepository whRepo;
    private final BusinessUnitRepository buRepo;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "warehouses");
        return "organizations/wh-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {

        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<Warehouse> all = orgId != null
                ? whRepo.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                : whRepo.findAll();

        String q = search.trim().toLowerCase();
        List<Warehouse> filtered = q.isBlank() ? all : all.stream()
                .filter(w -> contains(w.getWarehouseName(), q) || contains(w.getWarehouseCode(), q))
                .toList();

        long total = whRepo.count(), filtCount = filtered.size();
        List<Warehouse> page = filtered.stream().skip(start).limit(length).toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;

        for (Warehouse w : page) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("sl",            sl++);
            r.put("code",          esc(w.getWarehouseCode()));
            r.put("name",          esc(w.getWarehouseName()));
            r.put("item_type",     "<span class='badge bg-primary'>" + w.getItemType().name() + "</span>");
            r.put("business_unit", esc(w.getBusinessUnit().getName()));
            r.put("manager",       w.getManagerName() != null ? esc(w.getManagerName()) : "—");
            r.put("contact",       w.getContactNumber() != null ? esc(w.getContactNumber()) : "—");
            r.put("status",        w.isActive()
                    ? "<span class='badge bg-success'>Active</span>"
                    : "<span class='badge bg-danger'>Inactive</span>");
            r.put("created_at",    w.getCreatedAt() != null ? w.getCreatedAt().toString().substring(0, 16) : "—");
            r.put("actions",       actions(w.getId(), w.isActive()));
            rows.add(r);
        }
        return DataTableResponse.of(draw, total, filtCount, rows);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Warehouse w = whRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", toMap(w)));
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
            String code     = str(body.get("warehouseCode")).toUpperCase();
            String name     = str(body.get("warehouseName"));
            String typeStr  = str(body.get("itemType"));

            if (code.isBlank() || name.isBlank() || buId == null || typeStr.isBlank())
                throw new IllegalArgumentException("Business Unit, Code, Name and Item Type are required.");

            ItemType itemType = ItemType.valueOf(typeStr);
            BusinessUnit bu = buRepo.findById(buId)
                    .orElseThrow(() -> new IllegalArgumentException("Business Unit not found."));

            Warehouse w;
            if (id == null) {
                if (whRepo.existsByWarehouseCode(code))
                    throw new IllegalArgumentException("Code '" + code + "' already exists.");
                w = new Warehouse();
            } else {
                w = whRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
                if (!w.getWarehouseCode().equals(code) && whRepo.existsByWarehouseCode(code))
                    throw new IllegalArgumentException("Code '" + code + "' already exists.");
            }
            w.setBusinessUnit(bu);
            w.setWarehouseCode(code);
            w.setWarehouseName(name);
            w.setItemType(itemType);
            w.setAddress(strN(body.get("address")));
            w.setManagerName(strN(body.get("managerName")));
            w.setContactNumber(strN(body.get("contactNumber")));
            w.setActive(boolVal(body.get("active"), true));
            whRepo.save(w);
            res.put("success", true);
            res.put("message", id == null ? "Warehouse created." : "Warehouse updated.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Warehouse w = whRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            w.setActive(!w.isActive());
            whRepo.save(w);
            res.put("success", true);
            res.put("message", "Warehouse " + (w.isActive() ? "activated." : "deactivated."));
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Warehouse w = whRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found."));
            whRepo.delete(w);
            res.put("success", true); res.put("message", "Warehouse deleted.");
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    /** Dropdown for stock/transfer forms */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> active() {
        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        List<Warehouse> list = orgId != null
                ? whRepo.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                : whRepo.findAll().stream().filter(Warehouse::isActive).toList();
        return list.stream().map(w -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("code", w.getWarehouseCode());
            m.put("name", w.getWarehouseName());
            m.put("itemType", w.getItemType().name());
            m.put("businessUnitId", w.getBusinessUnit().getId());
            return m;
        }).collect(Collectors.toList());
    }

    /** ItemType enum values for the form select */
    @GetMapping("/item-types")
    @ResponseBody
    public List<String> itemTypes() {
        return Arrays.stream(ItemType.values()).map(Enum::name).toList();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(Warehouse w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("warehouseCode", w.getWarehouseCode());
        m.put("warehouseName", w.getWarehouseName());
        m.put("itemType", w.getItemType().name());
        m.put("address", w.getAddress());
        m.put("managerName", w.getManagerName());
        m.put("contactNumber", w.getContactNumber());
        m.put("active", w.isActive());
        m.put("businessUnitId", w.getBusinessUnit().getId());
        m.put("businessUnitName", w.getBusinessUnit().getName());
        m.put("createdAt", w.getCreatedAt() != null ? w.getCreatedAt().toString() : "");
        m.put("updatedAt", w.getUpdatedAt() != null ? w.getUpdatedAt().toString() : "");
        m.put("createdBy", w.getCreatedBy()); m.put("updatedBy", w.getUpdatedBy());
        return m;
    }

    private String actions(Long id, boolean active) {
        String ti = active ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",   "whShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",   "whEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='Toggle' onclick='whToggle(" + id + ")'>"
             + "<i class='fa " + ti + "'></i></button>"
             + btn("danger",  "fa-trash",  "Delete", "whDelete(" + id + ")")
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
