-- =============================================================================
--  Optimum ERP — HRM Master Seed Data (Designations, Employees)
--  File   : 02_seed_hrm_data.sql
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
(designation_code, designation_name, description, grade, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
VALUES

-- Board / Executive
('DESIG-CHAIR',      'Chairman',                  'Board Chairman',                        'E0', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-VC',         'Vice Chairman',             'Vice Chairman',                         'E0', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-MD',         'Managing Director',         'Managing Director',                     'E1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-CEO',        'Chief Executive Officer',   'Chief Executive Officer',               'E1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-COO',        'Chief Operating Officer',   'Chief Operating Officer',               'E1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-CFO',        'Chief Financial Officer',   'Chief Financial Officer',               'E1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-CTO',        'Chief Technology Officer',  'Chief Technology Officer',              'E1', true, 1, NOW(), NOW(), 'system', 'system'),

-- Senior Management
('DESIG-DIR',        'Director',                  'Functional Director',                   'E2', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-GM',         'General Manager',           'Department Head',                       'E2', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-DGM',        'Deputy General Manager',    'Deputy Department Head',                'E3', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-AGM',        'Assistant General Manager', 'Assistant Department Head',             'E4', true, 1, NOW(), NOW(), 'system', 'system'),

-- Management
('DESIG-MGR',        'Manager',                   'Department Manager',                    'M1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-DM',         'Deputy Manager',            'Deputy Manager',                        'M2', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-AM',         'Assistant Manager',         'Assistant Manager',                     'M3', true, 1, NOW(), NOW(), 'system', 'system'),

-- Executive Level
('DESIG-SR-EXE',     'Senior Executive',          'Senior Executive',                      'S1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-EXE',        'Executive',                 'Executive',                             'S2', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-JR-EXE',     'Junior Executive',          'Junior Executive',                      'S3', true, 1, NOW(), NOW(), 'system', 'system'),

-- Officer Level
('DESIG-SR-OFF',     'Senior Officer',            'Senior Officer',                        'O1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-OFF',        'Officer',                   'Officer',                               'O2', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-JR-OFF',     'Junior Officer',            'Junior Officer',                        'O3', true, 1, NOW(), NOW(), 'system', 'system'),

-- Support Staff
('DESIG-SUP',        'Supervisor',                'Operational Supervisor',                'G1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-SR-STAFF',   'Senior Staff',              'Senior Staff Member',                   'G2', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-STAFF',      'Staff',                     'General Staff',                         'G3', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-ASSIST',     'Assistant',                 'Office Assistant',                      'G4', true, 1, NOW(), NOW(), 'system', 'system'),

-- Factory / Operations
('DESIG-FOREMAN',    'Foreman',                   'Production Foreman',                    'W1', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-OPERATOR',   'Operator',                  'Machine Operator',                      'W2', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-TECH',       'Technician',                'Maintenance Technician',                'W2', true, 1, NOW(), NOW(), 'system', 'system'),
('DESIG-WORKER',     'Worker',                    'Production Worker',                     'W3', true, 1, NOW(), NOW(), 'system', 'system')

ON CONFLICT ON CONSTRAINT uq_desig_org_code DO NOTHING;

-- CEO
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date, email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days, basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id, created_at, updated_at, created_by, updated_by)
SELECT 'EMP-0001', 'Rashed', 'Karim', 'MALE', DATE '1978-05-12', DATE '2015-01-10', 'rashed.karim@company.com', '+8801710000001', 'PERMANENT', 'ACTIVE', 20,10,14, 300000, 380000, 1, d.id, ds.id, NULL, NOW(),NOW(),'system','system'
FROM org_departments d  JOIN hrm_designations ds ON ds.designation_code='DESIG-CEO'
WHERE d.code='DEPT-MGT' AND d.organization_id=1 AND ds.organization_id=1 ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- CFO
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date, email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days, basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id, created_at, updated_at, created_by, updated_by)
SELECT  'EMP-0002',  'Nusrat',  'Jahan',  'FEMALE',  DATE '1984-03-22',  DATE '2017-06-01',  'nusrat.jahan@company.com',  '+8801710000002',  'PERMANENT',  'ACTIVE',  18,10,14,  200000,  250000,  1,  d.id,  ds.id,  mgr.id,  NOW(),NOW(),'system','system'
FROM org_departments d  JOIN hrm_designations ds ON ds.designation_code='DESIG-CFO' JOIN hrm_employees mgr ON mgr.employee_code='EMP-0001'
WHERE d.code='DEPT-ACC' ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- HR Manager
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date, email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days, basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id, created_at, updated_at, created_by, updated_by)
SELECT 'EMP-0003', 'Farhana', 'Akter', 'FEMALE', DATE '1988-01-11', DATE '2019-02-01', 'farhana.akter@company.com', '+8801710000003', 'PERMANENT', 'ACTIVE', 18,10,14, 90000, 120000, 1, d.id, ds.id, mgr.id, NOW(),NOW(),'system','system'
FROM org_departments d  JOIN hrm_designations ds ON ds.designation_code='DESIG-MGR' JOIN hrm_employees mgr ON mgr.employee_code='EMP-0001'
WHERE d.code='DEPT-HR' ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- Purchase Manager
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date, email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days, basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id, created_at, updated_at, created_by, updated_by)
SELECT 'EMP-0004', 'Imran', 'Hossain', 'MALE', DATE '1987-08-05', DATE '2020-01-15', 'imran.hossain@company.com', '+8801710000004', 'PERMANENT', 'ACTIVE', 18,10,14, 85000, 110000, 1, d.id, ds.id, mgr.id, NOW(),NOW(),'system','system'
FROM org_departments d JOIN hrm_designations ds ON ds.designation_code='DESIG-MGR' JOIN hrm_employees mgr ON mgr.employee_code='EMP-0001'
WHERE d.code='DEPT-PUR' ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- Inventory Manager
INSERT INTO hrm_employees
(employee_code, first_name, last_name, gender, date_of_birth, joining_date, email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days, basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id, created_at, updated_at, created_by, updated_by)
SELECT  'EMP-0005',  'Tanvir',  'Ahmed',  'MALE',  DATE '1989-11-08',  DATE '2020-05-10',  'tanvir.ahmed@company.com',  '+8801710000005',  'PERMANENT',  'ACTIVE',  18,10,14,  85000,  110000,  1,  d.id,  ds.id,  mgr.id,  NOW(),NOW(),'system','system'
FROM org_departments d  JOIN hrm_designations ds ON ds.designation_code='DESIG-MGR' JOIN hrm_employees mgr ON mgr.employee_code='EMP-0001'
WHERE d.code='DEPT-INV' ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

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