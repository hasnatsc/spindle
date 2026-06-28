package com.asg.spindleserp.security.service;

import com.asg.spindleserp.organization.entity.Organization;
import com.asg.spindleserp.organization.repository.OrganizationRepository;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.security.dto.OrgModuleDTO;
import com.asg.spindleserp.security.entity.OrgAdminScope;
import com.asg.spindleserp.security.entity.OrgModule;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.OrgAdminScopeRepository;
import com.asg.spindleserp.security.repository.OrgModuleRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OrgModuleService
 *
 * ══════════════════════════════════════════════════════════════════════
 * RESPONSIBILITY SPLIT
 * ══════════════════════════════════════════════════════════════════════
 *
 * SUPER ADMIN (ROLE_SUPER_ADMIN):
 *   • Enable / disable any module for any organization.
 *   • Grant / revoke org-admin status to any user for any org.
 *   • See all organizations and their full module grid.
 *
 * ORG ADMIN (has OrgAdminScope row for their org):
 *   • Can only manage users, roles, and permissions within their org.
 *   • Cannot enable modules — they can only USE the modules the super
 *     admin has already enabled for their org.
 *   • Cannot see other organizations.
 *
 * ══════════════════════════════════════════════════════════════════════
 * HOW AUTHORIZATION ENFORCEMENT WORKS
 * ══════════════════════════════════════════════════════════════════════
 *
 * DynamicAuthorizationManager (updated in this PR) now does:
 *   1. Gets user's orgId from CustomUserDetails.getOrganizationId().
 *   2. Looks up the org's active moduleKeys from the in-memory cache
 *      (5-minute TTL, same pattern as the permission cache).
 *   3. For each permission that matches the URI, checks that the
 *      permission's module is in the org's active modules set.
 *   4. If the module is disabled → DENY even if the user has the perm.
 *
 * This means super admin can switch off a whole module for an org
 * (e.g. disable COMMERCIAL) and all 40+ URLs under /commercial/**
 * are blocked automatically — no need to remove individual permissions.
 *
 * ══════════════════════════════════════════════════════════════════════
 * ORG ADMIN ROLE RESTRICTION
 * ══════════════════════════════════════════════════════════════════════
 *
 * RoleServiceImpl.create() and RoleServiceImpl.update() call
 * orgModuleService.assertRolePermissionsAllowedForOrg() to verify
 * that every permission the org-admin is trying to assign belongs
 * to a module that is active for their organization.
 *
 * Super admin bypasses this check entirely (checked via SecurityHelper).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrgModuleService {

    private final OrgModuleRepository    orgModuleRepository;
    private final OrgAdminScopeRepository orgAdminScopeRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository         userRepository;

    // ── Display names for the UI ──────────────────────────────────────────────
    private static final Map<String, String> MODULE_DISPLAY = new LinkedHashMap<>();
    static {
        MODULE_DISPLAY.put("CORE_SECURITY",               "Security & IAM");
        MODULE_DISPLAY.put("HRM",                         "Human Resources");
        MODULE_DISPLAY.put("SALES_CUSTOMER_OPERATIONS",   "Sales & CRM");
        MODULE_DISPLAY.put("PURCHASE_SUPPLIER",           "Purchase");
        MODULE_DISPLAY.put("INVENTORY_WAREHOUSE",         "Inventory");
        MODULE_DISPLAY.put("FINANCE_ACCOUNTS",            "Accounts / GL");
        MODULE_DISPLAY.put("PRODUCTION",                  "Production");
        MODULE_DISPLAY.put("PRODUCT_CATALOG_ECOMMERCE",   "Product Catalogue");
        MODULE_DISPLAY.put("POS",                         "Point of Sale");
        MODULE_DISPLAY.put("CRM",                         "CRM");
        MODULE_DISPLAY.put("COMMUNICATION_NOTIFICATION",  "Notifications");
        MODULE_DISPLAY.put("COMMERCIAL",                  "Commercial");
        MODULE_DISPLAY.put("REPORTS_ANALYTICS",           "Reports");
        MODULE_DISPLAY.put("BUDGET",                      "Budget");
        MODULE_DISPLAY.put("FIXED_ASSETS",                "Fixed Assets");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MODULE GRID — read
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returns the full module grid for one org — one row per ModuleKey,
     * with active=true/false. Rows are created on-demand if missing.
     * Called by the super-admin "Module Access" panel for each org.
     */
    @Transactional(readOnly = true)
    public OrgModuleDTO.OrgModuleSummary getModuleSummary(Long orgId) {
        Organization org = requireOrg(orgId);
        Map<String, OrgModule> existing = orgModuleRepository
                .findByOrganizationIdOrderByModuleKey(orgId)
                .stream()
                .collect(Collectors.toMap(OrgModule::getModuleKey, m -> m));

        List<OrgModuleDTO> list = MODULE_DISPLAY.keySet().stream()
                .map(key -> {
                    OrgModule m = existing.get(key);
                    return OrgModuleDTO.builder()
                            .id(m != null ? m.getId() : null)
                            .organizationId(orgId)
                            .organizationName(org.getName())
                            .moduleKey(key)
                            .moduleDisplayName(MODULE_DISPLAY.get(key))
                            .active(m != null && m.isActive())
                            .grantedBy(m != null ? m.getGrantedBy() : null)
                            .grantedAt(m != null ? m.getGrantedAt() : null)
                            .revokedBy(m != null ? m.getRevokedBy() : null)
                            .revokedAt(m != null ? m.getRevokedAt() : null)
                            .notes(m != null ? m.getNotes() : null)
                            .build();
                })
                .toList();

        return OrgModuleDTO.OrgModuleSummary.builder()
                .organizationId(orgId)
                .organizationName(org.getName())
                .modules(list)
                .build();
    }

    /**
     * All orgs with their active module counts — for the super-admin overview table.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllOrgModuleCounts() {
        // Pure JDBC would be faster here; keeping JPA for simplicity
        return organizationRepository.findAll().stream()
                .map(org -> {
                    long active = orgModuleRepository
                            .findByOrganizationIdOrderByModuleKey(org.getId())
                            .stream().filter(OrgModule::isActive).count();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("orgId",        org.getId());
                    row.put("orgName",      org.getName());
                    row.put("activeModules", active);
                    row.put("totalModules",  MODULE_DISPLAY.size());
                    return row;
                })
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MODULE TOGGLE — super admin only
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Enables or disables a single module for an org.
     * SUPER_ADMIN only — controller enforces @PreAuthorize("hasRole('SUPER_ADMIN')").
     *
     * @return the updated OrgModuleDTO
     */
    public OrgModuleDTO setModuleActive(Long orgId, String moduleKey, boolean active, String notes) {
        requireSuperAdmin();
        String actor = SecurityHelper.currentUsername().orElse("system");
        Organization org = requireOrg(orgId);
        String key = moduleKey.trim().toUpperCase();

        if (!MODULE_DISPLAY.containsKey(key)) {
            throw new IllegalArgumentException("Unknown module key: '" + key + "'.");
        }

        OrgModule om = orgModuleRepository
                .findByOrganizationIdAndModuleKey(orgId, key)
                .orElseGet(() -> OrgModule.builder()
                        .organization(org)
                        .moduleKey(key)
                        .build());

        om.setActive(active);
        om.setNotes(notes);
        if (active) {
            om.setGrantedBy(actor);
            om.setGrantedAt(LocalDateTime.now());
            om.setRevokedBy(null);
            om.setRevokedAt(null);
        } else {
            om.setRevokedBy(actor);
            om.setRevokedAt(LocalDateTime.now());
        }

        OrgModule saved = orgModuleRepository.save(om);
        log.info("Module '{}' {} for org '{}' by {}", key,
                active ? "ENABLED" : "DISABLED", org.getName(), actor);

        return toDTO(saved, org);
    }

    /**
     * Bulk replace — sets all modules for an org in one call.
     * Payload: map of moduleKey → active(boolean).
     * Used by the "Save All" button on the module grid.
     */
    public OrgModuleDTO.OrgModuleSummary bulkSetModules(Long orgId,
                                                         Map<String, Boolean> moduleActiveMap,
                                                         String notes) {
        requireSuperAdmin();
        for (Map.Entry<String, Boolean> e : moduleActiveMap.entrySet()) {
            setModuleActive(orgId, e.getKey(), Boolean.TRUE.equals(e.getValue()), notes);
        }
        return getModuleSummary(orgId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ORG ADMIN GRANT / REVOKE — super admin only
    // ═══════════════════════════════════════════════════════════════════════

    /** Grant org-admin status to a user for a specific org. */
    public void grantOrgAdmin(Long userId, Long orgId, String notes) {
        requireSuperAdmin();
        String actor = SecurityHelper.currentUsername().orElse("system");
        User user    = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User #" + userId + " not found."));
        Organization org = requireOrg(orgId);

        OrgAdminScope scope = orgAdminScopeRepository
                .findByUserIdAndOrganizationId(userId, orgId)
                .orElseGet(() -> OrgAdminScope.builder()
                        .user(user)
                        .organization(org)
                        .build());

        scope.setActive(true);
        scope.setGrantedBy(actor);
        scope.setGrantedAt(LocalDateTime.now());
        scope.setNotes(notes);
        orgAdminScopeRepository.save(scope);

        log.info("Org-admin granted: user='{}' org='{}' by {}", user.getUsername(), org.getName(), actor);
    }

    /** Revoke org-admin status for a user/org pair. */
    public void revokeOrgAdmin(Long userId, Long orgId) {
        requireSuperAdmin();
        String actor = SecurityHelper.currentUsername().orElse("system");
        orgAdminScopeRepository.findByUserIdAndOrganizationId(userId, orgId)
                .ifPresent(scope -> {
                    scope.setActive(false);
                    orgAdminScopeRepository.save(scope);
                    log.info("Org-admin revoked: userId={} orgId={} by {}", userId, orgId, actor);
                });
    }

    /** All active org-admin grants for an org — used by the admin management page. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrgAdmins(Long orgId) {
        return orgAdminScopeRepository
                .findByOrganizationIdAndActiveTrue(orgId)
                .stream()
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("userId",       s.getUser().getId());
                    row.put("username",     s.getUser().getUsername());
                    row.put("fullName",     s.getUser().getFullName());
                    row.put("grantedBy",    s.getGrantedBy());
                    row.put("grantedAt",    s.getGrantedAt() != null ? s.getGrantedAt().toString() : "—");
                    row.put("notes",        s.getNotes() != null ? s.getNotes() : "—");
                    return row;
                })
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENFORCEMENT — called by RoleServiceImpl
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called by RoleServiceImpl.create() / update() when the actor is an org-admin.
     * Verifies every permission being assigned belongs to a module that is
     * active for the org-admin's organization.
     *
     * Super-admin callers skip this check (SecurityHelper.isSuperAdmin() gate in callers).
     *
     * @param orgId          the organization in context
     * @param permModules    the module keys of the permissions being assigned
     */
    @Transactional(readOnly = true)
    public void assertPermissionsAllowedForOrg(Long orgId, Set<String> permModules) {
        if (orgId == null || permModules == null || permModules.isEmpty()) return;

        // CORE_SECURITY is always allowed — org admins need it for user/role management
        Set<String> activeModules = orgModuleRepository.findActiveModuleKeysByOrgId(orgId);
        activeModules.add("CORE_SECURITY"); // always permitted

        Set<String> blocked = permModules.stream()
                .filter(m -> m != null && !m.isBlank())
                .filter(m -> !activeModules.contains(m.toUpperCase()))
                .collect(Collectors.toSet());

        if (!blocked.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot assign permissions for modules your organization does not have access to: "
                    + String.join(", ", blocked));
        }
    }

    /**
     * Returns the set of active module keys for an org.
     * Called by DynamicAuthorizationManager's org-module cache.
     */
    @Transactional(readOnly = true)
    public Set<String> getActiveModuleKeys(Long orgId) {
        if (orgId == null) return Set.of();
        Set<String> keys = new HashSet<>(orgModuleRepository.findActiveModuleKeysByOrgId(orgId));
        keys.add("CORE_SECURITY"); // always allowed
        return Collections.unmodifiableSet(keys);
    }

    /**
     * Loads ALL (orgId → moduleKeys) pairs in one query.
     * Used to warm the DynamicAuthorizationManager's org-module cache.
     */
    @Transactional(readOnly = true)
    public Map<Long, Set<String>> loadAllActiveOrgModules() {
        Map<Long, Set<String>> map = new HashMap<>();
        orgModuleRepository.findAllActive().forEach(om -> {
            map.computeIfAbsent(om.getOrganization().getId(), k -> new HashSet<>())
               .add(om.getModuleKey().toUpperCase());
        });
        // CORE_SECURITY always present for every org that has at least one row
        map.values().forEach(s -> s.add("CORE_SECURITY"));
        return map;
    }

    /** Whether a given user is an org-admin for a specific org. */
    @Transactional(readOnly = true)
    public boolean isOrgAdmin(Long userId, Long orgId) {
        return orgAdminScopeRepository.existsByUserIdAndOrganizationIdAndActiveTrue(userId, orgId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STATIC HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    public static Map<String, String> moduleDisplayNames() {
        return Collections.unmodifiableMap(MODULE_DISPLAY);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE
    // ═══════════════════════════════════════════════════════════════════════

    private OrgModuleDTO toDTO(OrgModule om, Organization org) {
        return OrgModuleDTO.builder()
                .id(om.getId())
                .organizationId(org.getId())
                .organizationName(org.getName())
                .moduleKey(om.getModuleKey())
                .moduleDisplayName(MODULE_DISPLAY.getOrDefault(om.getModuleKey(), om.getModuleKey()))
                .active(om.isActive())
                .grantedBy(om.getGrantedBy())
                .grantedAt(om.getGrantedAt())
                .revokedBy(om.getRevokedBy())
                .revokedAt(om.getRevokedAt())
                .notes(om.getNotes())
                .build();
    }

    private Organization requireOrg(Long orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization #" + orgId + " not found."));
    }

    private void requireSuperAdmin() {
        if (!SecurityHelper.isSuperAdmin()) {
            throw new SecurityException("Only SUPER_ADMIN can manage organization module access.");
        }
    }
}
