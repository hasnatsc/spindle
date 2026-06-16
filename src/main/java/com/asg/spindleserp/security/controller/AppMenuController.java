package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.security.entity.AppMenu;
import com.asg.spindleserp.security.repository.AppMenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AppMenuController  /menus
 *
 * JS fn → endpoint:
 *   menuShow(id)   GET    /menus/show/{id}
 *   menuEdit(id)   GET    /menus/show/{id}
 *   menuToggle(id) POST   /menus/toggle/{id}
 *   menuDelete(id) DELETE /menus/delete/{id}
 *   (save)         POST   /menus/save
 *   (reorder)      POST   /menus/reorder
 */
@Slf4j
@Controller
@RequestMapping("/menus")
@RequiredArgsConstructor
public class AppMenuController {

    private final AppMenuRepository menuRepository;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "menus");
        return "security/menus-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {

        List<AppMenu> all = menuRepository.findAllByOrderByDisplayOrderAsc();
        String q = search.trim().toLowerCase();

        List<AppMenu> filtered = q.isBlank() ? all : all.stream()
                .filter(m -> m.getMenuName().toLowerCase().contains(q)
                          || m.getMenuCode().toLowerCase().contains(q)
                          || (m.getMenuUrl() != null && m.getMenuUrl().toLowerCase().contains(q)))
                .toList();

        long total     = all.size();
        long filtCount = filtered.size();
        List<AppMenu> page = filtered.stream().skip(start).limit(length).toList();

        // Build parentCode lookup
        Map<Long, String> parentNames = all.stream()
                .collect(Collectors.toMap(AppMenu::getId, AppMenu::getMenuName));

        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;
        for (AppMenu m : page) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sl",          sl++);
            row.put("menu_code",   "<code>" + esc(m.getMenuCode()) + "</code>");
            row.put("menu_name",   esc(m.getMenuName()));
            row.put("menu_type",   typeBadge(m.getMenuType()));
            row.put("parent",      m.getParentId() != null
                                   ? parentNames.getOrDefault(m.getParentId(), "#" + m.getParentId())
                                   : "<span class='text-muted'>—</span>");
            row.put("menu_url",    m.getMenuUrl() != null
                                   ? "<code class='text-info'>" + esc(m.getMenuUrl()) + "</code>"
                                   : "<span class='text-muted'>—</span>");
            row.put("icon",        m.getIcon() != null
                                   ? "<i class='" + esc(m.getIcon()) + "'></i> <code class='small'>" + esc(m.getIcon()) + "</code>"
                                   : "—");
            row.put("order",       m.getDisplayOrder());
            row.put("status",      m.isActive() && !m.isDeleted()
                                   ? "<span class='badge bg-success'>Active</span>"
                                   : "<span class='badge bg-danger'>Inactive</span>");
            row.put("actions",     buildActions(m.getId(), m.isActive()));
            rows.add(row);
        }

        return DataTableResponse.of(draw, total, filtCount, rows);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            AppMenu m = menuRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Menu #" + id + " not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", toMap(m)));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
            String menuCode = String.valueOf(body.get("menuCode")).trim().toUpperCase();
            String menuName = String.valueOf(body.get("menuName")).trim();

            if (menuCode.isBlank() || menuName.isBlank())
                throw new IllegalArgumentException("Menu code and name are required.");

            String actor = actor();

            AppMenu menu;
            if (id == null) {
                if (menuRepository.existsByMenuCode(menuCode))
                    throw new IllegalArgumentException("Menu code '" + menuCode + "' already exists.");
                menu = new AppMenu();
                menu.setCreatedBy(actor);
                menu.setCreatedAt(LocalDateTime.now());
            } else {
                menu = menuRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Menu #" + id + " not found."));
                if (menuRepository.existsByMenuCodeAndIdNot(menuCode, id))
                    throw new IllegalArgumentException("Menu code '" + menuCode + "' already exists.");
            }

            menu.setMenuCode(menuCode);
            menu.setMenuName(menuName);
            menu.setMenuUrl(strOrNull(body.get("menuUrl")));
            menu.setIcon(strOrNull(body.get("icon")));
            menu.setParentId(body.get("parentId") != null && !body.get("parentId").toString().isBlank()
                             ? Long.valueOf(body.get("parentId").toString()) : null);
            menu.setDisplayOrder(body.get("displayOrder") != null
                                 ? Integer.parseInt(body.get("displayOrder").toString()) : 0);
            menu.setMenuType(body.get("menuType") != null ? body.get("menuType").toString() : "LEAF");
            menu.setModuleName(strOrNull(body.get("moduleName")));
            menu.setRequiredPermission(strOrNull(body.get("requiredPermission")));
            menu.setDescription(strOrNull(body.get("description")));
            menu.setTarget(body.get("target") != null ? body.get("target").toString() : "_self");
            menu.setActive(body.get("active") == null || Boolean.parseBoolean(body.get("active").toString()));
            menu.setVisible(body.get("visible") == null || Boolean.parseBoolean(body.get("visible").toString()));
            menu.setUpdatedBy(actor);
            menu.setUpdatedAt(LocalDateTime.now());

            menuRepository.save(menu);
            res.put("success", true);
            res.put("message", id == null ? "Menu created successfully." : "Menu updated successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            AppMenu m = menuRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Menu #" + id + " not found."));
            m.setActive(!m.isActive());
            m.setUpdatedBy(actor());
            m.setUpdatedAt(LocalDateTime.now());
            menuRepository.save(m);
            res.put("success", true);
            res.put("message", "Menu " + (m.isActive() ? "activated" : "deactivated") + ".");
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
            AppMenu m = menuRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Menu #" + id + " not found."));

            // Guard: check if any children exist
            long childCount = menuRepository.findAllByOrderByDisplayOrderAsc().stream()
                    .filter(child -> id.equals(child.getParentId())).count();
            if (childCount > 0)
                throw new IllegalArgumentException("Cannot delete: " + childCount + " child menu(s) exist.");

            m.setDeleted(true);
            m.setActive(false);
            m.setUpdatedBy(actor());
            m.setUpdatedAt(LocalDateTime.now());
            menuRepository.save(m);
            res.put("success", true);
            res.put("message", "Menu deleted successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Parent options ────────────────────────────────────────────────────────

    @GetMapping("/parents/all")
    @ResponseBody
    public List<Map<String, Object>> parentOptions() {
        return menuRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc()
                .stream()
                .filter(m -> !"LEAF".equals(m.getMenuType()))
                .map(m -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id",       m.getId());
                    r.put("menuCode", m.getMenuCode());
                    r.put("menuName", m.getMenuName());
                    r.put("menuType", m.getMenuType());
                    return r;
                }).collect(Collectors.toList());
    }

    // ── Permissions options (for requiredPermission dropdown) ─────────────────

    @GetMapping("/permissions/all")
    @ResponseBody
    public List<Map<String, Object>> permissionOptions(
            @org.springframework.beans.factory.annotation.Autowired
            com.asg.spindleserp.security.repository.PermissionRepository permRepo) {
        return permRepo.findAllActive().stream().map(p -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("name", p.getName());
            r.put("description", p.getDescription() != null ? p.getDescription() : "");
            return r;
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> toMap(AppMenu m) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id",                 m.getId());
        r.put("menuCode",           m.getMenuCode());
        r.put("menuName",           m.getMenuName());
        r.put("menuUrl",            m.getMenuUrl());
        r.put("icon",               m.getIcon());
        r.put("parentId",           m.getParentId());
        r.put("displayOrder",       m.getDisplayOrder());
        r.put("menuType",           m.getMenuType());
        r.put("moduleName",         m.getModuleName());
        r.put("requiredPermission", m.getRequiredPermission());
        r.put("description",        m.getDescription());
        r.put("target",             m.getTarget());
        r.put("active",             m.isActive());
        r.put("visible",            m.isVisible());
        r.put("createdAt",          m.getCreatedAt() != null ? m.getCreatedAt().toString() : "");
        r.put("updatedAt",          m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : "");
        return r;
    }

    private String typeBadge(String type) {
        return switch (type != null ? type : "LEAF") {
            case "MODULE" -> "<span class='badge bg-primary'>MODULE</span>";
            case "GROUP"  -> "<span class='badge bg-info text-dark'>GROUP</span>";
            default       -> "<span class='badge bg-secondary'>LEAF</span>";
        };
    }

    private String buildActions(Long id, boolean active) {
        String toggleIcon  = active ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        String toggleTitle = active ? "Deactivate" : "Activate";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",   "menuShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",   "menuEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='" + toggleTitle
             +   "' onclick='menuToggle(" + id + ")'><i class='fa " + toggleIcon + "'></i></button>"
             + btn("danger",  "fa-trash",  "Delete", "menuDelete(" + id + ")")
             + "</div>";
    }

    private String btn(String c, String i, String t, String o) {
        return "<button class='btn btn-outline-"+c+"' title='"+t+"' onclick='"+o+"'>"
             + "<i class='fa "+i+"'></i></button>";
    }

    private String actor() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        } catch (Exception e) { return "system"; }
    }

    private String strOrNull(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isBlank() ? null : s;
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
