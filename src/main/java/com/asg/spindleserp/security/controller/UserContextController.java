package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.organization.entity.*;
import com.asg.spindleserp.organization.repository.*;
import com.asg.spindleserp.security.dto.UserContextDTO;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.entity.UserContext;
import com.asg.spindleserp.security.repository.UserContextRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import com.asg.spindleserp.security.service.UserContextService;
import com.asg.spindleserp.security.session.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserContextController — /user-context
 *
 * Serves the top-menu context switcher.
 *
 * Endpoint consumed by topMenuHeader.html:
 *   GET  /user-context/allowed/context            — current state + all allowed dropdowns
 *   POST /user-context/switch/organization/{id}
 *   POST /user-context/switch/business-unit/{id}
 *   POST /user-context/switch/cost-center/{id}
 *   POST /user-context/switch/warehouse/{id}
 *
 * On every switch:
 *   1. Validates that the user is actually allowed to access the target
 *   2. Updates the user_context row in DB
 *   3. Calls userContextService.loadContext() to refresh the session holder
 *   4. Returns { success, message }
 */
@Controller
@RequestMapping("/user-context")
@RequiredArgsConstructor
public class UserContextController {

    private final UserContextService    userContextService;
    private final UserContextHolder     contextHolder;
    private final UserContextRepository contextRepository;
    private final UserRepository        userRepository;
    private final OrganizationRepository organizationRepository;
    private final BusinessUnitRepository businessUnitRepository;
    private final CostCenterRepository   costCenterRepository;
    private final WarehouseRepository    warehouseRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  ALLOWED CONTEXT — feeds the top-menu dropdowns
    //  All data comes from the session holder — zero DB hit
    // ═══════════════════════════════════════════════════════════════════════

    @GetMapping("/allowed/context")
    @ResponseBody
    public Map<String, Object> getAllowedContext() {
        UserContextDTO dto = contextHolder.get();
        Map<String, Object> res = new HashMap<>();

        // Current active selection
        Map<String, Object> current = new HashMap<>();
        current.put("organizationId",   dto.getOrganizationId());
        current.put("organizationName", dto.getOrganizationName());
        current.put("businessUnitId",   dto.getBusinessUnitId());
        current.put("businessUnitName", dto.getBusinessUnitName());
        current.put("costCenterId",     dto.getCostCenterId());
        current.put("costCenterName",   dto.getCostCenterName());
        current.put("warehouseId",      dto.getWarehouseId());
        current.put("warehouseName",    dto.getWarehouseName());
        res.put("current", current);

        // Allowed dropdown lists (from session — no DB)
        res.put("organizations", toMapList(dto.getAllowedOrganizations()));
        res.put("businessUnits", toMapList(dto.getAllowedBusinessUnits()));
        res.put("costCenters",   toMapList(dto.getAllowedCostCenters()));
        res.put("warehouses",    toMapList(dto.getAllowedWarehouses()));

        // Header info
        res.put("username",             dto.getUsername());
        res.put("fullName",             dto.getFullName());
        res.put("pendingCount",         dto.getPendingApprovalsCount());
        res.put("unreadNotifications",  dto.getUnreadNotificationsCount());
        res.put("showBadge",            dto.getShowApprovalBadge());

        return res;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SWITCH ORGANIZATION
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/switch/organization/{orgId}")
    @ResponseBody
    @Transactional
    public Map<String, Object> switchOrganization(@PathVariable Long orgId, Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = loadUser(auth.getName());
            Organization org = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new RuntimeException("Organization not found: " + orgId));

            // Permission check against the user's allowed set
            boolean allowed = user.getOrganizations().stream()
                    .anyMatch(o -> o.getId().equals(orgId));
            if (!allowed) throw new RuntimeException("You are not permitted to access this organization.");

            UserContext ctx = userContextService.getOrCreateContext(user);
            ctx.setOrganization(org);
            ctx.setBusinessUnit(null);   // cascade-clear downstream
            ctx.setCostCenter(null);
            ctx.setWarehouse(null);
            contextRepository.save(ctx);
            userContextService.loadContext(user);   // refresh session

            res.put("success", true);
            res.put("message", "Switched to: " + org.getName());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SWITCH BUSINESS UNIT
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/switch/business-unit/{buId}")
    @ResponseBody
    @Transactional
    public Map<String, Object> switchBusinessUnit(@PathVariable Long buId, Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = loadUser(auth.getName());
            BusinessUnit bu = businessUnitRepository.findById(buId)
                    .orElseThrow(() -> new RuntimeException("Business Unit not found: " + buId));

            boolean allowed = user.getAllowedBusinessUnits().stream()
                    .anyMatch(b -> b.getId().equals(buId));
            if (!allowed) throw new RuntimeException("You are not permitted to access this business unit.");

            UserContext ctx = userContextService.getOrCreateContext(user);
            // Org consistency check
            if (ctx.getOrganization() != null && bu.getOrganization() != null
                    && !bu.getOrganization().getId().equals(ctx.getOrganization().getId())) {
                throw new RuntimeException(
                        "This business unit does not belong to the selected organization.");
            }

            ctx.setBusinessUnit(bu);
            ctx.setCostCenter(null);
            ctx.setWarehouse(null);
            contextRepository.save(ctx);
            userContextService.loadContext(user);

            res.put("success", true);
            res.put("message", "Switched to: " + bu.getName());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SWITCH COST CENTER
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/switch/cost-center/{ccId}")
    @ResponseBody
    @Transactional
    public Map<String, Object> switchCostCenter(@PathVariable Long ccId, Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = loadUser(auth.getName());
            CostCenter cc = costCenterRepository.findById(ccId)
                    .orElseThrow(() -> new RuntimeException("Cost Center not found: " + ccId));

            boolean allowed = user.getAllowedCostCenters().stream()
                    .anyMatch(c -> c.getId().equals(ccId));
            if (!allowed) throw new RuntimeException("You are not permitted to access this cost center.");

            UserContext ctx = userContextService.getOrCreateContext(user);
            if (ctx.getBusinessUnit() != null && cc.getBusinessUnit() != null
                    && !cc.getBusinessUnit().getId().equals(ctx.getBusinessUnit().getId())) {
                throw new RuntimeException(
                        "This cost center does not belong to the selected business unit.");
            }

            ctx.setCostCenter(cc);
            contextRepository.save(ctx);
            userContextService.loadContext(user);

            res.put("success", true);
            res.put("message", "Switched to: " + cc.getCostCenterName());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SWITCH WAREHOUSE
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/switch/warehouse/{whId}")
    @ResponseBody
    @Transactional
    public Map<String, Object> switchWarehouse(@PathVariable Long whId, Authentication auth) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = loadUser(auth.getName());
            Warehouse wh = warehouseRepository.findById(whId)
                    .orElseThrow(() -> new RuntimeException("Warehouse not found: " + whId));

            boolean allowed = user.getAllowedWarehouses().stream()
                    .anyMatch(w -> w.getId().equals(whId));
            if (!allowed) throw new RuntimeException("You are not permitted to access this warehouse.");

            UserContext ctx = userContextService.getOrCreateContext(user);
            if (ctx.getBusinessUnit() != null && wh.getBusinessUnit() != null
                    && !wh.getBusinessUnit().getId().equals(ctx.getBusinessUnit().getId())) {
                throw new RuntimeException(
                        "This warehouse does not belong to the selected business unit.");
            }

            ctx.setWarehouse(wh);
            contextRepository.save(ctx);
            userContextService.loadContext(user);

            res.put("success", true);
            res.put("message", "Switched to: " + wh.getWarehouseName());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  APPROVAL PREFERENCES
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping("/approval/preferences/notifications")
    @ResponseBody
    public Map<String, Object> updateNotificationPreferences(
            @RequestParam String  frequency,
            @RequestParam Boolean emailEnabled,
            @RequestParam Boolean smsEnabled,
            @RequestParam Boolean pushEnabled,
            @RequestParam Boolean whatsappEnabled) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long uid = contextHolder.get().getUserId();
            userContextService.updateNotificationPreferences(
                    uid, frequency, emailEnabled, smsEnabled, pushEnabled, whatsappEnabled);
            res.put("success", true);
            res.put("message", "Notification preferences updated.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/approval/preferences/display")
    @ResponseBody
    public Map<String, Object> updateDisplayPreferences(
            @RequestParam String  defaultView,
            @RequestParam Integer refreshInterval,
            @RequestParam Boolean soundEnabled,
            @RequestParam Boolean desktopNotification,
            @RequestParam Boolean showBadge) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long uid = contextHolder.get().getUserId();
            userContextService.updateDisplayPreferences(
                    uid, defaultView, refreshInterval, soundEnabled, desktopNotification, showBadge);
            res.put("success", true);
            res.put("message", "Display preferences updated.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE
    // ═══════════════════════════════════════════════════════════════════════

    /** Load user WITH all allowed scope sets — one query. */
    private User loadUser(String username) {
        return userRepository.findByUsernameWithAllContext(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private List<Map<String, Object>> toMapList(List<UserContextDTO.ScopeItem> items) {
        if (items == null) return List.of();
        return items.stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",       i.getId());
            m.put("code",     i.getCode());
            m.put("name",     i.getName());
            m.put("parentId", i.getParentId());
            return m;
        }).toList();
    }
}
