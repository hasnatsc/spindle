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
INSERT INTO stp_banks
(bank_code, bank_name, bank_name_local, short_name, bank_type, bank_category,
 swift_code, routing_number_prefix, central_bank_code,
 head_office_address, head_office_city, head_office_country, head_office_phone, head_office_email, website,
 rating, is_active, organization_id,
 supports_lc, supports_import_lc, supports_export_lc, supports_inland_lc, supports_btb_lc, supports_online_banking,
 correspondent_bank_name, correspondent_swift_code, correspondent_account_number,
 created_at, updated_at, created_by, updated_by)
VALUES
    ('DBBL', 'Dutch-Bangla Bank Ltd.', 'ডাচ্-বাংলা ব্যাংক লিমিটেড', 'DBBL', 'PRIVATE_COMMERCIAL', 'SCHEDULED',
     'DBBLBDDH', '090260', 'BB-090',
     'Sena Kalyan Bhaban, 195, Motijheel C/A', 'Dhaka', 'Bangladesh', '+88029566699', 'info@dutchbanglabank.com', 'https://www.dutchbanglabank.com',
     'AA', true, 1,
     true, true, true, true, true, true,
     NULL, NULL, NULL,
     NOW(), NOW(), 'system', 'system'),

    ('EBL', 'Eastern Bank Ltd.', 'ইস্টার্ন ব্যাংক লিমিটেড', 'EBL', 'PRIVATE_COMMERCIAL', 'SCHEDULED',
     'EBLDBDDH', '090270', 'BB-095',
     'Jiban Bima Bhaban, 10 Dilkusha C/A', 'Dhaka', 'Bangladesh', '+88029553940', 'info@ebl-bd.com', 'https://www.ebl.com.bd',
     'AA+', true, 1,
     true, true, true, true, false, true,
     NULL, NULL, NULL,
     NOW(), NOW(), 'system', 'system'),

    ('SCB', 'Standard Chartered Bank', 'স্ট্যান্ডার্ড চার্টার্ড ব্যাংক', 'SCB', 'FOREIGN_COMMERCIAL', 'SCHEDULED',
     'SCBLBDDX', '090180', 'BB-010',
     '67 Gulshan Avenue', 'Dhaka', 'Bangladesh', '+88029883301', 'info.bd@sc.com', 'https://www.sc.com/bd',
     'AAA', true, 1,
     true, true, true, true, true, true,
     'Standard Chartered Bank, New York', 'SCBLUS33', '3582013247001',
     NOW(), NOW(), 'system', 'system'),

    ('SONALI', 'Sonali Bank Ltd.', 'সোনালী ব্যাংক লিমিটেড', 'SONALI', 'STATE_OWNED', 'SCHEDULED',
     'BSONBDDH', '200261', 'BB-001',
     '35-42, Motijheel C/A', 'Dhaka', 'Bangladesh', '+88029560000', 'info@sonalibank.com.bd', 'https://www.sonalibank.com.bd',
     'A', true, 1,
     true, true, false, true, false, false,
     NULL, NULL, NULL,
     NOW(), NOW(), 'system', 'system'),

    ('BB', 'Bangladesh Bank', 'বাংলাদেশ ব্যাংক', 'BB', 'CENTRAL_BANK', 'CENTRAL',
     'BBHOBDDH', NULL, 'BB-000',
     'Motijheel C/A', 'Dhaka', 'Bangladesh', '+88029530070', 'bb@bb.org.bd', 'https://www.bb.org.bd',
     NULL, true, 1,
     false, false, false, false, false, false,
     NULL, NULL, NULL,
     NOW(), NOW(), 'system', 'system')
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