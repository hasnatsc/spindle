package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.organization.entity.BusinessUnit;
import com.asg.spindleserp.organization.entity.CostCenter;
import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.entity.Warehouse;
import com.asg.spindleserp.organization.repository.BusinessUnitRepository;
import com.asg.spindleserp.organization.repository.CostCenterRepository;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.organization.repository.WarehouseRepository;
import com.asg.spindleserp.security.dto.UserDTO;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.RoleRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import com.asg.spindleserp.security.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UserController — full CRUD for system users.
 *
 * URL prefix: /users
 *
 * ── Page endpoints ──────────────────────────────────────────────────────────
 *   GET  /users                    → users-index.html
 *
 * ── DataTable ───────────────────────────────────────────────────────────────
 *   GET  /users/list               → DataTableResponse JSON
 *
 * ── CRUD ────────────────────────────────────────────────────────────────────
 *   GET  /users/show/{id}          → UserDTO JSON  (view + edit pre-fill)
 *   POST /users/save               → create or update
 *   POST /users/toggle/{id}        → toggle active status
 *   POST /users/change-password/{id}
 *   DELETE /users/delete/{id}      → soft delete
 *
 * ── Reference data (form selects) ───────────────────────────────────────────
 *   GET  /users/roles/all
 *   GET  /users/organizations/all
 *   GET  /users/business-units/all
 *   GET  /users/cost-centers/all
 *   GET  /users/warehouses/all
 *   GET  /users/dashboards/all
 *
 * ── JS function → endpoint mapping (must match users-index.html) ─────────────
 *   userShow(id)   → GET    /users/show/{id}
 *   userEdit(id)   → GET    /users/show/{id}   (same endpoint; form toggles mode)
 *   userToggle(id) → POST   /users/toggle/{id}
 *   userPwd(id)    → opens passwordModal (no fetch — index JS only)
 *   userDelete(id) → DELETE /users/delete/{id}
 */
@Slf4j
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService            userService;
    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final OrganizationRepository orgRepository;
    private final BusinessUnitRepository buRepository;
    private final CostCenterRepository   ccRepository;
    private final WarehouseRepository    whRepository;

    // ── Index page ────────────────────────────────────────────────────────────

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "users");
        return "security/users-index";
    }

    // ── DataTable ─────────────────────────────────────────────────────────────

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
            res.put("message", "User " + (dto.isEnabled() ? "enabled" : "disabled") + " successfully.");
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

    // ─────────────────────────────────────────────────────────────────────────
    // REFERENCE DATA  (form select lists)
    // All return List<Map<String,Object>> consumed by users-form.html JS
    // ─────────────────────────────────────────────────────────────────────────

    /** GET /users/roles/all */
    @GetMapping("/roles/all")
    @ResponseBody
    public List<Map<String, Object>> allRoles() {
        return roleRepository.findAllActiveWithPermissions().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          r.getId());
            m.put("name",        r.getName().replace("ROLE_", ""));
            m.put("fullName",    r.getName());
            m.put("description", r.getDescription() != null ? r.getDescription() : "");
            return m;
        }).collect(Collectors.toList());
    }

    /** GET /users/organizations/all */
    @GetMapping("/organizations/all")
    @ResponseBody
    public List<Map<String, Object>> allOrganizations() {
        return orgRepository.findByIsActiveTrue().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   o.getId());
            m.put("code", o.getCode());
            m.put("name", o.getName());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * GET /users/business-units/all
     * Returns BUs across all active organizations.
     * In a multi-org setup the form can chain-load on org selection;
     * for simplicity we return all here and the form filters client-side.
     */
    @GetMapping("/business-units/all")
    @ResponseBody
    public List<Map<String, Object>> allBusinessUnits(
            @AuthenticationPrincipal UserDetails principal) {

        // Resolve current user's org to scope the dropdown
        Long orgId = resolveCurrentOrgId(principal);

        List<BusinessUnit> list = (orgId != null) ? buRepository.findByOrganizationIdAndIsActiveTrue(orgId) : buRepository.findAll().stream().filter(BusinessUnit::isActive).toList();

        return list.stream().map(bu -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   bu.getId());
            m.put("code", bu.getCode());
            m.put("name", bu.getName());
            m.put("organizationId", bu.getOrganization().getId());
            return m;
        }).collect(Collectors.toList());
    }

    /** GET /users/cost-centers/all */
    @GetMapping("/cost-centers/all")
    @ResponseBody
    public List<Map<String, Object>> allCostCenters(
            @AuthenticationPrincipal UserDetails principal) {

        Long orgId = resolveCurrentOrgId(principal);

        List<CostCenter> list = (orgId != null)
                ? ccRepository.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                : ccRepository.findAll().stream().filter(CostCenter::isActive).toList();

        return list.stream().map(cc -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   cc.getId());
            m.put("code", cc.getCostCenterCode());
            m.put("name", cc.getCostCenterName());
            return m;
        }).collect(Collectors.toList());
    }

    /** GET /users/warehouses/all */
    @GetMapping("/warehouses/all")
    @ResponseBody
    public List<Map<String, Object>> allWarehouses(
            @AuthenticationPrincipal UserDetails principal) {

        Long orgId = resolveCurrentOrgId(principal);

        List<Warehouse> list = (orgId != null)
                ? whRepository.findByBusinessUnitOrganizationIdAndIsActiveTrue(orgId)
                : whRepository.findAll().stream().filter(Warehouse::isActive).toList();

        return list.stream().map(wh -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   wh.getId());
            m.put("code", wh.getWarehouseCode());
            m.put("name", wh.getWarehouseName());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * GET /users/dashboards/all
     * Returns all dashboard enum values for the defaultDashboard select.
     */
    @GetMapping("/dashboards/all")
    @ResponseBody
    public List<Map<String, Object>> allDashboards() {
        return Arrays.stream(User.DefaultDashboard.values()).map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name",        d.name());
            m.put("displayName", toLabel(d));
            return m;
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Serializes UserDTO → JSON map consumed by show/edit JS handlers.
     * Keys must exactly match field names referenced in users-index.html.
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
        // Access scopes
        m.put("organizationIds",       dto.getOrganizationIds());
        m.put("businessUnitIds",       dto.getBusinessUnitIds());
        m.put("costCenterIds",         dto.getCostCenterIds());
        m.put("warehouseIds",          dto.getWarehouseIds());
        m.put("organizationNames",     dto.getOrganizationNames());
        m.put("businessUnitNames",     dto.getBusinessUnitNames());
        m.put("costCenterNames",       dto.getCostCenterNames());
        m.put("warehouseNames",        dto.getWarehouseNames());
        // Audit
        m.put("createdAt",   dto.getCreatedAt()   != null ? dto.getCreatedAt().toString()   : "");
        m.put("updatedAt",   dto.getUpdatedAt()   != null ? dto.getUpdatedAt().toString()   : "");
        m.put("lastLoginAt", dto.getLastLoginAt() != null ? dto.getLastLoginAt().toString() : "Never");
        return m;
    }

    /**
     * Resolve the logged-in user's organization ID.
     * Used to scope reference-data dropdowns without a separate context cookie.
     */
    private Long resolveCurrentOrgId(UserDetails principal) {
        if (principal == null) return null;
        try {
            return userRepository
                    .findByUsernameAndDeletedFalse(principal.getUsername())
                    .map(u -> u.getOrganization().getId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String toLabel(User.DefaultDashboard d) {
        return switch (d) {
            case DEFAULT    -> "Default";
            case ACCOUNTS   -> "Accounts";
            case INVENTORY  -> "Inventory";
            case PRODUCTION -> "Production";
            case SALES      -> "Sales";
            case PURCHASE   -> "Purchase";
            case HRM        -> "HRM";
            case COMMERCIAL -> "Commercial";
            case HR         -> "HR";
            case FINANCE    -> "Finance";
        };
    }
}
