-- =============================================================================
--  Optimum ERP — Full Security Seed Data
--  File   : 01_seed_security_data.sql
--  Target : PostgreSQL (app_menus, sec_permissions, sec_roles,
--           sec_role_permissions, sec_mrole_menus)
--
--  Execution order:
--    1. sec_permissions   (no FKs)
--    2. sec_roles         (no FKs)
--    3. sec_role_permissions (role + permission FKs)
--    4. app_menus         (self-referential parentId; insert parents first)
--    5. sec_mrole_menus   (role + menu FKs)
--
--  Idempotent via ON CONFLICT DO NOTHING on unique constraints.
--  Run as many times as needed — safe to re-run.
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- 0. SUPERADMIN WILDCARD  (already created by SecurityDataInitializer, but
--    we re-upsert here so this file is standalone when needed)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES ('*', 'Super admin wildcard — all access', '/**', NULL, 'CORE_SECURITY', 'SYSTEM', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- =============================================================================
-- 1.  PERMISSIONS
--     Format: module.entity.action   e.g. SECURITY.USER.VIEW
--     category matches the Permission.Module enum in the entity
-- =============================================================================

-- ─── CORE SECURITY ────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('SECURITY.USER.VIEW',        'View users list',            '/users/**',                  'GET',    'CORE_SECURITY', 'USER_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.USER.CREATE',      'Create new user',            '/users/save',                'POST',   'CORE_SECURITY', 'USER_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.USER.EDIT',        'Edit user',                  '/users/save',                'POST',   'CORE_SECURITY', 'USER_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.USER.DELETE',      'Delete user',                '/users/delete/**',           'DELETE', 'CORE_SECURITY', 'USER_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.USER.TOGGLE',      'Enable/disable user',        '/users/toggle/**',           'POST',   'CORE_SECURITY', 'USER_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.USER.PWD',         'Change user password',       '/users/change-password/**',  'POST',   'CORE_SECURITY', 'USER_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.ROLE.VIEW',        'View roles list',            '/roles/**',                  'GET',    'CORE_SECURITY', 'ROLE_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.ROLE.CREATE',      'Create role',                '/roles/save',                'POST',   'CORE_SECURITY', 'ROLE_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.ROLE.EDIT',        'Edit role',                  '/roles/save',                'POST',   'CORE_SECURITY', 'ROLE_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.ROLE.DELETE',      'Delete role',                '/roles/delete/**',           'DELETE', 'CORE_SECURITY', 'ROLE_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.MENU.VIEW',        'View menus',                 '/menus/**',                  'GET',    'CORE_SECURITY', 'MENU_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.MENU.CREATE',      'Create menu',                '/menus/save',                'POST',   'CORE_SECURITY', 'MENU_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.MENU.EDIT',        'Edit menu',                  '/menus/save',                'POST',   'CORE_SECURITY', 'MENU_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.MENU.DELETE',      'Delete menu',                '/menus/delete/**',           'DELETE', 'CORE_SECURITY', 'MENU_MANAGEMENT', true, NOW(), NOW()),
('SECURITY.PERMISSION.VIEW',  'View permissions',           '/permissions/**',            'GET',    'CORE_SECURITY', 'PERMISSION_MGMT', true, NOW(), NOW()),
('SECURITY.ROLEMENU.MANAGE',  'Manage role-menu access',    '/role-menus/**',             'POST',   'CORE_SECURITY', 'ROLE_MANAGEMENT', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── ORGANIZATION SETUP ───────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('ORG.ORGANIZATION.VIEW',     'View organizations',         '/organizations/**',          'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.ORGANIZATION.CREATE',   'Create organization',        '/organizations/save',        'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.ORGANIZATION.EDIT',     'Edit organization',          '/organizations/save',        'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.ORGANIZATION.DELETE',   'Delete organization',        '/organizations/delete/**',   'DELETE', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.BUSINESS_UNIT.VIEW',    'View business units',        '/business-units/**',         'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.BUSINESS_UNIT.CREATE',  'Create business unit',       '/business-units/save',       'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.BUSINESS_UNIT.EDIT',    'Edit business unit',         '/business-units/save',       'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.BUSINESS_UNIT.DELETE',  'Delete business unit',       '/business-units/delete/**',  'DELETE', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.WAREHOUSE.VIEW',        'View warehouses',            '/warehouses/**',             'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.WAREHOUSE.CREATE',      'Create warehouse',           '/warehouses/save',           'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.WAREHOUSE.EDIT',        'Edit warehouse',             '/warehouses/save',           'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.WAREHOUSE.DELETE',      'Delete warehouse',           '/warehouses/delete/**',      'DELETE', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.COST_CENTER.VIEW',      'View cost centers',          '/cost-centers/**',           'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.COST_CENTER.CREATE',    'Create cost center',         '/cost-centers/save',         'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.COST_CENTER.EDIT',      'Edit cost center',           '/cost-centers/save',         'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.COST_CENTER.DELETE',    'Delete cost center',         '/cost-centers/delete/**',    'DELETE', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.DEPARTMENT.VIEW',       'View departments',           '/departments/**',            'GET',    'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.DEPARTMENT.CREATE',     'Create department',          '/departments/save',          'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.DEPARTMENT.EDIT',       'Edit department',            '/departments/save',          'POST',   'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW()),
('ORG.DEPARTMENT.DELETE',     'Delete department',          '/departments/delete/**',     'DELETE', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── SETUP (Reference Data) ───────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('SETUP.BANK.VIEW',           'View banks',                 '/setup/banks/**',            'GET',    'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.BANK.CREATE',         'Create bank',                '/setup/banks/save',          'POST',   'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.BANK.EDIT',           'Edit bank',                  '/setup/banks/save',          'POST',   'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.BANK.DELETE',         'Delete bank',                '/setup/banks/delete/**',     'DELETE', 'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.CURRENCY.VIEW',       'View currencies',            '/setup/currencies/**',       'GET',    'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.CURRENCY.CREATE',     'Create currency',            '/setup/currencies/save',     'POST',   'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.CURRENCY.EDIT',       'Edit currency',              '/setup/currencies/save',     'POST',   'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.CURRENCY.DELETE',     'Delete currency',            '/setup/currencies/delete/**','DELETE', 'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.TERMS.VIEW',          'View payment terms',         '/setup/terms/**',            'GET',    'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.TERMS.CREATE',        'Create payment term',        '/setup/terms/save',          'POST',   'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.TERMS.EDIT',          'Edit payment term',          '/setup/terms/save',          'POST',   'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.TERMS.DELETE',        'Delete payment term',        '/setup/terms/delete/**',     'DELETE', 'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.HS_CODE.VIEW',        'View HS codes',              '/setup/hs-codes/**',         'GET',    'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.HS_CODE.CREATE',      'Create HS code',             '/setup/hs-codes/save',       'POST',   'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.HS_CODE.EDIT',        'Edit HS code',               '/setup/hs-codes/save',       'POST',   'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
('SETUP.HS_CODE.DELETE',      'Delete HS code',             '/setup/hs-codes/delete/**',  'DELETE', 'CORE_SECURITY', 'SETUP', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── INVENTORY ────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('INV.UOM.VIEW',              'View UOMs',                  '/inventory/uoms/**',         'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.UOM.CREATE',            'Create UOM',                 '/inventory/uoms/save',       'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.UOM.EDIT',              'Edit UOM',                   '/inventory/uoms/save',       'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.UOM.DELETE',            'Delete UOM',                 '/inventory/uoms/delete/**',  'DELETE', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.CATEGORY.VIEW',         'View item categories',       '/inventory/categories/**',   'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.CATEGORY.CREATE',       'Create item category',       '/inventory/categories/save', 'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.CATEGORY.EDIT',         'Edit item category',         '/inventory/categories/save', 'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.CATEGORY.DELETE',       'Delete item category',       '/inventory/categories/delete/**','DELETE','INVENTORY_WAREHOUSE','INVENTORY',true,NOW(),NOW()),
('INV.BRAND.VIEW',            'View brands',                '/inventory/brands/**',       'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.BRAND.CREATE',          'Create brand',               '/inventory/brands/save',     'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.BRAND.EDIT',            'Edit brand',                 '/inventory/brands/save',     'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.BRAND.DELETE',          'Delete brand',               '/inventory/brands/delete/**','DELETE', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.MODEL.VIEW',            'View item models',           '/inventory/models/**',       'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.MODEL.CREATE',          'Create item model',          '/inventory/models/save',     'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.MODEL.EDIT',            'Edit item model',            '/inventory/models/save',     'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.MODEL.DELETE',          'Delete item model',          '/inventory/models/delete/**','DELETE', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.ITEM.VIEW',             'View items',                 '/inventory/items/**',        'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.ITEM.CREATE',           'Create item',                '/inventory/items/save',      'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.ITEM.EDIT',             'Edit item',                  '/inventory/items/save',      'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.ITEM.DELETE',           'Delete item',                '/inventory/items/delete/**', 'DELETE', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.STOCK.VIEW',            'View stock / lots',          '/inventory/stocks/**',       'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.STOCK_ADJ.VIEW',        'View stock adjustments',     '/inventory/adjustments/**',  'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.STOCK_ADJ.CREATE',      'Create stock adjustment',    '/inventory/adjustments/save','POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.STOCK_TRANSFER.VIEW',   'View stock transfers',       '/inventory/transfers/**',    'GET',    'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
('INV.STOCK_TRANSFER.CREATE', 'Create stock transfer',      '/inventory/transfers/save',  'POST',   'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── PURCHASE ─────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('PUR.SUPPLIER.VIEW',         'View suppliers',             '/purchase/suppliers/**',     'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.SUPPLIER.CREATE',       'Create supplier',            '/purchase/suppliers/save',   'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.SUPPLIER.EDIT',         'Edit supplier',              '/purchase/suppliers/save',   'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.SUPPLIER.DELETE',       'Delete supplier',            '/purchase/suppliers/delete/**','DELETE','PURCHASE_SUPPLIER','PURCHASE',true,NOW(),NOW()),
('PUR.PO.VIEW',               'View purchase orders',       '/purchase/orders/**',        'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.PO.CREATE',             'Create purchase order',      '/purchase/orders/save',      'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.PO.EDIT',               'Edit purchase order',        '/purchase/orders/save',      'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.PO.DELETE',             'Delete purchase order',      '/purchase/orders/delete/**', 'DELETE', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.PO.APPROVE',            'Approve purchase order',     '/purchase/orders/approve/**','POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.GRN.VIEW',              'View GRNs',                  '/purchase/grns/**',          'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.GRN.CREATE',            'Create GRN',                 '/purchase/grns/save',        'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.GRN.EDIT',              'Edit GRN',                   '/purchase/grns/save',        'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.GRN.DELETE',            'Delete GRN',                 '/purchase/grns/delete/**',   'DELETE', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.INVOICE.VIEW',          'View purchase invoices',     '/purchase/invoices/**',      'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.INVOICE.CREATE',        'Create purchase invoice',    '/purchase/invoices/save',    'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.INVOICE.EDIT',          'Edit purchase invoice',      '/purchase/invoices/save',    'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.INVOICE.DELETE',        'Delete purchase invoice',    '/purchase/invoices/delete/**','DELETE','PURCHASE_SUPPLIER','PURCHASE',true,NOW(),NOW()),
('PUR.DEBIT_NOTE.VIEW',       'View debit notes',           '/purchase/debit-notes/**',   'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.DEBIT_NOTE.CREATE',     'Create debit note',          '/purchase/debit-notes/save', 'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.DEBIT_NOTE.EDIT',       'Edit debit note',            '/purchase/debit-notes/save', 'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.PAYMENT.VIEW',          'View payment vouchers (purch)','/purchase/payments/**',     'GET',    'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.PAYMENT.CREATE',        'Create payment voucher',     '/purchase/payments/save',    'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
('PUR.PAYMENT.EDIT',          'Edit payment voucher',       '/purchase/payments/save',    'POST',   'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── SALES ────────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('SAL.CUSTOMER.VIEW',         'View customers',             '/sales/customers/**',        'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.CUSTOMER.CREATE',       'Create customer',            '/sales/customers/save',      'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.CUSTOMER.EDIT',         'Edit customer',              '/sales/customers/save',      'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.CUSTOMER.DELETE',       'Delete customer',            '/sales/customers/delete/**', 'DELETE', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.SO.VIEW',               'View sales orders',          '/sales/orders/**',           'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.SO.CREATE',             'Create sales order',         '/sales/orders/save',         'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.SO.EDIT',               'Edit sales order',           '/sales/orders/save',         'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.SO.DELETE',             'Delete sales order',         '/sales/orders/delete/**',    'DELETE', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.SO.APPROVE',            'Approve sales order',        '/sales/orders/approve/**',   'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.DELIVERY.VIEW',         'View delivery notes',        '/sales/deliveries/**',       'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.DELIVERY.CREATE',       'Create delivery note',       '/sales/deliveries/save',     'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.DELIVERY.EDIT',         'Edit delivery note',         '/sales/deliveries/save',     'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.DELIVERY.DELETE',       'Delete delivery note',       '/sales/deliveries/delete/**','DELETE', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.INVOICE.VIEW',          'View sales invoices',        '/sales/invoices/**',         'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.INVOICE.CREATE',        'Create sales invoice',       '/sales/invoices/save',       'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.INVOICE.EDIT',          'Edit sales invoice',         '/sales/invoices/save',       'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.INVOICE.DELETE',        'Delete sales invoice',       '/sales/invoices/delete/**',  'DELETE', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.CREDIT_NOTE.VIEW',      'View credit notes',          '/sales/credit-notes/**',     'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.CREDIT_NOTE.CREATE',    'Create credit note',         '/sales/credit-notes/save',   'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.CREDIT_NOTE.EDIT',      'Edit credit note',           '/sales/credit-notes/save',   'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.RECEIPT.VIEW',          'View receipt vouchers (sales)','/sales/receipts/**',        'GET',    'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.RECEIPT.CREATE',        'Create receipt voucher',     '/sales/receipts/save',       'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
('SAL.RECEIPT.EDIT',          'Edit receipt voucher',       '/sales/receipts/save',       'POST',   'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── FINANCE / ACCOUNTS ───────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, module, description, url_pattern, http_method, active) VALUES
-- Chart of Accounts
('accounts.coa.view',           'ACCOUNTS', 'View chart of accounts',       '/accounts/chart',               'GET', TRUE),
('accounts.coa.create',         'ACCOUNTS', 'Create chart account',         '/accounts/chart',               'POST',TRUE),
('accounts.coa.edit',           'ACCOUNTS', 'Edit chart account',           '/accounts/chart/**',            'POST',TRUE),
('accounts.coa.delete',         'ACCOUNTS', 'Delete chart account',         '/accounts/chart/**',            'DELETE',TRUE),
('accounts.coa.toggle',         'ACCOUNTS', 'Toggle chart account status',  '/accounts/chart/toggle/**',     'POST',TRUE),
-- Sub-accounts (all types)
('accounts.subledger.view',     'ACCOUNTS', 'View sub-ledger accounts',     '/accounts/sub-accounts',        'GET', TRUE),
('accounts.subledger.create',   'ACCOUNTS', 'Create sub-ledger account',    '/accounts/sub-accounts',        'POST',TRUE),
('accounts.subledger.edit',     'ACCOUNTS', 'Edit sub-ledger account',      '/accounts/sub-accounts/**',     'POST',TRUE),
('accounts.subledger.delete',   'ACCOUNTS', 'Delete sub-ledger account',    '/accounts/sub-accounts/**',     'DELETE',TRUE),
-- Bank / Cash
('accounts.bank.view',          'ACCOUNTS', 'View bank accounts',           '/accounts/bank-accounts',       'GET', TRUE),
('accounts.bank.create',        'ACCOUNTS', 'Create bank account',          '/accounts/bank-accounts',       'POST',TRUE),
('accounts.cash.view',          'ACCOUNTS', 'View cash accounts',           '/accounts/cash-accounts',       'GET', TRUE),
('accounts.cash.create',        'ACCOUNTS', 'Create cash account',          '/accounts/cash-accounts',       'POST',TRUE),
-- Customer / Supplier / Employee
('accounts.customer.view',      'ACCOUNTS', 'View customer accounts',       '/accounts/customer-accounts',   'GET', TRUE),
('accounts.customer.create',    'ACCOUNTS', 'Create customer account',      '/accounts/customer-accounts',   'POST',TRUE),
('accounts.supplier.view',      'ACCOUNTS', 'View supplier accounts',       '/accounts/supplier-accounts',   'GET', TRUE),
('accounts.supplier.create',    'ACCOUNTS', 'Create supplier account',      '/accounts/supplier-accounts',   'POST',TRUE),
('accounts.employee.view',      'ACCOUNTS', 'View employee accounts',       '/accounts/employee-accounts',   'GET', TRUE),
('accounts.employee.create',    'ACCOUNTS', 'Create employee account',      '/accounts/employee-accounts',   'POST',TRUE),
-- Accounting Periods
('accounts.period.view',        'ACCOUNTS', 'View accounting periods',      '/accounts/periods',             'GET', TRUE),
('accounts.period.create',      'ACCOUNTS', 'Create accounting period',     '/accounts/periods',             'POST',TRUE),
('accounts.period.close',       'ACCOUNTS', 'Close accounting period',      '/accounts/periods/close/**',    'POST',TRUE),
-- Journals / Vouchers
('accounts.journal.view',       'ACCOUNTS', 'View journal vouchers',        '/accounts/journals',            'GET', TRUE),
('accounts.journal.create',     'ACCOUNTS', 'Create journal voucher',       '/accounts/journals',            'POST',TRUE),
('accounts.journal.post',       'ACCOUNTS', 'Post journal voucher',         '/accounts/journals/post/**',    'POST',TRUE),
('accounts.journal.reverse',    'ACCOUNTS', 'Reverse journal voucher',      '/accounts/journals/reverse/**', 'POST',TRUE),
('accounts.journal.delete',     'ACCOUNTS', 'Delete journal voucher',       '/accounts/journals/**',         'DELETE',TRUE),
('accounts.payment.view',       'ACCOUNTS', 'View payment vouchers',        '/accounts/payment-vouchers',    'GET', TRUE),
('accounts.payment.create',     'ACCOUNTS', 'Create payment voucher',       '/accounts/payment-vouchers',    'POST',TRUE),
('accounts.receipt.view',       'ACCOUNTS', 'View receipt vouchers',        '/accounts/receipt-vouchers',    'GET', TRUE),
('accounts.receipt.create',     'ACCOUNTS', 'Create receipt voucher',       '/accounts/receipt-vouchers',    'POST',TRUE),
('accounts.contra.view',        'ACCOUNTS', 'View contra vouchers',         '/accounts/contra-vouchers',     'GET', TRUE),
('accounts.debitnote.view',     'ACCOUNTS', 'View debit notes',             '/accounts/debit-notes',         'GET', TRUE),
('accounts.creditnote.view',    'ACCOUNTS', 'View credit notes',            '/accounts/credit-notes',        'GET', TRUE),
('accounts.openingbal.view',    'ACCOUNTS', 'View opening balances',        '/accounts/opening-balances',    'GET', TRUE),
('accounts.openingbal.post',    'ACCOUNTS', 'Post opening balances',        '/accounts/opening-balances/post/**','POST',TRUE),
-- Config
('accounts.mapping.view',       'ACCOUNTS', 'View account mapping',         '/accounts/mapping',             'GET', TRUE),
('accounts.mapping.create',     'ACCOUNTS', 'Create account mapping',       '/accounts/mapping',             'POST',TRUE),
('accounts.policy.view',        'ACCOUNTS', 'View voucher policy',          '/accounts/policy',              'GET', TRUE),
('accounts.policy.create',      'ACCOUNTS', 'Create voucher policy',        '/accounts/policy',              'POST',TRUE),
('accounts.autotemplate.view',  'ACCOUNTS', 'View auto journal templates',  '/accounts/auto-templates',      'GET', TRUE),
('accounts.autotemplate.create','ACCOUNTS', 'Create auto journal template', '/accounts/auto-templates',      'POST',TRUE),
-- Reports
('accounts.report.trial',       'ACCOUNTS', 'View trial balance',           '/accounts/reports/trial-balance','GET',TRUE),
('accounts.report.ledger',      'ACCOUNTS', 'View account ledger',          '/accounts/reports/ledger',      'GET', TRUE),
('accounts.report.cashbook',    'ACCOUNTS', 'View cash book',               '/accounts/reports/cash-book',   'GET', TRUE),
('accounts.report.bankbook',    'ACCOUNTS', 'View bank book',               '/accounts/reports/bank-book',   'GET', TRUE)
ON CONFLICT (name) DO NOTHING;

-- ─── HRM ──────────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('HRM.DESIGNATION.VIEW',      'View designations',          '/hrm/designations/**',       'GET',    'HRM', 'HRM', true, NOW(), NOW()),
('HRM.DESIGNATION.CREATE',    'Create designation',         '/hrm/designations/save',     'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.DESIGNATION.EDIT',      'Edit designation',           '/hrm/designations/save',     'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.DESIGNATION.DELETE',    'Delete designation',         '/hrm/designations/delete/**','DELETE', 'HRM', 'HRM', true, NOW(), NOW()),
('HRM.EMPLOYEE.VIEW',         'View employees',             '/hrm/employees/**',          'GET',    'HRM', 'HRM', true, NOW(), NOW()),
('HRM.EMPLOYEE.CREATE',       'Create employee',            '/hrm/employees/save',        'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.EMPLOYEE.EDIT',         'Edit employee',              '/hrm/employees/save',        'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.EMPLOYEE.DELETE',       'Delete employee',            '/hrm/employees/delete/**',   'DELETE', 'HRM', 'HRM', true, NOW(), NOW()),
('HRM.ATTENDANCE.VIEW',       'View attendance',            '/hrm/attendance/**',         'GET',    'HRM', 'HRM', true, NOW(), NOW()),
('HRM.ATTENDANCE.CREATE',     'Create attendance',          '/hrm/attendance/save',       'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.ATTENDANCE.EDIT',       'Edit attendance',            '/hrm/attendance/save',       'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.LEAVE.VIEW',            'View leave applications',    '/hrm/leaves/**',             'GET',    'HRM', 'HRM', true, NOW(), NOW()),
('HRM.LEAVE.CREATE',          'Create leave application',   '/hrm/leaves/save',           'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.LEAVE.APPROVE',         'Approve leave application',  '/hrm/leaves/approve/**',     'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.PAYROLL.VIEW',          'View payroll runs',          '/hrm/payroll/**',            'GET',    'HRM', 'HRM', true, NOW(), NOW()),
('HRM.PAYROLL.CREATE',        'Process payroll',            '/hrm/payroll/save',          'POST',   'HRM', 'HRM', true, NOW(), NOW()),
('HRM.PAYROLL.APPROVE',       'Approve payroll',            '/hrm/payroll/approve/**',    'POST',   'HRM', 'HRM', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── PRODUCTION ───────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('PRD.ORDER.VIEW',            'View production orders',     '/production-orders/**',      'GET',    'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
('PRD.ORDER.CREATE',          'Create production order',    '/production-orders/save',    'POST',   'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
('PRD.ORDER.EDIT',            'Edit production order',      '/production-orders/save',    'POST',   'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
('PRD.ORDER.DELETE',          'Delete production order',    '/production-orders/delete/**','DELETE','PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
('PRD.ORDER.APPROVE',         'Approve production order',   '/production-orders/approve/**','POST', 'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
('PRD.MATERIAL_REQ.VIEW',     'View material requisitions', '/production/material-req/**','GET',    'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
('PRD.MATERIAL_REQ.CREATE',   'Create material requisition','/production/material-req/save','POST', 'PRODUCTION', 'PRODUCTION', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── COMMERCIAL / LC ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('COM.LC.VIEW',               'View letters of credit',     '/commercial/lc/**',          'GET',    'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
('COM.LC.CREATE',             'Open LC',                    '/commercial/lc/save',        'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
('COM.LC.EDIT',               'Amend LC',                   '/commercial/lc/save',        'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
('COM.LC.DELETE',             'Delete LC',                  '/commercial/lc/delete/**',   'DELETE', 'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
('COM.EXPORT.VIEW',           'View export documents',      '/commercial/exports/**',     'GET',    'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
('COM.EXPORT.CREATE',         'Create export document',     '/commercial/exports/save',   'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
('COM.IMPORT.VIEW',           'View import documents',      '/commercial/imports/**',     'GET',    'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
('COM.IMPORT.CREATE',         'Create import document',     '/commercial/imports/save',   'POST',   'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── APPROVAL ─────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('APR.CONFIG.VIEW',           'View approval configs',      '/approvals/configs/**',      'GET',    'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
('APR.CONFIG.CREATE',         'Create approval config',     '/approvals/configs/save',    'POST',   'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
('APR.CONFIG.EDIT',           'Edit approval config',       '/approvals/configs/save',    'POST',   'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
('APR.REQUEST.VIEW',          'View approval requests',     '/approvals/requests/**',     'GET',    'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
('APR.REQUEST.APPROVE',       'Approve requests',           '/approvals/requests/approve/**','POST','CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
('APR.REQUEST.REJECT',        'Reject requests',            '/approvals/requests/reject/**','POST', 'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
('APR.DELEGATION.VIEW',       'View approval delegations',  '/approvals/delegations/**',  'GET',    'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW()),
('APR.DELEGATION.CREATE',     'Create approval delegation', '/approvals/delegations/save','POST',   'CORE_SECURITY', 'APPROVAL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ─── DASHBOARD / REPORTS ──────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at) VALUES
('RPT.DASHBOARD.VIEW',        'View dashboard',             '/dashboard/**',              'GET',    'REPORTS_ANALYTICS', 'DASHBOARD', true, NOW(), NOW()),
('RPT.ACCOUNTS_DASH.VIEW',    'View accounts dashboard',    '/dashboard/accounts-dashboard/**','GET','REPORTS_ANALYTICS','DASHBOARD',true,NOW(),NOW()),
('RPT.REPORTS.VIEW',          'View reports module',        '/reports/**',                'GET',    'REPORTS_ANALYTICS', 'REPORTS',  true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- =============================================================================
-- 2.  ROLES
-- =============================================================================
INSERT INTO sec_roles (name, name_bn, description, master_role, active, created_at, updated_at) VALUES

('ROLE_SUPER_ADMIN','সুপার অ্যাডমিন','Full system access — bypasses all permission checks.','ROLE_SUPER_ADMIN', true, NOW(), NOW()),
('ROLE_ACCOUNTS_ADMIN',
 'হিসাব প্রশাসক',
 'Full access to Accounts, Finance, Reports. View-only on Purchase/Sales.',
 'ROLE_ACCOUNTS_ADMIN', true, NOW(), NOW()),

('ROLE_ACCOUNTANT',
 'হিসাবরক্ষক',
 'Journal entry, payment, receipt vouchers. View COA, ledger, reports.',
 'ROLE_ACCOUNTANT', true, NOW(), NOW()),

('ROLE_INVENTORY_MANAGER',
 'ইনভেন্টরি ম্যানেজার',
 'Full access to inventory items, stock, adjustments, transfers.',
 'ROLE_INVENTORY_MANAGER', true, NOW(), NOW()),

('ROLE_WAREHOUSE_STAFF',
 'ওয়্যারহাউস স্টাফ',
 'View-only stock + create GRN, stock transfer.',
 'ROLE_WAREHOUSE_STAFF', true, NOW(), NOW()),

('ROLE_PURCHASE_MANAGER',
 'ক্রয় ম্যানেজার',
 'Full purchase cycle: supplier, PO, GRN, invoice, payment, returns.',
 'ROLE_PURCHASE_MANAGER', true, NOW(), NOW()),

('ROLE_PURCHASE_OFFICER',
 'ক্রয় কর্মকর্তা',
 'Create/edit PO, GRN. View invoices, payments.',
 'ROLE_PURCHASE_OFFICER', true, NOW(), NOW()),

('ROLE_SALES_MANAGER',
 'বিক্রয় ম্যানেজার',
 'Full sales cycle: customer, SO, delivery, invoice, receipt, returns.',
 'ROLE_SALES_MANAGER', true, NOW(), NOW()),

('ROLE_SALES_EXECUTIVE',
 'বিক্রয় নির্বাহী',
 'Create/edit SO, view delivery, invoices, receipts.',
 'ROLE_SALES_EXECUTIVE', true, NOW(), NOW()),

('ROLE_HRM',
 'এইচআরএম ম্যানেজার',
 'Full HRM access: employees, attendance, leave, payroll.',
 'ROLE_HRM', true, NOW(), NOW()),

('ROLE_PRODUCTION_MANAGER',
 'উৎপাদন ম্যানেজার',
 'Full production access: orders, material requisitions, work orders.',
 'ROLE_PRODUCTION_MANAGER', true, NOW(), NOW()),

('ROLE_PRODUCTION_SUPERVISOR',
 'উৎপাদন সুপারভাইজার',
 'View production orders, create material requisitions.',
 'ROLE_PRODUCTION_SUPERVISOR', true, NOW(), NOW()),

('ROLE_COMMERCIAL_MANAGER',
 'বাণিজ্যিক ম্যানেজার',
 'Full commercial/LC access: LC open, amend, export, import documents.',
 'ROLE_COMMERCIAL_MANAGER', true, NOW(), NOW()),

('ROLE_COMMERCIAL_EXECUTIVE',
 'বাণিজ্যিক নির্বাহী',
 'View and create commercial/export/import documents.',
 'ROLE_COMMERCIAL_EXECUTIVE', true, NOW(), NOW())

ON CONFLICT (name) DO NOTHING;


-- =============================================================================
-- 3.  ROLE ↔ PERMISSION  (sec_role_permissions)
--     Uses subqueries — order-independent.
-- =============================================================================

-- ── ROLE_SUPER_ADMIN — only the wildcard ─────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r, sec_permissions p
WHERE r.name = 'ROLE_SUPER_ADMIN' AND p.name = '*'
ON CONFLICT DO NOTHING;

-- ── ROLE_ACCOUNTS_ADMIN ───────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW','RPT.ACCOUNTS_DASH.VIEW','RPT.REPORTS.VIEW',
    'ACC.COA.VIEW','ACC.COA.CREATE','ACC.COA.EDIT','ACC.COA.DELETE',
    'ACC.SUBACC.VIEW','ACC.SUBACC.CREATE','ACC.SUBACC.EDIT',
    'ACC.BANK_ACC.VIEW','ACC.BANK_ACC.CREATE','ACC.BANK_ACC.EDIT',
    'ACC.MAPPING.VIEW','ACC.MAPPING.EDIT',
    'ACC.JOURNAL.VIEW','ACC.JOURNAL.CREATE','ACC.JOURNAL.EDIT','ACC.JOURNAL.DELETE',
    'ACC.PAYMENT.VIEW','ACC.PAYMENT.CREATE','ACC.PAYMENT.EDIT',
    'ACC.RECEIPT.VIEW','ACC.RECEIPT.CREATE','ACC.RECEIPT.EDIT',
    'ACC.POLICY.VIEW','ACC.POLICY.CREATE','ACC.POLICY.EDIT',
    'ACC.LEDGER.VIEW','ACC.TRIAL_BAL.VIEW','ACC.PROFIT_LOSS.VIEW','ACC.BALANCE_SHEET.VIEW',
    'PUR.INVOICE.VIEW','PUR.PAYMENT.VIEW',
    'SAL.INVOICE.VIEW','SAL.RECEIPT.VIEW',
    'SETUP.BANK.VIEW','SETUP.CURRENCY.VIEW','SETUP.TERMS.VIEW',
    'APR.REQUEST.VIEW','APR.REQUEST.APPROVE','APR.REQUEST.REJECT'
  )
WHERE r.name = 'ROLE_ACCOUNTS_ADMIN'
ON CONFLICT DO NOTHING;

-- ── ROLE_ACCOUNTANT ───────────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW','RPT.ACCOUNTS_DASH.VIEW',
    'ACC.COA.VIEW','ACC.SUBACC.VIEW','ACC.BANK_ACC.VIEW','ACC.MAPPING.VIEW',
    'ACC.JOURNAL.VIEW','ACC.JOURNAL.CREATE','ACC.JOURNAL.EDIT',
    'ACC.PAYMENT.VIEW','ACC.PAYMENT.CREATE','ACC.PAYMENT.EDIT',
    'ACC.RECEIPT.VIEW','ACC.RECEIPT.CREATE','ACC.RECEIPT.EDIT',
    'ACC.LEDGER.VIEW','ACC.TRIAL_BAL.VIEW','ACC.PROFIT_LOSS.VIEW','ACC.BALANCE_SHEET.VIEW',
    'PUR.INVOICE.VIEW','PUR.PAYMENT.VIEW',
    'SAL.INVOICE.VIEW','SAL.RECEIPT.VIEW',
    'SETUP.BANK.VIEW','SETUP.CURRENCY.VIEW','SETUP.TERMS.VIEW',
    'APR.REQUEST.VIEW','APR.REQUEST.APPROVE','APR.REQUEST.REJECT'
  )
WHERE r.name = 'ROLE_ACCOUNTANT'
ON CONFLICT DO NOTHING;

-- ── ROLE_INVENTORY_MANAGER ────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'INV.UOM.VIEW','INV.UOM.CREATE','INV.UOM.EDIT','INV.UOM.DELETE',
    'INV.CATEGORY.VIEW','INV.CATEGORY.CREATE','INV.CATEGORY.EDIT','INV.CATEGORY.DELETE',
    'INV.BRAND.VIEW','INV.BRAND.CREATE','INV.BRAND.EDIT','INV.BRAND.DELETE',
    'INV.MODEL.VIEW','INV.MODEL.CREATE','INV.MODEL.EDIT','INV.MODEL.DELETE',
    'INV.ITEM.VIEW','INV.ITEM.CREATE','INV.ITEM.EDIT','INV.ITEM.DELETE',
    'INV.STOCK.VIEW',
    'INV.STOCK_ADJ.VIEW','INV.STOCK_ADJ.CREATE',
    'INV.STOCK_TRANSFER.VIEW','INV.STOCK_TRANSFER.CREATE',
    'ORG.WAREHOUSE.VIEW',
    'SETUP.HS_CODE.VIEW','SETUP.HS_CODE.CREATE','SETUP.HS_CODE.EDIT',
    'APR.REQUEST.VIEW','APR.REQUEST.APPROVE','APR.REQUEST.REJECT'
  )
WHERE r.name = 'ROLE_INVENTORY_MANAGER'
ON CONFLICT DO NOTHING;

-- ── ROLE_WAREHOUSE_STAFF ──────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'INV.ITEM.VIEW','INV.STOCK.VIEW',
    'INV.STOCK_TRANSFER.VIEW','INV.STOCK_TRANSFER.CREATE',
    'PUR.GRN.VIEW','PUR.GRN.CREATE',
    'ORG.WAREHOUSE.VIEW'
  )
WHERE r.name = 'ROLE_WAREHOUSE_STAFF'
ON CONFLICT DO NOTHING;

-- ── ROLE_PURCHASE_MANAGER ─────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'PUR.SUPPLIER.VIEW','PUR.SUPPLIER.CREATE','PUR.SUPPLIER.EDIT','PUR.SUPPLIER.DELETE',
    'PUR.PO.VIEW','PUR.PO.CREATE','PUR.PO.EDIT','PUR.PO.DELETE','PUR.PO.APPROVE',
    'PUR.GRN.VIEW','PUR.GRN.CREATE','PUR.GRN.EDIT','PUR.GRN.DELETE',
    'PUR.INVOICE.VIEW','PUR.INVOICE.CREATE','PUR.INVOICE.EDIT','PUR.INVOICE.DELETE',
    'PUR.DEBIT_NOTE.VIEW','PUR.DEBIT_NOTE.CREATE','PUR.DEBIT_NOTE.EDIT',
    'PUR.PAYMENT.VIEW','PUR.PAYMENT.CREATE','PUR.PAYMENT.EDIT',
    'INV.ITEM.VIEW','INV.STOCK.VIEW',
    'SETUP.BANK.VIEW','SETUP.CURRENCY.VIEW','SETUP.TERMS.VIEW','SETUP.HS_CODE.VIEW',
    'APR.REQUEST.VIEW','APR.REQUEST.APPROVE','APR.REQUEST.REJECT'
  )
WHERE r.name = 'ROLE_PURCHASE_MANAGER'
ON CONFLICT DO NOTHING;

-- ── ROLE_PURCHASE_OFFICER ─────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'PUR.SUPPLIER.VIEW',
    'PUR.PO.VIEW','PUR.PO.CREATE','PUR.PO.EDIT',
    'PUR.GRN.VIEW','PUR.GRN.CREATE','PUR.GRN.EDIT',
    'PUR.INVOICE.VIEW','PUR.DEBIT_NOTE.VIEW','PUR.PAYMENT.VIEW',
    'INV.ITEM.VIEW','INV.STOCK.VIEW',
    'SETUP.TERMS.VIEW','SETUP.HS_CODE.VIEW',
    'APR.REQUEST.VIEW'
  )
WHERE r.name = 'ROLE_PURCHASE_OFFICER'
ON CONFLICT DO NOTHING;

-- ── ROLE_SALES_MANAGER ────────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'SAL.CUSTOMER.VIEW','SAL.CUSTOMER.CREATE','SAL.CUSTOMER.EDIT','SAL.CUSTOMER.DELETE',
    'SAL.SO.VIEW','SAL.SO.CREATE','SAL.SO.EDIT','SAL.SO.DELETE','SAL.SO.APPROVE',
    'SAL.DELIVERY.VIEW','SAL.DELIVERY.CREATE','SAL.DELIVERY.EDIT','SAL.DELIVERY.DELETE',
    'SAL.INVOICE.VIEW','SAL.INVOICE.CREATE','SAL.INVOICE.EDIT','SAL.INVOICE.DELETE',
    'SAL.CREDIT_NOTE.VIEW','SAL.CREDIT_NOTE.CREATE','SAL.CREDIT_NOTE.EDIT',
    'SAL.RECEIPT.VIEW','SAL.RECEIPT.CREATE','SAL.RECEIPT.EDIT',
    'INV.ITEM.VIEW','INV.STOCK.VIEW',
    'SETUP.BANK.VIEW','SETUP.CURRENCY.VIEW','SETUP.TERMS.VIEW','SETUP.HS_CODE.VIEW',
    'APR.REQUEST.VIEW','APR.REQUEST.APPROVE','APR.REQUEST.REJECT'
  )
WHERE r.name = 'ROLE_SALES_MANAGER'
ON CONFLICT DO NOTHING;

-- ── ROLE_SALES_EXECUTIVE ──────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'SAL.CUSTOMER.VIEW',
    'SAL.SO.VIEW','SAL.SO.CREATE','SAL.SO.EDIT',
    'SAL.DELIVERY.VIEW','SAL.INVOICE.VIEW','SAL.RECEIPT.VIEW',
    'SAL.CREDIT_NOTE.VIEW',
    'INV.ITEM.VIEW','INV.STOCK.VIEW',
    'SETUP.TERMS.VIEW',
    'APR.REQUEST.VIEW'
  )
WHERE r.name = 'ROLE_SALES_EXECUTIVE'
ON CONFLICT DO NOTHING;

-- ── ROLE_HRM ──────────────────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'HRM.DESIGNATION.VIEW','HRM.DESIGNATION.CREATE','HRM.DESIGNATION.EDIT','HRM.DESIGNATION.DELETE',
    'HRM.EMPLOYEE.VIEW','HRM.EMPLOYEE.CREATE','HRM.EMPLOYEE.EDIT','HRM.EMPLOYEE.DELETE',
    'HRM.ATTENDANCE.VIEW','HRM.ATTENDANCE.CREATE','HRM.ATTENDANCE.EDIT',
    'HRM.LEAVE.VIEW','HRM.LEAVE.CREATE','HRM.LEAVE.APPROVE',
    'HRM.PAYROLL.VIEW','HRM.PAYROLL.CREATE','HRM.PAYROLL.APPROVE',
    'ORG.DEPARTMENT.VIEW',
    'APR.REQUEST.VIEW','APR.REQUEST.APPROVE','APR.REQUEST.REJECT'
  )
WHERE r.name = 'ROLE_HRM'
ON CONFLICT DO NOTHING;

-- ── ROLE_PRODUCTION_MANAGER ───────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'PRD.ORDER.VIEW','PRD.ORDER.CREATE','PRD.ORDER.EDIT','PRD.ORDER.DELETE','PRD.ORDER.APPROVE',
    'PRD.MATERIAL_REQ.VIEW','PRD.MATERIAL_REQ.CREATE',
    'INV.ITEM.VIEW','INV.STOCK.VIEW',
    'APR.REQUEST.VIEW','APR.REQUEST.APPROVE','APR.REQUEST.REJECT'
  )
WHERE r.name = 'ROLE_PRODUCTION_MANAGER'
ON CONFLICT DO NOTHING;

-- ── ROLE_PRODUCTION_SUPERVISOR ────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'PRD.ORDER.VIEW','PRD.MATERIAL_REQ.VIEW','PRD.MATERIAL_REQ.CREATE',
    'INV.ITEM.VIEW','INV.STOCK.VIEW',
    'APR.REQUEST.VIEW'
  )
WHERE r.name = 'ROLE_PRODUCTION_SUPERVISOR'
ON CONFLICT DO NOTHING;

-- ── ROLE_COMMERCIAL_MANAGER ───────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'COM.LC.VIEW','COM.LC.CREATE','COM.LC.EDIT','COM.LC.DELETE',
    'COM.EXPORT.VIEW','COM.EXPORT.CREATE',
    'COM.IMPORT.VIEW','COM.IMPORT.CREATE',
    'SETUP.BANK.VIEW','SETUP.CURRENCY.VIEW','SETUP.TERMS.VIEW','SETUP.HS_CODE.VIEW',
    'APR.REQUEST.VIEW','APR.REQUEST.APPROVE','APR.REQUEST.REJECT'
  )
WHERE r.name = 'ROLE_COMMERCIAL_MANAGER'
ON CONFLICT DO NOTHING;

-- ── ROLE_COMMERCIAL_EXECUTIVE ─────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sec_roles r JOIN sec_permissions p
  ON p.name IN (
    'RPT.DASHBOARD.VIEW',
    'COM.LC.VIEW',
    'COM.EXPORT.VIEW','COM.EXPORT.CREATE',
    'COM.IMPORT.VIEW','COM.IMPORT.CREATE',
    'SETUP.BANK.VIEW','SETUP.CURRENCY.VIEW','SETUP.TERMS.VIEW',
    'APR.REQUEST.VIEW'
  )
WHERE r.name = 'ROLE_COMMERCIAL_EXECUTIVE'
ON CONFLICT DO NOTHING;


-- =============================================================================
-- 4.  APP_MENUS
--     menuType: MODULE | GROUP | LEAF
--     Insert order: MODULE → GROUP → LEAF (parent must exist before child)
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 4A. MODULE-LEVEL  (top-level nav items — parentId IS NULL)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
VALUES
('MOD_DASHBOARD',   'Dashboard',    '/dashboard',   'fa fa-th-large',       NULL, 10,  'MODULE', 'DASHBOARD',  'RPT.DASHBOARD.VIEW',      '_self', true, true, false, NOW(), NOW()),
('MOD_INVENTORY',   'Inventory',    NULL,           'fa fa-boxes',          NULL, 20,  'MODULE', 'INVENTORY',  NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_PURCHASE',    'Purchase',     NULL,           'fa fa-shopping-cart',  NULL, 30,  'MODULE', 'PURCHASE',   NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_SALES',       'Sales',        NULL,           'fa fa-tags',           NULL, 40,  'MODULE', 'SALES',      NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_ACCOUNTS',    'Accounts',     NULL,           'fa fa-calculator',     NULL, 50,  'MODULE', 'ACCOUNTS',   NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_HRM',         'HRM',          NULL,           'fa fa-users',          NULL, 60,  'MODULE', 'HRM',        NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_PRODUCTION',  'Production',   NULL,           'fa fa-industry',       NULL, 70,  'MODULE', 'PRODUCTION', NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_COMMERCIAL',  'Commercial',   NULL,           'fa fa-ship',           NULL, 80,  'MODULE', 'COMMERCIAL', NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_SETUP',       'Setup',        NULL,           'fa fa-cogs',           NULL, 90,  'MODULE', 'SETUP',      NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_SECURITY',    'Security',     NULL,           'fa fa-shield-alt',     NULL, 100, 'MODULE', 'SECURITY',   NULL,                      '_self', true, true, false, NOW(), NOW()),
('MOD_APPROVALS',   'Approvals',    NULL,           'fa fa-tasks',          NULL, 110, 'MODULE', 'APPROVALS',  'APR.REQUEST.VIEW',        '_self', true, true, false, NOW(), NOW()),
('MOD_REPORTS',     'Reports',      '/reports',     'fa fa-chart-bar',      NULL, 120, 'MODULE', 'REPORTS',    'RPT.REPORTS.VIEW',        '_self', true, true, false, NOW(), NOW())
ON CONFLICT (menu_code) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────────────────
-- 4B. GROUP-LEVEL  (second level — parentId = MODULE id)
-- ─────────────────────────────────────────────────────────────────────────────

-- INVENTORY groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_INV_MASTER', 'Item Master', NULL, 'fa fa-layer-group',
       m.id, 10, 'GROUP', 'INVENTORY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_INVENTORY'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_INV_STOCK', 'Stock Management', NULL, 'fa fa-warehouse',
       m.id, 20, 'GROUP', 'INVENTORY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_INVENTORY'
ON CONFLICT (menu_code) DO NOTHING;

-- PURCHASE groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_MASTER', 'Suppliers', NULL, 'fa fa-truck',
       m.id, 10, 'GROUP', 'PURCHASE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PURCHASE'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_CYCLE', 'Purchase Cycle', NULL, 'fa fa-file-invoice',
       m.id, 20, 'GROUP', 'PURCHASE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PURCHASE'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_RETURNS', 'Returns & Payments', NULL, 'fa fa-undo',
       m.id, 30, 'GROUP', 'PURCHASE', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PURCHASE'
ON CONFLICT (menu_code) DO NOTHING;

-- SALES groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_MASTER', 'Customers', NULL, 'fa fa-user-tie',
       m.id, 10, 'GROUP', 'SALES', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SALES'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_CYCLE', 'Sales Cycle', NULL, 'fa fa-file-invoice-dollar',
       m.id, 20, 'GROUP', 'SALES', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SALES'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_RETURNS', 'Returns & Receipts', NULL, 'fa fa-undo-alt',
       m.id, 30, 'GROUP', 'SALES', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SALES'
ON CONFLICT (menu_code) DO NOTHING;

-- ACCOUNTS groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_MASTER', 'Chart & Accounts', NULL, 'fa fa-list-alt',
       m.id, 10, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_VOUCHERS', 'Vouchers', NULL, 'fa fa-receipt',
       m.id, 20, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_REPORTS', 'Financial Reports', NULL, 'fa fa-chart-line',
       m.id, 30, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_CONFIG', 'Accounts Config', NULL, 'fa fa-sliders-h',
       m.id, 40, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;

-- HRM groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_MASTER', 'HR Master', NULL, 'fa fa-id-card',
       m.id, 10, 'GROUP', 'HRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_HRM'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_ATTENDANCE', 'Attendance & Leave', NULL, 'fa fa-calendar-check',
       m.id, 20, 'GROUP', 'HRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_HRM'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_PAYROLL', 'Payroll', NULL, 'fa fa-money-bill-wave',
       m.id, 30, 'GROUP', 'HRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_HRM'
ON CONFLICT (menu_code) DO NOTHING;

-- PRODUCTION groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PRD_ORDERS', 'Production Orders', NULL, 'fa fa-clipboard-list',
       m.id, 10, 'GROUP', 'PRODUCTION', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PRODUCTION'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PRD_MATERIALS', 'Materials', NULL, 'fa fa-cubes',
       m.id, 20, 'GROUP', 'PRODUCTION', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_PRODUCTION'
ON CONFLICT (menu_code) DO NOTHING;

-- COMMERCIAL groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_COM_LC', 'Letter of Credit', NULL, 'fa fa-file-contract',
       m.id, 10, 'GROUP', 'COMMERCIAL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_COMMERCIAL'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_COM_TRADE', 'Trade Documents', NULL, 'fa fa-globe',
       m.id, 20, 'GROUP', 'COMMERCIAL', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_COMMERCIAL'
ON CONFLICT (menu_code) DO NOTHING;

-- SETUP groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_STP_ORG', 'Organization', NULL, 'fa fa-building',
       m.id, 10, 'GROUP', 'SETUP', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SETUP'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_STP_REF', 'Reference Data', NULL, 'fa fa-book',
       m.id, 20, 'GROUP', 'SETUP', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SETUP'
ON CONFLICT (menu_code) DO NOTHING;

-- SECURITY groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SEC_USER', 'Users & Roles', NULL, 'fa fa-user-shield',
       m.id, 10, 'GROUP', 'SECURITY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SECURITY'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SEC_MENU', 'Menu & Permissions', NULL, 'fa fa-sitemap',
       m.id, 20, 'GROUP', 'SECURITY', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_SECURITY'
ON CONFLICT (menu_code) DO NOTHING;

-- APPROVALS groups
INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_APR_CONFIG', 'Approval Setup', NULL, 'fa fa-tools',
       m.id, 10, 'GROUP', 'APPROVALS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_APPROVALS'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus
  (menu_code, menu_name, menu_url, icon, parent_id, display_order,
   menu_type, module_name, required_permission, target,
   active, visible, deleted, created_at, updated_at)
SELECT 'GRP_APR_PENDING', 'My Approvals', NULL, 'fa fa-clock',
       m.id, 20, 'GROUP', 'APPROVALS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_APPROVALS'
ON CONFLICT (menu_code) DO NOTHING;


-- ─────────────────────────────────────────────────────────────────────────────
-- 4C. LEAF-LEVEL  (actual clickable pages)
-- ─────────────────────────────────────────────────────────────────────────────

-- ── INVENTORY MASTER leaves ───────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_UOM',       'Units of Measure',  '/inventory/uoms',       'fa fa-ruler', g.id, 10, 'LEAF', 'INVENTORY', 'INV.UOM.VIEW',      '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_INV_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_CATEGORY',  'Item Categories',   '/inventory/categories', 'fa fa-sitemap', g.id, 20, 'LEAF', 'INVENTORY', 'INV.CATEGORY.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_INV_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_BRAND',     'Brands',            '/inventory/brands',     'fa fa-trademark', g.id, 30, 'LEAF', 'INVENTORY', 'INV.BRAND.VIEW',   '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_INV_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_MODEL',     'Item Models',       '/inventory/models',     'fa fa-cube', g.id, 40, 'LEAF', 'INVENTORY', 'INV.MODEL.VIEW',     '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_INV_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_ITEM',      'Items / Products',  '/inventory/items',      'fa fa-boxes', g.id, 50, 'LEAF', 'INVENTORY', 'INV.ITEM.VIEW',      '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_INV_MASTER' ON CONFLICT (menu_code) DO NOTHING;

-- ── INVENTORY STOCK leaves ────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_STOCK',     'Stock Ledger',      '/inventory/stocks',     'fa fa-dolly', g.id, 10, 'LEAF', 'INVENTORY', 'INV.STOCK.VIEW',     '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_INV_STOCK' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_STOCK_ADJ', 'Stock Adjustments', '/inventory/adjustments', 'fa fa-balance-scale', g.id, 20, 'LEAF', 'INVENTORY', 'INV.STOCK_ADJ.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_INV_STOCK' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_TRANSFER',  'Stock Transfer',    '/inventory/transfers',  'fa fa-exchange-alt', g.id, 30, 'LEAF', 'INVENTORY', 'INV.STOCK_TRANSFER.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_INV_STOCK' ON CONFLICT (menu_code) DO NOTHING;

-- ── PURCHASE MASTER leaves ────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_SUPPLIER',  'Suppliers',         '/purchase/suppliers',   'fa fa-truck-loading', g.id, 10, 'LEAF', 'PURCHASE', 'PUR.SUPPLIER.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_PUR_MASTER' ON CONFLICT (menu_code) DO NOTHING;

-- ── PURCHASE CYCLE leaves ─────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_PO',        'Purchase Orders',   '/purchase/orders',      'fa fa-file-alt', g.id, 10, 'LEAF', 'PURCHASE', 'PUR.PO.VIEW',       '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_PUR_CYCLE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_GRN',       'Goods Receipt (GRN)','/purchase/grns',        'fa fa-clipboard-check', g.id, 20, 'LEAF', 'PURCHASE', 'PUR.GRN.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_PUR_CYCLE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_INVOICE',   'Purchase Invoices', '/purchase/invoices',    'fa fa-file-invoice', g.id, 30, 'LEAF', 'PURCHASE', 'PUR.INVOICE.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_PUR_CYCLE' ON CONFLICT (menu_code) DO NOTHING;

-- ── PURCHASE RETURNS / PAYMENTS leaves ────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_DEBIT_NOTE','Debit Notes',       '/purchase/debit-notes', 'fa fa-file-minus', g.id, 10, 'LEAF', 'PURCHASE', 'PUR.DEBIT_NOTE.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_PUR_RETURNS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_PAYMENT',   'Payment Vouchers',  '/purchase/payments',    'fa fa-money-check', g.id, 20, 'LEAF', 'PURCHASE', 'PUR.PAYMENT.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_PUR_RETURNS' ON CONFLICT (menu_code) DO NOTHING;

-- ── SALES MASTER leaves ───────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_CUSTOMER',  'Customers',         '/sales/customers',      'fa fa-user-friends', g.id, 10, 'LEAF', 'SALES', 'SAL.CUSTOMER.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SAL_MASTER' ON CONFLICT (menu_code) DO NOTHING;

-- ── SALES CYCLE leaves ────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_SO',        'Sales Orders',      '/sales/orders',         'fa fa-file-alt', g.id, 10, 'LEAF', 'SALES', 'SAL.SO.VIEW',        '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SAL_CYCLE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_DELIVERY',  'Delivery Notes',    '/sales/deliveries',     'fa fa-shipping-fast', g.id, 20, 'LEAF', 'SALES', 'SAL.DELIVERY.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SAL_CYCLE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_INVOICE',   'Sales Invoices',    '/sales/invoices',       'fa fa-file-invoice-dollar', g.id, 30, 'LEAF', 'SALES', 'SAL.INVOICE.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SAL_CYCLE' ON CONFLICT (menu_code) DO NOTHING;

-- ── SALES RETURNS / RECEIPTS leaves ──────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_CREDIT_NOTE','Credit Notes',     '/sales/credit-notes',   'fa fa-file-minus', g.id, 10, 'LEAF', 'SALES', 'SAL.CREDIT_NOTE.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SAL_RETURNS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_RECEIPT',   'Receipt Vouchers',  '/sales/receipts',       'fa fa-money-bill', g.id, 20, 'LEAF', 'SALES', 'SAL.RECEIPT.VIEW',  '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SAL_RETURNS' ON CONFLICT (menu_code) DO NOTHING;

-- ── ACCOUNTS MASTER leaves ────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_COA',       'Chart of Accounts', '/accounts/chart',       'fa fa-project-diagram', g.id, 10, 'LEAF', 'ACCOUNTS', 'ACC.COA.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_SUBACC',    'Sub-Accounts',      '/accounts/sub-accounts',  'fa fa-code-branch', g.id, 20, 'LEAF', 'ACCOUNTS', 'ACC.SUBACC.VIEW',  '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_BANK_ACC',  'Bank Accounts',     '/accounts/bank-accounts','fa fa-university', g.id, 30, 'LEAF', 'ACCOUNTS', 'ACC.BANK_ACC.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_MASTER' ON CONFLICT (menu_code) DO NOTHING;

-- ── ACCOUNTS VOUCHERS leaves ──────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_JOURNAL',   'Journal Vouchers',  '/accounts/journals',    'fa fa-book-open', g.id, 10, 'LEAF', 'ACCOUNTS', 'ACC.JOURNAL.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_VOUCHERS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_PAYMENT',   'Payment Vouchers',  '/accounts/payment-vouchers',    'fa fa-hand-holding-usd', g.id, 20, 'LEAF', 'ACCOUNTS', 'ACC.PAYMENT.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_VOUCHERS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_RECEIPT',   'Receipt Vouchers',  '/accounts/receipt-vouchers',    'fa fa-hand-holding', g.id, 30, 'LEAF', 'ACCOUNTS', 'ACC.RECEIPT.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_VOUCHERS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_CONTRA',   'Contra Vouchers',  '/accounts/contra-vouchers',    'fa fa-hand-holding', g.id, 40, 'LEAF', 'ACCOUNTS', 'ACC.CONTRA.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_VOUCHERS' ON CONFLICT (menu_code) DO NOTHING;

-- ── ACCOUNTS REPORTS leaves ───────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_LEDGER',    'General Ledger',    '/accounts/ledger',      'fa fa-book', g.id, 10, 'LEAF', 'ACCOUNTS', 'ACC.LEDGER.VIEW',       '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_TRIAL_BAL', 'Trial Balance',     '/accounts/trial-balance','fa fa-balance-scale', g.id, 20, 'LEAF', 'ACCOUNTS', 'ACC.TRIAL_BAL.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_PNL',       'Profit & Loss',     '/accounts/profit-loss', 'fa fa-chart-line', g.id, 30, 'LEAF', 'ACCOUNTS', 'ACC.PROFIT_LOSS.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_BAL_SHEET', 'Balance Sheet',     '/accounts/balance-sheet','fa fa-file-chart-pie', g.id, 40, 'LEAF', 'ACCOUNTS', 'ACC.BALANCE_SHEET.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS' ON CONFLICT (menu_code) DO NOTHING;

-- ── ACCOUNTS CONFIG leaves ────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_MAPPING',   'Account Mappings',  '/accounts/mapping',    'fa fa-link', g.id, 10, 'LEAF', 'ACCOUNTS', 'ACC.MAPPING.VIEW',  '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_CONFIG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_POLICY',    'Accounts Policies', '/accounts/policy',    'fa fa-clipboard', g.id, 20, 'LEAF', 'ACCOUNTS', 'ACC.POLICY.VIEW',   '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_CONFIG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus ( menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_PERIODS', 'Accounting Periods', '/accounts/periods', 'fa fa-calendar-alt', g.id, 30, 'LEAF', 'ACCOUNTS', 'ACC.PERIOD.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_ACC_CONFIG' ON CONFLICT (menu_code) DO NOTHING;

-- ── HRM MASTER leaves ─────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_DESIG',     'Designations',      '/hrm/designations',     'fa fa-id-badge', g.id, 10, 'LEAF', 'HRM', 'HRM.DESIGNATION.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_HRM_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_EMPLOYEE',  'Employees',         '/hrm/employees',        'fa fa-user-tie', g.id, 20, 'LEAF', 'HRM', 'HRM.EMPLOYEE.VIEW',     '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_HRM_MASTER' ON CONFLICT (menu_code) DO NOTHING;

-- ── HRM ATTENDANCE & LEAVE leaves ─────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_ATTENDANCE','Attendance',        '/hrm/attendance',       'fa fa-calendar-alt', g.id, 10, 'LEAF', 'HRM', 'HRM.ATTENDANCE.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_HRM_ATTENDANCE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_LEAVE',     'Leave Applications','/hrm/leaves',           'fa fa-calendar-times', g.id, 20, 'LEAF', 'HRM', 'HRM.LEAVE.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_HRM_ATTENDANCE' ON CONFLICT (menu_code) DO NOTHING;

-- ── HRM PAYROLL leaves ────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_PAYROLL',   'Payroll Processing','/hrm/payroll',          'fa fa-money-bill-alt', g.id, 10, 'LEAF', 'HRM', 'HRM.PAYROLL.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_HRM_PAYROLL' ON CONFLICT (menu_code) DO NOTHING;

-- ── PRODUCTION ORDERS leaves ──────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PRD_ORDER',     'Production Orders', '/production-orders',    'fa fa-cogs', g.id, 10, 'LEAF', 'PRODUCTION', 'PRD.ORDER.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_PRD_ORDERS' ON CONFLICT (menu_code) DO NOTHING;

-- ── PRODUCTION MATERIALS leaves ───────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PRD_MAT_REQ',   'Material Requisitions','/production/material-req','fa fa-cubes', g.id, 10, 'LEAF', 'PRODUCTION', 'PRD.MATERIAL_REQ.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_PRD_MATERIALS' ON CONFLICT (menu_code) DO NOTHING;

-- ── COMMERCIAL LC leaves ──────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_LC',        'Letters of Credit', '/commercial/lc',        'fa fa-file-contract', g.id, 10, 'LEAF', 'COMMERCIAL', 'COM.LC.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_COM_LC' ON CONFLICT (menu_code) DO NOTHING;

-- ── COMMERCIAL TRADE leaves ───────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_EXPORT',    'Export Documents',  '/commercial/exports',   'fa fa-plane-departure', g.id, 10, 'LEAF', 'COMMERCIAL', 'COM.EXPORT.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_COM_TRADE' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_IMPORT',    'Import Documents',  '/commercial/imports',   'fa fa-plane-arrival', g.id, 20, 'LEAF', 'COMMERCIAL', 'COM.IMPORT.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_COM_TRADE' ON CONFLICT (menu_code) DO NOTHING;

-- ── SETUP ORGANIZATION leaves ─────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_ORG',       'Organizations',     '/organizations',        'fa fa-building', g.id, 10, 'LEAF', 'SETUP', 'ORG.ORGANIZATION.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_ORG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_BU',        'Business Units',    '/business-units',       'fa fa-briefcase', g.id, 20, 'LEAF', 'SETUP', 'ORG.BUSINESS_UNIT.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_ORG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_DEPT',      'Departments',       '/departments',          'fa fa-sitemap', g.id, 30, 'LEAF', 'SETUP', 'ORG.DEPARTMENT.VIEW',    '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_ORG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_WAREHOUSE', 'Warehouses',        '/warehouses',           'fa fa-warehouse', g.id, 40, 'LEAF', 'SETUP', 'ORG.WAREHOUSE.VIEW',   '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_ORG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_COST_CTR',  'Cost Centers',      '/cost-centers',         'fa fa-chart-pie', g.id, 50, 'LEAF', 'SETUP', 'ORG.COST_CENTER.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_ORG' ON CONFLICT (menu_code) DO NOTHING;

-- ── SETUP REFERENCE DATA leaves ───────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_BANK',      'Banks',             '/setup/banks',          'fa fa-landmark', g.id, 10, 'LEAF', 'SETUP', 'SETUP.BANK.VIEW',     '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_REF' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_CURRENCY',  'Currencies',        '/setup/currencies',     'fa fa-coins', g.id, 20, 'LEAF', 'SETUP', 'SETUP.CURRENCY.VIEW',  '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_REF' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_TERMS',     'Payment Terms',     '/setup/terms',          'fa fa-handshake', g.id, 30, 'LEAF', 'SETUP', 'SETUP.TERMS.VIEW',   '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_REF' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_HS_CODE',   'HS Codes',          '/setup/hs-codes',       'fa fa-barcode', g.id, 40, 'LEAF', 'SETUP', 'SETUP.HS_CODE.VIEW',  '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_STP_REF' ON CONFLICT (menu_code) DO NOTHING;

-- ── SECURITY USERS & ROLES leaves ─────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_USERS',     'Users',             '/users',                'fa fa-user', g.id, 10, 'LEAF', 'SECURITY', 'SECURITY.USER.VIEW',    '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SEC_USER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_ROLES',     'Roles',             '/roles',                'fa fa-user-shield', g.id, 20, 'LEAF', 'SECURITY', 'SECURITY.ROLE.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SEC_USER' ON CONFLICT (menu_code) DO NOTHING;

-- ── SECURITY MENU & PERMISSIONS leaves ───────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_MENUS',     'Menu Management',   '/menus',                'fa fa-bars', g.id, 10, 'LEAF', 'SECURITY', 'SECURITY.MENU.VIEW',  '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SEC_MENU' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_PERMISSIONS','Permissions',      '/permissions',          'fa fa-key', g.id, 20, 'LEAF', 'SECURITY', 'SECURITY.PERMISSION.VIEW', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SEC_MENU' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_ROLE_MENU', 'Role Menu Access',  '/role-menus',           'fa fa-unlock-alt', g.id, 30, 'LEAF', 'SECURITY', 'SECURITY.ROLEMENU.MANAGE', '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_SEC_MENU' ON CONFLICT (menu_code) DO NOTHING;

-- ── APPROVALS leaves ──────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'APR_CONFIG',    'Approval Config',   '/approvals/configs',    'fa fa-sliders-h', g.id, 10, 'LEAF', 'APPROVALS', 'APR.CONFIG.VIEW',       '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_APR_CONFIG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'APR_DELEGATION','Delegations',       '/approvals/delegations','fa fa-random', g.id, 20, 'LEAF', 'APPROVALS', 'APR.DELEGATION.VIEW',    '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_APR_CONFIG' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'APR_PENDING',   'Pending Approvals', '/approvals/requests',   'fa fa-hourglass-half', g.id, 10, 'LEAF', 'APPROVALS', 'APR.REQUEST.VIEW',  '_self', true, true, false, NOW(), NOW() FROM app_menus g WHERE g.menu_code = 'GRP_APR_PENDING' ON CONFLICT (menu_code) DO NOTHING;


-- =============================================================================
-- 5.  sec_mrole_menus  (RoleMenuAccess)
--
--     SUPER_ADMIN: gets ALL menus, full CRUD — inserted by a single subquery.
--     Other roles: explicit per-role inserts mapping role → menu + flags.
--     canView=true always; canCreate/Edit/Delete per business rules.
-- =============================================================================

-- ── ROLE_SUPER_ADMIN → ALL active menus, full CRUD ───────────────────────────
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true, true, true, true, NOW(), NOW()
FROM sec_roles r
CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND m.active = true AND m.deleted = false
ON CONFLICT (role_id, menu_id) DO NOTHING;


-- ── Helper macro (repeated pattern): view-only access ─────────────────────────
-- ROLE_ACCOUNTS_ADMIN
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,                                                      -- can_view always true
  m.menu_code IN ('ACC_COA','ACC_SUBACC','ACC_BANK_ACC',
                  'ACC_JOURNAL','ACC_PAYMENT','ACC_RECEIPT',
                  'ACC_MAPPING','ACC_POLICY'),                -- can_create
  m.menu_code IN ('ACC_COA','ACC_SUBACC','ACC_BANK_ACC',
                  'ACC_JOURNAL','ACC_PAYMENT','ACC_RECEIPT',
                  'ACC_MAPPING','ACC_POLICY'),                -- can_edit
  m.menu_code IN ('ACC_COA','ACC_JOURNAL'),                  -- can_delete
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_ACCOUNTS_ADMIN'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_ACCOUNTS','MOD_SETUP','MOD_APPROVALS',
      'GRP_ACC_MASTER','GRP_ACC_VOUCHERS','GRP_ACC_REPORTS','GRP_ACC_CONFIG',
      'GRP_STP_REF','GRP_APR_PENDING',
      'ACC_COA','ACC_SUBACC','ACC_BANK_ACC',
      'ACC_JOURNAL','ACC_PAYMENT','ACC_RECEIPT',
      'ACC_LEDGER','ACC_TRIAL_BAL','ACC_PNL','ACC_BAL_SHEET',
      'ACC_MAPPING','ACC_POLICY',
      'STP_BANK','STP_CURRENCY','STP_TERMS',
      'APR_PENDING',
      'PUR_INVOICE','PUR_PAYMENT',
      'SAL_INVOICE','SAL_RECEIPT'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_ACCOUNTANT
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('ACC_JOURNAL','ACC_PAYMENT','ACC_RECEIPT'),
  m.menu_code IN ('ACC_JOURNAL','ACC_PAYMENT','ACC_RECEIPT'),
  false,
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_ACCOUNTANT'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_ACCOUNTS','MOD_SETUP',
      'GRP_ACC_MASTER','GRP_ACC_VOUCHERS','GRP_ACC_REPORTS','GRP_ACC_CONFIG',
      'GRP_STP_REF',
      'ACC_COA','ACC_SUBACC','ACC_BANK_ACC',
      'ACC_JOURNAL','ACC_PAYMENT','ACC_RECEIPT',
      'ACC_LEDGER','ACC_TRIAL_BAL','ACC_PNL','ACC_BAL_SHEET',
      'ACC_MAPPING','ACC_POLICY',
      'STP_BANK','STP_CURRENCY','STP_TERMS',
      'PUR_INVOICE','PUR_PAYMENT',
      'SAL_INVOICE','SAL_RECEIPT'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_INVENTORY_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('INV_UOM','INV_CATEGORY','INV_BRAND','INV_MODEL','INV_ITEM',
                  'INV_STOCK_ADJ','INV_TRANSFER'),
  m.menu_code IN ('INV_UOM','INV_CATEGORY','INV_BRAND','INV_MODEL','INV_ITEM',
                  'INV_STOCK_ADJ','INV_TRANSFER'),
  m.menu_code IN ('INV_UOM','INV_CATEGORY','INV_BRAND','INV_MODEL','INV_ITEM'),
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_INVENTORY_MANAGER'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_INVENTORY','MOD_SETUP',
      'GRP_INV_MASTER','GRP_INV_STOCK','GRP_STP_REF',
      'INV_UOM','INV_CATEGORY','INV_BRAND','INV_MODEL','INV_ITEM',
      'INV_STOCK','INV_STOCK_ADJ','INV_TRANSFER',
      'STP_HS_CODE','STP_WAREHOUSE'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_WAREHOUSE_STAFF
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('INV_TRANSFER','PUR_GRN'),
  m.menu_code IN ('INV_TRANSFER','PUR_GRN'),
  false,
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_WAREHOUSE_STAFF'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_INVENTORY','MOD_PURCHASE',
      'GRP_INV_MASTER','GRP_INV_STOCK','GRP_PUR_CYCLE',
      'INV_ITEM','INV_STOCK','INV_TRANSFER',
      'PUR_GRN'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_PURCHASE_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('PUR_SUPPLIER','PUR_PO','PUR_GRN','PUR_INVOICE',
                  'PUR_DEBIT_NOTE','PUR_PAYMENT'),
  m.menu_code IN ('PUR_SUPPLIER','PUR_PO','PUR_GRN','PUR_INVOICE',
                  'PUR_DEBIT_NOTE','PUR_PAYMENT'),
  m.menu_code IN ('PUR_SUPPLIER','PUR_PO','PUR_GRN','PUR_INVOICE',
                  'PUR_DEBIT_NOTE'),
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_PURCHASE_MANAGER'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_PURCHASE','MOD_INVENTORY','MOD_SETUP',
      'GRP_PUR_MASTER','GRP_PUR_CYCLE','GRP_PUR_RETURNS',
      'GRP_INV_MASTER','GRP_INV_STOCK','GRP_STP_REF',
      'PUR_SUPPLIER','PUR_PO','PUR_GRN','PUR_INVOICE',
      'PUR_DEBIT_NOTE','PUR_PAYMENT',
      'INV_ITEM','INV_STOCK',
      'STP_BANK','STP_CURRENCY','STP_TERMS','STP_HS_CODE'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_PURCHASE_OFFICER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('PUR_PO','PUR_GRN'),
  m.menu_code IN ('PUR_PO','PUR_GRN'),
  false,
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_PURCHASE_OFFICER'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_PURCHASE','MOD_INVENTORY',
      'GRP_PUR_MASTER','GRP_PUR_CYCLE','GRP_PUR_RETURNS',
      'GRP_INV_MASTER','GRP_INV_STOCK',
      'PUR_SUPPLIER','PUR_PO','PUR_GRN',
      'PUR_INVOICE','PUR_DEBIT_NOTE','PUR_PAYMENT',
      'INV_ITEM','INV_STOCK',
      'STP_TERMS','STP_HS_CODE'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_SALES_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('SAL_CUSTOMER','SAL_SO','SAL_DELIVERY','SAL_INVOICE',
                  'SAL_CREDIT_NOTE','SAL_RECEIPT'),
  m.menu_code IN ('SAL_CUSTOMER','SAL_SO','SAL_DELIVERY','SAL_INVOICE',
                  'SAL_CREDIT_NOTE','SAL_RECEIPT'),
  m.menu_code IN ('SAL_CUSTOMER','SAL_SO','SAL_DELIVERY','SAL_INVOICE',
                  'SAL_CREDIT_NOTE'),
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SALES_MANAGER'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_SALES','MOD_INVENTORY','MOD_SETUP',
      'GRP_SAL_MASTER','GRP_SAL_CYCLE','GRP_SAL_RETURNS',
      'GRP_INV_MASTER','GRP_INV_STOCK','GRP_STP_REF',
      'SAL_CUSTOMER','SAL_SO','SAL_DELIVERY','SAL_INVOICE',
      'SAL_CREDIT_NOTE','SAL_RECEIPT',
      'INV_ITEM','INV_STOCK',
      'STP_BANK','STP_CURRENCY','STP_TERMS','STP_HS_CODE'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_SALES_EXECUTIVE
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('SAL_SO'),
  m.menu_code IN ('SAL_SO'),
  false,
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SALES_EXECUTIVE'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_SALES','MOD_INVENTORY',
      'GRP_SAL_MASTER','GRP_SAL_CYCLE','GRP_SAL_RETURNS',
      'GRP_INV_MASTER','GRP_INV_STOCK',
      'SAL_CUSTOMER','SAL_SO',
      'SAL_DELIVERY','SAL_INVOICE','SAL_RECEIPT','SAL_CREDIT_NOTE',
      'INV_ITEM','INV_STOCK',
      'STP_TERMS'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_HRM
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('HRM_DESIG','HRM_EMPLOYEE','HRM_ATTENDANCE','HRM_LEAVE','HRM_PAYROLL'),
  m.menu_code IN ('HRM_DESIG','HRM_EMPLOYEE','HRM_ATTENDANCE','HRM_LEAVE','HRM_PAYROLL'),
  m.menu_code IN ('HRM_DESIG','HRM_EMPLOYEE'),
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_HRM'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_HRM','MOD_SETUP',
      'GRP_HRM_MASTER','GRP_HRM_ATTENDANCE','GRP_HRM_PAYROLL',
      'GRP_STP_ORG',
      'HRM_DESIG','HRM_EMPLOYEE',
      'HRM_ATTENDANCE','HRM_LEAVE','HRM_PAYROLL',
      'STP_DEPT'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_PRODUCTION_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('PRD_ORDER','PRD_MAT_REQ'),
  m.menu_code IN ('PRD_ORDER','PRD_MAT_REQ'),
  m.menu_code IN ('PRD_ORDER','PRD_MAT_REQ'),
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_PRODUCTION_MANAGER'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_PRODUCTION','MOD_INVENTORY',
      'GRP_PRD_ORDERS','GRP_PRD_MATERIALS',
      'GRP_INV_MASTER','GRP_INV_STOCK',
      'PRD_ORDER','PRD_MAT_REQ',
      'INV_ITEM','INV_STOCK'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_PRODUCTION_SUPERVISOR
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('PRD_MAT_REQ'),
  m.menu_code IN ('PRD_MAT_REQ'),
  false,
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_PRODUCTION_SUPERVISOR'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_PRODUCTION','MOD_INVENTORY',
      'GRP_PRD_ORDERS','GRP_PRD_MATERIALS',
      'GRP_INV_MASTER','GRP_INV_STOCK',
      'PRD_ORDER','PRD_MAT_REQ',
      'INV_ITEM','INV_STOCK'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_COMMERCIAL_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('COM_LC','COM_EXPORT','COM_IMPORT'),
  m.menu_code IN ('COM_LC','COM_EXPORT','COM_IMPORT'),
  m.menu_code IN ('COM_LC'),
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_COMMERCIAL_MANAGER'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_COMMERCIAL','MOD_SETUP',
      'GRP_COM_LC','GRP_COM_TRADE','GRP_STP_REF',
      'COM_LC','COM_EXPORT','COM_IMPORT',
      'STP_BANK','STP_CURRENCY','STP_TERMS','STP_HS_CODE'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_COMMERCIAL_EXECUTIVE
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id,
  true,
  m.menu_code IN ('COM_EXPORT','COM_IMPORT'),
  m.menu_code IN ('COM_EXPORT','COM_IMPORT'),
  false,
  NOW(), NOW()
FROM sec_roles r CROSS JOIN app_menus m
WHERE r.name = 'ROLE_COMMERCIAL_EXECUTIVE'
  AND m.menu_code IN (
      'MOD_DASHBOARD','MOD_COMMERCIAL','MOD_SETUP',
      'GRP_COM_LC','GRP_COM_TRADE','GRP_STP_REF',
      'COM_LC','COM_EXPORT','COM_IMPORT',
      'STP_BANK','STP_CURRENCY','STP_TERMS'
  )
ON CONFLICT (role_id, menu_id) DO NOTHING;


-- =============================================================================
--  VERIFICATION QUERIES  (run separately to confirm counts)
-- =============================================================================
-- SELECT 'Permissions' AS tbl, COUNT(*) FROM sec_permissions
-- UNION ALL SELECT 'Roles',         COUNT(*) FROM sec_roles
-- UNION ALL SELECT 'Role-Perms',    COUNT(*) FROM sec_role_permissions
-- UNION ALL SELECT 'Menus',         COUNT(*) FROM app_menus
-- UNION ALL SELECT 'Role-Menus',    COUNT(*) FROM sec_mrole_menus;

COMMIT;
