package com.asg.spindleserp.security.service;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.security.auth.SecurityHelper;
import com.asg.spindleserp.security.dto.RoleDTO;
import com.asg.spindleserp.security.entity.Permission;
import com.asg.spindleserp.security.entity.Role;
import com.asg.spindleserp.security.repository.PermissionRepository;
import com.asg.spindleserp.security.repository.RoleRepository;
import com.asg.spindleserp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RoleServiceImpl
 *
 * ══════════════════════════════════════════════════════════════════════
 * ORG-ADMIN PERMISSION ENFORCEMENT (new)
 * ══════════════════════════════════════════════════════════════════════
 *
 * When an org-admin (not a super-admin) creates or updates a role,
 * the permissions they assign are filtered through OrgModuleService:
 *
 *   1. Resolve the set of permission module-keys being assigned.
 *   2. Call orgModuleService.assertPermissionsAllowedForOrg(orgId, modules).
 *   3. If any module is not active for their org → throw, reject the save.
 *
 * Super-admin bypasses this check (SecurityHelper.isSuperAdmin() gate).
 *
 * Everything else is unchanged from the previous version.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository       roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository       userRepository;
    private final OrgModuleService     orgModuleService;   // ← NEW dependency

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final String SUPER_ADMIN   = "ROLE_SUPER_ADMIN";

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public RoleDTO create(RoleDTO dto) {
        String roleName = normalizeName(dto.getName());
        if (roleRepository.existsByName(roleName))
            throw new IllegalArgumentException("Role '" + roleName + "' already exists.");

        Set<Permission> perms = resolvePermissions(dto.getPermissionIds());

        // ── ORG-ADMIN CHECK: reject permissions for disabled modules ──────
        if (!SecurityHelper.isSuperAdmin()) {
            Long orgId = SecurityHelper.currentOrgId().orElse(null);
            assertModulesAllowed(orgId, perms);
        }

        Role role = Role.builder()
                .name(roleName)
                .nameBn(dto.getNameBn())
                .description(dto.getDescription())
                .masterRole(dto.getMasterRole())
                .active(dto.isActive())
                .permissions(perms)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Role saved = roleRepository.save(role);
        log.info("Role '{}' created by {}", saved.getName(), actor());
        return toDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleDTO> findById(Long id) {
        return roleRepository.findById(id).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> findAll() {
        return roleRepository.findAllActiveWithPermissions().stream()
                .map(this::toDTO).toList();
    }

    /**
     * Returns only roles whose permissions are within the modules active for
     * the current org. Super-admin gets all roles.
     * Used by the user-form role dropdown for org-admins.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> findAllForCurrentOrg() {
        if (SecurityHelper.isSuperAdmin()) return findAll();

        Long orgId = SecurityHelper.currentOrgId().orElse(null);
        Set<String> activeModules = orgId != null
                ? orgModuleService.getActiveModuleKeys(orgId)
                : Set.of("CORE_SECURITY");

        return roleRepository.findAllActiveWithPermissions().stream()
                .filter(role -> role.getPermissions().stream().allMatch(p ->
                        p.getModule() == null
                        || p.getModule().isBlank()
                        || activeModules.contains(p.getModule().toUpperCase())))
                .map(this::toDTO)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public RoleDTO update(Long id, RoleDTO dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role #" + id + " not found."));

        guardSuperAdmin(role);

        String roleName = normalizeName(dto.getName());
        if (roleRepository.existsByNameAndIdNot(roleName, id))
            throw new IllegalArgumentException("Role '" + roleName + "' already exists.");

        Set<Permission> perms = resolvePermissions(dto.getPermissionIds());

        // ── ORG-ADMIN CHECK ──────────────────────────────────────────────
        if (!SecurityHelper.isSuperAdmin()) {
            Long orgId = SecurityHelper.currentOrgId().orElse(null);
            assertModulesAllowed(orgId, perms);
        }

        role.setName(roleName);
        role.setNameBn(dto.getNameBn());
        role.setDescription(dto.getDescription());
        role.setMasterRole(dto.getMasterRole());
        role.setActive(dto.isActive());
        role.setPermissions(perms);
        role.setUpdatedAt(LocalDateTime.now());

        Role saved = roleRepository.save(role);
        log.info("Role '{}' updated by {}", saved.getName(), actor());
        return toDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role #" + id + " not found."));
        guardSuperAdmin(role);

        long userCount = userRepository.countByRoleId(id);
        if (userCount > 0)
            throw new IllegalArgumentException(
                    "Cannot delete: " + userCount + " user(s) are assigned this role.");

        roleRepository.delete(role);
        log.info("Role '{}' deleted by {}", role.getName(), actor());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOGGLE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public RoleDTO toggleStatus(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role #" + id + " not found."));
        guardSuperAdmin(role);

        role.setActive(!role.isActive());
        role.setUpdatedAt(LocalDateTime.now());
        return toDTO(roleRepository.save(role));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATATABLE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DataTableResponse datatableList(int draw, int start, int length, String search) {
        List<Role> all = roleRepository.findAllActiveWithPermissions();

        // Org-admin: filter to only roles within active modules for their org
        if (!SecurityHelper.isSuperAdmin()) {
            Long orgId = SecurityHelper.currentOrgId().orElse(null);
            Set<String> activeModules = orgId != null
                    ? orgModuleService.getActiveModuleKeys(orgId)
                    : Set.of("CORE_SECURITY");
            all = all.stream()
                    .filter(role -> role.getPermissions().stream().allMatch(p ->
                            p.getModule() == null || p.getModule().isBlank()
                            || activeModules.contains(p.getModule().toUpperCase())))
                    .collect(Collectors.toList());
        }

        String q = search == null ? "" : search.trim().toLowerCase();
        List<Role> filtered = q.isBlank() ? all : all.stream()
                .filter(r -> r.getName().toLowerCase().contains(q)
                          || (r.getDescription() != null && r.getDescription().toLowerCase().contains(q)))
                .toList();

        long total         = roleRepository.count();
        long filteredCount = q.isBlank() ? total : filtered.size();

        List<Role> page = filtered.stream().skip(start).limit(length).toList();
        List<Map<String, Object>> rows = new ArrayList<>();
        int sl = start + 1;

        for (Role r : page) {
            long uc = userRepository.countByRoleId(r.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sl",           sl++);
            row.put("name",         "<code>" + r.getName() + "</code>");
            row.put("display_name", r.getName().replace("ROLE_", ""));
            row.put("name_bn",      r.getNameBn() != null ? r.getNameBn() : "—");
            row.put("description",  r.getDescription() != null ? r.getDescription() : "—");
            row.put("permissions",  "<span class='badge bg-info text-dark'>"
                                    + r.getPermissions().size() + " perms</span>");
            row.put("users",        "<span class='badge bg-secondary'>" + uc + " users</span>");
            row.put("status",       r.isActive()
                    ? "<span class='badge bg-success'>Active</span>"
                    : "<span class='badge bg-danger'>Inactive</span>");
            row.put("created_at",   r.getCreatedAt() != null ? r.getCreatedAt().format(DT) : "—");
            row.put("actions",      buildActions(r.getId(), r.isActive()));
            rows.add(row);
        }

        return DataTableResponse.of(draw, total, filteredCount, rows);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONVERSION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public RoleDTO toDTO(Role role) {
        Set<Long>   ids   = role.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());
        Set<String> names = role.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet());
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .nameBn(role.getNameBn())
                .description(role.getDescription())
                .masterRole(role.getMasterRole())
                .active(role.isActive())
                .permissionIds(ids)
                .permissionNames(names)
                .permissionCount(ids.size())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Checks that all permissions being assigned belong to modules
     * that are active for the given org.
     */
    private void assertModulesAllowed(Long orgId, Set<Permission> perms) {
        if (orgId == null || perms.isEmpty()) return;
        Set<String> permModules = perms.stream()
                .map(Permission::getModule)
                .filter(m -> m != null && !m.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        orgModuleService.assertPermissionsAllowedForOrg(orgId, permModules);
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        name = name.trim().toUpperCase().replace(" ", "_");
        return name.startsWith("ROLE_") ? name : "ROLE_" + name;
    }

    private Set<Permission> resolvePermissions(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(permissionRepository.findAllById(ids));
    }

    private void guardSuperAdmin(Role role) {
        if (SUPER_ADMIN.equals(role.getName()))
            throw new IllegalArgumentException("The ROLE_SUPER_ADMIN cannot be modified.");
    }

    private String actor() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        } catch (Exception e) { return "system"; }
    }

    private String buildActions(Long id, boolean active) {
        String toggleIcon  = active ? "fa-toggle-on text-success" : "fa-toggle-off text-muted";
        String toggleTitle = active ? "Deactivate" : "Activate";
        return "<div class='btn-group btn-group-sm'>"
             + btn("info",    "fa-eye",    "View",   "roleShow("   + id + ")")
             + btn("warning", "fa-pencil", "Edit",   "roleEdit("   + id + ")")
             + "<button class='btn btn-outline-secondary' title='" + toggleTitle
             +   "' onclick='roleToggle(" + id + ")'>"
             +   "<i class='fa " + toggleIcon + "'></i></button>"
             + btn("danger",  "fa-trash",  "Delete", "roleDelete(" + id + ")")
             + "</div>";
    }

    private String btn(String color, String icon, String title, String onclick) {
        return "<button class='btn btn-outline-" + color + "' title='" + title
             + "' onclick='" + onclick + "'><i class='fa " + icon + "'></i></button>";
    }
}
