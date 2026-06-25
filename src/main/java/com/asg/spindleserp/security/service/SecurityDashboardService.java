package com.asg.spindleserp.security.service;

import com.asg.spindleserp.security.auth.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * SecurityDashboardService
 *
 * Org-scoped security dashboard summary.
 * Called by GET /security/dashboard/summary
 *
 * Response keys consumed by security-dashboard.html:
 *
 *   users: {
 *     total, active, disabled, deleted, locked, superAdminCount,
 *     noRoleCount, loggedInToday, addedMTD
 *   }
 *   roles: {
 *     total, active, inactive, avgPermissionsPerRole
 *   }
 *   permissions: {
 *     total, active, inactive
 *   }
 *   menus: {
 *     total, active, inactive, moduleCount, groupCount, leafCount
 *   }
 *   usersByRole:          [ {role_name, user_count} ]
 *   rolesByPermCount:     [ {role_name, perm_count} ] — top 10 richest roles
 *   menusByModule:        [ {module_name, menu_count} ]
 *   recentUsers:          [ last 10 created users ]
 *   recentLoginUsers:     [ last 10 by last_login_at ]
 *   disabledUsers:        [ all disabled/locked users ]
 *   noRoleUsers:          [ users with no roles assigned ]
 *   permissionsByModule:  [ {module, active_count, inactive_count} ]
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecurityDashboardService {

    private final JdbcTemplate jdbc;

    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> summary() {
        Long   orgId    = SecurityHelper.currentOrgId().orElse(null);
        String fOrg     = orgId != null ? " AND organization_id = " + orgId : "";
        String fOrgU    = orgId != null ? " AND u.organization_id = " + orgId : "";
        String mtdStart = LocalDate.now().withDayOfMonth(1).toString();
        String today    = LocalDate.now().toString();

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            _loadUserKpis(result, orgId, fOrg, fOrgU, mtdStart, today);
            _loadRoleKpis(result);
            _loadPermissionKpis(result);
            _loadMenuKpis(result);
            _loadUsersByRole(result, fOrgU);
            _loadRolesByPermCount(result);
            _loadMenusByModule(result);
            _loadRecentUsers(result, orgId, fOrg);
            _loadRecentLoginUsers(result, orgId, fOrg);
            _loadDisabledUsers(result, orgId, fOrg);
            _loadNoRoleUsers(result, orgId, fOrg);
            _loadPermissionsByModule(result);
        } catch (Exception e) {
            log.error("SecurityDashboard summary error", e);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ── 1. USER KPIs ─────────────────────────────────────────────────────────
    private void _loadUserKpis(Map<String, Object> result, Long orgId,
                                String fOrg, String fOrgU,
                                String mtdStart, String today) {
        String sql = """
            SELECT
              COUNT(*)                                         AS total,
              COUNT(*) FILTER (WHERE enabled = true
                                 AND deleted = false)          AS active,
              COUNT(*) FILTER (WHERE enabled = false
                                 AND deleted = false)          AS disabled,
              COUNT(*) FILTER (WHERE deleted = true)           AS deleted,
              COUNT(*) FILTER (WHERE account_non_locked = false
                                 AND deleted = false)          AS locked,
              COUNT(*) FILTER (WHERE created_at >= ?::timestamp
                                 AND deleted = false)          AS added_mtd,
              COUNT(*) FILTER (WHERE last_login_at >= ?::timestamp) AS logged_in_today
            FROM sec_users WHERE 1=1
            """ + fOrg;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, mtdStart, today);

        // Super-admin count
        Long superAdminCount = jdbc.queryForObject("""
            SELECT COUNT(DISTINCT u.id) FROM sec_users u
            JOIN sec_user_roles ur ON ur.user_id = u.id
            JOIN sec_roles r ON r.id = ur.role_id
            WHERE r.name = 'ROLE_SUPER_ADMIN' AND u.deleted = false
            """ + fOrgU.replace("u.organization_id", "u.organization_id"), Long.class);

        // No-role count
        Long noRoleCount = jdbc.queryForObject("""
            SELECT COUNT(*) FROM sec_users u
            WHERE u.deleted = false AND u.enabled = true
            AND NOT EXISTS (SELECT 1 FROM sec_user_roles ur WHERE ur.user_id = u.id)
            """ + fOrgU, Long.class);

        Map<String, Object> users = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            users.put("total",          toLong(r, "total"));
            users.put("active",         toLong(r, "active"));
            users.put("disabled",       toLong(r, "disabled"));
            users.put("deleted",        toLong(r, "deleted"));
            users.put("locked",         toLong(r, "locked"));
            users.put("addedMTD",       toLong(r, "added_mtd"));
            users.put("loggedInToday",  toLong(r, "logged_in_today"));
        }
        users.put("superAdminCount", superAdminCount != null ? superAdminCount : 0L);
        users.put("noRoleCount",     noRoleCount     != null ? noRoleCount     : 0L);
        result.put("users", users);
    }

    // ── 2. ROLE KPIs ─────────────────────────────────────────────────────────
    private void _loadRoleKpis(Map<String, Object> result) {
        String sql = """
            SELECT
              COUNT(*)                                   AS total,
              COUNT(*) FILTER (WHERE active = true)     AS active,
              COUNT(*) FILTER (WHERE active = false)    AS inactive
            FROM sec_roles
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql);

        // Avg permissions per active role
        Double avgPerms = jdbc.queryForObject("""
            SELECT COALESCE(AVG(perm_count),0) FROM (
              SELECT r.id, COUNT(rp.permission_id) AS perm_count
              FROM sec_roles r
              LEFT JOIN sec_role_permissions rp ON rp.role_id = r.id
              WHERE r.active = true
              GROUP BY r.id
            ) t
            """, Double.class);

        Map<String, Object> roles = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            roles.put("total",                toLong(r, "total"));
            roles.put("active",               toLong(r, "active"));
            roles.put("inactive",             toLong(r, "inactive"));
            roles.put("avgPermissionsPerRole", avgPerms != null ? Math.round(avgPerms * 10.0) / 10.0 : 0.0);
        }
        result.put("roles", roles);
    }

    // ── 3. PERMISSION KPIs ───────────────────────────────────────────────────
    private void _loadPermissionKpis(Map<String, Object> result) {
        String sql = """
            SELECT
              COUNT(*)                                   AS total,
              COUNT(*) FILTER (WHERE active = true)     AS active,
              COUNT(*) FILTER (WHERE active = false)    AS inactive
            FROM sec_permissions
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        Map<String, Object> perms = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            perms.put("total",    toLong(r, "total"));
            perms.put("active",   toLong(r, "active"));
            perms.put("inactive", toLong(r, "inactive"));
        }
        result.put("permissions", perms);
    }

    // ── 4. MENU KPIs ─────────────────────────────────────────────────────────
    private void _loadMenuKpis(Map<String, Object> result) {
        String sql = """
            SELECT
              COUNT(*)                                          AS total,
              COUNT(*) FILTER (WHERE active = true
                                 AND deleted = false)          AS active,
              COUNT(*) FILTER (WHERE (active = false
                                  OR deleted = true))          AS inactive,
              COUNT(*) FILTER (WHERE menu_type = 'MODULE'
                                 AND deleted = false)          AS module_count,
              COUNT(*) FILTER (WHERE menu_type = 'GROUP'
                                 AND deleted = false)          AS group_count,
              COUNT(*) FILTER (WHERE menu_type = 'LEAF'
                                 AND deleted = false)          AS leaf_count
            FROM app_menus
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        Map<String, Object> menus = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            Map<String, Object> r = rows.get(0);
            menus.put("total",       toLong(r, "total"));
            menus.put("active",      toLong(r, "active"));
            menus.put("inactive",    toLong(r, "inactive"));
            menus.put("moduleCount", toLong(r, "module_count"));
            menus.put("groupCount",  toLong(r, "group_count"));
            menus.put("leafCount",   toLong(r, "leaf_count"));
        }
        result.put("menus", menus);
    }

    // ── 5. USERS BY ROLE ─────────────────────────────────────────────────────
    private void _loadUsersByRole(Map<String, Object> result, String fOrgU) {
        String sql = """
            SELECT r.name AS role_name, COUNT(DISTINCT u.id) AS user_count
            FROM sec_roles r
            LEFT JOIN sec_user_roles ur ON ur.role_id = r.id
            LEFT JOIN sec_users u ON u.id = ur.user_id AND u.deleted = false
            WHERE r.active = true
            GROUP BY r.id, r.name
            ORDER BY user_count DESC
            LIMIT 12
            """;
        result.put("usersByRole", jdbc.queryForList(sql));
    }

    // ── 6. ROLES BY PERMISSION COUNT — top 10 richest ────────────────────────
    private void _loadRolesByPermCount(Map<String, Object> result) {
        String sql = """
            SELECT r.name AS role_name, COUNT(rp.permission_id) AS perm_count
            FROM sec_roles r
            LEFT JOIN sec_role_permissions rp ON rp.role_id = r.id
            WHERE r.active = true
            GROUP BY r.id, r.name
            ORDER BY perm_count DESC
            LIMIT 10
            """;
        result.put("rolesByPermCount", jdbc.queryForList(sql));
    }

    // ── 7. MENUS BY MODULE ───────────────────────────────────────────────────
    private void _loadMenusByModule(Map<String, Object> result) {
        String sql = """
            SELECT COALESCE(module_name,'—') AS module_name,
                   COUNT(*) AS menu_count
            FROM app_menus
            WHERE active = true AND deleted = false
            GROUP BY module_name
            ORDER BY menu_count DESC
            """;
        result.put("menusByModule", jdbc.queryForList(sql));
    }

    // ── 8. RECENT USERS — last 10 created ────────────────────────────────────
    private void _loadRecentUsers(Map<String, Object> result, Long orgId, String fOrg) {
        String sql = """
            SELECT u.id, u.username, u.full_name, u.email, u.phone,
                   u.enabled, u.deleted, u.account_non_locked,
                   TO_CHAR(u.created_at, 'DD-Mon-YYYY HH24:MI') AS created_at,
                   COALESCE(u.created_by, '—') AS created_by,
                   (SELECT STRING_AGG(r.name, ', ')
                    FROM sec_user_roles ur
                    JOIN sec_roles r ON r.id = ur.role_id
                    WHERE ur.user_id = u.id) AS roles
            FROM sec_users u
            WHERE u.deleted = false
            """ + fOrg + """
            ORDER BY u.created_at DESC NULLS LAST
            LIMIT 10
            """;
        result.put("recentUsers", jdbc.queryForList(sql));
    }

    // ── 9. RECENT LOGIN USERS — last 10 by login ─────────────────────────────
    private void _loadRecentLoginUsers(Map<String, Object> result, Long orgId, String fOrg) {
        String sql = """
            SELECT u.id, u.username, u.full_name,
                   TO_CHAR(u.last_login_at, 'DD-Mon-YYYY HH24:MI') AS last_login_at,
                   u.enabled
            FROM sec_users u
            WHERE u.deleted = false AND u.last_login_at IS NOT NULL
            """ + fOrg + """
            ORDER BY u.last_login_at DESC
            LIMIT 10
            """;
        result.put("recentLoginUsers", jdbc.queryForList(sql));
    }

    // ── 10. DISABLED / LOCKED USERS ──────────────────────────────────────────
    private void _loadDisabledUsers(Map<String, Object> result, Long orgId, String fOrg) {
        String sql = """
            SELECT u.id, u.username, u.full_name, u.email,
                   u.enabled, u.account_non_locked, u.deleted,
                   CASE
                     WHEN u.deleted = true         THEN 'DELETED'
                     WHEN u.account_non_locked = false THEN 'LOCKED'
                     WHEN u.enabled = false        THEN 'DISABLED'
                   END AS status_reason,
                   TO_CHAR(u.updated_at, 'DD-Mon-YYYY') AS updated_at
            FROM sec_users u
            WHERE u.deleted = false
              AND (u.enabled = false OR u.account_non_locked = false)
            """ + fOrg + """
            ORDER BY u.updated_at DESC NULLS LAST
            LIMIT 20
            """;
        result.put("disabledUsers", jdbc.queryForList(sql));
    }

    // ── 11. USERS WITH NO ROLES ───────────────────────────────────────────────
    private void _loadNoRoleUsers(Map<String, Object> result, Long orgId, String fOrg) {
        String sql = """
            SELECT u.id, u.username, u.full_name, u.email, u.enabled,
                   TO_CHAR(u.created_at, 'DD-Mon-YYYY') AS created_at
            FROM sec_users u
            WHERE u.deleted = false AND u.enabled = true
            AND NOT EXISTS (
                SELECT 1 FROM sec_user_roles ur WHERE ur.user_id = u.id
            )
            """ + fOrg + """
            ORDER BY u.created_at DESC NULLS LAST
            LIMIT 15
            """;
        result.put("noRoleUsers", jdbc.queryForList(sql));
    }

    // ── 12. PERMISSIONS BY MODULE ─────────────────────────────────────────────
    private void _loadPermissionsByModule(Map<String, Object> result) {
        String sql = """
            SELECT COALESCE(module,'OTHER') AS module,
                   COUNT(*) FILTER (WHERE active = true)  AS active_count,
                   COUNT(*) FILTER (WHERE active = false) AS inactive_count,
                   COUNT(*) AS total_count
            FROM sec_permissions
            GROUP BY module
            ORDER BY total_count DESC
            """;
        result.put("permissionsByModule", jdbc.queryForList(sql));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Long toLong(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return 0L;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }
}
