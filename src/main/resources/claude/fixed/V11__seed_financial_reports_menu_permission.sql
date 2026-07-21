-- =============================================================================
--  Spindle ERP  —  Financial Reports: Menu + Permission + Role-Menu Seed
--  File   : V11__seed_financial_reports_menu_permission.sql
--  Target : PostgreSQL
--
--  Adds 14 new financial report pages under the existing GRP_ACC_REPORTS group.
--
--  New Reports:
--    1. Day Book              (/accounts/reports/day-book)
--    2. Voucher Register      (/accounts/reports/voucher-register)
--    3. Cash Flow Statement   (/accounts/reports/cash-flow)
--    4. Bank Book             (/accounts/reports/bank-book)
--    5. Cash Book             (/accounts/reports/cash-book)
--    6. Party Ledger          (/accounts/reports/party-ledger)
--    7. Comparative P&L       (/accounts/reports/comparative-pl)
--    8. Comparative TB        (/accounts/reports/comparative-tb)
--    9. Tax Summary           (/accounts/reports/tax-summary)
--   10. Financial KPIs        (/accounts/reports/financial-kpis)
--   11. Cost Center Summary   (/accounts/reports/cost-center-summary)
--   12. Account Summary       (/accounts/reports/account-summary)
--   13. Sub-Account Summary    (/accounts/reports/sub-account-summary)
--   14. Budget vs Actual      (/accounts/reports/budget-vs-actual)
--
--  Safe to re-run: all INSERTs use ON CONFLICT DO NOTHING.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1. PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at, updated_at)
VALUES
-- Day Book
('acc.report.daybook.view', 'View Day Book report', '/accounts/reports/day-book/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.daybook.data', 'Day Book data JSON', '/accounts/reports/data/day-book', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Voucher Register
('acc.report.vreg.view', 'View Voucher Register report', '/accounts/reports/voucher-register/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.vreg.data', 'Voucher Register data JSON', '/accounts/reports/data/voucher-register', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Cash Flow Statement
('acc.report.cashflow.view', 'View Cash Flow Statement', '/accounts/reports/cash-flow/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.cashflow.data', 'Cash Flow data JSON', '/accounts/reports/data/cash-flow', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Bank Book
('acc.report.bankbook.view', 'View Bank Book report', '/accounts/reports/bank-book/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.bankbook.data', 'Bank Book data JSON', '/accounts/reports/data/bank-book', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Cash Book
('acc.report.cashbook.view', 'View Cash Book report', '/accounts/reports/cash-book/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.cashbook.data', 'Cash Book data JSON', '/accounts/reports/data/cash-book', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Party Ledger
('acc.report.partyledger.view', 'View Party Ledger report', '/accounts/reports/party-ledger/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.partyledger.data', 'Party Ledger data JSON', '/accounts/reports/data/party-ledger', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Comparative P&L
('acc.report.comppl.view', 'View Comparative P&L', '/accounts/reports/comparative-pl/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.comppl.data', 'Comparative P&L data JSON', '/accounts/reports/data/comparative-pl', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Comparative Trial Balance
('acc.report.comptb.view', 'View Comparative Trial Balance', '/accounts/reports/comparative-tb/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.comptb.data', 'Comparative TB data JSON', '/accounts/reports/data/comparative-tb', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Tax Summary
('acc.report.tax.view', 'View Tax Summary report', '/accounts/reports/tax-summary/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.tax.data', 'Tax Summary data JSON', '/accounts/reports/data/tax-summary', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Financial KPIs
('acc.report.kpis.view', 'View Financial KPIs dashboard', '/accounts/reports/financial-kpis/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.kpis.data', 'Financial KPIs data JSON', '/accounts/reports/data/financial-kpis', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Cost Center Summary
('acc.report.cc.view', 'View Cost Center Summary', '/accounts/reports/cost-center-summary/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.cc.data', 'Cost Center Summary data JSON', '/accounts/reports/data/cost-center-summary', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Account Summary
('acc.report.accsum.view', 'View Account Transaction Summary', '/accounts/reports/account-summary/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.accsum.data', 'Account Summary data JSON', '/accounts/reports/data/account-summary', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Sub-Account Summary
('acc.report.subsum.view', 'View Sub-Account Summary', '/accounts/reports/sub-account-summary/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.subsum.data', 'Sub-Account Summary data JSON', '/accounts/reports/data/sub-account-summary', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),

-- Budget vs Actual
('acc.report.bgtact.view', 'View Budget vs Actual report', '/accounts/reports/budget-vs-actual/**', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW()),
('acc.report.bgtact.data', 'Budget vs Actual data JSON', '/accounts/reports/data/budget-vs-actual', 'GET', 'FINANCE_ACCOUNTS', 'REPORTS', true, NOW(), NOW())

ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 2. MENU ENTRIES (under GRP_ACC_REPORTS)
-- ═════════════════════════════════════════════════════════════════════════════

-- Day Book (order 60 - after Aging which is 50)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_DAYBOOK', 'Day Book', '/accounts/reports/day-book', 'fa fa-calendar-day', g.id, 60, 'LEAF', 'ACCOUNTS', 'acc.report.daybook.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Voucher Register (70)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_VREG', 'Voucher Register', '/accounts/reports/voucher-register', 'fa fa-list', g.id, 70, 'LEAF', 'ACCOUNTS', 'acc.report.vreg.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Cash Flow Statement (80)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_CASHFLOW', 'Cash Flow Statement', '/accounts/reports/cash-flow', 'fa fa-money-bill-wave', g.id, 80, 'LEAF', 'ACCOUNTS', 'acc.report.cashflow.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Bank Book (90)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_BANKBOOK', 'Bank Book', '/accounts/reports/bank-book', 'fa fa-university', g.id, 90, 'LEAF', 'ACCOUNTS', 'acc.report.bankbook.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Cash Book (100)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_CASHBOOK', 'Cash Book', '/accounts/reports/cash-book', 'fa fa-coins', g.id, 100, 'LEAF', 'ACCOUNTS', 'acc.report.cashbook.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Party Ledger (110)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_PARTYLEDGER', 'Party Ledger', '/accounts/reports/party-ledger', 'fa fa-address-book', g.id, 110, 'LEAF', 'ACCOUNTS', 'acc.report.partyledger.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Comparative P&L (120)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_COMPPL', 'Comparative P&L', '/accounts/reports/comparative-pl', 'fa fa-chart-simple', g.id, 120, 'LEAF', 'ACCOUNTS', 'acc.report.comppl.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Comparative Trial Balance (130)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_COMPTB', 'Comparative TB', '/accounts/reports/comparative-tb', 'fa fa-scale-balanced', g.id, 130, 'LEAF', 'ACCOUNTS', 'acc.report.comptb.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Tax Summary (140)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_TAX', 'Tax Summary', '/accounts/reports/tax-summary', 'fa fa-file-invoice', g.id, 140, 'LEAF', 'ACCOUNTS', 'acc.report.tax.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Financial KPIs (150)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_KPIS', 'Financial KPIs', '/accounts/reports/financial-kpis', 'fa fa-chart-pie', g.id, 150, 'LEAF', 'ACCOUNTS', 'acc.report.kpis.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Cost Center Summary (160)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_COSTCENTER', 'Cost Center Summary', '/accounts/reports/cost-center-summary', 'fa fa-layer-group', g.id, 160, 'LEAF', 'ACCOUNTS', 'acc.report.cc.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Account Transaction Summary (170)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_ACCSUM', 'Account Summary', '/accounts/reports/account-summary', 'fa fa-calculator', g.id, 170, 'LEAF', 'ACCOUNTS', 'acc.report.accsum.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Sub-Account Summary (180)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_SUBSUM', 'Sub-Account Summary', '/accounts/reports/sub-account-summary', 'fa fa-users', g.id, 180, 'LEAF', 'ACCOUNTS', 'acc.report.subsum.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;

-- Budget vs Actual (190)
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'ACC_REP_BGTACT', 'Budget vs Actual', '/accounts/reports/budget-vs-actual', 'fa fa-chart-column', g.id, 190, 'LEAF', 'ACCOUNTS', 'acc.report.bgtact.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_REPORTS'
ON CONFLICT (menu_code) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 3. ROLE-MENU ACCESS GRANTS
-- ═════════════════════════════════════════════════════════════════════════════

-- Finance Manager gets all new reports with CRUD
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true, false, false, false, NOW(), NOW()
FROM sec_roles r
CROSS JOIN app_menus m
WHERE r.name = 'ROLE_FINANCE_MANAGER'
  AND m.menu_code IN (
    'ACC_REP_DAYBOOK', 'ACC_REP_VREG', 'ACC_REP_CASHFLOW',
    'ACC_REP_BANKBOOK', 'ACC_REP_CASHBOOK', 'ACC_REP_PARTYLEDGER',
    'ACC_REP_COMPPL', 'ACC_REP_COMPTB', 'ACC_REP_TAX',
    'ACC_REP_KPIS', 'ACC_REP_COSTCENTER', 'ACC_REP_ACCSUM',
    'ACC_REP_SUBSUM', 'ACC_REP_BGTACT'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sec_mrole_menus x
    WHERE x.role_id = r.id AND x.menu_id = m.id
  );

-- Report Manager gets all new reports with view-only
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true, false, false, false, NOW(), NOW()
FROM sec_roles r
CROSS JOIN app_menus m
WHERE r.name = 'ROLE_REPORT_MANAGER'
  AND m.menu_code IN (
    'ACC_REP_DAYBOOK', 'ACC_REP_VREG', 'ACC_REP_CASHFLOW',
    'ACC_REP_BANKBOOK', 'ACC_REP_CASHBOOK', 'ACC_REP_PARTYLEDGER',
    'ACC_REP_COMPPL', 'ACC_REP_COMPTB', 'ACC_REP_TAX',
    'ACC_REP_KPIS', 'ACC_REP_COSTCENTER', 'ACC_REP_ACCSUM',
    'ACC_REP_SUBSUM', 'ACC_REP_BGTACT'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sec_mrole_menus x
    WHERE x.role_id = r.id AND x.menu_id = m.id
  );

-- SUPER_ADMIN gets all new reports (handled by wildcard permission '*', but menu visibility still needed)
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true, true, true, true, NOW(), NOW()
FROM sec_roles r
CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND m.menu_code IN (
    'ACC_REP_DAYBOOK', 'ACC_REP_VREG', 'ACC_REP_CASHFLOW',
    'ACC_REP_BANKBOOK', 'ACC_REP_CASHBOOK', 'ACC_REP_PARTYLEDGER',
    'ACC_REP_COMPPL', 'ACC_REP_COMPTB', 'ACC_REP_TAX',
    'ACC_REP_KPIS', 'ACC_REP_COSTCENTER', 'ACC_REP_ACCSUM',
    'ACC_REP_SUBSUM', 'ACC_REP_BGTACT',
    'GRP_ACC_REPORTS'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sec_mrole_menus x
    WHERE x.role_id = r.id AND x.menu_id = m.id
  );

-- Also ensure SUPER_ADMIN has GRP_ACC_REPORTS access
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true, true, true, true, NOW(), NOW()
FROM sec_roles r
CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND m.menu_code = 'GRP_ACC_REPORTS'
  AND NOT EXISTS (
    SELECT 1 FROM sec_mrole_menus x WHERE x.role_id = r.id AND x.menu_id = m.id
  );

-- ═════════════════════════════════════════════════════════════════════════════
-- 4. ROLE-PERMISSION GRANTS
-- ═════════════════════════════════════════════════════════════════════════════

-- Finance Manager: all new report permissions
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
CROSS JOIN sec_permissions p
WHERE r.name = 'ROLE_FINANCE_MANAGER'
  AND p.name LIKE 'acc.report.%'
  AND p.active = true
  AND NOT EXISTS (
    SELECT 1 FROM sec_role_permissions x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- Report Manager: all new report permissions
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
CROSS JOIN sec_permissions p
WHERE r.name = 'ROLE_REPORT_MANAGER'
  AND p.name LIKE 'acc.report.%'
  AND p.active = true
  AND NOT EXISTS (
    SELECT 1 FROM sec_role_permissions x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- Admin role: all new report permissions
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
CROSS JOIN sec_permissions p
WHERE r.name = 'ROLE_ADMIN'
  AND p.name LIKE 'acc.report.%'
  AND p.active = true
  AND NOT EXISTS (
    SELECT 1 FROM sec_role_permissions x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );

-- Budget Manager: Budget vs Actual permission
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
CROSS JOIN sec_permissions p
WHERE r.name = 'ROLE_BUDGET_MANAGER'
  AND p.name IN ('acc.report.bgtact.view', 'acc.report.bgtact.data')
  AND NOT EXISTS (
    SELECT 1 FROM sec_role_permissions x
    WHERE x.role_id = r.id AND x.permission_id = p.id
  );

COMMIT;
