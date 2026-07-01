-- =============================================================================
--  Spindle ERP  —  Travel Module Menu + Permission + Role Seed  v1.0 (Phase 2)
--  File   : V103__seed_travel_phase2_menu_permission.sql
--  Target : PostgreSQL
--  Depends on: V101__seed_travel_menu_permission.sql (Phase 1 seed — MOD_TRAVEL,
--              ROLE_TRAVEL_MANAGER/EXECUTIVE must already exist)
--              and V102__create_travel_phase2_packages_visa_tours.sql (schema)
--
--  Covers:
--    Permissions  — package, tour, visa entities
--    Menus        — 2 new GROUPs (Packages & Tours, Visa Services) → 3 LEAFs,
--                    appended under the existing MOD_TRAVEL module
--    Role-Perms   — extends both existing Travel roles
--    Role-Menus   — extends both existing Travel roles
--
--  Naming convention: identical to V101 (trv.<entity>.<action>, TRV_* menus)
--  Safe to re-run: all INSERTs use ON CONFLICT DO NOTHING.
--  RENUMBER this file to run immediately after your Phase 2 schema migration.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1. PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

-- ── Package (pre-built bundled itineraries) ──────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.package.view', 'View packages', '/travel/packages/**', 'GET', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.package.create', 'Create package', '/travel/packages/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.package.edit', 'Edit package', '/travel/packages/save', 'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.package.delete', 'Delete package', '/travel/packages/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Tour (day-trips / excursions) ─────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.tour.view', 'View tours', '/travel/tours/**', 'GET', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.tour.create', 'Create tour', '/travel/tours/save', 'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.tour.edit', 'Edit tour', '/travel/tours/save', 'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.tour.delete', 'Delete tour', '/travel/tours/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Visa Application ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.visa.view', 'View visa applications', '/travel/visa-applications/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.visa.create', 'Create visa application', '/travel/visa-applications/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.visa.edit', 'Edit visa application', '/travel/visa-applications/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.visa.status', 'Update visa application status', '/travel/visa-applications/status/**', 'POST',
        'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.visa.delete', 'Delete visa application', '/travel/visa-applications/delete/**', 'DELETE', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 2. APP_MENUS  (new GROUPs appended to existing MOD_TRAVEL, plus their LEAFs)
-- ═════════════════════════════════════════════════════════════════════════════

-- Packages & Tours
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_PACKAGES', 'Packages & Tours', NULL, 'fa fa-suitcase-rolling', m.id, 40, 'GROUP', 'TRAVEL', NULL,
       '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- Visa Services
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_VISA', 'Visa Services', NULL, 'fa fa-passport', m.id, 50, 'GROUP', 'TRAVEL', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Packages & Tours leaves ───────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_PACKAGE', 'Packages', '/travel/packages', 'fa fa-suitcase-rolling', g.id, 10, 'LEAF', 'TRAVEL',
       'trv.package.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_PACKAGES'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_TOUR', 'Tours', '/travel/tours', 'fa fa-route', g.id, 20, 'LEAF', 'TRAVEL', 'trv.tour.view',
       '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_PACKAGES'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Visa Services leaf ────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_VISA', 'Visa Applications', '/travel/visa-applications', 'fa fa-passport', g.id, 10, 'LEAF', 'TRAVEL',
       'trv.visa.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_VISA'
ON CONFLICT (menu_code) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 3. ROLE PERMISSIONS  (extend existing roles from V101)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_TRAVEL_MANAGER — full access to Packages, Tours, Visa
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'trv.package.view', 'trv.package.create', 'trv.package.edit',
                                              'trv.package.delete',
                                              'trv.tour.view', 'trv.tour.create', 'trv.tour.edit',
                                              'trv.tour.delete',
                                              'trv.visa.view', 'trv.visa.create', 'trv.visa.edit',
                                              'trv.visa.status', 'trv.visa.delete'
    )
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE — view + create/edit, no delete
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'trv.package.view',
                                              'trv.tour.view',
                                              'trv.visa.view', 'trv.visa.create', 'trv.visa.edit',
                                              'trv.visa.status'
    )
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
ON CONFLICT DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 4. ROLE MENUS  (extend existing roles from V101)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_TRAVEL_MANAGER — full create/edit/delete on the new leaves
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA'),
       m.menu_code IN ('TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA'),
       m.menu_code IN ('TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
  AND m.menu_code IN ('GRP_TRV_PACKAGES', 'GRP_TRV_VISA', 'TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE — view + create/edit, no delete
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('TRV_VISA'),
       m.menu_code IN ('TRV_VISA'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
  AND m.menu_code IN ('GRP_TRV_PACKAGES', 'GRP_TRV_VISA', 'TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA')
ON CONFLICT (role_id, menu_id) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- REFERENCE DATA — common visa types (global, not org-scoped; edit/extend freely)
-- ═════════════════════════════════════════════════════════════════════════════

INSERT INTO trv_visa_types (country, visa_category, processing_days, fee_amount, currency, description)
SELECT * FROM (VALUES
    ('Saudi Arabia', 'Umrah Visa',      7,  8000.00, 'BDT', 'Single-entry Umrah pilgrimage visa'),
    ('Saudi Arabia', 'Tourist Visa',    5,  6000.00, 'BDT', 'eVisa, multiple entry, 1 year validity'),
    ('UAE',          'Tourist Visa',    3,  7500.00, 'BDT', '30-day single entry'),
    ('Thailand',     'Tourist Visa',    5,  3500.00, 'BDT', 'Single entry, 60-day stay'),
    ('Malaysia',     'Tourist Visa',    3,  2500.00, 'BDT', 'eVisa, single entry'),
    ('India',        'Tourist Visa',    3,  1200.00, 'BDT', 'eVisa, multiple entry'),
    ('Singapore',    'Tourist Visa',    5,  3000.00, 'BDT', 'Single entry, 30-day stay'),
    ('Schengen',     'Tourist Visa',   15, 12000.00, 'BDT', 'Short-stay Schengen visa, up to 90 days')
) AS v(country, visa_category, processing_days, fee_amount, currency, description)
WHERE NOT EXISTS (
    SELECT 1 FROM trv_visa_types t WHERE t.country = v.country AND t.visa_category = v.visa_category
);


-- ═════════════════════════════════════════════════════════════════════════════
-- VERIFICATION (uncomment to run counts after execution)
-- ═════════════════════════════════════════════════════════════════════════════
-- SELECT 'TRV Phase-2 Permissions' AS table_name, COUNT(*) AS total FROM sec_permissions
--             WHERE name LIKE 'trv.package.%' OR name LIKE 'trv.tour.%' OR name LIKE 'trv.visa.%'
-- UNION ALL SELECT 'TRV Phase-2 Menus', COUNT(*) FROM app_menus
--             WHERE menu_code IN ('GRP_TRV_PACKAGES','GRP_TRV_VISA','TRV_PACKAGE','TRV_TOUR','TRV_VISA')
-- UNION ALL SELECT 'TRV Visa Types', COUNT(*) FROM trv_visa_types;

COMMIT;
