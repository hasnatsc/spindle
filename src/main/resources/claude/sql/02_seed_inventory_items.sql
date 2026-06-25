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
-- Generic
('BRD-GEN',         'Generic',          'Generic / unbranded items',                TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Bearings
('BRD-SKF',         'SKF',              'SKF bearings and seals',                   TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-NSK',         'NSK',              'NSK bearings',                             TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-NTN',         'NTN',              'NTN bearings',                             TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-FAG',         'FAG',              'FAG bearings',                             TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-TIMKEN',      'Timken',           'Timken bearings',                          TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Motors & Drives
('BRD-ABB',         'ABB',              'ABB motors and drives',                    TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-SIEMENS',     'Siemens',          'Siemens motors and automation',            TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-WEG',         'WEG',              'WEG electric motors',                      TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-SEW',         'SEW Eurodrive',    'Gear motors and drives',                   TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Automation
('BRD-SCHNEIDER',   'Schneider Electric','Industrial automation products',          TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-OMRON',       'Omron',            'Sensors and PLC systems',                  TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-MITSU',       'Mitsubishi',       'Industrial automation systems',            TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-DELTA',       'Delta',            'VFD and automation products',              TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Pneumatics
('BRD-SMC',         'SMC',              'Pneumatic equipment',                      TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-FESTO',       'Festo',            'Pneumatic and automation systems',         TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Textile Machinery
('BRD-RIETER',      'Rieter',           'Spinning machinery',                       TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-TRUTZSCHLER', 'Trutzschler',      'Blowroom and carding machinery',           TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-SAURER',      'Saurer',           'Spinning and twisting machinery',          TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-MURATA',      'Murata',           'Automatic winding machines',               TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-LMW',         'LMW',              'Lakshmi Machine Works textile machines',   TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Tools
('BRD-BOSCH',       'Bosch',            'Bosch tools and parts',                    TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-MAKITA',      'Makita',           'Power tools',                              TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-DEWALT',      'DeWalt',           'Industrial tools',                         TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Electronics
('BRD-SAMSUNG',     'Samsung',          'Samsung electronics',                      TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-LG',          'LG',               'LG electronics',                           TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Lab Equipment
('BRD-USTER',       'Uster',            'Textile testing instruments',              TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-MESDAN',      'Mesdan',           'Laboratory testing equipment',             TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Chemicals
('BRD-ARCHROMA',    'Archroma',         'Textile chemicals',                        TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-HUNTSMAN',    'Huntsman',         'Textile dyes and chemicals',               TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-DYSTAR',      'DyStar',           'Textile dyes and auxiliaries',             TRUE, 1, NOW(), NOW(), 'system', 'system'),

-- Packaging
('BRD-3M',          '3M',               'Industrial tapes and packaging',           TRUE, 1, NOW(), NOW(), 'system', 'system'),
('BRD-TESA',        'Tesa',             'Industrial adhesive products',             TRUE, 1, NOW(), NOW(), 'system', 'system')

ON CONFLICT ON CONSTRAINT uq_brand_org_code DO NOTHING;


-- =============================================================================
-- 3. ITEM MODELS  (depends on brands)
-- =============================================================================
-- =============================================================================
-- SKF
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
         CROSS JOIN (
    VALUES
        ('6203-2RS','6203-2RS','SKF Bearing'),
        ('6204-2RS','6204-2RS','SKF Bearing'),
        ('6205-2RS','6205-2RS','SKF Bearing'),
        ('6206-2RS','6206-2RS','SKF Bearing'),
        ('6207-2RS','6207-2RS','SKF Bearing'),
        ('6305-2RS','6305-2RS','SKF Bearing'),
        ('6306-2RS','6306-2RS','SKF Bearing')
) v(model_code,model_name,description)
WHERE b.brand_code='BRD-SKF'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- =============================================================================
-- SAMSUNG
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
         CROSS JOIN (
    VALUES
        ('GS24','Galaxy S24','Samsung Smartphone'),
        ('GS24P','Galaxy S24 Plus','Samsung Smartphone'),
        ('GS24U','Galaxy S24 Ultra','Samsung Smartphone'),
        ('A55','Galaxy A55','Samsung Smartphone'),
        ('A35','Galaxy A35','Samsung Smartphone'),
        ('M35','Galaxy M35','Samsung Smartphone')
) v(model_code,model_name,description)
WHERE b.brand_code='BRD-SAMSUNG'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- =============================================================================
-- LG
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
         CROSS JOIN (
    VALUES
        ('AC18INV','Dual Inverter 1.5 Ton','Air Conditioner'),
        ('AC24INV','Dual Inverter 2 Ton','Air Conditioner'),
        ('WM8KG','Front Load 8KG','Washing Machine'),
        ('REF300L','300L Refrigerator','Refrigerator')
) v(model_code,model_name,description)
WHERE b.brand_code='BRD-LG'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- =============================================================================
-- ABB
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
         CROSS JOIN (
    VALUES
        ('M2BAX90L','3 HP Motor','ABB Motor'),
        ('M2BAX100L','5 HP Motor','ABB Motor'),
        ('M2BAX112M','7.5 HP Motor','ABB Motor')
) v(model_code,model_name,description)
WHERE b.brand_code='BRD-ABB'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- =============================================================================
-- SIEMENS
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
         CROSS JOIN (
    VALUES
        ('S71200','S7-1200 PLC','PLC'),
        ('S71500','S7-1500 PLC','PLC'),
        ('MM420','Micromaster 420','VFD'),
        ('MM440','Micromaster 440','VFD')
) v(model_code,model_name,description)
WHERE b.brand_code='BRD-SIEMENS'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- =============================================================================
-- SCHNEIDER
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
         CROSS JOIN (
    VALUES
        ('ATV320','ATV320','VFD'),
        ('ATV630','ATV630','VFD'),
        ('M221','Modicon M221','PLC'),
        ('M241','Modicon M241','PLC')
) v(model_code,model_name,description)
WHERE b.brand_code='BRD-SCHNEIDER'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- =============================================================================
-- OMRON
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
         CROSS JOIN (
    VALUES
        ('CP1E','CP1E PLC','PLC'),
        ('CP1H','CP1H PLC','PLC'),
        ('E3Z','E3Z Sensor','Sensor'),
        ('E2E','E2E Sensor','Sensor')
) v(model_code,model_name,description)
WHERE b.brand_code='BRD-OMRON'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;

-- =============================================================================
-- DELTA
-- =============================================================================
INSERT INTO inv_item_models
(model_code, model_name, description, is_active, organization_id, brand_id, created_at, updated_at, created_by, updated_by)
SELECT v.model_code, v.model_name, v.description, TRUE, 1, b.id, NOW(), NOW(), 'system', 'system'
FROM inv_item_brands b
         CROSS JOIN (
    VALUES
        ('MS300','MS300','VFD'),
        ('CP2000','CP2000','VFD'),
        ('DVP14SS','DVP14SS2 PLC','PLC'),
        ('DVP28SV','DVP28SV PLC','PLC')
) v(model_code,model_name,description)
WHERE b.brand_code='BRD-DELTA'
ON CONFLICT ON CONSTRAINT uq_model_org_brand_code DO NOTHING;



INSERT INTO inv_item_uom
(active, category, code, conversion_factor, is_base_unit, name, organization_id, symbol, created_at, updated_at)
VALUES

-- WEIGHT
(true,'WEIGHT','KG',1.000000,true,'Kilogram',1,'kg',NOW(),NOW()),
(true,'WEIGHT','GM',0.001000,false,'Gram',1,'g',NOW(),NOW()),
(true,'WEIGHT','MG',0.000001,false,'Milligram',1,'mg',NOW(),NOW()),
(true,'WEIGHT','MT',1000.000000,false,'Metric Ton',1,'mt',NOW(),NOW()),
(true,'WEIGHT','LB',0.453592,false,'Pound',1,'lb',NOW(),NOW()),
(true,'WEIGHT','OZ',0.028349,false,'Ounce',1,'oz',NOW(),NOW()),

-- COUNT
(true,'COUNT','PCS',1.000000,true,'Piece',1,'pcs',NOW(),NOW()),
(true,'COUNT','DOZ',12.000000,false,'Dozen',1,'doz',NOW(),NOW()),
(true,'COUNT','PAIR',2.000000,false,'Pair',1,'pair',NOW(),NOW()),
(true,'COUNT','SET',1.000000,false,'Set',1,'set',NOW(),NOW()),

-- LENGTH
(true,'LENGTH','MTR',1.000000,true,'Meter',1,'m',NOW(),NOW()),
(true,'LENGTH','CM',0.010000,false,'Centimeter',1,'cm',NOW(),NOW()),
(true,'LENGTH','MM',0.001000,false,'Millimeter',1,'mm',NOW(),NOW()),
(true,'LENGTH','FT',0.304800,false,'Feet',1,'ft',NOW(),NOW()),
(true,'LENGTH','IN',0.025400,false,'Inch',1,'in',NOW(),NOW()),
(true,'LENGTH','YD',0.914400,false,'Yard',1,'yd',NOW(),NOW()),

-- AREA
(true,'AREA','SQM',1.000000,true,'Square Meter',1,'sqm',NOW(),NOW()),
(true,'AREA','SQFT',0.092903,false,'Square Feet',1,'sqft',NOW(),NOW()),

-- VOLUME
(true,'VOLUME','LTR',1.000000,true,'Liter',1,'ltr',NOW(),NOW()),
(true,'VOLUME','ML',0.001000,false,'Milliliter',1,'ml',NOW(),NOW()),
(true,'VOLUME','GAL',3.785410,false,'Gallon',1,'gal',NOW(),NOW()),

-- PACKAGING
(true,'PACKING','PKT',1.000000,true,'Packet',1,'pkt',NOW(),NOW()),
(true,'PACKING','BOX',1.000000,false,'Box',1,'box',NOW(),NOW()),
(true,'PACKING','CTN',1.000000,false,'Carton',1,'ctn',NOW(),NOW()),
(true,'PACKING','BAG',1.000000,false,'Bag',1,'bag',NOW(),NOW()),
(true,'PACKING','SACK',1.000000,false,'Sack',1,'sack',NOW(),NOW()),
(true,'PACKING','BOTTLE',1.000000,false,'Bottle',1,'btl',NOW(),NOW()),
(true,'PACKING','CAN',1.000000,false,'Can',1,'can',NOW(),NOW()),
(true,'PACKING','JAR',1.000000,false,'Jar',1,'jar',NOW(),NOW()),
(true,'PACKING','TIN',1.000000,false,'Tin',1,'tin',NOW(),NOW()),
(true,'PACKING','DRUM',1.000000,false,'Drum',1,'drum',NOW(),NOW()),
(true,'PACKING','ROLL',1.000000,false,'Roll',1,'roll',NOW(),NOW()),
(true,'PACKING','BUNDLE',1.000000,false,'Bundle',1,'bundle',NOW(),NOW()),
(true,'PACKING','TRAY',1.000000,false,'Tray',1,'tray',NOW(),NOW()),
(true,'PACKING','CASE',1.000000,false,'Case',1,'case',NOW(),NOW()),
(true,'PACKING','TUBE',1.000000,false,'Tube',1,'tube',NOW(),NOW()),

-- GENERIC UNIT
(true,'UNIT','UNIT',1.000000,true,'Unit',1,'unit',NOW(),NOW());



-- =============================================================================
-- 2. CATEGORIES  (ROOT -> GROUP -> ITEM)
-- =============================================================================

-- =============================================================================
-- RAW MATERIAL GROUPS
-- =============================================================================

INSERT INTO inv_item_categories
(
    category_code,
    category_name,
    description,
    is_active,
    item_type,
    layer_type,
    organization_id,
    parent_category_id,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    v.code,
    v.name,
    v.description,
    TRUE,
    v.item_type,
    'ROOT',
    1,
    NULL,
    NOW(),
    NOW(),
    'system',
    'system'
FROM (
         VALUES
             ('CAT-RAW','Raw Materials','Raw material root category','RAW_MATERIAL'),
             ('CAT-FG','Finished Goods','Finished goods root category','FINISHED_GOOD'),
             ('CAT-SPARE','Spare Parts','Spare parts root category','SPARE_PART'),
             ('CAT-CONS','Consumables','Consumables root category','CONSUMABLE')
     ) v(code,name,description,item_type)
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code,
    v.name,
    v.description,
    TRUE,
    'RAW_MATERIAL',
    'GROUP',
    1,
    p.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-RAW-FOOD','Food Ingredients','Food raw materials'),
        ('CAT-RAW-CHEM','Chemicals','Industrial chemicals'),
        ('CAT-RAW-PACK','Packaging Materials','Packaging raw materials'),
        ('CAT-RAW-TEXT','Textile Materials','Textile raw materials'),
        ('CAT-RAW-METAL','Metal Materials','Metal raw materials'),
        ('CAT-RAW-PLAS','Plastic Materials','Plastic raw materials'),
        ('CAT-RAW-PAPER','Paper Materials','Paper raw materials')
) v(code,name,description)
WHERE p.category_code='CAT-RAW'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- FINISHED GOODS GROUPS
-- =============================================================================

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code,
    v.name,
    v.description,
    TRUE,
    'FINISHED_GOOD',
    'GROUP',
    1,
    p.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-GROC','Grocery','Grocery products'),
        ('CAT-FG-BEV','Beverages','Beverage products'),
        ('CAT-FG-ELEC','Electronics','Electronic products'),
        ('CAT-FG-FASH','Fashion','Fashion products'),
        ('CAT-FG-COS','Cosmetics','Cosmetics and beauty products'),
        ('CAT-FG-HOME','Home & Kitchen','Home and kitchen products'),
        ('CAT-FG-STAT','Stationery','Stationery products'),
        ('CAT-FG-PHAR','Pharmacy','Medicine and healthcare products'),
        ('CAT-FG-AUTO','Automotive','Automotive products'),
        ('CAT-FG-HARD','Hardware','Hardware and tools'),
        ('CAT-FG-SPORT','Sports & Toys','Sports and toy products')
) v(code,name,description)
WHERE p.category_code='CAT-FG'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- SPARE PART GROUPS
-- =============================================================================

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code,
    v.name,
    v.description,
    TRUE,
    'SPARE_PART',
    'GROUP',
    1,
    p.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-SPARE-MECH','Mechanical Spares','Mechanical spare parts'),
        ('CAT-SPARE-ELEC','Electrical Spares','Electrical spare parts'),
        ('CAT-SPARE-AUTO','Automotive Spares','Vehicle spare parts'),
        ('CAT-SPARE-IT','IT Spares','Computer and IT spare parts')
) v(code,name,description)
WHERE p.category_code='CAT-SPARE'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- CONSUMABLE GROUPS
-- =============================================================================

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code,
    v.name,
    v.description,
    TRUE,
    'CONSUMABLE',
    'GROUP',
    1,
    p.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-CONS-PACK','Packaging Consumables','Packaging consumables'),
        ('CAT-CONS-CLEAN','Cleaning Supplies','Cleaning consumables'),
        ('CAT-CONS-OFF','Office Supplies','Office consumables')
) v(code,name,description)
WHERE p.category_code='CAT-CONS'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- FIXED ASSET GROUPS
-- =============================================================================

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code,
    v.name,
    v.description,
    TRUE,
    'FIXED_ASSET',
    'GROUP',
    1,
    p.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-ASSET-COMP','Computers','Computer assets'),
        ('CAT-ASSET-FURN','Furniture','Furniture assets'),
        ('CAT-ASSET-VEH','Vehicles','Vehicle assets'),
        ('CAT-ASSET-MACH','Machinery','Machinery assets'),
        ('CAT-ASSET-BUILD','Buildings','Building assets')
) v(code,name,description)
WHERE p.category_code='CAT-ASSET'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- SERVICE GROUPS
-- =============================================================================

INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code,
    v.name,
    v.description,
    TRUE,
    'SERVICE',
    'GROUP',
    1,
    p.id,
    NOW(),
    NOW(),
    'system',
    'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-SERV-REPAIR','Repair Service','Repair services'),
        ('CAT-SERV-TRANSPORT','Transport Service','Transport services'),
        ('CAT-SERV-CONSULT','Consultancy Service','Consultancy services'),
        ('CAT-SERV-INSTALL','Installation Service','Installation services'),
        ('CAT-SERV-MAINT','Maintenance Service','Maintenance services')
) v(code,name,description)
WHERE p.category_code='CAT-SERV'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;




-- =============================================================================
-- GROCERY ITEMS
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-GROC-RICE','Rice'),
        ('CAT-FG-GROC-FLOUR','Flour'),
        ('CAT-FG-GROC-SUGAR','Sugar'),
        ('CAT-FG-GROC-SALT','Salt'),
        ('CAT-FG-GROC-OIL','Cooking Oil'),
        ('CAT-FG-GROC-DAL','Lentils'),
        ('CAT-FG-GROC-SPICE','Spices'),
        ('CAT-FG-GROC-BISC','Biscuits'),
        ('CAT-FG-GROC-SNACK','Snacks'),
        ('CAT-FG-GROC-NOOD','Noodles'),
        ('CAT-FG-GROC-MILK','Milk Products')
) v(code,name)
WHERE p.category_code='CAT-FG-GROC'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- BEVERAGES
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-BEV-WATER','Mineral Water'),
        ('CAT-FG-BEV-SOFT','Soft Drinks'),
        ('CAT-FG-BEV-JUICE','Juice'),
        ('CAT-FG-BEV-TEA','Tea'),
        ('CAT-FG-BEV-COFFEE','Coffee'),
        ('CAT-FG-BEV-ENERGY','Energy Drinks')
) v(code,name)
WHERE p.category_code='CAT-FG-BEV'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- ELECTRONICS
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-ELEC-MOB','Mobile Phones'),
        ('CAT-FG-ELEC-LAP','Laptops'),
        ('CAT-FG-ELEC-TAB','Tablets'),
        ('CAT-FG-ELEC-TV','Televisions'),
        ('CAT-FG-ELEC-PRN','Printers'),
        ('CAT-FG-ELEC-NET','Networking Devices'),
        ('CAT-FG-ELEC-CAM','CCTV & Cameras'),
        ('CAT-FG-ELEC-ACC','Accessories')
) v(code,name)
WHERE p.category_code='CAT-FG-ELEC'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- FASHION
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-FASH-MENS','Mens Wear'),
        ('CAT-FG-FASH-WMNS','Womens Wear'),
        ('CAT-FG-FASH-KIDS','Kids Wear'),
        ('CAT-FG-FASH-SHOE','Shoes'),
        ('CAT-FG-FASH-BAG','Bags'),
        ('CAT-FG-FASH-ACC','Fashion Accessories')
) v(code,name)
WHERE p.category_code='CAT-FG-FASH'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- COSMETICS
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-COS-SOAP','Soap'),
        ('CAT-FG-COS-SHAM','Shampoo'),
        ('CAT-FG-COS-CREAM','Cream'),
        ('CAT-FG-COS-PERF','Perfume'),
        ('CAT-FG-COS-MAKEUP','Makeup'),
        ('CAT-FG-COS-HAIR','Hair Care')
) v(code,name)
WHERE p.category_code='CAT-FG-COS'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- HOME & KITCHEN
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-HOME-KITCH','Kitchenware'),
        ('CAT-FG-HOME-COOK','Cookware'),
        ('CAT-FG-HOME-FURN','Furniture'),
        ('CAT-FG-HOME-DECO','Home Decor')
) v(code,name)
WHERE p.category_code='CAT-FG-HOME'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- STATIONERY
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-STAT-PEN','Pens'),
        ('CAT-FG-STAT-PAPER','Paper'),
        ('CAT-FG-STAT-NOTE','Notebooks'),
        ('CAT-FG-STAT-OFFICE','Office Supplies')
) v(code,name)
WHERE p.category_code='CAT-FG-STAT'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- PHARMACY
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-PHAR-MED','Medicine'),
        ('CAT-FG-PHAR-SURG','Surgical'),
        ('CAT-FG-PHAR-HCARE','Healthcare'),
        ('CAT-FG-PHAR-SUPP','Supplements')
) v(code,name)
WHERE p.category_code='CAT-FG-PHAR'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- AUTOMOTIVE
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-AUTO-BATT','Battery'),
        ('CAT-FG-AUTO-TYRE','Tyres'),
        ('CAT-FG-AUTO-OIL','Lubricants'),
        ('CAT-FG-AUTO-FLTR','Filters')
) v(code,name)
WHERE p.category_code='CAT-FG-AUTO'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;


-- =============================================================================
-- HARDWARE
-- =============================================================================
INSERT INTO inv_item_categories
(category_code, category_name, description, is_active, item_type, layer_type,
 organization_id, parent_category_id, created_at, updated_at, created_by, updated_by)
SELECT
    v.code, v.name, v.name,
    TRUE, 'FINISHED_GOOD', 'ITEM',
    1, p.id,
    NOW(), NOW(), 'system', 'system'
FROM inv_item_categories p
         CROSS JOIN (
    VALUES
        ('CAT-FG-HARD-TOOLS','Tools'),
        ('CAT-FG-HARD-FAST','Fasteners'),
        ('CAT-FG-HARD-PAINT','Paints'),
        ('CAT-FG-HARD-ELEC','Electrical Hardware'),
        ('CAT-FG-HARD-PIPE','Pipes & Fittings')
) v(code,name)
WHERE p.category_code='CAT-FG-HARD'
ON CONFLICT ON CONSTRAINT uq_icat_org_code DO NOTHING;






-- =============================================================================
-- ITEM 1 : Samsung Galaxy A35
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-A35-001',
    'Samsung Galaxy A35',
    'Samsung Smartphone 128GB',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    28000,28000,34000,
    10,300,20,
    12,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-SAMSUNG'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='A35'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-MOB'
  AND c.organization_id=1
  AND b.organization_id=1
  AND m.organization_id=1
  AND u.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 2 : Samsung Galaxy M35
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-M35-001',
    'Samsung Galaxy M35',
    'Samsung Smartphone 5G',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    30000,30000,36500,
    10,250,20,
    12,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-SAMSUNG'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='M35'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-MOB'
  AND c.organization_id=1
  AND b.organization_id=1
  AND m.organization_id=1
  AND u.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 3 : LG AC 1.5 Ton
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-LG-AC18',
    'LG Dual Inverter AC 1.5 Ton',
    'Energy Saving Air Conditioner',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    55000,55000,65000,
    2,30,5,
    24,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-LG'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='AC18INV'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-ACC'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 4 : LG AC 2 Ton
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-LG-AC24',
    'LG Dual Inverter AC 2 Ton',
    '2 Ton Air Conditioner',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    68000,68000,79000,
    2,20,5,
    24,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-LG'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='AC24INV'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-ACC'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 5 : LG Washing Machine
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-LG-WM8KG',
    'LG Front Load Washing Machine 8KG',
    'Automatic Washing Machine',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    42000,42000,52000,
    2,20,5,
    24,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-LG'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='WM8KG'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-ACC'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 6 : LG Refrigerator
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 warranty_months,organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-LG-REF300',
    'LG Refrigerator 300L',
    'Double Door Refrigerator',
    'FINISHED_GOOD',
    TRUE,TRUE,FALSE,
    FALSE,TRUE,
    'PCS','PCS','PCS',
    48000,48000,59000,
    2,20,5,
    24,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-LG'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='REF300L'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-FG-ELEC-ACC'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 7 : SKF Bearing 6203
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-SKF-6203',
    'SKF Bearing 6203-2RS',
    'Deep Groove Ball Bearing',
    'SPARE_PART',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    350,350,450,
    50,500,100,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-SKF'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='6203-2RS'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-SPARE-MECH-BRG'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 8 : SKF Bearing 6204
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-SKF-6204',
    'SKF Bearing 6204-2RS',
    'Deep Groove Ball Bearing',
    'SPARE_PART',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    390,390,500,
    50,500,100,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-SKF'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='6204-2RS'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-SPARE-MECH-BRG'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 9 : SKF Bearing 6206
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-SKF-6206',
    'SKF Bearing 6206-2RS',
    'Deep Groove Ball Bearing',
    'SPARE_PART',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    520,520,680,
    40,400,80,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-SKF'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='6206-2RS'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-SPARE-MECH-BRG'
  AND c.organization_id=1
ON CONFLICT ON CONSTRAINT uq_item_org_code DO NOTHING;

-- =============================================================================
-- ITEM 10 : SKF Bearing 6305
-- =============================================================================
INSERT INTO inv_items
(item_code,item_name,description,item_type,is_active,is_approved,is_hazardous,
 has_lot_tracking,has_serial,unit_of_measure,purchase_unit_code,sales_unit_code,
 cost_price,standard_cost,unit_price,minimum_stock,maximum_stock,reorder_level,
 organization_id,category_id,brand_id,model_id,
 purchase_unit_id,sales_unit_id,operation_unit_id,
 created_at,updated_at,created_by,updated_by)
SELECT
    'ITM-SKF-6305',
    'SKF Bearing 6305-2RS',
    'Deep Groove Ball Bearing',
    'SPARE_PART',
    TRUE,TRUE,FALSE,
    FALSE,FALSE,
    'PCS','PCS','PCS',
    620,620,780,
    40,400,80,
    1,c.id,b.id,m.id,
    u.id,u.id,u.id,
    NOW(),NOW(),'system','system'
FROM inv_item_categories c
         JOIN inv_item_brands b ON b.brand_code='BRD-SKF'
         JOIN inv_item_models m ON m.brand_id=b.id AND m.model_code='6305-2RS'
         JOIN inv_item_uom u ON u.code='PCS'
WHERE c.category_code='CAT-SPARE-MECH-BRG'
  AND c.organization_id=1
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