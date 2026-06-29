-- =============================================================================
-- SPINDLE ERP — ENTERPRISE ECOMMERCE MODULE
-- FILE 12 : PERMISSIONS & MENU SEED  (Flyway migration — idempotent)
-- PostgreSQL 16+
--
-- Permission naming convention: ec.<entity>.<action>  (dot-notation)
-- Module:   ECOMMERCE
-- Category: varies per section
--
-- Menus hook into app_menus (existing ERP navigation tree).
-- Place ecommerce menus under a top-level "E-Commerce" parent.
-- =============================================================================

-- =============================================================================
-- SECTION 1 : PERMISSIONS
-- =============================================================================

INSERT INTO sec_permissions (name, description, module, category, active, created_at)
VALUES

-- ── Dashboard ──────────────────────────────────────────────────────────────────
('ec.dashboard.view',           'View ecommerce dashboard',                  'ECOMMERCE', 'Dashboard',    TRUE, NOW()),

-- ── Product Catalog ───────────────────────────────────────────────────────────
('ec.product.view',             'View product catalog',                      'ECOMMERCE', 'Products',     TRUE, NOW()),
('ec.product.create',           'Create new product',                        'ECOMMERCE', 'Products',     TRUE, NOW()),
('ec.product.edit',             'Edit product details',                      'ECOMMERCE', 'Products',     TRUE, NOW()),
('ec.product.delete',           'Delete/deactivate product',                 'ECOMMERCE', 'Products',     TRUE, NOW()),
('ec.product.publish',          'Publish / unpublish product',               'ECOMMERCE', 'Products',     TRUE, NOW()),
('ec.product.image.manage',     'Manage product images',                     'ECOMMERCE', 'Products',     TRUE, NOW()),
('ec.product.variant.manage',   'Manage product variants',                   'ECOMMERCE', 'Products',     TRUE, NOW()),

-- ── Categories ────────────────────────────────────────────────────────────────
('ec.category.view',            'View product categories',                   'ECOMMERCE', 'Categories',   TRUE, NOW()),
('ec.category.create',          'Create category',                           'ECOMMERCE', 'Categories',   TRUE, NOW()),
('ec.category.edit',            'Edit category',                             'ECOMMERCE', 'Categories',   TRUE, NOW()),
('ec.category.delete',          'Delete category',                           'ECOMMERCE', 'Categories',   TRUE, NOW()),

-- ── Orders ────────────────────────────────────────────────────────────────────
('ec.order.view',               'View all orders',                           'ECOMMERCE', 'Orders',       TRUE, NOW()),
('ec.order.create',             'Create manual order',                       'ECOMMERCE', 'Orders',       TRUE, NOW()),
('ec.order.edit',               'Edit order details',                        'ECOMMERCE', 'Orders',       TRUE, NOW()),
('ec.order.confirm',            'Confirm pending order',                     'ECOMMERCE', 'Orders',       TRUE, NOW()),
('ec.order.cancel',             'Cancel order',                              'ECOMMERCE', 'Orders',       TRUE, NOW()),
('ec.order.export',             'Export order data',                         'ECOMMERCE', 'Orders',       TRUE, NOW()),
('ec.order.note.add',           'Add order notes',                           'ECOMMERCE', 'Orders',       TRUE, NOW()),

-- ── Returns & Refunds ─────────────────────────────────────────────────────────
('ec.return.view',              'View return requests',                      'ECOMMERCE', 'Returns',      TRUE, NOW()),
('ec.return.approve',           'Approve return request',                    'ECOMMERCE', 'Returns',      TRUE, NOW()),
('ec.return.reject',            'Reject return request',                     'ECOMMERCE', 'Returns',      TRUE, NOW()),
('ec.refund.process',           'Process refund',                            'ECOMMERCE', 'Returns',      TRUE, NOW()),

-- ── Payments ──────────────────────────────────────────────────────────────────
('ec.payment.view',             'View payment records',                      'ECOMMERCE', 'Payments',     TRUE, NOW()),
('ec.payment.reconcile',        'Reconcile payments with GL',                'ECOMMERCE', 'Payments',     TRUE, NOW()),

-- ── Shipping ──────────────────────────────────────────────────────────────────
('ec.shipping.view',            'View shipments',                            'ECOMMERCE', 'Shipping',     TRUE, NOW()),
('ec.shipping.update',          'Update shipping status / tracking',         'ECOMMERCE', 'Shipping',     TRUE, NOW()),
('ec.shipping.method.manage',   'Manage shipping methods and zones',         'ECOMMERCE', 'Shipping',     TRUE, NOW()),

-- ── Customers ─────────────────────────────────────────────────────────────────
('ec.customer.view',            'View customer list',                        'ECOMMERCE', 'Customers',    TRUE, NOW()),
('ec.customer.create',          'Create customer account',                   'ECOMMERCE', 'Customers',    TRUE, NOW()),
('ec.customer.edit',            'Edit customer profile',                     'ECOMMERCE', 'Customers',    TRUE, NOW()),
('ec.customer.block',           'Block / unblock customer',                  'ECOMMERCE', 'Customers',    TRUE, NOW()),
('ec.customer.wallet.manage',   'Manage customer wallet',                    'ECOMMERCE', 'Customers',    TRUE, NOW()),
('ec.customer.reward.manage',   'Manage reward points',                      'ECOMMERCE', 'Customers',    TRUE, NOW()),

-- ── Reviews & Q&A ─────────────────────────────────────────────────────────────
('ec.review.view',              'View product reviews',                      'ECOMMERCE', 'Reviews',      TRUE, NOW()),
('ec.review.approve',           'Approve / reject reviews',                  'ECOMMERCE', 'Reviews',      TRUE, NOW()),
('ec.review.delete',            'Delete review',                             'ECOMMERCE', 'Reviews',      TRUE, NOW()),
('ec.qa.view',                  'View product Q&A',                          'ECOMMERCE', 'Reviews',      TRUE, NOW()),
('ec.qa.answer',                'Post official answers',                     'ECOMMERCE', 'Reviews',      TRUE, NOW()),

-- ── Promotions ────────────────────────────────────────────────────────────────
('ec.coupon.view',              'View coupons',                              'ECOMMERCE', 'Promotions',   TRUE, NOW()),
('ec.coupon.create',            'Create coupon',                             'ECOMMERCE', 'Promotions',   TRUE, NOW()),
('ec.coupon.edit',              'Edit coupon',                               'ECOMMERCE', 'Promotions',   TRUE, NOW()),
('ec.coupon.delete',            'Delete coupon',                             'ECOMMERCE', 'Promotions',   TRUE, NOW()),
('ec.campaign.view',            'View promotion campaigns',                  'ECOMMERCE', 'Promotions',   TRUE, NOW()),
('ec.campaign.create',          'Create campaign',                           'ECOMMERCE', 'Promotions',   TRUE, NOW()),
('ec.campaign.edit',            'Edit campaign',                             'ECOMMERCE', 'Promotions',   TRUE, NOW()),

-- ── CMS ───────────────────────────────────────────────────────────────────────
('ec.banner.view',              'View banners',                              'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.banner.manage',            'Create/edit/delete banners',                'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.blog.view',                'View blog posts',                           'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.blog.create',              'Create blog post',                          'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.blog.edit',                'Edit blog post',                            'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.blog.delete',              'Delete blog post',                          'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.blog.publish',             'Publish / unpublish blog',                  'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.page.manage',              'Manage static pages',                       'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.media.upload',             'Upload media to library',                   'ECOMMERCE', 'CMS',          TRUE, NOW()),
('ec.media.delete',             'Delete media from library',                 'ECOMMERCE', 'CMS',          TRUE, NOW()),

-- ── Marketing ─────────────────────────────────────────────────────────────────
('ec.newsletter.view',          'View newsletter subscribers',               'ECOMMERCE', 'Marketing',    TRUE, NOW()),
('ec.newsletter.export',        'Export subscriber list',                    'ECOMMERCE', 'Marketing',    TRUE, NOW()),
('ec.email.campaign.manage',    'Manage email campaigns',                    'ECOMMERCE', 'Marketing',    TRUE, NOW()),
('ec.push.notification.send',   'Send push notifications',                   'ECOMMERCE', 'Marketing',    TRUE, NOW()),
('ec.loyalty.manage',           'Manage loyalty program',                    'ECOMMERCE', 'Marketing',    TRUE, NOW()),
('ec.gift.card.manage',         'Manage gift cards',                         'ECOMMERCE', 'Marketing',    TRUE, NOW()),
('ec.affiliate.manage',         'Manage affiliate program',                  'ECOMMERCE', 'Marketing',    TRUE, NOW()),

-- ── Analytics ─────────────────────────────────────────────────────────────────
('ec.analytics.view',           'View ecommerce analytics',                  'ECOMMERCE', 'Analytics',    TRUE, NOW()),
('ec.analytics.export',         'Export analytics reports',                  'ECOMMERCE', 'Analytics',    TRUE, NOW()),
('ec.seo.manage',               'Manage SEO metadata and redirects',         'ECOMMERCE', 'Analytics',    TRUE, NOW()),

-- ── Settings ──────────────────────────────────────────────────────────────────
('ec.settings.view',            'View ecommerce settings',                   'ECOMMERCE', 'Settings',     TRUE, NOW()),
('ec.settings.edit',            'Edit ecommerce settings',                   'ECOMMERCE', 'Settings',     TRUE, NOW()),
('ec.tax.manage',               'Manage tax classes and rules',              'ECOMMERCE', 'Settings',     TRUE, NOW()),
('ec.shipping.zone.manage',     'Manage shipping zones',                     'ECOMMERCE', 'Settings',     TRUE, NOW()),
('ec.api.config.manage',        'Manage payment/courier API configs',        'ECOMMERCE', 'Settings',     TRUE, NOW()),
('ec.gl.defaults.manage',       'Manage ecommerce GL account defaults',      'ECOMMERCE', 'Settings',     TRUE, NOW()),
('ec.feature.flag.manage',      'Manage feature flags',                      'ECOMMERCE', 'Settings',     TRUE, NOW())

ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- SECTION 2 : APP MENUS
-- Parent: E-Commerce section (display_order 90 — after existing ERP modules)
-- =============================================================================

-- ── Top-level E-Commerce parent menu ──────────────────────────────────────────
INSERT INTO app_menus
    (menu_code, menu_name, menu_type, menu_url, icon, module_name,
     display_order, parent_id, required_permission, target,
     active, visible, deleted, created_at)
VALUES
    ('EC', 'E-Commerce', 'MODULE', NULL, 'fa fa-shopping-cart', 'ECOMMERCE',
     90, NULL, 'ec.dashboard.view', '_self',
     TRUE, TRUE, FALSE, NOW())
ON CONFLICT (menu_code) DO NOTHING;

-- ── Dashboard ─────────────────────────────────────────────────────────────────
INSERT INTO app_menus
    (menu_code, menu_name, menu_type, menu_url, icon, module_name,
     display_order, parent_id, required_permission, target,
     active, visible, deleted, created_at)
SELECT 'EC_DASH', 'EC Dashboard', 'MENU', '/ec/dashboard', 'fa fa-tachometer', 'ECOMMERCE',
       1, id, 'ec.dashboard.view', '_self', TRUE, TRUE, FALSE, NOW()
FROM app_menus WHERE menu_code = 'EC'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Products ──────────────────────────────────────────────────────────────────
INSERT INTO app_menus
    (menu_code, menu_name, menu_type, menu_url, icon, module_name,
     display_order, parent_id, required_permission, target,
     active, visible, deleted, created_at)
SELECT 'EC_PRODUCT', 'Products', 'GROUP', NULL, 'fa fa-tag', 'ECOMMERCE',
       2, id, 'ec.product.view', '_self', TRUE, TRUE, FALSE, NOW()
FROM app_menus WHERE menu_code = 'EC'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
    (menu_code, menu_name, menu_type, menu_url, icon, module_name,
     display_order, parent_id, required_permission, target,
     active, visible, deleted, created_at)
SELECT 'EC_PRODUCT_LIST',     'Product Catalog',   'MENU', '/ec/products',   'fa fa-list',    'ECOMMERCE', 1, id, 'ec.product.view',    '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_PRODUCT' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
    (menu_code, menu_name, menu_type, menu_url, icon, module_name,
     display_order, parent_id, required_permission, target,
     active, visible, deleted, created_at)
SELECT 'EC_CATEGORY_LIST',    'Categories',        'MENU', '/ec/categories', 'fa fa-sitemap', 'ECOMMERCE', 2, id, 'ec.category.view',   '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_PRODUCT' ON CONFLICT (menu_code) DO NOTHING;

-- ── Orders ────────────────────────────────────────────────────────────────────
INSERT INTO app_menus
    (menu_code, menu_name, menu_type, menu_url, icon, module_name,
     display_order, parent_id, required_permission, target,
     active, visible, deleted, created_at)
SELECT 'EC_ORDERS', 'Orders', 'GROUP', NULL, 'fa fa-file-text', 'ECOMMERCE',
       3, id, 'ec.order.view', '_self', TRUE, TRUE, FALSE, NOW()
FROM app_menus WHERE menu_code = 'EC'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_ORDER_LIST',    'All Orders',       'MENU', '/ec/orders',         'fa fa-shopping-bag',  'ECOMMERCE', 1, id, 'ec.order.view',   '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_ORDERS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_RETURN_LIST',   'Returns & Refunds','MENU', '/ec/returns',        'fa fa-undo',          'ECOMMERCE', 2, id, 'ec.return.view',  '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_ORDERS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_PAYMENT_LIST',  'Payments',         'MENU', '/ec/payments',       'fa fa-credit-card',   'ECOMMERCE', 3, id, 'ec.payment.view', '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_ORDERS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_SHIPPING_LIST', 'Shipments',        'MENU', '/ec/shipments',      'fa fa-truck',         'ECOMMERCE', 4, id, 'ec.shipping.view','_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_ORDERS' ON CONFLICT (menu_code) DO NOTHING;

-- ── Customers ─────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_CUSTOMERS', 'Customers', 'MENU', '/ec/customers', 'fa fa-users', 'ECOMMERCE', 4, id, 'ec.customer.view', '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC' ON CONFLICT (menu_code) DO NOTHING;

-- ── Promotions ────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_PROMOS', 'Promotions', 'GROUP', NULL, 'fa fa-percent', 'ECOMMERCE', 5, id, 'ec.coupon.view', '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_COUPON_LIST',   'Coupons',   'MENU', '/ec/coupons',   'fa fa-ticket', 'ECOMMERCE', 1, id, 'ec.coupon.view',   '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_PROMOS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_CAMPAIGN_LIST', 'Campaigns', 'MENU', '/ec/campaigns', 'fa fa-bullhorn','ECOMMERCE', 2, id, 'ec.campaign.view', '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_PROMOS' ON CONFLICT (menu_code) DO NOTHING;

-- ── CMS ───────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_CMS', 'Content', 'GROUP', NULL, 'fa fa-pencil-square', 'ECOMMERCE', 6, id, 'ec.banner.view', '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_BANNER_LIST', 'Banners',    'MENU', '/ec/banners', 'fa fa-image',  'ECOMMERCE', 1, id, 'ec.banner.view',  '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_CMS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_BLOG_LIST',   'Blog Posts', 'MENU', '/ec/blog',    'fa fa-rss',    'ECOMMERCE', 2, id, 'ec.blog.view',    '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_CMS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_MEDIA_LIB',   'Media',      'MENU', '/ec/media',   'fa fa-folder-open','ECOMMERCE', 3, id, 'ec.media.upload', '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC_CMS' ON CONFLICT (menu_code) DO NOTHING;

-- ── Analytics ─────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_ANALYTICS', 'Analytics', 'MENU', '/ec/analytics', 'fa fa-bar-chart', 'ECOMMERCE', 7, id, 'ec.analytics.view', '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC' ON CONFLICT (menu_code) DO NOTHING;

-- ── Settings ──────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_type, menu_url, icon, module_name, display_order, parent_id, required_permission, target, active, visible, deleted, created_at) SELECT 'EC_SETTINGS', 'EC Settings', 'MENU', '/ec/settings', 'fa fa-cogs', 'ECOMMERCE', 8, id, 'ec.settings.view', '_self', TRUE, TRUE, FALSE, NOW() FROM app_menus WHERE menu_code = 'EC' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- SECTION 3 : REGISTER ECOMMERCE MODULE KEY IN sec_org_modules
-- Each org must explicitly have ECOMMERCE enabled by a super-admin.
-- DynamicAuthorizationManager checks this before allowing access.
-- =============================================================================

INSERT INTO sec_org_modules (organization_id, module_key, active, granted_by, granted_at, notes)
SELECT id, 'ECOMMERCE', FALSE, 'SYSTEM', NOW(),
       'Ecommerce module — disabled by default, enable per org via super-admin'
FROM org_organizations
ON CONFLICT (organization_id, module_key) DO NOTHING;
