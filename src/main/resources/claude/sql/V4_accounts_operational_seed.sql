-- =============================================================================
--  Spindle ERP  —  Accounts Operational Seed  v4.0
--  File   : V4_accounts_operational_seed.sql
--  Target : PostgreSQL + Flyway compatible, idempotent (WHERE NOT EXISTS)
--
--  Prerequisites:
--    V3_chart_of_accounts.sql must be executed first.
--    org_organizations row with id=1 must exist.
--
--  Covers:
--    1. acc_periods          – Fiscal Year 2024-25 (12 monthly + 1 yearly)
--    2. acc_mapping          – One mapping per transaction type (all 30 types)
--    3. acc_mapping_details  – DR/CR lines for each mapping (key mappings expanded)
--    4. acc_policy           – One policy per voucher type (8 types)
--    5. acc_opening_balances – Placeholder opening balances for root COA accounts
--
--  Account code references:
--    All account_id lookups use: (SELECT id FROM acc_chart_of_accounts WHERE account_code='XXXX')
--    This makes the seed independent of auto-generated IDs.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. ACC_PERIODS  —  Fiscal Year 2024-2025 (Bangladesh: July–June)
--    12 monthly periods  +  1 full-year summary period
-- =============================================================================

-- Full Year
INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'FY-2024-25', 'YEARLY', 2024, '2024-07-01', '2025-06-30', TRUE, FALSE,
       'Full Fiscal Year 2024-2025 (July 2024 – June 2025)', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'FY-2024-25');

-- Monthly periods
INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'JUL-2024', 'MONTHLY', 2024, '2024-07-01', '2024-07-31', TRUE, FALSE,
       'July 2024', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'JUL-2024');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'AUG-2024', 'MONTHLY', 2024, '2024-08-01', '2024-08-31', TRUE, FALSE,
       'August 2024', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'AUG-2024');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'SEP-2024', 'MONTHLY', 2024, '2024-09-01', '2024-09-30', TRUE, FALSE,
       'September 2024', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'SEP-2024');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'OCT-2024', 'MONTHLY', 2024, '2024-10-01', '2024-10-31', TRUE, FALSE,
       'October 2024', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'OCT-2024');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'NOV-2024', 'MONTHLY', 2024, '2024-11-01', '2024-11-30', TRUE, FALSE,
       'November 2024', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'NOV-2024');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'DEC-2024', 'MONTHLY', 2024, '2024-12-01', '2024-12-31', TRUE, FALSE,
       'December 2024', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'DEC-2024');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'JAN-2025', 'MONTHLY', 2025, '2025-01-01', '2025-01-31', TRUE, FALSE,
       'January 2025', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'JAN-2025');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'FEB-2025', 'MONTHLY', 2025, '2025-02-01', '2025-02-28', TRUE, FALSE,
       'February 2025', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'FEB-2025');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'MAR-2025', 'MONTHLY', 2025, '2025-03-01', '2025-03-31', TRUE, FALSE,
       'March 2025', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'MAR-2025');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'APR-2025', 'MONTHLY', 2025, '2025-04-01', '2025-04-30', TRUE, FALSE,
       'April 2025', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'APR-2025');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'MAY-2025', 'MONTHLY', 2025, '2025-05-01', '2025-05-31', TRUE, FALSE,
       'May 2025', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'MAY-2025');

INSERT INTO acc_periods (organization_id, period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, description, created_by, created_at, updated_at)
SELECT 1, 'JUN-2025', 'MONTHLY', 2025, '2025-06-01', '2025-06-30', TRUE, FALSE,
       'June 2025', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_periods WHERE period_name = 'JUN-2025');


-- =============================================================================
-- 2. ACC_MAPPING  —  One system mapping per transaction_type
--    All 30 transaction_type enum values covered.
--    Default DR/CR accounts use codes from V3 COA seed.
--    organization_id = 1, idempotent via unique(organization_id, mapping_code).
-- =============================================================================

-- ── PURCHASE ─────────────────────────────────────────────────────────────────

-- PURCHASE_INVOICE
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    input_vat_account_id, discount_account_id, freight_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'PUR_INVOICE', 'Purchase Invoice', 'PURCHASE', 'PURCHASE_INVOICE', 'PURCHASE_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'), -- DR: Raw Materials
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'), -- CR: Accounts Payable
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1151'), -- Input VAT Local
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5130'), -- Purchase Discount
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5140'), -- Freight In
    TRUE, TRUE, TRUE, FALSE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, TRUE,
    'Purchase Invoice #{documentNumber} from {partyName}', 'PV',
    'System mapping for purchase invoices — debits inventory/expense, credits AP', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'PUR_INVOICE');

-- PURCHASE_RETURN  (Debit Note)
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'PUR_RETURN', 'Purchase Return (Debit Note)', 'PURCHASE', 'PURCHASE_RETURN', 'DEBIT_NOTE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'), -- DR: Accounts Payable
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'), -- CR: Raw Materials
    TRUE, TRUE, TRUE, FALSE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, TRUE,
    'Purchase Return #{documentNumber} to {partyName}', 'DN',
    'System mapping for purchase returns — reverses inventory debit, reduces AP', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'PUR_RETURN');

-- PURCHASE_ORDER
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'PUR_ORDER', 'Purchase Order', 'PURCHASE', 'PURCHASE_ORDER', 'JOURNAL_VOUCHER',
    TRUE, FALSE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'Purchase Order #{documentNumber} to {partyName}', 'PO',
    'System mapping for purchase orders — commitment tracking only, no GL impact', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'PUR_ORDER');

-- GRN (Goods Receipt)
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'GOODS_RECEIPT', 'Goods Receipt Note (GRN)', 'PURCHASE', 'GRN', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'), -- DR: Raw Materials
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'), -- CR: Goods Received Not Invoiced (AP)
    TRUE, TRUE, TRUE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    TRUE, FALSE,
    'GRN #{documentNumber} received from {partyName}', 'GRN',
    'System mapping for goods receipt — posts inventory receipt to stock', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'GOODS_RECEIPT');

-- SUPPLIER_PAYMENT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'SUP_PAYMENT', 'Supplier Payment', 'PURCHASE', 'SUPPLIER_PAYMENT', 'PAYMENT_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'), -- DR: Accounts Payable
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'), -- CR: Bank - Main
    TRUE, TRUE, TRUE, FALSE, TRUE,
    FALSE, FALSE, TRUE,
    TRUE, TRUE,
    'Payment to {partyName} against {referenceNumber}', 'PV',
    'System mapping for supplier payments — reduces AP, reduces bank', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'SUP_PAYMENT');

-- ADVANCE_PAYMENT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'ADV_PAYMENT', 'Advance Payment to Supplier', 'PURCHASE', 'ADVANCE_PAYMENT', 'PAYMENT_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1500'), -- DR: Advances to Suppliers
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'), -- CR: Bank - Main
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    TRUE, TRUE,
    'Advance payment to {partyName}', 'APV',
    'System mapping for advance payments to suppliers', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'ADV_PAYMENT');

-- ── SALES ─────────────────────────────────────────────────────────────────────

-- SALES_INVOICE
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    output_vat_account_id, discount_account_id, freight_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'SAL_INVOICE', 'Sales Invoice', 'SALES', 'SALES_INVOICE', 'SALES_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1200'), -- DR: Accounts Receivable
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4110'), -- CR: Sales Revenue
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2311'), -- Output VAT
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4230'), -- Sales Discount
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4330'), -- Freight Out (Revenue)
    TRUE, TRUE, TRUE, FALSE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, TRUE,
    'Sales Invoice #{documentNumber} to {partyName}', 'SV',
    'System mapping for sales invoices — debits AR, credits revenue', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'SAL_INVOICE');

-- SALES_RETURN  (Credit Note)
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'SAL_RETURN', 'Sales Return (Credit Note)', 'SALES', 'SALES_RETURN', 'CREDIT_NOTE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4110'), -- DR: Sales Revenue
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1200'), -- CR: Accounts Receivable
    TRUE, TRUE, TRUE, FALSE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, TRUE,
    'Sales Return #{documentNumber} from {partyName}', 'CN',
    'System mapping for sales returns — reduces revenue, reduces AR', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'SAL_RETURN');

-- SALES_ORDER
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'SAL_ORDER', 'Sales Order', 'SALES', 'SALES_ORDER', 'JOURNAL_VOUCHER',
    TRUE, FALSE, TRUE, FALSE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Sales Order #{documentNumber} for {partyName}', 'SO',
    'System mapping for sales orders — no GL impact, order commitment only', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'SAL_ORDER');

-- DELIVERY_NOTE
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'DEL_NOTE', 'Delivery Note / COGS Entry', 'SALES', 'DELIVERY_NOTE', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5110'), -- DR: COGS
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1330'), -- CR: Finished Goods
    TRUE, TRUE, TRUE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Delivery #{documentNumber} to {partyName}', 'DN',
    'System mapping for delivery notes — records COGS on dispatch', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'DEL_NOTE');

-- CUSTOMER_RECEIPT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'CUST_RECEIPT', 'Customer Receipt', 'SALES', 'CUSTOMER_RECEIPT', 'RECEIPT_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'), -- DR: Bank - Main
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1200'), -- CR: Accounts Receivable
    TRUE, TRUE, TRUE, FALSE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, TRUE,
    'Receipt from {partyName} against {referenceNumber}', 'RV',
    'System mapping for customer receipts — increases bank, reduces AR', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'CUST_RECEIPT');

-- ADVANCE_RECEIPT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'ADV_RECEIPT', 'Advance Receipt from Customer', 'SALES', 'ADVANCE_RECEIPT', 'RECEIPT_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'), -- DR: Bank - Main
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2500'), -- CR: Unearned Revenue
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, FALSE,
    TRUE, TRUE,
    'Advance receipt from {partyName}', 'ARV',
    'System mapping for advance receipts from customers', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'ADV_RECEIPT');

-- ── INVENTORY ─────────────────────────────────────────────────────────────────

-- STOCK_RECEIPT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'STK_RECEIPT', 'Stock Receipt / Adjustment In', 'INVENTORY', 'STOCK_RECEIPT', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'), -- DR: Raw Materials
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '3300'), -- CR: Retained Earnings (adjustment)
    TRUE, TRUE, TRUE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Stock receipt adjustment #{documentNumber}', 'SAJ',
    'System mapping for positive stock adjustments (receipt)', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'STK_RECEIPT');

-- STOCK_ISSUE
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'STK_ISSUE', 'Stock Issue / Write-off', 'INVENTORY', 'STOCK_ISSUE', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5110'), -- DR: COGS / Write-off
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'), -- CR: Raw Materials
    TRUE, TRUE, TRUE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Stock issue #{documentNumber}', 'SIS',
    'System mapping for stock issue or write-off', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'STK_ISSUE');

-- STOCK_TRANSFER
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'STK_TRANSFER', 'Inter-Warehouse Stock Transfer', 'INVENTORY', 'STOCK_TRANSFER', 'JOURNAL_VOUCHER',
    TRUE, TRUE, TRUE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Stock transfer #{documentNumber} from {sourceWarehouse} to {destWarehouse}', 'STR',
    'System mapping for inter-warehouse transfers — balance-neutral inventory move', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'STK_TRANSFER');

-- STOCK_ADJUSTMENT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'STK_ADJUST', 'Stock Adjustment (Net)', 'INVENTORY', 'STOCK_ADJUSTMENT', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'), -- DR: Raw Materials
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '3300'), -- CR: Retained Earnings
    TRUE, TRUE, TRUE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Stock adjustment #{documentNumber}', 'SAJ',
    'System mapping for stock quantity or value adjustments', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'STK_ADJUST');

-- ── PRODUCTION ────────────────────────────────────────────────────────────────

-- PRODUCTION_ISSUE  (Material to WIP)
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'PRD_ISSUE', 'Production Material Issue', 'PRODUCTION', 'PRODUCTION_ISSUE', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1320'), -- DR: Work in Progress
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'), -- CR: Raw Materials
    TRUE, TRUE, TRUE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Material issue for production order #{documentNumber}', 'PIJ',
    'System mapping for production material issue — moves RM to WIP', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'PRD_ISSUE');

-- PRODUCTION_RECEIPT  (WIP to Finished Goods)
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'PRD_RECEIPT', 'Production Receipt (FG)', 'PRODUCTION', 'PRODUCTION_RECEIPT', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1330'), -- DR: Finished Goods
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1320'), -- CR: Work in Progress
    TRUE, TRUE, TRUE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Finished goods receipt for production order #{documentNumber}', 'PRJ',
    'System mapping for completed production — moves WIP to Finished Goods', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'PRD_RECEIPT');

-- ── PAYROLL ───────────────────────────────────────────────────────────────────

-- SALARY_PROCESSING
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    tds_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'SAL_PROCESS', 'Salary Processing', 'PAYROLL', 'SALARY_PROCESSING', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5211'), -- DR: Salaries & Wages
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2210'), -- CR: Salaries Payable
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2230'), -- TDS: Employee Income Tax
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'Payroll processing for {periodName}', 'PLJ',
    'System mapping for monthly payroll — records salary expense and deductions', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'SAL_PROCESS');

-- SALARY_PAYMENT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'SAL_PAYMENT', 'Salary Payment', 'PAYROLL', 'SALARY_PAYMENT', 'PAYMENT_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2210'), -- DR: Salaries Payable
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1116'), -- CR: Bank - Payroll
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'Salary disbursement for {periodName}', 'SPV',
    'System mapping for salary payment — reduces payable, reduces payroll bank', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'SAL_PAYMENT');

-- ── FIXED ASSETS ──────────────────────────────────────────────────────────────

-- ASSET_PURCHASE
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'ASSET_PUR', 'Fixed Asset Purchase', 'FIXED_ASSETS', 'ASSET_PURCHASE', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1630'), -- DR: Machinery & Equipment
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'), -- CR: Accounts Payable
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'Asset purchase #{documentNumber} - {assetName}', 'FAJ',
    'System mapping for fixed asset purchase — capitalises asset, records liability', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'ASSET_PUR');

-- ASSET_DISPOSAL
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'ASSET_DISP', 'Fixed Asset Disposal', 'FIXED_ASSETS', 'ASSET_DISPOSAL', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'), -- DR: Bank (proceeds)
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1630'), -- CR: Machinery (cost)
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'Asset disposal #{documentNumber} - {assetName}', 'FAD',
    'System mapping for fixed asset disposal — removes asset, records gain/loss', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'ASSET_DISP');

-- DEPRECIATION
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'DEPRECIATION', 'Depreciation Run', 'FIXED_ASSETS', 'DEPRECIATION', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5510'), -- DR: Depreciation Expense
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1631'), -- CR: Accum. Depreciation Machinery
    TRUE, TRUE, TRUE, FALSE, FALSE,
    TRUE, FALSE, FALSE,
    FALSE, FALSE,
    'Depreciation for {periodName}', 'DEP',
    'System mapping for monthly depreciation runs', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'DEPRECIATION');

-- ── COMMERCIAL / LC ───────────────────────────────────────────────────────────

-- LC_OPENING
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'LC_OPEN', 'LC Opening / Margin', 'LC_MANAGEMENT', 'LC_OPENING', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1180'), -- DR: LC Margin
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'), -- CR: Bank
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'LC opening margin for LC #{documentNumber}', 'LCJ',
    'System mapping for LC opening — records margin deposit with bank', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'LC_OPEN');

-- LC_AMENDMENT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'LC_AMEND', 'LC Amendment', 'LC_MANAGEMENT', 'LC_AMENDMENT', 'JOURNAL_VOUCHER',
    TRUE, FALSE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'LC amendment for LC #{documentNumber}', 'LCA',
    'System mapping for LC amendments', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'LC_AMEND');

-- LC_PAYMENT
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'LC_PAY', 'LC Settlement Payment', 'LC_MANAGEMENT', 'LC_PAYMENT', 'PAYMENT_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'), -- DR: Accounts Payable
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'), -- CR: Bank
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    TRUE, FALSE,
    'LC payment for LC #{documentNumber}', 'LCP',
    'System mapping for LC settlement payments', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'LC_PAY');

-- ── PETTY CASH ────────────────────────────────────────────────────────────────

-- PETTY_CASH
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'PETTY_CASH', 'Petty Cash Expense', 'CASH_MANAGEMENT', 'PETTY_CASH', 'EXPENSE_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5200'), -- DR: Operating Expenses
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1112'), -- CR: Petty Cash
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Petty cash expense #{documentNumber}', 'PCE',
    'System mapping for petty cash expense recording', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'PETTY_CASH');

-- EXPENSE_CLAIM
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'EXP_CLAIM', 'Employee Expense Claim', 'GENERAL_LEDGER', 'EXPENSE_CLAIM', 'EXPENSE_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5200'), -- DR: Operating Expenses
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2210'), -- CR: Salaries Payable (reimbursable)
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'Expense claim #{documentNumber} by {employeeName}', 'EXP',
    'System mapping for employee expense claims', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'EXP_CLAIM');

-- ── GENERAL LEDGER ────────────────────────────────────────────────────────────

-- JOURNAL_ENTRY
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'JRN_ENTRY', 'General Journal Entry', 'GENERAL_LEDGER', 'JOURNAL_ENTRY', 'JOURNAL_VOUCHER',
    TRUE, TRUE, TRUE, FALSE, TRUE,
    FALSE, FALSE, FALSE,
    FALSE, FALSE,
    'Journal entry #{documentNumber}', 'JV',
    'System mapping for manual journal entries — free-form DR/CR', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'JRN_ENTRY');

-- OPENING_BALANCE
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'OPN_BAL', 'Opening Balance Entry', 'GENERAL_LEDGER', 'OPENING_BALANCE', 'JOURNAL_VOUCHER',
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    TRUE, TRUE,
    'Opening balance entry for {periodName}', 'OBJ',
    'System mapping for opening balance postings at period start', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'OPN_BAL');

-- CLOSING_ENTRY
INSERT INTO acc_mapping (organization_id, mapping_code, mapping_name, module_type, transaction_type, default_voucher_type,
    default_debit_account_id, default_credit_account_id,
    is_active, is_default, is_system, auto_post, allow_partial_posting,
    consolidate_entries, create_reversing_entry, require_approval,
    update_sub_account_balance, use_sub_ledger,
    default_narration_template, voucher_prefix, description, created_by, created_at, updated_at)
SELECT 1, 'CLOSE_ENTRY', 'Year-End Closing Entry', 'GENERAL_LEDGER', 'CLOSING_ENTRY', 'JOURNAL_VOUCHER',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4000'), -- DR: Revenue (close to zero)
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '3310'), -- CR: Current Year Earnings
    TRUE, TRUE, TRUE, FALSE, FALSE,
    FALSE, FALSE, TRUE,
    FALSE, FALSE,
    'Year-end closing entry for {fiscalYear}', 'CEJ',
    'System mapping for year-end revenue and expense closing entries', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_mapping WHERE organization_id = 1 AND mapping_code = 'CLOSE_ENTRY');


-- =============================================================================
-- 3. ACC_MAPPING_DETAILS  —  DR/CR lines for key mappings
--    Covers the 5 most frequently used mappings fully:
--    PURCHASE_INVOICE, SALES_INVOICE, SUPPLIER_PAYMENT, CUSTOMER_RECEIPT, DEPRECIATION
-- =============================================================================

-- ── PURCHASE INVOICE lines ────────────────────────────────────────────────────
-- Line 1: DR Inventory (Raw Materials) — full subtotal
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 1, 1, 'DEBIT', 'Inventory Debit', 'Debit inventory for purchase amount',
    'SUBTOTAL',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    TRUE, 'INVENTORY', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'PUR_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 1);

-- Line 2: DR Input VAT — tax amount
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, tax_type, tax_rate, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 2, 2, 'DEBIT', 'Input VAT', 'Input VAT on purchase (15%)',
    'TAX_ONLY', 'INPUT_VAT', 15.0000,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1151'),
    TRUE, TRUE, TRUE, FALSE, TRUE, TRUE,
    FALSE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'PUR_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 2);

-- Line 3: DR Freight — shipping charges
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 3, 3, 'DEBIT', 'Freight In', 'Freight and delivery charges on purchase',
    'FIELD_VALUE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5140'),
    TRUE, TRUE, FALSE, FALSE, TRUE, TRUE,
    FALSE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'PUR_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 3);

-- Line 4: CR Purchase Discount — contra
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 4, 4, 'CREDIT', 'Purchase Discount', 'Discount received from supplier',
    'FIELD_VALUE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5130'),
    TRUE, TRUE, FALSE, FALSE, TRUE, TRUE,
    FALSE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'PUR_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 4);

-- Line 5: CR Accounts Payable — net payable (balance)
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 5, 5, 'CREDIT', 'Accounts Payable', 'Net payable to supplier',
    'BALANCE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, 'SUPPLIER', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'PUR_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 5);

-- ── SALES INVOICE lines ───────────────────────────────────────────────────────
-- Line 1: DR Accounts Receivable — full balance due
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 1, 1, 'DEBIT', 'Accounts Receivable', 'Amount due from customer including VAT',
    'BALANCE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1200'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, 'CUSTOMER', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SAL_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 1);

-- Line 2: CR Sales Revenue — subtotal
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 2, 2, 'CREDIT', 'Sales Revenue', 'Revenue from goods/services sold',
    'SUBTOTAL',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4110'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    TRUE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SAL_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 2);

-- Line 3: CR Output VAT — 15%
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, tax_type, tax_rate, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 3, 3, 'CREDIT', 'Output VAT', 'VAT collected from customer (15%)',
    'TAX_ONLY', 'OUTPUT_VAT', 15.0000,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2311'),
    TRUE, TRUE, TRUE, FALSE, TRUE, TRUE,
    FALSE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SAL_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 3);

-- Line 4: DR Sales Discount — contra revenue
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 4, 4, 'DEBIT', 'Sales Discount', 'Discount given to customer',
    'FIELD_VALUE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4230'),
    TRUE, TRUE, FALSE, FALSE, TRUE, TRUE,
    FALSE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SAL_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 4);

-- Line 5: CR Freight/Delivery — shipping revenue
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 5, 5, 'CREDIT', 'Freight Revenue', 'Delivery and shipping charges billed',
    'FIELD_VALUE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4330'),
    TRUE, TRUE, FALSE, FALSE, TRUE, TRUE,
    FALSE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SAL_INVOICE' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 5);

-- ── SUPPLIER PAYMENT lines ────────────────────────────────────────────────────
-- Line 1: DR Accounts Payable
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 1, 1, 'DEBIT', 'Accounts Payable', 'Reduce supplier outstanding balance',
    'FULL_AMOUNT',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, 'SUPPLIER', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SUP_PAYMENT' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 1);

-- Line 2: CR Bank Account
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 2, 2, 'CREDIT', 'Bank Account', 'Reduce bank balance on payment',
    'FULL_AMOUNT',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, 'BANK', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SUP_PAYMENT' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 2);

-- ── CUSTOMER RECEIPT lines ────────────────────────────────────────────────────
-- Line 1: DR Bank Account
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 1, 1, 'DEBIT', 'Bank Account', 'Increase bank balance on customer receipt',
    'FULL_AMOUNT',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, 'BANK', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'CUST_RECEIPT' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 1);

-- Line 2: CR Accounts Receivable
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 2, 2, 'CREDIT', 'Accounts Receivable', 'Reduce customer outstanding balance',
    'FULL_AMOUNT',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1200'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, 'CUSTOMER', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'CUST_RECEIPT' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 2);

-- ── DEPRECIATION lines ────────────────────────────────────────────────────────
-- Line 1: DR Depreciation Expense
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 1, 1, 'DEBIT', 'Depreciation Expense', 'Monthly depreciation charge',
    'FORMULA',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5510'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    TRUE, 'ASSET', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'DEPRECIATION' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 1);

-- Line 2: CR Accumulated Depreciation
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 2, 2, 'CREDIT', 'Accumulated Depreciation', 'Accumulated depreciation on asset',
    'FORMULA',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1631'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, 'ASSET', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'DEPRECIATION' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 2);

-- ── SALARY PROCESSING lines ───────────────────────────────────────────────────
-- Line 1: DR Salaries & Wages
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 1, 1, 'DEBIT', 'Salaries & Wages', 'Gross salary expense',
    'FULL_AMOUNT',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5211'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    TRUE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SAL_PROCESS' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 1);

-- Line 2: CR Employee Tax Withheld
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, tax_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 2, 2, 'CREDIT', 'Employee Tax Withheld', 'Income tax deducted from salary',
    'FIELD_VALUE', 'INCOME_TAX',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2230'),
    TRUE, TRUE, TRUE, FALSE, TRUE, TRUE,
    FALSE, 'NONE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SAL_PROCESS' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 2);

-- Line 3: CR Salaries Payable (net)
INSERT INTO acc_mapping_details (accounts_mapping_id, line_number, sort_order, entry_type, entry_name, entry_description,
    amount_type, account_id, is_active, is_optional, is_tax_entry, negate_amount, round_amount, skip_if_zero,
    inherit_cost_center, control_account_type, created_by, created_at, updated_at)
SELECT m.id, 3, 3, 'CREDIT', 'Net Salaries Payable', 'Net salary payable after deductions',
    'BALANCE',
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2210'),
    TRUE, FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, 'EMPLOYEE', 'SYSTEM', NOW(), NOW()
FROM acc_mapping m WHERE m.mapping_code = 'SAL_PROCESS' AND m.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM acc_mapping_details d WHERE d.accounts_mapping_id = m.id AND d.line_number = 3);


-- =============================================================================
-- 4. ACC_POLICY  —  One system policy per voucher type (all 8 types)
-- =============================================================================

-- JOURNAL_VOUCHER policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_JV', 'Journal Voucher Policy', 'JOURNAL_VOUCHER', 'GENERAL_LEDGER',
    TRUE, TRUE, TRUE,
    TRUE, 30, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, FALSE,
    FALSE, TRUE,
    TRUE, 'JV', 6, 'FISCAL_YEAR', 1,
    FALSE, FALSE, FALSE, FALSE,
    1, 0,
    'Journal Entry #{voucherNumber} dated {voucherDate}',
    'Standard system policy for all manual journal vouchers', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_JV');

-- PAYMENT_VOUCHER policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_PV', 'Payment Voucher Policy', 'PAYMENT_VOUCHER', 'ACCOUNTS_PAYABLE',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    FALSE, TRUE,
    TRUE, 'PV', 6, 'FISCAL_YEAR', 1,
    TRUE, FALSE, TRUE, TRUE,
    1, 50000,
    'Payment #{voucherNumber} to {partyName} dated {voucherDate}',
    'Standard system policy for all supplier payment vouchers', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_PV');

-- RECEIPT_VOUCHER policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_RV', 'Receipt Voucher Policy', 'RECEIPT_VOUCHER', 'ACCOUNTS_RECEIVABLE',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    FALSE, TRUE,
    TRUE, 'RV', 6, 'FISCAL_YEAR', 1,
    FALSE, FALSE, FALSE, FALSE,
    1, 0,
    'Receipt #{voucherNumber} from {partyName} dated {voucherDate}',
    'Standard system policy for all customer receipt vouchers', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_RV');

-- CONTRA_VOUCHER policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_CV', 'Contra Voucher Policy', 'CONTRA_VOUCHER', 'CASH_MANAGEMENT',
    TRUE, TRUE, TRUE,
    TRUE, 3, FALSE, 0,
    FALSE, FALSE, FALSE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, FALSE,
    FALSE, TRUE,
    TRUE, 'CV', 6, 'FISCAL_YEAR', 1,
    TRUE, FALSE, FALSE, FALSE,
    1, 100000,
    'Contra #{voucherNumber} dated {voucherDate}',
    'Policy for inter-account and bank-to-cash contra entries', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_CV');

-- PURCHASE_VOUCHER policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_PURV', 'Purchase Voucher Policy', 'PURCHASE_VOUCHER', 'PURCHASE',
    TRUE, TRUE, TRUE,
    TRUE, 15, FALSE, 0,
    TRUE, FALSE, FALSE, TRUE, TRUE,
    FALSE, FALSE, FALSE,
    TRUE, 5, TRUE, TRUE,
    TRUE, TRUE,
    TRUE, 'PURV', 6, 'FISCAL_YEAR', 1,
    TRUE, FALSE, TRUE, TRUE,
    2, 100000,
    'Purchase Invoice #{voucherNumber} from {partyName}',
    'Policy for purchase invoices and AP vouchers', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_PURV');

-- SALES_VOUCHER policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_SALV', 'Sales Voucher Policy', 'SALES_VOUCHER', 'SALES',
    TRUE, TRUE, TRUE,
    TRUE, 7, TRUE, 3,
    TRUE, FALSE, FALSE, TRUE, FALSE,
    FALSE, FALSE, FALSE,
    TRUE, 5, TRUE, TRUE,
    TRUE, TRUE,
    TRUE, 'SALV', 6, 'FISCAL_YEAR', 1,
    FALSE, TRUE, FALSE, FALSE,
    1, 0,
    'Sales Invoice #{voucherNumber} to {partyName}',
    'Policy for sales invoices and AR vouchers', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_SALV');

-- DEBIT_NOTE policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_DN', 'Debit Note Policy', 'DEBIT_NOTE', 'PURCHASE',
    TRUE, TRUE, TRUE,
    TRUE, 30, FALSE, 0,
    TRUE, FALSE, FALSE, FALSE, FALSE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    FALSE, TRUE,
    TRUE, 'DN', 6, 'FISCAL_YEAR', 1,
    TRUE, FALSE, TRUE, TRUE,
    1, 0,
    'Debit Note #{voucherNumber} to {partyName}',
    'Policy for purchase return debit notes', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_DN');

-- CREDIT_NOTE policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_CN', 'Credit Note Policy', 'CREDIT_NOTE', 'SALES',
    TRUE, TRUE, TRUE,
    TRUE, 30, FALSE, 0,
    TRUE, FALSE, FALSE, FALSE, FALSE,
    FALSE, FALSE, FALSE,
    TRUE, 10, TRUE, TRUE,
    FALSE, TRUE,
    TRUE, 'CN', 6, 'FISCAL_YEAR', 1,
    TRUE, FALSE, TRUE, TRUE,
    1, 0,
    'Credit Note #{voucherNumber} to {partyName}',
    'Policy for sales return credit notes', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_CN');

-- EXPENSE_VOUCHER policy
INSERT INTO acc_policy (organization_id, policy_code, policy_name, policy_type, module_type,
    is_active, is_default, is_system,
    allow_backdating, backdating_days, allow_future_dating, future_dating_days,
    allow_edit, allow_edit_after_post, allow_delete, allow_reversal, allow_reversal_approval,
    allow_negative_amount, allow_zero_amount, allow_direct_post,
    require_narration, min_narration_length, require_balanced_entry, require_reference,
    require_cost_center, restrict_to_open_period,
    auto_numbering, voucher_prefix, number_padding, numbering_reset, next_voucher_number,
    require_approval, auto_post, auto_post_on_approval, post_on_approval,
    apr_levels, approval_threshold,
    default_narration_template, description, created_by, created_at, updated_at)
SELECT 1, 'POL_EXP', 'Expense Voucher Policy', 'EXPENSE_VOUCHER', 'GENERAL_LEDGER',
    TRUE, TRUE, TRUE,
    TRUE, 7, FALSE, 0,
    TRUE, FALSE, FALSE, FALSE, FALSE,
    FALSE, FALSE, FALSE,
    TRUE, 5, TRUE, FALSE,
    TRUE, TRUE,
    TRUE, 'EXP', 6, 'FISCAL_YEAR', 1,
    TRUE, FALSE, TRUE, TRUE,
    1, 10000,
    'Expense #{voucherNumber} dated {voucherDate}',
    'Policy for expense and petty cash vouchers', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM acc_policy WHERE organization_id = 1 AND policy_code = 'POL_EXP');


-- =============================================================================
-- 5. ACC_OPENING_BALANCES  —  Zero opening balances for root-level COA accounts
--    Period: FY-2024-25 (full year period)
--    All accounts start at 0 — accountants will update actual figures.
--    Covers all 5 root accounts (1000/2000/3000/4000/5000) + key control accounts.
-- =============================================================================

-- Root: ASSETS (1000)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1000'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance for Assets root account — FY 2024-25', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1000')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Cash on Hand (1111)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1111'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Cash on Hand', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1111')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Petty Cash (1112)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1112'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Petty Cash', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1112')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Bank Main (1115)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Bank Account Main', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1115')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Accounts Receivable (1200)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1200'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Accounts Receivable control', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1200')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Raw Materials (1310)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Raw Materials inventory', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1310')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Finished Goods (1330)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1330'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Finished Goods inventory', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '1330')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Root: LIABILITIES (2000)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2000'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Liabilities root account', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2000')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Accounts Payable (2110)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Accounts Payable control', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2110')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Salaries Payable (2210)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2210'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Salaries Payable', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '2210')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Root: EQUITY (3000)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '3000'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Equity root account', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '3000')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Retained Earnings (3300)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '3300'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Retained Earnings', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '3300')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Root: REVENUE (4000)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4000'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Revenue root account (income statement reset)', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '4000')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);

-- Root: EXPENSES (5000)
INSERT INTO acc_opening_balances (organization_id, account_id, accounting_period_id, sub_account_id,
    opening_debit_balance, opening_credit_balance, is_posted, is_active,
    balance_type, remarks, created_by, created_at, updated_at)
SELECT 1,
    (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5000'),
    (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1),
    NULL,
    0, 0, FALSE, TRUE,
    'OPENING', 'Opening balance — Expenses root account (income statement reset)', 'SYSTEM', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM acc_opening_balances ob
    WHERE ob.organization_id = 1
      AND ob.account_id = (SELECT id FROM acc_chart_of_accounts WHERE account_code = '5000')
      AND ob.accounting_period_id = (SELECT id FROM acc_periods WHERE period_name = 'FY-2024-25' AND organization_id = 1)
      AND ob.sub_account_id IS NULL
);


-- =============================================================================
-- VERIFICATION (uncomment to run)
-- =============================================================================
-- SELECT 'Periods'         AS tbl, COUNT(*) FROM acc_periods         WHERE organization_id = 1
-- UNION ALL SELECT 'Mappings',      COUNT(*) FROM acc_mapping         WHERE organization_id = 1
-- UNION ALL SELECT 'Mapping Lines', COUNT(*) FROM acc_mapping_details d
--           JOIN acc_mapping m ON m.id = d.accounts_mapping_id      WHERE m.organization_id = 1
-- UNION ALL SELECT 'Policies',      COUNT(*) FROM acc_policy          WHERE organization_id = 1
-- UNION ALL SELECT 'Opening Bals',  COUNT(*) FROM acc_opening_balances WHERE organization_id = 1;

COMMIT;
