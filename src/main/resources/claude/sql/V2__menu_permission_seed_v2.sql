-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║  OPTIMUM ERP — MENU & PERMISSION SEED  v2.0                                 ║
-- ║  Generic ERP Edition (no yarn/spinning)                                     ║
-- ║                                                                              ║
-- ║  Hierarchy:  MODULE → GROUP → LEAF                                           ║
-- ║  Icons:      Font Awesome 6 (fas / far)                                      ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- ─────────────────────────────────────────────────────────────────────────────
-- ROLES
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO sec_roles (name, name_bn, description, master_role, active) VALUES
  ('ROLE_SUPER_ADMIN',         'সুপার অ্যাডমিন',        'Full system access',                    'ROLE_SUPER_ADMIN',         TRUE),
  ('ROLE_ADMIN',               'অ্যাডমিন',               'Organization admin',                    'ROLE_ADMIN',               TRUE),
  ('ROLE_ACCOUNTS_ADMIN',      'অ্যাকাউন্টস অ্যাডমিন', 'Finance & accounts manager',            'ROLE_ACCOUNTS_ADMIN',      TRUE),
  ('ROLE_PURCHASE_MANAGER',    'ক্রয় ব্যবস্থাপক',       'Purchase department head',              'ROLE_PURCHASE_MANAGER',    TRUE),
  ('ROLE_PURCHASE_OFFICER',    'ক্রয় কর্মকর্তা',        'Purchase officer',                      'ROLE_PURCHASE_OFFICER',    TRUE),
  ('ROLE_SALES_MANAGER',       'বিক্রয় ব্যবস্থাপক',     'Sales department head',                 'ROLE_SALES_MANAGER',       TRUE),
  ('ROLE_SALES_EXECUTIVE',     'বিক্রয় নির্বাহী',        'Sales executive',                       'ROLE_SALES_EXECUTIVE',     TRUE),
  ('ROLE_INVENTORY_MANAGER',   'ইনভেন্টরি ব্যবস্থাপক',  'Inventory & warehouse manager',         'ROLE_INVENTORY_MANAGER',   TRUE),
  ('ROLE_WAREHOUSE_STAFF',     'গুদাম কর্মী',             'Warehouse staff',                       'ROLE_WAREHOUSE_STAFF',     TRUE),
  ('ROLE_PRODUCTION_MANAGER',  'উৎপাদন ব্যবস্থাপক',    'Production manager',                    'ROLE_PRODUCTION_MANAGER',  TRUE),
  ('ROLE_PRODUCTION_SUPERVISOR','উৎপাদন সুপারভাইজার',    'Production supervisor',                 'ROLE_PRODUCTION_SUPERVISOR',TRUE),
  ('ROLE_HRM',                 'এইচআরএম',                'HRM manager',                           'ROLE_HRM',                 TRUE),
  ('ROLE_ASSET_MANAGER',       'সম্পদ ব্যবস্থাপক',       'Fixed assets manager',                  'ROLE_ASSET_MANAGER',       TRUE),
  ('ROLE_BUDGET_MANAGER',      'বাজেট ব্যবস্থাপক',       'Budget & planning manager',             'ROLE_BUDGET_MANAGER',      TRUE),
  ('ROLE_CRM_MANAGER',         'সিআরএম ব্যবস্থাপক',     'CRM & sales pipeline manager',          'ROLE_CRM_MANAGER',         TRUE),
  ('ROLE_COMMERCIAL_MANAGER',  'বাণিজ্যিক ব্যবস্থাপক',  'Commercial & LC manager',               'ROLE_COMMERCIAL_MANAGER',  TRUE),
  ('ROLE_VIEWER',              'দর্শক',                  'Read-only access',                      'ROLE_VIEWER',              TRUE)
ON CONFLICT (name) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- MENUS — Top-level MODULE entries
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible) VALUES

-- 01 Dashboard
('M_DASHBOARD',       'Dashboard',         '/dashboard',     'fas fa-gauge-high',    NULL, 10,'MODULE','DASHBOARD',    'dashboard.view',   TRUE, TRUE),
('M_PURCHASE',        'Purchase',          '#',              'fas fa-cart-shopping', NULL, 20,'MODULE','PURCHASE',     NULL,               TRUE, TRUE),

-- 03 Sales
('M_SALES',           'Sales',             '#',              'fas fa-tags',          NULL, 30,'MODULE','SALES',        NULL,               TRUE, TRUE),

-- 04 Inventory
('M_INVENTORY',       'Inventory',         '#',              'fas fa-boxes-stacked', NULL, 40,'MODULE','INVENTORY',    NULL,               TRUE, TRUE),

-- 05 Production
('M_PRODUCTION',      'Production',        '#',              'fas fa-industry',      NULL, 50,'MODULE','PRODUCTION',   NULL,               TRUE, TRUE),

-- 06 Accounts
('M_ACCOUNTS',        'Accounts',          '#',              'fas fa-calculator',    NULL, 60,'MODULE','ACCOUNTS',     NULL,               TRUE, TRUE),

-- 07 HRM
('M_HRM',             'Human Resources',   '#',              'fas fa-users',         NULL, 70,'MODULE','HRM',          NULL,               TRUE, TRUE),

-- 08 Fixed Assets
('M_ASSETS',          'Fixed Assets',      '#',              'fas fa-building',      NULL, 80,'MODULE','FIXED_ASSETS', NULL,               TRUE, TRUE),

-- 09 Budget
('M_BUDGET',          'Budget',            '#',              'fas fa-chart-pie',     NULL, 90,'MODULE','BUDGET',       NULL,               TRUE, TRUE),

-- 10 CRM
('M_CRM',             'CRM',               '#',              'fas fa-handshake',     NULL,100,'MODULE','CRM',          NULL,               TRUE, TRUE),

-- 11 Commercial
('M_COMMERCIAL',      'Commercial',        '#',              'fas fa-ship',          NULL,110,'MODULE','COMMERCIAL',   NULL,               TRUE, TRUE),

-- 12 Reports
('M_REPORTS',         'Reports',           '#',              'fas fa-chart-bar',     NULL,120,'MODULE','REPORTS',      NULL,               TRUE, TRUE),

-- 13 Settings
('M_SETTINGS',        'Settings',          '#',              'fas fa-gear',          NULL,130,'MODULE','SETTINGS',     NULL,               TRUE, TRUE)

ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- PURCHASE sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_PURCHASE_MASTERS',     'Masters',                '#',                                         'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_PURCHASE'), 10,'GROUP','PURCHASE',NULL,TRUE,TRUE),
  ('M_SUPPLIER',             'Suppliers',              '/purchase/suppliers',                        'fas fa-truck',          (SELECT id FROM app_menus WHERE menu_code='M_PURCHASE_MASTERS'), 10,'LEAF','PURCHASE','purchase.supplier.view',TRUE,TRUE),
  ('M_PURCHASE_TRANSACTIONS','Transactions',           '#',                                         'fas fa-receipt',        (SELECT id FROM app_menus WHERE menu_code='M_PURCHASE'), 20,'GROUP','PURCHASE',NULL,TRUE,TRUE),
  ('M_PRF',                  'Purchase Requisition',  '/purchase/requisitions',                     'fas fa-file-pen',       (SELECT id FROM app_menus WHERE menu_code='M_PURCHASE_TRANSACTIONS'), 10,'LEAF','PURCHASE','purchase.requisition.view',TRUE,TRUE),
  ('M_RFQ',                  'Request for Quotation', '/purchase/rfq',                              'fas fa-file-circle-question',(SELECT id FROM app_menus WHERE menu_code='M_PURCHASE_TRANSACTIONS'), 20,'LEAF','PURCHASE','purchase.rfq.view',TRUE,TRUE),
  ('M_PO',                   'Purchase Orders',       '/purchase/orders',                           'fas fa-file-invoice',   (SELECT id FROM app_menus WHERE menu_code='M_PURCHASE_TRANSACTIONS'), 30,'LEAF','PURCHASE','purchase.order.view',TRUE,TRUE),
  ('M_GRN',                  'Goods Receipt (GRN)',   '/purchase/grn',                              'fas fa-truck-ramp-box', (SELECT id FROM app_menus WHERE menu_code='M_PURCHASE_TRANSACTIONS'), 40,'LEAF','PURCHASE','purchase.grn.view',TRUE,TRUE),
  ('M_PURCHASE_INVOICE',     'Purchase Invoice',      '/purchase/invoices',                         'fas fa-file-invoice-dollar',(SELECT id FROM app_menus WHERE menu_code='M_PURCHASE_TRANSACTIONS'), 50,'LEAF','PURCHASE','purchase.invoice.view',TRUE,TRUE),
  ('M_PAYMENT_VOUCHER',      'Payment Voucher',       '/accounts/payment-vouchers',                 'fas fa-money-bill-transfer',(SELECT id FROM app_menus WHERE menu_code='M_PURCHASE_TRANSACTIONS'), 60,'LEAF','PURCHASE','accounts.payment.view',TRUE,TRUE),
  ('M_PURCHASE_RETURN',      'Purchase Return (DN)',  '/purchase/returns',                          'fas fa-rotate-left',    (SELECT id FROM app_menus WHERE menu_code='M_PURCHASE_TRANSACTIONS'), 70,'LEAF','PURCHASE','purchase.return.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- SALES sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_SALES_MASTERS',     'Masters',               '#',                                'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_SALES'), 10,'GROUP','SALES',NULL,TRUE,TRUE),
  ('M_CUSTOMER',          'Customers',             '/sales/customers',                  'fas fa-user-tie',       (SELECT id FROM app_menus WHERE menu_code='M_SALES_MASTERS'), 10,'LEAF','SALES','sales.customer.view',TRUE,TRUE),
  ('M_SALES_TRANSACTIONS','Transactions',          '#',                                'fas fa-receipt',        (SELECT id FROM app_menus WHERE menu_code='M_SALES'), 20,'GROUP','SALES',NULL,TRUE,TRUE),
  ('M_QUOTATION',         'Sales Quotation',       '/sales/quotations',                 'fas fa-file-pen',       (SELECT id FROM app_menus WHERE menu_code='M_SALES_TRANSACTIONS'), 10,'LEAF','SALES','sales.quotation.view',TRUE,TRUE),
  ('M_SALES_ORDER',       'Sales Orders',          '/sales/orders',                     'fas fa-file-invoice',   (SELECT id FROM app_menus WHERE menu_code='M_SALES_TRANSACTIONS'), 20,'LEAF','SALES','sales.order.view',TRUE,TRUE),
  ('M_DELIVERY',          'Delivery Challan',      '/sales/delivery',                   'fas fa-truck',          (SELECT id FROM app_menus WHERE menu_code='M_SALES_TRANSACTIONS'), 30,'LEAF','SALES','sales.delivery.view',TRUE,TRUE),
  ('M_SALES_INVOICE',     'Sales Invoice',         '/sales/invoices',                   'fas fa-file-invoice-dollar',(SELECT id FROM app_menus WHERE menu_code='M_SALES_TRANSACTIONS'), 40,'LEAF','SALES','sales.invoice.view',TRUE,TRUE),
  ('M_RECEIPT_VOUCHER',   'Receipt Voucher',       '/accounts/receipt-vouchers',        'fas fa-money-bill-wave',(SELECT id FROM app_menus WHERE menu_code='M_SALES_TRANSACTIONS'), 50,'LEAF','SALES','accounts.receipt.view',TRUE,TRUE),
  ('M_SALES_RETURN',      'Sales Return (CN)',     '/sales/returns',                    'fas fa-rotate-left',   (SELECT id FROM app_menus WHERE menu_code='M_SALES_TRANSACTIONS'), 60,'LEAF','SALES','sales.return.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- INVENTORY sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_INV_MASTERS',       'Item Masters',          '#',                                'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_INVENTORY'), 10,'GROUP','INVENTORY',NULL,TRUE,TRUE),
  ('M_INV_ITEMS',         'Items',                 '/inventory/items',                  'fas fa-box',            (SELECT id FROM app_menus WHERE menu_code='M_INV_MASTERS'), 10,'LEAF','INVENTORY','inventory.item.view',TRUE,TRUE),
  ('M_INV_CATEGORIES',    'Categories',            '/inventory/categories',             'fas fa-sitemap',        (SELECT id FROM app_menus WHERE menu_code='M_INV_MASTERS'), 20,'LEAF','INVENTORY','inventory.category.view',TRUE,TRUE),
  ('M_INV_UOM',           'Units of Measure',      '/inventory/uom',                    'fas fa-scale-balanced', (SELECT id FROM app_menus WHERE menu_code='M_INV_MASTERS'), 30,'LEAF','INVENTORY','inventory.uom.view',TRUE,TRUE),
  ('M_INV_BRANDS',        'Brands & Models',       '/inventory/brands',                 'fas fa-certificate',    (SELECT id FROM app_menus WHERE menu_code='M_INV_MASTERS'), 40,'LEAF','INVENTORY','inventory.brand.view',TRUE,TRUE),
  ('M_INV_WAREHOUSES',    'Warehouses',            '/inventory/warehouses',             'fas fa-warehouse',      (SELECT id FROM app_menus WHERE menu_code='M_INV_MASTERS'), 50,'LEAF','INVENTORY','inventory.warehouse.view',TRUE,TRUE),
  ('M_INV_OPERATIONS',    'Operations',            '#',                                'fas fa-arrows-rotate',  (SELECT id FROM app_menus WHERE menu_code='M_INVENTORY'), 20,'GROUP','INVENTORY',NULL,TRUE,TRUE),
  ('M_INV_LOTS',          'Lot Management',        '/inventory/lots',                   'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_INV_OPERATIONS'), 10,'LEAF','INVENTORY','inventory.lot.view',TRUE,TRUE),
  ('M_INV_STOCK',         'Stock Position',        '/inventory/stock',                  'fas fa-chart-column',   (SELECT id FROM app_menus WHERE menu_code='M_INV_OPERATIONS'), 20,'LEAF','INVENTORY','inventory.stock.view',TRUE,TRUE),
  ('M_INV_TRANSFER',      'Stock Transfer',        '/inventory/transfers',              'fas fa-right-left',     (SELECT id FROM app_menus WHERE menu_code='M_INV_OPERATIONS'), 30,'LEAF','INVENTORY','inventory.transfer.view',TRUE,TRUE),
  ('M_INV_ADJUSTMENT',    'Stock Adjustment',      '/inventory/adjustments',            'fas fa-sliders',        (SELECT id FROM app_menus WHERE menu_code='M_INV_OPERATIONS'), 40,'LEAF','INVENTORY','inventory.adjustment.view',TRUE,TRUE),
  ('M_STORE_REQ',         'Store Requisition',     '/inventory/store-requisitions',     'fas fa-clipboard-list', (SELECT id FROM app_menus WHERE menu_code='M_INV_OPERATIONS'), 50,'LEAF','INVENTORY','inventory.storereq.view',TRUE,TRUE),
  ('M_MATERIAL_ISSUE',    'Material Issue',        '/inventory/material-issue',         'fas fa-arrow-up-from-bracket',(SELECT id FROM app_menus WHERE menu_code='M_INV_OPERATIONS'), 60,'LEAF','INVENTORY','inventory.materialissue.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- PRODUCTION sub-menus  (Generic)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_PRD_MASTERS',         'Masters',               '#',                                'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_PRODUCTION'), 10,'GROUP','PRODUCTION',NULL,TRUE,TRUE),
  ('M_BOM',                 'Bill of Materials',     '/production/boms',                   'fas fa-list-check',     (SELECT id FROM app_menus WHERE menu_code='M_PRD_MASTERS'), 10,'LEAF','PRODUCTION','production.bom.view',TRUE,TRUE),
  ('M_PRD_TRANSACTIONS',    'Production',            '#',                                'fas fa-gears',          (SELECT id FROM app_menus WHERE menu_code='M_PRODUCTION'), 20,'GROUP','PRODUCTION',NULL,TRUE,TRUE),
  ('M_PRD_ORDER',           'Production Orders',     '/production/orders',                'fas fa-file-pen',       (SELECT id FROM app_menus WHERE menu_code='M_PRD_TRANSACTIONS'), 10,'LEAF','PRODUCTION','production.order.view',TRUE,TRUE),
  ('M_PRD_MATERIAL_REQ',    'Material Requisition',  '/production/material-requisitions', 'fas fa-clipboard-list', (SELECT id FROM app_menus WHERE menu_code='M_PRD_TRANSACTIONS'), 20,'LEAF','PRODUCTION','production.material.view',TRUE,TRUE),
  ('M_PRD_INPUT',           'Material Consumption',  '/production/inputs',                'fas fa-arrow-right-to-bracket',(SELECT id FROM app_menus WHERE menu_code='M_PRD_TRANSACTIONS'), 30,'LEAF','PRODUCTION','production.input.view',TRUE,TRUE),
  ('M_PRD_OUTPUT',          'Finished Goods Receive','/production/outputs',               'fas fa-boxes-stacked',  (SELECT id FROM app_menus WHERE menu_code='M_PRD_TRANSACTIONS'), 40,'LEAF','PRODUCTION','production.output.view',TRUE,TRUE),
  ('M_PRD_COST_SHEET',      'Cost Sheet',            '/production/cost-sheets',           'fas fa-file-invoice',   (SELECT id FROM app_menus WHERE menu_code='M_PRD_TRANSACTIONS'), 50,'LEAF','PRODUCTION','production.costsheet.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- ACCOUNTS sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_ACC_MASTERS',         'Chart of Accounts',     '#',                               'fas fa-layer-group',      (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 10,'GROUP','ACCOUNTS',NULL,TRUE,TRUE),
  ('M_COA',                 'Ledger Accounts',       '/accounts/chart',                  'fas fa-book',             (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 10,'LEAF','ACCOUNTS','accounts.coa.view',TRUE,TRUE),
  ('M_SUB_LEDGER',          'Sub Ledger',            '/accounts/sub-ledger',             'fas fa-book-open',        (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 20,'LEAF','ACCOUNTS','accounts.subledger.view',TRUE,TRUE),
  ('M_BANK_ACCOUNT',        'Bank Accounts',         '/accounts/bank-accounts',          'fas fa-building-columns', (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 30,'LEAF','ACCOUNTS','accounts.bank.view',TRUE,TRUE),
  ('M_CASH_ACCOUNT',        'Cash Accounts',         '/accounts/cash-accounts',          'fas fa-vault',            (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 40,'LEAF','ACCOUNTS','accounts.cash.view',TRUE,TRUE),
  ('M_ACC_TRANSACTIONS',    'Transactions',          '#',                               'fas fa-receipt',           (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 20,'GROUP','ACCOUNTS',NULL,TRUE,TRUE),
  ('M_JOURNAL_VOUCHER',     'Journal Voucher',       '/accounts/journals',               'fas fa-pen-to-square',    (SELECT id FROM app_menus WHERE menu_code='M_ACC_TRANSACTIONS'), 10,'LEAF','ACCOUNTS','accounts.journal.view',TRUE,TRUE),
  ('M_CONTRA_VOUCHER',      'Contra Voucher',        '/accounts/contra-vouchers',        'fas fa-right-left',       (SELECT id FROM app_menus WHERE menu_code='M_ACC_TRANSACTIONS'), 20,'LEAF','ACCOUNTS','accounts.contra.view',TRUE,TRUE),
  ('M_EXPENSE_VOUCHER',     'Expense Voucher',       '/accounts/expense-vouchers',       'fas fa-money-bill',       (SELECT id FROM app_menus WHERE menu_code='M_ACC_TRANSACTIONS'), 30,'LEAF','ACCOUNTS','accounts.expense.view',TRUE,TRUE),
  ('M_ACC_PERIOD',          'Accounting Periods',    '/accounts/periods',                'fas fa-calendar-days',    (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 30,'GROUP','ACCOUNTS','accounts.period.view',TRUE,TRUE),
  ('M_OPENING_BALANCE',     'Opening Balances',      '/accounts/opening-balances',       'fas fa-scale-unbalanced', (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 40,'GROUP','ACCOUNTS','accounts.opening.view',TRUE,TRUE),
  ('M_ACC_MAPPING',         'Account Mapping',       '/accounts/mapping',                'fas fa-diagram-project',  (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 50,'GROUP','ACCOUNTS','accounts.mapping.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- HRM sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_HRM_MASTERS',        'Masters',               '#',                               'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_HRM'), 10,'GROUP','HRM',NULL,TRUE,TRUE),
  ('M_DESIGNATIONS',       'Designations',          '/hrm/designations',                'fas fa-id-badge',       (SELECT id FROM app_menus WHERE menu_code='M_HRM_MASTERS'), 10,'LEAF','HRM','hrm.designation.view',TRUE,TRUE),
  ('M_DEPARTMENTS',        'Departments',           '/hrm/departments',                 'fas fa-sitemap',        (SELECT id FROM app_menus WHERE menu_code='M_HRM_MASTERS'), 20,'LEAF','HRM','hrm.department.view',TRUE,TRUE),
  ('M_EMPLOYEES',          'Employees',             '/hrm/employees',                   'fas fa-user',           (SELECT id FROM app_menus WHERE menu_code='M_HRM'), 20,'LEAF','HRM','hrm.employee.view',TRUE,TRUE),
  ('M_HRM_OPERATIONS',     'Operations',            '#',                               'fas fa-tasks',          (SELECT id FROM app_menus WHERE menu_code='M_HRM'), 30,'GROUP','HRM',NULL,TRUE,TRUE),
  ('M_ATTENDANCE',         'Attendance',            '/hrm/attendance',                  'fas fa-clock',          (SELECT id FROM app_menus WHERE menu_code='M_HRM_OPERATIONS'), 10,'LEAF','HRM','hrm.attendance.view',TRUE,TRUE),
  ('M_LEAVE',              'Leave Management',      '/hrm/leaves',                      'fas fa-calendar-xmark', (SELECT id FROM app_menus WHERE menu_code='M_HRM_OPERATIONS'), 20,'LEAF','HRM','hrm.leave.view',TRUE,TRUE),
  ('M_PAYROLL',            'Payroll',               '/hrm/payroll',                     'fas fa-money-check',    (SELECT id FROM app_menus WHERE menu_code='M_HRM_OPERATIONS'), 30,'LEAF','HRM','hrm.payroll.view',TRUE,TRUE),
  ('M_LABOR_COST',         'Labor Cost Allocation', '/hrm/cost-allocations',            'fas fa-chart-line',     (SELECT id FROM app_menus WHERE menu_code='M_HRM_OPERATIONS'), 40,'LEAF','HRM','hrm.costallocation.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- FIXED ASSETS sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_FA_CATEGORIES',  'Asset Categories',   '/assets/categories',         'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_ASSETS'), 10,'LEAF','FIXED_ASSETS','assets.category.view',TRUE,TRUE),
  ('M_FA_ASSETS',      'Assets Register',    '/assets/register',           'fas fa-building',       (SELECT id FROM app_menus WHERE menu_code='M_ASSETS'), 20,'LEAF','FIXED_ASSETS','assets.asset.view',TRUE,TRUE),
  ('M_FA_DEPRECIATION','Depreciation Runs',  '/assets/depreciation',       'fas fa-chart-line',     (SELECT id FROM app_menus WHERE menu_code='M_ASSETS'), 30,'LEAF','FIXED_ASSETS','assets.depreciation.view',TRUE,TRUE),
  ('M_FA_DISPOSAL',    'Asset Disposal',     '/assets/disposals',          'fas fa-trash-can',      (SELECT id FROM app_menus WHERE menu_code='M_ASSETS'), 40,'LEAF','FIXED_ASSETS','assets.disposal.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- BUDGET sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_BGT_FISCAL',     'Fiscal Years',       '/budget/fiscal-years',       'fas fa-calendar',       (SELECT id FROM app_menus WHERE menu_code='M_BUDGET'), 10,'LEAF','BUDGET','budget.fiscalyear.view',TRUE,TRUE),
  ('M_BGT_HEADS',      'Budget Heads',       '/budget/heads',              'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_BUDGET'), 20,'LEAF','BUDGET','budget.head.view',TRUE,TRUE),
  ('M_BGT_BUDGETS',    'Budgets',            '/budget/budgets',            'fas fa-file-invoice',   (SELECT id FROM app_menus WHERE menu_code='M_BUDGET'), 30,'LEAF','BUDGET','budget.budget.view',TRUE,TRUE),
  ('M_BGT_ENCUMBRANCE','Encumbrances',       '/budget/encumbrances',       'fas fa-lock',           (SELECT id FROM app_menus WHERE menu_code='M_BUDGET'), 40,'LEAF','BUDGET','budget.encumbrance.view',TRUE,TRUE),
  ('M_BGT_VS_ACTUAL',  'Budget vs Actual',   '/budget/vs-actual',          'fas fa-chart-bar',      (SELECT id FROM app_menus WHERE menu_code='M_BUDGET'), 50,'LEAF','BUDGET','budget.vsactual.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- CRM sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_CRM_LEADS',      'Leads',              '/crm/leads',                 'fas fa-user-plus',      (SELECT id FROM app_menus WHERE menu_code='M_CRM'), 10,'LEAF','CRM','crm.lead.view',TRUE,TRUE),
  ('M_CRM_OPP',        'Opportunities',      '/crm/opportunities',         'fas fa-bullseye',       (SELECT id FROM app_menus WHERE menu_code='M_CRM'), 20,'LEAF','CRM','crm.opportunity.view',TRUE,TRUE),
  ('M_CRM_ACTIVITIES', 'Activities',         '/crm/activities',            'fas fa-calendar-check', (SELECT id FROM app_menus WHERE menu_code='M_CRM'), 30,'LEAF','CRM','crm.activity.view',TRUE,TRUE),
  ('M_CRM_CONTACTS',   'Contacts',           '/crm/contacts',              'fas fa-address-book',   (SELECT id FROM app_menus WHERE menu_code='M_CRM'), 40,'LEAF','CRM','crm.contact.view',TRUE,TRUE),
  ('M_CRM_FEEDBACK',   'Customer Feedback',  '/crm/feedback',              'fas fa-star',           (SELECT id FROM app_menus WHERE menu_code='M_CRM'), 50,'LEAF','CRM','crm.feedback.view',TRUE,TRUE),
  ('M_CRM_PIPELINE',   'Pipeline',           '/crm/pipeline',              'fas fa-filter',         (SELECT id FROM app_menus WHERE menu_code='M_CRM'), 60,'LEAF','CRM','crm.pipeline.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- COMMERCIAL sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_COM_LC',         'LC Management',      '/commercial/lc',             'fas fa-file-contract',  (SELECT id FROM app_menus WHERE menu_code='M_COMMERCIAL'), 10,'LEAF','COMMERCIAL','commercial.lc.view',TRUE,TRUE),
  ('M_COM_INVOICE',    'Commercial Invoice', '/commercial/invoices',        'fas fa-file-invoice',   (SELECT id FROM app_menus WHERE menu_code='M_COMMERCIAL'), 20,'LEAF','COMMERCIAL','commercial.invoice.view',TRUE,TRUE),
  ('M_COM_SETTLEMENT', 'LC Settlement',      '/commercial/settlements',     'fas fa-handshake',      (SELECT id FROM app_menus WHERE menu_code='M_COMMERCIAL'), 30,'LEAF','COMMERCIAL','commercial.settlement.view',TRUE,TRUE),
  ('M_COM_HS',         'HS Codes',           '/commercial/hs-codes',        'fas fa-barcode',        (SELECT id FROM app_menus WHERE menu_code='M_COMMERCIAL'), 40,'LEAF','COMMERCIAL','commercial.hscode.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- REPORTS sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_RPT_INVENTORY',  'Inventory Reports',  '#',                          'fas fa-boxes-stacked',  (SELECT id FROM app_menus WHERE menu_code='M_REPORTS'), 10,'GROUP','REPORTS',NULL,TRUE,TRUE),
  ('M_RPT_STOCK',      'Stock Position',     '/reports/stock-position',     'fas fa-chart-column',   (SELECT id FROM app_menus WHERE menu_code='M_RPT_INVENTORY'), 10,'LEAF','REPORTS','reports.stock.view',TRUE,TRUE),
  ('M_RPT_STOCK_LEDGER','Stock Ledger',      '/reports/stock-ledger',       'fas fa-book',           (SELECT id FROM app_menus WHERE menu_code='M_RPT_INVENTORY'), 20,'LEAF','REPORTS','reports.stockledger.view',TRUE,TRUE),
  ('M_RPT_PRODUCTION2','Production Reports', '#',                          'fas fa-industry',       (SELECT id FROM app_menus WHERE menu_code='M_REPORTS'), 20,'GROUP','REPORTS',NULL,TRUE,TRUE),
  ('M_RPT_PRD_COST',   'Production Cost',    '/reports/production-cost',    'fas fa-file-invoice',   (SELECT id FROM app_menus WHERE menu_code='M_RPT_PRODUCTION2'), 10,'LEAF','REPORTS','reports.production.view',TRUE,TRUE),
  ('M_RPT_COGS',       'COGS Report',        '/reports/cogs',               'fas fa-chart-line',     (SELECT id FROM app_menus WHERE menu_code='M_RPT_PRODUCTION2'), 20,'LEAF','REPORTS','reports.cogs.view',TRUE,TRUE),
  ('M_RPT_FINANCE',    'Finance Reports',    '#',                          'fas fa-calculator',     (SELECT id FROM app_menus WHERE menu_code='M_REPORTS'), 30,'GROUP','REPORTS',NULL,TRUE,TRUE),
  ('M_RPT_PL',         'Profit & Loss',      '/reports/profit-loss',        'fas fa-chart-bar',      (SELECT id FROM app_menus WHERE menu_code='M_RPT_FINANCE'), 10,'LEAF','REPORTS','reports.pl.view',TRUE,TRUE),
  ('M_RPT_BS',         'Balance Sheet',      '/reports/balance-sheet',      'fas fa-scale-balanced', (SELECT id FROM app_menus WHERE menu_code='M_RPT_FINANCE'), 20,'LEAF','REPORTS','reports.bs.view',TRUE,TRUE),
  ('M_RPT_TB',         'Trial Balance',      '/reports/trial-balance',      'fas fa-list-ol',        (SELECT id FROM app_menus WHERE menu_code='M_RPT_FINANCE'), 30,'LEAF','REPORTS','reports.tb.view',TRUE,TRUE),
  ('M_RPT_CASHFLOW',   'Cash Flow',          '/reports/cash-flow',          'fas fa-water',          (SELECT id FROM app_menus WHERE menu_code='M_RPT_FINANCE'), 40,'LEAF','REPORTS','reports.cashflow.view',TRUE,TRUE),
  ('M_RPT_BUDGET',     'Budget vs Actual',   '/reports/budget-actual',      'fas fa-chart-pie',      (SELECT id FROM app_menus WHERE menu_code='M_RPT_FINANCE'), 50,'LEAF','REPORTS','reports.budgetactual.view',TRUE,TRUE),
  ('M_RPT_PURCHASE',   'Purchase Reports',   '#',                          'fas fa-cart-shopping',  (SELECT id FROM app_menus WHERE menu_code='M_REPORTS'), 40,'GROUP','REPORTS',NULL,TRUE,TRUE),
  ('M_RPT_PO_SUM',     'PO Summary',         '/reports/purchase-summary',   'fas fa-file-invoice',   (SELECT id FROM app_menus WHERE menu_code='M_RPT_PURCHASE'), 10,'LEAF','REPORTS','reports.purchase.view',TRUE,TRUE),
  ('M_RPT_SUPPLIER_LED','Supplier Ledger',   '/reports/supplier-ledger',    'fas fa-book',           (SELECT id FROM app_menus WHERE menu_code='M_RPT_PURCHASE'), 20,'LEAF','REPORTS','reports.supplierledger.view',TRUE,TRUE),
  ('M_RPT_SALES',      'Sales Reports',      '#',                          'fas fa-tags',           (SELECT id FROM app_menus WHERE menu_code='M_REPORTS'), 50,'GROUP','REPORTS',NULL,TRUE,TRUE),
  ('M_RPT_SO_SUM',     'SO Summary',         '/reports/sales-summary',      'fas fa-file-invoice',   (SELECT id FROM app_menus WHERE menu_code='M_RPT_SALES'), 10,'LEAF','REPORTS','reports.sales.view',TRUE,TRUE),
  ('M_RPT_CUSTOMER_LED','Customer Ledger',   '/reports/customer-ledger',    'fas fa-book',           (SELECT id FROM app_menus WHERE menu_code='M_RPT_SALES'), 20,'LEAF','REPORTS','reports.customerledger.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- SETTINGS sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES
  ('M_SET_ORG',       'Organization',       '/settings/organization',      'fas fa-building',       (SELECT id FROM app_menus WHERE menu_code='M_SETTINGS'), 10,'LEAF','SETTINGS','settings.org.view',TRUE,TRUE),
  ('M_SET_BU',        'Business Units',     '/settings/business-units',    'fas fa-diagram-project','(SELECT id FROM app_menus WHERE menu_code=''M_SETTINGS'')',20,'LEAF','SETTINGS','settings.bu.view',TRUE,TRUE),
  ('M_SET_CC',        'Cost Centers',       '/settings/cost-centers',      'fas fa-layer-group',    (SELECT id FROM app_menus WHERE menu_code='M_SETTINGS'), 30,'LEAF','SETTINGS','settings.cc.view',TRUE,TRUE),
  ('M_SET_SECURITY',  'Security',           '#',                          'fas fa-shield-halved',  (SELECT id FROM app_menus WHERE menu_code='M_SETTINGS'), 40,'GROUP','SETTINGS',NULL,TRUE,TRUE),
  ('M_SET_USERS',     'Users',              '/settings/users',             'fas fa-users-cog',      (SELECT id FROM app_menus WHERE menu_code='M_SET_SECURITY'), 10,'LEAF','SETTINGS','settings.user.view',TRUE,TRUE),
  ('M_SET_ROLES',     'Roles',              '/settings/roles',             'fas fa-user-shield',    (SELECT id FROM app_menus WHERE menu_code='M_SET_SECURITY'), 20,'LEAF','SETTINGS','settings.role.view',TRUE,TRUE),
  ('M_SET_PERMISSIONS','Permissions',       '/settings/permissions',       'fas fa-key',            (SELECT id FROM app_menus WHERE menu_code='M_SET_SECURITY'), 30,'LEAF','SETTINGS','settings.permission.view',TRUE,TRUE),
  ('M_SET_MENUS',     'Menu Management',    '/settings/menus',             'fas fa-bars',           (SELECT id FROM app_menus WHERE menu_code='M_SET_SECURITY'), 40,'LEAF','SETTINGS','settings.menu.view',TRUE,TRUE),
  ('M_SET_MASTERS',   'Reference Masters',  '#',                          'fas fa-database',       (SELECT id FROM app_menus WHERE menu_code='M_SETTINGS'), 50,'GROUP','SETTINGS',NULL,TRUE,TRUE),
  ('M_SET_BANKS',     'Banks',              '/settings/banks',             'fas fa-building-columns',(SELECT id FROM app_menus WHERE menu_code='M_SET_MASTERS'), 10,'LEAF','SETTINGS','settings.bank.view',TRUE,TRUE),
  ('M_SET_CURRENCIES','Currencies',         '/settings/currencies',        'fas fa-coins',          (SELECT id FROM app_menus WHERE menu_code='M_SET_MASTERS'), 20,'LEAF','SETTINGS','settings.currency.view',TRUE,TRUE),
  ('M_SET_SEQ',       'Document Sequences', '/settings/sequences',         'fas fa-hashtag',        (SELECT id FROM app_menus WHERE menu_code='M_SET_MASTERS'), 30,'LEAF','SETTINGS','settings.sequence.view',TRUE,TRUE),
  ('M_SET_TERMS',     'Terms & Conditions', '/settings/terms',             'fas fa-file-contract',  (SELECT id FROM app_menus WHERE menu_code='M_SET_MASTERS'), 40,'LEAF','SETTINGS','settings.terms.view',TRUE,TRUE),
  ('M_SET_APPROVAL',  'Approval Configs',   '/settings/approval-configs',  'fas fa-stamp',          (SELECT id FROM app_menus WHERE menu_code='M_SETTINGS'), 60,'LEAF','SETTINGS','settings.approval.view',TRUE,TRUE)
) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- PERMISSIONS seed (key permissions per module)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, module, description, url_pattern, http_method, active) VALUES
-- Dashboard
('dashboard.view',              'DASHBOARD',     'View dashboard',               '/dashboard',                    'GET', TRUE),
-- Purchase
('purchase.supplier.view',      'PURCHASE',      'View suppliers',               '/purchase/suppliers',           'GET', TRUE),
('purchase.supplier.create',    'PURCHASE',      'Create supplier',              '/purchase/suppliers',           'POST',TRUE),
('purchase.supplier.edit',      'PURCHASE',      'Edit supplier',                '/purchase/suppliers/**',        'PUT', TRUE),
('purchase.supplier.delete',    'PURCHASE',      'Delete supplier',              '/purchase/suppliers/**',        'DELETE',TRUE),
('purchase.requisition.view',   'PURCHASE',      'View requisitions',            '/purchase/requisitions',        'GET', TRUE),
('purchase.rfq.view',           'PURCHASE',      'View RFQ',                     '/purchase/rfq',                 'GET', TRUE),
('purchase.order.view',         'PURCHASE',      'View purchase orders',         '/purchase/orders',              'GET', TRUE),
('purchase.order.create',       'PURCHASE',      'Create purchase order',        '/purchase/orders',              'POST',TRUE),
('purchase.order.approve',      'PURCHASE',      'Approve purchase order',       '/purchase/orders/*/approve',    'POST',TRUE),
('purchase.grn.view',           'PURCHASE',      'View GRN',                     '/purchase/grn',                 'GET', TRUE),
('purchase.grn.create',         'PURCHASE',      'Create GRN',                   '/purchase/grn',                 'POST',TRUE),
('purchase.invoice.view',       'PURCHASE',      'View purchase invoice',        '/purchase/invoices',            'GET', TRUE),
('purchase.return.view',        'PURCHASE',      'View purchase return',         '/purchase/returns',             'GET', TRUE),
-- Sales
('sales.customer.view',         'SALES',         'View customers',               '/sales/customers',              'GET', TRUE),
('sales.customer.create',       'SALES',         'Create customer',              '/sales/customers',              'POST',TRUE),
('sales.quotation.view',        'SALES',         'View quotations',              '/sales/quotations',             'GET', TRUE),
('sales.order.view',            'SALES',         'View sales orders',            '/sales/orders',                 'GET', TRUE),
('sales.order.create',          'SALES',         'Create sales order',           '/sales/orders',                 'POST',TRUE),
('sales.order.approve',         'SALES',         'Approve sales order',          '/sales/orders/*/approve',       'POST',TRUE),
('sales.delivery.view',         'SALES',         'View delivery',                '/sales/delivery',               'GET', TRUE),
('sales.invoice.view',          'SALES',         'View sales invoice',           '/sales/invoices',               'GET', TRUE),
('sales.return.view',           'SALES',         'View sales return',            '/sales/returns',                'GET', TRUE),
-- Inventory
('inventory.item.view',         'INVENTORY',     'View items',                   '/inventory/items',              'GET', TRUE),
('inventory.item.create',       'INVENTORY',     'Create item',                  '/inventory/items',              'POST',TRUE),
('inventory.category.view',     'INVENTORY',     'View categories',              '/inventory/categories',         'GET', TRUE),
('inventory.uom.view',          'INVENTORY',     'View UOM',                     '/inventory/uom',                'GET', TRUE),
('inventory.brand.view',        'INVENTORY',     'View brands',                  '/inventory/brands',             'GET', TRUE),
('inventory.warehouse.view',    'INVENTORY',     'View warehouses',              '/inventory/warehouses',         'GET', TRUE),
('inventory.lot.view',          'INVENTORY',     'View lots',                    '/inventory/lots',               'GET', TRUE),
('inventory.stock.view',        'INVENTORY',     'View stock',                   '/inventory/stock',              'GET', TRUE),
('inventory.transfer.view',     'INVENTORY',     'View stock transfers',         '/inventory/transfers',          'GET', TRUE),
('inventory.adjustment.view',   'INVENTORY',     'View stock adjustments',       '/inventory/adjustments',        'GET', TRUE),
('inventory.storereq.view',     'INVENTORY',     'View store requisitions',      '/inventory/store-requisitions', 'GET', TRUE),
('inventory.materialissue.view','INVENTORY',     'View material issue',          '/inventory/material-issue',     'GET', TRUE),
-- Production
('production.bom.view',         'PRODUCTION',    'View BOM',                     '/production/boms',               'GET', TRUE),
('production.bom.create',       'PRODUCTION',    'Create BOM',                   '/production/boms',               'POST',TRUE),
('production.order.view',       'PRODUCTION',    'View production orders',       '/production/orders',            'GET', TRUE),
('production.order.create',     'PRODUCTION',    'Create production order',      '/production/orders',            'POST',TRUE),
('production.order.approve',    'PRODUCTION',    'Approve production order',     '/production/orders/*/approve',  'POST',TRUE),
('production.material.view',    'PRODUCTION',    'View material requisitions',   '/production/material-requisitions','GET',TRUE),
('production.input.view',       'PRODUCTION',    'View material consumption',    '/production/inputs',            'GET', TRUE),
('production.output.view',      'PRODUCTION',    'View finished goods receive',  '/production/outputs',           'GET', TRUE),
('production.costsheet.view',   'PRODUCTION',    'View cost sheets',             '/production/cost-sheets',       'GET', TRUE),
-- Accounts
('accounts.coa.view',           'ACCOUNTS',      'View chart of accounts',       '/accounts/chart',               'GET', TRUE),
('accounts.subledger.view',     'ACCOUNTS',      'View sub ledger',              '/accounts/sub-ledger',          'GET', TRUE),
('accounts.bank.view',          'ACCOUNTS',      'View bank accounts',           '/accounts/bank-accounts',       'GET', TRUE),
('accounts.journal.view',       'ACCOUNTS',      'View journals',                '/accounts/journals',            'GET', TRUE),
('accounts.journal.create',     'ACCOUNTS',      'Create journal',               '/accounts/journals',            'POST',TRUE),
('accounts.payment.view',       'ACCOUNTS',      'View payment vouchers',        '/accounts/payment-vouchers',    'GET', TRUE),
('accounts.receipt.view',       'ACCOUNTS',      'View receipt vouchers',        '/accounts/receipt-vouchers',    'GET', TRUE),
('accounts.mapping.view',       'ACCOUNTS',      'View account mapping',         '/accounts/mapping',             'GET', TRUE),
-- HRM
('hrm.designation.view',        'HRM',           'View designations',            '/hrm/designations',             'GET', TRUE),
('hrm.department.view',         'HRM',           'View departments',             '/hrm/departments',              'GET', TRUE),
('hrm.employee.view',           'HRM',           'View employees',               '/hrm/employees',                'GET', TRUE),
('hrm.employee.create',         'HRM',           'Create employee',              '/hrm/employees',                'POST',TRUE),
('hrm.attendance.view',         'HRM',           'View attendance',              '/hrm/attendance',               'GET', TRUE),
('hrm.leave.view',              'HRM',           'View leaves',                  '/hrm/leaves',                   'GET', TRUE),
('hrm.payroll.view',            'HRM',           'View payroll',                 '/hrm/payroll',                  'GET', TRUE),
('hrm.costallocation.view',     'HRM',           'View cost allocations',        '/hrm/cost-allocations',         'GET', TRUE),
-- Fixed Assets
('assets.category.view',        'FIXED_ASSETS',  'View asset categories',        '/assets/categories',            'GET', TRUE),
('assets.asset.view',           'FIXED_ASSETS',  'View assets',                  '/assets/register',              'GET', TRUE),
('assets.depreciation.view',    'FIXED_ASSETS',  'View depreciation runs',       '/assets/depreciation',          'GET', TRUE),
('assets.disposal.view',        'FIXED_ASSETS',  'View disposals',               '/assets/disposals',             'GET', TRUE),
-- Budget
('budget.fiscalyear.view',      'BUDGET',        'View fiscal years',            '/budget/fiscal-years',          'GET', TRUE),
('budget.head.view',            'BUDGET',        'View budget heads',            '/budget/heads',                 'GET', TRUE),
('budget.budget.view',          'BUDGET',        'View budgets',                 '/budget/budgets',               'GET', TRUE),
('budget.encumbrance.view',     'BUDGET',        'View encumbrances',            '/budget/encumbrances',          'GET', TRUE),
('budget.vsactual.view',        'BUDGET',        'View budget vs actual',        '/budget/vs-actual',             'GET', TRUE),
-- CRM
('crm.lead.view',               'CRM',           'View leads',                   '/crm/leads',                    'GET', TRUE),
('crm.opportunity.view',        'CRM',           'View opportunities',           '/crm/opportunities',            'GET', TRUE),
('crm.activity.view',           'CRM',           'View activities',              '/crm/activities',               'GET', TRUE),
('crm.contact.view',            'CRM',           'View contacts',                '/crm/contacts',                 'GET', TRUE),
('crm.feedback.view',           'CRM',           'View customer feedback',       '/crm/feedback',                 'GET', TRUE),
('crm.pipeline.view',           'CRM',           'View CRM pipeline',            '/crm/pipeline',                 'GET', TRUE),
-- Commercial
('commercial.lc.view',          'COMMERCIAL',    'View LC',                      '/commercial/lc',                'GET', TRUE),
('commercial.invoice.view',     'COMMERCIAL',    'View commercial invoices',     '/commercial/invoices',          'GET', TRUE),
('commercial.settlement.view',  'COMMERCIAL',    'View LC settlements',          '/commercial/settlements',       'GET', TRUE),
('commercial.hscode.view',      'COMMERCIAL',    'View HS codes',                '/commercial/hs-codes',          'GET', TRUE),
-- Reports
('reports.stock.view',          'REPORTS',       'View stock reports',           '/reports/stock-position',       'GET', TRUE),
('reports.cogs.view',           'REPORTS',       'View COGS report',             '/reports/cogs',                 'GET', TRUE),
('reports.pl.view',             'REPORTS',       'View P&L report',              '/reports/profit-loss',          'GET', TRUE),
('reports.bs.view',             'REPORTS',       'View Balance Sheet',           '/reports/balance-sheet',        'GET', TRUE),
('reports.purchase.view',       'REPORTS',       'View purchase reports',        '/reports/purchase-summary',     'GET', TRUE),
('reports.sales.view',          'REPORTS',       'View sales reports',           '/reports/sales-summary',        'GET', TRUE),
('reports.production.view',     'REPORTS',       'View production reports',      '/reports/production-cost',      'GET', TRUE),
('reports.budgetactual.view',   'REPORTS',       'View budget actual report',    '/reports/budget-actual',        'GET', TRUE),
-- Settings
('settings.org.view',           'SETTINGS',      'View organization',            '/settings/organization',        'GET', TRUE),
('settings.user.view',          'SETTINGS',      'View users',                   '/settings/users',               'GET', TRUE),
('settings.role.view',          'SETTINGS',      'View roles',                   '/settings/roles',               'GET', TRUE),
('settings.menu.view',          'SETTINGS',      'View menus',                   '/settings/menus',               'GET', TRUE),
('settings.approval.view',      'SETTINGS',      'View approval configs',        '/settings/approval-configs',    'GET', TRUE)
ON CONFLICT (name) DO NOTHING;
