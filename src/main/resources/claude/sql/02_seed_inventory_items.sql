-- =============================================================================
--  Optimum ERP — Inventory Master Seed Data (Items)
--  File   : 02_seed_inventory_items.sql
--  Target : PostgreSQL (inv_item_brands, inv_item_categories,
--           inv_item_models, inv_item_uom, inv_items)
--
--  Execution order:
--    1. inv_item_brands
--    2. inv_item_categories (ROOT -> GROUP -> ITEM)
--    3. inv_item_models     (depends on brands)
--    4. inv_item_uom
--    5. inv_items            (depends on brands, categories, models, uom)
--
--  Idempotent via ON CONFLICT DO NOTHING on unique constraints.
-- =============================================================================

BEGIN;

-- assume organization_id = 1 throughout; change as needed

-- =============================================================================
-- 1. BRANDS
-- =============================================================================
INSERT INTO inv_item_brands
(brand_code, brand_name, description, is_active, organization_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('BRD-GEN',    'Generic',        'Generic / unbranded items',     true, 1, NOW(), NOW(), 'system', 'system'),
    ('BRD-BOSCH',  'Bosch',          'Bosch tools and parts',         true, 1, NOW(), NOW(), 'system', 'system'),
    ('BRD-SKF',    'SKF',            'SKF bearings and seals',        true, 1, NOW(), NOW(), 'system', 'system'),
    ('BRD-SAMSUNG','Samsung',        'Samsung electronics',           true, 1, NOW(), NOW(), 'system', 'system')
    ON CONFLICT ON CONSTRAINT uq_brand_org_code DO NOTHING;


-- =============================================================================
-- 2. CATEGORIES  (ROOT -> GROUP -> ITEM)
-- =============================================================================

-- ROOT level
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
VALUES
    ('CAT-RAW',  'Raw Materials',   'All raw material categories',  true, 'RAW_MATERIAL',  'ROOT', 1, NULL, NOW(), NOW(), 'system', 'system'),
    ('CAT-FG',   'Finished Goods',  'All finished good categories', true, 'FINISHED_GOOD', 'ROOT', 1, NULL, NOW(), NOW(), 'system', 'system'),
    ('CAT-SPARE','Spare Parts',     'All spare part categories',    true, 'SPARE_PART',    'ROOT', 1, NULL, NOW(), NOW(), 'system', 'system'),
    ('CAT-CONS', 'Consumables',     'All consumable categories',    true, 'CONSUMABLE',    'ROOT', 1, NULL, NOW(), NOW(), 'system', 'system')
    ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

-- GROUP level (children of ROOT)
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT 'CAT-RAW-CHEM', 'Chemicals', 'Chemical raw materials', true, 'RAW_MATERIAL', 'GROUP', 1, c.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c WHERE c.category_code = 'CAT-RAW' AND c.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT 'CAT-FG-ELEC', 'Electronics', 'Finished electronic goods', true, 'FINISHED_GOOD', 'GROUP', 1, c.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c WHERE c.category_code = 'CAT-FG' AND c.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT 'CAT-SPARE-MECH', 'Mechanical Spares', 'Mechanical spare parts', true, 'SPARE_PART', 'GROUP', 1, c.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c WHERE c.category_code = 'CAT-SPARE' AND c.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

-- ITEM level (children of GROUP) — leaf categories actually assigned to items
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT 'CAT-RAW-CHEM-SOLV', 'Solvents', 'Industrial solvents', true, 'RAW_MATERIAL', 'ITEM', 1, c.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c WHERE c.category_code = 'CAT-RAW-CHEM' AND c.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT 'CAT-FG-ELEC-MOB', 'Mobile Devices', 'Finished mobile devices', true, 'FINISHED_GOOD', 'ITEM', 1, c.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c WHERE c.category_code = 'CAT-FG-ELEC' AND c.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type, organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT 'CAT-SPARE-MECH-BRG', 'Bearings', 'Mechanical bearings', true, 'SPARE_PART', 'ITEM', 1, c.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c WHERE c.category_code = 'CAT-SPARE-MECH' AND c.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- 3. ITEM MODELS  (depends on brands)
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT 'MDL-GS24', 'Galaxy S24', 'Samsung Galaxy S24 series', true, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b WHERE b.brand_code = 'BRD-SAMSUNG' AND b.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT 'MDL-6205', '6205-2RS', 'SKF deep groove ball bearing', true, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b WHERE b.brand_code = 'BRD-SKF' AND b.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;


-- =============================================================================
-- 4. UOM
-- =============================================================================
INSERT INTO inv_item_uom
(code, name, symbol, category, conversion_factor, is_base_unit, active, organization_id, created_at, updated_at)
VALUES
    ('PCS', 'Piece',     'pcs', 'COUNT',  1.000000, true,  true, 1, NOW(), NOW()),
    ('BOX', 'Box',        'box', 'PACKING',1.000000, true,  true, 1, NOW(), NOW()),
    ('KG',  'Kilogram',  'kg',  'WEIGHT', 1.000000, true,  true, 1, NOW(), NOW()),
    ('LTR', 'Liter',     'L',   'VOLUME', 1.000000, true,  true, 1, NOW(), NOW()),
    ('MTR', 'Meter',     'm',   'LENGTH', 1.000000, true,  true, 1, NOW(), NOW())
    ON CONFLICT ON CONSTRAINT uq_uom_org_code DO NOTHING;


-- =============================================================================
-- 5. ITEMS
-- =============================================================================

-- ── Item 1: Raw material (solvent) ───────────────────────────────────────────
INSERT INTO inv_items
(item_code, item_name, description, item_type, is_active, is_approved, is_hazardous,
 has_lot_tracking, has_serial, unit_of_measure, purchase_unit_code, sales_unit_code,
 cost_price, standard_cost, unit_price, minimum_stock, maximum_stock, reorder_level,
 organization_id, category_id, brand_id, model_id,
 purchase_unit_id, sales_unit_id, operation_unit_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'ITM-SOLV-001', 'Industrial Acetone', 'High purity acetone solvent for cleaning', 'RAW_MATERIAL',
    true, true, true,
    true, false, 'LTR', 'LTR', 'LTR',
    120.0000, 120.0000, 150.0000, 50.000, 1000.000, 100.000,
    1, c.id, b.id, NULL,
    u.id, u.id, u.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c, inv_item_brands b, inv_item_uom u
WHERE c.category_code = 'CAT-RAW-CHEM-SOLV' AND c.organization_id = 1
  AND b.brand_code = 'BRD-GEN' AND b.organization_id = 1
  AND u.code = 'LTR' AND u.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- ── Item 2: Finished good (mobile phone) ─────────────────────────────────────
INSERT INTO inv_items
(item_code, item_name, description, item_type, is_active, is_approved, is_hazardous,
 has_lot_tracking, has_serial, unit_of_measure, purchase_unit_code, sales_unit_code,
 cost_price, standard_cost, unit_price, minimum_stock, maximum_stock, reorder_level,
 warranty_months, organization_id, category_id, brand_id, model_id,
 purchase_unit_id, sales_unit_id, operation_unit_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'ITM-GS24-001', 'Samsung Galaxy S24 256GB', 'Flagship smartphone, 256GB storage', 'FINISHED_GOOD',
    true, true, false,
    false, true, 'PCS', 'PCS', 'PCS',
    65000.0000, 65000.0000, 89999.0000, 5.000, 200.000, 10.000,
    12, 1, c.id, b.id, m.id,
    u.id, u.id, u.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c, inv_item_brands b, inv_item_models m, inv_item_uom u
WHERE c.category_code = 'CAT-FG-ELEC-MOB' AND c.organization_id = 1
  AND b.brand_code = 'BRD-SAMSUNG' AND b.organization_id = 1
  AND m.model_code = 'MDL-GS24' AND m.organization_id = 1
  AND u.code = 'PCS' AND u.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- ── Item 3: Spare part (bearing) ──────────────────────────────────────────────
INSERT INTO inv_items
(item_code, item_name, description, item_type, is_active, is_approved, is_hazardous,
 has_lot_tracking, has_serial, unit_of_measure, purchase_unit_code, sales_unit_code,
 cost_price, standard_cost, unit_price, minimum_stock, maximum_stock, reorder_level,
 organization_id, category_id, brand_id, model_id,
 purchase_unit_id, sales_unit_id, operation_unit_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'ITM-BRG-6205', 'SKF Bearing 6205-2RS', 'Deep groove ball bearing, sealed', 'SPARE_PART',
    true, true, false,
    false, false, 'PCS', 'PCS', 'PCS',
    450.0000, 450.0000, 600.0000, 20.000, 500.000, 50.000,
    1, c.id, b.id, m.id,
    u.id, u.id, u.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c, inv_item_brands b, inv_item_models m, inv_item_uom u
WHERE c.category_code = 'CAT-SPARE-MECH-BRG' AND c.organization_id = 1
  AND b.brand_code = 'BRD-SKF' AND b.organization_id = 1
  AND m.model_code = 'MDL-6205' AND m.organization_id = 1
  AND u.code = 'PCS' AND u.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- ── Item 4: Consumable (no brand/model) ───────────────────────────────────────
INSERT INTO inv_items
(item_code, item_name, description, item_type, is_active, is_approved, is_hazardous,
 has_lot_tracking, has_serial, unit_of_measure, purchase_unit_code, sales_unit_code,
 cost_price, standard_cost, unit_price, minimum_stock, maximum_stock, reorder_level,
 organization_id, category_id, brand_id, model_id,
 purchase_unit_id, sales_unit_id, operation_unit_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'ITM-GLOVE-001', 'Cotton Hand Gloves', 'Safety hand gloves, cotton', 'CONSUMABLE',
    true, true, false,
    false, false, 'PCS', 'BOX', 'PCS',
    5.0000, 5.0000, 8.0000, 100.000, 5000.000, 500.000,
    1, c.id, NULL, NULL,
    ub.id, up.id, up.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories c, inv_item_uom ub, inv_item_uom up
WHERE c.category_code = 'CAT-RAW-CHEM-SOLV' AND c.organization_id = 1  -- replace with a CONSUMABLE leaf category in real use
  AND ub.code = 'BOX' AND ub.organization_id = 1
  AND up.code = 'PCS' AND up.organization_id = 1
    ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'Brands',     COUNT(*) FROM inv_item_brands
-- UNION ALL SELECT 'Categories', COUNT(*) FROM inv_item_categories
-- UNION ALL SELECT 'Models',     COUNT(*) FROM inv_item_models
-- UNION ALL SELECT 'UOM',        COUNT(*) FROM inv_item_uom
-- UNION ALL SELECT 'Items',      COUNT(*) FROM inv_items;