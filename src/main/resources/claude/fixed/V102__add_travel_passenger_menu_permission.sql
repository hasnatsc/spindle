-- =============================================================================
--  Spindle ERP  —  Travel Passenger Menu + Permission Seed
--  File   : V102__add_travel_passenger_menu_permission.sql
--  Target : PostgreSQL
--
--  Adds Passenger management to the Travel module:
--    Permissions  — trv.passenger.view/create/edit/delete
--    Menus        — TRV_PASSENGER leaf under GRP_TRV_SALES (display_order 15)
--    Role-Perms   — wired to ROLE_TRAVEL_MANAGER + ROLE_TRAVEL_EXECUTIVE
--    Role-Menus   — wired to both roles
--
--  Safe to re-run: all INSERTs use ON CONFLICT DO NOTHING.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1. PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES ('trv.passenger.view', 'View passengers', '/travel/passengers/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.passenger.create', 'Create passenger', '/travel/passengers/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.passenger.edit', 'Edit passenger', '/travel/passengers/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.passenger.delete', 'Delete passenger', '/travel/passengers/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 2. MENU  (leaf under Bookings & Sales group)
-- ═════════════════════════════════════════════════════════════════════════════

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_PASSENGER', 'Passengers', '/travel/passengers', 'fa fa-users', g.id, 15, 'LEAF', 'TRAVEL',
       'trv.passenger.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_SALES'
ON CONFLICT (menu_code) DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 3. ROLE PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_TRAVEL_MANAGER — full CRUD on passengers
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         CROSS JOIN sec_permissions p
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
  AND p.name IN ('trv.passenger.view', 'trv.passenger.create', 'trv.passenger.edit', 'trv.passenger.delete')
ON CONFLICT DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE — view + create/edit, no delete
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         CROSS JOIN sec_permissions p
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
  AND p.name IN ('trv.passenger.view', 'trv.passenger.create', 'trv.passenger.edit')
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 4. ROLE MENUS
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_TRAVEL_MANAGER — full CRUD
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
       true,
       true,
       true,
       true,
       NOW(), NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
  AND m.menu_code = 'TRV_PASSENGER'
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE — view + create/edit, no delete
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
       true,
       true,
       true,
       false,
       NOW(), NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
  AND m.menu_code = 'TRV_PASSENGER'
ON CONFLICT (role_id, menu_id) DO NOTHING;

COMMIT;
