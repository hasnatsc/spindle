-- =============================================================================
--  Spindle ERP  —  Travel Module Menu + Permission + Role Seed  v1.0 (Phase 1)
--  File   : V101__seed_travel_menu_permission.sql
--  Target : PostgreSQL
--  Depends on: 00_seed_menu_permission_complete.sql (must run first —
--              this file only adds new rows, never modifies existing ones)
--              and V100__create_travel_module.sql (Phase 1 schema)
--
--  Covers:
--    Permissions  — dashboard + Phase 1 Travel entities
--                    (booking, hotel, room type, hotel booking, air ticket,
--                     supplier cost, hotel category, meal plan, airline,
--                     airport, cabin class, settings)
--    Roles        — ROLE_TRAVEL_MANAGER, ROLE_TRAVEL_EXECUTIVE
--    Menus        — MOD_TRAVEL → 5 GROUPs → 9 LEAFs (dashboard-first
--                    convention preserved, matching INV/PUR/SAL/EC pattern)
--    Role-Perms   — both new roles wired to their permission set
--    Role-Menus   — both new roles wired to their menu set
--
--  Naming convention (matches v4.0 / eCommerce seed exactly):
--    Permission:  trv.<entity>.<action>     e.g. trv.booking.confirm
--    Module enum: TRAVEL
--    Category enum: TRAVEL (DASHBOARD for the dashboard permissions)
--    Menu prefix: MOD_TRAVEL / GRP_TRV_* / TRV_*
--
--  Safe to re-run: all INSERTs use ON CONFLICT DO NOTHING.
--  RENUMBER this file to your next available Flyway version if V101 collides.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1. PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

-- ── Travel module dashboard ──────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.dashboard.view', 'View Travel dashboard', '/travel/dashboard', 'GET', 'TRAVEL', 'DASHBOARD',
        true, NOW(), NOW()),
       ('trv.dashboard.summary', 'Travel dashboard summary JSON', '/travel/dashboard/summary', 'GET',
        'TRAVEL', 'DASHBOARD', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Booking (core sales document: services + passengers) ────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.booking.view', 'View bookings', '/travel/bookings/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.booking.create', 'Create booking', '/travel/bookings/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.booking.edit', 'Edit booking', '/travel/bookings/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.booking.delete', 'Delete booking', '/travel/bookings/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.booking.confirm', 'Confirm booking (posts Sales Voucher to GL)', '/travel/bookings/confirm/**', 'POST',
        'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.booking.cancel', 'Cancel booking', '/travel/bookings/cancel/**', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.booking.receipt', 'Create Receipt Voucher from booking', '/travel/bookings/receipt-prefill', 'GET',
        'TRAVEL', 'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Hotel (partner directory) ────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.hotel.view', 'View hotels', '/travel/hotels/**', 'GET', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.hotel.create', 'Create hotel', '/travel/hotels/save', 'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.hotel.edit', 'Edit hotel', '/travel/hotels/save', 'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.hotel.delete', 'Delete hotel', '/travel/hotels/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Room Type (nested under hotel — no standalone menu leaf, still gated) ────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.room_type.view', 'View room types', '/travel/hotels/room-types/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.room_type.create', 'Create room type', '/travel/hotels/room-types/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.room_type.edit', 'Edit room type', '/travel/hotels/room-types/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.room_type.delete', 'Delete room type', '/travel/hotels/room-types/delete/**', 'DELETE', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Hotel Booking (fulfillment for HOTEL service lines) ──────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.hotel_booking.view', 'View hotel bookings', '/travel/hotel-bookings/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.hotel_booking.create', 'Create hotel booking', '/travel/hotel-bookings/save', 'POST', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW()),
       ('trv.hotel_booking.edit', 'Edit hotel booking', '/travel/hotel-bookings/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.hotel_booking.delete', 'Delete hotel booking', '/travel/hotel-bookings/delete/**', 'DELETE', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW()),
       ('trv.hotel_booking.confirm', 'Confirm hotel booking', '/travel/hotel-bookings/confirm/**', 'POST', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Air Ticket (fulfillment for AIR service lines) ───────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.air_ticket.view', 'View air tickets', '/travel/air-tickets/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.air_ticket.create', 'Create air ticket', '/travel/air-tickets/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.air_ticket.edit', 'Edit air ticket', '/travel/air-tickets/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.air_ticket.delete', 'Delete air ticket', '/travel/air-tickets/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Supplier Cost (cost tracking against service lines) ──────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.supplier_cost.view', 'View supplier costs', '/travel/supplier-costs/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.supplier_cost.create', 'Create supplier cost', '/travel/supplier-costs/save', 'POST', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW()),
       ('trv.supplier_cost.edit', 'Edit supplier cost', '/travel/supplier-costs/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.supplier_cost.delete', 'Delete supplier cost', '/travel/supplier-costs/delete/**', 'DELETE', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Hotel Category (lookup) ───────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.hotel_category.view', 'View hotel categories', '/travel/masters/hotel-categories/**', 'GET', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW()),
       ('trv.hotel_category.create', 'Create hotel category', '/travel/masters/hotel-categories/save', 'POST',
        'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.hotel_category.edit', 'Edit hotel category', '/travel/masters/hotel-categories/save', 'POST', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW()),
       ('trv.hotel_category.delete', 'Delete hotel category', '/travel/masters/hotel-categories/delete/**',
        'DELETE', 'TRAVEL', 'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Meal Plan (lookup) ────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.meal_plan.view', 'View meal plans', '/travel/masters/meal-plans/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.meal_plan.create', 'Create meal plan', '/travel/masters/meal-plans/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.meal_plan.edit', 'Edit meal plan', '/travel/masters/meal-plans/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.meal_plan.delete', 'Delete meal plan', '/travel/masters/meal-plans/delete/**', 'DELETE', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Airline (lookup) ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.airline.view', 'View airlines', '/travel/masters/airlines/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.airline.create', 'Create airline', '/travel/masters/airlines/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.airline.edit', 'Edit airline', '/travel/masters/airlines/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.airline.delete', 'Delete airline', '/travel/masters/airlines/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Airport (lookup) ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.airport.view', 'View airports', '/travel/masters/airports/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.airport.create', 'Create airport', '/travel/masters/airports/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.airport.edit', 'Edit airport', '/travel/masters/airports/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.airport.delete', 'Delete airport', '/travel/masters/airports/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Cabin Class (lookup) ──────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.cabin_class.view', 'View cabin classes', '/travel/masters/cabin-classes/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.cabin_class.create', 'Create cabin class', '/travel/masters/cabin-classes/save', 'POST', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW()),
       ('trv.cabin_class.edit', 'Edit cabin class', '/travel/masters/cabin-classes/save', 'POST', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW()),
       ('trv.cabin_class.delete', 'Delete cabin class', '/travel/masters/cabin-classes/delete/**', 'DELETE',
        'TRAVEL', 'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Settings (singleton GL account defaults per org) ──────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.setting.view', 'View Travel GL account defaults', '/travel/settings/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.setting.edit', 'Edit Travel GL account defaults', '/travel/settings/defaults', 'POST', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 2. ROLES
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO sec_roles (name, name_bn, description, master_role, active, created_at, updated_at)
VALUES ('ROLE_TRAVEL_MANAGER', 'ভ্রমণ ব্যবস্থাপক',
        'Full Travel module management: bookings, hotels, air tickets, supplier costs, master data, settings',
        'ROLE_TRAVEL_MANAGER', true, NOW(), NOW()),
       ('ROLE_TRAVEL_EXECUTIVE', 'ভ্রমণ নির্বাহী',
        'Day-to-day travel operations: booking creation, fulfillment, no settings or delete',
        'ROLE_TRAVEL_EXECUTIVE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 3. APP_MENUS  (MODULE → GROUP → LEAF)
-- ═════════════════════════════════════════════════════════════════════════════

-- ── 3A. MODULE level ─────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
VALUES ('MOD_TRAVEL', 'Travel', NULL, 'fa fa-plane', NULL, 96, 'MODULE', 'TRAVEL', 'trv.dashboard.view',
        '_self', true, true, false, NOW(), NOW())
ON CONFLICT (menu_code) DO NOTHING;

-- ── 3B. GROUP level ──────────────────────────────────────────────────────────

-- Bookings & Sales
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_SALES', 'Bookings & Sales', NULL, 'fa fa-ticket-alt', m.id, 10, 'GROUP', 'TRAVEL', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- Hotel Management
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_HOTEL', 'Hotel Management', NULL, 'fa fa-hotel', m.id, 20, 'GROUP', 'TRAVEL', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- Air Ticketing
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_AIR', 'Air Ticketing', NULL, 'fa fa-plane-departure', m.id, 30, 'GROUP', 'TRAVEL', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- Finance
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_FINANCE', 'Finance', NULL, 'fa fa-file-invoice-dollar', m.id, 60, 'GROUP', 'TRAVEL', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- Configuration
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_CONFIG', 'Configuration', NULL, 'fa fa-sliders-h', m.id, 70, 'GROUP', 'TRAVEL', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- ── 3C. LEAF level ───────────────────────────────────────────────────────────

-- ── Bookings & Sales group (dashboard-first, matching v4.0 convention) ───────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_DASHBOARD', 'Dashboard', '/travel/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'TRAVEL',
       'trv.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_SALES'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_BOOKING', 'Bookings', '/travel/bookings', 'fa fa-ticket-alt', g.id, 10, 'LEAF', 'TRAVEL',
       'trv.booking.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_SALES'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Hotel Management group ────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_HOTEL', 'Hotels', '/travel/hotels', 'fa fa-hotel', g.id, 10, 'LEAF', 'TRAVEL', 'trv.hotel.view',
       '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_HOTEL'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_HOTEL_BOOKING', 'Hotel Bookings', '/travel/hotel-bookings', 'fa fa-bed', g.id, 20, 'LEAF', 'TRAVEL',
       'trv.hotel_booking.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_HOTEL'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Air Ticketing group ───────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_AIR_TICKET', 'Air Tickets', '/travel/air-tickets', 'fa fa-plane-departure', g.id, 10, 'LEAF', 'TRAVEL',
       'trv.air_ticket.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_AIR'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Finance group ──────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_SUPPLIER_COST', 'Supplier Costs', '/travel/supplier-costs', 'fa fa-file-invoice-dollar', g.id, 10,
       'LEAF', 'TRAVEL', 'trv.supplier_cost.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_FINANCE'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Configuration group ───────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_MASTER', 'Master Data', '/travel/masters', 'fa fa-database', g.id, 10, 'LEAF', 'TRAVEL',
       'trv.hotel_category.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_SETTING', 'Settings', '/travel/settings', 'fa fa-cog', g.id, 20, 'LEAF', 'TRAVEL', 'trv.setting.view',
       '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 4. ROLE PERMISSIONS  (sec_role_permissions)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_TRAVEL_MANAGER — every action across the module
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'trv.dashboard.view', 'trv.dashboard.summary',
                                              'trv.booking.view', 'trv.booking.create', 'trv.booking.edit',
                                              'trv.booking.delete', 'trv.booking.confirm', 'trv.booking.cancel',
                                              'trv.booking.receipt',
                                              'trv.hotel.view', 'trv.hotel.create', 'trv.hotel.edit',
                                              'trv.hotel.delete',
                                              'trv.room_type.view', 'trv.room_type.create', 'trv.room_type.edit',
                                              'trv.room_type.delete',
                                              'trv.hotel_booking.view', 'trv.hotel_booking.create',
                                              'trv.hotel_booking.edit', 'trv.hotel_booking.delete',
                                              'trv.hotel_booking.confirm',
                                              'trv.air_ticket.view', 'trv.air_ticket.create', 'trv.air_ticket.edit',
                                              'trv.air_ticket.delete',
                                              'trv.supplier_cost.view', 'trv.supplier_cost.create',
                                              'trv.supplier_cost.edit', 'trv.supplier_cost.delete',
                                              'trv.hotel_category.view', 'trv.hotel_category.create',
                                              'trv.hotel_category.edit', 'trv.hotel_category.delete',
                                              'trv.meal_plan.view', 'trv.meal_plan.create', 'trv.meal_plan.edit',
                                              'trv.meal_plan.delete',
                                              'trv.airline.view', 'trv.airline.create', 'trv.airline.edit',
                                              'trv.airline.delete',
                                              'trv.airport.view', 'trv.airport.create', 'trv.airport.edit',
                                              'trv.airport.delete',
                                              'trv.cabin_class.view', 'trv.cabin_class.create',
                                              'trv.cabin_class.edit', 'trv.cabin_class.delete',
                                              'trv.setting.view', 'trv.setting.edit',
                                              'acc.coa.view', 'acc.sub.view',
                                              'apr.request.view', 'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE — day-to-day ops: bookings, fulfillment, no config/delete
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view',
                                              'trv.dashboard.view', 'trv.dashboard.summary',
                                              'trv.booking.view', 'trv.booking.create', 'trv.booking.edit',
                                              'trv.booking.confirm', 'trv.booking.receipt',
                                              'trv.hotel.view',
                                              'trv.room_type.view',
                                              'trv.hotel_booking.view', 'trv.hotel_booking.create',
                                              'trv.hotel_booking.edit', 'trv.hotel_booking.confirm',
                                              'trv.air_ticket.view', 'trv.air_ticket.create',
                                              'trv.air_ticket.edit',
                                              'trv.supplier_cost.view', 'trv.supplier_cost.create',
                                              'trv.hotel_category.view', 'trv.meal_plan.view', 'trv.airline.view',
                                              'trv.airport.view', 'trv.cabin_class.view',
                                              'apr.request.view'
    )
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
ON CONFLICT DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 5. ROLE MENUS  (sec_mrole_menus)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_TRAVEL_MANAGER — full create/edit/delete on every leaf
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('TRV_BOOKING', 'TRV_HOTEL', 'TRV_HOTEL_BOOKING', 'TRV_AIR_TICKET', 'TRV_SUPPLIER_COST',
                       'TRV_MASTER'),
       m.menu_code IN ('TRV_BOOKING', 'TRV_HOTEL', 'TRV_HOTEL_BOOKING', 'TRV_AIR_TICKET', 'TRV_SUPPLIER_COST',
                       'TRV_MASTER', 'TRV_SETTING'),
       m.menu_code IN ('TRV_BOOKING', 'TRV_HOTEL', 'TRV_HOTEL_BOOKING', 'TRV_AIR_TICKET', 'TRV_SUPPLIER_COST',
                       'TRV_MASTER'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
  AND m.menu_code IN ('MOD_TRAVEL',
                      'GRP_TRV_SALES', 'GRP_TRV_HOTEL', 'GRP_TRV_AIR', 'GRP_TRV_FINANCE', 'GRP_TRV_CONFIG',
                      'TRV_DASHBOARD', 'TRV_BOOKING', 'TRV_HOTEL', 'TRV_HOTEL_BOOKING', 'TRV_AIR_TICKET',
                      'TRV_SUPPLIER_COST', 'TRV_MASTER', 'TRV_SETTING')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE — view + limited create/edit, no delete, no config menus
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('TRV_BOOKING', 'TRV_HOTEL_BOOKING', 'TRV_AIR_TICKET', 'TRV_SUPPLIER_COST'),
       m.menu_code IN ('TRV_BOOKING', 'TRV_HOTEL_BOOKING', 'TRV_AIR_TICKET'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
  AND m.menu_code IN ('MOD_TRAVEL',
                      'GRP_TRV_SALES', 'GRP_TRV_HOTEL', 'GRP_TRV_AIR', 'GRP_TRV_FINANCE',
                      'TRV_DASHBOARD', 'TRV_BOOKING', 'TRV_HOTEL', 'TRV_HOTEL_BOOKING', 'TRV_AIR_TICKET',
                      'TRV_SUPPLIER_COST')
ON CONFLICT (role_id, menu_id) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- REFERENCE DATA — meal plans, cabin classes (global, not org-scoped)
-- ═════════════════════════════════════════════════════════════════════════════

INSERT INTO trv_meal_plans (plan_code, plan_name, description, created_at)
VALUES ('RO', 'Room Only', 'No meals included', NOW()),
       ('BB', 'Bed & Breakfast', 'Breakfast included', NOW()),
       ('HB', 'Half Board', 'Breakfast and dinner included', NOW()),
       ('FB', 'Full Board', 'Breakfast, lunch and dinner included', NOW()),
       ('AI', 'All Inclusive', 'All meals and selected beverages included', NOW())
ON CONFLICT (plan_code) DO NOTHING;

INSERT INTO trv_cabin_classes (class_code, class_name)
VALUES ('Y', 'Economy'),
       ('W', 'Premium Economy'),
       ('C', 'Business'),
       ('F', 'First Class')
ON CONFLICT (class_code) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- MANUAL INTEGRATION STEPS (cannot be scripted — do these after migrating)
-- ═════════════════════════════════════════════════════════════════════════════
-- 1. Add TRAVEL to OrgModule.ModuleKey enum (security/entity/OrgModule.java)
--    so it can be toggled per-organization from the Module Access screen.
-- 2. Assign ROLE_TRAVEL_MANAGER / ROLE_TRAVEL_EXECUTIVE to users via the
--    existing user-role assignment screen (sec_user_roles).
-- 3. Enable the TRAVEL module for each organization that needs it
--    (sec_org_modules) before assigning roles — DynamicAuthorizationManager
--    denies by default if the module row doesn't exist for that org.
-- 4. Configure trv_gl_account_defaults per organization (Settings page)
--    before confirming any booking, or the GL bridge will fail validation.


-- ═════════════════════════════════════════════════════════════════════════════
-- VERIFICATION (uncomment to run counts after execution)
-- ═════════════════════════════════════════════════════════════════════════════
-- SELECT 'TRV Permissions' AS table_name, COUNT(*) AS total FROM sec_permissions WHERE name LIKE 'trv.%'
-- UNION ALL SELECT 'TRV Roles',      COUNT(*) FROM sec_roles WHERE name LIKE 'ROLE_TRAVEL%'
-- UNION ALL SELECT 'TRV Menus',      COUNT(*) FROM app_menus WHERE module_name = 'TRAVEL'
-- UNION ALL SELECT 'TRV Role-Perms', COUNT(*) FROM sec_role_permissions rp
--             JOIN sec_roles r ON r.id = rp.role_id WHERE r.name LIKE 'ROLE_TRAVEL%'
-- UNION ALL SELECT 'TRV Role-Menus', COUNT(*) FROM sec_mrole_menus rm
--             JOIN sec_roles r ON r.id = rm.role_id WHERE r.name LIKE 'ROLE_TRAVEL%';

COMMIT;
