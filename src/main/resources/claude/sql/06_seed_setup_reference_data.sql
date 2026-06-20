-- =============================================================================
--  Optimum ERP — Setup / Reference Data Seed
--  File   : 06_seed_setup_reference_data.sql
--  Target : PostgreSQL (stp_banks, stp_currencies, stp_document_sequences,
--           stp_location_countries, stp_terms_master)
--
--  NOTE: assumes organization_id = 1 already seeded.
--
--  Execution order:
--    1. stp_currencies          (no FKs)
--    2. stp_banks                (organization scoped, no FKs to currency)
--    3. stp_location_countries   (depends on stp_currencies)
--    4. stp_document_sequences   (organization scoped, no FKs)
--    5. stp_terms_master         (no FKs)
--
--  Idempotent via ON CONFLICT DO NOTHING on unique constraints.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. CURRENCIES
-- =============================================================================
INSERT INTO stp_currencies (code, name, symbol, decimal_places, active)
VALUES
    ('BDT', 'Bangladeshi Taka', '৳', 2, true),
    ('USD', 'US Dollar',        '$', 2, true),
    ('EUR', 'Euro',              '€', 2, true),
    ('GBP', 'British Pound',     '£', 2, true),
    ('INR', 'Indian Rupee',      '₹', 2, true),
    ('CNY', 'Chinese Yuan',      '¥', 2, true),
    ('SGD', 'Singapore Dollar',  'S$', 2, true),
    ('AED', 'UAE Dirham',        'د.إ', 2, true),
    ('SAR', 'Saudi Riyal',       '﷼', 2, true),
    ('JPY', 'Japanese Yen',      '¥', 0, true)
    ON CONFLICT ON CONSTRAINT uk2jg2r4qbrdlvbbgvwm92kybxf DO NOTHING;


-- =============================================================================
-- 2. BANKS
-- =============================================================================
INSERT INTO stp_banks ( bank_code, bank_name, short_name, bank_type, bank_category, rating, is_active, organization_id, supports_lc, supports_import_lc, supports_export_lc, supports_inland_lc, supports_btb_lc, supports_online_banking, created_at, updated_at, created_by, updated_by)
VALUES
-- State Owned
('SONALI','Sonali Bank PLC','SONALI','STATE_OWNED','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('JANATA','Janata Bank PLC','JANATA','STATE_OWNED','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('AGRANI','Agrani Bank PLC','AGRANI','STATE_OWNED','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('RUPALI','Rupali Bank PLC','RUPALI','STATE_OWNED','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('BASIC','BASIC Bank PLC','BASIC','STATE_OWNED','SCHEDULED','AVERAGE',true,1,true,true,false,true,false,true,NOW(),NOW(),'system','system'),

-- Specialized
('BKB','Bangladesh Krishi Bank','BKB','SPECIALIZED','SPECIALIZED','GOOD',true,1,false,false,false,false,false,true,NOW(),NOW(),'system','system'),
('RAKUB','Rajshahi Krishi Unnayan Bank','RAKUB','SPECIALIZED','SPECIALIZED','GOOD',true,1,false,false,false,false,false,true,NOW(),NOW(),'system','system'),
('PKB','Probashi Kallyan Bank','PKB','SPECIALIZED','SPECIALIZED','GOOD',true,1,false,false,false,false,false,true,NOW(),NOW(),'system','system'),
('BDBL','Bangladesh Development Bank PLC','BDBL','DEVELOPMENT','SPECIALIZED','GOOD',true,1,false,false,false,false,false,true,NOW(),NOW(),'system','system'),

-- Private Commercial
('ABBL','AB Bank PLC','ABBL','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('BANKASIA','Bank Asia PLC','BANKASIA','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('BRAC','BRAC Bank PLC','BRAC','PRIVATE','SCHEDULED','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('CITY','The City Bank PLC','CITY','PRIVATE','SCHEDULED','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('DBBL','Dutch-Bangla Bank PLC','DBBL','PRIVATE','SCHEDULED','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('EBL','Eastern Bank PLC','EBL','PRIVATE','SCHEDULED','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('IFIC','IFIC Bank PLC','IFIC','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('JBL','Jamuna Bank PLC','JBL','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('DBL','Dhaka Bank PLC','DBL','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('MBL','Mercantile Bank PLC','MBL','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('MIDLAND','Midland Bank PLC','MIDLAND','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('MTB','Mutual Trust Bank PLC','MTB','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('NBL','National Bank PLC','NBL','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('NCC','NCC Bank PLC','NCC','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('NRBC','NRBC Bank PLC','NRBC','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('ONE','ONE Bank PLC','ONE','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('PRIME','Prime Bank PLC','PRIME','PRIVATE','SCHEDULED','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('PUBALI','Pubali Bank PLC','PUBALI','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('SEBL','Southeast Bank PLC','SEBL','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('TRUST','Trust Bank PLC','TRUST','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('UCB','United Commercial Bank PLC','UCB','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('COMMUNITY','Community Bank Bangladesh PLC','COMMUNITY','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('BENGAL','Bengal Commercial Bank PLC','BENGAL','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('SBAC','SBAC Bank PLC','SBAC','PRIVATE','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),

-- Islamic
('IBBL','Islami Bank Bangladesh PLC','IBBL','ISLAMIC','SCHEDULED','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('EXIM','EXIM Bank PLC','EXIM','ISLAMIC','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('FSIBL','First Security Islami Bank PLC','FSIBL','ISLAMIC','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('GIB','Global Islami Bank PLC','GIB','ISLAMIC','SCHEDULED','AVERAGE',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('SIBL','Social Islami Bank PLC','SIBL','ISLAMIC','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('SJIBL','Shahjalal Islami Bank PLC','SJIBL','ISLAMIC','SCHEDULED','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),

-- Foreign
('SCB','Standard Chartered Bank','SCB','FOREIGN','FOREIGN','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('HSBC','HSBC Bangladesh','HSBC','FOREIGN','FOREIGN','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('CITI','Citibank N.A.','CITI','FOREIGN','FOREIGN','EXCELLENT',true,1,true,true,true,true,true,true,NOW(),NOW(),'system','system'),
('SBI','State Bank of India','SBI','FOREIGN','FOREIGN','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('WOORI','Woori Bank','WOORI','FOREIGN','FOREIGN','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('HBL','Habib Bank Limited','HBL','FOREIGN','FOREIGN','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system'),
('NBP','National Bank of Pakistan','NBP','FOREIGN','FOREIGN','GOOD',true,1,true,true,true,true,false,true,NOW(),NOW(),'system','system')

ON CONFLICT ON CONSTRAINT uq_bank_org_code DO NOTHING;


-- =============================================================================
-- 3. LOCATION COUNTRIES  (depends on stp_currencies)
-- =============================================================================
INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'BGD', 'BD', 'Bangladesh', 'বাংলাদেশ', '+880', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'BDT'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'USA', 'US', 'United States', 'United States', '+1', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'USD'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'GBR', 'GB', 'United Kingdom', 'United Kingdom', '+44', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'GBP'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'IND', 'IN', 'India', 'भारत', '+91', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'INR'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'CHN', 'CN', 'China', '中国', '+86', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'CNY'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'SGP', 'SG', 'Singapore', 'Singapore', '+65', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'SGD'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'ARE', 'AE', 'United Arab Emirates', 'الإمارات العربية المتحدة', '+971', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'AED'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'SAU', 'SA', 'Saudi Arabia', 'المملكة العربية السعودية', '+966', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'SAR'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'JPN', 'JP', 'Japan', '日本', '+81', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'JPY'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;

INSERT INTO stp_location_countries
(iso_code, iso_code2, name, name_native, phone_code, active, currency_id, created_at)
SELECT 'DEU', 'DE', 'Germany', 'Deutschland', '+49', true, c.id, NOW()
FROM stp_currencies c WHERE c.code = 'EUR'
    ON CONFLICT ON CONSTRAINT ukag7uy3idk68huoxeu1tdk5t5j DO NOTHING;


-- =============================================================================
-- 4. DOCUMENT SEQUENCES  (one row per voucher-type prefix per year)
-- =============================================================================
INSERT INTO stp_document_sequences (organization_id, prefix, year_code, last_seq, created_at, updated_at)
VALUES
    (1, 'SAL-',  '2025-26', 0, NOW(), NOW()),
    (1, 'PUR-',  '2025-26', 0, NOW(), NOW()),
    (1, 'JV-',   '2025-26', 0, NOW(), NOW()),
    (1, 'GRN-',  '2025-26', 0, NOW(), NOW()),
    (1, 'INV-',  '2025-26', 0, NOW(), NOW()),
    (1, 'PO-',   '2025-26', 0, NOW(), NOW()),
    (1, 'SO-',   '2025-26', 0, NOW(), NOW()),
    (1, 'DN-',   '2025-26', 0, NOW(), NOW()),
    (1, 'CN-',   '2025-26', 0, NOW(), NOW()),
    (1, 'LC-',   '2025-26', 0, NOW(), NOW())
    ON CONFLICT ON CONSTRAINT uq_docseq_org_prefix_year DO NOTHING;


-- =============================================================================
-- 5. TERMS MASTER  (payment/delivery/LC terms templates)
-- =============================================================================
INSERT INTO stp_terms_master (title, description, document_type, is_active, is_default, sort_order)
VALUES
    ('Net 30',          'Payment due within 30 days of invoice date',            'PAYMENT_TERM', true, true,  10),
    ('Net 60',          'Payment due within 60 days of invoice date',            'PAYMENT_TERM', true, false, 20),
    ('Cash on Delivery', 'Payment due immediately upon delivery',                'PAYMENT_TERM', true, false, 30),
    ('Advance Payment',  '100% payment required in advance',                     'PAYMENT_TERM', true, false, 40),
    ('50% Advance, 50% on Delivery', 'Half payment in advance, remainder on delivery', 'PAYMENT_TERM', true, false, 50),

    ('FOB',             'Free on Board — seller delivers goods on board the vessel', 'DELIVERY_TERM', true, true,  10),
    ('CIF',             'Cost, Insurance and Freight included in price',         'DELIVERY_TERM', true, false, 20),
    ('EXW',             'Ex Works — buyer bears all transportation costs',       'DELIVERY_TERM', true, false, 30),
    ('DDP',             'Delivered Duty Paid — seller bears all costs and risks','DELIVERY_TERM', true, false, 40),

    ('At Sight',        'LC payable immediately upon presentation of documents', 'LC_TERM', true, true,  10),
    ('Usance 90 Days',  'LC payable 90 days after presentation of documents',    'LC_TERM', true, false, 20),
    ('Usance 180 Days', 'LC payable 180 days after presentation of documents',   'LC_TERM', true, false, 30),

    ('Standard Warranty 12 Months', 'Standard 12-month manufacturer warranty',  'WARRANTY_TERM', true, true, 10),
    ('Extended Warranty 24 Months', 'Extended 24-month warranty coverage',      'WARRANTY_TERM', true, false, 20)
;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'Currencies', COUNT(*) FROM stp_currencies
-- UNION ALL SELECT 'Banks',     COUNT(*) FROM stp_banks
-- UNION ALL SELECT 'Countries', COUNT(*) FROM stp_location_countries
-- UNION ALL SELECT 'Doc Seq',   COUNT(*) FROM stp_document_sequences
-- UNION ALL SELECT 'Terms',     COUNT(*) FROM stp_terms_master;