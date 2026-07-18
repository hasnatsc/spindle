-- =============================================================================
--  SPINDLE ERP  —  Complete Access Control Seed  v5.0
--  File   : V00__seed_access_control_complete.sql
--  Target : PostgreSQL 15+
--
--  CONSOLIDATED from:
--    • 00_seed_menu_permission_complete.sql   (core modules v4.0)
--    • 01_seed_ecommerce_menu_permission.sql  (eCommerce module)
--    • V101__seed_travel_menu_permission.sql  (Travel Phase 1)
--    • V103__seed_travel_phase2_menu_permission.sql (Travel Phase 2)
--    • V10__org_module_access.sql             (Org module licensing)
--
--  ORGANIZATION:
--    SECTION 1 — PERMISSIONS    (grouped by module)
--    SECTION 2 — ROLES          (all roles in one block)
--    SECTION 3 — APP MENUS      (hierarchical: MODULE → GROUP → LEAF)
--    SECTION 4 — ROLE-PERMISSIONS (role → permission grants)
--    SECTION 5 — ROLE-MENUS     (role → menu CRUD access)
--
--  Each INSERT uses ON CONFLICT DO NOTHING — safe to re-run.
--  Wrapped in a single BEGIN/COMMIT transaction.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- SECTION 1 — PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

-- ── 1.01  Super Admin Wildcard ───────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES ('*', 'Super admin wildcard — all access', '/**', NULL, 'CORE_SECURITY', 'SYSTEM', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.02  Dashboard / Main ───────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('dashboard.view',                'View ERP main dashboard',               '/dashboard',                          'GET',  'REPORTS_ANALYTICS',      'DASHBOARD', true, NOW(), NOW()),
    ('dashboard.summary',             'ERP dashboard summary JSON',             '/dashboard/erp-summary',              'GET',  'REPORTS_ANALYTICS',      'DASHBOARD', true, NOW(), NOW()),
    ('security.dashboard.view',       'View security & IAM dashboard',          '/security/dashboard',                 'GET',  'CORE_SECURITY',          'DASHBOARD', true, NOW(), NOW()),
    ('security.dashboard.summary',    'Security dashboard summary JSON',        '/security/dashboard/summary',         'GET',  'CORE_SECURITY',          'DASHBOARD', true, NOW(), NOW()),
    ('inv.dashboard.view',            'View inventory dashboard',              '/inventory/dashboard',                'GET',  'INVENTORY_WAREHOUSE',    'DASHBOARD', true, NOW(), NOW()),
    ('inv.dashboard.summary',         'Inventory dashboard summary JSON',      '/inventory/dashboard/summary',        'GET',  'INVENTORY_WAREHOUSE',    'DASHBOARD', true, NOW(), NOW()),
    ('pur.dashboard.view',            'View purchase dashboard',               '/purchase/dashboard',                 'GET',  'PURCHASE_SUPPLIER',      'DASHBOARD', true, NOW(), NOW()),
    ('pur.dashboard.summary',         'Purchase dashboard summary JSON',       '/purchase/dashboard/summary',         'GET',  'PURCHASE_SUPPLIER',      'DASHBOARD', true, NOW(), NOW()),
    ('sal.dashboard.view',            'View sales dashboard',                  '/sales/dashboard',                    'GET',  'SALES_CUSTOMER_OPERATIONS', 'DASHBOARD', true, NOW(), NOW()),
    ('sal.dashboard.summary',         'Sales dashboard summary JSON',          '/sales/dashboard/summary',            'GET',  'SALES_CUSTOMER_OPERATIONS', 'DASHBOARD', true, NOW(), NOW()),
    ('acc.dashboard.view',            'View accounts module dashboard',        '/accounts/dashboard',                 'GET',  'FINANCE_ACCOUNTS',       'DASHBOARD', true, NOW(), NOW()),
    ('acc.dashboard.summary',         'Accounts dashboard summary JSON',       '/accounts/dashboard/summary',         'GET',  'FINANCE_ACCOUNTS',       'DASHBOARD', true, NOW(), NOW()),
    ('hrm.dashboard.view',            'View HRM dashboard',                    '/hrm/dashboard',                      'GET',  'HRM',                    'DASHBOARD', true, NOW(), NOW()),
    ('hrm.dashboard.summary',         'HRM dashboard summary JSON',            '/hrm/dashboard/full-summary',         'GET',  'HRM',                    'DASHBOARD', true, NOW(), NOW()),
    ('prd.dashboard.view',            'View production dashboard',             '/production/dashboard',               'GET',  'PRODUCTION',             'DASHBOARD', true, NOW(), NOW()),
    ('prd.dashboard.summary',         'Production dashboard summary JSON',     '/production/dashboard/summary',       'GET',  'PRODUCTION',             'DASHBOARD', true, NOW(), NOW()),
    ('com.dashboard.view',            'View commercial dashboard',             '/commercial/dashboard',               'GET',  'COMMERCIAL',             'DASHBOARD', true, NOW(), NOW()),
    ('com.dashboard.summary',         'Commercial dashboard summary JSON',     '/commercial/dashboard/summary',       'GET',  'COMMERCIAL',             'DASHBOARD', true, NOW(), NOW()),
    ('crm.dashboard.view',            'View CRM dashboard',                    '/crm/dashboard',                      'GET',  'CRM',                    'DASHBOARD', true, NOW(), NOW()),
    ('crm.dashboard.summary',         'CRM dashboard summary JSON',            '/crm/dashboard/summary',              'GET',  'CRM',                    'DASHBOARD', true, NOW(), NOW()),
    ('budget.dashboard.view',         'View budget dashboard',                 '/budget/dashboard',                   'GET',  'BUDGET',                 'DASHBOARD', true, NOW(), NOW()),
    ('budget.dashboard.summary',      'Budget dashboard summary JSON',         '/budget/dashboard/summary',           'GET',  'BUDGET',                 'DASHBOARD', true, NOW(), NOW()),
    ('fa.dashboard.view',             'View fixed assets dashboard',           '/fixed-assets/dashboard',             'GET',  'FIXED_ASSETS',           'DASHBOARD', true, NOW(), NOW()),
    ('fa.dashboard.summary',          'Fixed assets dashboard summary JSON',   '/fixed-assets/dashboard/summary',     'GET',  'FIXED_ASSETS',           'DASHBOARD', true, NOW(), NOW()),
    ('apr.dashboard.view',            'View approvals dashboard',              '/approval/dashboard',                 'GET',  'CORE_SECURITY',          'DASHBOARD', true, NOW(), NOW()),
    ('apr.dashboard.summary',         'Approvals dashboard summary JSON',      '/approval/dashboard/summary',         'GET',  'CORE_SECURITY',          'DASHBOARD', true, NOW(), NOW()),
    ('reports.view',                  'View reports module',                   '/reports/**',                         'GET',  'REPORTS_ANALYTICS',      'REPORTS',   true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.03  Security / IAM ─────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('security.user.view',              'View users',                         '/users/**',                     'GET',    'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.user.create',            'Create user',                        '/users/save',                   'POST',   'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.user.edit',              'Edit user',                          '/users/save',                   'POST',   'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.user.delete',            'Delete user',                        '/users/delete/**',              'DELETE', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.user.toggle',            'Toggle user status',                 '/users/toggle/**',              'POST',   'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.role.view',              'View roles',                         '/roles/**',                     'GET',    'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.role.create',            'Create role',                        '/roles/save',                   'POST',   'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.role.delete',            'Delete role',                        '/roles/delete/**',              'DELETE', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.menu.view',              'View menus',                         '/menus/**',                     'GET',    'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.menu.create',            'Create menu',                        '/menus/save',                   'POST',   'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.menu.delete',            'Delete menu',                        '/menus/delete/**',              'DELETE', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.permission.view',        'View permissions',                   '/permissions/**',               'GET',    'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.rolemenu.manage',        'Manage role-menu access',            '/role-menus/**',                'POST',   'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.org_module.view',        'View org module access',             '/security/org-modules/**',      'GET',    'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
    ('security.org_module.manage',      'Manage org module access',           '/security/org-modules/**',      'POST',   'CORE_SECURITY', 'SECURITY', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.04  Organization / Setup ────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('org.organization.view',           'View organizations',                 '/organizations/**',             'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.organization.create',         'Create organization',                '/organizations/save',           'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.organization.edit',           'Edit organization',                  '/organizations/save',           'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.organization.delete',         'Delete organization',                '/organizations/delete/**',      'DELETE', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.business_unit.view',          'View business units',                '/business-units/**',            'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.business_unit.create',        'Create business unit',               '/business-units/save',          'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.department.view',             'View departments',                   '/departments/**',               'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.department.create',           'Create department',                  '/departments/save',             'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.department.delete',           'Delete department',                  '/departments/delete/**',        'DELETE', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.warehouse.view',              'View warehouses',                    '/warehouses/**',                'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.warehouse.create',            'Create warehouse',                   '/warehouses/save',              'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.cost_center.view',            'View cost centers',                  '/cost-centers/**',              'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('org.cost_center.create',          'Create cost center',                 '/cost-centers/save',            'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
    ('setup.bank.view',                 'View banks',                         '/setup/banks/**',               'GET',    'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.bank.create',               'Create bank',                        '/setup/banks/save',             'POST',   'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.currency.view',             'View currencies',                    '/setup/currencies/**',          'GET',    'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.currency.create',           'Create currency',                    '/setup/currencies/save',        'POST',   'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.terms.view',                'View payment terms',                 '/setup/terms/**',               'GET',    'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.terms.create',              'Create payment term',                '/setup/terms/save',             'POST',   'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.hs_code.view',              'View HS codes',                      '/setup/hs-codes/**',            'GET',    'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.hs_code.create',            'Create HS code',                     '/setup/hs-codes/save',          'POST',   'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.sequence.view',             'View document sequences',            '/setup/sequences/**',           'GET',    'CORE_SECURITY', 'SETUP',     true, NOW(), NOW()),
    ('setup.sequence.create',           'Create document sequence',           '/setup/sequences/save',         'POST',   'CORE_SECURITY', 'SETUP',     true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.05  Inventory ───────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('inv.uom.view',                    'View UOMs',                          '/inventory/uoms/**',            'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.uom.create',                  'Create UOM',                         '/inventory/uoms/save',          'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.uom.edit',                    'Edit UOM',                           '/inventory/uoms/save',          'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.uom.delete',                  'Delete UOM',                         '/inventory/uoms/delete/**',     'DELETE', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.category.view',               'View item categories',               '/inventory/categories/**',      'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.category.create',             'Create item category',               '/inventory/categories/save',    'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.category.edit',               'Edit item category',                 '/inventory/categories/save',    'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.category.delete',             'Delete item category',               '/inventory/categories/delete/**', 'DELETE','INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.brand.view',                  'View brands',                        '/inventory/brands/**',          'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.brand.create',                'Create brand',                       '/inventory/brands/save',        'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.model.view',                  'View item models',                   '/inventory/models/**',          'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.model.create',                'Create item model',                  '/inventory/models/save',        'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.item.view',                   'View items',                         '/inventory/items/**',           'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.item.create',                 'Create item',                        '/inventory/items/save',         'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.item.edit',                   'Edit item',                          '/inventory/items/save',         'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.item.delete',                 'Delete item',                        '/inventory/items/delete/**',    'DELETE', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.stock.view',                  'View stock ledger',                  '/inventory/stocks/**',          'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.adjustment.view',             'View stock adjustments',             '/inventory/adjustments/**',     'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.adjustment.create',           'Create stock adjustment',            '/inventory/adjustments/save',   'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.transfer.view',               'View stock transfers',               '/inventory/transfers/**',       'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
    ('inv.transfer.create',             'Create stock transfer',              '/inventory/transfers/save',     'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.06  Purchase ────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('pur.supplier.view',               'View suppliers',                     '/purchase/suppliers/**',        'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.supplier.create',             'Create supplier',                    '/purchase/suppliers/save',      'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.supplier.edit',               'Edit supplier',                      '/purchase/suppliers/save',      'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.supplier.delete',             'Delete supplier',                    '/purchase/suppliers/delete/**', 'DELETE', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.po.view',                     'View purchase orders',               '/purchase/orders/**',           'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.po.create',                   'Create purchase order',              '/purchase/orders/save',         'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.po.edit',                     'Edit purchase order',                '/purchase/orders/save',         'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.po.delete',                   'Delete purchase order',              '/purchase/orders/delete/**',    'DELETE', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.po.approve',                  'Approve purchase order',             '/purchase/orders/approve/**',   'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.grn.view',                    'View GRNs',                          '/purchase/grns/**',             'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.grn.create',                  'Create GRN',                         '/purchase/grns/save',           'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.grn.edit',                    'Edit GRN',                           '/purchase/grns/save',           'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.grn.delete',                  'Delete GRN',                         '/purchase/grns/delete/**',      'DELETE', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.invoice.view',                'View purchase invoices',             '/purchase/invoices/**',         'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.invoice.create',              'Create purchase invoice',            '/purchase/invoices/save',       'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.invoice.edit',                'Edit purchase invoice',              '/purchase/invoices/save',       'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.invoice.delete',              'Delete purchase invoice',            '/purchase/invoices/delete/**',  'DELETE', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.debit_note.view',             'View debit notes',                   '/purchase/debit-notes/**',      'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.debit_note.create',           'Create debit note',                  '/purchase/debit-notes/save',    'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.payment.view',                'View purchase payments',             '/purchase/payments/**',         'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
    ('pur.payment.create',              'Create purchase payment',            '/purchase/payments/save',       'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.07  Sales ───────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('sal.customer.view',               'View customers',                     '/sales/customers/**',           'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.customer.create',             'Create customer',                    '/sales/customers/save',         'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.customer.edit',               'Edit customer',                      '/sales/customers/save',         'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.customer.delete',             'Delete customer',                    '/sales/customers/delete/**',    'DELETE', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.so.view',                     'View sales orders',                  '/sales/orders/**',              'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.so.create',                   'Create sales order',                 '/sales/orders/save',            'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.so.edit',                     'Edit sales order',                   '/sales/orders/save',            'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.so.delete',                   'Delete sales order',                 '/sales/orders/delete/**',       'DELETE', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.so.approve',                  'Approve sales order',                '/sales/orders/approve/**',      'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.delivery.view',               'View delivery notes',                '/sales/deliveries/**',          'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.delivery.create',             'Create delivery note',               '/sales/deliveries/save',        'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.delivery.edit',               'Edit delivery note',                 '/sales/deliveries/save',        'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.delivery.delete',             'Delete delivery note',               '/sales/deliveries/delete/**',   'DELETE', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.invoice.view',                'View sales invoices',                '/sales/invoices/**',            'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.invoice.create',              'Create sales invoice',               '/sales/invoices/save',          'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.invoice.edit',                'Edit sales invoice',                 '/sales/invoices/save',          'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.invoice.delete',              'Delete sales invoice',               '/sales/invoices/delete/**',     'DELETE', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.credit_note.view',            'View credit notes',                  '/sales/credit-notes/**',        'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.credit_note.create',          'Create credit note',                 '/sales/credit-notes/save',      'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.receipt.view',                'View receipt vouchers',              '/sales/receipts/**',            'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
    ('sal.receipt.create',              'Create receipt voucher',             '/sales/receipts/save',          'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.08  Accounts / GL ───────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    -- Chart of Accounts
    ('acc.coa.view',                    'View chart of accounts',             '/accounts/chart-of-accounts',             'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.coa.list',                    'List COA (DataTable)',               '/accounts/chart-of-accounts/list',        'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.coa.show',                    'Show COA detail',                    '/accounts/chart-of-accounts/show/**',     'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.coa.create',                  'Create chart of account',            '/accounts/chart-of-accounts/save',        'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.coa.edit',                    'Edit chart of account',              '/accounts/chart-of-accounts/save',        'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.coa.toggle',                  'Activate/deactivate COA',            '/accounts/chart-of-accounts/toggle/**',   'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.coa.delete',                  'Delete chart of account',            '/accounts/chart-of-accounts/delete/**',   'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.coa.search',                  'Search COA (Select2)',               '/accounts/chart-of-accounts/search',      'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.coa.tree',                    'COA tree view',                      '/accounts/chart-of-accounts/tree',        'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Sub-accounts
    ('acc.sub.view',                    'View sub-accounts',                  '/accounts/sub-accounts',                  'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.sub.list',                    'List sub-accounts',                   '/accounts/sub-accounts/list',             'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.sub.show',                    'Show sub-account detail',            '/accounts/sub-accounts/show/**',           'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.sub.create',                  'Create sub-account',                 '/accounts/sub-accounts/save',             'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.sub.edit',                    'Edit sub-account',                   '/accounts/sub-accounts/save',             'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.sub.toggle',                  'Activate/deactivate sub-account',    '/accounts/sub-accounts/toggle/**',         'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.sub.delete',                  'Delete sub-account',                 '/accounts/sub-accounts/delete/**',        'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.sub.search',                  'Search sub-accounts (Select2)',      '/accounts/sub-accounts/search',            'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Bank Accounts
    ('acc.bank_acc.view',               'View bank accounts',                 '/accounts/bank-accounts/**',               'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.bank_acc.create',             'Create bank account',                '/accounts/bank-accounts/save',             'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Journal Voucher
    ('acc.jv.view',                     'View journal voucher page',          '/accounts/journals',                       'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.jv.list',                     'List journal vouchers',              '/accounts/vouchers/list',                  'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.jv.show',                     'Show journal voucher detail',        '/accounts/vouchers/show/**',               'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.jv.create',                   'Create / save journal voucher',      '/accounts/vouchers/save',                  'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.jv.post',                     'Post journal voucher',               '/accounts/vouchers/post/**',               'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.jv.reverse',                  'Reverse journal voucher',            '/accounts/vouchers/reverse/**',            'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.jv.delete',                   'Delete draft journal voucher',       '/accounts/vouchers/delete/**',             'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Payment Voucher
    ('acc.pv.view',                     'View payment voucher page',          '/accounts/payment-vouchers',               'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.pv.create',                   'Create payment voucher',             '/accounts/vouchers/save',                  'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Receipt Voucher
    ('acc.rv.view',                     'View receipt voucher page',          '/accounts/receipt-vouchers',               'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.rv.create',                   'Create receipt voucher',             '/accounts/vouchers/save',                  'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Contra Voucher
    ('acc.cv.view',                     'View contra voucher page',           '/accounts/contra-vouchers',                'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.cv.create',                   'Create contra voucher',              '/accounts/vouchers/save',                  'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Allocations
    ('acc.alloc.open_for_party',        'Get open vouchers for party',        '/accounts/vouchers/open-for-party',        'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Aging
    ('acc.aging.view',                  'View AP/AR aging report page',       '/accounts/aging',                          'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.aging.summary',               'Aging summary (DataTable)',          '/accounts/aging/summary',                  'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.aging.detail',                'Aging detail for a party',           '/accounts/aging/detail',                   'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Accounting Periods
    ('acc.period.view',                 'View accounting periods',            '/accounts/periods',                        'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.period.list',                 'List periods (DataTable)',           '/accounts/periods/list',                   'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.period.show',                 'Show period detail',                 '/accounts/periods/show/**',                'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.period.create',               'Create accounting period',           '/accounts/periods/save',                   'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.period.edit',                 'Edit accounting period',             '/accounts/periods/save',                   'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.period.toggle',               'Open/close accounting period',       '/accounts/periods/toggle/**',              'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.period.delete',               'Delete accounting period',           '/accounts/periods/delete/**',              'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Opening Balances
    ('acc.ob.view',                     'View opening balances',              '/accounts/opening-balances',               'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.ob.list',                     'List opening balances',              '/accounts/opening-balances/list',          'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.ob.create',                   'Create opening balance',             '/accounts/opening-balances/save',          'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.ob.post',                     'Post opening balance to GL',         '/accounts/opening-balances/post/**',       'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.ob.delete',                   'Delete opening balance',             '/accounts/opening-balances/delete/**',     'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Accounts Mapping
    ('acc.mapping.view',                'View accounts mapping',              '/accounts/mapping',                        'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.mapping.list',                'List mappings (DataTable)',          '/accounts/mapping/list',                   'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.mapping.create',              'Create accounts mapping',            '/accounts/mapping/save',                   'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.mapping.edit',                'Edit accounts mapping',              '/accounts/mapping/save',                   'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.mapping.toggle',              'Activate/deactivate mapping',        '/accounts/mapping/toggle/**',              'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.mapping.delete',              'Delete accounts mapping',            '/accounts/mapping/delete/**',              'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- Accounts Policy
    ('acc.policy.view',                 'View accounts policies',             '/accounts/policy',                         'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.policy.create',               'Create accounts policy',             '/accounts/policy/save',                    'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.policy.toggle',               'Activate/deactivate policy',         '/accounts/policy/toggle/**',               'POST',   'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.policy.delete',               'Delete accounts policy',             '/accounts/policy/delete/**',               'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    -- GL Reports
    ('acc.ledger.view',                 'View general ledger',                '/accounts/ledger/**',                      'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.trial_bal.view',              'View trial balance',                 '/accounts/trial-balance/**',               'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.profit_loss.view',            'View profit & loss',                 '/accounts/profit-loss/**',                 'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
    ('acc.balance_sheet.view',          'View balance sheet',                 '/accounts/balance-sheet/**',               'GET',    'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.09  HRM ─────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('hrm.designation.view',            'View designations',                  '/hrm/designations/**',            'GET',    'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.designation.create',          'Create designation',                 '/hrm/designations/save',          'POST',   'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.designation.edit',            'Edit designation',                   '/hrm/designations/save',          'POST',   'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.designation.delete',          'Delete designation',                 '/hrm/designations/delete/**',     'DELETE', 'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.employee.view',               'View employees',                     '/hrm/employees/**',               'GET',    'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.employee.create',             'Create employee',                    '/hrm/employees/save',             'POST',   'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.employee.edit',               'Edit employee',                      '/hrm/employees/save',             'POST',   'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.employee.delete',             'Delete employee',                    '/hrm/employees/delete/**',        'DELETE', 'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.attendance.view',             'View attendance',                    '/hrm/attendance/**',              'GET',    'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.attendance.create',           'Create attendance',                  '/hrm/attendance/save',            'POST',   'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.leave.view',                  'View leaves',                        '/hrm/leaves/**',                  'GET',    'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.leave.create',                'Create leave',                       '/hrm/leaves/save',                'POST',   'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.leave.approve',               'Approve leave',                      '/hrm/leaves/approve/**',          'POST',   'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.payroll.view',                'View payroll',                       '/hrm/payroll/**',                 'GET',    'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.payroll.create',              'Process payroll',                    '/hrm/payroll/save',               'POST',   'HRM', 'HRM', true, NOW(), NOW()),
    ('hrm.payroll.approve',             'Approve payroll',                    '/hrm/payroll/approve/**',         'POST',   'HRM', 'HRM', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.10  Production ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('prd.order.view',                  'View production orders',             '/production/orders/**',           'GET',    'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
    ('prd.order.create',                'Create production order',            '/production/orders/save',         'POST',   'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
    ('prd.order.edit',                  'Edit production order',              '/production/orders/save',         'POST',   'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
    ('prd.order.delete',                'Delete production order',            '/production/orders/delete/**',    'DELETE', 'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
    ('prd.order.approve',               'Approve production order',           '/production/orders/approve/**',   'POST',   'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
    ('prd.bom.view',                    'View BOMs',                          '/production/boms/**',             'GET',    'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
    ('prd.bom.create',                  'Create BOM',                         '/production/boms/save',           'POST',   'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
    ('prd.material_req.view',           'View material requisitions',         '/production/material-req/**',     'GET',    'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
    ('prd.material_req.create',         'Create material requisition',        '/production/material-req/save',   'POST',   'PRODUCTION', 'PRODUCTION', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.11  Commercial / LC ─────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('com.lc.view',                     'View letters of credit',             '/commercial/lc/**',               'GET',    'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.lc.create',                   'Create LC',                          '/commercial/lc/save',             'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.lc.edit',                     'Amend LC',                           '/commercial/lc/save',             'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.lc.delete',                   'Delete LC',                          '/commercial/lc/delete/**',        'DELETE', 'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.export.view',                 'View export documents',              '/commercial/exports/**',          'GET',    'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.export.create',               'Create export document',             '/commercial/exports/save',        'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.import.view',                 'View import documents',              '/commercial/imports/**',          'GET',    'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.import.create',               'Create import document',             '/commercial/imports/save',        'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.settlement.view',             'View LC settlements',                '/commercial/settlements/**',      'GET',    'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
    ('com.settlement.create',           'Create LC settlement',               '/commercial/settlements/save',    'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.12  CRM ─────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('crm.lead.view',                   'View CRM leads',                     '/crm/leads/**',                   'GET',    'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.lead.create',                 'Create lead',                        '/crm/leads/save',                 'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.lead.edit',                   'Edit lead',                          '/crm/leads/save',                 'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.lead.delete',                 'Delete lead',                        '/crm/leads/delete/**',            'DELETE', 'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.opportunity.view',            'View opportunities',                 '/crm/opportunities/**',           'GET',    'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.opportunity.create',          'Create opportunity',                 '/crm/opportunities/save',         'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.opportunity.edit',            'Edit opportunity',                   '/crm/opportunities/save',         'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.opportunity.delete',          'Delete opportunity',                 '/crm/opportunities/delete/**',    'DELETE', 'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.contact.view',                'View CRM contacts',                  '/crm/contacts/**',                'GET',    'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.contact.create',              'Create contact',                     '/crm/contacts/save',              'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.contact.edit',                'Edit contact',                       '/crm/contacts/save',              'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.contact.delete',              'Delete contact',                     '/crm/contacts/delete/**',         'DELETE', 'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.activity.view',               'View CRM activities',                '/crm/activities/**',              'GET',    'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.activity.create',             'Create activity',                    '/crm/activities/save',            'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.activity.edit',               'Edit activity',                      '/crm/activities/save',            'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.activity.delete',             'Delete activity',                    '/crm/activities/delete/**',       'DELETE', 'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.feedback.view',               'View customer feedback',             '/crm/feedback/**',                'GET',    'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.feedback.create',             'Create feedback entry',              '/crm/feedback/save',              'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.feedback.edit',               'Edit feedback entry',                '/crm/feedback/save',              'POST',   'CRM', 'CRM', true, NOW(), NOW()),
    ('crm.feedback.delete',             'Delete feedback entry',              '/crm/feedback/delete/**',         'DELETE', 'CRM', 'CRM', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.13  Budget ──────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('budget.fiscalyear.view',          'View fiscal years',                  '/budget/fiscal-years/**',         'GET',    'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.fiscalyear.create',        'Create fiscal year',                 '/budget/fiscal-years/save',       'POST',   'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.fiscalyear.edit',          'Edit fiscal year',                   '/budget/fiscal-years/save',       'POST',   'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.fiscalyear.delete',        'Delete fiscal year',                 '/budget/fiscal-years/delete/**',  'DELETE', 'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.head.view',                'View budget heads',                  '/budget/heads/**',                'GET',    'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.head.create',              'Create budget head',                 '/budget/heads/save',              'POST',   'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.head.edit',                'Edit budget head',                   '/budget/heads/save',              'POST',   'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.head.delete',              'Delete budget head',                 '/budget/heads/delete/**',         'DELETE', 'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.budget.view',              'View budgets',                       '/budget/list/**',                 'GET',    'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.budget.create',            'Create budget',                      '/budget/list/save',               'POST',   'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.budget.edit',              'Edit budget',                        '/budget/list/save',               'POST',   'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.budget.delete',            'Delete budget',                      '/budget/list/delete/**',          'DELETE', 'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.revision.view',            'View budget revisions',              '/budget/revisions/**',            'GET',    'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.revision.create',          'Create budget revision',             '/budget/revisions/save',          'POST',   'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.transfer.view',            'View budget transfers',              '/budget/transfers/**',            'GET',    'BUDGET', 'BUDGET', true, NOW(), NOW()),
    ('budget.transfer.create',          'Create budget transfer',             '/budget/transfers/save',          'POST',   'BUDGET', 'BUDGET', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.14  Fixed Assets ────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('fa.category.view',                'View asset categories',              '/fixed-assets/categories/**',       'GET',    'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.category.create',              'Create asset category',              '/fixed-assets/categories/save',     'POST',   'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.category.edit',                'Edit asset category',                '/fixed-assets/categories/save',     'POST',   'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.category.delete',              'Delete asset category',              '/fixed-assets/categories/delete/**', 'DELETE', 'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.asset.view',                   'View assets register',               '/fixed-assets/assets/**',           'GET',    'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.asset.create',                 'Register asset',                     '/fixed-assets/assets/save',         'POST',   'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.asset.edit',                   'Edit asset',                         '/fixed-assets/assets/save',         'POST',   'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.asset.delete',                 'Delete asset',                       '/fixed-assets/assets/delete/**',    'DELETE', 'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.depreciation.view',            'View depreciation runs',             '/fixed-assets/depreciation/**',     'GET',    'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.depreciation.calculate',       'Calculate depreciation',             '/fixed-assets/depreciation/calculate', 'POST', 'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.depreciation.post',            'Post depreciation run',              '/fixed-assets/depreciation/post/**','POST',   'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.depreciation.reverse',         'Reverse depreciation run',           '/fixed-assets/depreciation/reverse/**','POST','FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.disposal.view',                'View asset disposals',               '/fixed-assets/disposals/**',        'GET',    'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
    ('fa.disposal.create',              'Dispose asset',                      '/fixed-assets/disposals/save',      'POST',   'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.15  Approval ────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('apr.config.view',                 'View approval configs',              '/approval/configs/**',             'GET',    'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
    ('apr.config.create',               'Create approval config',             '/approval/configs/save',           'POST',   'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
    ('apr.request.view',                'View approval requests',             '/approval/requests/**',            'GET',    'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
    ('apr.request.approve',             'Approve requests',                   '/approval/requests/approve/**',    'POST',   'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
    ('apr.request.reject',              'Reject requests',                    '/approval/requests/reject/**',     'POST',   'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
    ('apr.delegation.view',             'View delegations',                   '/approval/delegations/**',         'GET',    'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
    ('apr.delegation.create',           'Create delegation',                  '/approval/delegations/save',       'POST',   'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.16  eCommerce ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('ec.dashboard.view',               'View eCommerce dashboard',           '/ecommerce/dashboard',              'GET',  'ECOMMERCE', 'DASHBOARD', true, NOW(), NOW()),
    ('ec.dashboard.summary',            'eCommerce dashboard summary JSON',   '/ecommerce/dashboard/summary',      'GET',  'ECOMMERCE', 'DASHBOARD', true, NOW(), NOW()),
    ('ec.product.view',                 'View product catalog',               '/ecommerce/products/**',            'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.product.create',               'Create product catalog entry',       '/ecommerce/products/save',          'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.product.edit',                 'Edit product catalog entry',         '/ecommerce/products/save',          'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.product.delete',               'Delete product catalog entry',       '/ecommerce/products/delete/**',     'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.product.toggle',               'Toggle product publish status',      '/ecommerce/products/toggle-publish/**','POST','ECOMMERCE','ECOMMERCE', true, NOW(), NOW()),
    ('ec.category.view',                'View storefront categories',         '/ecommerce/categories/**',          'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.category.create',              'Create storefront category',         '/ecommerce/categories/save',        'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.category.edit',                'Edit storefront category',           '/ecommerce/categories/save',        'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.category.delete',              'Delete storefront category',         '/ecommerce/categories/delete/**',   'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.category.toggle',              'Toggle category status',            '/ecommerce/categories/toggle/**',   'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.customer.view',                'View storefront customers',          '/ecommerce/customers/**',           'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.customer.create',              'Create storefront customer',         '/ecommerce/customers/save',         'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.customer.edit',                'Edit storefront customer',           '/ecommerce/customers/save',         'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.customer.delete',              'Delete storefront customer',         '/ecommerce/customers/delete/**',    'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.customer.toggle',              'Toggle customer account status',    '/ecommerce/customers/toggle/**',    'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.order.view',                   'View storefront orders',             '/ecommerce/orders/**',              'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.order.status',                 'Update order status',                '/ecommerce/orders/status/**',       'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.order.delete',                 'Delete order',                       '/ecommerce/orders/delete/**',       'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.coupon.view',                  'View discount coupons',              '/ecommerce/coupons/**',             'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.coupon.create',                'Create discount coupon',             '/ecommerce/coupons/save',           'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.coupon.edit',                  'Edit discount coupon',               '/ecommerce/coupons/save',           'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.coupon.delete',                'Delete discount coupon',             '/ecommerce/coupons/delete/**',      'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.coupon.toggle',                'Toggle coupon status',              '/ecommerce/coupons/toggle/**',     'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.shipping_method.view',         'View shipping methods',              '/ecommerce/shipping-methods/**',    'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.shipping_method.create',       'Create shipping method',             '/ecommerce/shipping-methods/save',  'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.shipping_method.edit',         'Edit shipping method',               '/ecommerce/shipping-methods/save',  'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.shipping_method.delete',       'Delete shipping method',             '/ecommerce/shipping-methods/delete/**','DELETE','ECOMMERCE','ECOMMERCE', true, NOW(), NOW()),
    ('ec.shipping_method.toggle',       'Toggle shipping method status',     '/ecommerce/shipping-methods/toggle/**','POST','ECOMMERCE','ECOMMERCE', true, NOW(), NOW()),
    ('ec.review.view',                  'View customer reviews',              '/ecommerce/reviews/**',             'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.review.approve',               'Approve customer review',            '/ecommerce/reviews/approve/**',     'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.review.reject',                'Reject customer review',             '/ecommerce/reviews/reject/**',      'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.review.hide',                  'Hide customer review',               '/ecommerce/reviews/hide/**',        'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.review.delete',                'Delete customer review',             '/ecommerce/reviews/delete/**',      'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.home_section.view',            'View homepage sections',             '/ecommerce/home-sections/**',       'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.home_section.create',          'Create homepage section',            '/ecommerce/home-sections/save',     'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.home_section.edit',            'Edit homepage section',              '/ecommerce/home-sections/save',     'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.home_section.delete',          'Delete homepage section',            '/ecommerce/home-sections/delete/**','DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.home_section.toggle',          'Toggle homepage section status',    '/ecommerce/home-sections/toggle/**','POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.gl_defaults.view',             'View eCommerce GL account defaults', '/ecommerce/gl-defaults/**',         'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.gl_defaults.edit',             'Edit eCommerce GL account defaults', '/ecommerce/gl-defaults/save',       'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.tax_class.view',               'View tax classes',                   '/ecommerce/tax-classes/**',         'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.tax_class.create',             'Create tax class',                   '/ecommerce/tax-classes/save',       'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.tax_class.edit',               'Edit tax class',                     '/ecommerce/tax-classes/save',       'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.tax_class.delete',             'Delete tax class',                   '/ecommerce/tax-classes/delete/**',  'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.tax_class.toggle',             'Toggle tax class status',           '/ecommerce/tax-classes/toggle/**', 'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.return.view',                  'View return requests',               '/ecommerce/returns/**',             'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.return.create',                'Create return request',              '/ecommerce/returns/save',           'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.return.status',                'Update return status',               '/ecommerce/returns/status/**',      'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.return.delete',                'Delete return request',              '/ecommerce/returns/delete/**',      'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.refund.view',                  'View refund disbursements',          '/ecommerce/refunds/**',             'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.refund.create',                'Issue refund',                       '/ecommerce/refunds/save',           'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.refund.delete',                'Delete refund record',               '/ecommerce/refunds/delete/**',      'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.payment.view',                 'View payment transactions',          '/ecommerce/payments/**',            'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.payment.status',               'Update payment status',              '/ecommerce/payments/status/**',     'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.payment.delete',               'Delete payment record',              '/ecommerce/payments/delete/**',     'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.cart.view',                    'View customer carts',                '/ecommerce/carts/**',               'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.cart.abandon',                 'Mark cart as abandoned',             '/ecommerce/carts/abandon/**',       'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.cart.delete',                  'Delete cart record',                 '/ecommerce/carts/delete/**',        'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.setting.view',                 'View eCommerce settings',            '/ecommerce/settings/**',            'GET',  'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.setting.create',               'Create/update eCommerce setting',    '/ecommerce/settings/save',          'POST', 'ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW()),
    ('ec.setting.delete',               'Delete eCommerce setting',           '/ecommerce/settings/delete/**',     'DELETE','ECOMMERCE', 'ECOMMERCE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── 1.17  Travel (Phase 1 + 2) ────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
    ('trv.dashboard.view',              'View Travel dashboard',              '/travel/dashboard',                   'GET',  'TRAVEL', 'DASHBOARD', true, NOW(), NOW()),
    ('trv.dashboard.summary',           'Travel dashboard summary JSON',      '/travel/dashboard/summary',           'GET',  'TRAVEL', 'DASHBOARD', true, NOW(), NOW()),
    ('trv.booking.view',                'View bookings',                      '/travel/bookings/**',                  'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.booking.create',              'Create booking',                     '/travel/bookings/save',                'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.booking.edit',                'Edit booking',                       '/travel/bookings/save',                'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.booking.delete',              'Delete booking',                     '/travel/bookings/delete/**',           'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.booking.confirm',             'Confirm booking (posts to GL)',      '/travel/bookings/confirm/**',          'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.booking.cancel',              'Cancel booking',                     '/travel/bookings/cancel/**',           'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.booking.receipt',             'Create Receipt Voucher from booking','/travel/bookings/receipt-prefill',     'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel.view',                  'View hotels',                        '/travel/hotels/**',                    'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel.create',                'Create hotel',                       '/travel/hotels/save',                  'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel.edit',                  'Edit hotel',                         '/travel/hotels/save',                  'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel.delete',                'Delete hotel',                       '/travel/hotels/delete/**',             'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.room_type.view',              'View room types',                    '/travel/hotels/room-types/**',         'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.room_type.create',            'Create room type',                   '/travel/hotels/room-types/save',       'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.room_type.edit',              'Edit room type',                     '/travel/hotels/room-types/save',       'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.room_type.delete',            'Delete room type',                   '/travel/hotels/room-types/delete/**',  'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_booking.view',          'View hotel bookings',                '/travel/hotel-bookings/**',            'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_booking.create',        'Create hotel booking',               '/travel/hotel-bookings/save',          'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_booking.edit',          'Edit hotel booking',                 '/travel/hotel-bookings/save',          'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_booking.delete',        'Delete hotel booking',               '/travel/hotel-bookings/delete/**',     'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_booking.confirm',       'Confirm hotel booking',              '/travel/hotel-bookings/confirm/**',    'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.air_ticket.view',             'View air tickets',                   '/travel/air-tickets/**',               'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.air_ticket.create',           'Create air ticket',                  '/travel/air-tickets/save',             'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.air_ticket.edit',             'Edit air ticket',                    '/travel/air-tickets/save',             'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.air_ticket.delete',           'Delete air ticket',                  '/travel/air-tickets/delete/**',        'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.supplier_cost.view',          'View supplier costs',                '/travel/supplier-costs/**',            'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.supplier_cost.create',        'Create supplier cost',               '/travel/supplier-costs/save',          'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.supplier_cost.edit',          'Edit supplier cost',                 '/travel/supplier-costs/save',          'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.supplier_cost.delete',        'Delete supplier cost',               '/travel/supplier-costs/delete/**',     'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_category.view',         'View hotel categories',              '/travel/masters/hotel-categories/**',  'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_category.create',       'Create hotel category',              '/travel/masters/hotel-categories/save','POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_category.edit',         'Edit hotel category',                '/travel/masters/hotel-categories/save','POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.hotel_category.delete',       'Delete hotel category',              '/travel/masters/hotel-categories/delete/**','DELETE','TRAVEL','TRAVEL', true, NOW(), NOW()),
    ('trv.meal_plan.view',              'View meal plans',                    '/travel/masters/meal-plans/**',        'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.meal_plan.create',            'Create meal plan',                   '/travel/masters/meal-plans/save',      'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.meal_plan.edit',              'Edit meal plan',                     '/travel/masters/meal-plans/save',      'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.meal_plan.delete',            'Delete meal plan',                   '/travel/masters/meal-plans/delete/**', 'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.airline.view',                'View airlines',                      '/travel/masters/airlines/**',          'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.airline.create',              'Create airline',                     '/travel/masters/airlines/save',        'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.airline.edit',                'Edit airline',                       '/travel/masters/airlines/save',        'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.airline.delete',              'Delete airline',                     '/travel/masters/airlines/delete/**',   'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.airport.view',                'View airports',                      '/travel/masters/airports/**',          'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.airport.create',              'Create airport',                     '/travel/masters/airports/save',        'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.airport.edit',                'Edit airport',                       '/travel/masters/airports/save',        'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.airport.delete',              'Delete airport',                     '/travel/masters/airports/delete/**',   'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.cabin_class.view',            'View cabin classes',                 '/travel/masters/cabin-classes/**',     'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.cabin_class.create',          'Create cabin class',                 '/travel/masters/cabin-classes/save',   'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.cabin_class.edit',            'Edit cabin class',                   '/travel/masters/cabin-classes/save',   'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.cabin_class.delete',          'Delete cabin class',                 '/travel/masters/cabin-classes/delete/**','DELETE','TRAVEL','TRAVEL', true, NOW(), NOW()),
    ('trv.setting.view',                'View Travel settings',               '/travel/settings/**',                  'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.setting.edit',                'Edit Travel settings',               '/travel/settings/defaults',            'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    -- Travel Phase 2
    ('trv.package.view',                'View packages',                      '/travel/packages/**',                  'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.package.create',              'Create package',                     '/travel/packages/save',                'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.package.edit',                'Edit package',                       '/travel/packages/save',                'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.package.delete',              'Delete package',                     '/travel/packages/delete/**',           'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.tour.view',                   'View tours',                         '/travel/tours/**',                     'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.tour.create',                 'Create tour',                        '/travel/tours/save',                   'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.tour.edit',                   'Edit tour',                          '/travel/tours/save',                   'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.tour.delete',                 'Delete tour',                        '/travel/tours/delete/**',              'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.visa.view',                   'View visa applications',             '/travel/visa-applications/**',         'GET',  'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.visa.create',                 'Create visa application',            '/travel/visa-applications/save',       'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.visa.edit',                   'Edit visa application',              '/travel/visa-applications/save',       'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.visa.status',                 'Update visa application status',    '/travel/visa-applications/status/**',  'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
    ('trv.visa.delete',                 'Delete visa application',            '/travel/visa-applications/delete/**',  'DELETE','TRAVEL', 'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- SECTION 2 — ROLES
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO sec_roles (name, name_bn, description, master_role, active, created_at, updated_at)
VALUES
    -- Core ERP Roles
    ('ROLE_SUPER_ADMIN',          'সুপার অ্যাডমিন',       'Full system access — bypasses all permission checks', 'ROLE_SUPER_ADMIN', true, NOW(), NOW()),
    ('ROLE_ACCOUNTS_ADMIN',       'হিসাব প্রশাসক',         'Full accounts/finance access, view purchase/sales',  'ROLE_ACCOUNTS_ADMIN', true, NOW(), NOW()),
    ('ROLE_ACCOUNTANT',           'হিসাবরক্ষক',            'Journal, payment, receipt vouchers; view reports',     'ROLE_ACCOUNTANT', true, NOW(), NOW()),
    ('ROLE_INVENTORY_MANAGER',    'ইনভেন্টরি ম্যানেজার',   'Full inventory management',                            'ROLE_INVENTORY_MANAGER', true, NOW(), NOW()),
    ('ROLE_WAREHOUSE_STAFF',      'ওয়্যারহাউস স্টাফ',     'View stock, create GRN, stock transfer',              'ROLE_WAREHOUSE_STAFF', true, NOW(), NOW()),
    ('ROLE_PURCHASE_MANAGER',     'ক্রয় ম্যানেজার',       'Full purchase cycle',                                 'ROLE_PURCHASE_MANAGER', true, NOW(), NOW()),
    ('ROLE_PURCHASE_OFFICER',     'ক্রয় কর্মকর্তা',        'PO, GRN create/edit; view invoices',                  'ROLE_PURCHASE_OFFICER', true, NOW(), NOW()),
    ('ROLE_SALES_MANAGER',        'বিক্রয় ম্যানেজার',      'Full sales cycle',                                    'ROLE_SALES_MANAGER', true, NOW(), NOW()),
    ('ROLE_SALES_EXECUTIVE',      'বিক্রয় নির্বাহী',       'SO create/edit; view delivery, invoice, receipt',     'ROLE_SALES_EXECUTIVE', true, NOW(), NOW()),
    ('ROLE_HRM',                  'এইচআরএম ম্যানেজার',      'Full HRM access',                                    'ROLE_HRM', true, NOW(), NOW()),
    ('ROLE_PRODUCTION_MANAGER',   'উৎপাদন ম্যানেজার',       'Full production access',                              'ROLE_PRODUCTION_MANAGER', true, NOW(), NOW()),
    ('ROLE_PRODUCTION_SUPERVISOR','উৎপাদন সুপারভাইজার',      'View production, create material requisitions',      'ROLE_PRODUCTION_SUPERVISOR', true, NOW(), NOW()),
    ('ROLE_COMMERCIAL_MANAGER',   'বাণিজ্যিক ম্যানেজার',      'Full commercial/LC access',                          'ROLE_COMMERCIAL_MANAGER', true, NOW(), NOW()),
    ('ROLE_COMMERCIAL_EXECUTIVE', 'বাণিজ্যিক নির্বাহী',       'View and create commercial/export/import docs',      'ROLE_COMMERCIAL_EXECUTIVE', true, NOW(), NOW()),
    ('ROLE_CRM_MANAGER',          'সিআরএম ম্যানেজার',        'Full CRM access: leads, opportunities, contacts',    'ROLE_CRM_MANAGER', true, NOW(), NOW()),
    ('ROLE_CRM_EXECUTIVE',        'সিআরএম নির্বাহী',         'View leads/opportunities, create activities',        'ROLE_CRM_EXECUTIVE', true, NOW(), NOW()),
    ('ROLE_BUDGET_MANAGER',       'বাজেট ম্যানেজার',         'Full budget management: fiscal years, heads, budgets','ROLE_BUDGET_MANAGER', true, NOW(), NOW()),
    ('ROLE_ASSET_MANAGER',        'সম্পদ ব্যবস্থাপক',        'Full fixed assets: register, depreciation, disposal', 'ROLE_ASSET_MANAGER', true, NOW(), NOW()),
    -- Module-Specific Roles
    ('ROLE_ECOMMERCE_MANAGER',    'ই-কমার্স ম্যানেজার',     'Full storefront management: catalog, orders, customers, coupons, returns/refunds, settings', 'ROLE_ECOMMERCE_MANAGER', true, NOW(), NOW()),
    ('ROLE_ECOMMERCE_EXECUTIVE',  'ই-কমার্স নির্বাহী',      'Day-to-day storefront operations: order fulfillment, review moderation, customer support', 'ROLE_ECOMMERCE_EXECUTIVE', true, NOW(), NOW()),
    ('ROLE_TRAVEL_MANAGER',       'ভ্রমণ ব্যবস্থাপক',        'Full Travel module management: bookings, hotels, air tickets, supplier costs, master data', 'ROLE_TRAVEL_MANAGER', true, NOW(), NOW()),
    ('ROLE_TRAVEL_EXECUTIVE',     'ভ্রমণ নির্বাহী',          'Day-to-day travel operations: booking creation, fulfillment, no settings or delete', 'ROLE_TRAVEL_EXECUTIVE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- SECTION 3 — APP MENUS  (MODULE → GROUP → LEAF)
-- ═════════════════════════════════════════════════════════════════════════════

-- ── 3A. MODULE level ─────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
VALUES
    ('MOD_DASHBOARD',    'Dashboard',   '/dashboard',   'fa fa-th-large',     NULL, 10,  'MODULE', 'DASHBOARD',   'dashboard.view',      '_self', true, true, false, NOW(), NOW()),
    ('MOD_INVENTORY',    'Inventory',   NULL,           'fa fa-boxes',        NULL, 20,  'MODULE', 'INVENTORY',   NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_PURCHASE',     'Purchase',    NULL,           'fa fa-shopping-cart',NULL, 30,  'MODULE', 'PURCHASE',    NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_SALES',        'Sales',       NULL,           'fa fa-tags',         NULL, 40,  'MODULE', 'SALES',       NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_ACCOUNTS',     'Accounts',    NULL,           'fa fa-calculator',   NULL, 50,  'MODULE', 'ACCOUNTS',    NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_HRM',          'HRM',         NULL,           'fa fa-users',        NULL, 60,  'MODULE', 'HRM',         NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_PRODUCTION',   'Production',  NULL,           'fa fa-industry',     NULL, 70,  'MODULE', 'PRODUCTION',  NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_COMMERCIAL',   'Commercial',  NULL,           'fa fa-ship',         NULL, 80,  'MODULE', 'COMMERCIAL',  NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_CRM',          'CRM',         NULL,           'fa fa-handshake',    NULL, 90,  'MODULE', 'CRM',         'crm.lead.view',       '_self', true, true, false, NOW(), NOW()),
    ('MOD_ECOMMERCE',    'eCommerce',   NULL,           'fa fa-store',        NULL, 95,  'MODULE', 'ECOMMERCE',   'ec.product.view',     '_self', true, true, false, NOW(), NOW()),
    ('MOD_TRAVEL',       'Travel',      NULL,           'fa fa-plane',        NULL, 96,  'MODULE', 'TRAVEL',      'trv.dashboard.view',  '_self', true, true, false, NOW(), NOW()),
    ('MOD_BUDGET',       'Budget',      NULL,           'fa fa-chart-pie',    NULL, 100, 'MODULE', 'BUDGET',      'budget.budget.view',  '_self', true, true, false, NOW(), NOW()),
    ('MOD_FIXED_ASSETS', 'Fixed Assets',NULL,           'fa fa-building',     NULL, 110, 'MODULE', 'FIXED_ASSETS','fa.asset.view',       '_self', true, true, false, NOW(), NOW()),
    ('MOD_SETUP',        'Setup',       NULL,           'fa fa-cogs',         NULL, 120, 'MODULE', 'SETUP',       NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_SECURITY',     'Security',    NULL,           'fa fa-shield-alt',   NULL, 130, 'MODULE', 'SECURITY',    NULL,                  '_self', true, true, false, NOW(), NOW()),
    ('MOD_APPROVALS',    'Approvals',   NULL,           'fa fa-tasks',        NULL, 140, 'MODULE', 'APPROVALS',   'apr.request.view',    '_self', true, true, false, NOW(), NOW()),
    ('MOD_REPORTS',      'Reports',     '/reports',     'fa fa-chart-bar',    NULL, 150, 'MODULE', 'REPORTS',     'reports.view',        '_self', true, true, false, NOW(), NOW())
ON CONFLICT (menu_code) DO NOTHING;


-- 3B. GROUP level
-- Inventory
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_INV_MASTER', 'Item Master', NULL, 'fa fa-layer-group', m.id, 10, 'GROUP', 'INVENTORY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_INVENTORY' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_INV_STOCK', 'Stock Management', NULL, 'fa fa-warehouse', m.id, 20, 'GROUP', 'INVENTORY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_INVENTORY' ON CONFLICT (menu_code) DO NOTHING;

-- Purchase groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_MASTER', 'Suppliers', NULL, 'fa fa-truck', m.id, 10, 'GROUP', 'PURCHASE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PURCHASE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_CYCLE', 'Purchase Cycle', NULL, 'fa fa-file-invoice', m.id, 20, 'GROUP', 'PURCHASE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PURCHASE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_RETURNS', 'Returns & Payments', NULL, 'fa fa-undo', m.id, 30, 'GROUP', 'PURCHASE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PURCHASE' ON CONFLICT (menu_code) DO NOTHING;

-- Sales groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_MASTER', 'Customers', NULL, 'fa fa-user-tie', m.id, 10, 'GROUP', 'SALES', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SALES' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_CYCLE', 'Sales Cycle', NULL, 'fa fa-file-invoice-dollar', m.id, 20, 'GROUP', 'SALES', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SALES' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_RETURNS', 'Returns & Receipts', NULL, 'fa fa-undo-alt', m.id, 30, 'GROUP', 'SALES', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SALES' ON CONFLICT (menu_code) DO NOTHING;

-- Accounts groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_MASTER', 'Chart & Accounts', NULL, 'fa fa-list-alt', m.id, 10, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_VOUCHERS', 'Vouchers', NULL, 'fa fa-receipt', m.id, 20, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_REPORTS', 'Financial Reports', NULL, 'fa fa-chart-line', m.id, 30, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_CONFIG', 'Accounts Config', NULL, 'fa fa-sliders-h', m.id, 40, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS' ON CONFLICT (menu_code) DO NOTHING;

-- HRM groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_MASTER', 'HR Master', NULL, 'fa fa-id-card', m.id, 10, 'GROUP', 'HRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_HRM' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_ATTENDANCE', 'Attendance & Leave', NULL, 'fa fa-calendar-check', m.id, 20, 'GROUP', 'HRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_HRM' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_PAYROLL', 'Payroll', NULL, 'fa fa-money-bill-wave', m.id, 30, 'GROUP', 'HRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_HRM' ON CONFLICT (menu_code) DO NOTHING;

-- Production groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PRD_MASTER', 'Production Master', NULL, 'fa fa-layer-group', m.id, 10, 'GROUP', 'PRODUCTION', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PRODUCTION' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PRD_ORDERS', 'Production Orders', NULL, 'fa fa-clipboard-list', m.id, 20, 'GROUP', 'PRODUCTION', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PRODUCTION' ON CONFLICT (menu_code) DO NOTHING;

-- Commercial groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_COM_LC', 'Letter of Credit', NULL, 'fa fa-file-contract', m.id, 10, 'GROUP', 'COMMERCIAL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_COMMERCIAL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_COM_TRADE', 'Trade Documents', NULL, 'fa fa-globe', m.id, 20, 'GROUP', 'COMMERCIAL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_COMMERCIAL' ON CONFLICT (menu_code) DO NOTHING;

-- CRM groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_CRM_PIPELINE', 'Pipeline', NULL, 'fa fa-filter', m.id, 10, 'GROUP', 'CRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_CRM' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_CRM_ENGAGE', 'Engagement', NULL, 'fa fa-comments', m.id, 20, 'GROUP', 'CRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_CRM' ON CONFLICT (menu_code) DO NOTHING;

-- Budget groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_BGT_MASTER', 'Budget Master', NULL, 'fa fa-layer-group', m.id, 10, 'GROUP', 'BUDGET', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_BUDGET' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_BGT_TRANSACTIONS', 'Budget Transactions', NULL, 'fa fa-exchange-alt', m.id, 20, 'GROUP', 'BUDGET', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_BUDGET' ON CONFLICT (menu_code) DO NOTHING;

-- Fixed Assets groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_FA_MASTER', 'Asset Master', NULL, 'fa fa-layer-group', m.id, 10, 'GROUP', 'FIXED_ASSETS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_FIXED_ASSETS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_FA_OPERATIONS', 'Asset Operations', NULL, 'fa fa-cogs', m.id, 20, 'GROUP', 'FIXED_ASSETS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_FIXED_ASSETS' ON CONFLICT (menu_code) DO NOTHING;

-- Setup groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_STP_ORG', 'Organization', NULL, 'fa fa-building', m.id, 10, 'GROUP', 'SETUP', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SETUP' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_STP_REF', 'Reference Data', NULL, 'fa fa-book', m.id, 20, 'GROUP', 'SETUP', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SETUP' ON CONFLICT (menu_code) DO NOTHING;

-- Security groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SEC_USER', 'Users & Roles', NULL, 'fa fa-user-shield', m.id, 10, 'GROUP', 'SECURITY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SECURITY' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SEC_MENU', 'Menu & Permissions', NULL, 'fa fa-sitemap', m.id, 20, 'GROUP', 'SECURITY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SECURITY' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SEC_ORG', 'Org Administration', NULL, 'fa fa-building', m.id, 30, 'GROUP', 'SECURITY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SECURITY' ON CONFLICT (menu_code) DO NOTHING;

-- Approvals groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_APR_CONFIG', 'Approval Setup', NULL, 'fa fa-tools', m.id, 10, 'GROUP', 'APPROVALS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_APPROVALS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_APR_PENDING', 'My Approvals', NULL, 'fa fa-clock', m.id, 20, 'GROUP', 'APPROVALS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_APPROVALS' ON CONFLICT (menu_code) DO NOTHING;

-- eCommerce groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_CATALOG', 'Catalog', NULL, 'fa fa-tags', m.id, 10, 'GROUP', 'ECOMMERCE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_SALES', 'Sales & Orders', NULL, 'fa fa-receipt', m.id, 20, 'GROUP', 'ECOMMERCE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_CUSTOMERS', 'Customers', NULL, 'fa fa-users', m.id, 30, 'GROUP', 'ECOMMERCE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_MARKETING', 'Marketing', NULL, 'fa fa-bullhorn', m.id, 40, 'GROUP', 'ECOMMERCE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_RETURNS', 'Returns & Payments', NULL, 'fa fa-undo', m.id, 50, 'GROUP', 'ECOMMERCE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_EC_CONFIG', 'Configuration', NULL, 'fa fa-sliders-h', m.id, 60, 'GROUP', 'ECOMMERCE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ECOMMERCE' ON CONFLICT (menu_code) DO NOTHING;

-- Travel groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_SALES', 'Bookings & Sales', NULL, 'fa fa-ticket-alt', m.id, 10, 'GROUP', 'TRAVEL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_HOTEL', 'Hotel Management', NULL, 'fa fa-hotel', m.id, 20, 'GROUP', 'TRAVEL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_AIR', 'Air Ticketing', NULL, 'fa fa-plane-departure', m.id, 30, 'GROUP', 'TRAVEL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_PACKAGES', 'Packages & Tours', NULL, 'fa fa-suitcase-rolling', m.id, 40, 'GROUP', 'TRAVEL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_VISA', 'Visa Services', NULL, 'fa fa-passport', m.id, 50, 'GROUP', 'TRAVEL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_FINANCE', 'Finance', NULL, 'fa fa-file-invoice-dollar', m.id, 60, 'GROUP', 'TRAVEL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_CONFIG', 'Configuration', NULL, 'fa fa-sliders-h', m.id, 70, 'GROUP', 'TRAVEL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL' ON CONFLICT (menu_code) DO NOTHING;

-- 3C. LEAF level
-- =============================================================================
-- 3C. LEAF level — Inventory
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_DASHBOARD', 'Dashboard', '/inventory/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'INVENTORY', 'inv.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_INVENTORY' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_UOM', 'Units of Measure', '/inventory/uoms', 'fa fa-ruler', g.id, 10, 'LEAF', 'INVENTORY', 'inv.uom.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_INV_MASTER' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — Purchase
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_DASHBOARD', 'Dashboard', '/purchase/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'PURCHASE', 'pur.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_PURCHASE' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — Sales
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_DASHBOARD', 'Dashboard', '/sales/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'SALES', 'sal.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_SALES' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — Accounts
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_DASHBOARD', 'Dashboard', '/accounts/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'ACCOUNTS', 'acc.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_ACCOUNTS' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — HRM
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_DASHBOARD', 'Dashboard', '/hrm/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'HRM', 'hrm.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_HRM' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — Production
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PRD_DASHBOARD', 'Dashboard', '/production/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'PRODUCTION', 'prd.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_PRODUCTION' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — Commercial
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_DASHBOARD', 'Dashboard', '/commercial/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'COMMERCIAL', 'com.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_COMMERCIAL' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — CRM
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_DASHBOARD', 'Dashboard', '/crm/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'CRM', 'crm.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_CRM' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — Budget
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_DASHBOARD', 'Dashboard', '/budget/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'BUDGET', 'budget.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_BUDGET' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — Fixed Assets
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_DASHBOARD', 'Dashboard', '/fixed-assets/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'FIXED_ASSETS', 'fa.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_FIXED_ASSETS' ON CONFLICT (menu_code) DO NOTHING;

-- =============================================================================
-- 3C. LEAF level — Travel (Phase 1 + Phase 2)
-- =============================================================================
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_DASHBOARD', 'Dashboard', '/travel/dashboard', 'fa fa-tachometer-alt', g.id, 5, 'LEAF', 'TRAVEL', 'trv.dashboard.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'MOD_TRAVEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_BOOKINGS', 'Bookings', '/travel/bookings', 'fa fa-ticket-alt', g.id, 10, 'LEAF', 'TRAVEL', 'trv.booking.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_SALES' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_FRONT_DESK', 'Front Desk', '/travel/frontdesk', 'fa fa-concierge-bell', g.id, 20, 'LEAF', 'TRAVEL', 'trv.booking.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_SALES' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_HOTELS', 'Hotels', '/travel/hotels', 'fa fa-hotel', g.id, 10, 'LEAF', 'TRAVEL', 'trv.hotel.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_HOTEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_HOTEL_BOOKINGS', 'Hotel Bookings', '/travel/hotel-bookings', 'fa fa-calendar-check', g.id, 20, 'LEAF', 'TRAVEL', 'trv.hotel_booking.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_HOTEL' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_AIR_TICKETS', 'Air Tickets', '/travel/air-tickets', 'fa fa-plane', g.id, 10, 'LEAF', 'TRAVEL', 'trv.air_ticket.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_AIR' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_PACKAGES', 'Packages', '/travel/packages', 'fa fa-suitcase', g.id, 10, 'LEAF', 'TRAVEL', 'trv.package.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_PACKAGES' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_TOURS', 'Tours', '/travel/tours', 'fa fa-map-marked-alt', g.id, 30, 'LEAF', 'TRAVEL', 'trv.tour.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_PACKAGES' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_VISA_APPLICATIONS', 'Visa Applications', '/travel/visa-applications', 'fa fa-passport', g.id, 10, 'LEAF', 'TRAVEL', 'trv.visa.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_VISA' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_SUPPLIER_COSTS', 'Supplier Costs', '/travel/supplier-costs', 'fa fa-file-invoice-dollar', g.id, 10, 'LEAF', 'TRAVEL', 'trv.supplier_cost.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_FINANCE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_MASTERS', 'Master Data', '/travel/masters', 'fa fa-list-alt', g.id, 10, 'LEAF', 'TRAVEL', 'trv.setting.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_CONFIG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_SETTINGS', 'Settings', '/travel/settings', 'fa fa-cog', g.id, 20, 'LEAF', 'TRAVEL', 'trv.setting.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_CONFIG' ON CONFLICT (menu_code) DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- SECTION 4 — ROLE PERMISSIONS  (sec_role_permissions)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_SUPER_ADMIN → wildcard only
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r, sec_permissions p
WHERE r.name = 'ROLE_SUPER_ADMIN' AND p.name = '*'
ON CONFLICT DO NOTHING;

-- ROLE_TRAVEL_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r
JOIN sec_permissions p ON p.name IN (
    'dashboard.view', 'dashboard.summary',
    'trv.dashboard.view', 'trv.dashboard.summary',
    'trv.booking.view', 'trv.booking.create', 'trv.booking.edit',
    'trv.booking.delete', 'trv.booking.confirm', 'trv.booking.cancel', 'trv.booking.receipt',
    'trv.hotel.view', 'trv.hotel.create', 'trv.hotel.edit', 'trv.hotel.delete',
    'trv.room_type.view', 'trv.room_type.create', 'trv.room_type.edit', 'trv.room_type.delete',
    'trv.hotel_booking.view', 'trv.hotel_booking.create', 'trv.hotel_booking.edit',
    'trv.hotel_booking.delete', 'trv.hotel_booking.confirm',
    'trv.air_ticket.view', 'trv.air_ticket.create', 'trv.air_ticket.edit', 'trv.air_ticket.delete',
    'trv.supplier_cost.view', 'trv.supplier_cost.create', 'trv.supplier_cost.edit', 'trv.supplier_cost.delete',
    'trv.hotel_category.view', 'trv.hotel_category.create', 'trv.hotel_category.edit', 'trv.hotel_category.delete',
    'trv.meal_plan.view', 'trv.meal_plan.create', 'trv.meal_plan.edit', 'trv.meal_plan.delete',
    'trv.airline.view', 'trv.airline.create', 'trv.airline.edit', 'trv.airline.delete',
    'trv.airport.view', 'trv.airport.create', 'trv.airport.edit', 'trv.airport.delete',
    'trv.cabin_class.view', 'trv.cabin_class.create', 'trv.cabin_class.edit', 'trv.cabin_class.delete',
    'trv.setting.view', 'trv.setting.edit',
    'trv.package.view', 'trv.package.create', 'trv.package.edit', 'trv.package.delete',
    'trv.tour.view', 'trv.tour.create', 'trv.tour.edit', 'trv.tour.delete',
    'trv.visa.view', 'trv.visa.create', 'trv.visa.edit', 'trv.visa.status', 'trv.visa.delete'
)
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE (no delete rights)
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r
JOIN sec_permissions p ON p.name IN (
    'dashboard.view', 'dashboard.summary',
    'trv.dashboard.view', 'trv.dashboard.summary',
    'trv.booking.view', 'trv.booking.create', 'trv.booking.edit',
    'trv.hotel.view', 'trv.hotel.create', 'trv.hotel.edit',
    'trv.room_type.view',
    'trv.hotel_booking.view', 'trv.hotel_booking.create', 'trv.hotel_booking.edit',
    'trv.air_ticket.view', 'trv.air_ticket.create', 'trv.air_ticket.edit',
    'trv.supplier_cost.view', 'trv.supplier_cost.create', 'trv.supplier_cost.edit',
    'trv.hotel_category.view', 'trv.meal_plan.view',
    'trv.airline.view', 'trv.airport.view', 'trv.cabin_class.view',
    'trv.package.view', 'trv.package.create', 'trv.package.edit',
    'trv.tour.view', 'trv.tour.create', 'trv.tour.edit',
    'trv.visa.view', 'trv.visa.create', 'trv.visa.edit', 'trv.visa.status'
)
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- SECTION 5 — SEC_MROLE_MENUS (Role ↔ Menu CRUD access)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_SUPER_ADMIN → all menus, full CRUD
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true, true, true, true, NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND m.active = true AND m.deleted = false
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_TRAVEL_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true,
       m.menu_code IN ('TRV_BOOKINGS','TRV_HOTELS','TRV_HOTEL_BOOKINGS','TRV_AIR_TICKETS',
                       'TRV_PACKAGES','TRV_TOURS','TRV_VISA_APPLICATIONS','TRV_SUPPLIER_COSTS','TRV_MASTERS'),
       m.menu_code IN ('TRV_BOOKINGS','TRV_HOTELS','TRV_HOTEL_BOOKINGS','TRV_AIR_TICKETS',
                       'TRV_PACKAGES','TRV_TOURS','TRV_VISA_APPLICATIONS','TRV_SUPPLIER_COSTS','TRV_MASTERS'),
       m.menu_code IN ('TRV_BOOKINGS','TRV_HOTELS','TRV_HOTEL_BOOKINGS','TRV_AIR_TICKETS',
                       'TRV_PACKAGES','TRV_TOURS','TRV_VISA_APPLICATIONS','TRV_SUPPLIER_COSTS','TRV_MASTERS'),
       NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD','MOD_TRAVEL',
                      'GRP_TRV_SALES','GRP_TRV_HOTEL','GRP_TRV_AIR','GRP_TRV_PACKAGES','GRP_TRV_VISA',
                      'GRP_TRV_FINANCE','GRP_TRV_CONFIG',
                      'TRV_DASHBOARD','TRV_BOOKINGS','TRV_FRONT_DESK','TRV_HOTELS','TRV_HOTEL_BOOKINGS',
                      'TRV_AIR_TICKETS','TRV_PACKAGES','TRV_TOURS',
                      'TRV_VISA_APPLICATIONS','TRV_SUPPLIER_COSTS','TRV_MASTERS','TRV_SETTINGS')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE (view+create+edit only, no delete)
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true,
       m.menu_code IN ('TRV_BOOKINGS','TRV_HOTELS','TRV_HOTEL_BOOKINGS','TRV_AIR_TICKETS',
                       'TRV_PACKAGES','TRV_TOURS','TRV_VISA_APPLICATIONS','TRV_SUPPLIER_COSTS'),
       m.menu_code IN ('TRV_BOOKINGS','TRV_HOTELS','TRV_HOTEL_BOOKINGS','TRV_AIR_TICKETS',
                       'TRV_PACKAGES','TRV_TOURS','TRV_VISA_APPLICATIONS','TRV_SUPPLIER_COSTS'),
       false, NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
  AND m.menu_code IN ('MOD_DASHBOARD','MOD_TRAVEL',
                      'GRP_TRV_SALES','GRP_TRV_HOTEL','GRP_TRV_AIR','GRP_TRV_PACKAGES','GRP_TRV_VISA',
                      'GRP_TRV_FINANCE',
                      'TRV_DASHBOARD','TRV_BOOKINGS','TRV_FRONT_DESK','TRV_HOTELS','TRV_HOTEL_BOOKINGS',
                      'TRV_AIR_TICKETS','TRV_PACKAGES','TRV_TOURS',
                      'TRV_VISA_APPLICATIONS','TRV_SUPPLIER_COSTS','TRV_MASTERS')
ON CONFLICT (role_id, menu_id) DO NOTHING;

COMMIT;
