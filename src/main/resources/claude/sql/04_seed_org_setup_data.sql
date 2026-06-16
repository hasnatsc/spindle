-- =============================================================================
--  Optimum ERP — Organization Setup Seed Data
--  File   : 04_seed_org_setup_data.sql
--  Target : PostgreSQL (org_business_units, org_departments,
--           org_cost_centers, org_warehouses)
--
--  NOTE: org_organizations row already exists via SecurityDataInitializer
--        (assumed id = 1, code = 'ORG-001'). Adjust the lookup below if
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
-- 1. BUSINESS UNITS  (depends on existing organization)
-- =============================================================================
INSERT INTO org_business_units
(code, name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT 'BU-HO',  'Head Office',        'Corporate head office',           true, o.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o WHERE o.code = 'ORG-001'
    ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;

INSERT INTO org_business_units
(code, name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT 'BU-MFG', 'Manufacturing Unit', 'Production / manufacturing plant', true, o.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o WHERE o.code = 'ORG-001'
    ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;

INSERT INTO org_business_units
(code, name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT 'BU-TRD', 'Trading Division',   'Import/export trading division',  true, o.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o WHERE o.code = 'ORG-001'
    ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;


-- =============================================================================
-- 2. DEPARTMENTS  (top-level first, then children referencing parent)
-- =============================================================================

-- top-level departments (parent_department_id = NULL)
INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-MGT', 'Management',  'Executive management', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o WHERE o.code = 'ORG-001'
    ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-FIN', 'Finance & Accounts', 'Finance and accounts department', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o WHERE o.code = 'ORG-001'
    ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-OPS', 'Operations', 'Overall operations department', true, o.id, NULL, NOW(), NOW(), 'system', 'system'
FROM org_organizations o WHERE o.code = 'ORG-001'
    ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

-- child departments (parent = DEPT-OPS)
INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-WH', 'Warehouse & Inventory', 'Warehouse and inventory operations', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o, org_departments p
WHERE o.code = 'ORG-001' AND p.code = 'DEPT-OPS' AND p.organization_id = o.id
    ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-PRD', 'Production', 'Manufacturing/production department', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o, org_departments p
WHERE o.code = 'ORG-001' AND p.code = 'DEPT-OPS' AND p.organization_id = o.id
    ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-PUR', 'Procurement', 'Purchasing department', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o, org_departments p
WHERE o.code = 'ORG-001' AND p.code = 'DEPT-OPS' AND p.organization_id = o.id
    ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;

INSERT INTO org_departments
(code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT 'DEPT-SAL', 'Sales & Marketing', 'Sales and marketing department', true, o.id, p.id, NOW(), NOW(), 'system', 'system'
FROM org_organizations o, org_departments p
WHERE o.code = 'ORG-001' AND p.code = 'DEPT-OPS' AND p.organization_id = o.id
    ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a DO NOTHING;


-- =============================================================================
-- 3. COST CENTERS  (depends on org_business_units; self-referential parent)
-- =============================================================================

-- top-level cost centers
INSERT INTO org_cost_centers
(cost_center_code, cost_center_name, cost_center_type, description, is_active,
 manager_name, manager_email, business_unit_id, parent_cost_center_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'CC-HO', 'Head Office', 'BRANCH', 'Head office cost center', true,
       'Rashed Karim', 'rashed.karim@example.com', bu.id, NULL,
       NOW(), NOW(), 'system', 'system'
FROM org_business_units bu WHERE bu.code = 'BU-HO'
    ON CONFLICT ON CONSTRAINT ukc7mv2nlnq1omcvcalytdltgyr DO NOTHING;

INSERT INTO org_cost_centers
(cost_center_code, cost_center_name, cost_center_type, description, is_active,
 manager_name, manager_email, business_unit_id, parent_cost_center_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'CC-MFG', 'Manufacturing Plant', 'BRANCH', 'Manufacturing plant cost center', true,
       'Tanvir Ahmed', 'tanvir.ahmed@example.com', bu.id, NULL,
       NOW(), NOW(), 'system', 'system'
FROM org_business_units bu WHERE bu.code = 'BU-MFG'
    ON CONFLICT ON CONSTRAINT ukc7mv2nlnq1omcvcalytdltgyr DO NOTHING;

-- child cost center under Head Office (department-level)
INSERT INTO org_cost_centers
(cost_center_code, cost_center_name, cost_center_type, description, is_active,
 manager_name, manager_email, business_unit_id, parent_cost_center_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'CC-FIN', 'Finance Department', 'DEPARTMENT', 'Finance dept cost center', true,
       'Nusrat Jahan', 'nusrat.jahan@example.com', bu.id, p.id,
       NOW(), NOW(), 'system', 'system'
FROM org_business_units bu, org_cost_centers p
WHERE bu.code = 'BU-HO' AND p.cost_center_code = 'CC-HO'
    ON CONFLICT ON CONSTRAINT ukc7mv2nlnq1omcvcalytdltgyr DO NOTHING;

INSERT INTO org_cost_centers
(cost_center_code, cost_center_name, cost_center_type, description, is_active,
 manager_name, manager_email, business_unit_id, parent_cost_center_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'CC-SAL', 'Sales Department', 'DEPARTMENT', 'Sales dept cost center', true,
       NULL, NULL, bu.id, p.id,
       NOW(), NOW(), 'system', 'system'
FROM org_business_units bu, org_cost_centers p
WHERE bu.code = 'BU-HO' AND p.cost_center_code = 'CC-HO'
    ON CONFLICT ON CONSTRAINT ukc7mv2nlnq1omcvcalytdltgyr DO NOTHING;

-- child cost center under Manufacturing Plant (project-level)
INSERT INTO org_cost_centers
(cost_center_code, cost_center_name, cost_center_type, description, is_active,
 manager_name, manager_email, business_unit_id, parent_cost_center_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'CC-PRD-A', 'Production Line A', 'PROJECT', 'Production line A cost center', true,
       NULL, NULL, bu.id, p.id,
       NOW(), NOW(), 'system', 'system'
FROM org_business_units bu, org_cost_centers p
WHERE bu.code = 'BU-MFG' AND p.cost_center_code = 'CC-MFG'
    ON CONFLICT ON CONSTRAINT ukc7mv2nlnq1omcvcalytdltgyr DO NOTHING;


-- =============================================================================
-- 4. WAREHOUSES  (depends on org_business_units)
-- =============================================================================
INSERT INTO org_warehouses
(warehouse_code, warehouse_name, item_type, address, contact_number, manager_name,
 is_active, business_unit_id, created_at, updated_at, created_by, updated_by)
SELECT 'WH-HO-01', 'Head Office Store', 'GENERAL', 'House 1, Gulshan, Dhaka', '+8802-9800001',
       'Imran Hossain', true, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_business_units bu WHERE bu.code = 'BU-HO'
    ON CONFLICT ON CONSTRAINT uk3o67m4s0wu9k8fx62x8527oqk DO NOTHING;

INSERT INTO org_warehouses
(warehouse_code, warehouse_name, item_type, address, contact_number, manager_name,
 is_active, business_unit_id, created_at, updated_at, created_by, updated_by)
SELECT 'WH-MFG-RM', 'Raw Material Warehouse', 'RAW_MATERIAL', 'Plot 14, EPZ Road, Chattogram', '+88031-720001',
       'Imran Hossain', true, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_business_units bu WHERE bu.code = 'BU-MFG'
    ON CONFLICT ON CONSTRAINT uk3o67m4s0wu9k8fx62x8527oqk DO NOTHING;

INSERT INTO org_warehouses
(warehouse_code, warehouse_name, item_type, address, contact_number, manager_name,
 is_active, business_unit_id, created_at, updated_at, created_by, updated_by)
SELECT 'WH-MFG-FG', 'Finished Goods Warehouse', 'FINISHED_GOOD', 'Plot 15, EPZ Road, Chattogram', '+88031-720002',
       NULL, true, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_business_units bu WHERE bu.code = 'BU-MFG'
    ON CONFLICT ON CONSTRAINT uk3o67m4s0wu9k8fx62x8527oqk DO NOTHING;

INSERT INTO org_warehouses
(warehouse_code, warehouse_name, item_type, address, contact_number, manager_name,
 is_active, business_unit_id, created_at, updated_at, created_by, updated_by)
SELECT 'WH-TRD-01', 'Trading Goods Warehouse', 'GENERAL', 'Agrabad Commercial Area, Chattogram', '+88031-720003',
       NULL, true, bu.id, NOW(), NOW(), 'system', 'system'
FROM org_business_units bu WHERE bu.code = 'BU-TRD'
    ON CONFLICT ON CONSTRAINT uk3o67m4s0wu9k8fx62x8527oqk DO NOTHING;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'Business Units', COUNT(*) FROM org_business_units
-- UNION ALL SELECT 'Departments',  COUNT(*) FROM org_departments
-- UNION ALL SELECT 'Cost Centers', COUNT(*) FROM org_cost_centers
-- UNION ALL SELECT 'Warehouses',   COUNT(*) FROM org_warehouses;