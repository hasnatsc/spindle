package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.security.entity.AppMenu;
import com.asg.spindleserp.security.entity.Role;
import com.asg.spindleserp.security.entity.RoleMenuAccess;
import com.asg.spindleserp.security.repository.AppMenuRepository;
import com.asg.spindleserp.security.repository.RoleMenuAccessRepository;
import com.asg.spindleserp.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RoleMenuAccessController  /role-menus
 *
 * Single-page UI: pick a role → see all menus as a tree with
 * canView / canCreate / canEdit / canDelete checkboxes.
 * Save writes the entire role's access in one POST.
 *
 * Endpoints:
 *   GET  /role-menus                  → role-menus-index.html
 *   GET  /role-menus/roles/all        → List of roles for dropdown
 *   GET  /role-menus/matrix/{roleId}  → full menu tree + current flags
 *   POST /role-menus/save/{roleId}    → replace all access rows for role
 *   POST /role-menus/grant-all/{roleId}   → grant all menus, full CRUD
 *   POST /role-menus/revoke-all/{roleId}  → remove all access for role
 */
@Slf4j
@Controller
@RequestMapping("/role-menus")
@RequiredArgsConstructor
public class RoleMenuAccessController {

    private final RoleRepository           roleRepository;
    private final AppMenuRepository        menuRepository;
    private final RoleMenuAccessRepository accessRepository;

    // ── Page ─────────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "role-menus");
        return "security/role-menus-index";
    }

    // ── Roles list (for the role picker dropdown) ─────────────────────────────

    @GetMapping("/roles/all")
    @ResponseBody
    public List<Map<String, Object>> allRoles() {
        return roleRepository.findAll().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          r.getId());
            m.put("name",        r.getName());
            m.put("displayName", r.getName().replace("ROLE_", ""));
            m.put("active",      r.isActive());
            return m;
        }).sorted(Comparator.comparing(m -> m.get("name").toString()))
          .collect(Collectors.toList());
    }

    // ── Matrix: all menus with current access flags for a role ────────────────

    @GetMapping("/matrix/{roleId}")
    @ResponseBody
    public Map<String, Object> matrix(@PathVariable Long roleId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role #" + roleId + " not found."));

            // Current access map: menuId → RoleMenuAccess
            Map<Long, RoleMenuAccess> accessMap = accessRepository.findAllByRoleId(roleId).stream()
                    .collect(Collectors.toMap(rma -> rma.getMenu().getId(), rma -> rma));

            // All menus ordered
            List<AppMenu> allMenus = menuRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();

            // Build flat list with access flags
            List<Map<String, Object>> menuItems = allMenus.stream().map(m -> {
                RoleMenuAccess rma = accessMap.get(m.getId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",                 m.getId());
                item.put("menuCode",           m.getMenuCode());
                item.put("menuName",           m.getMenuName());
                item.put("menuType",           m.getMenuType());
                item.put("parentId",           m.getParentId());
                item.put("displayOrder",       m.getDisplayOrder());
                item.put("icon",               m.getIcon());
                item.put("menuUrl",            m.getMenuUrl());
                item.put("canView",            rma != null && rma.isCanView());
                item.put("canCreate",          rma != null && rma.isCanCreate());
                item.put("canEdit",            rma != null && rma.isCanEdit());
                item.put("canDelete",          rma != null && rma.isCanDelete());
                item.put("hasAccess",          rma != null);
                return item;
            }).collect(Collectors.toList());

            res.put("success",  true);
            res.put("roleId",   roleId);
            res.put("roleName", role.getName());
            res.put("menus",    menuItems);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Save: replace all access rows for the role ────────────────────────────

    @PostMapping("/save/{roleId}")
    @ResponseBody
    public Map<String, Object> save(
            @PathVariable Long roleId,
            @RequestBody List<Map<String, Object>> accessList) {

        Map<String, Object> res = new HashMap<>();
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role #" + roleId + " not found."));

            if ("ROLE_SUPER_ADMIN".equals(role.getName()))
                throw new IllegalArgumentException("ROLE_SUPER_ADMIN access cannot be managed via this screen.");

            // Delete all existing rows for this role
            accessRepository.deleteByRoleId(roleId);

            // Insert fresh rows
            LocalDateTime now = LocalDateTime.now();
            List<RoleMenuAccess> toSave = new ArrayList<>();

            for (Map<String, Object> entry : accessList) {
                Boolean canView = bool(entry.get("canView"));
                if (!canView) continue; // skip menus with no view access

                Long menuId = Long.valueOf(entry.get("menuId").toString());
                AppMenu menu = menuRepository.findById(menuId).orElse(null);
                if (menu == null || menu.isDeleted()) continue;

                toSave.add(RoleMenuAccess.builder()
                        .role(role)
                        .menu(menu)
                        .canView(true)
                        .canCreate(bool(entry.get("canCreate")))
                        .canEdit(bool(entry.get("canEdit")))
                        .canDelete(bool(entry.get("canDelete")))
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }

            accessRepository.saveAll(toSave);
            log.info("RoleMenuAccess saved: role={} entries={}", role.getName(), toSave.size());
            res.put("success", true);
            res.put("message", "Access saved — " + toSave.size() + " menu(s) assigned to " + role.getName() + ".");
        } catch (Exception e) {
            log.error("RoleMenuAccess save error", e);
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Grant all: give role access to every active menu with full CRUD ───────

    @PostMapping("/grant-all/{roleId}")
    @ResponseBody
    public Map<String, Object> grantAll(@PathVariable Long roleId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found."));
            if ("ROLE_SUPER_ADMIN".equals(role.getName()))
                throw new IllegalArgumentException("ROLE_SUPER_ADMIN is managed by the wildcard permission.");

            accessRepository.deleteByRoleId(roleId);
            List<AppMenu> menus = menuRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();
            LocalDateTime now = LocalDateTime.now();
            List<RoleMenuAccess> rows = menus.stream().map(m ->
                RoleMenuAccess.builder()
                    .role(role).menu(m)
                    .canView(true).canCreate(true).canEdit(true).canDelete(true)
                    .createdAt(now).updatedAt(now)
                    .build()
            ).collect(Collectors.toList());
            accessRepository.saveAll(rows);
            res.put("success", true);
            res.put("message", rows.size() + " menus granted to " + role.getName() + ".");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Revoke all ────────────────────────────────────────────────────────────

    @PostMapping("/revoke-all/{roleId}")
    @ResponseBody
    public Map<String, Object> revokeAll(@PathVariable Long roleId) {
        Map<String, Object> res = new HashMap<>();
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found."));
            if ("ROLE_SUPER_ADMIN".equals(role.getName()))
                throw new IllegalArgumentException("Cannot revoke ROLE_SUPER_ADMIN access.");
            accessRepository.deleteByRoleId(roleId);
            res.put("success", true);
            res.put("message", "All menu access revoked from " + role.getName() + ".");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private boolean bool(Object o) {
        return o != null && Boolean.parseBoolean(o.toString());
    }
}
