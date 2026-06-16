package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.security.dto.RoleDTO;
import com.asg.spindleserp.security.repository.PermissionRepository;
import com.asg.spindleserp.security.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RoleController  /roles
 *
 * JS fn → endpoint:
 *   roleShow(id)   GET  /roles/show/{id}
 *   roleEdit(id)   GET  /roles/show/{id}
 *   roleToggle(id) POST /roles/toggle/{id}
 *   roleDelete(id) DELETE /roles/delete/{id}
 *   (save)         POST /roles/save
 */
@Slf4j
@Controller
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService        roleService;
    private final PermissionRepository permissionRepository;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "roles");
        return "security/roles-index";
    }

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {
        return roleService.datatableList(draw, start, length, search);
    }

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            RoleDTO dto = roleService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Role #" + id + " not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", dto));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody RoleDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                roleService.update(dto.getId(), dto);
                res.put("message", "Role updated successfully.");
            } else {
                roleService.create(dto);
                res.put("message", "Role created successfully.");
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
            RoleDTO dto = roleService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "Role " + (dto.isActive() ? "activated" : "deactivated") + ".");
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
            roleService.delete(id);
            res.put("success", true);
            res.put("message", "Role deleted successfully.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    /** Reference: grouped permissions for the form checkboxes */
    @GetMapping("/permissions/all")
    @ResponseBody
    public Map<String, List<Map<String, Object>>> allPermissions() {
        return permissionRepository.findAllActive().stream()
                .collect(Collectors.groupingBy(
                        p -> p.getModule() != null ? p.getModule() : "OTHER",
                        LinkedHashMap::new,
                        Collectors.mapping(p -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("id",          p.getId());
                            m.put("name",        p.getName());
                            m.put("description", p.getDescription() != null ? p.getDescription() : "");
                            m.put("category",    p.getCategory() != null ? p.getCategory() : "");
                            return m;
                        }, Collectors.toList())
                ));
    }
}
