-- ╔══════════════════════════════════════════════════════════════════════════════╗
-- ║  SPINDLE ERP — ACCOUNTS MODULE MENU & PERMISSION SEED  v3.0                 ║
-- ║  Appended as V3 migration after V2 (generic ERP menus).                     ║
-- ║                                                                              ║
-- ║  Hierarchy:  M_ACCOUNTS (MODULE) → GROUP → LEAF                             ║
-- ║  URLs map to:  ChartOfAccountController  /accounts/chart                    ║
-- ║                ChartOfAccountSubController /accounts/sub-accounts            ║
-- ║                JournalController            /accounts/journals               ║
-- ║                VoucherController            /accounts/vouchers               ║
-- ║                AccountsMappingController    /accounts/mapping                ║
-- ║                AccountsPolicyController     /accounts/policy                 ║
-- ║                AccountingPeriodController   /accounts/periods                ║
-- ╚══════════════════════════════════════════════════════════════════════════════╝

-- ─────────────────────────────────────────────────────────────────────────────
-- ACCOUNTS sub-menus
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
SELECT v.* FROM (VALUES

  -- ── MASTERS GROUP ───────────────────────────────────────────────────────────
  ('M_ACC_MASTERS',          'Masters',                    '#',
   'fas fa-layer-group',
   (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 10,'GROUP','ACCOUNTS',NULL,TRUE,TRUE),

  ('M_ACC_COA',              'Chart of Accounts',          '/accounts/chart',
   'fas fa-sitemap',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 10,'LEAF','ACCOUNTS','accounts.coa.view',TRUE,TRUE),

  ('M_ACC_SUB',              'Sub-Ledger Accounts',        '/accounts/sub-accounts',
   'fas fa-list-tree',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 20,'LEAF','ACCOUNTS','accounts.subledger.view',TRUE,TRUE),

  ('M_ACC_BANK_ACCTS',       'Bank Accounts',              '/accounts/bank-accounts',
   'fas fa-building-columns',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 30,'LEAF','ACCOUNTS','accounts.bank.view',TRUE,TRUE),

  ('M_ACC_CASH_ACCTS',       'Cash Accounts',              '/accounts/cash-accounts',
   'fas fa-vault',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 40,'LEAF','ACCOUNTS','accounts.cash.view',TRUE,TRUE),

  ('M_ACC_CUSTOMERS',        'Customer Accounts',          '/accounts/customer-accounts',
   'fas fa-user-tie',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 50,'LEAF','ACCOUNTS','accounts.customer.view',TRUE,TRUE),

  ('M_ACC_SUPPLIERS',        'Supplier Accounts',          '/accounts/supplier-accounts',
   'fas fa-truck',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 60,'LEAF','ACCOUNTS','accounts.supplier.view',TRUE,TRUE),

  ('M_ACC_EMPLOYEES',        'Employee Accounts',          '/accounts/employee-accounts',
   'fas fa-id-badge',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 70,'LEAF','ACCOUNTS','accounts.employee.view',TRUE,TRUE),

  ('M_ACC_PERIODS',          'Accounting Periods',         '/accounts/periods',
   'fas fa-calendar-days',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_MASTERS'), 80,'LEAF','ACCOUNTS','accounts.period.view',TRUE,TRUE),

  -- ── TRANSACTIONS GROUP ───────────────────────────────────────────────────────
  ('M_ACC_TX',               'Transactions',               '#',
   'fas fa-receipt',
   (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 20,'GROUP','ACCOUNTS',NULL,TRUE,TRUE),

  ('M_ACC_JOURNAL',          'Journal Voucher',            '/accounts/journals',
   'fas fa-book-open',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_TX'), 10,'LEAF','ACCOUNTS','accounts.journal.view',TRUE,TRUE),

  ('M_ACC_PAYMENT',          'Payment Voucher',            '/accounts/payment-vouchers',
   'fas fa-money-bill-transfer',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_TX'), 20,'LEAF','ACCOUNTS','accounts.payment.view',TRUE,TRUE),

  ('M_ACC_RECEIPT',          'Receipt Voucher',            '/accounts/receipt-vouchers',
   'fas fa-money-bill-wave',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_TX'), 30,'LEAF','ACCOUNTS','accounts.receipt.view',TRUE,TRUE),

  ('M_ACC_CONTRA',           'Contra Voucher',             '/accounts/contra-vouchers',
   'fas fa-right-left',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_TX'), 40,'LEAF','ACCOUNTS','accounts.contra.view',TRUE,TRUE),

  ('M_ACC_DEBIT_NOTE',       'Debit Note',                 '/accounts/debit-notes',
   'fas fa-file-circle-minus',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_TX'), 50,'LEAF','ACCOUNTS','accounts.debitnote.view',TRUE,TRUE),

  ('M_ACC_CREDIT_NOTE',      'Credit Note',                '/accounts/credit-notes',
   'fas fa-file-circle-plus',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_TX'), 60,'LEAF','ACCOUNTS','accounts.creditnote.view',TRUE,TRUE),

  ('M_ACC_OPENING_BAL',      'Opening Balances',           '/accounts/opening-balances',
   'fas fa-scale-balanced',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_TX'), 70,'LEAF','ACCOUNTS','accounts.openingbal.view',TRUE,TRUE),

  -- ── CONFIGURATION GROUP ──────────────────────────────────────────────────────
  ('M_ACC_CONFIG',           'Configuration',              '#',
   'fas fa-sliders',
   (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 30,'GROUP','ACCOUNTS',NULL,TRUE,TRUE),

  ('M_ACC_MAPPING',          'Accounts Mapping',           '/accounts/mapping',
   'fas fa-diagram-project',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_CONFIG'), 10,'LEAF','ACCOUNTS','accounts.mapping.view',TRUE,TRUE),

  ('M_ACC_POLICY',           'Voucher Policy',             '/accounts/policy',
   'fas fa-shield-check',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_CONFIG'), 20,'LEAF','ACCOUNTS','accounts.policy.view',TRUE,TRUE),

  ('M_ACC_AUTO_TEMPLATES',   'Auto Journal Templates',     '/accounts/auto-templates',
   'fas fa-wand-magic-sparkles',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_CONFIG'), 30,'LEAF','ACCOUNTS','accounts.autotemplate.view',TRUE,TRUE),

  -- ── REPORTS GROUP ────────────────────────────────────────────────────────────
  ('M_ACC_REPORTS',          'Reports',                    '#',
   'fas fa-chart-bar',
   (SELECT id FROM app_menus WHERE menu_code='M_ACCOUNTS'), 40,'GROUP','ACCOUNTS',NULL,TRUE,TRUE),

  ('M_ACC_TRIAL',            'Trial Balance',              '/accounts/reports/trial-balance',
   'fas fa-balance-scale',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_REPORTS'), 10,'LEAF','ACCOUNTS','accounts.report.trial',TRUE,TRUE),

  ('M_ACC_PL',               'Profit & Loss',              '/accounts/reports/profit-loss',
   'fas fa-chart-line',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_REPORTS'), 20,'LEAF','ACCOUNTS','reports.pl.view',TRUE,TRUE),

  ('M_ACC_BS',               'Balance Sheet',              '/accounts/reports/balance-sheet',
   'fas fa-file-invoice-dollar',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_REPORTS'), 30,'LEAF','ACCOUNTS','reports.bs.view',TRUE,TRUE),

  ('M_ACC_LEDGER',           'Account Ledger',             '/accounts/reports/ledger',
   'fas fa-book',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_REPORTS'), 40,'LEAF','ACCOUNTS','accounts.report.ledger',TRUE,TRUE),

  ('M_ACC_CASHBOOK',         'Cash Book',                  '/accounts/reports/cash-book',
   'fas fa-money-bill',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_REPORTS'), 50,'LEAF','ACCOUNTS','accounts.report.cashbook',TRUE,TRUE),

  ('M_ACC_BANKBOOK',         'Bank Book',                  '/accounts/reports/bank-book',
   'fas fa-building-columns',
   (SELECT id FROM app_menus WHERE menu_code='M_ACC_REPORTS'), 60,'LEAF','ACCOUNTS','accounts.report.bankbook',TRUE,TRUE)

) AS v(menu_code,menu_name,menu_url,icon,parent_id,display_order,menu_type,module_name,required_permission,active,visible)
ON CONFLICT (menu_code) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- ACCOUNTS permissions
-- ─────────────────────────────────────────────────────────────────────────────
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

-- ─────────────────────────────────────────────────────────────────────────────
-- Grant all accounts permissions to ROLE_SUPER_ADMIN and ROLE_ACCOUNTS_ADMIN
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
CROSS JOIN sec_permissions p
WHERE r.name IN ('ROLE_SUPER_ADMIN', 'ROLE_ACCOUNTS_ADMIN')
  AND p.module = 'ACCOUNTS'
ON CONFLICT DO NOTHING;

-- Grant view-only accounts perms to ROLE_ADMIN, ROLE_VIEWER
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
CROSS JOIN sec_permissions p
WHERE r.name IN ('ROLE_ADMIN', 'ROLE_VIEWER')
  AND p.module = 'ACCOUNTS'
  AND p.name LIKE '%.view'
ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- Grant all Accounts menus to ROLE_SUPER_ADMIN & ROLE_ACCOUNTS_ADMIN
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete)
SELECT r.id, m.id, TRUE, TRUE, TRUE, TRUE
FROM sec_roles r
CROSS JOIN app_menus m
WHERE r.name IN ('ROLE_SUPER_ADMIN', 'ROLE_ACCOUNTS_ADMIN')
  AND m.module_name = 'ACCOUNTS'
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- View-only for ROLE_VIEWER
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete)
SELECT r.id, m.id, TRUE, FALSE, FALSE, FALSE
FROM sec_roles r
CROSS JOIN app_menus m
WHERE r.name = 'ROLE_VIEWER'
  AND m.module_name = 'ACCOUNTS'
ON CONFLICT (role_id, menu_id) DO NOTHING;
