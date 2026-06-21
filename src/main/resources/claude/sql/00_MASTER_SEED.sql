-- =============================================================================
--  Spindle ERP — MASTER SEED FILE  (replaces + supersedes all numbered seeds)
--  File   : 00_MASTER_SEED.sql
--  Target : PostgreSQL 14+
--
--  ROOT CAUSES OF FAILURES IN PREVIOUS SEEDS (analysed):
--  ─────────────────────────────────────────────────────
--  1. organization_id=1 hardcoded — SecurityDataInitializer creates org with
--     code='DEFAULT' (not 'ORG-001'); id is assigned by IDENTITY, NOT 1.
--     FIX: All org lookups use SELECT id FROM org_organizations LIMIT 1.
--
--  2. Department codes mis-match — seed uses 'DEPT-MGT','DEPT-FIN' etc. but
--     constraint is on (name) UNIQUE not (code) UNIQUE; re-insert fails.
--     FIX: use ON CONFLICT ON CONSTRAINT ukgfae5yel86q41kw1tl5pbr80a (name).
--
--  3. MovementType enum in global_inventory_transactions constraint does NOT
--     include 'SUPPLIER_RETURN' or 'PRODUCTION_RECEIPT' — only 11 values.
--     FIX: seed data only uses values actually in the CHECK constraint.
--
--  4. acc_chart_of_accounts_sub.sub_account_type discriminator column is
--     NOT the enum column — it's the inheritance column; seeding both
--     sub_account_type and sub_account_type_enum to valid values.
--
--  5. 07_seed_approval_configs.sql references usernames that don't exist
--     yet (purchase.manager, gm, etc.) — JOIN returns 0 rows → silent skips.
--     FIX: insert levels with approver_user_id = NULL (dynamically assigned).
--
--  6. 09_seed_crm_data.sql references sales.manager, sales.executive — same.
--     FIX: assigned_to_id = NULL for all CRM seeds.
--
--  7. Missing menu entries that were added later (BOM, production/boms,
--     Fixed Assets, CRM, Budget, Approvals sub-menus) — now consolidated.
--
--  8. stp_item_uoms table name inconsistency — schema uses inv_item_uom
--     FIX: all UOM seeds target inv_item_uom.
--
--  9. org_departments.code has UNIQUE constraint BUT name also has UNIQUE —
--     previous seeds created conflicts via name uniqueness.
--
--  10. Global inventory_transactions CHECK constraint only has 11 values
--      (SUPPLIER_RETURN excluded, PRODUCTION_RECEIPT excluded), matching
--      MovementType enum in the uploaded file.
--
--  EXECUTION ORDER:
--    Block 0  — Organization (already created by SecurityDataInitializer;
--                we upsert safely)
--    Block 1  — Currencies, Banks, Reference Data
--    Block 2  — Organization hierarchy: BU, Departments, Cost Centers,
--                Warehouses
--    Block 3  — UOM (inv_item_uom)
--    Block 4  — Item Categories, Brands, Models, Items
--    Block 5  — Chart of Accounts (root → sub-groups → leaf)
--    Block 6  — Chart of Accounts Sub-accounts (Customer, Supplier, Bank, LC)
--    Block 7  — HRM: Designations, Employees, Addresses, Salaries
--    Block 8  — Approval Configs + Levels
--    Block 9  — Fixed Asset Categories + Assets
--    Block 10 — CRM: Leads, Opportunities, Contacts
--    Block 11 — BOM templates
--    Block 12 — Document Sequences
--    Block 13 — Accounting Periods + Mapping + Policy
--    Block 14 — App Menus (missing leaves for new modules)
--    Block 15 — Role permissions for new modules
--    Block 16 — superadmin user_context bootstrapping
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- HELPER: get org id once into a temp var via session variable
-- ─────────────────────────────────────────────────────────────────────────────
-- We reference the org dynamically everywhere via sub-select.
-- The SecurityDataInitializer creates code='DEFAULT'; admin can rename it later.
-- All subsequent references use: (SELECT id FROM org_organizations LIMIT 1)


-- =============================================================================
-- BLOCK 0: Ensure the default organization has a proper code
-- (SecurityDataInitializer may create it as 'DEFAULT' — update to 'ORG-001')
-- =============================================================================
UPDATE org_organizations
SET code = 'ORG-001', name = 'Spindle ERP Demo', is_active = true
WHERE id = (SELECT id FROM org_organizations LIMIT 1)
  AND code = 'DEFAULT';


-- =============================================================================
-- BLOCK 1: CURRENCIES
-- =============================================================================
INSERT INTO stp_currencies (code, name, symbol, decimal_places, active)
VALUES
    ('BDT', 'Bangladeshi Taka',  '৳',  2, true),
    ('USD', 'US Dollar',         '$',   2, true),
    ('EUR', 'Euro',              '€',   2, true),
    ('GBP', 'British Pound',     '£',   2, true),
    ('INR', 'Indian Rupee',      '₹',   2, true),
    ('CNY', 'Chinese Yuan',      '¥',   2, true),
    ('SGD', 'Singapore Dollar',  'S$',  2, true),
    ('AED', 'UAE Dirham',        'AED', 2, true),
    ('SAR', 'Saudi Riyal',       'SAR', 2, true),
    ('JPY', 'Japanese Yen',      '¥',   0, true)
ON CONFLICT ON CONSTRAINT uk2jg2r4qbrdlvbbgvwm92kybxf DO NOTHING;


-- =============================================================================
-- BLOCK 1B: BANKS
-- =============================================================================
INSERT INTO stp_banks (bank_code, bank_name, short_name, bank_type, bank_category,
  rating, is_active, organization_id,
  supports_lc, supports_import_lc, supports_export_lc,
  supports_inland_lc, supports_btb_lc, supports_online_banking,
  created_at, updated_at, created_by, updated_by)
SELECT code, name, short, btype, cat, rating, true,
       (SELECT id FROM org_organizations LIMIT 1),
       lc, imp, exp, inland, btb, online, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('SONALI','Sonali Bank PLC',              'SONALI',  'STATE_OWNED','SCHEDULED','GOOD',     true,true,true,true,false,true),
    ('JANATA','Janata Bank PLC',              'JANATA',  'STATE_OWNED','SCHEDULED','GOOD',     true,true,true,true,false,true),
    ('AGRANI','Agrani Bank PLC',              'AGRANI',  'STATE_OWNED','SCHEDULED','GOOD',     true,true,true,true,false,true),
    ('RUPALI','Rupali Bank PLC',              'RUPALI',  'STATE_OWNED','SCHEDULED','GOOD',     true,true,true,true,false,true),
    ('BRAC',  'BRAC Bank PLC',               'BRAC',    'PRIVATE',   'SCHEDULED','EXCELLENT', true,true,true,true,true, true),
    ('CITY',  'The City Bank PLC',           'CITY',    'PRIVATE',   'SCHEDULED','EXCELLENT', true,true,true,true,true, true),
    ('DBBL',  'Dutch-Bangla Bank PLC',       'DBBL',    'PRIVATE',   'SCHEDULED','EXCELLENT', true,true,true,true,true, true),
    ('EBL',   'Eastern Bank PLC',            'EBL',     'PRIVATE',   'SCHEDULED','EXCELLENT', true,true,true,true,true, true),
    ('PRIME', 'Prime Bank PLC',              'PRIME',   'PRIVATE',   'SCHEDULED','EXCELLENT', true,true,true,true,true, true),
    ('IBBL',  'Islami Bank Bangladesh PLC',  'IBBL',    'ISLAMIC',   'SCHEDULED','EXCELLENT', true,true,true,true,true, true),
    ('SCB',   'Standard Chartered Bank',     'SCB',     'FOREIGN',   'FOREIGN',  'EXCELLENT', true,true,true,true,true, true),
    ('HSBC',  'HSBC Bangladesh',             'HSBC',    'FOREIGN',   'FOREIGN',  'EXCELLENT', true,true,true,true,true, true)
) AS t(code, name, short, btype, cat, rating, lc, imp, exp, inland, btb, online)
ON CONFLICT ON CONSTRAINT uq_bank_org_code DO NOTHING;


-- =============================================================================
-- BLOCK 1C: TERMS MASTER (payment + delivery terms)
-- =============================================================================
INSERT INTO stp_terms_master (title, description, document_type, is_active, is_default, sort_order)
VALUES
    ('Net 30',           'Payment due within 30 days',                     'PURCHASE_ORDER', true, true,  10),
    ('Net 60',           'Payment due within 60 days',                     'PURCHASE_ORDER', true, false, 20),
    ('Net 90',           'Payment due within 90 days',                     'PURCHASE_ORDER', true, false, 30),
    ('Advance Payment',  'Full payment before delivery',                   'PURCHASE_ORDER', true, false, 40),
    ('50% Advance',      '50% advance, balance on delivery',               'PURCHASE_ORDER', true, false, 50),
    ('On Delivery',      'Payment on delivery (COD)',                      'PURCHASE_ORDER', true, false, 60),
    ('LC at Sight',      'Letter of Credit payable at sight',              'PURCHASE_ORDER', true, false, 70),
    ('Net 30',           'Payment due within 30 days',                     'SALES_ORDER',   true, true,  10),
    ('Net 60',           'Payment due within 60 days',                     'SALES_ORDER',   true, false, 20),
    ('Advance Payment',  'Full advance payment required',                  'SALES_ORDER',   true, false, 30),
    ('FOB Port',         'Free On Board — at loading port',                'SALES_ORDER',   true, false, 40),
    ('All sales final',  'No returns unless defective',                    'SALES_ORDER',   true, false, 50)
ON CONFLICT DO NOTHING;


-- =============================================================================
-- BLOCK 2: ORGANIZATION HIERARCHY
-- =============================================================================

-- 2A. Business Units
INSERT INTO org_business_units
(code, name, description, is_active, organization_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    code,
    name,
    description,
    true,
    (SELECT id FROM org_organizations LIMIT 1),
    NOW(), NOW(), 'system', 'system'
FROM (
         VALUES
             ('BU-HO',  'Head Office',        'Corporate head office'),
             ('BU-MFG', 'Manufacturing Unit', 'Production / manufacturing plant'),
             ('BU-TRD', 'Trading Division',   'Import/export trading division')
     ) AS t(code, name, description)
ON CONFLICT ON CONSTRAINT uq_bu_org_code DO NOTHING;


-- 2B. Departments (top-level, name is the UNIQUE key not code)
INSERT INTO org_departments (code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT code, dname, desc, true, (SELECT id FROM org_organizations LIMIT 1), NULL, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('DEPT-MGT', 'Management',         'Executive management'),
    ('DEPT-FIN', 'Finance & Accounts', 'Finance and accounts department'),
    ('DEPT-OPS', 'Operations',         'Overall operations department')
) AS t(code, dname, desc)
ON CONFLICT ON CONSTRAINT uktb3nvc5p49dyxs7j7j63hck6w DO NOTHING;  -- unique (name)

-- Child departments (parent = Operations)
INSERT INTO org_departments (code, name, description, active, organization_id, parent_department_id, created_at, updated_at, created_by, updated_by)
SELECT code, dname, desc, true, (SELECT id FROM org_organizations LIMIT 1), p.id, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('DEPT-WH',  'Warehouse & Inventory', 'Warehouse and inventory operations'),
    ('DEPT-PRD', 'Production',            'Manufacturing/production department'),
    ('DEPT-PUR', 'Procurement',           'Purchasing department'),
    ('DEPT-SAL', 'Sales & Marketing',     'Sales and marketing department'),
    ('DEPT-HRM', 'Human Resources',       'HRM department')
) AS t(code, dname, desc),
org_departments p
WHERE p.name = 'Operations'
  AND p.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uktb3nvc5p49dyxs7j7j63hck6w DO NOTHING;


-- 2C. Cost Centers
-- Top-level
INSERT INTO org_cost_centers (cost_center_code, cost_center_name, cost_center_type, description, is_active, business_unit_id, parent_cost_center_id, created_at, updated_at, created_by, updated_by)
SELECT cc_code, cc_name, cc_type, cc_desc, true, bu.id, NULL, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('CC-HO',  'Head Office',        'BRANCH',     'Head office cost center', 'BU-HO'),
    ('CC-MFG', 'Manufacturing Plant','BRANCH',     'Manufacturing plant cost center', 'BU-MFG'),
    ('CC-TRD', 'Trading Division',   'DIVISION',   'Trading division cost center', 'BU-TRD')
) AS t(cc_code, cc_name, cc_type, cc_desc, bu_code)
JOIN org_business_units bu ON bu.code = t.bu_code
    AND bu.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT ukc7mv2nlnq1omcvcalytdltgyr DO NOTHING;

-- Child cost centers
INSERT INTO org_cost_centers (cost_center_code, cost_center_name, cost_center_type, description, is_active, business_unit_id, parent_cost_center_id, created_at, updated_at, created_by, updated_by)
SELECT cc_code, cc_name, cc_type, cc_desc, true, bu.id, p.id, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('CC-FIN',   'Finance Department',  'DEPARTMENT', 'Finance dept cost center',    'BU-HO',  'CC-HO'),
    ('CC-SAL',   'Sales Department',    'DEPARTMENT', 'Sales dept cost center',       'BU-HO',  'CC-HO'),
    ('CC-PRD-A', 'Production Line A',   'PROJECT',    'Production line A',            'BU-MFG', 'CC-MFG'),
    ('CC-PRD-B', 'Production Line B',   'PROJECT',    'Production line B',            'BU-MFG', 'CC-MFG')
) AS t(cc_code, cc_name, cc_type, cc_desc, bu_code, parent_code)
JOIN org_business_units bu ON bu.code = t.bu_code
    AND bu.organization_id = (SELECT id FROM org_organizations LIMIT 1)
JOIN org_cost_centers p ON p.cost_center_code = t.parent_code
ON CONFLICT ON CONSTRAINT ukc7mv2nlnq1omcvcalytdltgyr DO NOTHING;


-- 2D. Warehouses
INSERT INTO org_warehouses (warehouse_code, warehouse_name, item_type, address, contact_number, manager_name, is_active, business_unit_id, created_at, updated_at, created_by, updated_by)
SELECT wh_code, wh_name, wh_type, addr, contact, mgr, true, bu.id, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('WH-HO-GEN',  'Head Office General Store',  'GENERAL',       'House 1, Gulshan, Dhaka',           '+8802-9800001', 'Store Keeper',  'BU-HO'),
    ('WH-MFG-RM',  'Raw Material Warehouse',      'RAW_MATERIAL',  'Plot 14, EPZ Road, Chattogram',     '+88031-720001', 'WH Manager',    'BU-MFG'),
    ('WH-MFG-FG',  'Finished Goods Warehouse',    'FINISHED_GOOD', 'Plot 15, EPZ Road, Chattogram',     '+88031-720002', 'WH Manager',    'BU-MFG'),
    ('WH-MFG-SP',  'Spare Parts Warehouse',       'SPARE_PART',    'Plot 16, EPZ Road, Chattogram',     '+88031-720003', 'WH Manager',    'BU-MFG'),
    ('WH-TRD-01',  'Trading Goods Warehouse',     'GENERAL',       'Agrabad Commercial Area, Ctg',      '+88031-720004', 'WH Manager',    'BU-TRD')
) AS t(wh_code, wh_name, wh_type, addr, contact, mgr, bu_code)
JOIN org_business_units bu ON bu.code = t.bu_code
    AND bu.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uk3o67m4s0wu9k8fx62x8527oqk DO NOTHING;


-- =============================================================================
-- BLOCK 3: UOM  (inv_item_uom — NOT stp_item_uoms)
-- =============================================================================
INSERT INTO inv_item_uom (code, name, symbol, category, conversion_factor, is_base_unit, active, organization_id, created_at, updated_at)
SELECT code, name, sym, cat, 1.000000, true, true,
       (SELECT id FROM org_organizations LIMIT 1), NOW(), NOW()
FROM (VALUES
    ('PCS', 'Piece',           'pcs', 'COUNT'),
    ('BOX', 'Box',             'box', 'PACKING'),
    ('CTN', 'Carton',          'ctn', 'PACKING'),
    ('DZN', 'Dozen',           'dz',  'COUNT'),
    ('KG',  'Kilogram',        'kg',  'WEIGHT'),
    ('G',   'Gram',            'g',   'WEIGHT'),
    ('MT',  'Metric Ton',      'MT',  'WEIGHT'),
    ('LB',  'Pound',           'lb',  'WEIGHT'),
    ('LTR', 'Liter',           'L',   'VOLUME'),
    ('ML',  'Milliliter',      'mL',  'VOLUME'),
    ('MTR', 'Meter',           'm',   'LENGTH'),
    ('CM',  'Centimeter',      'cm',  'LENGTH'),
    ('YD',  'Yard',            'yd',  'LENGTH'),
    ('SQM', 'Square Meter',    'm²',  'AREA'),
    ('SQF', 'Square Foot',     'ft²', 'AREA'),
    ('SET', 'Set',             'set', 'UNIT'),
    ('PAIR','Pair',            'pr',  'COUNT'),
    ('ROLL','Roll',            'rl',  'UNIT')
) AS t(code, name, sym, cat)
ON CONFLICT ON CONSTRAINT uq_uom_org_code DO NOTHING;


-- =============================================================================
-- BLOCK 4: ITEM CATEGORIES, BRANDS, MODELS, ITEMS
-- =============================================================================

-- 4A. Categories (ROOT level)
INSERT INTO inv_item_categories (category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, desc, true, itype, 'ROOT',
       (SELECT id FROM org_organizations LIMIT 1), NULL, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('CAT-RAW',   'Raw Materials',   'All raw material categories',   'RAW_MATERIAL'),
    ('CAT-FG',    'Finished Goods',  'All finished good categories',  'FINISHED_GOOD'),
    ('CAT-SEMI',  'Semi-Finished',   'WIP / semi-finished goods',     'SEMI_FINISHED'),
    ('CAT-SPARE', 'Spare Parts',     'Machine / equipment spares',    'SPARE_PART'),
    ('CAT-CONS',  'Consumables',     'Low-value consumables',         'CONSUMABLE'),
    ('CAT-SVC',   'Services',        'Service items',                 'SERVICE'),
    ('CAT-FA',    'Fixed Assets',    'Capital asset items',           'FIXED_ASSET')
) AS t(code, name, desc, itype)
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

-- 4B. Group-level categories (children of ROOT)
INSERT INTO inv_item_categories (category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, desc, true, itype, 'GROUP',
       (SELECT id FROM org_organizations LIMIT 1), p.id, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('CAT-RAW-CHEM',  'Chemicals',          'Chemical raw materials',      'RAW_MATERIAL', 'CAT-RAW'),
    ('CAT-RAW-FAB',   'Fabrics',            'Fabric raw materials',        'RAW_MATERIAL', 'CAT-RAW'),
    ('CAT-RAW-PACK',  'Packaging',          'Packaging materials',         'RAW_MATERIAL', 'CAT-RAW'),
    ('CAT-FG-TEXTILE','Textile Products',   'Finished textile products',   'FINISHED_GOOD','CAT-FG'),
    ('CAT-FG-ELEC',   'Electronics',        'Finished electronic goods',   'FINISHED_GOOD','CAT-FG'),
    ('CAT-SPARE-MECH','Mechanical Spares',  'Mechanical spare parts',      'SPARE_PART',   'CAT-SPARE'),
    ('CAT-CONS-CLEAN', 'Cleaning Supplies', 'Cleaning and hygiene items',  'CONSUMABLE',   'CAT-CONS'),
    ('CAT-CONS-SAFETY','Safety Equipment',  'PPE and safety equipment',    'CONSUMABLE',   'CAT-CONS')
) AS t(code, name, desc, itype, pcode)
JOIN inv_item_categories p ON p.category_code = t.pcode
    AND p.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

-- 4C. ITEM-level (leaf) categories
INSERT INTO inv_item_categories (category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, desc, true, itype, 'ITEM',
       (SELECT id FROM org_organizations LIMIT 1), p.id, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('CAT-RAW-CHEM-SOLV', 'Solvents',          'Industrial solvents',    'RAW_MATERIAL', 'CAT-RAW-CHEM'),
    ('CAT-RAW-CHEM-DYE',  'Dyes & Pigments',   'Industrial dyes',        'RAW_MATERIAL', 'CAT-RAW-CHEM'),
    ('CAT-RAW-FAB-COTTON','Cotton Fabrics',     'Cotton fabric materials','RAW_MATERIAL', 'CAT-RAW-FAB'),
    ('CAT-FG-TEXTILE-WOV','Woven Products',     'Woven textile products', 'FINISHED_GOOD','CAT-FG-TEXTILE'),
    ('CAT-SPARE-MECH-BRG','Bearings',           'Mechanical bearings',    'SPARE_PART',   'CAT-SPARE-MECH'),
    ('CAT-CONS-SAFETY-GL','Gloves',             'Safety hand gloves',     'CONSUMABLE',   'CAT-CONS-SAFETY')
) AS t(code, name, desc, itype, pcode)
JOIN inv_item_categories p ON p.category_code = t.pcode
    AND p.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- 4D. Brands
INSERT INTO inv_item_brands (brand_code, brand_name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, desc, true, (SELECT id FROM org_organizations LIMIT 1), NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('BRD-GEN',    'Generic',       'Generic / unbranded items'),
    ('BRD-LOCAL',  'Local Brand',   'Locally manufactured'),
    ('BRD-SKF',    'SKF',           'SKF bearings and seals'),
    ('BRD-3M',     '3M',            '3M safety products'),
    ('BRD-BOSCH',  'Bosch',         'Bosch tools and equipment')
) AS t(code, name, desc)
ON CONFLICT ON CONSTRAINT uq_brand_org_code DO NOTHING;


-- 4E. Items (5 representative items for testing all modules)
-- PCS UOM
INSERT INTO inv_items (item_code, item_name, description, item_type, is_active, is_approved, is_hazardous,
 has_lot_tracking, has_serial, unit_of_measure, purchase_unit_code, sales_unit_code,
 cost_price, standard_cost, unit_price, minimum_stock, maximum_stock, reorder_level,
 organization_id, category_id, brand_id, model_id, purchase_unit_id, sales_unit_id, operation_unit_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    code, name, desc, itype, true, true, hazard,
    lot_track, serial, 'KG', 'KG', 'KG',
    cost, cost, price, min_s, max_s, reorder,
    (SELECT id FROM org_organizations LIMIT 1),
    cat.id, brnd.id, NULL,
    uom.id, uom.id, uom.id,
    NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('ITM-RM-ACE-001', 'Industrial Acetone 99%',     'High purity acetone solvent',     'RAW_MATERIAL',  true, true,  120.00, 150.00,  50.000, 1000.000,  100.000, 'CAT-RAW-CHEM-SOLV', 'BRD-GEN'),
    ('ITM-RM-COT-001', 'Cotton Grey Fabric 60x60',   '60x60 grey cotton fabric',        'RAW_MATERIAL',  false,false, 180.00, 200.00, 100.000, 5000.000,  500.000, 'CAT-RAW-FAB-COTTON','BRD-LOCAL'),
    ('ITM-SP-BRG-6205','SKF Bearing 6205-2RS',       'Deep groove ball bearing',        'SPARE_PART',    false,false, 450.00, 600.00,  20.000,  500.000,   50.000, 'CAT-SPARE-MECH-BRG','BRD-SKF'),
    ('ITM-FG-FAB-001', 'Woven Shirt Fabric 40x40',   'Finished woven shirt fabric',     'FINISHED_GOOD', false,false, 280.00, 320.00, 200.000, 10000.000, 1000.000,'CAT-FG-TEXTILE-WOV','BRD-LOCAL'),
    ('ITM-CONS-GL-001','Cotton Hand Gloves (12-pr box)','Safety gloves box of 12 pairs','CONSUMABLE',   false,false,  60.00,  90.00, 100.000,  2000.000,  200.000, 'CAT-CONS-SAFETY-GL','BRD-3M')
) AS t(code, name, desc, itype, hazard, lot_track, cost, price, min_s, max_s, reorder, catcode, brandcode)
JOIN inv_item_categories cat   ON cat.category_code   = t.catcode   AND cat.organization_id   = (SELECT id FROM org_organizations LIMIT 1)
JOIN inv_item_brands     brnd  ON brnd.brand_code      = t.brandcode AND brnd.organization_id   = (SELECT id FROM org_organizations LIMIT 1)
JOIN inv_item_uom        uom   ON uom.code = 'KG'                   AND uom.organization_id    = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- PCS items
INSERT INTO inv_items (item_code, item_name, description, item_type, is_active, is_approved, is_hazardous,
 has_lot_tracking, has_serial, unit_of_measure, purchase_unit_code, sales_unit_code,
 cost_price, standard_cost, unit_price, minimum_stock, maximum_stock, reorder_level,
 organization_id, category_id, brand_id, model_id, purchase_unit_id, sales_unit_id, operation_unit_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    code, name, desc, itype, true, true, false,
    false, false, 'PCS', 'PCS', 'PCS',
    cost, cost, price, min_s, max_s, reorder,
    (SELECT id FROM org_organizations LIMIT 1),
    cat.id, brnd.id, NULL,
    uom.id, uom.id, uom.id,
    NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('ITM-SP-MTR-001', 'Electric Motor 5HP',   '5HP industrial electric motor',    'SPARE_PART', 8500.00,  12000.00, 2.000,  20.000,  5.000,  'CAT-SPARE-MECH', 'BRD-BOSCH'),
    ('ITM-FG-PRD-001', 'Finished Polo Shirt',  'Finished polo shirt, 100% cotton', 'FINISHED_GOOD', 180.00, 350.00, 500.000, 50000.000, 5000.000, 'CAT-FG-TEXTILE-WOV', 'BRD-LOCAL')
) AS t(code, name, desc, itype, cost, price, min_s, max_s, reorder, catcode, brandcode)
JOIN inv_item_categories cat  ON cat.category_code  = t.catcode   AND cat.organization_id  = (SELECT id FROM org_organizations LIMIT 1)
JOIN inv_item_brands     brnd ON brnd.brand_code     = t.brandcode AND brnd.organization_id  = (SELECT id FROM org_organizations LIMIT 1)
JOIN inv_item_uom        uom  ON uom.code = 'PCS'                  AND uom.organization_id   = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;


-- =============================================================================
-- BLOCK 5: CHART OF ACCOUNTS
-- =============================================================================

-- Level 1: Root groups
INSERT INTO acc_chart_of_accounts (account_code, account_name, account_type, account_nature, level, is_active, is_system, is_control_account, allow_manual_entry, currency, opening_balance, current_balance, organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, atype, nature, 1, true, true, true, false, 'BDT', 0.00, 0.00,
       (SELECT id FROM org_organizations LIMIT 1), NULL, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('1000','Assets',       'ASSET',    'DEBIT'),
    ('2000','Liabilities',  'LIABILITY','CREDIT'),
    ('3000','Equity',       'EQUITY',   'CREDIT'),
    ('4000','Revenue',      'REVENUE',  'CREDIT'),
    ('5000','Expenses',     'EXPENSE',  'DEBIT')
) AS t(code, name, atype, nature)
ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- Level 2: Sub-groups
INSERT INTO acc_chart_of_accounts (account_code, account_name, account_type, account_nature, level, is_active, is_system, is_control_account, allow_manual_entry, currency, opening_balance, current_balance, organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, atype, nature, 2, true, true, true, false, 'BDT', 0.00, 0.00,
       (SELECT id FROM org_organizations LIMIT 1), p.id, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('1100','Current Assets',           'ASSET',    'DEBIT',  '1000'),
    ('1200','Fixed Assets',             'ASSET',    'DEBIT',  '1000'),
    ('1300','Accumulated Depreciation', 'ASSET',    'CREDIT', '1000'),
    ('2100','Current Liabilities',      'LIABILITY','CREDIT', '2000'),
    ('2200','Long-term Liabilities',    'LIABILITY','CREDIT', '2000'),
    ('5100','Cost of Goods Sold',       'EXPENSE',  'DEBIT',  '5000'),
    ('5200','Operating Expenses',       'EXPENSE',  'DEBIT',  '5000'),
    ('5300','Financial Expenses',       'EXPENSE',  'DEBIT',  '5000')
) AS t(code, name, atype, nature, pcode)
JOIN acc_chart_of_accounts p ON p.account_code = t.pcode
    AND p.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;

-- Level 3: Leaf accounts
INSERT INTO acc_chart_of_accounts (account_code, account_name, account_type, account_nature, level, is_active, is_system, is_control_account, allow_manual_entry, currency, opening_balance, current_balance, organization_id, parent_account_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, atype, nature, 3, true, false, ctrl, manual, 'BDT', 0.00, 0.00,
       (SELECT id FROM org_organizations LIMIT 1), p.id, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    -- Current Assets
    ('1101','Cash in Hand',              'ASSET',    'DEBIT',  false, true,  '1100'),
    ('1102','Bank Accounts (Control)',   'ASSET',    'DEBIT',  true,  false, '1100'),
    ('1103','Accounts Receivable',       'ASSET',    'DEBIT',  true,  false, '1100'),
    ('1104','Inventory / Stock',         'ASSET',    'DEBIT',  true,  false, '1100'),
    ('1105','Prepaid Expenses',          'ASSET',    'DEBIT',  false, true,  '1100'),
    ('1106','Input VAT Receivable',      'ASSET',    'DEBIT',  false, true,  '1100'),
    ('1107','Advance to Suppliers',      'ASSET',    'DEBIT',  true,  false, '1100'),
    ('1108','Advance to Employees',      'ASSET',    'DEBIT',  true,  false, '1100'),
    ('1109','LC Margin Account',         'ASSET',    'DEBIT',  false, true,  '1100'),
    -- Fixed Assets
    ('1201','Plant & Machinery',         'ASSET',    'DEBIT',  false, true,  '1200'),
    ('1202','Furniture & Fixtures',      'ASSET',    'DEBIT',  false, true,  '1200'),
    ('1203','Computers & Equipment',     'ASSET',    'DEBIT',  false, true,  '1200'),
    ('1204','Vehicles',                  'ASSET',    'DEBIT',  false, true,  '1200'),
    -- Accum. Dep.
    ('1301','Accum. Dep. — Machinery',  'ASSET',    'CREDIT', false, false, '1300'),
    ('1302','Accum. Dep. — Furniture',  'ASSET',    'CREDIT', false, false, '1300'),
    ('1303','Accum. Dep. — Computers',  'ASSET',    'CREDIT', false, false, '1300'),
    ('1304','Accum. Dep. — Vehicles',   'ASSET',    'CREDIT', false, false, '1300'),
    -- Current Liabilities
    ('2101','Accounts Payable',          'LIABILITY','CREDIT', true,  false, '2100'),
    ('2102','Output VAT Payable',        'LIABILITY','CREDIT', false, true,  '2100'),
    ('2103','TDS Payable',               'LIABILITY','CREDIT', false, true,  '2100'),
    ('2104','Salary Payable',            'LIABILITY','CREDIT', false, true,  '2100'),
    ('2105','Advance from Customers',    'LIABILITY','CREDIT', true,  false, '2100'),
    ('2106','LC Liability',              'LIABILITY','CREDIT', false, true,  '2100'),
    -- Equity (Level 2 directly)
    ('3001','Share Capital',             'EQUITY',   'CREDIT', false, true,  '3000'),
    ('3002','Retained Earnings',         'EQUITY',   'CREDIT', false, false, '3000'),
    -- Revenue
    ('4001','Sales Revenue — Local',     'REVENUE',  'CREDIT', false, true,  '4000'),
    ('4002','Sales Revenue — Export',    'REVENUE',  'CREDIT', false, true,  '4000'),
    ('4003','Discount Allowed',          'REVENUE',  'DEBIT',  false, true,  '4000'),
    -- COGS
    ('5101','Raw Material Consumed',     'EXPENSE',  'DEBIT',  false, true,  '5100'),
    ('5102','Direct Labour',             'EXPENSE',  'DEBIT',  false, true,  '5100'),
    ('5103','Factory Overhead',          'EXPENSE',  'DEBIT',  false, true,  '5100'),
    ('5104','Production Variance',       'EXPENSE',  'DEBIT',  false, true,  '5100'),
    -- Operating Expenses
    ('5201','Salary & Wages',            'EXPENSE',  'DEBIT',  false, true,  '5200'),
    ('5202','Rent & Utilities',          'EXPENSE',  'DEBIT',  false, true,  '5200'),
    ('5203','Depreciation Expense',      'EXPENSE',  'DEBIT',  false, false, '5200'),
    ('5204','Office & Admin Expense',    'EXPENSE',  'DEBIT',  false, true,  '5200'),
    ('5205','Marketing & Promotion',     'EXPENSE',  'DEBIT',  false, true,  '5200'),
    ('5206','Transport & Freight',       'EXPENSE',  'DEBIT',  false, true,  '5200'),
    -- Financial Expenses
    ('5301','Bank Charges',              'EXPENSE',  'DEBIT',  false, true,  '5300'),
    ('5302','LC Charges & Commission',   'EXPENSE',  'DEBIT',  false, true,  '5300'),
    ('5303','Interest Expense',          'EXPENSE',  'DEBIT',  false, true,  '5300'),
    ('5304','Foreign Exchange Loss',     'EXPENSE',  'DEBIT',  false, true,  '5300')
) AS t(code, name, atype, nature, ctrl, manual, pcode)
JOIN acc_chart_of_accounts p ON p.account_code = t.pcode
    AND p.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_coa_org_code DO NOTHING;


-- =============================================================================
-- BLOCK 6: COA SUB-ACCOUNTS (Customers, Suppliers, Bank, LC)
-- sub_account_type = discriminator column; sub_account_type_enum = enum
-- =============================================================================

-- Customers (sub_account_type = 'CUSTOMER')
INSERT INTO acc_chart_of_accounts_sub (
    sub_account_type, sub_account_type_enum, sub_account_code, sub_account_name,
    is_active, requires_approval, current_balance, opening_balance,
    credit_limit, credit_days, payment_terms,
    contact_person, contact_email, contact_phone, city, country,
    organization_id, main_account_id, created_at, updated_at, created_by, updated_by)
SELECT
    'CUSTOMER', 'CUSTOMER', code, name, true, false, 0.00, 0.00,
    credit_lmt, credit_d, pterm,
    contact, email, phone, city, 'Bangladesh',
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM acc_chart_of_accounts WHERE account_code='1103' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('CUST-0001','Hasan Textiles Ltd.',         5000000.00, 60, 'Net 60', 'Mahmudul Hasan',  'mahmudul@hasantextiles.com',  '+8801812345001', 'Dhaka'),
    ('CUST-0002','Greenfield Apparels',          2000000.00, 45, 'Net 45', 'Farzana Rahman',  'farzana@greenfieldapparels.com','+8801812345002','Chattogram'),
    ('CUST-0003','Summit Trading Corp.',         3000000.00, 30, 'Net 30', 'Arif Hossain',    'arif@summittrading.com',      '+8801812345003', 'Dhaka'),
    ('CUST-0004','United Garments PLC',          8000000.00, 90, 'Net 90', 'Nasrin Akter',    'nasrin@unitedgarments.com',   '+8801812345004', 'Gazipur'),
    ('CUST-0005','Pacific Export Ltd.',          1500000.00, 30, 'Net 30', 'Jabir Rahman',    'jabir@pacificexport.com',     '+8801812345005', 'Dhaka')
) AS t(code, name, credit_lmt, credit_d, pterm, contact, email, phone, city)
ON CONFLICT ON CONSTRAINT uk4jbleouqhrkuju9onykdx0r8x DO NOTHING;

-- Suppliers
INSERT INTO acc_chart_of_accounts_sub (
    sub_account_type, sub_account_type_enum, sub_account_code, sub_account_name,
    is_active, requires_approval, current_balance, opening_balance,
    credit_limit, credit_days, payment_terms,
    contact_person, contact_email, contact_phone, city, country,
    organization_id, main_account_id, created_at, updated_at, created_by, updated_by)
SELECT
    'SUPPLIER', 'SUPPLIER', code, name, true, false, 0.00, 0.00,
    credit_lmt, credit_d, pterm,
    contact, email, phone, city, ctry,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM acc_chart_of_accounts WHERE account_code='2101' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('SUPP-0001','Acme Chemicals Ltd.',          3000000.00, 30, 'Net 30', 'Ahmed Karim',   'ahmed@acmechem.com',    '+8801755000001', 'Dhaka',       'Bangladesh'),
    ('SUPP-0002','SKF Bangladesh Distributor',   2000000.00, 60, 'Net 60', 'Rafique Islam', 'rafique@skfbd.com',     '+8801755000002', 'Chattogram',  'Bangladesh'),
    ('SUPP-0003','Bosch Tools Bangladesh',        500000.00, 30, 'Net 30', 'Sumon Das',     'sumon@boschtoolsbd.com','+8801755000003', 'Dhaka',       'Bangladesh'),
    ('SUPP-0004','China Fabric Exports Co.',    10000000.00, 90, 'Net 90', 'Zhang Wei',     'zhang@chinafabric.cn',  '+86-21-12345001','Shanghai',    'China'),
    ('SUPP-0005','Global Safety Products',        800000.00, 45, 'Net 45', 'Lee Min Ho',    'lee@globalsafety.sg',   '+65-6234-5678', 'Singapore',   'Singapore')
) AS t(code, name, credit_lmt, credit_d, pterm, contact, email, phone, city, ctry)
ON CONFLICT ON CONSTRAINT uk4jbleouqhrkuju9onykdx0r8x DO NOTHING;

-- Bank Sub-accounts
INSERT INTO acc_chart_of_accounts_sub (
    sub_account_type, sub_account_type_enum, sub_account_code, sub_account_name, bank_account_code,
    bank_name, bank_account_type, currency,
    is_active, requires_approval, current_balance, opening_balance,
    organization_id, main_account_id, created_at, updated_at, created_by, updated_by)
SELECT
    'BANK', 'BANK', code, name, bac,
    bname, 'CURRENT', 'BDT',
    true, false, 0.00, 0.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM acc_chart_of_accounts WHERE account_code='1102' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('BANK-DBBL-001', 'DBBL — Gulshan Branch (Current)', 'BAC-001', 'Dutch-Bangla Bank PLC'),
    ('BANK-EBL-001',  'EBL — Motijheel Branch (Current)','BAC-002', 'Eastern Bank PLC'),
    ('BANK-IBBL-001', 'IBBL — Mirpur Branch (Current)',  'BAC-003', 'Islami Bank Bangladesh PLC')
) AS t(code, name, bac, bname)
ON CONFLICT ON CONSTRAINT uk4jbleouqhrkuju9onykdx0r8x DO NOTHING;

-- Cash Sub-account
INSERT INTO acc_chart_of_accounts_sub (
    sub_account_type, sub_account_type_enum, sub_account_code, sub_account_name, cash_account_code,
    cash_account_type, currency,
    is_active, requires_approval, current_balance, opening_balance,
    organization_id, main_account_id, created_at, updated_at, created_by, updated_by)
VALUES (
    'CASH', 'CASH', 'CASH-HO-001', 'Head Office — Petty Cash', 'CAC-001',
    'PETTY', 'BDT',
    true, false, 0.00, 0.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM acc_chart_of_accounts WHERE account_code='1101' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    NOW(), NOW(), 'system', 'system'
) ON CONFLICT ON CONSTRAINT uk4jbleouqhrkuju9onykdx0r8x DO NOTHING;

-- LC sub-accounts
INSERT INTO acc_chart_of_accounts_sub (
    sub_account_type, sub_account_type_enum, sub_account_code, sub_account_name, lc_number, lc_type, lc_status, currency,
    is_active, requires_approval, current_balance, opening_balance,
    organization_id, main_account_id, created_at, updated_at, created_by, updated_by)
SELECT
    'LC', 'LC', code, name, lc_no, lc_type, 'ACTIVE', 'USD',
    true, true, 0.00, 0.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM acc_chart_of_accounts WHERE account_code='1109' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('LC-2026-001', 'Import LC — China Fabric Co. #2026-001',  'LC-IMP-2026-001', 'IMPORT'),
    ('LC-2026-002', 'Import LC — Global Safety Products #2026', 'LC-IMP-2026-002', 'IMPORT'),
    ('LC-2026-003', 'Export LC — Pacific Export Ltd. #2026-E1', 'LC-EXP-2026-001', 'EXPORT')
) AS t(code, name, lc_no, lc_type)
ON CONFLICT ON CONSTRAINT uk4jbleouqhrkuju9onykdx0r8x DO NOTHING;


-- =============================================================================
-- BLOCK 7: HRM — DESIGNATIONS, EMPLOYEES, SALARIES
-- =============================================================================

-- 7A. Designations
INSERT INTO hrm_designations (designation_code, designation_name, description, grade, is_active, organization_id, created_at, updated_at, created_by, updated_by)
SELECT code, dname, desc, grade, true, (SELECT id FROM org_organizations LIMIT 1), NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('DESIG-MD',    'Managing Director',   'Top executive leadership',      'E1'),
    ('DESIG-GM',    'General Manager',     'Department head level',         'E2'),
    ('DESIG-DGM',   'Deputy General Manager','Senior management',           'E3'),
    ('DESIG-MGR',   'Manager',             'Mid-level management',          'M1'),
    ('DESIG-ASST-MGR','Assistant Manager', 'Assistant to manager',          'M2'),
    ('DESIG-SR-EXE','Senior Executive',    'Senior individual contributor', 'S1'),
    ('DESIG-EXE',   'Executive',           'Individual contributor',        'S2'),
    ('DESIG-OFF',   'Officer',             'Operational officer role',      'O1'),
    ('DESIG-SR-OFF','Senior Officer',      'Senior operational officer',    'O2'),
    ('DESIG-STAFF', 'Staff',               'General staff role',            'G1'),
    ('DESIG-INTERN','Intern',              'Trainee / intern',              'G2'),
    ('DESIG-SUPVR', 'Supervisor',          'Floor supervisor',              'M3'),
    ('DESIG-TECH',  'Technician',          'Technical staff',               'T1'),
    ('DESIG-ACC',   'Accountant',          'Accounts staff',                'S2'),
    ('DESIG-HR-EXE','HR Executive',        'Human resources staff',         'S2')
) AS t(code, dname, desc, grade)
ON CONFLICT ON CONSTRAINT uq_desig_org_code DO NOTHING;


-- 7B. Employees (top-down so reporting_manager_id can reference prior rows)
-- Department sub-select uses name (which is unique) not code
-- Employee 1: Managing Director
INSERT INTO hrm_employees (employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'EMP00001','Rashed','Karim','MALE',DATE '1975-05-12',DATE '2015-01-10',
    'rashed.karim@spindle.local','+8801710000001','PERMANENT','ACTIVE', 20,10,14, 250000.00, 320000.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM org_departments WHERE name='Management' LIMIT 1),
    (SELECT id FROM hrm_designations WHERE designation_code='DESIG-MD' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    NULL, NOW(), NOW(), 'system', 'system'
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- Employee 2: GM Finance
INSERT INTO hrm_employees (employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'EMP00002','Nusrat','Jahan','FEMALE',DATE '1982-03-22',DATE '2017-06-01',
    'nusrat.jahan@spindle.local','+8801710000002','PERMANENT','ACTIVE', 18,10,14, 120000.00, 155000.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM org_departments WHERE name='Finance & Accounts' LIMIT 1),
    (SELECT id FROM hrm_designations WHERE designation_code='DESIG-GM' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    (SELECT id FROM hrm_employees WHERE employee_code='EMP00001' LIMIT 1),
    NOW(), NOW(), 'system', 'system'
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- Employee 3: Accounts Manager
INSERT INTO hrm_employees (employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'EMP00003','Tanvir','Ahmed','MALE',DATE '1988-11-08',DATE '2019-02-15',
    'tanvir.ahmed@spindle.local','+8801710000003','PERMANENT','ACTIVE', 16,10,14, 85000.00, 110000.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM org_departments WHERE name='Finance & Accounts' LIMIT 1),
    (SELECT id FROM hrm_designations WHERE designation_code='DESIG-MGR' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    (SELECT id FROM hrm_employees WHERE employee_code='EMP00002' LIMIT 1),
    NOW(), NOW(), 'system', 'system'
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- Employee 4: HR Executive
INSERT INTO hrm_employees (employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'EMP00004','Sadia','Islam','FEMALE',DATE '1992-07-19',DATE '2021-09-01',
    'sadia.islam@spindle.local','+8801710000004','PERMANENT','ACTIVE', 14,10,14, 45000.00, 58000.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM org_departments WHERE name='Human Resources' LIMIT 1),
    (SELECT id FROM hrm_designations WHERE designation_code='DESIG-HR-EXE' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    (SELECT id FROM hrm_employees WHERE employee_code='EMP00001' LIMIT 1),
    NOW(), NOW(), 'system', 'system'
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- Employee 5: Warehouse staff
INSERT INTO hrm_employees (employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'EMP00005','Imran','Hossain','MALE',DATE '1997-01-30',DATE '2023-04-10',
    'imran.hossain@spindle.local','+8801710000005','CONTRACT','ACTIVE', 10,7,10, 30000.00, 35000.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM org_departments WHERE name='Warehouse & Inventory' LIMIT 1),
    (SELECT id FROM hrm_designations WHERE designation_code='DESIG-STAFF' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    (SELECT id FROM hrm_employees WHERE employee_code='EMP00003' LIMIT 1),
    NOW(), NOW(), 'system', 'system'
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;

-- Employee 6: Production Supervisor
INSERT INTO hrm_employees (employee_code, first_name, last_name, gender, date_of_birth, joining_date,
 email, phone, employee_type, status, annual_leave_days, casual_leave_days, sick_leave_days,
 basic_salary, gross_salary, organization_id, department_id, designation_id, reporting_manager_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'EMP00006','Kamal','Uddin','MALE',DATE '1985-04-15',DATE '2016-07-01',
    'kamal.uddin@spindle.local','+8801710000006','PERMANENT','ACTIVE', 16,10,14, 75000.00, 95000.00,
    (SELECT id FROM org_organizations LIMIT 1),
    (SELECT id FROM org_departments WHERE name='Production' LIMIT 1),
    (SELECT id FROM hrm_designations WHERE designation_code='DESIG-SUPVR' AND organization_id=(SELECT id FROM org_organizations LIMIT 1)),
    (SELECT id FROM hrm_employees WHERE employee_code='EMP00001' LIMIT 1),
    NOW(), NOW(), 'system', 'system'
ON CONFLICT ON CONSTRAINT uq_emp_org_code DO NOTHING;


-- 7C. Employee Salaries (current)
INSERT INTO hrm_employee_salaries (basic_salary, gross_salary, house_rent, medical_allowance, transport_allowance,
 other_allowances, provident_fund, income_tax, other_deductions, net_salary,
 effective_date, is_current, remarks, employee_id, created_at, created_by)
SELECT basic, gross, house, med, transport, 0.00, pf, tax, 0.00, (gross - pf - tax),
       DATE '2024-01-01', true, 'Initial salary record',
       e.id, NOW(), 'system'
FROM (VALUES
    ('EMP00001', 250000.00, 320000.00, 50000.00, 10000.00, 10000.00, 12500.00, 15000.00),
    ('EMP00002', 120000.00, 155000.00, 25000.00,  5000.00,  5000.00,  6000.00,  7000.00),
    ('EMP00003',  85000.00, 110000.00, 18000.00,  4000.00,  3000.00,  4250.00,  4500.00),
    ('EMP00004',  45000.00,  58000.00,  9000.00,  2000.00,  2000.00,  2250.00,  2000.00),
    ('EMP00005',  30000.00,  35000.00,  4000.00,  1000.00,     0.00,     0.00,     0.00),
    ('EMP00006',  75000.00,  95000.00, 15000.00,  3000.00,  2000.00,  3750.00,  3500.00)
) AS t(emp_code, basic, gross, house, med, transport, pf, tax)
JOIN hrm_employees e ON e.employee_code = t.emp_code
WHERE NOT EXISTS (
    SELECT 1 FROM hrm_employee_salaries s WHERE s.employee_id = e.id AND s.is_current = true
);


-- =============================================================================
-- BLOCK 8: APPROVAL CONFIGS + LEVELS
-- (approver_user_id = NULL → dynamically resolved or manually assigned)
-- =============================================================================
INSERT INTO apr_configs (code, name, description, module, document_type, flow_type,
 min_amount, max_amount, priority, use_reporting_hierarchy, enable_reminders,
 reminder_interval_hours, auto_escalation_hours, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, desc, module, doc_type, 'SEQUENTIAL',
       min_amt, NULL, priority, true, true, 24, 72, true,
       (SELECT id FROM org_organizations LIMIT 1), NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('APR-PO',       'Purchase Order Approval',   'PO approval above threshold',      'PURCHASE_SUPPLIER',         'PURCHASE_ORDER',   50000.00,  10),
    ('APR-SO',       'Sales Order Approval',      'SO approval above threshold',      'SALES_CUSTOMER_OPERATIONS', 'SALES_ORDER',     100000.00,  10),
    ('APR-JV',       'Journal Voucher Approval',  'Manual journal voucher approval',  'FINANCE_ACCOUNTS',          'JOURNAL_VOUCHER',      0.00,  20),
    ('APR-PAYMENT',  'Payment Voucher Approval',  'Payment voucher approval',         'FINANCE_ACCOUNTS',          'PAYMENT_VOUCHER',  20000.00,  15),
    ('APR-LEAVE',    'Leave Application Approval','Employee leave approval',          'HRM',                       'LEAVE_APPLICATION',    0.00,  30),
    ('APR-PAYROLL',  'Payroll Approval',          'Payroll run approval',             'HRM',                       'PAYROLL_RUN',          0.00,   5),
    ('APR-LC',       'LC Approval',               'LC opening/amendment approval',   'COMMERCIAL',                'LETTER_OF_CREDIT', 500000.00,  1),
    ('APR-STOCK-ADJ','Stock Adjustment Approval', 'Stock adjustment approval',       'INVENTORY_WAREHOUSE',       'STOCK_ADJUSTMENT',     0.00,  25),
    ('APR-PRD',      'Production Order Approval', 'Production work order approval',  'PRODUCTION',                'PRODUCTION_ORDER',     0.00,  20)
) AS t(code, name, desc, module, doc_type, min_amt, priority)
ON CONFLICT ON CONSTRAINT uq_aprc_code DO NOTHING;

-- Levels — 2-level sequential for major docs, 1-level for operational
INSERT INTO apr_levels (level_number, level_name, approver_description, approval_config_id, approver_user_id,
 can_approve_with_changes, can_forward, can_hold, can_delegate, description, is_active,
 organization_id, created_at, updated_at, created_by, updated_by)
SELECT lvl, lname, ldesc, c.id, NULL, true, true, true, true, ldesc, true,
       (SELECT id FROM org_organizations LIMIT 1), NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('APR-PO',       1, 'Purchase Manager Review',  'First-level review by purchase manager'),
    ('APR-PO',       2, 'GM Final Approval',        'Final approval by GM'),
    ('APR-SO',       1, 'Sales Manager Approval',   'Sales manager sign-off'),
    ('APR-JV',       1, 'Accounts Admin Review',    'Accounts admin review'),
    ('APR-PAYMENT',  1, 'Accountant Review',        'Initial accountant review'),
    ('APR-PAYMENT',  2, 'Accounts Admin Approval',  'Final approval before disbursement'),
    ('APR-LEAVE',    1, 'Manager Approval',         'Direct manager approves leave'),
    ('APR-PAYROLL',  1, 'HRM Manager Approval',     'HRM manager sign-off'),
    ('APR-LC',       1, 'Commercial Manager Approval','Commercial manager approves LC'),
    ('APR-LC',       2, 'GM Final Approval',        'GM final sign-off for LC'),
    ('APR-STOCK-ADJ',1, 'Inventory Manager Approval','Inventory manager sign-off'),
    ('APR-PRD',      1, 'Production Manager Review','Production manager review'),
    ('APR-PRD',      2, 'GM Approval',              'GM approval for production orders')
) AS t(cfg_code, lvl, lname, ldesc)
JOIN apr_configs c ON c.code = t.cfg_code
    AND c.organization_id = (SELECT id FROM org_organizations LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM apr_levels al WHERE al.approval_config_id = c.id AND al.level_number = t.lvl
);


-- =============================================================================
-- BLOCK 9: FIXED ASSET CATEGORIES
-- (Assets themselves are transactional; only categories seeded here)
-- =============================================================================
INSERT INTO fa_asset_categories (code, name, description, is_active,
 default_dep_method, default_dep_rate, default_useful_life_years, default_residual_pct,
 gl_asset_account_id, gl_accum_dep_account_id, gl_dep_exp_account_id,
 organization_id, parent_id, created_at, updated_at, created_by, updated_by)
SELECT code, name, desc, true, dep_method, dep_rate, life_yrs, residual_pct,
       asset_acct.id, accum_acct.id, dep_exp_acct.id,
       (SELECT id FROM org_organizations LIMIT 1), NULL, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('FAC-PM',   'Plant & Machinery',     'Manufacturing plant and machinery',  'STRAIGHT_LINE',      10.00, 10, 5.00, '1201','1301','5203'),
    ('FAC-FUR',  'Furniture & Fixtures',  'Office furniture and fixtures',       'STRAIGHT_LINE',      10.00, 10, 5.00, '1202','1302','5203'),
    ('FAC-COMP', 'Computers & Equipment', 'IT equipment and computers',          'DECLINING_BALANCE',  25.00,  5, 5.00, '1203','1303','5203'),
    ('FAC-VEH',  'Vehicles',              'Company vehicles',                    'STRAIGHT_LINE',      20.00,  5, 5.00, '1204','1304','5203')
) AS t(code, name, desc, dep_method, dep_rate, life_yrs, residual_pct, asset_code, accum_code, dep_exp_code)
JOIN acc_chart_of_accounts asset_acct   ON asset_acct.account_code   = t.asset_code   AND asset_acct.organization_id   = (SELECT id FROM org_organizations LIMIT 1)
JOIN acc_chart_of_accounts accum_acct   ON accum_acct.account_code   = t.accum_code   AND accum_acct.organization_id   = (SELECT id FROM org_organizations LIMIT 1)
JOIN acc_chart_of_accounts dep_exp_acct ON dep_exp_acct.account_code = t.dep_exp_code AND dep_exp_acct.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_fac_org_code DO NOTHING;


-- =============================================================================
-- BLOCK 10: CRM SEED (Leads, Contacts)
-- assigned_to_id = NULL → to be assigned manually in the UI
-- =============================================================================
INSERT INTO crm_leads (lead_no, contact_name, designation, company_name, contact_email, contact_phone,
 city, country, lead_type, source, status, product_interest, remarks,
 organization_id, assigned_to_id, converted_to_id, created_at, updated_at, created_by, updated_by)
SELECT lead_no, contact_name, desig, company, email, phone, city, country, ltype, source, status, product, remarks,
       (SELECT id FROM org_organizations LIMIT 1), NULL, NULL, NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('LEAD-0001','Mahmudul Hasan',  'Procurement Head',    'Hasan Textiles Ltd.',    'mahmudul@hasantextiles.com',  '+8801812345001','Dhaka',      'Bangladesh','EXPORT',  'TRADE_FAIR','NEW',       'Industrial solvents and chemicals','Met at Dhaka Trade Fair 2026'),
    ('LEAD-0002','Farzana Rahman',  'Supply Chain Manager','Greenfield Apparels',    'farzana@greenfield.com',      '+8801812345002','Chattogram', 'Bangladesh','DOMESTIC','REFERRAL',  'QUALIFIED', 'Cotton fabrics for production',    'Referred by existing customer'),
    ('LEAD-0003','Wang Fang',       'Purchasing Director', 'Shanghai Textiles Corp.','wang.fang@shanghaitex.cn',    '+86-21-555001', 'Shanghai',   'China',     'EXPORT',  'ONLINE',    'NEW',       'Woven shirt fabric export',        'Contacted via trade website'),
    ('LEAD-0004','Arif Chowdhury',  'MD',                  'Chowdhury Industries',   'arif@chowdhuryind.com',       '+8801812345004','Narayanganj','Bangladesh','DOMESTIC','COLD_CALL', 'CONTACTED', 'Spare parts for machinery',        'Called from directory listing')
) AS t(lead_no, contact_name, desig, company, email, phone, city, country, ltype, source, status, product, remarks)
ON CONFLICT ON CONSTRAINT uq_crl_org_no DO NOTHING;

-- CRM Contacts
INSERT INTO crm_contacts (first_name, last_name, designation, department, email, mobile, phone,
 is_active, is_primary, notes, organization_id, customer_id, created_at, updated_at, created_by, updated_by)
SELECT fname, lname, desig, dept, email, mobile, phone, true, true, notes,
       (SELECT id FROM org_organizations LIMIT 1),
       (SELECT id FROM acc_chart_of_accounts_sub WHERE sub_account_code = cust_code LIMIT 1),
       NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('Mahmudul','Hasan',  'Procurement Head',    'Procurement',  'mahmudul@hasantextiles.com',  '+8801812345001','+8802-9800101','Primary contact at Hasan Textiles',    'CUST-0001'),
    ('Farzana', 'Rahman', 'Supply Chain Manager','Supply Chain', 'farzana@greenfieldapparels.com','+8801812345002','+8802-9800102','Primary contact at Greenfield Apparel','CUST-0002'),
    ('Arif',    'Hossain','Director',            'Management',   'arif@summittrading.com',      '+8801812345003','+8802-9800103','Primary contact at Summit Trading',     'CUST-0003')
) AS t(fname, lname, desig, dept, email, mobile, phone, notes, cust_code)
WHERE NOT EXISTS (SELECT 1 FROM crm_contacts cc WHERE cc.email = t.email);


-- =============================================================================
-- BLOCK 11: BOM TEMPLATES
-- =============================================================================
INSERT INTO prd_bom (bom_code, bom_name, bom_version, output_quantity, yield_percent,
 is_active, is_default, description, organization_id, finished_item_id, output_unit_id,
 created_at, updated_at, created_by, updated_by)
SELECT 'BOM-POLO-001', 'Polo Shirt Production BOM v1', '1.0', 100.000, 95.00,
       true, true, 'BOM for producing 100 pcs finished polo shirts',
       (SELECT id FROM org_organizations LIMIT 1),
       (SELECT id FROM inv_items WHERE item_code = 'ITM-FG-PRD-001' LIMIT 1),
       (SELECT id FROM inv_item_uom WHERE code = 'PCS' AND organization_id = (SELECT id FROM org_organizations LIMIT 1)),
       NOW(), NOW(), 'system', 'system'
WHERE EXISTS (SELECT 1 FROM inv_items WHERE item_code = 'ITM-FG-PRD-001')
ON CONFLICT ON CONSTRAINT uq_bom_org_code DO NOTHING;

-- BOM Items (raw material inputs)
INSERT INTO prd_bom_items (bom_id, raw_item_id, unit_id, line_number, quantity, scrap_pct, is_optional, created_at, created_by)
SELECT b.id, i.id, u.id, ln, qty, scrap, false, NOW(), 'system'
FROM (VALUES
    ('BOM-POLO-001', 'ITM-RM-COT-001', 'KG', 1, 60.000, 5.00),
    ('BOM-POLO-001', 'ITM-RM-ACE-001', 'KG', 2,  2.000, 2.00),
    ('BOM-POLO-001', 'ITM-CONS-GL-001','KG', 3,  0.500, 1.00)
) AS t(bom_code, item_code, uom_code, ln, qty, scrap)
JOIN prd_bom  b ON b.bom_code = t.bom_code AND b.organization_id = (SELECT id FROM org_organizations LIMIT 1)
JOIN inv_items i ON i.item_code = t.item_code AND i.organization_id = (SELECT id FROM org_organizations LIMIT 1)
JOIN inv_item_uom u ON u.code = t.uom_code AND u.organization_id = (SELECT id FROM org_organizations LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM prd_bom_items bi WHERE bi.bom_id = b.id AND bi.line_number = t.ln
);


-- =============================================================================
-- BLOCK 12: DOCUMENT SEQUENCES
-- =============================================================================
INSERT INTO stp_document_sequences (organization_id, prefix, year_code, last_seq, created_at, updated_at)
SELECT (SELECT id FROM org_organizations LIMIT 1), prefix, '26', 0, NOW(), NOW()
FROM (VALUES
    ('PO'),('GRN'),('PI'),('DN'),
    ('SO'),('DC'),('SI'),('CN'),
    ('JV'),('PV'),('RV'),('CV'),
    ('SA'),('ST'),
    ('PRD'),('BOM'),
    ('ECI'),('ICI'),
    ('LEAD'),('OPP'),
    ('BGT'),('FY'),
    ('DESIG'),('EMP')
) AS t(prefix)
ON CONFLICT ON CONSTRAINT uq_docseq_org_prefix_year DO NOTHING;


-- =============================================================================
-- BLOCK 13: ACCOUNTING PERIODS
-- =============================================================================
INSERT INTO acc_periods (period_name, period_type, fiscal_year, start_date, end_date, is_active, is_closed, organization_id, created_at, updated_at, created_by, updated_by)
SELECT pname, 'MONTHLY', 2026, sdate, edate, is_act, false,
       (SELECT id FROM org_organizations LIMIT 1), NOW(), NOW(), 'system', 'system'
FROM (VALUES
    ('Jan-2026', DATE '2026-01-01', DATE '2026-01-31', false),
    ('Feb-2026', DATE '2026-02-01', DATE '2026-02-28', false),
    ('Mar-2026', DATE '2026-03-01', DATE '2026-03-31', false),
    ('Apr-2026', DATE '2026-04-01', DATE '2026-04-30', false),
    ('May-2026', DATE '2026-05-01', DATE '2026-05-31', false),
    ('Jun-2026', DATE '2026-06-01', DATE '2026-06-30', true),
    ('Jul-2026', DATE '2026-07-01', DATE '2026-07-31', true),
    ('Aug-2026', DATE '2026-08-01', DATE '2026-08-31', false),
    ('Sep-2026', DATE '2026-09-01', DATE '2026-09-30', false),
    ('Oct-2026', DATE '2026-10-01', DATE '2026-10-31', false),
    ('Nov-2026', DATE '2026-11-01', DATE '2026-11-30', false),
    ('Dec-2026', DATE '2026-12-01', DATE '2026-12-31', false)
) AS t(pname, sdate, edate, is_act)
ON CONFLICT ON CONSTRAINT uk73i7mph9xad8yj4wjl9uvnfts DO NOTHING;


-- =============================================================================
-- BLOCK 14: MISSING MENU LEAVES (new modules added in Spindle ERP vs Optimum)
-- =============================================================================

-- Fixed Assets group + leaves under MOD_ACCOUNTS
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_ACC_FA', 'Fixed Assets', NULL, 'fa fa-building',
       m.id, 50, 'GROUP', 'ACCOUNTS', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_ACCOUNTS'
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_CATEGORIES','Asset Categories', '/fixed-assets/categories', 'fa fa-folder', g.id, 10, 'LEAF', 'ACCOUNTS', 'SECURITY.USER.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_FA' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_ASSETS',    'Asset Register',   '/fixed-assets/assets',     'fa fa-cog',    g.id, 20, 'LEAF', 'ACCOUNTS', 'SECURITY.USER.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_FA' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_DEPRECIATION','Depreciation Runs','/fixed-assets/depreciation','fa fa-chart-line',g.id,30,'LEAF','ACCOUNTS','SECURITY.USER.VIEW','_self',true,true,false,NOW(),NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_FA' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'FA_DISPOSALS', 'Asset Disposals',  '/fixed-assets/disposals',  'fa fa-trash',  g.id, 40, 'LEAF', 'ACCOUNTS', 'SECURITY.USER.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_ACC_FA' ON CONFLICT (menu_code) DO NOTHING;

-- CRM Group under new MOD_CRM module
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
VALUES ('MOD_CRM', 'CRM', NULL, 'fa fa-handshake', NULL, 75, 'MODULE', 'CRM', NULL, '_self', true, true, false, NOW(), NOW())
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_CRM_LEADS', 'Lead Management', NULL, 'fa fa-funnel-dollar', m.id, 10, 'GROUP', 'CRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_CRM' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_CRM_OPS', 'CRM Operations', NULL, 'fa fa-tasks', m.id, 20, 'GROUP', 'CRM', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_CRM' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_LEADS',    'Leads',      '/crm/leads',        'fa fa-user-plus',   g.id, 10, 'LEAF', 'CRM', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_CRM_LEADS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_OPPS',     'Opportunities','/crm/opportunities','fa fa-star',       g.id, 20, 'LEAF', 'CRM', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_CRM_LEADS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_CONTACTS', 'Contacts',   '/crm/contacts',     'fa fa-address-book',g.id, 10, 'LEAF', 'CRM', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_CRM_OPS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_ACTIVITIES','Activities','/crm/activities',   'fa fa-calendar',   g.id, 20, 'LEAF', 'CRM', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_CRM_OPS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'CRM_FEEDBACK',  'Feedback',  '/crm/feedback',     'fa fa-comment-alt',g.id, 30, 'LEAF', 'CRM', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_CRM_OPS' ON CONFLICT (menu_code) DO NOTHING;

-- BOM leaf under Production
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'PRD_BOM', 'Bill of Materials', '/production/boms', 'fa fa-list-alt', g.id, 5, 'LEAF', 'PRODUCTION', 'PRD.ORDER.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_PRD_ORDERS' ON CONFLICT (menu_code) DO NOTHING;

-- Document Sequences leaf under Setup Reference Data
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'STP_DOC_SEQ', 'Document Sequences', '/document-sequences', 'fa fa-list-ol', g.id, 50, 'LEAF', 'SETUP', 'SECURITY.USER.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_STP_REF' ON CONFLICT (menu_code) DO NOTHING;

-- Budget module
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
VALUES ('MOD_BUDGET', 'Budget', NULL, 'fa fa-wallet', NULL, 85, 'MODULE', 'BUDGET', NULL, '_self', true, true, false, NOW(), NOW())
ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_BGT_MASTER', 'Budget Setup', NULL, 'fa fa-sliders-h', m.id, 10, 'GROUP', 'BUDGET', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_BUDGET' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_BGT_OPS', 'Budget Operations', NULL, 'fa fa-tasks', m.id, 20, 'GROUP', 'BUDGET', NULL, '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_BUDGET' ON CONFLICT (menu_code) DO NOTHING;

INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_FY',     'Fiscal Years',    '/budget/fiscal-years',  'fa fa-calendar',    g.id, 10, 'LEAF', 'BUDGET', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_BGT_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_HEADS',  'Budget Heads',    '/budget/heads',         'fa fa-layer-group', g.id, 20, 'LEAF', 'BUDGET', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_BGT_MASTER' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_LIST',   'Budgets',         '/budget/list',          'fa fa-file-invoice',g.id, 10, 'LEAF', 'BUDGET', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_BGT_OPS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_REVISIONS','Revisions',    '/budget/revisions',      'fa fa-edit',        g.id, 20, 'LEAF', 'BUDGET', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_BGT_OPS' ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'BGT_TRANSFERS','Transfers',    '/budget/transfers',      'fa fa-exchange-alt',g.id, 30, 'LEAF', 'BUDGET', 'RPT.DASHBOARD.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_BGT_OPS' ON CONFLICT (menu_code) DO NOTHING;

-- Approval Inbox leaf
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name, required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'APR_INBOX',  'My Inbox',  '/approval/inbox', 'fa fa-inbox', g.id, 5, 'LEAF', 'APPROVALS', 'APR.REQUEST.VIEW', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_APR_PENDING' ON CONFLICT (menu_code) DO NOTHING;


-- =============================================================================
-- BLOCK 15: Grant SUPER_ADMIN new menu leaves
-- =============================================================================
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id, m.id, true, true, true, true, NOW(), NOW()
FROM sec_roles r
CROSS JOIN app_menus m
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND m.active = true AND m.deleted = false
ON CONFLICT (role_id, menu_id) DO NOTHING;


-- =============================================================================
-- BLOCK 16: USER_CONTEXT bootstrap for superadmin
-- =============================================================================
INSERT INTO user_context (user_id, organization_id, business_unit_id, warehouse_id, cost_center_id)
SELECT u.id,
       (SELECT id FROM org_organizations LIMIT 1),
       (SELECT id FROM org_business_units WHERE code='BU-HO' LIMIT 1),
       (SELECT id FROM org_warehouses WHERE warehouse_code='WH-HO-GEN' LIMIT 1),
       (SELECT id FROM org_cost_centers WHERE cost_center_code='CC-HO' LIMIT 1)
FROM sec_users u WHERE u.username = 'superadmin'
ON CONFLICT (user_id) DO UPDATE SET
    organization_id = EXCLUDED.organization_id,
    business_unit_id = EXCLUDED.business_unit_id,
    warehouse_id = EXCLUDED.warehouse_id,
    cost_center_id = EXCLUDED.cost_center_id;


-- =============================================================================
-- BLOCK 17: sec_user_access_scopes for superadmin
-- =============================================================================
INSERT INTO sec_user_access_scopes (user_id, scope_type, reference_id, created_at)
SELECT u.id, 'ORGANIZATION', o.id, NOW()
FROM sec_users u, org_organizations o
WHERE u.username = 'superadmin'
ON CONFLICT ON CONSTRAINT uq_uas_user_scope_ref DO NOTHING;

INSERT INTO sec_user_access_scopes (user_id, scope_type, reference_id, created_at)
SELECT u.id, 'BUSINESS_UNIT', bu.id, NOW()
FROM sec_users u CROSS JOIN org_business_units bu
WHERE u.username = 'superadmin'
  AND bu.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_uas_user_scope_ref DO NOTHING;

INSERT INTO sec_user_access_scopes (user_id, scope_type, reference_id, created_at)
SELECT u.id, 'WAREHOUSE', wh.id, NOW()
FROM sec_users u CROSS JOIN org_warehouses wh
WHERE u.username = 'superadmin'
  AND wh.organization_id = (SELECT id FROM org_organizations LIMIT 1)
ON CONFLICT ON CONSTRAINT uq_uas_user_scope_ref DO NOTHING;


-- =============================================================================
-- VERIFICATION SUMMARY
-- =============================================================================
DO $$
DECLARE
    v_org_id bigint;
BEGIN
    SELECT id INTO v_org_id FROM org_organizations LIMIT 1;
    RAISE NOTICE '=== Spindle ERP Seed Verification ===';
    RAISE NOTICE 'Organization id = %', v_org_id;
    RAISE NOTICE 'BU count       = %', (SELECT COUNT(*) FROM org_business_units   WHERE organization_id = v_org_id);
    RAISE NOTICE 'Departments    = %', (SELECT COUNT(*) FROM org_departments       WHERE organization_id = v_org_id);
    RAISE NOTICE 'Cost Centers   = %', (SELECT COUNT(*) FROM org_cost_centers cc JOIN org_business_units bu ON bu.id=cc.business_unit_id WHERE bu.organization_id=v_org_id);
    RAISE NOTICE 'Warehouses     = %', (SELECT COUNT(*) FROM org_warehouses WHERE organization_id = v_org_id);
    RAISE NOTICE 'UOM            = %', (SELECT COUNT(*) FROM inv_item_uom    WHERE organization_id = v_org_id);
    RAISE NOTICE 'Item Cats      = %', (SELECT COUNT(*) FROM inv_item_categories WHERE organization_id = v_org_id);
    RAISE NOTICE 'Items          = %', (SELECT COUNT(*) FROM inv_items        WHERE organization_id = v_org_id);
    RAISE NOTICE 'COA Accounts   = %', (SELECT COUNT(*) FROM acc_chart_of_accounts WHERE organization_id = v_org_id);
    RAISE NOTICE 'Sub-accounts   = %', (SELECT COUNT(*) FROM acc_chart_of_accounts_sub WHERE organization_id = v_org_id);
    RAISE NOTICE 'Designations   = %', (SELECT COUNT(*) FROM hrm_designations WHERE organization_id = v_org_id);
    RAISE NOTICE 'Employees      = %', (SELECT COUNT(*) FROM hrm_employees    WHERE organization_id = v_org_id);
    RAISE NOTICE 'Salaries       = %', (SELECT COUNT(*) FROM hrm_employee_salaries es JOIN hrm_employees e ON e.id=es.employee_id WHERE e.organization_id=v_org_id);
    RAISE NOTICE 'Approval Cfgs  = %', (SELECT COUNT(*) FROM apr_configs WHERE organization_id = v_org_id);
    RAISE NOTICE 'Approval Lvls  = %', (SELECT COUNT(*) FROM apr_levels  WHERE organization_id = v_org_id);
    RAISE NOTICE 'FA Categories  = %', (SELECT COUNT(*) FROM fa_asset_categories WHERE organization_id = v_org_id);
    RAISE NOTICE 'BOMs           = %', (SELECT COUNT(*) FROM prd_bom     WHERE organization_id = v_org_id);
    RAISE NOTICE 'BOM Items      = %', (SELECT COUNT(*) FROM prd_bom_items bi JOIN prd_bom b ON b.id=bi.bom_id WHERE b.organization_id=v_org_id);
    RAISE NOTICE 'CRM Leads      = %', (SELECT COUNT(*) FROM crm_leads   WHERE organization_id = v_org_id);
    RAISE NOTICE 'CRM Contacts   = %', (SELECT COUNT(*) FROM crm_contacts WHERE organization_id = v_org_id);
    RAISE NOTICE 'Doc Sequences  = %', (SELECT COUNT(*) FROM stp_document_sequences WHERE organization_id = v_org_id);
    RAISE NOTICE 'Acc Periods    = %', (SELECT COUNT(*) FROM acc_periods  WHERE organization_id = v_org_id);
    RAISE NOTICE 'Menus Total    = %', (SELECT COUNT(*) FROM app_menus    WHERE active=true AND deleted=false);
    RAISE NOTICE 'Role-Menus     = %', (SELECT COUNT(*) FROM sec_mrole_menus);
    RAISE NOTICE 'Currencies     = %', (SELECT COUNT(*) FROM stp_currencies);
    RAISE NOTICE 'Banks          = %', (SELECT COUNT(*) FROM stp_banks WHERE organization_id = v_org_id);
    RAISE NOTICE '=== Seed Complete ===';
END;
$$;

COMMIT;
