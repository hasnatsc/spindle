-- =============================================================================
--  Spindle ERP  —  eCommerce Module Menu + Permission + Role Seed  v1.0
--  File   : 01_seed_ecommerce_menu_permission.sql
--  Target : PostgreSQL
--  Depends on: 00_seed_menu_permission_complete.sql (must run first —
--              this file only adds new rows, never modifies existing ones)
--
--  Covers:
--    Permissions  — dashboard + all 14 eCommerce admin entities
--                    (catalog, category, customer, order, coupon,
--                     shipping method, review, home section, GL defaults,
--                     tax class, return, refund, payment, cart, settings)
--    Roles        — ROLE_ECOMMERCE_MANAGER, ROLE_ECOMMERCE_EXECUTIVE
--    Menus        — MOD_ECOMMERCE → 6 GROUPs → 24 LEAFs (dashboard-first
--                    convention preserved, matching INV/PUR/SAL pattern)
--    Role-Perms   — both new roles wired to their permission set
--    Role-Menus   — both new roles wired to their menu set
--
--  Naming convention (matches v4.0 exactly):
--    Permission:  ec.<entity>.<action>     e.g. ec.category.view
--    Module enum: ECOMMERCE
--    Category enum: ECOMMERCE
--    Menu prefix: MOD_ECOMMERCE / GRP_EC_* / EC_*
--
--  Safe to re-run: all INSERTs use ON CONFLICT DO NOTHING.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1. PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

-- ── eCommerce module dashboard (consolidated under Dashboard/Main, matching
--    the v4.0 convention where every module dashboard lives in this block) ────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.dashboard.view', 'View eCommerce dashboard', '/ecommerce/dashboard', 'GET', 'ECOMMERCE', 'DASHBOARD',
        true, NOW(), NOW()),
       ('ec.dashboard.summary', 'eCommerce dashboard summary JSON', '/ecommerce/dashboard/summary', 'GET',
        'ECOMMERCE', 'DASHBOARD', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Product Catalog ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.product.view', 'View product catalog', '/ecommerce/products/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.product.create', 'Create product catalog entry', '/ecommerce/products/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.product.edit', 'Edit product catalog entry', '/ecommerce/products/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.product.delete', 'Delete product catalog entry', '/ecommerce/products/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.product.toggle', 'Toggle product publish status', '/ecommerce/products/toggle-publish/**', 'POST',
        'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Category (storefront navigation tree + attribute schema) ────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.category.view', 'View storefront categories', '/ecommerce/categories/**', 'GET', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.category.create', 'Create storefront category', '/ecommerce/categories/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.category.edit', 'Edit storefront category', '/ecommerce/categories/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.category.delete', 'Delete storefront category', '/ecommerce/categories/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.category.toggle', 'Toggle category status', '/ecommerce/categories/toggle/**', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Customer (B2C portal registrations) ──────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.customer.view', 'View storefront customers', '/ecommerce/customers/**', 'GET', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.customer.create', 'Create storefront customer', '/ecommerce/customers/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.customer.edit', 'Edit storefront customer', '/ecommerce/customers/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.customer.delete', 'Delete storefront customer', '/ecommerce/customers/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.customer.toggle', 'Toggle customer account status', '/ecommerce/customers/toggle/**', 'POST',
        'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Order (lifecycle management) ─────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.order.view', 'View storefront orders', '/ecommerce/orders/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.order.status', 'Update order status', '/ecommerce/orders/status/**', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.order.delete', 'Delete order', '/ecommerce/orders/delete/**', 'DELETE', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Coupon ────────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.coupon.view', 'View discount coupons', '/ecommerce/coupons/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.coupon.create', 'Create discount coupon', '/ecommerce/coupons/save', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.coupon.edit', 'Edit discount coupon', '/ecommerce/coupons/save', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.coupon.delete', 'Delete discount coupon', '/ecommerce/coupons/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.coupon.toggle', 'Toggle coupon status', '/ecommerce/coupons/toggle/**', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Shipping Method ───────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.shipping_method.view', 'View shipping methods', '/ecommerce/shipping-methods/**', 'GET', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.shipping_method.create', 'Create shipping method', '/ecommerce/shipping-methods/save', 'POST',
        'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
       ('ec.shipping_method.edit', 'Edit shipping method', '/ecommerce/shipping-methods/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.shipping_method.delete', 'Delete shipping method', '/ecommerce/shipping-methods/delete/**', 'DELETE',
        'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
       ('ec.shipping_method.toggle', 'Toggle shipping method status', '/ecommerce/shipping-methods/toggle/**',
        'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Review (moderation: approve/reject/hide) ─────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.review.view', 'View customer reviews', '/ecommerce/reviews/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.review.approve', 'Approve customer review', '/ecommerce/reviews/approve/**', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.review.reject', 'Reject customer review', '/ecommerce/reviews/reject/**', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.review.hide', 'Hide customer review', '/ecommerce/reviews/hide/**', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.review.delete', 'Delete customer review', '/ecommerce/reviews/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Home Section (CMS homepage blocks) ───────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.home_section.view', 'View homepage sections', '/ecommerce/home-sections/**', 'GET', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.home_section.create', 'Create homepage section', '/ecommerce/home-sections/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.home_section.edit', 'Edit homepage section', '/ecommerce/home-sections/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.home_section.delete', 'Delete homepage section', '/ecommerce/home-sections/delete/**', 'DELETE',
        'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
       ('ec.home_section.toggle', 'Toggle homepage section status', '/ecommerce/home-sections/toggle/**', 'POST',
        'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── GL Account Defaults (singleton settings) ─────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.gl_defaults.view', 'View eCommerce GL account defaults', '/ecommerce/gl-defaults/**', 'GET',
        'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
       ('ec.gl_defaults.edit', 'Edit eCommerce GL account defaults', '/ecommerce/gl-defaults/save', 'POST',
        'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Tax Class (VAT/tax rules) ─────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.tax_class.view', 'View tax classes', '/ecommerce/tax-classes/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.tax_class.create', 'Create tax class', '/ecommerce/tax-classes/save', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.tax_class.edit', 'Edit tax class', '/ecommerce/tax-classes/save', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.tax_class.delete', 'Delete tax class', '/ecommerce/tax-classes/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.tax_class.toggle', 'Toggle tax class status', '/ecommerce/tax-classes/toggle/**', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Return (REQUESTED → APPROVED → RECEIVED → REFUNDED → COMPLETED) ─────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.return.view', 'View return requests', '/ecommerce/returns/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.return.create', 'Create return request', '/ecommerce/returns/save', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.return.status', 'Update return status', '/ecommerce/returns/status/**', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.return.delete', 'Delete return request', '/ecommerce/returns/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Refund (linked to approved/received returns) ─────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.refund.view', 'View refund disbursements', '/ecommerce/refunds/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.refund.create', 'Issue refund', '/ecommerce/refunds/save', 'POST', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.refund.delete', 'Delete refund record', '/ecommerce/refunds/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Payment (gateway transaction log) ─────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.payment.view', 'View payment transactions', '/ecommerce/payments/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.payment.status', 'Update payment status', '/ecommerce/payments/status/**', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.payment.delete', 'Delete payment record', '/ecommerce/payments/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Cart (abandoned cart monitoring) ──────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.cart.view', 'View customer carts', '/ecommerce/carts/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.cart.abandon', 'Mark cart as abandoned', '/ecommerce/carts/abandon/**', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.cart.delete', 'Delete cart record', '/ecommerce/carts/delete/**', 'DELETE', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Settings (key-value store per org) ────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('ec.setting.view', 'View eCommerce settings', '/ecommerce/settings/**', 'GET', 'ECOMMERCE', 'ECOMMERCE',
        true, NOW(), NOW()),
       ('ec.setting.create', 'Create/update eCommerce setting', '/ecommerce/settings/save', 'POST', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW()),
       ('ec.setting.delete', 'Delete eCommerce setting', '/ecommerce/settings/delete/**', 'DELETE', 'ECOMMERCE',
        'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 2. ROLES
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO sec_roles (name, name_bn, description, master_role, active, created_at, updated_at)
VALUES ('ROLE_ECOMMERCE_MANAGER', 'ই-কমার্স ম্যানেজার',
        'Full storefront management: catalog, orders, customers, coupons, returns/refunds, settings',
        'ROLE_ECOMMERCE_MANAGER', true, NOW(), NOW()),
       ('ROLE_ECOMMERCE_EXECUTIVE', 'ই-কমার্স নির্বাহী',
        'Day-to-day storefront operations: order fulfillment, review moderation, customer support',
        'ROLE_ECOMMERCE_EXECUTIVE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 3. APP_MENUS  (MODULE → GROUP → LEAF)
-- ═════════════════════════════════════════════════════════════════════════════

-- ── 3A. MODULE level ─────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
VALUES ('MOD_ECOMMERCE', 'eCommerce', NULL, 'fa fa-store', NULL, 95, 'MODULE', 'ECOMMERCE', 'ec.product.view',
        '_self', true, true, false, NOW(), NOW())
ON CONFLICT (menu_code) DO NOTHING;

-- ── 3B. GROUP level ──────────────────────────────────────────────────────────

-- Catalog & Navigation
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_CATALOG', 'Catalog', NULL, 'fa fa-tags', m.id, 10, 'GROUP', 'ECOMMERCE', NULL, '_self', true, true,
       false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE'
ON CONFLICT (menu_code) DO NOTHING;

-- Sales & Orders
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_SALES', 'Sales & Orders', NULL, 'fa fa-receipt', m.id, 20, 'GROUP', 'ECOMMERCE', NULL, '_self', true,
       true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE'
ON CONFLICT (menu_code) DO NOTHING;

-- Customers
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_CUSTOMERS', 'Customers', NULL, 'fa fa-users', m.id, 30, 'GROUP', 'ECOMMERCE', NULL, '_self', true,
       true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE'
ON CONFLICT (menu_code) DO NOTHING;

-- Marketing
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_MARKETING', 'Marketing', NULL, 'fa fa-bullhorn', m.id, 40, 'GROUP', 'ECOMMERCE', NULL, '_self', true,
       true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE'
ON CONFLICT (menu_code) DO NOTHING;

-- Returns & Payments
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_RETURNS', 'Returns & Payments', NULL, 'fa fa-undo', m.id, 50, 'GROUP', 'ECOMMERCE', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE'
ON CONFLICT (menu_code) DO NOTHING;

-- Configuration
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_CONFIG', 'Configuration', NULL, 'fa fa-sliders-h', m.id, 60, 'GROUP', 'ECOMMERCE', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE'
ON CONFLICT (menu_code) DO NOTHING;

-- ── 3C. LEAF level ───────────────────────────────────────────────────────────

-- ── Catalog group (dashboard-first, matching v4.0 convention) ────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_DASHBOARD', 'Dashboard', '/ecommerce/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'ECOMMERCE',
       'ec.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CATALOG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_PRODUCT', 'Products', '/ecommerce/products', 'fa fa-shirt', g.id, 10, 'LEAF', 'ECOMMERCE',
       'ec.product.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CATALOG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_CATEGORY', 'Categories', '/ecommerce/categories', 'fa fa-sitemap', g.id, 20, 'LEAF', 'ECOMMERCE',
       'ec.category.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CATALOG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_HOME_SECTION', 'Home Sections (CMS)', '/ecommerce/home-sections', 'fa fa-th-large', g.id, 30, 'LEAF',
       'ECOMMERCE', 'ec.home_section.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CATALOG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_REVIEW', 'Reviews', '/ecommerce/reviews', 'fa fa-star', g.id, 40, 'LEAF', 'ECOMMERCE', 'ec.review.view',
       '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CATALOG'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Sales & Orders group ─────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_ORDER', 'Orders', '/ecommerce/orders', 'fa fa-receipt', g.id, 10, 'LEAF', 'ECOMMERCE', 'ec.order.view',
       '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_SALES'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_CART', 'Carts', '/ecommerce/carts', 'fa fa-shopping-cart', g.id, 20, 'LEAF', 'ECOMMERCE', 'ec.cart.view',
       '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_SALES'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Customers group ──────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_CUSTOMER', 'Customers', '/ecommerce/customers', 'fa fa-user', g.id, 10, 'LEAF', 'ECOMMERCE',
       'ec.customer.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CUSTOMERS'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Marketing group ──────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_COUPON', 'Coupons', '/ecommerce/coupons', 'fa fa-tag', g.id, 10, 'LEAF', 'ECOMMERCE', 'ec.coupon.view',
       '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_MARKETING'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Returns & Payments group ─────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_RETURN', 'Returns', '/ecommerce/returns', 'fa fa-rotate-left', g.id, 10, 'LEAF', 'ECOMMERCE',
       'ec.return.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_RETURNS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_REFUND', 'Refunds', '/ecommerce/refunds', 'fa fa-money-bill-wave', g.id, 20, 'LEAF', 'ECOMMERCE',
       'ec.refund.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_RETURNS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_PAYMENT', 'Payments', '/ecommerce/payments', 'fa fa-credit-card', g.id, 30, 'LEAF', 'ECOMMERCE',
       'ec.payment.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_RETURNS'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Configuration group ──────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_SHIPPING_METHOD', 'Shipping Methods', '/ecommerce/shipping-methods', 'fa fa-truck', g.id, 10, 'LEAF',
       'ECOMMERCE', 'ec.shipping_method.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_TAX_CLASS', 'Tax Classes', '/ecommerce/tax-classes', 'fa fa-percent', g.id, 20, 'LEAF', 'ECOMMERCE',
       'ec.tax_class.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_GL_DEFAULTS', 'GL Account Defaults', '/ecommerce/gl-defaults', 'fa fa-book', g.id, 30, 'LEAF',
       'ECOMMERCE', 'ec.gl_defaults.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'EC_SETTING', 'General Settings', '/ecommerce/settings', 'fa fa-cog', g.id, 40, 'LEAF', 'ECOMMERCE',
       'ec.setting.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_EC_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 4. ROLE PERMISSIONS  (sec_role_permissions)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_ECOMMERCE_MANAGER — full storefront management, every action
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'ec.dashboard.view', 'ec.dashboard.summary',
                                              'ec.product.view', 'ec.product.create', 'ec.product.edit',
                                              'ec.product.delete', 'ec.product.toggle',
                                              'ec.category.view', 'ec.category.create', 'ec.category.edit',
                                              'ec.category.delete', 'ec.category.toggle',
                                              'ec.customer.view', 'ec.customer.create', 'ec.customer.edit',
                                              'ec.customer.delete', 'ec.customer.toggle',
                                              'ec.order.view', 'ec.order.status', 'ec.order.delete',
                                              'ec.coupon.view', 'ec.coupon.create', 'ec.coupon.edit',
                                              'ec.coupon.delete', 'ec.coupon.toggle',
                                              'ec.shipping_method.view', 'ec.shipping_method.create',
                                              'ec.shipping_method.edit', 'ec.shipping_method.delete',
                                              'ec.shipping_method.toggle',
                                              'ec.review.view', 'ec.review.approve', 'ec.review.reject',
                                              'ec.review.hide', 'ec.review.delete',
                                              'ec.home_section.view', 'ec.home_section.create',
                                              'ec.home_section.edit', 'ec.home_section.delete',
                                              'ec.home_section.toggle',
                                              'ec.gl_defaults.view', 'ec.gl_defaults.edit',
                                              'ec.tax_class.view', 'ec.tax_class.create', 'ec.tax_class.edit',
                                              'ec.tax_class.delete', 'ec.tax_class.toggle',
                                              'ec.return.view', 'ec.return.create', 'ec.return.status',
                                              'ec.return.delete',
                                              'ec.refund.view', 'ec.refund.create', 'ec.refund.delete',
                                              'ec.payment.view', 'ec.payment.status', 'ec.payment.delete',
                                              'ec.cart.view', 'ec.cart.abandon', 'ec.cart.delete',
                                              'ec.setting.view', 'ec.setting.create', 'ec.setting.delete',
                                              'acc.coa.view', 'acc.sub.view', 'acc.bank_acc.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_ECOMMERCE_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_ECOMMERCE_EXECUTIVE — day-to-day ops: fulfillment, moderation, support
-- (view everywhere; create/edit only where front-line staff act directly;
--  no delete, no GL/tax/shipping-method config, no settings)
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view',
                                              'ec.dashboard.view', 'ec.dashboard.summary',
                                              'ec.product.view',
                                              'ec.category.view',
                                              'ec.customer.view', 'ec.customer.edit',
                                              'ec.order.view', 'ec.order.status',
                                              'ec.coupon.view',
                                              'ec.shipping_method.view',
                                              'ec.review.view', 'ec.review.approve', 'ec.review.reject',
                                              'ec.review.hide',
                                              'ec.home_section.view',
                                              'ec.tax_class.view',
                                              'ec.return.view', 'ec.return.create', 'ec.return.status',
                                              'ec.refund.view',
                                              'ec.payment.view', 'ec.payment.status',
                                              'ec.cart.view', 'ec.cart.abandon',
                                              'apr.request.view', 'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_ECOMMERCE_EXECUTIVE'
ON CONFLICT DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 5. ROLE MENUS  (sec_mrole_menus)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_ECOMMERCE_MANAGER — full create/edit/delete on every leaf
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('EC_PRODUCT', 'EC_CATEGORY', 'EC_HOME_SECTION', 'EC_COUPON', 'EC_SHIPPING_METHOD',
                       'EC_TAX_CLASS', 'EC_RETURN', 'EC_REFUND', 'EC_CUSTOMER', 'EC_SETTING'),
       m.menu_code IN ('EC_PRODUCT', 'EC_CATEGORY', 'EC_HOME_SECTION', 'EC_COUPON', 'EC_SHIPPING_METHOD',
                       'EC_TAX_CLASS', 'EC_CUSTOMER', 'EC_GL_DEFAULTS', 'EC_ORDER', 'EC_PAYMENT', 'EC_RETURN',
                       'EC_REVIEW', 'EC_CART'),
       m.menu_code IN ('EC_PRODUCT', 'EC_CATEGORY', 'EC_HOME_SECTION', 'EC_COUPON', 'EC_SHIPPING_METHOD',
                       'EC_TAX_CLASS', 'EC_CUSTOMER', 'EC_REVIEW', 'EC_RETURN', 'EC_REFUND', 'EC_PAYMENT',
                       'EC_CART', 'EC_SETTING'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_ECOMMERCE_MANAGER'
  AND m.menu_code IN ('MOD_ECOMMERCE',
                      'GRP_EC_CATALOG', 'GRP_EC_SALES', 'GRP_EC_CUSTOMERS', 'GRP_EC_MARKETING',
                      'GRP_EC_RETURNS', 'GRP_EC_CONFIG',
                      'EC_DASHBOARD', 'EC_PRODUCT', 'EC_CATEGORY', 'EC_HOME_SECTION', 'EC_REVIEW',
                      'EC_ORDER', 'EC_CART', 'EC_CUSTOMER', 'EC_COUPON',
                      'EC_RETURN', 'EC_REFUND', 'EC_PAYMENT',
                      'EC_SHIPPING_METHOD', 'EC_TAX_CLASS', 'EC_GL_DEFAULTS', 'EC_SETTING')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_ECOMMERCE_EXECUTIVE — view + limited create/edit, no delete, no config menus
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('EC_RETURN'),
       m.menu_code IN ('EC_CUSTOMER', 'EC_ORDER', 'EC_REVIEW', 'EC_RETURN', 'EC_PAYMENT', 'EC_CART'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_ECOMMERCE_EXECUTIVE'
  AND m.menu_code IN ('MOD_ECOMMERCE',
                      'GRP_EC_CATALOG', 'GRP_EC_SALES', 'GRP_EC_CUSTOMERS', 'GRP_EC_MARKETING', 'GRP_EC_RETURNS',
                      'EC_DASHBOARD', 'EC_PRODUCT', 'EC_CATEGORY', 'EC_REVIEW',
                      'EC_ORDER', 'EC_CART', 'EC_CUSTOMER', 'EC_COUPON',
                      'EC_RETURN', 'EC_REFUND', 'EC_PAYMENT')
ON CONFLICT (role_id, menu_id) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- VERIFICATION (uncomment to run counts after execution)
-- ═════════════════════════════════════════════════════════════════════════════
-- SELECT 'EC Permissions' AS table_name, COUNT(*) AS total FROM sec_permissions WHERE name LIKE 'ec.%'
-- UNION ALL SELECT 'EC Roles',      COUNT(*) FROM sec_roles WHERE name LIKE 'ROLE_ECOMMERCE%'
-- UNION ALL SELECT 'EC Menus',      COUNT(*) FROM app_menus WHERE module_name = 'ECOMMERCE'
-- UNION ALL SELECT 'EC Role-Perms', COUNT(*) FROM sec_role_permissions rp
--             JOIN sec_roles r ON r.id = rp.role_id WHERE r.name LIKE 'ROLE_ECOMMERCE%'
-- UNION ALL SELECT 'EC Role-Menus', COUNT(*) FROM sec_mrole_menus rm
--             JOIN sec_roles r ON r.id = rm.role_id WHERE r.name LIKE 'ROLE_ECOMMERCE%';

COMMIT;
