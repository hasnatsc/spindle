package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.security.dto.UserDTO;
import com.asg.spindleserp.security.repository.RoleRepository;
import com.asg.spindleserp.security.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserController — full CRUD for system users.
 *
 * URL prefix : /users
 *
 * JS function → endpoint mapping (must match users-index.html):
 *   userShow(id)   → GET    /users/show/{id}
 *   userEdit(id)   → GET    /users/show/{id}      (same endpoint, form toggles mode)
 *   userToggle(id) → POST   /users/toggle/{id}
 *   userPwd(id)    → POST   /users/change-password/{id}
 *   userDelete(id) → DELETE /users/delete/{id}
 *   (create/edit)  → POST   /users/save
 */
@Slf4j
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService    userService;
    private final RoleRepository roleRepository;

    // ── Index page ────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "users");
        return "commercial/fabric-precosting-dashboard";
    }

    // ── DataTable list ────────────────────────────────────────────────────────

    @GetMapping("/list")
    @ResponseBody
    public DataTableResponse list(
            @RequestParam(defaultValue = "1")  int draw,
            @RequestParam(defaultValue = "0")  int start,
            @RequestParam(defaultValue = "10") int length,
            @RequestParam(value = "search[value]", defaultValue = "") String search) {

        return userService.datatableList(draw, start, length, search);
    }

    // ── Show / pre-fill ───────────────────────────────────────────────────────

    @GetMapping("/show/{id}")
    @ResponseBody
    public Map<String, Object> show(@PathVariable Long id) {
        Map<String, Object> res = new HashMap<>();
        try {
            UserDTO dto = userService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User #" + id + " not found."));
            res.put("success", true);
            res.put("obj", Map.of("defaultData", buildPayload(dto)));
        } catch (Exception e) {
            log.warn("show user #{}: {}", id, e.getMessage());
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Save (create + update) ────────────────────────────────────────────────

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody UserDTO dto) {
        Map<String, Object> res = new HashMap<>();
        try {
            if (dto.getId() != null) {
                userService.updateUser(dto.getId(), dto);
                res.put("message", "User updated successfully.");
            } else {
                userService.createUser(dto);
                res.put("message", "User created successfully.");
            }
            res.put("success", true);
        } catch (Exception e) {
            log.warn("save user: {}", e.getMessage());
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
            UserDTO dto = userService.toggleStatus(id);
            res.put("success", true);
            res.put("message", "User " + (dto.isEnabled() ? "enabled" : "disabled") + ".");
        } catch (Exception e) {
            log.warn("toggle user #{}: {}", id, e.getMessage());
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
            userService.deleteUser(id);
            res.put("success", true);
            res.put("message", "User deleted successfully.");
        } catch (Exception e) {
            log.warn("delete user #{}: {}", id, e.getMessage());
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Change password ───────────────────────────────────────────────────────

    @PostMapping("/change-password/{id}")
    @ResponseBody
    public Map<String, Object> changePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> res = new HashMap<>();
        try {
            String pw  = body.get("newPassword");
            String cpw = body.get("confirmPassword");

            if (pw == null || pw.isBlank())
                throw new IllegalArgumentException("New password is required.");
            if (!pw.equals(cpw))
                throw new IllegalArgumentException("Passwords do not match.");

            userService.changePassword(id, pw);
            res.put("success", true);
            res.put("message", "Password changed successfully.");
        } catch (Exception e) {
            log.warn("change-password user #{}: {}", id, e.getMessage());
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── Reference data (form select lists) ───────────────────────────────────

    @GetMapping("/roles/all")
    @ResponseBody
    public List<Map<String, Object>> allRoles() {
        return roleRepository.findAllActiveWithPermissions().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",          r.getId());
            m.put("name",        r.getName().replace("ROLE_", ""));
            m.put("fullName",    r.getName());
            m.put("description", r.getDescription());
            return m;
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Serializes a UserDTO into the JSON payload consumed by show/edit JS handlers.
     * Keys must exactly match the field names referenced in users-index.html.
     */
    private Map<String, Object> buildPayload(UserDTO dto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                    dto.getId());
        m.put("username",              dto.getUsername());
        m.put("email",                 dto.getEmail());
        m.put("fullName",              dto.getFullName());
        m.put("phone",                 dto.getPhone());
        m.put("defaultDashboard",      dto.getDefaultDashboard() != null
                                           ? dto.getDefaultDashboard().name() : "DEFAULT");
        m.put("defaultDashboardLabel", dto.getDefaultDashboardLabel());
        m.put("enabled",               dto.isEnabled());
        m.put("accountNonExpired",     dto.isAccountNonExpired());
        m.put("accountNonLocked",      dto.isAccountNonLocked());
        m.put("credentialsNonExpired", dto.isCredentialsNonExpired());
        m.put("roleIds",               dto.getRoleIds());
        m.put("roleNames",             dto.getRoleNames());
        m.put("roleCount",             dto.getRoleCount());
        m.put("createdAt",  dto.getCreatedAt()   != null ? dto.getCreatedAt().toString()   : "");
        m.put("updatedAt",  dto.getUpdatedAt()   != null ? dto.getUpdatedAt().toString()   : "");
        m.put("lastLoginAt",dto.getLastLoginAt() != null ? dto.getLastLoginAt().toString() : "Never");
        return m;
    }
}
