-- =============================================================================
--  Optimum ERP — Chart of Accounts Seed Data
--  File   : 05_seed_chart_of_accounts.sql
--  Target : PostgreSQL (acc_chart_of_accounts, acc_chart_of_accounts_sub,
--           acc_periods, acc_opening_balances, acc_mapping,
--           acc_mapping_details, acc_policy)
--
--  NOTE: assumes organization_id = 1 (org_organizations.code = 'ORG-001'),
--        and org_business_units / org_cost_centers already seeded.
--        acc_chart_of_accounts_sub.bank_id references stp_banks — left NULL
--        below since no bank seed data was provided; populate after banks
--        are seeded if sub-accounts need a bank link.
--
--  Execution order:
--    1. acc_chart_of_accounts   (self-referential parent_account_id; ROOT->LEAF)
--    2. acc_chart_of_accounts_sub (depends on main_account_id)
--    3. acc_periods
--    4. acc_opening_balances    (depends on accounts + periods)
--    5. acc_mapping             (depends on chart of accounts)
--    6. acc_mapping_details     (depends on acc_mapping)
--    7. acc_policy              (depends on chart of accounts, acc_mapping)
--
--  Idempotent via ON CONFLICT DO NOTHING on unique constraints.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. CHART OF ACCOUNTS  (level 1 = root groups, level 2 = sub-groups, level 3 = leaf accounts)
-- =============================================================================

-- ── Level 1: Root account groups (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE) ──
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('1000', 'Assets',      'ASSET',     'DEBIT',  1, true, true, true, false, 'BDT', 0.00, 0.00, 1, NULL, NOW(), NOW(), 'system', 'system'),
    ('2000', 'Liabilities', 'LIABILITY', 'CREDIT', 1, true, true, true, false, 'BDT', 0.00, 0.00, 1, NULL, NOW(), NOW(), 'system', 'system'),
    ('3000', 'Equity',      'EQUITY',    'CREDIT', 1, true, true, true, false, 'BDT', 0.00, 0.00, 1, NULL, NOW(), NOW(), 'system', 'system'),
    ('4000', 'Revenue',     'REVENUE',   'CREDIT', 1, true, true, true, false, 'BDT', 0.00, 0.00, 1, NULL, NOW(), NOW(), 'system', 'system'),
    ('5000', 'Expenses',    'EXPENSE',   'DEBIT',  1, true, true, true, false, 'BDT', 0.00, 0.00, 1, NULL, NOW(), NOW(), 'system', 'system')
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 2: Sub-groups under Assets ──────────────────────────────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '1100', 'Current Assets', 'ASSET', 'DEBIT', 2, true, true, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '1000' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '1200', 'Fixed Assets', 'ASSET', 'DEBIT', 2, true, true, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '1000' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 2: Sub-groups under Liabilities ────────────────────────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '2100', 'Current Liabilities', 'LIABILITY', 'CREDIT', 2, true, true, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '2000' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 2: Sub-groups under Expenses ────────────────────────────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '5100', 'Operating Expenses', 'EXPENSE', 'DEBIT', 2, true, true, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '5000' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 3: Leaf accounts under Current Assets (1100) ───────────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '1101', 'Cash in Hand', 'ASSET', 'DEBIT', 3, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '1100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '1102', 'Bank Accounts', 'ASSET', 'DEBIT', 3, true, false, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '1100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '1103', 'Accounts Receivable', 'ASSET', 'DEBIT', 3, true, false, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '1100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '1104', 'Inventory', 'ASSET', 'DEBIT', 3, true, false, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '1100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '1105', 'Input VAT', 'ASSET', 'DEBIT', 3, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '1100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 3: Leaf accounts under Fixed Assets (1200) ──────────────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '1201', 'Plant & Machinery', 'ASSET', 'DEBIT', 3, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '1200' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 3: Leaf accounts under Current Liabilities (2100) ──────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '2101', 'Accounts Payable', 'LIABILITY', 'CREDIT', 3, true, false, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '2100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '2102', 'Output VAT', 'LIABILITY', 'CREDIT', 3, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '2100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '2103', 'TDS Payable', 'LIABILITY', 'CREDIT', 3, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '2100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 3: Leaf accounts under Equity (3000) ────────────────────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '3001', 'Owner''s Capital', 'EQUITY', 'CREDIT', 2, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '3000' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '3002', 'Retained Earnings', 'EQUITY', 'CREDIT', 2, true, true, false, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '3000' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 3: Leaf accounts under Revenue (4000) ────────────────────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '4001', 'Sales Revenue', 'REVENUE', 'CREDIT', 2, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '4000' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '4002', 'Discount Allowed', 'REVENUE', 'DEBIT', 2, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '4000' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- ── Level 3: Leaf accounts under Operating Expenses (5100) ───────────────────
INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '5101', 'Salary & Wages', 'EXPENSE', 'DEBIT', 3, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '5100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '5102', 'Office Rent', 'EXPENSE', 'DEBIT', 3, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '5100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '5103', 'Cost of Goods Sold', 'EXPENSE', 'DEBIT', 3, true, false, true, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '5100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '5104', 'Freight & Carrying Charges', 'EXPENSE', 'DEBIT', 3, true, false, false, true, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '5100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '5105', 'Rounding Off', 'EXPENSE', 'DEBIT', 3, true, true, false, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '5100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

INSERT INTO acc_chart_of_accounts
(account_code, account_name, account_type, account_nature, level, is_active, is_system,
 is_control_account, allow_manual_entry, currency, opening_balance, current_balance,
 organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT '5106', 'Foreign Exchange Gain/Loss', 'EXPENSE', 'DEBIT', 3, true, true, false, false, 'BDT', 0.00, 0.00, 1, p.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts p WHERE p.account_code = '5100' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;


-- =============================================================================
-- 2. CHART OF ACCOUNTS — SUB-ACCOUNTS (CUSTOMER / SUPPLIER / BANK / CASH examples)
-- =============================================================================

-- ── CASH sub-account (under Cash in Hand, 1101) ──────────────────────────────
INSERT INTO acc_chart_of_accounts_sub
(sub_account_code, sub_account_name, sub_account_type, cash_account_code, cash_account_type,
 is_active, requires_approval, organization_id, main_account_id, opening_balance, current_balance,
 created_at, updated_at, created_by, updated_by)
SELECT 'CASH-001', 'Head Office Petty Cash', 'CASH', 'CASH-001', 'PETTY_CASH',
       true, false, 1, m.id, 0.00, 0.00, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts m WHERE m.account_code = '1101' AND m.organization_id = 1
    ON CONFLICT ON CONSTRAINT ukouab30ywmi6yp6tvn9ys37fvh DO NOTHING;

-- ── BANK sub-account (under Bank Accounts, 1102) ─────────────────────────────
-- bank_id left NULL — link to stp_banks.id once banks are seeded
INSERT INTO acc_chart_of_accounts_sub
(sub_account_code, sub_account_name, sub_account_type, bank_account_code, bank_account_type,
 bank_name, branch_name, account_number, account_title, currency,
 is_active, requires_approval, organization_id, main_account_id, bank_id, opening_balance, current_balance,
 created_at, updated_at, created_by, updated_by)
SELECT 'BANK-001', 'DBBL Current Account', 'BANK', 'BANK-001', 'CURRENT',
       'Dutch-Bangla Bank Ltd.', 'Gulshan Branch', '1234567890123', 'Optimum ERP Ltd.', 'BDT',
       true, true, 1, m.id, NULL, 0.00, 0.00, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts m WHERE m.account_code = '1102' AND m.organization_id = 1
    ON CONFLICT ON CONSTRAINT ukjq8x73fw7yktf31iimaupoylw DO NOTHING;

-- ── CUSTOMER sub-account (under Accounts Receivable, 1103) ───────────────────
INSERT INTO acc_chart_of_accounts_sub
(sub_account_code, sub_account_name, sub_account_type, customer_code, customer_group,
 contact_person, contact_phone, contact_email, credit_limit, credit_days,
 is_active, requires_approval, organization_id, main_account_id, opening_balance, current_balance,
 created_at, updated_at, created_by, updated_by)
SELECT 'CUST-0001', 'ABC Trading Co.', 'CUSTOMER', 'CUST-0001', 'WHOLESALE',
       'Mr. Karim Uddin', '+8801711000111', 'karim@abctrading.com', 500000.00, 30,
       true, false, 1, m.id, 0.00, 0.00, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts m WHERE m.account_code = '1103' AND m.organization_id = 1
    ON CONFLICT ON CONSTRAINT uk4jbleouqhrkuju9onykdx0r8x DO NOTHING;

-- ── SUPPLIER sub-account (under Accounts Payable, 2101) ──────────────────────
INSERT INTO acc_chart_of_accounts_sub
(sub_account_code, sub_account_name, sub_account_type, supplier_code, payment_terms,
 contact_person, contact_phone, contact_email, credit_limit, credit_days,
 is_active, requires_approval, organization_id, main_account_id, opening_balance, current_balance,
 created_at, updated_at, created_by, updated_by)
SELECT 'SUPP-0001', 'Global Chemicals Ltd.', 'SUPPLIER', 'SUPP-0001', 'NET_30',
       'Ms. Farida Yasmin', '+8801911000222', 'farida@globalchem.com', 300000.00, 30,
       true, false, 1, m.id, 0.00, 0.00, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts m WHERE m.account_code = '2101' AND m.organization_id = 1
    ON CONFLICT ON CONSTRAINT uk4jbleouqhrkuju9onykdx0r8x DO NOTHING;

-- ── EMPLOYEE sub-account (under Accounts Receivable, 1103 — e.g. salary advances) ──
INSERT INTO acc_chart_of_accounts_sub
(sub_account_code, sub_account_name, sub_account_type, custodian, custodian_phone, custodian_email,
 is_active, requires_approval, organization_id, main_account_id, opening_balance, current_balance,
 created_at, updated_at, created_by, updated_by)
SELECT 'EMP-ADV-0001', 'Employee Advance - Tanvir Ahmed', 'EMPLOYEE', 'Tanvir Ahmed', '+8801710000003', 'tanvir.ahmed@example.com',
       true, true, 1, m.id, 0.00, 0.00, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts m WHERE m.account_code = '1103' AND m.organization_id = 1
    ON CONFLICT ON CONSTRAINT uk4jbleouqhrkuju9onykdx0r8x DO NOTHING;


-- =============================================================================
-- 3. ACCOUNTING PERIODS
-- =============================================================================
INSERT INTO acc_periods
(period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed,
 organization_id, description, created_at, updated_at, created_by, updated_by)
VALUES
    ('FY2025-2026', 'YEARLY', 2025, DATE '2025-07-01', DATE '2026-06-30', true, false, 1, 'Fiscal Year 2025-2026', NOW(), NOW(), 'system', 'system'),
    ('FY2025-2026-JUL', 'MONTHLY', 2025, DATE '2025-07-01', DATE '2025-07-31', true, true,  1, 'July 2025', NOW(), NOW(), 'system', 'system'),
    ('FY2025-2026-AUG', 'MONTHLY', 2025, DATE '2025-08-01', DATE '2025-08-31', true, true,  1, 'August 2025', NOW(), NOW(), 'system', 'system'),
    ('FY2025-2026-JUN', 'MONTHLY', 2026, DATE '2026-06-01', DATE '2026-06-30', true, false, 1, 'June 2026 (current)', NOW(), NOW(), 'system', 'system')
    ON CONFLICT ON CONSTRAINT uk73i7mph9xad8yj4wjl9uvnfts DO NOTHING;


-- =============================================================================
-- 4. OPENING BALANCES  (depends on acc_chart_of_accounts + acc_periods)
-- =============================================================================
INSERT INTO acc_opening_balances
(opening_debit_balance, opening_credit_balance, balance_type, is_posted, is_active, remarks,
 organization_id, account_id, accounting_period_id, posted_by, posted_date,
 created_at, updated_at, created_by, updated_by)
SELECT 500000.00, 0.00, 'DEBIT', true, true, 'Opening cash balance', 1, a.id, p.id, 'system', DATE '2025-07-01',
       NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts a, acc_periods p
WHERE a.account_code = '1101' AND a.organization_id = 1
  AND p.period_name = 'FY2025-2026';

INSERT INTO acc_opening_balances
(opening_debit_balance, opening_credit_balance, balance_type, is_posted, is_active, remarks,
 organization_id, account_id, accounting_period_id, posted_by, posted_date,
 created_at, updated_at, created_by, updated_by)
SELECT 2000000.00, 0.00, 'DEBIT', true, true, 'Opening bank balance', 1, a.id, p.id, 'system', DATE '2025-07-01',
       NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts a, acc_periods p
WHERE a.account_code = '1102' AND a.organization_id = 1
  AND p.period_name = 'FY2025-2026';

INSERT INTO acc_opening_balances
(opening_debit_balance, opening_credit_balance, balance_type, is_posted, is_active, remarks,
 organization_id, account_id, accounting_period_id, posted_by, posted_date,
 created_at, updated_at, created_by, updated_by)
SELECT 0.00, 2500000.00, 'CREDIT', true, true, 'Opening capital balance', 1, a.id, p.id, 'system', DATE '2025-07-01',
       NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts a, acc_periods p
WHERE a.account_code = '3001' AND a.organization_id = 1
  AND p.period_name = 'FY2025-2026';


-- =============================================================================
-- 5. ACCOUNTS MAPPING
-- =============================================================================
INSERT INTO acc_mapping
(mapping_code, mapping_name, module_type, transaction_type, description,
 default_debit_account_id, default_credit_account_id,
 discount_account_id, freight_account_id, rounding_account_id,
 input_vat_account_id, output_vat_account_id, tds_account_id, ait_account_id,
 forex_gain_account_id, forex_loss_account_id,
 default_voucher_type, voucher_type, voucher_prefix,
 auto_post, require_approval, is_active, is_default, is_system,
 use_sub_ledger, update_sub_account_balance, consolidate_entries,
 allow_partial_posting, create_reversing_entry,
 debit_control_type, credit_control_type, default_narration_template,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT
    'MAP-SALES', 'Sales Invoice Mapping', 'SALES', 'SALES_INVOICE', 'Default mapping for sales invoices',
    ar.id, rev.id,
    disc.id, NULL, round.id,
    NULL, vat.id, NULL, NULL,
    NULL, NULL,
    'SALES_VOUCHER', 'SALES_VOUCHER', 'SAL-',
    true, false, true, true, true,
    true, true, false,
    false, true,
    'CUSTOMER', 'GENERAL', 'Sales invoice {voucher_no} dated {voucher_date}',
    1, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts ar, acc_chart_of_accounts rev, acc_chart_of_accounts disc,
     acc_chart_of_accounts round, acc_chart_of_accounts vat
WHERE ar.account_code = '1103' AND ar.organization_id = 1
  AND rev.account_code = '4001' AND rev.organization_id = 1
  AND disc.account_code = '4002' AND disc.organization_id = 1
  AND round.account_code = '5105' AND round.organization_id = 1
  AND vat.account_code = '2102' AND vat.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_mapping_org_code DO NOTHING;

INSERT INTO acc_mapping
(mapping_code, mapping_name, module_type, transaction_type, description,
 default_debit_account_id, default_credit_account_id,
 discount_account_id, freight_account_id, rounding_account_id,
 input_vat_account_id, output_vat_account_id, tds_account_id, ait_account_id,
 forex_gain_account_id, forex_loss_account_id,
 default_voucher_type, voucher_type, voucher_prefix,
 auto_post, require_approval, is_active, is_default, is_system,
 use_sub_ledger, update_sub_account_balance, consolidate_entries,
 allow_partial_posting, create_reversing_entry,
 debit_control_type, credit_control_type, default_narration_template,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT
    'MAP-PURCHASE', 'Purchase Invoice Mapping', 'PURCHASE', 'PURCHASE_INVOICE', 'Default mapping for purchase invoices',
    inv.id, ap.id,
    NULL, freight.id, NULL,
    vat.id, NULL, tds.id, NULL,
    NULL, NULL,
    'PURCHASE_VOUCHER', 'PURCHASE_VOUCHER', 'PUR-',
    true, true, true, true, true,
    true, true, false,
    false, true,
    'GENERAL', 'SUPPLIER', 'Purchase invoice {voucher_no} dated {voucher_date}',
    1, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts inv, acc_chart_of_accounts ap, acc_chart_of_accounts freight,
     acc_chart_of_accounts vat, acc_chart_of_accounts tds
WHERE inv.account_code = '1104' AND inv.organization_id = 1
  AND ap.account_code = '2101' AND ap.organization_id = 1
  AND freight.account_code = '5104' AND freight.organization_id = 1
  AND vat.account_code = '1105' AND vat.organization_id = 1
  AND tds.account_code = '2103' AND tds.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_mapping_org_code DO NOTHING;


-- =============================================================================
-- 6. ACCOUNTS MAPPING DETAILS  (lines for MAP-SALES)
-- =============================================================================
INSERT INTO acc_mapping_details
(organization_id, line_number, sort_order, entry_type, entry_name, line_narration, entry_description,
 amount_type, field_reference, percentage, fixed_amount, formula,
 account_id, accounts_mapping_id, cost_center_id,
 is_active, is_optional, is_tax_entry, skip_if_zero, round_amount, negate_amount, inherit_cost_center,
 created_at, updated_at, created_by, updated_by)
SELECT 1, 1, 1, 'DEBIT', 'Receivable', 'Customer receivable', 'Debit accounts receivable for invoice total',
       'FIELD', 'invoice.totalAmount', NULL, NULL, NULL,
       ar.id, mp.id, NULL,
       true, false, false, true, false, false, false,
       NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts ar, acc_mapping mp
WHERE ar.account_code = '1103' AND ar.organization_id = 1
  AND mp.mapping_code = 'MAP-SALES' AND mp.organization_id = 1;

INSERT INTO acc_mapping_details
(organization_id, line_number, sort_order, entry_type, entry_name, line_narration, entry_description,
 amount_type, field_reference, percentage, fixed_amount, formula,
 account_id, accounts_mapping_id, cost_center_id,
 is_active, is_optional, is_tax_entry, skip_if_zero, round_amount, negate_amount, inherit_cost_center,
 created_at, updated_at, created_by, updated_by)
SELECT 1, 2, 2, 'CREDIT', 'Sales Revenue', 'Sales revenue', 'Credit sales revenue for invoice subtotal',
       'FIELD', 'invoice.subtotal', NULL, NULL, NULL,
       rev.id, mp.id, NULL,
       true, false, false, true, false, false, false,
       NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts rev, acc_mapping mp
WHERE rev.account_code = '4001' AND rev.organization_id = 1
  AND mp.mapping_code = 'MAP-SALES' AND mp.organization_id = 1;

INSERT INTO acc_mapping_details
(organization_id, line_number, sort_order, entry_type, entry_name, line_narration, entry_description,
 amount_type, field_reference, percentage, fixed_amount, formula,
 account_id, accounts_mapping_id, cost_center_id,
 is_active, is_optional, is_tax_entry, skip_if_zero, round_amount, negate_amount, inherit_cost_center,
 created_at, updated_at, created_by, updated_by)
SELECT 1, 3, 3, 'CREDIT', 'Output VAT', 'Output VAT on sale', 'Credit output VAT payable on invoice VAT amount',
       'FIELD', 'invoice.vatAmount', 15.0000, NULL, NULL,
       vat.id, mp.id, NULL,
       true, true, true, true, false, false, false,
       NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts vat, acc_mapping mp
WHERE vat.account_code = '2102' AND vat.organization_id = 1
  AND mp.mapping_code = 'MAP-SALES' AND mp.organization_id = 1;


-- =============================================================================
-- 7. ACCOUNTS POLICY
-- =============================================================================
INSERT INTO acc_policy
(policy_code, policy_name, policy_type, module_type, description,
 default_debit_account_id, default_credit_account_id, accounts_mapping_id,
 voucher_prefix, auto_numbering, number_padding, next_voucher_number,
 allow_backdating, backdating_days, allow_future_dating, allow_negative_amount, allow_zero_amount,
 allow_edit_after_post, allow_reversal, auto_post, require_approval, approval_threshold,
 minimum_amount, maximum_amount, default_narration_template,
 is_active, is_default, is_system, organization_id, created_at, updated_at, created_by, updated_by)
SELECT
    'POL-SALES', 'Sales Voucher Policy', 'VOUCHER_POLICY', 'SALES', 'Policy governing sales voucher posting rules',
    ar.id, rev.id, mp.id,
    'SAL-', true, 6, 1,
    true, 7, false, false, false,
    false, true, true, true, 100000.00,
    0.01, 10000000.00, 'Sales voucher {voucher_no}',
    true, true, true, 1, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts ar, acc_chart_of_accounts rev, acc_mapping mp
WHERE ar.account_code = '1103' AND ar.organization_id = 1
  AND rev.account_code = '4001' AND rev.organization_id = 1
  AND mp.mapping_code = 'MAP-SALES' AND mp.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_policy_org_code DO NOTHING;

INSERT INTO acc_policy
(policy_code, policy_name, policy_type, module_type, description,
 default_debit_account_id, default_credit_account_id, accounts_mapping_id,
 voucher_prefix, auto_numbering, number_padding, next_voucher_number,
 allow_backdating, backdating_days, allow_future_dating, allow_negative_amount, allow_zero_amount,
 allow_edit_after_post, allow_reversal, auto_post, require_approval, approval_threshold,
 minimum_amount, maximum_amount, default_narration_template,
 is_active, is_default, is_system, organization_id, created_at, updated_at, created_by, updated_by)
SELECT
    'POL-PURCHASE', 'Purchase Voucher Policy', 'VOUCHER_POLICY', 'PURCHASE', 'Policy governing purchase voucher posting rules',
    inv.id, ap.id, mp.id,
    'PUR-', true, 6, 1,
    true, 7, false, false, false,
    false, true, false, true, 50000.00,
    0.01, 10000000.00, 'Purchase voucher {voucher_no}',
    true, true, true, 1, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts inv, acc_chart_of_accounts ap, acc_mapping mp
WHERE inv.account_code = '1104' AND inv.organization_id = 1
  AND ap.account_code = '2101' AND ap.organization_id = 1
  AND mp.mapping_code = 'MAP-PURCHASE' AND mp.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_policy_org_code DO NOTHING;

INSERT INTO acc_policy
(policy_code, policy_name, policy_type, module_type, description,
 voucher_prefix, auto_numbering, number_padding, next_voucher_number,
 allow_backdating, backdating_days, allow_future_dating, allow_negative_amount, allow_zero_amount,
 allow_edit_after_post, allow_reversal, auto_post, require_approval,
 minimum_amount, maximum_amount, default_narration_template,
 is_active, is_default, is_system, organization_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('POL-JOURNAL', 'Journal Voucher Policy', 'VOUCHER_POLICY', 'GENERAL', 'Policy governing manual journal entries',
     'JV-', true, 6, 1,
     true, 30, false, false, false,
     false, true, false, true,
     0.01, 10000000.00, 'Journal voucher {voucher_no}',
     true, true, true, 1, NOW(), NOW(), 'system', 'system')
    ON CONFLICT ON CONSTRAINT uq_policy_org_code DO NOTHING;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'COA',             COUNT(*) FROM acc_chart_of_accounts
-- UNION ALL SELECT 'COA Sub',        COUNT(*) FROM acc_chart_of_accounts_sub
-- UNION ALL SELECT 'Periods',        COUNT(*) FROM acc_periods
-- UNION ALL SELECT 'Opening Bal',    COUNT(*) FROM acc_opening_balances
-- UNION ALL SELECT 'Mapping',        COUNT(*) FROM acc_mapping
-- UNION ALL SELECT 'Mapping Detail', COUNT(*) FROM acc_mapping_details
-- UNION ALL SELECT 'Policy',         COUNT(*) FROM acc_policy;