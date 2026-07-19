-- =============================================================================
--  Spindle ERP  —  User → Role → Menu Access Control View  v1.0
--  File   : V11__view_user_role_menu_access.sql
--  Target : PostgreSQL 15+
--
--  Provides a complete view of which users have access to which menus
--  through their role assignments, including CRUD permission flags.
--
--  Access chain:  sec_users → sec_user_roles → sec_roles
--                 → sec_mrole_menus → app_menus
--
--  Use this view for:
--    • Auditing who can see/do what
--    • Debugging access control issues
--    • Building admin reports on user entitlements
--    • Understanding the effective permissions for any user
--
--  Query examples:
--    SELECT * FROM v_user_role_menu_access WHERE username = 'admin';
--    SELECT * FROM v_user_role_menu_access WHERE role_name = 'ROLE_ACCOUNTANT';
--    SELECT username, menu_name, can_view, can_create, can_edit, can_delete
--      FROM v_user_role_menu_access
--      WHERE menu_type = 'LEAF' AND can_view = true
--      ORDER BY username, module_name, display_order;
--    SELECT username, COUNT(*) AS visible_menus
--      FROM v_user_role_menu_access WHERE can_view = true
--      GROUP BY username ORDER BY username;
-- =============================================================================

CREATE OR REPLACE VIEW v_user_role_menu_access AS
SELECT
    -- User
    u.id              AS user_id,
    u.username        AS username,
    u.full_name       AS full_name,
    u.organization_id AS organization_id,
    u.enabled         AS user_enabled,

    -- Role
    r.id              AS role_id,
    r.name            AS role_name,
    r.name_bn         AS role_name_bn,
    r.active          AS role_active,

    -- Menu
    m.id              AS menu_id,
    m.menu_code       AS menu_code,
    m.menu_name       AS menu_name,
    m.menu_url        AS menu_url,
    m.icon            AS menu_icon,
    m.parent_id       AS parent_menu_id,
    m.display_order   AS display_order,
    m.menu_type       AS menu_type,       -- MODULE | GROUP | LEAF
    m.module_name     AS module_name,
    m.required_permission AS required_permission,
    m.active          AS menu_active,
    m.visible         AS menu_visible,

    -- Parent menu name (for hierarchy context)
    p.menu_name       AS parent_menu_name,
    p.menu_code       AS parent_menu_code,

    -- CRUD permission flags from role-menu mapping
    rma.can_view      AS can_view,
    rma.can_create    AS can_create,
    rma.can_edit      AS can_edit,
    rma.can_delete    AS can_delete,

    -- Module-level org access (whether this user's org has the module active)
    om.active         AS org_module_active

FROM sec_users u
         JOIN sec_user_roles ur   ON ur.user_id = u.id
         JOIN sec_roles r         ON r.id = ur.role_id
         JOIN sec_mrole_menus rma ON rma.role_id = r.id
         JOIN app_menus m         ON m.id = rma.menu_id
         LEFT JOIN app_menus p    ON p.id = m.parent_id
         LEFT JOIN sec_org_modules om
                   ON om.organization_id = u.organization_id
                   AND om.module_key = m.module_name
WHERE u.deleted = false
  AND r.active = true
  AND m.active = true
  AND m.deleted = false
ORDER BY u.username, m.display_order;


-- =============================================================================
--  Supplementary View: User effective menu access (merged across roles)
--  When a user has multiple roles, the most-permissive CRUD flags win.
-- =============================================================================

CREATE OR REPLACE VIEW v_user_effective_menu_access AS
SELECT
    u.id              AS user_id,
    u.username        AS username,
    u.full_name       AS full_name,
    u.organization_id AS organization_id,
    m.id              AS menu_id,
    m.menu_code       AS menu_code,
    m.menu_name       AS menu_name,
    m.menu_url        AS menu_url,
    m.menu_type       AS menu_type,
    m.module_name     AS module_name,
    m.parent_id       AS parent_menu_id,
    m.display_order   AS display_order,
    m.required_permission AS required_permission,
    -- Most-permissive wins across all user's roles (OR merge)
    BOOLEAN_OR(rma.can_view)   AS can_view,
    BOOLEAN_OR(rma.can_create) AS can_create,
    BOOLEAN_OR(rma.can_edit)   AS can_edit,
    BOOLEAN_OR(rma.can_delete) AS can_delete
FROM sec_users u
         JOIN sec_user_roles ur   ON ur.user_id = u.id
         JOIN sec_roles r         ON r.id = ur.role_id
         JOIN sec_mrole_menus rma ON rma.role_id = r.id
         JOIN app_menus m         ON m.id = rma.menu_id
WHERE u.deleted = false
  AND r.active = true
  AND m.active = true
  AND m.deleted = false
GROUP BY u.id, u.username, u.full_name, u.organization_id,
         m.id, m.menu_code, m.menu_name, m.menu_url,
         m.menu_type, m.module_name, m.parent_id, m.display_order,
         m.required_permission
ORDER BY u.username, m.display_order;


-- =============================================================================
--  VERIFICATION (uncomment to check)
-- =============================================================================
-- SELECT 'Users with menus' AS section, COUNT(*) FROM v_user_role_menu_access;
-- SELECT 'Unique users'     AS section, COUNT(DISTINCT username) FROM v_user_role_menu_access;
-- SELECT 'Unique menus'     AS section, COUNT(DISTINCT menu_code) FROM v_user_role_menu_access;
-- SELECT username, COUNT(*) AS menus_visible
--   FROM v_user_role_menu_access WHERE can_view = true
--   GROUP BY username ORDER BY username;
