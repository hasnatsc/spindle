package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.security.entity.Permission;
import com.asg.spindleserp.security.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PermissionController  /permissions
 *
 * Permissions are rarely created via UI (mostly seeded via SQL) but the
 * management screen lets admins create custom API-level permissions.
 *
 * JS fn → endpoint:
 *   permShow(id)   GET    /permissions/show/{id}
 *   permEdit(id)   GET    /permissions/show/{id}
 *   permToggle(id) POST   /permissions/toggle/{id}
 *   permDelete(id) DELETE /permissions/delete/{id}
 *   (save)         POST   /permissions/save
 */
@Slf4j
@Controller
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "permissions");
        return "security/permissions-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {

        String q = search.trim().toLowerCase();
        List<Permission> all = permissionRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(p -> p.getModule() != null ? p.getModule() : ""))
                .toList();

        List<Permission> filtered = q.isBlank() ? all : all.stream()
                .filter(p -> p.getName().toLowerCase().contains(q)
                          || (p.getModule() != null && p.getModule().toLowerCase().contains(q))
                          || (p.getCategory() != null && p.getCategory().toLowerCase().contains(q)))
                .toList();

        long total     = all.size();
        long filtCount = filtered.size();
        List<Permission> page = filtered.stream().skip(start).limit(length).toList();

        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;
        for (Permission p : page) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sl",          sl++);
            row.put("name",        "<code class='text-success'>" + esc(p.getName()) + "</code>");
            row.put("module",      p.getModule() != null ? p.getModule() : "—");
            row.put("category",    p.getCategory() != null ? p.getCategory() : "—");
            row.put("url_pattern", p.getUrlPattern() != null ? "<code>" + esc(p.getUrlPattern()) + "</code>" : "—");
            row.put("http_method", buildMethodBadge(p.getHttpMethod()));
            row.put("description", p.getDescription() != null ? p.getDescription() : "—");
            row.put("status",      p.isActive()
                    ? "<span class='badge bg-success'>Active</span>"
                    : "<span class='badge bg-danger'>Inactive</span>");
            row.put("created_at",  p.getCreatedAt() != null ? p.getCreatedAt().format(DT) : "—");
            row.put("actions",     buildActions(p.getId(), p.isActive()));
            rows.add(row);
        }

        return DataTableResponse.of(draw, total, filtCount, rows);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Permission p = permissionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Permission #" + id + " not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", toMap(p)));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long id = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
            String name = String.valueOf(body.get("name")).trim();

            if (id == null) {
                if (permissionRepository.existsByName(name))
                    throw new IllegalArgumentException("Permission '" + name + "' already exists.");
                Permission p = Permission.builder()
                        .name(name)
                        .description((String) body.get("description"))
                        .urlPattern((String) body.get("urlPattern"))
                        .httpMethod((String) body.get("httpMethod"))
                        .module((String) body.get("module"))
                        .category((String) body.get("category"))
                        .active(body.get("active") == null || Boolean.parseBoolean(body.get("active").toString()))
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                        .build();
                permissionRepository.save(p);
                res.put("message", "Permission created successfully.");
            } else {
                Permission p = permissionRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Permission #" + id + " not found."));
                if (permissionRepository.existsByNameAndIdNot(name, id))
                    throw new IllegalArgumentException("Permission '" + name + "' already exists.");
                p.setName(name);
                p.setDescription((String) body.get("description"));
                p.setUrlPattern((String) body.get("urlPattern"));
                p.setHttpMethod((String) body.get("httpMethod"));
                p.setModule((String) body.get("module"));
                p.setCategory((String) body.get("category"));
                p.setActive(Boolean.parseBoolean(body.get("active").toString()));
                p.setUpdatedAt(LocalDateTime.now());
                permissionRepository.save(p);
                res.put("message", "Permission updated successfully.");
            }
            res.put("success", true);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Permission p = permissionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Permission #" + id + " not found."));
            if ("*".equals(p.getName()))
                throw new IllegalArgumentException("The wildcard permission cannot be toggled.");
            p.setActive(!p.isActive());
            p.setUpdatedAt(LocalDateTime.now());
            permissionRepository.save(p);
            res.put("success", true);
            res.put("message", "Permission " + (p.isActive() ? "activated" : "deactivated") + ".");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            Permission p = permissionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Permission #" + id + " not found."));
            if ("*".equals(p.getName()))
                throw new IllegalArgumentException("The wildcard permission cannot be deleted.");
            permissionRepository.delete(p);
            res.put("success", true);
            res.put("message", "Permission deleted.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    /** Modules enum values for the form dropdown */
    @GetMapping("/modules/all")
    @ResponseBody
    public List<String> allModules() {
        return Arrays.stream(Permission.Module.values())
                .map(Enum::name).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> toMap(Permission p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          p.getId());
        m.put("name",        p.getName());
        m.put("description", p.getDescription());
        m.put("urlPattern",  p.getUrlPattern());
        m.put("httpMethod",  p.getHttpMethod());
        m.put("module",      p.getModule());
        m.put("category",    p.getCategory());
        m.put("active",      p.isActive());
        m.put("createdAt",   p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
        m.put("updatedAt",   p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
        return m;
    }

    private String buildMethodBadge(String method) {
        if (method == null) return "<span class='badge bg-secondary'>ANY</span>";
        String color = switch (method) {
            case "GET"    -> "bg-info text-dark";
            case "POST"   -> "bg-success";
            case "PUT"    -> "bg-warning text-dark";
            case "DELETE" -> "bg-danger";
            default       -> "bg-secondary";
        };
        return "<span class='badge " + color + "'>" + method + "</span>";
    }

    private String buildActions(Long id, boolean active) {
        String toggleIcon  = active ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        String toggleTitle = active ? "Deactivate" : "Activate";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",   "permShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",   "permEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='" + toggleTitle
             +   "' onclick='permToggle(" + id + ")'><i class='fa " + toggleIcon + "'></i></button>"
             + btn("danger",  "fa-trash",  "Delete", "permDelete(" + id + ")")
             + "</div>";
    }

    private String btn(String c, String i, String t, String o) {
        return "<button class='btn btn-outline-"+c+"' title='"+t+"' onclick='"+o+"'>"
             + "<i class='fa "+i+"'></i></button>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
