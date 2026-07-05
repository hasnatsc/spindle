-- =============================================================================
--  Spindle ERP — Approval Workflow Seed Data  (FIXED)
--  File   : 07_seed_approval_configs.sql
--  Target : PostgreSQL (apr_configs, apr_levels, apr_delegations)
--
--  FIXES vs previous version:
--    1. apr_levels was missing NOT NULL organization_id → every level insert
--       failed. organization_id = 1 now supplied on all levels.
--    2. Approver lookups switched from INNER JOIN on sec_users to scalar
--       subqueries: if a username doesn't exist yet, the level row is still
--       created with approver_user_id = NULL (resolved later in the UI)
--       instead of silently inserting zero rows.
--    3. apr_levels has no unique constraint → NOT EXISTS guards added so the
--       file is safe to re-run.
--    4. apr_delegations kept as a JOIN (delegator/delegate are NOT NULL FKs);
--       the row is simply skipped if either user is absent.
--
--  Assumes organization_id = 1 and sec_users seeded with the referenced
--  usernames (purchase.manager, gm, sales.manager, accounts.admin,
--  accountant, hrm.manager, commercial.manager, inventory.manager).
--
--  Idempotent: ON CONFLICT on apr_configs/apr_delegations, NOT EXISTS on levels.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. APPROVAL CONFIGS
-- =============================================================================
INSERT INTO apr_configs
(code, name, description, module, document_type, flow_type,
 min_amount, max_amount, priority,
 use_reporting_hierarchy, enable_reminders, reminder_interval_hours, auto_escalation_hours,
 is_active, organization_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('APR-PO',       'Purchase Order Approval',   'Approval flow for purchase orders above threshold',  'PURCHASE_SUPPLIER',         'PURCHASE_ORDER',    'SEQUENTIAL', 50000.00,  NULL, 10, true,  true, 24, 72, true, 1, NOW(), NOW(), 'system', 'system'),
    ('APR-SO',       'Sales Order Approval',      'Approval flow for sales orders above threshold',     'SALES_CUSTOMER_OPERATIONS', 'SALES_ORDER',       'SEQUENTIAL', 100000.00, NULL, 10, true,  true, 24, 72, true, 1, NOW(), NOW(), 'system', 'system'),
    ('APR-JV',       'Journal Voucher Approval',  'Approval flow for manual journal vouchers',          'FINANCE_ACCOUNTS',          'JOURNAL_VOUCHER',   'SEQUENTIAL', 0.00,      NULL, 20, true,  true, 12, 48, true, 1, NOW(), NOW(), 'system', 'system'),
    ('APR-PAYMENT',  'Payment Voucher Approval',  'Approval flow for payment vouchers',                 'FINANCE_ACCOUNTS',          'PAYMENT_VOUCHER',   'SEQUENTIAL', 20000.00,  NULL, 15, true,  true, 12, 48, true, 1, NOW(), NOW(), 'system', 'system'),
    ('APR-LEAVE',    'Leave Application Approval','Approval flow for employee leave requests',          'HRM',                       'LEAVE_APPLICATION', 'SEQUENTIAL', NULL,      NULL, 30, true,  true, 24, 96, true, 1, NOW(), NOW(), 'system', 'system'),
    ('APR-PAYROLL',  'Payroll Approval',          'Approval flow for payroll run processing',           'HRM',                       'PAYROLL_RUN',       'SEQUENTIAL', NULL,      NULL, 5,  false, true, 12, 48, true, 1, NOW(), NOW(), 'system', 'system'),
    ('APR-LC',       'Letter of Credit Approval', 'Approval flow for LC opening/amendment',             'COMMERCIAL',                'LETTER_OF_CREDIT',  'SEQUENTIAL', 500000.00, NULL, 1,  false, true, 6,  24, true, 1, NOW(), NOW(), 'system', 'system'),
    ('APR-STOCK-ADJ','Stock Adjustment Approval', 'Approval flow for inventory stock adjustments',      'INVENTORY_WAREHOUSE',       'STOCK_ADJUSTMENT',  'SEQUENTIAL', NULL,      NULL, 25, true,  true, 24, 72, true, 1, NOW(), NOW(), 'system', 'system')
ON CONFLICT ON CONSTRAINT uq_aprc_code DO NOTHING;


-- =============================================================================
-- 2. APPROVAL LEVELS  (organization_id = 1 on every row; approver via scalar
--    subquery so missing users don't drop the level)
-- =============================================================================

-- ── APR-PO: 2-level approval (Purchase Manager -> GM) ────────────────────────
INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 1, 'Purchase Manager Review', 'Purchase Manager approval', c.id,
       (SELECT id FROM sec_users WHERE username = 'purchase.manager'),
       true, true, true, true, 'First level review by purchase manager', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-PO' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 1);

INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 2, 'GM Final Approval', 'General Manager final sign-off', c.id,
       (SELECT id FROM sec_users WHERE username = 'gm'),
       false, false, true, true, 'Final approval by GM for high-value POs', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-PO' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 2);

-- ── APR-SO: 1-level approval (Sales Manager) ─────────────────────────────────
INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 1, 'Sales Manager Approval', 'Sales Manager sign-off', c.id,
       (SELECT id FROM sec_users WHERE username = 'sales.manager'),
       true, false, true, true, 'Single level approval for sales orders', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-SO' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 1);

-- ── APR-JV: 1-level approval (Accounts Admin) ────────────────────────────────
INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 1, 'Accounts Admin Review', 'Accounts Admin approval of journal voucher', c.id,
       (SELECT id FROM sec_users WHERE username = 'accounts.admin'),
       true, false, true, false, 'Journal vouchers reviewed by accounts admin', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-JV' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 1);

-- ── APR-PAYMENT: 2-level approval (Accountant -> Accounts Admin) ─────────────
INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 1, 'Accountant Review', 'Initial accountant review', c.id,
       (SELECT id FROM sec_users WHERE username = 'accountant'),
       true, true, true, true, 'Initial review by accountant', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-PAYMENT' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 1);

INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 2, 'Accounts Admin Approval', 'Final approval by accounts admin', c.id,
       (SELECT id FROM sec_users WHERE username = 'accounts.admin'),
       false, false, true, false, 'Final sign-off before disbursement', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-PAYMENT' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 2);

-- ── APR-LEAVE: 1-level approval (reporting manager, resolved dynamically) ────
INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 1, 'Reporting Manager Approval', 'Direct reporting manager approves leave', c.id, NULL,
       false, true, true, true, 'Resolved dynamically via reporting hierarchy', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-LEAVE' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 1);

-- ── APR-PAYROLL: 1-level approval (HRM Manager) ──────────────────────────────
INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 1, 'HRM Manager Approval', 'HRM manager sign-off before disbursement', c.id,
       (SELECT id FROM sec_users WHERE username = 'hrm.manager'),
       false, false, true, false, 'Single level approval for payroll runs', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-PAYROLL' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 1);

-- ── APR-LC: 1-level approval (Commercial Manager) ────────────────────────────
INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 1, 'Commercial Manager Approval', 'Commercial manager approves LC opening', c.id,
       (SELECT id FROM sec_users WHERE username = 'commercial.manager'),
       true, true, true, false, 'High-value LC requires commercial manager sign-off', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-LC' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 1);

-- ── APR-STOCK-ADJ: 1-level approval (Inventory Manager) ──────────────────────
INSERT INTO apr_levels
(level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT 1, 'Inventory Manager Approval', 'Inventory manager approves stock adjustments', c.id,
       (SELECT id FROM sec_users WHERE username = 'inventory.manager'),
       false, false, true, true, 'Stock adjustments require inventory manager sign-off', true,
       1, NOW(), NOW(), 'system', 'system'
FROM apr_configs c
WHERE c.code = 'APR-STOCK-ADJ' AND c.organization_id = 1
  AND NOT EXISTS (SELECT 1 FROM apr_levels l WHERE l.approval_config_id = c.id AND l.level_number = 1);


-- =============================================================================
-- 3. APPROVAL DELEGATIONS  (skipped if either user is missing — both FKs are
--    NOT NULL so a scalar-subquery NULL would be invalid here)
-- =============================================================================
INSERT INTO apr_delegations
(delegation_code, delegator_id, delegate_id, module, document_type,
 start_date, end_date, max_amount, reason, status, is_active, notify_delegator,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT
    'DEL-2026-0001', d.id, dl.id, 'PURCHASE_SUPPLIER', 'PURCHASE_ORDER',
    DATE '2026-06-20', DATE '2026-06-30', 200000.00,
    'GM on annual leave — delegating PO approval authority', 'ACTIVE', true, true,
    1, NOW(), NOW(), 'system', 'system'
FROM sec_users d, sec_users dl
WHERE d.username = 'gm' AND dl.username = 'purchase.manager'
ON CONFLICT ON CONSTRAINT uq_aprd_code DO NOTHING;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'Configs',     COUNT(*) FROM apr_configs
-- UNION ALL SELECT 'Levels',      COUNT(*) FROM apr_levels
-- UNION ALL SELECT 'Delegations', COUNT(*) FROM apr_delegations;
