-- =============================================================================
--  Optimum ERP — Organization Setup Seed Data
--  File   : 01_seed_org_setup_data.sql
--  Target : PostgreSQL (org_business_units, org_departments,
--           org_cost_centers, org_warehouses)
--
--  NOTE: org_organizations row already exists via SecurityDataInitializer
--        (assumed id = 1, code = 'SE'). Adjust the lookup below if
--        your initializer uses a different code.
--
--  Execution order:
--    1. org_business_units   (depends on org_organizations)
--    2. org_departments      (self-referential parent_department_id)
--    3. org_cost_centers     (depends on org_business_units, self-ref parent)
--    4. org_warehouses       (depends on org_business_units)
--
--  Idempotent via ON CONFLICT DO NOTHING on unique constraints.
-- =============================================================================

BEGIN;

-- =============================================================================
-- BUSINESS UNITS
-- =============================================================================

INSERT INTO org_business_units
(code, name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT 'BU-HO', 'Head Office', 'Corporate Head Office', true, o.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;

INSERT INTO org_business_units
(code, name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT 'BU-SPN', 'Spinning Unit', 'Yarn Manufacturing Unit', true, o.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;

INSERT INTO org_business_units
(code, name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT 'BU-WHS', 'Central Warehouse', 'Central Raw Material and Finished Goods Warehouse', true, o.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;

INSERT INTO org_business_units
(code, name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT 'BU-SAL', 'Sales & Marketing', 'Domestic and Export Sales Operations', true, o.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;

INSERT INTO org_business_units
(code, name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT 'BU-EXP', 'Export Division', 'Commercial and Export Operations', true, o.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;


-- =============================================================================
-- DEPARTMENTS (TOP LEVEL)
-- =============================================================================

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-MGT', 'Management', 'Executive management department', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-ACC', 'Accounts & Finance', 'Accounts and finance department', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-HR', 'Human Resources', 'Human resource management', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-OPS', 'Operations', 'Overall mill operations', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-SAL', 'Sales & Marketing', 'Domestic and export sales', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-IT', 'Information Technology', 'ERP and IT infrastructure', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

-- =============================================================================
-- OPERATIONS SUB-DEPARTMENTS
-- =============================================================================

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-PUR', 'Purchase', 'Raw material procurement', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_departments p ON p.code = 'DEPT-OPS'
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT'DEPT-INV','Inventory & Store','Warehouse and inventory management',true,o.id,p.id,NOW(), NOW(),'system', 'system'
FROM org_organizations o JOIN org_departments p ON p.code = 'DEPT-OPS'
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-PRD', 'Production', 'Spinning production department', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_departments p ON p.code = 'DEPT-OPS'
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-QA', 'Quality Assurance', 'Quality control and testing', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_departments p ON p.code = 'DEPT-OPS'
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-MNT', 'Maintenance', 'Machine and utility maintenance', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_departments p ON p.code = 'DEPT-OPS'
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-EXP', 'Export & Commercial', 'Commercial and export operations', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_departments p ON p.code = 'DEPT-OPS'
WHERE o.code = 'SE' ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;


-- =============================================================================
-- COST CENTERS
-- =============================================================================

-- HEAD OFFICE
INSERT INTO org_cost_centers
( cost_center_code, cost_center_name, cost_center_type, description, is_active, manager_name, manager_email, organization_id, business_unit_id, parent_cost_center_id, created_at, updated_at, created_by, updated_by)
SELECT 'CC-HO', 'Head Office', 'BRANCH', 'Corporate Head Office Cost Center', true, 'System Admin', 'admin@spindleserp.com', o.id, bu.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu ON bu.organization_id = o.id
WHERE o.code = 'SE' AND bu.code = 'BU-HO' ON CONFLICT (cost_center_code) DO NOTHING;

-- SPINNING UNIT
INSERT INTO org_cost_centers
( cost_center_code, cost_center_name, cost_center_type, description, is_active, manager_name, manager_email, organization_id, business_unit_id, parent_cost_center_id, created_at, updated_at, created_by, updated_by)
SELECT 'CC-SPN', 'Spinning Mill', 'BRANCH', 'Main Yarn Manufacturing Cost Center', true, 'Production Head', 'production@spindleserp.com', o.id, bu.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu ON bu.organization_id = o.id
WHERE o.code = 'SE'  AND bu.code = 'BU-SPN' ON CONFLICT (cost_center_code) DO NOTHING;

-- FINANCE
INSERT INTO org_cost_centers
( cost_center_code, cost_center_name, cost_center_type, description, is_active, manager_name, manager_email, organization_id, business_unit_id, parent_cost_center_id, created_at, updated_at, created_by, updated_by)
SELECT 'CC-ACC', 'Accounts & Finance', 'DEPARTMENT', 'Finance Department Cost Center', true, 'Accounts Manager', 'accounts@spindleserp.com', o.id, bu.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu ON bu.organization_id = o.id JOIN org_cost_centers p ON p.cost_center_code = 'CC-HO'
WHERE o.code = 'SE' AND bu.code = 'BU-HO' ON CONFLICT (cost_center_code) DO NOTHING;

-- HR
INSERT INTO org_cost_centers
( cost_center_code, cost_center_name, cost_center_type, description, is_active, manager_name, manager_email, organization_id, business_unit_id, parent_cost_center_id, created_at, updated_at, created_by, updated_by)
SELECT 'CC-HR', 'Human Resources', 'DEPARTMENT', 'HR Department Cost Center', true, 'HR Manager', 'hr@spindleserp.com', o.id, bu.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu ON bu.organization_id = o.id  JOIN org_cost_centers p ON p.cost_center_code = 'CC-HO'
WHERE o.code = 'SE'  AND bu.code = 'BU-HO' ON CONFLICT (cost_center_code) DO NOTHING;

-- PURCHASE
INSERT INTO org_cost_centers
( cost_center_code, cost_center_name, cost_center_type, description, is_active, manager_name, manager_email, organization_id, business_unit_id, parent_cost_center_id, created_at, updated_at, created_by, updated_by)
SELECT 'CC-PUR', 'Purchase Department', 'DEPARTMENT', 'Purchase Cost Center', true, 'Purchase Manager', 'purchase@spindleserp.com', o.id, bu.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o  JOIN org_business_units bu ON bu.organization_id = o.id JOIN org_cost_centers p ON p.cost_center_code = 'CC-SPN'
WHERE o.code = 'SE'  AND bu.code = 'BU-SPN' ON CONFLICT (cost_center_code) DO NOTHING;

-- PRODUCTION
INSERT INTO org_cost_centers
(  cost_center_code,  cost_center_name,  cost_center_type,  description,  is_active,  manager_name,  manager_email,  organization_id,  business_unit_id,  parent_cost_center_id,  created_at,  updated_at,  created_by,  updated_by)
SELECT 'CC-PRD', 'Production Department', 'DEPARTMENT', 'Production Cost Center', true, 'Production Manager', 'production@spindleserp.com', o.id, bu.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu  ON bu.organization_id = o.id JOIN org_cost_centers p ON p.cost_center_code = 'CC-SPN'
WHERE o.code = 'SE'  AND bu.code = 'BU-SPN' ON CONFLICT (cost_center_code) DO NOTHING;

-- QUALITY
INSERT INTO org_cost_centers
( cost_center_code, cost_center_name, cost_center_type, description, is_active, manager_name, manager_email, organization_id, business_unit_id, parent_cost_center_id, created_at, updated_at, created_by, updated_by)
SELECT 'CC-QA', 'Quality Assurance', 'DEPARTMENT', 'Quality Cost Center', true, 'QA Manager', 'quality@spindleserp.com', o.id, bu.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu ON bu.organization_id = o.id JOIN org_cost_centers p ON p.cost_center_code = 'CC-SPN'
WHERE o.code = 'SE' AND bu.code = 'BU-SPN' ON CONFLICT (cost_center_code) DO NOTHING;


-- =============================================================================
-- WAREHOUSES
-- =============================================================================

-- General Warehouse
INSERT INTO org_warehouses
( warehouse_code, warehouse_name, item_type, address, contact_number, manager_name, is_active, organization_id, business_unit_id, created_at, updated_at, created_by, updated_by)
SELECT 'WH-GEN-01', 'General Warehouse', 'GENERAL', 'Head Office Compound', '+8801700000001', 'Store Manager', true, o.id, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu  ON bu.organization_id = o.id
WHERE o.code = 'SE'  AND bu.code = 'BU-WHS' ON CONFLICT (warehouse_code) DO NOTHING;

-- Raw Material Warehouse
INSERT INTO org_warehouses
(  warehouse_code,  warehouse_name,  item_type,  address,  contact_number,  manager_name,  is_active,  organization_id,  business_unit_id,  created_at,  updated_at,  created_by,  updated_by)
SELECT 'WH-RM-01', 'Raw Material Warehouse', 'RAW_MATERIAL', 'Warehouse Complex', '+8801700000002', 'Store Manager', true, o.id, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu ON bu.organization_id = o.id
WHERE o.code = 'SE'  AND bu.code = 'BU-WHS' ON CONFLICT (warehouse_code) DO NOTHING;

-- Work In Process Warehouse
INSERT INTO org_warehouses
(  warehouse_code,  warehouse_name,  item_type,  address,  contact_number,  manager_name,  is_active,  organization_id,  business_unit_id,  created_at,  updated_at,  created_by,  updated_by)
SELECT 'WH-WIP-01', 'Work In Process Warehouse', 'SEMI_FINISHED', 'Warehouse Complex', '+8801700000003', 'Store Manager', true, o.id, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o  JOIN org_business_units bu  ON bu.organization_id = o.id
WHERE o.code = 'SE' AND bu.code = 'BU-WHS' ON CONFLICT (warehouse_code) DO NOTHING;

-- Finished Goods Warehouse
INSERT INTO org_warehouses
( warehouse_code, warehouse_name, item_type, address, contact_number, manager_name, is_active, organization_id, business_unit_id, created_at, updated_at, created_by, updated_by)
SELECT 'WH-FG-01', 'Finished Goods Warehouse', 'FINISHED_GOOD', 'Warehouse Complex', '+8801700000004', 'Store Manager', true, o.id, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu ON bu.organization_id = o.id
WHERE o.code = 'SE' AND bu.code = 'BU-WHS' ON CONFLICT (warehouse_code) DO NOTHING;

-- Spare Parts Warehouse
INSERT INTO org_warehouses
(  warehouse_code,  warehouse_name,  item_type,  address,  contact_number,  manager_name,  is_active,  organization_id,  business_unit_id,  created_at,  updated_at,  created_by,  updated_by)
SELECT 'WH-SPR-01', 'Spare Parts Warehouse', 'SPARE_PART', 'Warehouse Complex', '+8801700000005', 'Store Manager', true, o.id, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o JOIN org_business_units bu  ON bu.organization_id = o.id
WHERE o.code = 'SE' AND bu.code = 'BU-WHS' ON CONFLICT (warehouse_code) DO NOTHING;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'Business Units', COUNT(*) FROM org_business_units
-- UNION ALL SELECT 'Departments',  COUNT(*) FROM org_departments
-- UNION ALL SELECT 'Cost Centers', COUNT(*) FROM org_cost_centers
-- UNION ALL SELECT 'Warehouses',   COUNT(*) FROM org_warehouses;