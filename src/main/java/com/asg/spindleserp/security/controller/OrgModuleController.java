package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.security.auth.DynamicAuthorizationManager;
import com.asg.spindleserp.security.dto.OrgModuleDTO;
import com.asg.spindleserp.security.entity.OrgModule;
import com.asg.spindleserp.security.service.OrgModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * OrgModuleController
 *
 * URL prefix: /security/org-modules
 *
 * All endpoints require ROLE_SUPER_ADMIN.
 * Org-admins never see these URLs — they are not in their permission set.
 *
 * ── Pages ─────────────────────────────────────────────────────────────
 *   GET  /security/org-modules                — overview grid (all orgs)
 *   GET  /security/org-modules/{orgId}        — module grid for one org
 *
 * ── AJAX endpoints ────────────────────────────────────────────────────
 *   GET  /security/org-modules/{orgId}/summary        — JSON module summary
 *   POST /security/org-modules/{orgId}/toggle         — toggle one module
 *   POST /security/org-modules/{orgId}/bulk           — save all modules
 *   GET  /security/org-modules/{orgId}/admins         — list org admins
 *   POST /security/org-modules/{orgId}/admins/grant   — grant org-admin
 *   POST /security/org-modules/{orgId}/admins/revoke  — revoke org-admin
 */
@Controller
@RequestMapping("/security/org-modules")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class OrgModuleController {

    private final OrgModuleService         orgModuleService;
    private final DynamicAuthorizationManager authManager;

    // ── Page: overview ─────────────────────────────────────────────────────────

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("pageTitle", "Organization Module Access");
        model.addAttribute("moduleNames", OrgModuleService.moduleDisplayNames());
        return "security/org-modules-index";
    }

    // ── Page: single org detail ────────────────────────────────────────────────

    @GetMapping("/{orgId}")
    public String orgDetail(@PathVariable Long orgId, Model model) {
        OrgModuleDTO.OrgModuleSummary summary = orgModuleService.getModuleSummary(orgId);
        model.addAttribute("pageTitle", "Module Access: " + summary.getOrganizationName());
        model.addAttribute("summary",   summary);
        model.addAttribute("allModules", OrgModule.ModuleKey.values());
        return "security/org-modules-detail";
    }

    // ── AJAX: overview counts ─────────────────────────────────────────────────

    @GetMapping("/summary")
    @ResponseBody
    public Map<String, Object> overviewSummary() {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            res.put("success", true);
            res.put("data",    orgModuleService.getAllOrgModuleCounts());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── AJAX: module summary for one org ──────────────────────────────────────

    @GetMapping("/{orgId}/summary")
    @ResponseBody
    public Map<String, Object> moduleSummary(@PathVariable Long orgId) {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            res.put("success", true);
            res.put("data",    orgModuleService.getModuleSummary(orgId));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── AJAX: toggle one module ───────────────────────────────────────────────

    @PostMapping("/{orgId}/toggle")
    @ResponseBody
    public Map<String, Object> toggleModule(
            @PathVariable Long orgId,
            @RequestBody  Map<String, Object> body) {

        Map<String, Object> res = new LinkedHashMap<>();
        try {
            String  moduleKey = String.valueOf(body.get("moduleKey"));
            boolean active    = Boolean.parseBoolean(String.valueOf(body.get("active")));
            String  notes     = body.containsKey("notes") ? String.valueOf(body.get("notes")) : null;

            OrgModuleDTO dto = orgModuleService.setModuleActive(orgId, moduleKey, active, notes);

            // Invalidate the authorization cache so the change takes effect immediately
            authManager.invalidateCache();

            res.put("success", true);
            res.put("message", "Module '" + dto.getModuleDisplayName() + "' "
                    + (active ? "enabled" : "disabled") + " for " + dto.getOrganizationName());
            res.put("data",    dto);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── AJAX: bulk save all modules ───────────────────────────────────────────

    @PostMapping("/{orgId}/bulk")
    @ResponseBody
    public Map<String, Object> bulkSave(
            @PathVariable Long orgId,
            @RequestBody  Map<String, Object> body) {

        Map<String, Object> res = new LinkedHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> moduleMap =
                    (Map<String, Boolean>) body.getOrDefault("modules", Map.of());
            String notes = body.containsKey("notes") ? String.valueOf(body.get("notes")) : null;

            OrgModuleDTO.OrgModuleSummary summary =
                    orgModuleService.bulkSetModules(orgId, moduleMap, notes);

            authManager.invalidateCache();

            res.put("success", true);
            res.put("message", "Module access saved for " + summary.getOrganizationName());
            res.put("data",    summary);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    // ── AJAX: org admins ──────────────────────────────────────────────────────

    @GetMapping("/{orgId}/admins")
    @ResponseBody
    public Map<String, Object> getAdmins(@PathVariable Long orgId) {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            res.put("success", true);
            res.put("data",    orgModuleService.getOrgAdmins(orgId));
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/{orgId}/admins/grant")
    @ResponseBody
    public Map<String, Object> grantAdmin(
            @PathVariable Long orgId,
            @RequestBody  Map<String, Object> body) {

        Map<String, Object> res = new LinkedHashMap<>();
        try {
            Long   userId = Long.parseLong(String.valueOf(body.get("userId")));
            String notes  = body.containsKey("notes") ? String.valueOf(body.get("notes")) : null;
            orgModuleService.grantOrgAdmin(userId, orgId, notes);
            res.put("success", true);
            res.put("message", "Org-admin access granted.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @PostMapping("/{orgId}/admins/revoke")
    @ResponseBody
    public Map<String, Object> revokeAdmin(
            @PathVariable Long orgId,
            @RequestBody  Map<String, Object> body) {

        Map<String, Object> res = new LinkedHashMap<>();
        try {
            Long userId = Long.parseLong(String.valueOf(body.get("userId")));
            orgModuleService.revokeOrgAdmin(userId, orgId);
            res.put("success", true);
            res.put("message", "Org-admin access revoked.");
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }
}
