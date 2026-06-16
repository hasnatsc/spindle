-- =============================================================================
--  Optimum ERP — Fixed Asset Seed Data
--  File   : 08_seed_fixed_assets.sql
--  Target : PostgreSQL (fa_asset_categories, fa_assets,
--           fa_depreciation_runs, fa_depreciation_run_lines)
--
--  NOTE: assumes organization_id = 1 and the following already seeded:
--        acc_chart_of_accounts (codes 1200/1201 etc. from earlier script),
--        org_cost_centers, org_departments, org_warehouses, hrm_employees.
--        fa_assets.supplier_id references acc_chart_of_accounts_sub — this
--        looks like a modeling quirk (supplier master lives in COA sub-accounts)
--        so we look up the SUPPLIER-type sub-account seeded earlier (SUPP-0001).
--        fa_asset_disposals not seeded here — it's a transactional table
--        populated when an asset is actually disposed.
--
--  Execution order:
--    1. fa_asset_categories (self-referential parent_id; ROOT -> CHILD)
--    2. fa_assets            (depends on categories, cost centers, depts,
--                              warehouses, employees, COA sub-accounts)
--    3. fa_depreciation_runs (depends on journal entry master — left NULL
--                              until journal posting happens)
--    4. fa_depreciation_run_lines (depends on runs + assets)
--
--  Idempotent via ON CONFLICT DO NOTHING on unique constraints.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. ASSET CATEGORIES
-- =============================================================================

-- ── Top-level categories ──────────────────────────────────────────────────────
INSERT INTO fa_asset_categories
(code, name, description, is_active, default_dep_method, default_dep_rate,
 default_useful_life_years, default_residual_pct,
 gl_asset_account_id, gl_accum_dep_account_id, gl_dep_exp_account_id, gl_disposal_account_id,
 organization_id, parent_id, created_at, updated_at, created_by, updated_by)
SELECT 'FAC-PM', 'Plant & Machinery', 'Manufacturing plant and machinery assets', true,
       'STRAIGHT_LINE', 10.00, 10, 5.00,
       asset.id, NULL, NULL, NULL,
       1, NULL, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts asset
WHERE asset.account_code = '1201' AND asset.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_fac_org_code DO NOTHING;

INSERT INTO fa_asset_categories
(code, name, description, is_active, default_dep_method, default_dep_rate,
 default_useful_life_years, default_residual_pct,
 gl_asset_account_id, gl_accum_dep_account_id, gl_dep_exp_account_id, gl_disposal_account_id,
 organization_id, parent_id, created_at, updated_at, created_by, updated_by)
SELECT 'FAC-FUR', 'Furniture & Fixtures', 'Office furniture and fixtures', true,
       'STRAIGHT_LINE', 10.00, 10, 5.00,
       NULL, NULL, NULL, NULL,
       1, NULL, NOW(), NOW(), 'system', 'system'
    ON CONFLICT ON CONSTRAINT uq_fac_org_code DO NOTHING;

INSERT INTO fa_asset_categories
(code, name, description, is_active, default_dep_method, default_dep_rate,
 default_useful_life_years, default_residual_pct,
 gl_asset_account_id, gl_accum_dep_account_id, gl_dep_exp_account_id, gl_disposal_account_id,
 organization_id, parent_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('FAC-IT', 'IT Equipment', 'Computers, servers, and networking equipment', true,
     'DECLINING_BALANCE', 25.00, 4, 0.00,
     NULL, NULL, NULL, NULL,
     1, NULL, NOW(), NOW(), 'system', 'system'),
    ('FAC-VEH', 'Vehicles', 'Company-owned vehicles', true,
     'STRAIGHT_LINE', 20.00, 5, 10.00,
     NULL, NULL, NULL, NULL,
     1, NULL, NOW(), NOW(), 'system', 'system')
    ON CONFLICT ON CONSTRAINT uq_fac_org_code DO NOTHING;

-- ── Sub-category under Plant & Machinery ──────────────────────────────────────
INSERT INTO fa_asset_categories
(code, name, description, is_active, default_dep_method, default_dep_rate,
 default_useful_life_years, default_residual_pct,
 organization_id, parent_id, created_at, updated_at, created_by, updated_by)
SELECT 'FAC-PM-PROD', 'Production Line Equipment', 'Equipment specific to production lines', true,
       'STRAIGHT_LINE', 10.00, 10, 5.00,
       1, p.id, NOW(), NOW(), 'system', 'system'
FROM fa_asset_categories p WHERE p.code = 'FAC-PM' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_fac_org_code DO NOTHING;

-- ── Sub-category under IT Equipment ───────────────────────────────────────────
INSERT INTO fa_asset_categories
(code, name, description, is_active, default_dep_method, default_dep_rate,
 default_useful_life_years, default_residual_pct,
 organization_id, parent_id, created_at, updated_at, created_by, updated_by)
SELECT 'FAC-IT-LAPTOP', 'Laptops & Desktops', 'End-user computing devices', true,
       'DECLINING_BALANCE', 25.00, 4, 0.00,
       1, p.id, NOW(), NOW(), 'system', 'system'
FROM fa_asset_categories p WHERE p.code = 'FAC-IT' AND p.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_fac_org_code DO NOTHING;


-- =============================================================================
-- 2. FIXED ASSETS
-- =============================================================================

-- ── Asset 1: Production machinery ─────────────────────────────────────────────
INSERT INTO fa_assets
(asset_code, asset_name, description, serial_number, manufacturer, model, barcode,
 acquisition_date, capitalisation_date, depreciation_start_date,
 purchase_cost, installation_cost, residual_value, accumulated_depreciation, current_book_value,
 currency, exchange_rate, depreciation_method, depreciation_rate, useful_life_years,
 status, condition, location, warranty_expiry_date, insurance_policy_no, insurance_expiry_date,
 organization_id, asset_category_id, cost_center_id, department_id, warehouse_id,
 responsible_employee_id, supplier_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'FA-0001', 'CNC Milling Machine', 'High-precision CNC milling machine for production line A',
    'CNC-2025-X4471', 'Haas Automation', 'VF-2SS', NULL,
    DATE '2025-08-15', DATE '2025-09-01', DATE '2025-09-01',
    3500000.00, 150000.00, 182500.00, 0.00, 3650000.00,
    'BDT', 1.0000, 'STRAIGHT_LINE', 10.00, 10,
    'ACTIVE', 'NEW', 'Production Line A, Chattogram Plant', DATE '2027-09-01', 'INS-2025-9981', DATE '2026-09-01',
    1, fc.id, cc.id, dpt.id, wh.id,
    emp.id, NULL,
    NOW(), NOW(), 'system', 'system'
FROM fa_asset_categories fc, org_cost_centers cc, org_departments dpt, org_warehouses wh, hrm_employees emp
WHERE fc.code = 'FAC-PM-PROD' AND fc.organization_id = 1
  AND cc.cost_center_code = 'CC-PRD-A'
  AND dpt.code = 'DEPT-PRD' AND dpt.organization_id = 1
  AND wh.warehouse_code = 'WH-MFG-RM'
  AND emp.employee_code = 'EMP-0003' AND emp.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_fa_org_code DO NOTHING;

-- ── Asset 2: Office furniture ──────────────────────────────────────────────────
INSERT INTO fa_assets
(asset_code, asset_name, description, serial_number, manufacturer, model, barcode,
 acquisition_date, capitalisation_date, depreciation_start_date,
 purchase_cost, installation_cost, residual_value, accumulated_depreciation, current_book_value,
 currency, exchange_rate, depreciation_method, depreciation_rate, useful_life_years,
 status, condition, location,
 organization_id, asset_category_id, cost_center_id, department_id, warehouse_id,
 responsible_employee_id, supplier_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'FA-0002', 'Executive Office Desk Set', 'Executive desk and chair set for MD office',
    NULL, 'Otobi Ltd.', 'Executive-Pro', NULL,
    DATE '2024-03-10', DATE '2024-03-15', DATE '2024-03-15',
    85000.00, 0.00, 8500.00, 15300.00, 61200.00,
    'BDT', 1.0000, 'STRAIGHT_LINE', 10.00, 10,
    'ACTIVE', 'GOOD', 'Head Office, Gulshan, Dhaka',
    1, fc.id, cc.id, dpt.id, NULL,
    emp.id, NULL,
    NOW(), NOW(), 'system', 'system'
FROM fa_asset_categories fc, org_cost_centers cc, org_departments dpt, hrm_employees emp
WHERE fc.code = 'FAC-FUR' AND fc.organization_id = 1
  AND cc.cost_center_code = 'CC-HO'
  AND dpt.code = 'DEPT-MGT' AND dpt.organization_id = 1
  AND emp.employee_code = 'EMP-0001' AND emp.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_fa_org_code DO NOTHING;

-- ── Asset 3: Laptop ────────────────────────────────────────────────────────────
INSERT INTO fa_assets
(asset_code, asset_name, description, serial_number, manufacturer, model, barcode,
 acquisition_date, capitalisation_date, depreciation_start_date,
 purchase_cost, installation_cost, residual_value, accumulated_depreciation, current_book_value,
 currency, exchange_rate, depreciation_method, depreciation_rate, useful_life_years,
 status, condition, location, warranty_expiry_date,
 organization_id, asset_category_id, cost_center_id, department_id, warehouse_id,
 responsible_employee_id, supplier_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'FA-0003', 'Dell Latitude 5440', 'Laptop assigned to Finance Manager',
    'DL5440-77821BD', 'Dell', 'Latitude 5440', NULL,
    DATE '2025-01-20', DATE '2025-01-22', DATE '2025-01-22',
    145000.00, 0.00, 0.00, 39192.71, 105807.29,
    'BDT', 1.0000, 'DECLINING_BALANCE', 25.00, 4,
    'ACTIVE', 'GOOD', 'Head Office, Gulshan, Dhaka', DATE '2028-01-22',
    1, fc.id, cc.id, dpt.id, NULL,
    emp.id, NULL,
    NOW(), NOW(), 'system', 'system'
FROM fa_asset_categories fc, org_cost_centers cc, org_departments dpt, hrm_employees emp
WHERE fc.code = 'FAC-IT-LAPTOP' AND fc.organization_id = 1
  AND cc.cost_center_code = 'CC-FIN'
  AND dpt.code = 'DEPT-FIN' AND dpt.organization_id = 1
  AND emp.employee_code = 'EMP-0002' AND emp.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_fa_org_code DO NOTHING;

-- ── Asset 4: Delivery vehicle ──────────────────────────────────────────────────
INSERT INTO fa_assets
(asset_code, asset_name, description, serial_number, manufacturer, model, barcode,
 acquisition_date, capitalisation_date, depreciation_start_date,
 purchase_cost, installation_cost, residual_value, accumulated_depreciation, current_book_value,
 currency, exchange_rate, depreciation_method, depreciation_rate, useful_life_years,
 status, condition, location, insurance_policy_no, insurance_expiry_date,
 organization_id, asset_category_id, cost_center_id, department_id, warehouse_id,
 responsible_employee_id, supplier_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'FA-0004', 'Hino Delivery Truck', 'Delivery truck for finished goods distribution',
    'HINO-300-88213', 'Hino Motors', '300 Series', NULL,
    DATE '2023-11-05', DATE '2023-11-10', DATE '2023-11-10',
    4200000.00, 80000.00, 428000.00, 855600.00, 2996400.00,
    'BDT', 1.0000, 'STRAIGHT_LINE', 20.00, 5,
    'ACTIVE', 'GOOD', 'Manufacturing Plant, Chattogram', 'INS-2023-5512', DATE '2026-11-10',
    1, fc.id, cc.id, dpt.id, wh.id,
    NULL, NULL,
    NOW(), NOW(), 'system', 'system'
FROM fa_asset_categories fc, org_cost_centers cc, org_departments dpt, org_warehouses wh
WHERE fc.code = 'FAC-VEH' AND fc.organization_id = 1
  AND cc.cost_center_code = 'CC-MFG'
  AND dpt.code = 'DEPT-OPS' AND dpt.organization_id = 1
  AND wh.warehouse_code = 'WH-MFG-FG'
    ON CONFLICT ON CONSTRAINT uq_fa_org_code DO NOTHING;


-- =============================================================================
-- 3. DEPRECIATION RUNS  (journal_entry_id left NULL — populated when posted)
-- =============================================================================
INSERT INTO fa_depreciation_runs
(run_date, run_type, period_start, period_end, status, total_assets, total_depreciation,
 posted_by, posted_at, journal_entry_id, organization_id, created_at, updated_at, created_by, updated_by)
VALUES
    (DATE '2026-05-31', 'MONTHLY', DATE '2026-05-01', DATE '2026-05-31', 'POSTED', 4, 38456.78,
     'system', NOW(), NULL, 1, NOW(), NOW(), 'system', 'system');

INSERT INTO fa_depreciation_runs
(run_date, run_type, period_start, period_end, status, total_assets, total_depreciation,
 posted_by, posted_at, journal_entry_id, organization_id, created_at, updated_at, created_by, updated_by)
VALUES
    (DATE '2026-06-30', 'MONTHLY', DATE '2026-06-01', DATE '2026-06-30', 'DRAFT', 4, 0.00,
     NULL, NULL, NULL, 1, NOW(), NOW(), 'system', 'system');


-- =============================================================================
-- 4. DEPRECIATION RUN LINES  (lines for the POSTED May 2026 run)
-- =============================================================================
INSERT INTO fa_depreciation_run_lines
(depreciation_run_id, asset_id, opening_book_value, depreciation_amount, closing_book_value,
 depreciation_method, rate_applied, units_produced, notes, created_at)
SELECT r.id, a.id, 3679166.67, 29166.67, 3650000.00,
       'STRAIGHT_LINE', 10.00, NULL, 'Monthly straight-line depreciation', NOW()
FROM fa_depreciation_runs r, fa_assets a
WHERE r.run_date = DATE '2026-05-31' AND r.organization_id = 1
  AND a.asset_code = 'FA-0001' AND a.organization_id = 1;

INSERT INTO fa_depreciation_run_lines
(depreciation_run_id, asset_id, opening_book_value, depreciation_amount, closing_book_value,
 depreciation_method, rate_applied, units_produced, notes, created_at)
SELECT r.id, a.id, 61837.50, 637.50, 61200.00,
       'STRAIGHT_LINE', 10.00, NULL, 'Monthly straight-line depreciation', NOW()
FROM fa_depreciation_runs r, fa_assets a
WHERE r.run_date = DATE '2026-05-31' AND r.organization_id = 1
  AND a.asset_code = 'FA-0002' AND a.organization_id = 1;

INSERT INTO fa_depreciation_run_lines
(depreciation_run_id, asset_id, opening_book_value, depreciation_amount, closing_book_value,
 depreciation_method, rate_applied, units_produced, notes, created_at)
SELECT r.id, a.id, 108037.97, 2230.68, 105807.29,
       'DECLINING_BALANCE', 25.00, NULL, 'Monthly declining balance depreciation', NOW()
FROM fa_depreciation_runs r, fa_assets a
WHERE r.run_date = DATE '2026-05-31' AND r.organization_id = 1
  AND a.asset_code = 'FA-0003' AND a.organization_id = 1;

INSERT INTO fa_depreciation_run_lines
(depreciation_run_id, asset_id, opening_book_value, depreciation_amount, closing_book_value,
 depreciation_method, rate_applied, units_produced, notes, created_at)
SELECT r.id, a.id, 3062333.33, 65933.33, 2996400.00,
       'STRAIGHT_LINE', 20.00, NULL, 'Monthly straight-line depreciation', NOW()
FROM fa_depreciation_runs r, fa_assets a
WHERE r.run_date = DATE '2026-05-31' AND r.organization_id = 1
  AND a.asset_code = 'FA-0004' AND a.organization_id = 1;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'Categories',  COUNT(*) FROM fa_asset_categories
-- UNION ALL SELECT 'Assets',      COUNT(*) FROM fa_assets
-- UNION ALL SELECT 'Dep Runs',    COUNT(*) FROM fa_depreciation_runs
-- UNION ALL SELECT 'Dep Lines',   COUNT(*) FROM fa_depreciation_run_lines;