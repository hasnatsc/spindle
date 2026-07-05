-- =============================================================================
--  Spindle ERP  —  Complete Menu + Permission + Role-Menu Seed  v4.0
--  File   : 00_seed_menu_permission_complete.sql
--  Target : PostgreSQL
--
--  Changes from v3.0:
--    1. PERMISSION NAME UNIFICATION
--       Role-permission section used shorthand aliases (acc.journal.*, acc.payment.*)
--       while the INSERT section defined acc.jv.*, acc.pv.*, acc.rv.* etc.
--       All permission names now consistent: acc.jv.view, acc.pv.view, acc.rv.view,
--       acc.cv.view, acc.sub.view, acc.bank_acc.view — matching controller URLs.
--
--    2. MISSING PERMISSIONS ADDED
--       - dashboard.view now covers /dashboard (not /dashboard/**)
--       - security.dashboard.view  → /security/dashboard/**
--       - security.org_module.view → /security/org-modules/**
--       - Per-module dashboard permissions for all 15 modules
--       - org.department.delete added (was referenced, not defined)
--
--    3. MODULE DASHBOARD MENUS ADDED
--       Every MODULE entry now has a DASHBOARD LEAF as its first child
--       (e.g. INV_DASHBOARD → /inventory/dashboard).
--
--    4. SECURITY MODULE ADDITIONS
--       Added: Security Dashboard, Org Module Access management pages.
--
--    5. ROLE-PERMISSION & ROLE-MENU ALIGNMENT
--       All role-permission and role-menu blocks now use canonical names.
--
--  Safe to re-run: all INSERTs use ON CONFLICT DO NOTHING.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1. PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

-- ── Super admin wildcard ──────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('*', 'Super admin wildcard — all access', '/**', NULL, 'CORE_SECURITY', 'SYSTEM', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Dashboard / Main — ALL module dashboards consolidated here ───────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES

-- ERP main dashboard
('dashboard.view', 'View ERP main dashboard', '/dashboard', 'GET', 'REPORTS_ANALYTICS', 'DASHBOARD', true, NOW(),
 NOW()),
('dashboard.summary', 'ERP dashboard summary JSON', '/dashboard/erp-summary', 'GET', 'REPORTS_ANALYTICS', 'DASHBOARD',
 true, NOW(), NOW()),

-- Security module dashboard
('security.dashboard.view', 'View security & IAM dashboard', '/security/dashboard', 'GET', 'CORE_SECURITY', 'DASHBOARD',
 true, NOW(), NOW()),
('security.dashboard.summary', 'Security dashboard summary JSON', '/security/dashboard/summary', 'GET', 'CORE_SECURITY',
 'DASHBOARD', true, NOW(), NOW()),

-- Inventory module dashboard
('inv.dashboard.view', 'View inventory dashboard', '/inventory/dashboard', 'GET', 'INVENTORY_WAREHOUSE', 'DASHBOARD',
 true, NOW(), NOW()),
('inv.dashboard.summary', 'Inventory dashboard summary JSON', '/inventory/dashboard/summary', 'GET',
 'INVENTORY_WAREHOUSE', 'DASHBOARD', true, NOW(), NOW()),

-- Purchase module dashboard
('pur.dashboard.view', 'View purchase dashboard', '/purchase/dashboard', 'GET', 'PURCHASE_SUPPLIER', 'DASHBOARD', true,
 NOW(), NOW()),
('pur.dashboard.summary', 'Purchase dashboard summary JSON', '/purchase/dashboard/summary', 'GET', 'PURCHASE_SUPPLIER',
 'DASHBOARD', true, NOW(), NOW()),

-- Sales module dashboard
('sal.dashboard.view', 'View sales dashboard', '/sales/dashboard', 'GET', 'SALES_CUSTOMER_OPERATIONS', 'DASHBOARD',
 true, NOW(), NOW()),
('sal.dashboard.summary', 'Sales dashboard summary JSON', '/sales/dashboard/summary', 'GET',
 'SALES_CUSTOMER_OPERATIONS', 'DASHBOARD', true, NOW(), NOW()),

-- Accounts / GL module dashboard
('acc.dashboard.view', 'View accounts module dashboard', '/accounts/dashboard', 'GET', 'FINANCE_ACCOUNTS', 'DASHBOARD',
 true, NOW(), NOW()),
('acc.dashboard.summary', 'Accounts dashboard summary JSON', '/accounts/dashboard/summary', 'GET', 'FINANCE_ACCOUNTS',
 'DASHBOARD', true, NOW(), NOW()),

-- HRM module dashboard
('hrm.dashboard.view', 'View HRM dashboard', '/hrm/dashboard', 'GET', 'HRM', 'DASHBOARD', true, NOW(), NOW()),
('hrm.dashboard.summary', 'HRM dashboard summary JSON', '/hrm/dashboard/full-summary', 'GET', 'HRM', 'DASHBOARD', true,
 NOW(), NOW()),

-- Production module dashboard
('prd.dashboard.view', 'View production dashboard', '/production/dashboard', 'GET', 'PRODUCTION', 'DASHBOARD', true,
 NOW(), NOW()),
('prd.dashboard.summary', 'Production dashboard summary JSON', '/production/dashboard/summary', 'GET', 'PRODUCTION',
 'DASHBOARD', true, NOW(), NOW()),

-- Commercial module dashboard
('com.dashboard.view', 'View commercial dashboard', '/commercial/dashboard', 'GET', 'COMMERCIAL', 'DASHBOARD', true,
 NOW(), NOW()),
('com.dashboard.summary', 'Commercial dashboard summary JSON', '/commercial/dashboard/summary', 'GET', 'COMMERCIAL',
 'DASHBOARD', true, NOW(), NOW()),

-- CRM module dashboard
('crm.dashboard.view', 'View CRM dashboard', '/crm/dashboard', 'GET', 'CRM', 'DASHBOARD', true, NOW(), NOW()),
('crm.dashboard.summary', 'CRM dashboard summary JSON', '/crm/dashboard/summary', 'GET', 'CRM', 'DASHBOARD', true,
 NOW(), NOW()),

-- Budget module dashboard
('budget.dashboard.view', 'View budget dashboard', '/budget/dashboard', 'GET', 'BUDGET', 'DASHBOARD', true, NOW(),
 NOW()),
('budget.dashboard.summary', 'Budget dashboard summary JSON', '/budget/dashboard/summary', 'GET', 'BUDGET', 'DASHBOARD',
 true, NOW(), NOW()),

-- Fixed Assets module dashboard
('fa.dashboard.view', 'View fixed assets dashboard', '/fixed-assets/dashboard', 'GET', 'FIXED_ASSETS', 'DASHBOARD',
 true, NOW(), NOW()),
('fa.dashboard.summary', 'Fixed assets dashboard summary JSON', '/fixed-assets/dashboard/summary', 'GET',
 'FIXED_ASSETS', 'DASHBOARD', true, NOW(), NOW()),

-- Approval & Workflow dashboard
('apr.dashboard.view', 'View approvals dashboard', '/approval/dashboard', 'GET', 'CORE_SECURITY', 'DASHBOARD', true,
 NOW(), NOW()),
('apr.dashboard.summary', 'Approvals dashboard summary JSON', '/approval/dashboard/summary', 'GET', 'CORE_SECURITY',
 'DASHBOARD', true, NOW(), NOW()),

-- Reports module
('reports.view', 'View reports module', '/reports/**', 'GET', 'REPORTS_ANALYTICS', 'REPORTS', true, NOW(), NOW())

ON CONFLICT (name) DO NOTHING;

-- ── Security / IAM ───────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('security.user.view', 'View users', '/users/**', 'GET', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
       ('security.user.create', 'Create user', '/users/save', 'POST', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
       ('security.user.edit', 'Edit user', '/users/save', 'POST', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
       ('security.user.delete', 'Delete user', '/users/delete/**', 'DELETE', 'CORE_SECURITY', 'SECURITY', true, NOW(),
        NOW()),
       ('security.user.toggle', 'Toggle user status', '/users/toggle/**', 'POST', 'CORE_SECURITY', 'SECURITY', true,
        NOW(), NOW()),
       ('security.role.view', 'View roles', '/roles/**', 'GET', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
       ('security.role.create', 'Create role', '/roles/save', 'POST', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
       ('security.role.delete', 'Delete role', '/roles/delete/**', 'DELETE', 'CORE_SECURITY', 'SECURITY', true, NOW(),
        NOW()),
       ('security.menu.view', 'View menus', '/menus/**', 'GET', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
       ('security.menu.create', 'Create menu', '/menus/save', 'POST', 'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
       ('security.menu.delete', 'Delete menu', '/menus/delete/**', 'DELETE', 'CORE_SECURITY', 'SECURITY', true, NOW(),
        NOW()),
       ('security.permission.view', 'View permissions', '/permissions/**', 'GET', 'CORE_SECURITY', 'SECURITY', true,
        NOW(), NOW()),
       ('security.rolemenu.manage', 'Manage role-menu access', '/role-menus/**', 'POST', 'CORE_SECURITY', 'SECURITY',
        true, NOW(), NOW()),
       ('security.org_module.view', 'View org module access (super-admin)', '/security/org-modules/**', 'GET',
        'CORE_SECURITY', 'SECURITY', true, NOW(), NOW()),
       ('security.org_module.manage', 'Manage org module access (super-admin)', '/security/org-modules/**', 'POST',
        'CORE_SECURITY', 'SECURITY', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Organization / Setup ─────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('org.organization.view', 'View organizations', '/organizations/**', 'GET', 'CORE_SECURITY', 'ORG_SETUP', true,
        NOW(), NOW()),
       ('org.organization.create', 'Create organization', '/organizations/save', 'POST', 'CORE_SECURITY', 'ORG_SETUP',
        true, NOW(), NOW()),
       ('org.organization.edit', 'Edit organization', '/organizations/save', 'POST', 'CORE_SECURITY', 'ORG_SETUP', true,
        NOW(), NOW()),
       ('org.organization.delete', 'Delete organization', '/organizations/delete/**', 'DELETE', 'CORE_SECURITY',
        'ORG_SETUP', true, NOW(), NOW()),
       ('org.business_unit.view', 'View business units', '/business-units/**', 'GET', 'CORE_SECURITY', 'ORG_SETUP',
        true, NOW(), NOW()),
       ('org.business_unit.create', 'Create business unit', '/business-units/save', 'POST', 'CORE_SECURITY',
        'ORG_SETUP', true, NOW(), NOW()),
       ('org.department.view', 'View departments', '/departments/**', 'GET', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(),
        NOW()),
       ('org.department.create', 'Create department', '/departments/save', 'POST', 'CORE_SECURITY', 'ORG_SETUP', true,
        NOW(), NOW()),
       ('org.department.delete', 'Delete department', '/departments/delete/**', 'DELETE', 'CORE_SECURITY', 'ORG_SETUP',
        true, NOW(), NOW()),
       ('org.warehouse.view', 'View warehouses', '/warehouses/**', 'GET', 'CORE_SECURITY', 'ORG_SETUP', true, NOW(),
        NOW()),
       ('org.warehouse.create', 'Create warehouse', '/warehouses/save', 'POST', 'CORE_SECURITY', 'ORG_SETUP', true,
        NOW(), NOW()),
       ('org.cost_center.view', 'View cost centers', '/cost-centers/**', 'GET', 'CORE_SECURITY', 'ORG_SETUP', true,
        NOW(), NOW()),
       ('org.cost_center.create', 'Create cost center', '/cost-centers/save', 'POST', 'CORE_SECURITY', 'ORG_SETUP',
        true, NOW(), NOW()),
       ('setup.bank.view', 'View banks', '/setup/banks/**', 'GET', 'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
       ('setup.bank.create', 'Create bank', '/setup/banks/save', 'POST', 'CORE_SECURITY', 'SETUP', true, NOW(), NOW()),
       ('setup.currency.view', 'View currencies', '/setup/currencies/**', 'GET', 'CORE_SECURITY', 'SETUP', true, NOW(),
        NOW()),
       ('setup.currency.create', 'Create currency', '/setup/currencies/save', 'POST', 'CORE_SECURITY', 'SETUP', true,
        NOW(), NOW()),
       ('setup.terms.view', 'View payment terms', '/setup/terms/**', 'GET', 'CORE_SECURITY', 'SETUP', true, NOW(),
        NOW()),
       ('setup.terms.create', 'Create payment term', '/setup/terms/save', 'POST', 'CORE_SECURITY', 'SETUP', true, NOW(),
        NOW()),
       ('setup.hs_code.view', 'View HS codes', '/setup/hs-codes/**', 'GET', 'CORE_SECURITY', 'SETUP', true, NOW(),
        NOW()),
       ('setup.hs_code.create', 'Create HS code', '/setup/hs-codes/save', 'POST', 'CORE_SECURITY', 'SETUP', true, NOW(),
        NOW()),
       ('setup.sequence.view', 'View document sequences', '/setup/sequences/**', 'GET', 'CORE_SECURITY', 'SETUP', true,
        NOW(), NOW()),
       ('setup.sequence.create', 'Create document sequence', '/setup/sequences/save', 'POST', 'CORE_SECURITY', 'SETUP',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Inventory ────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('inv.uom.view', 'View UOMs', '/inventory/uoms/**', 'GET', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(),
        NOW()),
       ('inv.uom.create', 'Create UOM', '/inventory/uoms/save', 'POST', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(),
        NOW()),
       ('inv.uom.edit', 'Edit UOM', '/inventory/uoms/save', 'POST', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(),
        NOW()),
       ('inv.uom.delete', 'Delete UOM', '/inventory/uoms/delete/**', 'DELETE', 'INVENTORY_WAREHOUSE', 'INVENTORY', true,
        NOW(), NOW()),
       ('inv.category.view', 'View item categories', '/inventory/categories/**', 'GET', 'INVENTORY_WAREHOUSE',
        'INVENTORY', true, NOW(), NOW()),
       ('inv.category.create', 'Create item category', '/inventory/categories/save', 'POST', 'INVENTORY_WAREHOUSE',
        'INVENTORY', true, NOW(), NOW()),
       ('inv.category.edit', 'Edit item category', '/inventory/categories/save', 'POST', 'INVENTORY_WAREHOUSE',
        'INVENTORY', true, NOW(), NOW()),
       ('inv.category.delete', 'Delete item category', '/inventory/categories/delete/**', 'DELETE',
        'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
       ('inv.brand.view', 'View brands', '/inventory/brands/**', 'GET', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(),
        NOW()),
       ('inv.brand.create', 'Create brand', '/inventory/brands/save', 'POST', 'INVENTORY_WAREHOUSE', 'INVENTORY', true,
        NOW(), NOW()),
       ('inv.model.view', 'View item models', '/inventory/models/**', 'GET', 'INVENTORY_WAREHOUSE', 'INVENTORY', true,
        NOW(), NOW()),
       ('inv.model.create', 'Create item model', '/inventory/models/save', 'POST', 'INVENTORY_WAREHOUSE', 'INVENTORY',
        true, NOW(), NOW()),
       ('inv.item.view', 'View items', '/inventory/items/**', 'GET', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(),
        NOW()),
       ('inv.item.create', 'Create item', '/inventory/items/save', 'POST', 'INVENTORY_WAREHOUSE', 'INVENTORY', true,
        NOW(), NOW()),
       ('inv.item.edit', 'Edit item', '/inventory/items/save', 'POST', 'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(),
        NOW()),
       ('inv.item.delete', 'Delete item', '/inventory/items/delete/**', 'DELETE', 'INVENTORY_WAREHOUSE', 'INVENTORY',
        true, NOW(), NOW()),
       ('inv.stock.view', 'View stock ledger', '/inventory/stocks/**', 'GET', 'INVENTORY_WAREHOUSE', 'INVENTORY', true,
        NOW(), NOW()),
       ('inv.adjustment.view', 'View stock adjustments', '/inventory/adjustments/**', 'GET', 'INVENTORY_WAREHOUSE',
        'INVENTORY', true, NOW(), NOW()),
       ('inv.adjustment.create', 'Create stock adjustment', '/inventory/adjustments/save', 'POST',
        'INVENTORY_WAREHOUSE', 'INVENTORY', true, NOW(), NOW()),
       ('inv.transfer.view', 'View stock transfers', '/inventory/transfers/**', 'GET', 'INVENTORY_WAREHOUSE',
        'INVENTORY', true, NOW(), NOW()),
       ('inv.transfer.create', 'Create stock transfer', '/inventory/transfers/save', 'POST', 'INVENTORY_WAREHOUSE',
        'INVENTORY', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Purchase ─────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('pur.supplier.view', 'View suppliers', '/purchase/suppliers/**', 'GET', 'PURCHASE_SUPPLIER', 'PURCHASE', true,
        NOW(), NOW()),
       ('pur.supplier.create', 'Create supplier', '/purchase/suppliers/save', 'POST', 'PURCHASE_SUPPLIER', 'PURCHASE',
        true, NOW(), NOW()),
       ('pur.supplier.edit', 'Edit supplier', '/purchase/suppliers/save', 'POST', 'PURCHASE_SUPPLIER', 'PURCHASE', true,
        NOW(), NOW()),
       ('pur.supplier.delete', 'Delete supplier', '/purchase/suppliers/delete/**', 'DELETE', 'PURCHASE_SUPPLIER',
        'PURCHASE', true, NOW(), NOW()),
       ('pur.po.view', 'View purchase orders', '/purchase/orders/**', 'GET', 'PURCHASE_SUPPLIER', 'PURCHASE', true,
        NOW(), NOW()),
       ('pur.po.create', 'Create purchase order', '/purchase/orders/save', 'POST', 'PURCHASE_SUPPLIER', 'PURCHASE',
        true, NOW(), NOW()),
       ('pur.po.edit', 'Edit purchase order', '/purchase/orders/save', 'POST', 'PURCHASE_SUPPLIER', 'PURCHASE', true,
        NOW(), NOW()),
       ('pur.po.delete', 'Delete purchase order', '/purchase/orders/delete/**', 'DELETE', 'PURCHASE_SUPPLIER',
        'PURCHASE', true, NOW(), NOW()),
       ('pur.po.approve', 'Approve purchase order', '/purchase/orders/approve/**', 'POST', 'PURCHASE_SUPPLIER',
        'PURCHASE', true, NOW(), NOW()),
       ('pur.grn.view', 'View GRNs', '/purchase/grns/**', 'GET', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
       ('pur.grn.create', 'Create GRN', '/purchase/grns/save', 'POST', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(),
        NOW()),
       ('pur.grn.edit', 'Edit GRN', '/purchase/grns/save', 'POST', 'PURCHASE_SUPPLIER', 'PURCHASE', true, NOW(), NOW()),
       ('pur.grn.delete', 'Delete GRN', '/purchase/grns/delete/**', 'DELETE', 'PURCHASE_SUPPLIER', 'PURCHASE', true,
        NOW(), NOW()),
       ('pur.invoice.view', 'View purchase invoices', '/purchase/invoices/**', 'GET', 'PURCHASE_SUPPLIER', 'PURCHASE',
        true, NOW(), NOW()),
       ('pur.invoice.create', 'Create purchase invoice', '/purchase/invoices/save', 'POST', 'PURCHASE_SUPPLIER',
        'PURCHASE', true, NOW(), NOW()),
       ('pur.invoice.edit', 'Edit purchase invoice', '/purchase/invoices/save', 'POST', 'PURCHASE_SUPPLIER', 'PURCHASE',
        true, NOW(), NOW()),
       ('pur.invoice.delete', 'Delete purchase invoice', '/purchase/invoices/delete/**', 'DELETE', 'PURCHASE_SUPPLIER',
        'PURCHASE', true, NOW(), NOW()),
       ('pur.debit_note.view', 'View debit notes', '/purchase/debit-notes/**', 'GET', 'PURCHASE_SUPPLIER', 'PURCHASE',
        true, NOW(), NOW()),
       ('pur.debit_note.create', 'Create debit note', '/purchase/debit-notes/save', 'POST', 'PURCHASE_SUPPLIER',
        'PURCHASE', true, NOW(), NOW()),
       ('pur.payment.view', 'View purchase payments', '/purchase/payments/**', 'GET', 'PURCHASE_SUPPLIER', 'PURCHASE',
        true, NOW(), NOW()),
       ('pur.payment.create', 'Create purchase payment', '/purchase/payments/save', 'POST', 'PURCHASE_SUPPLIER',
        'PURCHASE', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Sales ─────────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('sal.customer.view', 'View customers', '/sales/customers/**', 'GET', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true,
        NOW(), NOW()),
       ('sal.customer.create', 'Create customer', '/sales/customers/save', 'POST', 'SALES_CUSTOMER_OPERATIONS', 'SALES',
        true, NOW(), NOW()),
       ('sal.customer.edit', 'Edit customer', '/sales/customers/save', 'POST', 'SALES_CUSTOMER_OPERATIONS', 'SALES',
        true, NOW(), NOW()),
       ('sal.customer.delete', 'Delete customer', '/sales/customers/delete/**', 'DELETE', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW()),
       ('sal.so.view', 'View sales orders', '/sales/orders/**', 'GET', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true,
        NOW(), NOW()),
       ('sal.so.create', 'Create sales order', '/sales/orders/save', 'POST', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true,
        NOW(), NOW()),
       ('sal.so.edit', 'Edit sales order', '/sales/orders/save', 'POST', 'SALES_CUSTOMER_OPERATIONS', 'SALES', true,
        NOW(), NOW()),
       ('sal.so.delete', 'Delete sales order', '/sales/orders/delete/**', 'DELETE', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW()),
       ('sal.so.approve', 'Approve sales order', '/sales/orders/approve/**', 'POST', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW()),
       ('sal.delivery.view', 'View delivery notes', '/sales/deliveries/**', 'GET', 'SALES_CUSTOMER_OPERATIONS', 'SALES',
        true, NOW(), NOW()),
       ('sal.delivery.create', 'Create delivery note', '/sales/deliveries/save', 'POST', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW()),
       ('sal.delivery.edit', 'Edit delivery note', '/sales/deliveries/save', 'POST', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW()),
       ('sal.delivery.delete', 'Delete delivery note', '/sales/deliveries/delete/**', 'DELETE',
        'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
       ('sal.invoice.view', 'View sales invoices', '/sales/invoices/**', 'GET', 'SALES_CUSTOMER_OPERATIONS', 'SALES',
        true, NOW(), NOW()),
       ('sal.invoice.create', 'Create sales invoice', '/sales/invoices/save', 'POST', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW()),
       ('sal.invoice.edit', 'Edit sales invoice', '/sales/invoices/save', 'POST', 'SALES_CUSTOMER_OPERATIONS', 'SALES',
        true, NOW(), NOW()),
       ('sal.invoice.delete', 'Delete sales invoice', '/sales/invoices/delete/**', 'DELETE',
        'SALES_CUSTOMER_OPERATIONS', 'SALES', true, NOW(), NOW()),
       ('sal.credit_note.view', 'View credit notes', '/sales/credit-notes/**', 'GET', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW()),
       ('sal.credit_note.create', 'Create credit note', '/sales/credit-notes/save', 'POST', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW()),
       ('sal.receipt.view', 'View receipt vouchers', '/sales/receipts/**', 'GET', 'SALES_CUSTOMER_OPERATIONS', 'SALES',
        true, NOW(), NOW()),
       ('sal.receipt.create', 'Create receipt voucher', '/sales/receipts/save', 'POST', 'SALES_CUSTOMER_OPERATIONS',
        'SALES', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Accounts / GL ────────────────────────────────────────────────────────────
-- Canonical names: acc.jv.* acc.pv.* acc.rv.* acc.cv.* acc.sub.* acc.bank_acc.*
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES
-- Chart of Accounts
('acc.coa.view', 'View chart of accounts', '/accounts/chart-of-accounts', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.coa.list', 'List COA (DataTable)', '/accounts/chart-of-accounts/list', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.coa.show', 'Show COA detail', '/accounts/chart-of-accounts/show/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.coa.create', 'Create chart of account', '/accounts/chart-of-accounts/save', 'POST', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.coa.edit', 'Edit chart of account', '/accounts/chart-of-accounts/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.coa.toggle', 'Activate/deactivate COA', '/accounts/chart-of-accounts/toggle/**', 'POST', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.coa.delete', 'Delete chart of account', '/accounts/chart-of-accounts/delete/**', 'DELETE', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.coa.search', 'Search COA (Select2)', '/accounts/chart-of-accounts/search', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.coa.tree', 'COA tree view', '/accounts/chart-of-accounts/tree', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
-- Sub-accounts
('acc.sub.view', 'View sub-accounts', '/accounts/sub-accounts', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(),
 NOW()),
('acc.sub.list', 'List sub-accounts (DataTable)', '/accounts/sub-accounts/list', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.sub.show', 'Show sub-account detail', '/accounts/sub-accounts/show/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.sub.create', 'Create sub-account', '/accounts/sub-accounts/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.sub.edit', 'Edit sub-account', '/accounts/sub-accounts/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(),
 NOW()),
('acc.sub.toggle', 'Activate/deactivate sub-account', '/accounts/sub-accounts/toggle/**', 'POST', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.sub.delete', 'Delete sub-account', '/accounts/sub-accounts/delete/**', 'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.sub.search', 'Search sub-accounts (Select2)', '/accounts/sub-accounts/search', 'GET', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
-- Bank accounts
('acc.bank_acc.view', 'View bank accounts', '/accounts/bank-accounts/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.bank_acc.create', 'Create bank account', '/accounts/bank-accounts/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
-- Journal Voucher (JV)
('acc.jv.view', 'View journal voucher page', '/accounts/journals', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(),
 NOW()),
('acc.jv.list', 'List journal vouchers (DataTable)', '/accounts/vouchers/list', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.jv.show', 'Show journal voucher detail', '/accounts/vouchers/show/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.jv.create', 'Create / save journal voucher', '/accounts/vouchers/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.jv.post', 'Post journal voucher', '/accounts/vouchers/post/**', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.jv.reverse', 'Reverse journal voucher', '/accounts/vouchers/reverse/**', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.jv.delete', 'Delete draft journal voucher', '/accounts/vouchers/delete/**', 'DELETE', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
-- Payment Voucher (PV)
('acc.pv.view', 'View payment voucher page', '/accounts/payment-vouchers', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.pv.create', 'Create payment voucher', '/accounts/vouchers/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
-- Receipt Voucher (RV)
('acc.rv.view', 'View receipt voucher page', '/accounts/receipt-vouchers', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.rv.create', 'Create receipt voucher', '/accounts/vouchers/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
-- Contra Voucher (CV)
('acc.cv.view', 'View contra voucher page', '/accounts/contra-vouchers', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.cv.create', 'Create contra voucher', '/accounts/vouchers/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
-- Allocations
('acc.alloc.open_for_party', 'Get open vouchers for party', '/accounts/vouchers/open-for-party', 'GET',
 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(), NOW()),
-- Aging
('acc.aging.view', 'View AP/AR aging report page', '/accounts/aging', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.aging.summary', 'Aging summary (DataTable)', '/accounts/aging/summary', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.aging.detail', 'Aging detail for a party', '/accounts/aging/detail', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
-- Accounting Periods
('acc.period.view', 'View accounting periods', '/accounts/periods', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(),
 NOW()),
('acc.period.list', 'List periods (DataTable)', '/accounts/periods/list', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.period.show', 'Show period detail', '/accounts/periods/show/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.period.create', 'Create accounting period', '/accounts/periods/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.period.edit', 'Edit accounting period', '/accounts/periods/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.period.toggle', 'Open/close accounting period', '/accounts/periods/toggle/**', 'POST', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.period.delete', 'Delete accounting period', '/accounts/periods/delete/**', 'DELETE', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
-- Opening Balances
('acc.ob.view', 'View opening balances', '/accounts/opening-balances', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.ob.list', 'List opening balances (DataTable)', '/accounts/opening-balances/list', 'GET', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.ob.create', 'Create opening balance', '/accounts/opening-balances/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.ob.post', 'Post opening balance to GL', '/accounts/opening-balances/post/**', 'POST', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.ob.delete', 'Delete opening balance', '/accounts/opening-balances/delete/**', 'DELETE', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
-- Accounts Mapping
('acc.mapping.view', 'View accounts mapping', '/accounts/mapping', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(),
 NOW()),
('acc.mapping.list', 'List mappings (DataTable)', '/accounts/mapping/list', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.mapping.create', 'Create accounts mapping', '/accounts/mapping/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
('acc.mapping.edit', 'Edit accounts mapping', '/accounts/mapping/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.mapping.toggle', 'Activate/deactivate mapping', '/accounts/mapping/toggle/**', 'POST', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.mapping.delete', 'Delete accounts mapping', '/accounts/mapping/delete/**', 'DELETE', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
-- Accounts Policy
('acc.policy.view', 'View accounts policies', '/accounts/policy', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(),
 NOW()),
('acc.policy.create', 'Create accounts policy', '/accounts/policy/save', 'POST', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.policy.toggle', 'Activate/deactivate policy', '/accounts/policy/toggle/**', 'POST', 'FINANCE_ACCOUNTS',
 'ACCOUNTS', true, NOW(), NOW()),
('acc.policy.delete', 'Delete accounts policy', '/accounts/policy/delete/**', 'DELETE', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW()),
-- GL Reports
('acc.ledger.view', 'View general ledger', '/accounts/ledger/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true, NOW(),
 NOW()),
('acc.trial_bal.view', 'View trial balance', '/accounts/trial-balance/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.profit_loss.view', 'View profit & loss', '/accounts/profit-loss/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS', true,
 NOW(), NOW()),
('acc.balance_sheet.view', 'View balance sheet', '/accounts/balance-sheet/**', 'GET', 'FINANCE_ACCOUNTS', 'ACCOUNTS',
 true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── HRM ──────────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('hrm.designation.view', 'View designations', '/hrm/designations/**', 'GET', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.designation.create', 'Create designation', '/hrm/designations/save', 'POST', 'HRM', 'HRM', true, NOW(),
        NOW()),
       ('hrm.designation.edit', 'Edit designation', '/hrm/designations/save', 'POST', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.designation.delete', 'Delete designation', '/hrm/designations/delete/**', 'DELETE', 'HRM', 'HRM', true,
        NOW(), NOW()),
       ('hrm.employee.view', 'View employees', '/hrm/employees/**', 'GET', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.employee.create', 'Create employee', '/hrm/employees/save', 'POST', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.employee.edit', 'Edit employee', '/hrm/employees/save', 'POST', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.employee.delete', 'Delete employee', '/hrm/employees/delete/**', 'DELETE', 'HRM', 'HRM', true, NOW(),
        NOW()),
       ('hrm.attendance.view', 'View attendance', '/hrm/attendance/**', 'GET', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.attendance.create', 'Create attendance', '/hrm/attendance/save', 'POST', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.leave.view', 'View leaves', '/hrm/leaves/**', 'GET', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.leave.create', 'Create leave', '/hrm/leaves/save', 'POST', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.leave.approve', 'Approve leave', '/hrm/leaves/approve/**', 'POST', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.payroll.view', 'View payroll', '/hrm/payroll/**', 'GET', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.payroll.create', 'Process payroll', '/hrm/payroll/save', 'POST', 'HRM', 'HRM', true, NOW(), NOW()),
       ('hrm.payroll.approve', 'Approve payroll', '/hrm/payroll/approve/**', 'POST', 'HRM', 'HRM', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Production ───────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('prd.order.view', 'View production orders', '/production/orders/**', 'GET', 'PRODUCTION', 'PRODUCTION', true,
        NOW(), NOW()),
       ('prd.order.create', 'Create production order', '/production/orders/save', 'POST', 'PRODUCTION', 'PRODUCTION',
        true, NOW(), NOW()),
       ('prd.order.edit', 'Edit production order', '/production/orders/save', 'POST', 'PRODUCTION', 'PRODUCTION', true,
        NOW(), NOW()),
       ('prd.order.delete', 'Delete production order', '/production/orders/delete/**', 'DELETE', 'PRODUCTION',
        'PRODUCTION', true, NOW(), NOW()),
       ('prd.order.approve', 'Approve production order', '/production/orders/approve/**', 'POST', 'PRODUCTION',
        'PRODUCTION', true, NOW(), NOW()),
       ('prd.bom.view', 'View BOMs', '/production/boms/**', 'GET', 'PRODUCTION', 'PRODUCTION', true, NOW(), NOW()),
       ('prd.bom.create', 'Create BOM', '/production/boms/save', 'POST', 'PRODUCTION', 'PRODUCTION', true, NOW(),
        NOW()),
       ('prd.material_req.view', 'View material requisitions', '/production/material-req/**', 'GET', 'PRODUCTION',
        'PRODUCTION', true, NOW(), NOW()),
       ('prd.material_req.create', 'Create material requisition', '/production/material-req/save', 'POST', 'PRODUCTION',
        'PRODUCTION', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Commercial / LC ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('com.lc.view', 'View letters of credit', '/commercial/lc/**', 'GET', 'COMMERCIAL', 'COMMERCIAL', true, NOW(),
        NOW()),
       ('com.lc.create', 'Create LC', '/commercial/lc/save', 'POST', 'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
       ('com.lc.edit', 'Amend LC', '/commercial/lc/save', 'POST', 'COMMERCIAL', 'COMMERCIAL', true, NOW(), NOW()),
       ('com.lc.delete', 'Delete LC', '/commercial/lc/delete/**', 'DELETE', 'COMMERCIAL', 'COMMERCIAL', true, NOW(),
        NOW()),
       ('com.export.view', 'View export documents', '/commercial/exports/**', 'GET', 'COMMERCIAL', 'COMMERCIAL', true,
        NOW(), NOW()),
       ('com.export.create', 'Create export document', '/commercial/exports/save', 'POST', 'COMMERCIAL', 'COMMERCIAL',
        true, NOW(), NOW()),
       ('com.import.view', 'View import documents', '/commercial/imports/**', 'GET', 'COMMERCIAL', 'COMMERCIAL', true,
        NOW(), NOW()),
       ('com.import.create', 'Create import document', '/commercial/imports/save', 'POST', 'COMMERCIAL', 'COMMERCIAL',
        true, NOW(), NOW()),
       ('com.settlement.view', 'View LC settlements', '/commercial/settlements/**', 'GET', 'COMMERCIAL', 'COMMERCIAL',
        true, NOW(), NOW()),
       ('com.settlement.create', 'Create LC settlement', '/commercial/settlements/save', 'POST', 'COMMERCIAL',
        'COMMERCIAL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── CRM ──────────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('crm.lead.view', 'View CRM leads', '/crm/leads/**', 'GET', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.lead.create', 'Create lead', '/crm/leads/save', 'POST', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.lead.edit', 'Edit lead', '/crm/leads/save', 'POST', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.lead.delete', 'Delete lead', '/crm/leads/delete/**', 'DELETE', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.opportunity.view', 'View opportunities', '/crm/opportunities/**', 'GET', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.opportunity.create', 'Create opportunity', '/crm/opportunities/save', 'POST', 'CRM', 'CRM', true, NOW(),
        NOW()),
       ('crm.opportunity.edit', 'Edit opportunity', '/crm/opportunities/save', 'POST', 'CRM', 'CRM', true, NOW(),
        NOW()),
       ('crm.opportunity.delete', 'Delete opportunity', '/crm/opportunities/delete/**', 'DELETE', 'CRM', 'CRM', true,
        NOW(), NOW()),
       ('crm.contact.view', 'View CRM contacts', '/crm/contacts/**', 'GET', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.contact.create', 'Create contact', '/crm/contacts/save', 'POST', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.contact.edit', 'Edit contact', '/crm/contacts/save', 'POST', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.contact.delete', 'Delete contact', '/crm/contacts/delete/**', 'DELETE', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.activity.view', 'View CRM activities', '/crm/activities/**', 'GET', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.activity.create', 'Create activity', '/crm/activities/save', 'POST', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.activity.edit', 'Edit activity', '/crm/activities/save', 'POST', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.activity.delete', 'Delete activity', '/crm/activities/delete/**', 'DELETE', 'CRM', 'CRM', true, NOW(),
        NOW()),
       ('crm.feedback.view', 'View customer feedback', '/crm/feedback/**', 'GET', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.feedback.create', 'Create feedback entry', '/crm/feedback/save', 'POST', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.feedback.edit', 'Edit feedback entry', '/crm/feedback/save', 'POST', 'CRM', 'CRM', true, NOW(), NOW()),
       ('crm.feedback.delete', 'Delete feedback entry', '/crm/feedback/delete/**', 'DELETE', 'CRM', 'CRM', true, NOW(),
        NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Budget ───────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('budget.fiscalyear.view', 'View fiscal years', '/budget/fiscal-years/**', 'GET', 'BUDGET', 'BUDGET', true,
        NOW(), NOW()),
       ('budget.fiscalyear.create', 'Create fiscal year', '/budget/fiscal-years/save', 'POST', 'BUDGET', 'BUDGET', true,
        NOW(), NOW()),
       ('budget.fiscalyear.edit', 'Edit fiscal year', '/budget/fiscal-years/save', 'POST', 'BUDGET', 'BUDGET', true,
        NOW(), NOW()),
       ('budget.fiscalyear.delete', 'Delete fiscal year', '/budget/fiscal-years/delete/**', 'DELETE', 'BUDGET',
        'BUDGET', true, NOW(), NOW()),
       ('budget.head.view', 'View budget heads', '/budget/heads/**', 'GET', 'BUDGET', 'BUDGET', true, NOW(), NOW()),
       ('budget.head.create', 'Create budget head', '/budget/heads/save', 'POST', 'BUDGET', 'BUDGET', true, NOW(),
        NOW()),
       ('budget.head.edit', 'Edit budget head', '/budget/heads/save', 'POST', 'BUDGET', 'BUDGET', true, NOW(), NOW()),
       ('budget.head.delete', 'Delete budget head', '/budget/heads/delete/**', 'DELETE', 'BUDGET', 'BUDGET', true,
        NOW(), NOW()),
       ('budget.budget.view', 'View budgets', '/budget/list/**', 'GET', 'BUDGET', 'BUDGET', true, NOW(), NOW()),
       ('budget.budget.create', 'Create budget', '/budget/list/save', 'POST', 'BUDGET', 'BUDGET', true, NOW(), NOW()),
       ('budget.budget.edit', 'Edit budget', '/budget/list/save', 'POST', 'BUDGET', 'BUDGET', true, NOW(), NOW()),
       ('budget.budget.delete', 'Delete budget', '/budget/list/delete/**', 'DELETE', 'BUDGET', 'BUDGET', true, NOW(),
        NOW()),
       ('budget.revision.view', 'View budget revisions', '/budget/revisions/**', 'GET', 'BUDGET', 'BUDGET', true, NOW(),
        NOW()),
       ('budget.revision.create', 'Create budget revision', '/budget/revisions/save', 'POST', 'BUDGET', 'BUDGET', true,
        NOW(), NOW()),
       ('budget.transfer.view', 'View budget transfers', '/budget/transfers/**', 'GET', 'BUDGET', 'BUDGET', true, NOW(),
        NOW()),
       ('budget.transfer.create', 'Create budget transfer', '/budget/transfers/save', 'POST', 'BUDGET', 'BUDGET', true,
        NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Fixed Assets ─────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('fa.category.view', 'View asset categories', '/fixed-assets/categories/**', 'GET', 'FIXED_ASSETS',
        'FIXED_ASSETS', true, NOW(), NOW()),
       ('fa.category.create', 'Create asset category', '/fixed-assets/categories/save', 'POST', 'FIXED_ASSETS',
        'FIXED_ASSETS', true, NOW(), NOW()),
       ('fa.category.edit', 'Edit asset category', '/fixed-assets/categories/save', 'POST', 'FIXED_ASSETS',
        'FIXED_ASSETS', true, NOW(), NOW()),
       ('fa.category.delete', 'Delete asset category', '/fixed-assets/categories/delete/**', 'DELETE', 'FIXED_ASSETS',
        'FIXED_ASSETS', true, NOW(), NOW()),
       ('fa.asset.view', 'View assets register', '/fixed-assets/assets/**', 'GET', 'FIXED_ASSETS', 'FIXED_ASSETS', true,
        NOW(), NOW()),
       ('fa.asset.create', 'Register asset', '/fixed-assets/assets/save', 'POST', 'FIXED_ASSETS', 'FIXED_ASSETS', true,
        NOW(), NOW()),
       ('fa.asset.edit', 'Edit asset', '/fixed-assets/assets/save', 'POST', 'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(),
        NOW()),
       ('fa.asset.delete', 'Delete asset', '/fixed-assets/assets/delete/**', 'DELETE', 'FIXED_ASSETS', 'FIXED_ASSETS',
        true, NOW(), NOW()),
       ('fa.depreciation.view', 'View depreciation runs', '/fixed-assets/depreciation/**', 'GET', 'FIXED_ASSETS',
        'FIXED_ASSETS', true, NOW(), NOW()),
       ('fa.depreciation.calculate', 'Calculate depreciation', '/fixed-assets/depreciation/calculate', 'POST',
        'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
       ('fa.depreciation.post', 'Post depreciation run', '/fixed-assets/depreciation/post/**', 'POST', 'FIXED_ASSETS',
        'FIXED_ASSETS', true, NOW(), NOW()),
       ('fa.depreciation.reverse', 'Reverse depreciation run', '/fixed-assets/depreciation/reverse/**', 'POST',
        'FIXED_ASSETS', 'FIXED_ASSETS', true, NOW(), NOW()),
       ('fa.disposal.view', 'View asset disposals', '/fixed-assets/disposals/**', 'GET', 'FIXED_ASSETS', 'FIXED_ASSETS',
        true, NOW(), NOW()),
       ('fa.disposal.create', 'Dispose asset', '/fixed-assets/disposals/save', 'POST', 'FIXED_ASSETS', 'FIXED_ASSETS',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Approval ─────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('apr.config.view', 'View approval configs', '/approval/configs/**', 'GET', 'CORE_SECURITY', 'APPROVAL', true,
        NOW(), NOW()),
       ('apr.config.create', 'Create approval config', '/approval/configs/save', 'POST', 'CORE_SECURITY', 'APPROVAL',
        true, NOW(), NOW()),
       ('apr.request.view', 'View approval requests', '/approval/requests/**', 'GET', 'CORE_SECURITY', 'APPROVAL', true,
        NOW(), NOW()),
       ('apr.request.approve', 'Approve requests', '/approval/requests/approve/**', 'POST', 'CORE_SECURITY', 'APPROVAL',
        true, NOW(), NOW()),
       ('apr.request.reject', 'Reject requests', '/approval/requests/reject/**', 'POST', 'CORE_SECURITY', 'APPROVAL',
        true, NOW(), NOW()),
       ('apr.delegation.view', 'View delegations', '/approval/delegations/**', 'GET', 'CORE_SECURITY', 'APPROVAL', true,
        NOW(), NOW()),
       ('apr.delegation.create', 'Create delegation', '/approval/delegations/save', 'POST', 'CORE_SECURITY', 'APPROVAL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 2. ROLES
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO sec_roles (name, name_bn, description, master_role, active, created_at, updated_at)
VALUES ('ROLE_SUPER_ADMIN', 'সুপার অ্যাডমিন', 'Full system access — bypasses all permission checks', 'ROLE_SUPER_ADMIN',
        true, NOW(), NOW()),
       ('ROLE_ACCOUNTS_ADMIN', 'হিসাব প্রশাসক', 'Full accounts/finance access, view purchase/sales',
        'ROLE_ACCOUNTS_ADMIN', true, NOW(), NOW()),
       ('ROLE_ACCOUNTANT', 'হিসাবরক্ষক', 'Journal, payment, receipt vouchers; view reports', 'ROLE_ACCOUNTANT', true,
        NOW(), NOW()),
       ('ROLE_INVENTORY_MANAGER', 'ইনভেন্টরি ম্যানেজার', 'Full inventory management', 'ROLE_INVENTORY_MANAGER', true,
        NOW(), NOW()),
       ('ROLE_WAREHOUSE_STAFF', 'ওয়্যারহাউস স্টাফ', 'View stock, create GRN, stock transfer', 'ROLE_WAREHOUSE_STAFF',
        true, NOW(), NOW()),
       ('ROLE_PURCHASE_MANAGER', 'ক্রয় ম্যানেজার', 'Full purchase cycle', 'ROLE_PURCHASE_MANAGER', true, NOW(), NOW()),
       ('ROLE_PURCHASE_OFFICER', 'ক্রয় কর্মকর্তা', 'PO, GRN create/edit; view invoices', 'ROLE_PURCHASE_OFFICER', true,
        NOW(), NOW()),
       ('ROLE_SALES_MANAGER', 'বিক্রয় ম্যানেজার', 'Full sales cycle', 'ROLE_SALES_MANAGER', true, NOW(), NOW()),
       ('ROLE_SALES_EXECUTIVE', 'বিক্রয় নির্বাহী', 'SO create/edit; view delivery, invoice, receipt',
        'ROLE_SALES_EXECUTIVE', true, NOW(), NOW()),
       ('ROLE_HRM', 'এইচআরএম ম্যানেজার', 'Full HRM access', 'ROLE_HRM', true, NOW(), NOW()),
       ('ROLE_PRODUCTION_MANAGER', 'উৎপাদন ম্যানেজার', 'Full production access', 'ROLE_PRODUCTION_MANAGER', true, NOW(),
        NOW()),
       ('ROLE_PRODUCTION_SUPERVISOR', 'উৎপাদন সুপারভাইজার', 'View production, create material requisitions',
        'ROLE_PRODUCTION_SUPERVISOR', true, NOW(), NOW()),
       ('ROLE_COMMERCIAL_MANAGER', 'বাণিজ্যিক ম্যানেজার', 'Full commercial/LC access', 'ROLE_COMMERCIAL_MANAGER', true,
        NOW(), NOW()),
       ('ROLE_COMMERCIAL_EXECUTIVE', 'বাণিজ্যিক নির্বাহী', 'View and create commercial/export/import docs',
        'ROLE_COMMERCIAL_EXECUTIVE', true, NOW(), NOW()),
       ('ROLE_CRM_MANAGER', 'সিআরএম ম্যানেজার', 'Full CRM access: leads, opportunities, contacts', 'ROLE_CRM_MANAGER',
        true, NOW(), NOW()),
       ('ROLE_CRM_EXECUTIVE', 'সিআরএম নির্বাহী', 'View leads/opportunities, create activities', 'ROLE_CRM_EXECUTIVE',
        true, NOW(), NOW()),
       ('ROLE_BUDGET_MANAGER', 'বাজেট ম্যানেজার', 'Full budget management: fiscal years, heads, budgets',
        'ROLE_BUDGET_MANAGER', true, NOW(), NOW()),
       ('ROLE_ASSET_MANAGER', 'সম্পদ ব্যবস্থাপক', 'Full fixed assets: register, depreciation, disposal',
        'ROLE_ASSET_MANAGER', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 3. APP_MENUS  (MODULE → GROUP → LEAF)
-- ═════════════════════════════════════════════════════════════════════════════

-- ── 3A. MODULE level ─────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
VALUES ('MOD_DASHBOARD', 'Dashboard', '/dashboard', 'fa fa-th-large', NULL, 10, 'MODULE', 'DASHBOARD', 'dashboard.view',
        '_self', true, true, false, NOW(), NOW()),
       ('MOD_INVENTORY', 'Inventory', NULL, 'fa fa-boxes', NULL, 20, 'MODULE', 'INVENTORY', NULL, '_self', true, true,
        false, NOW(), NOW()),
       ('MOD_PURCHASE', 'Purchase', NULL, 'fa fa-shopping-cart', NULL, 30, 'MODULE', 'PURCHASE', NULL, '_self', true,
        true, false, NOW(), NOW()),
       ('MOD_SALES', 'Sales', NULL, 'fa fa-tags', NULL, 40, 'MODULE', 'SALES', NULL, '_self', true, true, false, NOW(),
        NOW()),
       ('MOD_ACCOUNTS', 'Accounts', NULL, 'fa fa-calculator', NULL, 50, 'MODULE', 'ACCOUNTS', NULL, '_self', true, true,
        false, NOW(), NOW()),
       ('MOD_HRM', 'HRM', NULL, 'fa fa-users', NULL, 60, 'MODULE', 'HRM', NULL, '_self', true, true, false, NOW(),
        NOW()),
       ('MOD_PRODUCTION', 'Production', NULL, 'fa fa-industry', NULL, 70, 'MODULE', 'PRODUCTION', NULL, '_self', true,
        true, false, NOW(), NOW()),
       ('MOD_COMMERCIAL', 'Commercial', NULL, 'fa fa-ship', NULL, 80, 'MODULE', 'COMMERCIAL', NULL, '_self', true, true,
        false, NOW(), NOW()),
       ('MOD_CRM', 'CRM', NULL, 'fa fa-handshake', NULL, 90, 'MODULE', 'CRM', 'crm.lead.view', '_self', true, true,
        false, NOW(), NOW()),
       ('MOD_BUDGET', 'Budget', NULL, 'fa fa-chart-pie', NULL, 100, 'MODULE', 'BUDGET', 'budget.budget.view', '_self',
        true, true, false, NOW(), NOW()),
       ('MOD_FIXED_ASSETS', 'Fixed Assets', NULL, 'fa fa-building', NULL, 110, 'MODULE', 'FIXED_ASSETS',
        'fa.asset.view', '_self', true, true, false, NOW(), NOW()),
       ('MOD_SETUP', 'Setup', NULL, 'fa fa-cogs', NULL, 120, 'MODULE', 'SETUP', NULL, '_self', true, true, false, NOW(),
        NOW()),
       ('MOD_SECURITY', 'Security', NULL, 'fa fa-shield-alt', NULL, 130, 'MODULE', 'SECURITY', NULL, '_self', true,
        true, false, NOW(), NOW()),
       ('MOD_APPROVALS', 'Approvals', NULL, 'fa fa-tasks', NULL, 140, 'MODULE', 'APPROVALS', 'apr.request.view',
        '_self', true, true, false, NOW(), NOW()),
       ('MOD_REPORTS', 'Reports', '/reports', 'fa fa-chart-bar', NULL, 150, 'MODULE', 'REPORTS', 'reports.view',
        '_self', true, true, false, NOW(), NOW())
ON CONFLICT (menu_code) DO NOTHING;

-- ── 3B. GROUP level ──────────────────────────────────────────────────────────

-- INVENTORY groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_INV_MASTER',
       'Item Master',
       NULL,
       'fa fa-layer-group',
       m.id,
       10,
       'GROUP',
       'INVENTORY',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_INVENTORY'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_INV_STOCK',
       'Stock Management',
       NULL,
       'fa fa-warehouse',
       m.id,
       20,
       'GROUP',
       'INVENTORY',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_INVENTORY'
ON CONFLICT (menu_code) DO NOTHING;

-- PURCHASE groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_MASTER',
       'Suppliers',
       NULL,
       'fa fa-truck',
       m.id,
       10,
       'GROUP',
       'PURCHASE',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_PURCHASE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_CYCLE',
       'Purchase Cycle',
       NULL,
       'fa fa-file-invoice',
       m.id,
       20,
       'GROUP',
       'PURCHASE',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_PURCHASE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PUR_RETURNS',
       'Returns & Payments',
       NULL,
       'fa fa-undo',
       m.id,
       30,
       'GROUP',
       'PURCHASE',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_PURCHASE'
ON CONFLICT (menu_code) DO NOTHING;

-- SALES groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_MASTER',
       'Customers',
       NULL,
       'fa fa-user-tie',
       m.id,
       10,
       'GROUP',
       'SALES',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_SALES'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_CYCLE',
       'Sales Cycle',
       NULL,
       'fa fa-file-invoice-dollar',
       m.id,
       20,
       'GROUP',
       'SALES',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_SALES'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SAL_RETURNS',
       'Returns & Receipts',
       NULL,
       'fa fa-undo-alt',
       m.id,
       30,
       'GROUP',
       'SALES',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_SALES'
ON CONFLICT (menu_code) DO NOTHING;

-- ACCOUNTS groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_MASTER',
       'Chart & Accounts',
       NULL,
       'fa fa-list-alt',
       m.id,
       10,
       'GROUP',
       'ACCOUNTS',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_VOUCHERS',
       'Vouchers',
       NULL,
       'fa fa-receipt',
       m.id,
       20,
       'GROUP',
       'ACCOUNTS',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_REPORTS',
       'Financial Reports',
       NULL,
       'fa fa-chart-line',
       m.id,
       30,
       'GROUP',
       'ACCOUNTS',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_CONFIG',
       'Accounts Config',
       NULL,
       'fa fa-sliders-h',
       m.id,
       40,
       'GROUP',
       'ACCOUNTS',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;

-- HRM groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_MASTER',
       'HR Master',
       NULL,
       'fa fa-id-card',
       m.id,
       10,
       'GROUP',
       'HRM',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_HRM'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_ATTENDANCE',
       'Attendance & Leave',
       NULL,
       'fa fa-calendar-check',
       m.id,
       20,
       'GROUP',
       'HRM',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_HRM'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_HRM_PAYROLL',
       'Payroll',
       NULL,
       'fa fa-money-bill-wave',
       m.id,
       30,
       'GROUP',
       'HRM',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_HRM'
ON CONFLICT (menu_code) DO NOTHING;

-- PRODUCTION groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PRD_MASTER',
       'Production Master',
       NULL,
       'fa fa-layer-group',
       m.id,
       10,
       'GROUP',
       'PRODUCTION',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_PRODUCTION'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_PRD_ORDERS',
       'Production Orders',
       NULL,
       'fa fa-clipboard-list',
       m.id,
       20,
       'GROUP',
       'PRODUCTION',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_PRODUCTION'
ON CONFLICT (menu_code) DO NOTHING;

-- COMMERCIAL groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_COM_LC',
       'Letter of Credit',
       NULL,
       'fa fa-file-contract',
       m.id,
       10,
       'GROUP',
       'COMMERCIAL',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_COMMERCIAL'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_COM_TRADE',
       'Trade Documents',
       NULL,
       'fa fa-globe',
       m.id,
       20,
       'GROUP',
       'COMMERCIAL',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_COMMERCIAL'
ON CONFLICT (menu_code) DO NOTHING;

-- CRM groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_CRM_PIPELINE',
       'Pipeline',
       NULL,
       'fa fa-filter',
       m.id,
       10,
       'GROUP',
       'CRM',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_CRM'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_CRM_ENGAGE',
       'Engagement',
       NULL,
       'fa fa-comments',
       m.id,
       20,
       'GROUP',
       'CRM',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_CRM'
ON CONFLICT (menu_code) DO NOTHING;

-- BUDGET groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_BGT_MASTER',
       'Budget Master',
       NULL,
       'fa fa-layer-group',
       m.id,
       10,
       'GROUP',
       'BUDGET',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_BUDGET'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_BGT_TRANSACTIONS',
       'Budget Transactions',
       NULL,
       'fa fa-exchange-alt',
       m.id,
       20,
       'GROUP',
       'BUDGET',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_BUDGET'
ON CONFLICT (menu_code) DO NOTHING;

-- FIXED ASSETS groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_FA_MASTER',
       'Asset Master',
       NULL,
       'fa fa-layer-group',
       m.id,
       10,
       'GROUP',
       'FIXED_ASSETS',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_FIXED_ASSETS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_FA_OPERATIONS',
       'Asset Operations',
       NULL,
       'fa fa-cogs',
       m.id,
       20,
       'GROUP',
       'FIXED_ASSETS',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_FIXED_ASSETS'
ON CONFLICT (menu_code) DO NOTHING;

-- SETUP groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_STP_ORG',
       'Organization',
       NULL,
       'fa fa-building',
       m.id,
       10,
       'GROUP',
       'SETUP',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_SETUP'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_STP_REF',
       'Reference Data',
       NULL,
       'fa fa-book',
       m.id,
       20,
       'GROUP',
       'SETUP',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_SETUP'
ON CONFLICT (menu_code) DO NOTHING;

-- SECURITY groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SEC_USER',
       'Users & Roles',
       NULL,
       'fa fa-user-shield',
       m.id,
       10,
       'GROUP',
       'SECURITY',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_SECURITY'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SEC_MENU',
       'Menu & Permissions',
       NULL,
       'fa fa-sitemap',
       m.id,
       20,
       'GROUP',
       'SECURITY',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_SECURITY'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_SEC_ORG',
       'Org Administration',
       NULL,
       'fa fa-building',
       m.id,
       30,
       'GROUP',
       'SECURITY',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_SECURITY'
ON CONFLICT (menu_code) DO NOTHING;

-- APPROVALS groups
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_APR_CONFIG',
       'Approval Setup',
       NULL,
       'fa fa-tools',
       m.id,
       10,
       'GROUP',
       'APPROVALS',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_APPROVALS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_APR_PENDING',
       'My Approvals',
       NULL,
       'fa fa-clock',
       m.id,
       20,
       'GROUP',
       'APPROVALS',
       NULL,
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus m
WHERE m.menu_code = 'MOD_APPROVALS'
ON CONFLICT (menu_code) DO NOTHING;


-- ── 3C. LEAF level ───────────────────────────────────────────────────────────

-- ── INVENTORY ────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_DASHBOARD',
       'Dashboard',
       '/inventory/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'INVENTORY',
       'inv.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_INVENTORY'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_UOM',
       'Units of Measure',
       '/inventory/uoms',
       'fa fa-ruler',
       g.id,
       10,
       'LEAF',
       'INVENTORY',
       'inv.uom.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_INV_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_CATEGORY',
       'Item Categories',
       '/inventory/categories',
       'fa fa-sitemap',
       g.id,
       20,
       'LEAF',
       'INVENTORY',
       'inv.category.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_INV_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_BRAND',
       'Brands',
       '/inventory/brands',
       'fa fa-trademark',
       g.id,
       30,
       'LEAF',
       'INVENTORY',
       'inv.brand.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_INV_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_MODEL',
       'Item Models',
       '/inventory/models',
       'fa fa-cube',
       g.id,
       40,
       'LEAF',
       'INVENTORY',
       'inv.model.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_INV_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_ITEM',
       'Items / Products',
       '/inventory/items',
       'fa fa-boxes',
       g.id,
       50,
       'LEAF',
       'INVENTORY',
       'inv.item.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_INV_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_STOCK',
       'Stock Ledger',
       '/inventory/stocks',
       'fa fa-dolly',
       g.id,
       10,
       'LEAF',
       'INVENTORY',
       'inv.stock.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_INV_STOCK'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_ADJUSTMENT',
       'Stock Adjustments',
       '/inventory/adjustments',
       'fa fa-balance-scale',
       g.id,
       20,
       'LEAF',
       'INVENTORY',
       'inv.adjustment.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_INV_STOCK'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'INV_TRANSFER',
       'Stock Transfer',
       '/inventory/transfers',
       'fa fa-exchange-alt',
       g.id,
       30,
       'LEAF',
       'INVENTORY',
       'inv.transfer.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_INV_STOCK'
ON CONFLICT (menu_code) DO NOTHING;

-- ── PURCHASE ─────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_DASHBOARD',
       'Dashboard',
       '/purchase/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'PURCHASE',
       'pur.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_PURCHASE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_SUPPLIER',
       'Suppliers',
       '/purchase/suppliers',
       'fa fa-truck-loading',
       g.id,
       10,
       'LEAF',
       'PURCHASE',
       'pur.supplier.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PUR_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_PO',
       'Purchase Orders',
       '/purchase/orders',
       'fa fa-file-alt',
       g.id,
       10,
       'LEAF',
       'PURCHASE',
       'pur.po.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PUR_CYCLE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_GRN',
       'Goods Receipt (GRN)',
       '/purchase/grns',
       'fa fa-clipboard-check',
       g.id,
       20,
       'LEAF',
       'PURCHASE',
       'pur.grn.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PUR_CYCLE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_INVOICE',
       'Purchase Invoices',
       '/purchase/invoices',
       'fa fa-file-invoice',
       g.id,
       30,
       'LEAF',
       'PURCHASE',
       'pur.invoice.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PUR_CYCLE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_DEBIT_NOTE',
       'Debit Notes',
       '/purchase/debit-notes',
       'fa fa-file-minus',
       g.id,
       10,
       'LEAF',
       'PURCHASE',
       'pur.debit_note.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PUR_RETURNS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PUR_PAYMENT',
       'Payment Vouchers',
       '/purchase/payments',
       'fa fa-money-check',
       g.id,
       20,
       'LEAF',
       'PURCHASE',
       'pur.payment.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PUR_RETURNS'
ON CONFLICT (menu_code) DO NOTHING;

-- ── SALES ────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_DASHBOARD',
       'Dashboard',
       '/sales/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'SALES',
       'sal.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_SALES'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_CUSTOMER',
       'Customers',
       '/sales/customers',
       'fa fa-user-friends',
       g.id,
       10,
       'LEAF',
       'SALES',
       'sal.customer.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SAL_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_SO',
       'Sales Orders',
       '/sales/orders',
       'fa fa-file-alt',
       g.id,
       10,
       'LEAF',
       'SALES',
       'sal.so.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SAL_CYCLE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_DELIVERY',
       'Delivery Notes',
       '/sales/deliveries',
       'fa fa-shipping-fast',
       g.id,
       20,
       'LEAF',
       'SALES',
       'sal.delivery.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SAL_CYCLE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_INVOICE',
       'Sales Invoices',
       '/sales/invoices',
       'fa fa-file-invoice-dollar',
       g.id,
       30,
       'LEAF',
       'SALES',
       'sal.invoice.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SAL_CYCLE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_CREDIT_NOTE',
       'Credit Notes',
       '/sales/credit-notes',
       'fa fa-file-minus',
       g.id,
       10,
       'LEAF',
       'SALES',
       'sal.credit_note.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SAL_RETURNS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SAL_RECEIPT',
       'Receipt Vouchers',
       '/sales/receipts',
       'fa fa-money-bill',
       g.id,
       20,
       'LEAF',
       'SALES',
       'sal.receipt.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SAL_RETURNS'
ON CONFLICT (menu_code) DO NOTHING;

-- ── ACCOUNTS ─────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_DASHBOARD',
       'Dashboard',
       '/accounts/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'ACCOUNTS',
       'acc.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_COA',
       'Chart of Accounts',
       '/accounts/chart-of-accounts',
       'fa fa-project-diagram',
       g.id,
       10,
       'LEAF',
       'ACCOUNTS',
       'acc.coa.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_SUBACC',
       'Sub-Accounts',
       '/accounts/sub-accounts',
       'fa fa-code-branch',
       g.id,
       20,
       'LEAF',
       'ACCOUNTS',
       'acc.sub.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_BANK_ACC',
       'Bank Accounts',
       '/accounts/bank-accounts',
       'fa fa-university',
       g.id,
       30,
       'LEAF',
       'ACCOUNTS',
       'acc.bank_acc.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_JOURNAL',
       'Journal Vouchers',
       '/accounts/journals',
       'fa fa-book-open',
       g.id,
       10,
       'LEAF',
       'ACCOUNTS',
       'acc.jv.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_VOUCHERS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_PAYMENT',
       'Payment Vouchers',
       '/accounts/payment-vouchers',
       'fa fa-hand-holding-usd',
       g.id,
       20,
       'LEAF',
       'ACCOUNTS',
       'acc.pv.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_VOUCHERS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_RECEIPT',
       'Receipt Vouchers',
       '/accounts/receipt-vouchers',
       'fa fa-hand-holding',
       g.id,
       30,
       'LEAF',
       'ACCOUNTS',
       'acc.rv.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_VOUCHERS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_CONTRA',
       'Contra Vouchers',
       '/accounts/contra-vouchers',
       'fa fa-right-left',
       g.id,
       40,
       'LEAF',
       'ACCOUNTS',
       'acc.cv.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_VOUCHERS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_LEDGER',
       'General Ledger',
       '/accounts/ledger',
       'fa fa-book',
       g.id,
       10,
       'LEAF',
       'ACCOUNTS',
       'acc.ledger.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_TRIAL_BAL',
       'Trial Balance',
       '/accounts/trial-balance',
       'fa fa-balance-scale',
       g.id,
       20,
       'LEAF',
       'ACCOUNTS',
       'acc.trial_bal.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_PNL',
       'Profit & Loss',
       '/accounts/profit-loss',
       'fa fa-chart-line',
       g.id,
       30,
       'LEAF',
       'ACCOUNTS',
       'acc.profit_loss.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_BAL_SHEET',
       'Balance Sheet',
       '/accounts/balance-sheet',
       'fa fa-file-chart-pie',
       g.id,
       40,
       'LEAF',
       'ACCOUNTS',
       'acc.balance_sheet.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_AGING',
       'AP/AR Aging',
       '/accounts/aging',
       'fa fa-clock',
       g.id,
       50,
       'LEAF',
       'ACCOUNTS',
       'acc.aging.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_MAPPING',
       'Account Mappings',
       '/accounts/mapping',
       'fa fa-link',
       g.id,
       10,
       'LEAF',
       'ACCOUNTS',
       'acc.mapping.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_PERIODS',
       'Accounting Periods',
       '/accounts/periods',
       'fa fa-calendar-alt',
       g.id,
       20,
       'LEAF',
       'ACCOUNTS',
       'acc.period.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_OB',
       'Opening Balances',
       '/accounts/opening-balances',
       'fa fa-flag',
       g.id,
       30,
       'LEAF',
       'ACCOUNTS',
       'acc.ob.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_POLICY',
       'Accounts Policy',
       '/accounts/policy',
       'fa fa-cog',
       g.id,
       40,
       'LEAF',
       'ACCOUNTS',
       'acc.policy.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_ACC_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;

-- ── HRM ──────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_DASHBOARD',
       'Dashboard',
       '/hrm/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'HRM',
       'hrm.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_HRM'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_DESIG',
       'Designations',
       '/hrm/designations',
       'fa fa-id-badge',
       g.id,
       10,
       'LEAF',
       'HRM',
       'hrm.designation.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_HRM_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_EMPLOYEE',
       'Employees',
       '/hrm/employees',
       'fa fa-user-tie',
       g.id,
       20,
       'LEAF',
       'HRM',
       'hrm.employee.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_HRM_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_ATTENDANCE',
       'Attendance',
       '/hrm/attendance',
       'fa fa-calendar-alt',
       g.id,
       10,
       'LEAF',
       'HRM',
       'hrm.attendance.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_HRM_ATTENDANCE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_LEAVE',
       'Leave Applications',
       '/hrm/leaves',
       'fa fa-calendar-times',
       g.id,
       20,
       'LEAF',
       'HRM',
       'hrm.leave.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_HRM_ATTENDANCE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'HRM_PAYROLL',
       'Payroll Processing',
       '/hrm/payroll',
       'fa fa-money-bill-alt',
       g.id,
       10,
       'LEAF',
       'HRM',
       'hrm.payroll.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_HRM_PAYROLL'
ON CONFLICT (menu_code) DO NOTHING;

-- ── PRODUCTION ────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PRD_DASHBOARD',
       'Dashboard',
       '/production/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'PRODUCTION',
       'prd.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_PRODUCTION'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PRD_BOM',
       'Bill of Materials',
       '/production/boms',
       'fa fa-list-check',
       g.id,
       10,
       'LEAF',
       'PRODUCTION',
       'prd.bom.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PRD_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PRD_ORDER',
       'Production Orders',
       '/production/orders',
       'fa fa-cogs',
       g.id,
       10,
       'LEAF',
       'PRODUCTION',
       'prd.order.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PRD_ORDERS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PRD_MAT_REQ',
       'Material Requisitions',
       '/production/material-req',
       'fa fa-cubes',
       g.id,
       20,
       'LEAF',
       'PRODUCTION',
       'prd.material_req.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_PRD_ORDERS'
ON CONFLICT (menu_code) DO NOTHING;

-- ── COMMERCIAL ────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_DASHBOARD',
       'Dashboard',
       '/commercial/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'COMMERCIAL',
       'com.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_COMMERCIAL'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_LC',
       'Letters of Credit',
       '/commercial/lc',
       'fa fa-file-contract',
       g.id,
       10,
       'LEAF',
       'COMMERCIAL',
       'com.lc.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_COM_LC'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_SETTLEMENT',
       'LC Settlements',
       '/commercial/settlements',
       'fa fa-handshake',
       g.id,
       20,
       'LEAF',
       'COMMERCIAL',
       'com.settlement.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_COM_LC'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_EXPORT',
       'Export Documents',
       '/commercial/exports',
       'fa fa-plane-departure',
       g.id,
       10,
       'LEAF',
       'COMMERCIAL',
       'com.export.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_COM_TRADE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'COM_IMPORT',
       'Import Documents',
       '/commercial/imports',
       'fa fa-plane-arrival',
       g.id,
       20,
       'LEAF',
       'COMMERCIAL',
       'com.import.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_COM_TRADE'
ON CONFLICT (menu_code) DO NOTHING;

-- ── CRM ──────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_DASHBOARD',
       'Dashboard',
       '/crm/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'CRM',
       'crm.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_CRM'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_LEADS',
       'Leads',
       '/crm/leads',
       'fa fa-user-plus',
       g.id,
       10,
       'LEAF',
       'CRM',
       'crm.lead.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_CRM_PIPELINE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_OPP',
       'Opportunities',
       '/crm/opportunities',
       'fa fa-bullseye',
       g.id,
       20,
       'LEAF',
       'CRM',
       'crm.opportunity.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_CRM_PIPELINE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_CONTACTS',
       'Contacts',
       '/crm/contacts',
       'fa fa-address-book',
       g.id,
       10,
       'LEAF',
       'CRM',
       'crm.contact.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_CRM_ENGAGE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_ACTIVITIES',
       'Activities',
       '/crm/activities',
       'fa fa-calendar-check',
       g.id,
       20,
       'LEAF',
       'CRM',
       'crm.activity.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_CRM_ENGAGE'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_FEEDBACK',
       'Customer Feedback',
       '/crm/feedback',
       'fa fa-star',
       g.id,
       30,
       'LEAF',
       'CRM',
       'crm.feedback.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_CRM_ENGAGE'
ON CONFLICT (menu_code) DO NOTHING;

-- ── BUDGET ───────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_DASHBOARD',
       'Dashboard',
       '/budget/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'BUDGET',
       'budget.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_BUDGET'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_FISCAL_YEAR',
       'Fiscal Years',
       '/budget/fiscal-years',
       'fa fa-calendar',
       g.id,
       10,
       'LEAF',
       'BUDGET',
       'budget.fiscalyear.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_BGT_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_HEADS',
       'Budget Heads',
       '/budget/heads',
       'fa fa-layer-group',
       g.id,
       20,
       'LEAF',
       'BUDGET',
       'budget.head.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_BGT_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_BUDGETS',
       'Budgets',
       '/budget/list',
       'fa fa-file-invoice',
       g.id,
       10,
       'LEAF',
       'BUDGET',
       'budget.budget.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_BGT_TRANSACTIONS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_REVISIONS',
       'Budget Revisions',
       '/budget/revisions',
       'fa fa-edit',
       g.id,
       20,
       'LEAF',
       'BUDGET',
       'budget.revision.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_BGT_TRANSACTIONS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_TRANSFERS',
       'Budget Transfers',
       '/budget/transfers',
       'fa fa-exchange-alt',
       g.id,
       30,
       'LEAF',
       'BUDGET',
       'budget.transfer.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_BGT_TRANSACTIONS'
ON CONFLICT (menu_code) DO NOTHING;

-- ── FIXED ASSETS ──────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_DASHBOARD',
       'Dashboard',
       '/fixed-assets/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'FIXED_ASSETS',
       'fa.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_FIXED_ASSETS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_CATEGORIES',
       'Asset Categories',
       '/fixed-assets/categories',
       'fa fa-layer-group',
       g.id,
       10,
       'LEAF',
       'FIXED_ASSETS',
       'fa.category.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_FA_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_ASSETS',
       'Assets Register',
       '/fixed-assets/assets',
       'fa fa-building',
       g.id,
       20,
       'LEAF',
       'FIXED_ASSETS',
       'fa.asset.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_FA_MASTER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_DEPRECIATION',
       'Depreciation Runs',
       '/fixed-assets/depreciation',
       'fa fa-chart-line',
       g.id,
       10,
       'LEAF',
       'FIXED_ASSETS',
       'fa.depreciation.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_FA_OPERATIONS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_DISPOSALS',
       'Asset Disposals',
       '/fixed-assets/disposals',
       'fa fa-trash-can',
       g.id,
       20,
       'LEAF',
       'FIXED_ASSETS',
       'fa.disposal.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_FA_OPERATIONS'
ON CONFLICT (menu_code) DO NOTHING;

-- ── SETUP ────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_ORG',
       'Organizations',
       '/organizations',
       'fa fa-building',
       g.id,
       10,
       'LEAF',
       'SETUP',
       'org.organization.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_ORG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_BU',
       'Business Units',
       '/business-units',
       'fa fa-briefcase',
       g.id,
       20,
       'LEAF',
       'SETUP',
       'org.business_unit.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_ORG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_DEPT',
       'Departments',
       '/departments',
       'fa fa-sitemap',
       g.id,
       30,
       'LEAF',
       'SETUP',
       'org.department.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_ORG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_WAREHOUSE',
       'Warehouses',
       '/warehouses',
       'fa fa-warehouse',
       g.id,
       40,
       'LEAF',
       'SETUP',
       'org.warehouse.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_ORG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_COST_CTR',
       'Cost Centers',
       '/cost-centers',
       'fa fa-chart-pie',
       g.id,
       50,
       'LEAF',
       'SETUP',
       'org.cost_center.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_ORG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_BANK',
       'Banks',
       '/setup/banks',
       'fa fa-landmark',
       g.id,
       10,
       'LEAF',
       'SETUP',
       'setup.bank.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_REF'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_CURRENCY',
       'Currencies',
       '/setup/currencies',
       'fa fa-coins',
       g.id,
       20,
       'LEAF',
       'SETUP',
       'setup.currency.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_REF'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_TERMS',
       'Payment Terms',
       '/setup/terms',
       'fa fa-handshake',
       g.id,
       30,
       'LEAF',
       'SETUP',
       'setup.terms.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_REF'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_HS_CODE',
       'HS Codes',
       '/setup/hs-codes',
       'fa fa-barcode',
       g.id,
       40,
       'LEAF',
       'SETUP',
       'setup.hs_code.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_REF'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_SEQUENCE',
       'Document Sequences',
       '/setup/sequences',
       'fa fa-hashtag',
       g.id,
       50,
       'LEAF',
       'SETUP',
       'setup.sequence.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_STP_REF'
ON CONFLICT (menu_code) DO NOTHING;

-- ── SECURITY ─────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_DASHBOARD',
       'Security Dashboard',
       '/security/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'SECURITY',
       'security.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_SECURITY'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_USERS',
       'Users',
       '/users',
       'fa fa-user',
       g.id,
       10,
       'LEAF',
       'SECURITY',
       'security.user.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SEC_USER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_ROLES',
       'Roles',
       '/roles',
       'fa fa-user-shield',
       g.id,
       20,
       'LEAF',
       'SECURITY',
       'security.role.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SEC_USER'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_MENUS',
       'Menu Management',
       '/menus',
       'fa fa-bars',
       g.id,
       10,
       'LEAF',
       'SECURITY',
       'security.menu.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SEC_MENU'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_PERMISSIONS',
       'Permissions',
       '/permissions',
       'fa fa-key',
       g.id,
       20,
       'LEAF',
       'SECURITY',
       'security.permission.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SEC_MENU'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_ROLE_MENU',
       'Role Menu Access',
       '/role-menus',
       'fa fa-unlock-alt',
       g.id,
       30,
       'LEAF',
       'SECURITY',
       'security.rolemenu.manage',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SEC_MENU'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'SEC_ORG_MODULES',
       'Org Module Access',
       '/security/org-modules',
       'fa fa-th-large',
       g.id,
       10,
       'LEAF',
       'SECURITY',
       'security.org_module.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_SEC_ORG'
ON CONFLICT (menu_code) DO NOTHING;

-- ── APPROVALS ─────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'APR_DASHBOARD',
       'Dashboard',
       '/approval/dashboard',
       'fa fa-tachometer-alt',
       g.id,
       5,
       'LEAF',
       'APPROVALS',
       'apr.dashboard.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'MOD_APPROVALS'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'APR_CONFIG',
       'Approval Config',
       '/approval/configs',
       'fa fa-sliders-h',
       g.id,
       10,
       'LEAF',
       'APPROVALS',
       'apr.config.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_APR_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'APR_DELEGATION',
       'Delegations',
       '/approval/delegations',
       'fa fa-random',
       g.id,
       20,
       'LEAF',
       'APPROVALS',
       'apr.delegation.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_APR_CONFIG'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'APR_PENDING',
       'Pending Approvals',
       '/approval/requests',
       'fa fa-hourglass-half',
       g.id,
       10,
       'LEAF',
       'APPROVALS',
       'apr.request.view',
       '_self',
       true,
       true,
       false,
       NOW(),
       NOW()
FROM app_menus g
WHERE g.menu_code = 'GRP_APR_PENDING'
ON CONFLICT (menu_code) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 4. ROLE PERMISSIONS  (sec_role_permissions)
--    All permission names now use canonical names matching the INSERT above.
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_SUPER_ADMIN → wildcard only
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r,
     sec_permissions p
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND p.name = '*'
ON CONFLICT DO NOTHING;

-- ROLE_ACCOUNTS_ADMIN
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'acc.dashboard.view', 'acc.dashboard.summary',
                                              'acc.coa.view', 'acc.coa.create', 'acc.coa.edit', 'acc.coa.delete',
                                              'acc.coa.toggle',
                                              'acc.coa.list', 'acc.coa.show', 'acc.coa.search', 'acc.coa.tree',
                                              'acc.sub.view', 'acc.sub.create', 'acc.sub.edit', 'acc.sub.delete',
                                              'acc.sub.search',
                                              'acc.bank_acc.view', 'acc.bank_acc.create',
                                              'acc.mapping.view', 'acc.mapping.create', 'acc.mapping.edit',
                                              'acc.mapping.toggle', 'acc.mapping.delete',
                                              'acc.policy.view', 'acc.policy.create', 'acc.policy.toggle',
                                              'acc.policy.delete',
                                              'acc.period.view', 'acc.period.create', 'acc.period.edit',
                                              'acc.period.toggle', 'acc.period.delete',
                                              'acc.ob.view', 'acc.ob.create', 'acc.ob.post', 'acc.ob.delete',
                                              'acc.jv.view', 'acc.jv.list', 'acc.jv.show', 'acc.jv.create',
                                              'acc.jv.post', 'acc.jv.reverse', 'acc.jv.delete',
                                              'acc.pv.view', 'acc.pv.create',
                                              'acc.rv.view', 'acc.rv.create',
                                              'acc.cv.view', 'acc.cv.create',
                                              'acc.alloc.open_for_party',
                                              'acc.ledger.view', 'acc.trial_bal.view', 'acc.profit_loss.view',
                                              'acc.balance_sheet.view',
                                              'acc.aging.view', 'acc.aging.summary', 'acc.aging.detail',
                                              'pur.invoice.view', 'pur.payment.view',
                                              'sal.invoice.view', 'sal.receipt.view',
                                              'setup.bank.view', 'setup.currency.view', 'setup.terms.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_ACCOUNTS_ADMIN'
ON CONFLICT DO NOTHING;

-- ROLE_ACCOUNTANT
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'acc.dashboard.view', 'acc.dashboard.summary',
                                              'acc.coa.view', 'acc.coa.list', 'acc.coa.search', 'acc.coa.tree',
                                              'acc.sub.view', 'acc.bank_acc.view', 'acc.mapping.view',
                                              'acc.jv.view', 'acc.jv.list', 'acc.jv.show', 'acc.jv.create',
                                              'acc.jv.post',
                                              'acc.pv.view', 'acc.pv.create',
                                              'acc.rv.view', 'acc.rv.create',
                                              'acc.cv.view', 'acc.cv.create',
                                              'acc.alloc.open_for_party',
                                              'acc.ledger.view', 'acc.trial_bal.view', 'acc.profit_loss.view',
                                              'acc.balance_sheet.view',
                                              'acc.aging.view', 'acc.aging.summary',
                                              'pur.invoice.view', 'pur.payment.view',
                                              'sal.invoice.view', 'sal.receipt.view',
                                              'setup.bank.view', 'setup.currency.view', 'setup.terms.view',
                                              'apr.request.view', 'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_ACCOUNTANT'
ON CONFLICT DO NOTHING;

-- ROLE_INVENTORY_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'inv.dashboard.view', 'inv.dashboard.summary',
                                              'inv.uom.view', 'inv.uom.create', 'inv.uom.edit', 'inv.uom.delete',
                                              'inv.category.view', 'inv.category.create', 'inv.category.edit',
                                              'inv.category.delete',
                                              'inv.brand.view', 'inv.brand.create',
                                              'inv.model.view', 'inv.model.create',
                                              'inv.item.view', 'inv.item.create', 'inv.item.edit', 'inv.item.delete',
                                              'inv.stock.view',
                                              'inv.adjustment.view', 'inv.adjustment.create',
                                              'inv.transfer.view', 'inv.transfer.create',
                                              'org.warehouse.view',
                                              'setup.hs_code.view', 'setup.hs_code.create',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_INVENTORY_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_WAREHOUSE_STAFF
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view',
                                              'inv.item.view', 'inv.stock.view',
                                              'inv.transfer.view', 'inv.transfer.create',
                                              'pur.grn.view', 'pur.grn.create',
                                              'org.warehouse.view'
    )
WHERE r.name = 'ROLE_WAREHOUSE_STAFF'
ON CONFLICT DO NOTHING;

-- ROLE_PURCHASE_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'pur.dashboard.view', 'pur.dashboard.summary',
                                              'pur.supplier.view', 'pur.supplier.create', 'pur.supplier.edit',
                                              'pur.supplier.delete',
                                              'pur.po.view', 'pur.po.create', 'pur.po.edit', 'pur.po.delete',
                                              'pur.po.approve',
                                              'pur.grn.view', 'pur.grn.create', 'pur.grn.edit', 'pur.grn.delete',
                                              'pur.invoice.view', 'pur.invoice.create', 'pur.invoice.edit',
                                              'pur.invoice.delete',
                                              'pur.debit_note.view', 'pur.debit_note.create',
                                              'pur.payment.view', 'pur.payment.create',
                                              'inv.item.view', 'inv.stock.view',
                                              'setup.bank.view', 'setup.currency.view', 'setup.terms.view',
                                              'setup.hs_code.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_PURCHASE_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_PURCHASE_OFFICER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view',
                                              'pur.supplier.view',
                                              'pur.po.view', 'pur.po.create', 'pur.po.edit',
                                              'pur.grn.view', 'pur.grn.create', 'pur.grn.edit',
                                              'pur.invoice.view', 'pur.debit_note.view', 'pur.payment.view',
                                              'inv.item.view', 'inv.stock.view',
                                              'setup.terms.view', 'setup.hs_code.view',
                                              'apr.request.view', 'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_PURCHASE_OFFICER'
ON CONFLICT DO NOTHING;

-- ROLE_SALES_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'sal.dashboard.view', 'sal.dashboard.summary',
                                              'sal.customer.view', 'sal.customer.create', 'sal.customer.edit',
                                              'sal.customer.delete',
                                              'sal.so.view', 'sal.so.create', 'sal.so.edit', 'sal.so.delete',
                                              'sal.so.approve',
                                              'sal.delivery.view', 'sal.delivery.create', 'sal.delivery.edit',
                                              'sal.delivery.delete',
                                              'sal.invoice.view', 'sal.invoice.create', 'sal.invoice.edit',
                                              'sal.invoice.delete',
                                              'sal.credit_note.view', 'sal.credit_note.create',
                                              'sal.receipt.view', 'sal.receipt.create',
                                              'inv.item.view', 'inv.stock.view',
                                              'setup.bank.view', 'setup.currency.view', 'setup.terms.view',
                                              'setup.hs_code.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_SALES_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_SALES_EXECUTIVE
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view',
                                              'sal.customer.view',
                                              'sal.so.view', 'sal.so.create', 'sal.so.edit',
                                              'sal.delivery.view', 'sal.invoice.view', 'sal.receipt.view',
                                              'sal.credit_note.view',
                                              'inv.item.view', 'inv.stock.view',
                                              'setup.terms.view',
                                              'apr.request.view', 'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_SALES_EXECUTIVE'
ON CONFLICT DO NOTHING;

-- ROLE_HRM
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'hrm.dashboard.view', 'hrm.dashboard.summary',
                                              'hrm.designation.view', 'hrm.designation.create', 'hrm.designation.edit',
                                              'hrm.designation.delete',
                                              'hrm.employee.view', 'hrm.employee.create', 'hrm.employee.edit',
                                              'hrm.employee.delete',
                                              'hrm.attendance.view', 'hrm.attendance.create',
                                              'hrm.leave.view', 'hrm.leave.create', 'hrm.leave.approve',
                                              'hrm.payroll.view', 'hrm.payroll.create', 'hrm.payroll.approve',
                                              'org.department.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_HRM'
ON CONFLICT DO NOTHING;

-- ROLE_PRODUCTION_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'prd.dashboard.view', 'prd.dashboard.summary',
                                              'prd.bom.view', 'prd.bom.create',
                                              'prd.order.view', 'prd.order.create', 'prd.order.edit',
                                              'prd.order.delete', 'prd.order.approve',
                                              'prd.material_req.view', 'prd.material_req.create',
                                              'inv.item.view', 'inv.stock.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_PRODUCTION_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_PRODUCTION_SUPERVISOR
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view',
                                              'prd.bom.view',
                                              'prd.order.view',
                                              'prd.material_req.view', 'prd.material_req.create',
                                              'inv.item.view', 'inv.stock.view',
                                              'apr.request.view', 'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_PRODUCTION_SUPERVISOR'
ON CONFLICT DO NOTHING;

-- ROLE_COMMERCIAL_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'com.dashboard.view', 'com.dashboard.summary',
                                              'com.lc.view', 'com.lc.create', 'com.lc.edit', 'com.lc.delete',
                                              'com.settlement.view', 'com.settlement.create',
                                              'com.export.view', 'com.export.create',
                                              'com.import.view', 'com.import.create',
                                              'setup.bank.view', 'setup.currency.view', 'setup.terms.view',
                                              'setup.hs_code.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_COMMERCIAL_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_COMMERCIAL_EXECUTIVE
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view',
                                              'com.lc.view', 'com.settlement.view',
                                              'com.export.view', 'com.export.create',
                                              'com.import.view', 'com.import.create',
                                              'setup.bank.view', 'setup.currency.view', 'setup.terms.view',
                                              'apr.request.view', 'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_COMMERCIAL_EXECUTIVE'
ON CONFLICT DO NOTHING;

-- ROLE_CRM_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'crm.dashboard.view', 'crm.dashboard.summary',
                                              'crm.lead.view', 'crm.lead.create', 'crm.lead.edit', 'crm.lead.delete',
                                              'crm.opportunity.view', 'crm.opportunity.create', 'crm.opportunity.edit',
                                              'crm.opportunity.delete',
                                              'crm.contact.view', 'crm.contact.create', 'crm.contact.edit',
                                              'crm.contact.delete',
                                              'crm.activity.view', 'crm.activity.create', 'crm.activity.edit',
                                              'crm.activity.delete',
                                              'crm.feedback.view', 'crm.feedback.create', 'crm.feedback.edit',
                                              'crm.feedback.delete',
                                              'sal.customer.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_CRM_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_CRM_EXECUTIVE
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view',
                                              'crm.lead.view', 'crm.lead.create', 'crm.lead.edit',
                                              'crm.opportunity.view', 'crm.opportunity.create', 'crm.opportunity.edit',
                                              'crm.contact.view', 'crm.contact.create',
                                              'crm.activity.view', 'crm.activity.create', 'crm.activity.edit',
                                              'crm.feedback.view',
                                              'sal.customer.view',
                                              'apr.request.view', 'apr.dashboard.view'
    )
WHERE r.name = 'ROLE_CRM_EXECUTIVE'
ON CONFLICT DO NOTHING;

-- ROLE_BUDGET_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'budget.dashboard.view', 'budget.dashboard.summary',
                                              'budget.fiscalyear.view', 'budget.fiscalyear.create',
                                              'budget.fiscalyear.edit', 'budget.fiscalyear.delete',
                                              'budget.head.view', 'budget.head.create', 'budget.head.edit',
                                              'budget.head.delete',
                                              'budget.budget.view', 'budget.budget.create', 'budget.budget.edit',
                                              'budget.budget.delete',
                                              'budget.revision.view', 'budget.revision.create',
                                              'budget.transfer.view', 'budget.transfer.create',
                                              'acc.coa.view', 'acc.coa.search', 'acc.ledger.view', 'acc.trial_bal.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_BUDGET_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_ASSET_MANAGER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'dashboard.view', 'dashboard.summary',
                                              'fa.dashboard.view', 'fa.dashboard.summary',
                                              'fa.category.view', 'fa.category.create', 'fa.category.edit',
                                              'fa.category.delete',
                                              'fa.asset.view', 'fa.asset.create', 'fa.asset.edit', 'fa.asset.delete',
                                              'fa.depreciation.view', 'fa.depreciation.calculate',
                                              'fa.depreciation.post', 'fa.depreciation.reverse',
                                              'fa.disposal.view', 'fa.disposal.create',
                                              'acc.coa.view', 'acc.coa.search', 'acc.ledger.view',
                                              'apr.request.view', 'apr.request.approve', 'apr.request.reject',
                                              'apr.dashboard.view', 'apr.dashboard.summary'
    )
WHERE r.name = 'ROLE_ASSET_MANAGER'
ON CONFLICT DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 5. SEC_MROLE_MENUS  (Role ↔ Menu visibility + CRUD flags)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_SUPER_ADMIN → all menus, full CRUD
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       true,
       true,
       true,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND m.active = true
  AND m.deleted = false
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_ACCOUNTS_ADMIN
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN
       ('ACC_COA', 'ACC_SUBACC', 'ACC_BANK_ACC', 'ACC_JOURNAL', 'ACC_PAYMENT', 'ACC_RECEIPT', 'ACC_CONTRA',
        'ACC_MAPPING', 'ACC_PERIODS', 'ACC_OB', 'ACC_POLICY'),
       m.menu_code IN
       ('ACC_COA', 'ACC_SUBACC', 'ACC_BANK_ACC', 'ACC_JOURNAL', 'ACC_PAYMENT', 'ACC_RECEIPT', 'ACC_CONTRA',
        'ACC_MAPPING', 'ACC_PERIODS', 'ACC_OB', 'ACC_POLICY'),
       m.menu_code IN ('ACC_COA', 'ACC_JOURNAL', 'ACC_MAPPING', 'ACC_OB', 'ACC_POLICY'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_ACCOUNTS_ADMIN'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_ACCOUNTS', 'MOD_SETUP', 'MOD_APPROVALS',
                      'GRP_ACC_MASTER', 'GRP_ACC_VOUCHERS', 'GRP_ACC_REPORTS', 'GRP_ACC_CONFIG',
                      'GRP_STP_REF', 'GRP_APR_PENDING',
                      'ACC_DASHBOARD', 'ACC_COA', 'ACC_SUBACC', 'ACC_BANK_ACC',
                      'ACC_JOURNAL', 'ACC_PAYMENT', 'ACC_RECEIPT', 'ACC_CONTRA',
                      'ACC_LEDGER', 'ACC_TRIAL_BAL', 'ACC_PNL', 'ACC_BAL_SHEET', 'ACC_AGING',
                      'ACC_MAPPING', 'ACC_PERIODS', 'ACC_OB', 'ACC_POLICY',
                      'STP_BANK', 'STP_CURRENCY', 'STP_TERMS',
                      'APR_PENDING', 'PUR_INVOICE', 'PUR_PAYMENT', 'SAL_INVOICE', 'SAL_RECEIPT')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_ACCOUNTANT
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('ACC_JOURNAL', 'ACC_PAYMENT', 'ACC_RECEIPT', 'ACC_CONTRA'),
       m.menu_code IN ('ACC_JOURNAL', 'ACC_PAYMENT', 'ACC_RECEIPT', 'ACC_CONTRA'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_ACCOUNTANT'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_ACCOUNTS', 'MOD_SETUP',
                      'GRP_ACC_MASTER', 'GRP_ACC_VOUCHERS', 'GRP_ACC_REPORTS', 'GRP_ACC_CONFIG', 'GRP_STP_REF',
                      'ACC_DASHBOARD', 'ACC_COA', 'ACC_SUBACC', 'ACC_BANK_ACC',
                      'ACC_JOURNAL', 'ACC_PAYMENT', 'ACC_RECEIPT', 'ACC_CONTRA',
                      'ACC_LEDGER', 'ACC_TRIAL_BAL', 'ACC_PNL', 'ACC_BAL_SHEET', 'ACC_AGING',
                      'ACC_MAPPING', 'ACC_PERIODS',
                      'STP_BANK', 'STP_CURRENCY', 'STP_TERMS',
                      'PUR_INVOICE', 'PUR_PAYMENT', 'SAL_INVOICE', 'SAL_RECEIPT')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_INVENTORY_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN
       ('INV_UOM', 'INV_CATEGORY', 'INV_BRAND', 'INV_MODEL', 'INV_ITEM', 'INV_ADJUSTMENT', 'INV_TRANSFER'),
       m.menu_code IN
       ('INV_UOM', 'INV_CATEGORY', 'INV_BRAND', 'INV_MODEL', 'INV_ITEM', 'INV_ADJUSTMENT', 'INV_TRANSFER'),
       m.menu_code IN ('INV_UOM', 'INV_CATEGORY', 'INV_BRAND', 'INV_MODEL', 'INV_ITEM'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_INVENTORY_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_INVENTORY', 'MOD_SETUP',
                      'GRP_INV_MASTER', 'GRP_INV_STOCK', 'GRP_STP_REF',
                      'INV_DASHBOARD', 'INV_UOM', 'INV_CATEGORY', 'INV_BRAND', 'INV_MODEL', 'INV_ITEM',
                      'INV_STOCK', 'INV_ADJUSTMENT', 'INV_TRANSFER',
                      'STP_HS_CODE', 'STP_WAREHOUSE')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_WAREHOUSE_STAFF
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('INV_TRANSFER', 'PUR_GRN'),
       m.menu_code IN ('INV_TRANSFER', 'PUR_GRN'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_WAREHOUSE_STAFF'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_INVENTORY', 'MOD_PURCHASE',
                      'GRP_INV_MASTER', 'GRP_INV_STOCK', 'GRP_PUR_CYCLE',
                      'INV_ITEM', 'INV_STOCK', 'INV_TRANSFER', 'PUR_GRN')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_PURCHASE_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('PUR_SUPPLIER', 'PUR_PO', 'PUR_GRN', 'PUR_INVOICE', 'PUR_DEBIT_NOTE', 'PUR_PAYMENT'),
       m.menu_code IN ('PUR_SUPPLIER', 'PUR_PO', 'PUR_GRN', 'PUR_INVOICE', 'PUR_DEBIT_NOTE', 'PUR_PAYMENT'),
       m.menu_code IN ('PUR_SUPPLIER', 'PUR_PO', 'PUR_GRN', 'PUR_INVOICE', 'PUR_DEBIT_NOTE'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_PURCHASE_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_PURCHASE', 'MOD_INVENTORY', 'MOD_SETUP',
                      'GRP_PUR_MASTER', 'GRP_PUR_CYCLE', 'GRP_PUR_RETURNS',
                      'GRP_INV_MASTER', 'GRP_INV_STOCK', 'GRP_STP_REF',
                      'PUR_DASHBOARD', 'PUR_SUPPLIER', 'PUR_PO', 'PUR_GRN', 'PUR_INVOICE', 'PUR_DEBIT_NOTE',
                      'PUR_PAYMENT',
                      'INV_ITEM', 'INV_STOCK', 'STP_BANK', 'STP_CURRENCY', 'STP_TERMS', 'STP_HS_CODE')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_PURCHASE_OFFICER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('PUR_PO', 'PUR_GRN'),
       m.menu_code IN ('PUR_PO', 'PUR_GRN'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_PURCHASE_OFFICER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_PURCHASE', 'MOD_INVENTORY',
                      'GRP_PUR_MASTER', 'GRP_PUR_CYCLE', 'GRP_PUR_RETURNS',
                      'GRP_INV_MASTER', 'GRP_INV_STOCK',
                      'PUR_SUPPLIER', 'PUR_PO', 'PUR_GRN', 'PUR_INVOICE', 'PUR_DEBIT_NOTE', 'PUR_PAYMENT',
                      'INV_ITEM', 'INV_STOCK', 'STP_TERMS', 'STP_HS_CODE')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_SALES_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('SAL_CUSTOMER', 'SAL_SO', 'SAL_DELIVERY', 'SAL_INVOICE', 'SAL_CREDIT_NOTE', 'SAL_RECEIPT'),
       m.menu_code IN ('SAL_CUSTOMER', 'SAL_SO', 'SAL_DELIVERY', 'SAL_INVOICE', 'SAL_CREDIT_NOTE', 'SAL_RECEIPT'),
       m.menu_code IN ('SAL_CUSTOMER', 'SAL_SO', 'SAL_DELIVERY', 'SAL_INVOICE', 'SAL_CREDIT_NOTE'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SALES_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_SALES', 'MOD_INVENTORY', 'MOD_SETUP',
                      'GRP_SAL_MASTER', 'GRP_SAL_CYCLE', 'GRP_SAL_RETURNS',
                      'GRP_INV_MASTER', 'GRP_INV_STOCK', 'GRP_STP_REF',
                      'SAL_DASHBOARD', 'SAL_CUSTOMER', 'SAL_SO', 'SAL_DELIVERY', 'SAL_INVOICE', 'SAL_CREDIT_NOTE',
                      'SAL_RECEIPT',
                      'INV_ITEM', 'INV_STOCK', 'STP_BANK', 'STP_CURRENCY', 'STP_TERMS', 'STP_HS_CODE')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_SALES_EXECUTIVE
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('SAL_SO'),
       m.menu_code IN ('SAL_SO'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SALES_EXECUTIVE'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_SALES', 'MOD_INVENTORY',
                      'GRP_SAL_MASTER', 'GRP_SAL_CYCLE', 'GRP_SAL_RETURNS',
                      'GRP_INV_MASTER', 'GRP_INV_STOCK',
                      'SAL_CUSTOMER', 'SAL_SO', 'SAL_DELIVERY', 'SAL_INVOICE', 'SAL_RECEIPT', 'SAL_CREDIT_NOTE',
                      'INV_ITEM', 'INV_STOCK', 'STP_TERMS')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_HRM
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('HRM_DESIG', 'HRM_EMPLOYEE', 'HRM_ATTENDANCE', 'HRM_LEAVE', 'HRM_PAYROLL'),
       m.menu_code IN ('HRM_DESIG', 'HRM_EMPLOYEE', 'HRM_ATTENDANCE', 'HRM_LEAVE', 'HRM_PAYROLL'),
       m.menu_code IN ('HRM_DESIG', 'HRM_EMPLOYEE'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_HRM'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_HRM', 'MOD_SETUP',
                      'GRP_HRM_MASTER', 'GRP_HRM_ATTENDANCE', 'GRP_HRM_PAYROLL', 'GRP_STP_ORG',
                      'HRM_DASHBOARD', 'HRM_DESIG', 'HRM_EMPLOYEE', 'HRM_ATTENDANCE', 'HRM_LEAVE', 'HRM_PAYROLL',
                      'STP_DEPT')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_PRODUCTION_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('PRD_BOM', 'PRD_ORDER', 'PRD_MAT_REQ'),
       m.menu_code IN ('PRD_BOM', 'PRD_ORDER', 'PRD_MAT_REQ'),
       m.menu_code IN ('PRD_BOM', 'PRD_ORDER', 'PRD_MAT_REQ'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_PRODUCTION_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_PRODUCTION', 'MOD_INVENTORY',
                      'GRP_PRD_MASTER', 'GRP_PRD_ORDERS', 'GRP_INV_MASTER', 'GRP_INV_STOCK',
                      'PRD_DASHBOARD', 'PRD_BOM', 'PRD_ORDER', 'PRD_MAT_REQ', 'INV_ITEM', 'INV_STOCK')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_PRODUCTION_SUPERVISOR
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('PRD_MAT_REQ'),
       m.menu_code IN ('PRD_MAT_REQ'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_PRODUCTION_SUPERVISOR'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_PRODUCTION', 'MOD_INVENTORY',
                      'GRP_PRD_MASTER', 'GRP_PRD_ORDERS', 'GRP_INV_MASTER', 'GRP_INV_STOCK',
                      'PRD_BOM', 'PRD_ORDER', 'PRD_MAT_REQ', 'INV_ITEM', 'INV_STOCK')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_COMMERCIAL_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('COM_LC', 'COM_SETTLEMENT', 'COM_EXPORT', 'COM_IMPORT'),
       m.menu_code IN ('COM_LC', 'COM_SETTLEMENT', 'COM_EXPORT', 'COM_IMPORT'),
       m.menu_code IN ('COM_LC'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_COMMERCIAL_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_COMMERCIAL', 'MOD_SETUP',
                      'GRP_COM_LC', 'GRP_COM_TRADE', 'GRP_STP_REF',
                      'COM_DASHBOARD', 'COM_LC', 'COM_SETTLEMENT', 'COM_EXPORT', 'COM_IMPORT',
                      'STP_BANK', 'STP_CURRENCY', 'STP_TERMS', 'STP_HS_CODE')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_COMMERCIAL_EXECUTIVE
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('COM_EXPORT', 'COM_IMPORT'),
       m.menu_code IN ('COM_EXPORT', 'COM_IMPORT'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_COMMERCIAL_EXECUTIVE'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_COMMERCIAL', 'MOD_SETUP',
                      'GRP_COM_LC', 'GRP_COM_TRADE', 'GRP_STP_REF',
                      'COM_LC', 'COM_SETTLEMENT', 'COM_EXPORT', 'COM_IMPORT',
                      'STP_BANK', 'STP_CURRENCY', 'STP_TERMS')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_CRM_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('CRM_LEADS', 'CRM_OPP', 'CRM_CONTACTS', 'CRM_ACTIVITIES', 'CRM_FEEDBACK'),
       m.menu_code IN ('CRM_LEADS', 'CRM_OPP', 'CRM_CONTACTS', 'CRM_ACTIVITIES', 'CRM_FEEDBACK'),
       m.menu_code IN ('CRM_LEADS', 'CRM_OPP', 'CRM_CONTACTS', 'CRM_ACTIVITIES', 'CRM_FEEDBACK'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_CRM_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_CRM',
                      'GRP_CRM_PIPELINE', 'GRP_CRM_ENGAGE',
                      'CRM_DASHBOARD', 'CRM_LEADS', 'CRM_OPP', 'CRM_CONTACTS', 'CRM_ACTIVITIES', 'CRM_FEEDBACK')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_CRM_EXECUTIVE
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('CRM_LEADS', 'CRM_OPP', 'CRM_CONTACTS', 'CRM_ACTIVITIES'),
       m.menu_code IN ('CRM_LEADS', 'CRM_OPP', 'CRM_CONTACTS', 'CRM_ACTIVITIES'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_CRM_EXECUTIVE'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_CRM',
                      'GRP_CRM_PIPELINE', 'GRP_CRM_ENGAGE',
                      'CRM_LEADS', 'CRM_OPP', 'CRM_CONTACTS', 'CRM_ACTIVITIES', 'CRM_FEEDBACK')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_BUDGET_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('BGT_FISCAL_YEAR', 'BGT_HEADS', 'BGT_BUDGETS', 'BGT_REVISIONS', 'BGT_TRANSFERS'),
       m.menu_code IN ('BGT_FISCAL_YEAR', 'BGT_HEADS', 'BGT_BUDGETS', 'BGT_REVISIONS', 'BGT_TRANSFERS'),
       m.menu_code IN ('BGT_FISCAL_YEAR', 'BGT_HEADS', 'BGT_BUDGETS'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_BUDGET_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_BUDGET', 'MOD_ACCOUNTS',
                      'GRP_BGT_MASTER', 'GRP_BGT_TRANSACTIONS',
                      'BGT_DASHBOARD', 'BGT_FISCAL_YEAR', 'BGT_HEADS', 'BGT_BUDGETS', 'BGT_REVISIONS', 'BGT_TRANSFERS',
                      'GRP_ACC_MASTER', 'GRP_ACC_REPORTS',
                      'ACC_COA', 'ACC_LEDGER', 'ACC_TRIAL_BAL')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_ASSET_MANAGER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('FA_CATEGORIES', 'FA_ASSETS', 'FA_DEPRECIATION', 'FA_DISPOSALS'),
       m.menu_code IN ('FA_CATEGORIES', 'FA_ASSETS', 'FA_DEPRECIATION', 'FA_DISPOSALS'),
       m.menu_code IN ('FA_CATEGORIES', 'FA_ASSETS'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_ASSET_MANAGER'
  AND m.menu_code IN ('MOD_DASHBOARD', 'MOD_FIXED_ASSETS', 'MOD_ACCOUNTS',
                      'GRP_FA_MASTER', 'GRP_FA_OPERATIONS',
                      'FA_DASHBOARD', 'FA_CATEGORIES', 'FA_ASSETS', 'FA_DEPRECIATION', 'FA_DISPOSALS',
                      'GRP_ACC_MASTER', 'GRP_ACC_REPORTS',
                      'ACC_COA', 'ACC_LEDGER')
ON CONFLICT (role_id, menu_id) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- VERIFICATION (uncomment to run counts after execution)
-- ═════════════════════════════════════════════════════════════════════════════
-- SELECT 'Permissions'       AS table_name, COUNT(*) AS total FROM sec_permissions
-- UNION ALL SELECT 'Roles',               COUNT(*) FROM sec_roles
-- UNION ALL SELECT 'Role-Permissions',    COUNT(*) FROM sec_role_permissions
-- UNION ALL SELECT 'Menus',               COUNT(*) FROM app_menus
-- UNION ALL SELECT 'Role-Menus',          COUNT(*) FROM sec_mrole_menus;

COMMIT;