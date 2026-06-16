-- =============================================================================
--  Optimum ERP — HRM Master Seed Data (Designations, Employees)
--  File   : 03_seed_hrm_data.sql
--  Target : PostgreSQL (hrm_designations, hrm_employees,
--           hrm_employee_addresses, hrm_employee_salaries)
--
--  Execution order:
--    1. hrm_designations
--    2. hrm_employees (self-referential reporting_manager_id; managers first)
--    3. hrm_employee_addresses
--    4. hrm_employee_salaries
--
--  Idempotent via ON CONFLICT DO NOTHING on unique constraints.
--  Assumes organization_id = 1 and org_departments already seeded with
--  codes referenced below (e.g. via department_code lookup).
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. DESIGNATIONS
-- =============================================================================
INSERT INTO hrm_designations
(designation_code, designation_name, description, grade, is_active, organization_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('DESIG-MD',    'Managing Director',     'Top executive leadership',          'E1', true, 1, NOW(), NOW(), 'system', 'system'),
    ('DESIG-GM',    'General Manager',       'Department head level',             'E2', true, 1, NOW(), NOW(), 'system', 'system'),
    ('DESIG-MGR',   'Manager',                'Mid-level management',              'M1', true, 1, NOW(), NOW(), 'system', 'system'),
    ('DESIG-SR-EXE','Senior Executive',      'Senior individual contributor',     'S1', true, 1, NOW(), NOW(), 'system', 'system'),
    ('DESIG-EXE',   'Executive',              'Individual contributor',            'S2', true, 1, NOW(), NOW(), 'system', 'system'),
    ('DESIG-OFF',   'Officer',                'Entry to mid operational role',     'O1', true, 1, NOW(), NOW(), 'system', 'system'),
    ('DESIG-STAFF', 'Staff',                  'General staff role',                'G1', true, 1, NOW(), NOW(), 'system', 'system')
ON CONFLICT ON CONSTRAINT uq_desig_org_code DO NOTHING;


-- =============================================================================
-- 2. EMPLOYEES
--    Insert top-down so reporting_manager_id can reference already-inserted rows.
--    Replace department lookups with your actual org_departments.department_code.
-- =============================================================================

-- ── Employee 1: Top-level manager (no reporting manager) ────────────────────
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status,
 annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary,
 organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'EMP-0001', 'Rashed', 'Karim', 'MALE', DATE '1978-05-12', DATE '2015-01-10',
    'rashed.karim@example.com', '+8801710000001', 'PERMANENT', 'ACTIVE',
    20, 10, 14,
    250000.00, 320000.00,
    1, d.id, ds.id, NULL,
    NOW(), NOW(), 'system', 'system'
FROM org_departments d, hrm_designations ds
WHERE d.code = 'DEPT-MGT' AND d.organization_id = 1
  AND ds.designation_code = 'DESIG-MD' AND ds.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- ── Employee 2: Department manager (reports to Employee 1) ──────────────────
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status,
 annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary,
 organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'EMP-0002', 'Nusrat', 'Jahan', 'FEMALE', DATE '1985-03-22', DATE '2017-06-01',
    'nusrat.jahan@example.com', '+8801710000002', 'PERMANENT', 'ACTIVE',
    18, 10, 14,
    120000.00, 155000.00,
    1, d.id, ds.id, mgr.id,
    NOW(), NOW(), 'system', 'system'
FROM org_departments d, hrm_designations ds, hrm_employees mgr
WHERE d.code = 'DEPT-FIN' AND d.organization_id = 1
  AND ds.designation_code = 'DESIG-GM' AND ds.organization_id = 1
  AND mgr.employee_code = 'EMP-0001' AND mgr.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- ── Employee 3: Mid-level manager (reports to Employee 2) ───────────────────
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status,
 annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary,
 organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'EMP-0003', 'Tanvir', 'Ahmed', 'MALE', DATE '1990-11-08', DATE '2019-02-15',
    'tanvir.ahmed@example.com', '+8801710000003', 'PERMANENT', 'ACTIVE',
    16, 10, 14,
    85000.00, 110000.00,
    1, d.id, ds.id, mgr.id,
    NOW(), NOW(), 'system', 'system'
FROM org_departments d, hrm_designations ds, hrm_employees mgr
WHERE d.code = 'DEPT-FIN' AND d.organization_id = 1
  AND ds.designation_code = 'DESIG-MGR' AND ds.organization_id = 1
  AND mgr.employee_code = 'EMP-0002' AND mgr.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- ── Employee 4: Executive staff (reports to Employee 3) ─────────────────────
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status,
 annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary,
 organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'EMP-0004', 'Sadia', 'Islam', 'FEMALE', DATE '1995-07-19', DATE '2021-09-01',
    'sadia.islam@example.com', '+8801710000004', 'PERMANENT', 'ACTIVE',
    14, 10, 14,
    45000.00, 58000.00,
    1, d.id, ds.id, mgr.id,
    NOW(), NOW(), 'system', 'system'
FROM org_departments d, hrm_designations ds, hrm_employees mgr
WHERE d.code = 'DEPT-FIN' AND d.organization_id = 1
  AND ds.designation_code = 'DESIG-EXE' AND ds.organization_id = 1
  AND mgr.employee_code = 'EMP-0003' AND mgr.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- ── Employee 5: Contract staff, no reporting manager set yet ────────────────
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status,
 annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary,
 organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'EMP-0005', 'Imran', 'Hossain', 'MALE', DATE '1998-01-30', DATE '2023-04-10',
    'imran.hossain@example.com', '+8801710000005', 'CONTRACT', 'ACTIVE',
    10, 7, 10,
    30000.00, 35000.00,
    1, d.id, ds.id, mgr.id,
    NOW(), NOW(), 'system', 'system'
FROM org_departments d, hrm_designations ds, hrm_employees mgr
WHERE d.code = 'DEPT-WH' AND d.organization_id = 1
  AND ds.designation_code = 'DESIG-STAFF' AND ds.organization_id = 1
  AND mgr.employee_code = 'EMP-0003' AND mgr.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;


-- =============================================================================
-- 3. EMPLOYEE ADDRESSES
-- =============================================================================
INSERT INTO hrm_employee_addresses
(address_line1, address_line2, address_type, city, district, country, postal_code, is_default, employee_id, created_at, created_by)
SELECT 'House 12, Road 5', 'Banani', 'PRESENT', 'Dhaka', 'Dhaka', 'Bangladesh', '1213', true, e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0001' AND e.organization_id = 1;

INSERT INTO hrm_employee_addresses
(address_line1, address_line2, address_type, city, district, country, postal_code, is_default, employee_id, created_at, created_by)
SELECT 'House 7, Road 11', 'Dhanmondi', 'PRESENT', 'Dhaka', 'Dhaka', 'Bangladesh', '1209', true, e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0002' AND e.organization_id = 1;

INSERT INTO hrm_employee_addresses
(address_line1, address_line2, address_type, city, district, country, postal_code, is_default, employee_id, created_at, created_by)
SELECT 'Flat 4B, Green Tower', 'Mirpur', 'PRESENT', 'Dhaka', 'Dhaka', 'Bangladesh', '1216', true, e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0003' AND e.organization_id = 1;

INSERT INTO hrm_employee_addresses
(address_line1, address_line2, address_type, city, district, country, postal_code, is_default, employee_id, created_at, created_by)
SELECT 'House 22, Sector 9', 'Uttara', 'PRESENT', 'Dhaka', 'Dhaka', 'Bangladesh', '1230', true, e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0004' AND e.organization_id = 1;

INSERT INTO hrm_employee_addresses
(address_line1, address_line2, address_type, city, district, country, postal_code, is_default, employee_id, created_at, created_by)
SELECT 'Village Road 3', 'Savar', 'PRESENT', 'Dhaka', 'Dhaka', 'Bangladesh', '1340', true, e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0005' AND e.organization_id = 1;


-- =============================================================================
-- 4. EMPLOYEE SALARIES (current/active salary record per employee)
-- =============================================================================
INSERT INTO hrm_employee_salaries
(basic_salary, gross_salary, house_rent, medical_allowance, transport_allowance, other_allowances,
 provident_fund, income_tax, other_deductions, net_salary,
 effective_date, end_date, is_current, remarks, employee_id, created_at, created_by)
SELECT 250000.00, 320000.00, 50000.00, 10000.00, 10000.00, 0.00,
       12500.00, 15000.00, 0.00, 292500.00,
       DATE '2015-01-10', NULL, true, 'Initial salary record', e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0001' AND e.organization_id = 1;

INSERT INTO hrm_employee_salaries
(basic_salary, gross_salary, house_rent, medical_allowance, transport_allowance, other_allowances,
 provident_fund, income_tax, other_deductions, net_salary,
 effective_date, end_date, is_current, remarks, employee_id, created_at, created_by)
SELECT 120000.00, 155000.00, 25000.00, 5000.00, 5000.00, 0.00,
       6000.00, 7000.00, 0.00, 142000.00,
       DATE '2017-06-01', NULL, true, 'Initial salary record', e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0002' AND e.organization_id = 1;

INSERT INTO hrm_employee_salaries
(basic_salary, gross_salary, house_rent, medical_allowance, transport_allowance, other_allowances,
 provident_fund, income_tax, other_deductions, net_salary,
 effective_date, end_date, is_current, remarks, employee_id, created_at, created_by)
SELECT 85000.00, 110000.00, 18000.00, 4000.00, 3000.00, 0.00,
       4250.00, 4500.00, 0.00, 101250.00,
       DATE '2019-02-15', NULL, true, 'Initial salary record', e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0003' AND e.organization_id = 1;

INSERT INTO hrm_employee_salaries
(basic_salary, gross_salary, house_rent, medical_allowance, transport_allowance, other_allowances,
 provident_fund, income_tax, other_deductions, net_salary,
 effective_date, end_date, is_current, remarks, employee_id, created_at, created_by)
SELECT 45000.00, 58000.00, 9000.00, 2000.00, 2000.00, 0.00,
       2250.00, 2000.00, 0.00, 53750.00,
       DATE '2021-09-01', NULL, true, 'Initial salary record', e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0004' AND e.organization_id = 1;

INSERT INTO hrm_employee_salaries
(basic_salary, gross_salary, house_rent, medical_allowance, transport_allowance, other_allowances,
 provident_fund, income_tax, other_deductions, net_salary,
 effective_date, end_date, is_current, remarks, employee_id, created_at, created_by)
SELECT 30000.00, 35000.00, 4000.00, 1000.00, 0.00, 0.00,
       0.00, 0.00, 0.00, 35000.00,
       DATE '2023-04-10', NULL, true, 'Initial salary record (contract)', e.id, NOW(), 'system'
FROM hrm_employees e WHERE e.employee_code = 'EMP-0005' AND e.organization_id = 1;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'Designations', COUNT(*) FROM hrm_designations
-- UNION ALL SELECT 'Employees',  COUNT(*) FROM hrm_employees
-- UNION ALL SELECT 'Addresses',  COUNT(*) FROM hrm_employee_addresses
-- UNION ALL SELECT 'Salaries',   COUNT(*) FROM hrm_employee_salaries;