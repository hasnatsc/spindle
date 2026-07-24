-- =============================================================================
--  Spindle ERP  —  Travel Module Menu + Permission + Role Seed  v1.0 (Phase 2)
--  File   : V103__seed_travel_phase2_menu_permission.sql
--  Target : PostgreSQL
--  Depends on: V101__seed_travel_menu_permission.sql (Phase 1 seed — MOD_TRAVEL,
--              ROLE_TRAVEL_MANAGER/EXECUTIVE must already exist)
--              and V8__travel_schema.sql (travel module schema)
--
--  Covers:
--    Permissions  — package, tour, visa entities
--    Menus        — 2 new GROUPs (Packages & Tours, Visa Services) → 3 LEAFs,
--                    appended under the existing MOD_TRAVEL module
--    Role-Perms   — extends both existing Travel roles
--    Role-Menus   — extends both existing Travel roles
--
--  Naming convention: identical to V101 (trv.<entity>.<action>, TRV_* menus)
--  Safe to re-run: all INSERTs use ON CONFLICT DO NOTHING.
--  RENUMBER this file to run immediately after your Phase 2 schema migration.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1. PERMISSIONS
-- ═════════════════════════════════════════════════════════════════════════════

-- ── Package (pre-built bundled itineraries) ──────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.package.view', 'View packages', '/travel/packages/**', 'GET', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.package.create', 'Create package', '/travel/packages/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.package.edit', 'Edit package', '/travel/packages/save', 'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.package.delete', 'Delete package', '/travel/packages/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Tour (day-trips / excursions) ─────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.tour.view', 'View tours', '/travel/tours/**', 'GET', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.tour.create', 'Create tour', '/travel/tours/save', 'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.tour.edit', 'Edit tour', '/travel/tours/save', 'POST', 'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.tour.delete', 'Delete tour', '/travel/tours/delete/**', 'DELETE', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- ── Visa Application ──────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, category, active, created_at,
                             updated_at)
VALUES ('trv.visa.view', 'View visa applications', '/travel/visa-applications/**', 'GET', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.visa.create', 'Create visa application', '/travel/visa-applications/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.visa.edit', 'Edit visa application', '/travel/visa-applications/save', 'POST', 'TRAVEL', 'TRAVEL',
        true, NOW(), NOW()),
       ('trv.visa.status', 'Update visa application status', '/travel/visa-applications/status/**', 'POST',
        'TRAVEL', 'TRAVEL', true, NOW(), NOW()),
       ('trv.visa.delete', 'Delete visa application', '/travel/visa-applications/delete/**', 'DELETE', 'TRAVEL',
        'TRAVEL', true, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 2. APP_MENUS  (new GROUPs appended to existing MOD_TRAVEL, plus their LEAFs)
-- ═════════════════════════════════════════════════════════════════════════════

-- Packages & Tours
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_PACKAGES', 'Packages & Tours', NULL, 'fa fa-suitcase-rolling', m.id, 40, 'GROUP', 'TRAVEL', NULL,
       '_self', true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- Visa Services
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'GRP_TRV_VISA', 'Visa Services', NULL, 'fa fa-passport', m.id, 50, 'GROUP', 'TRAVEL', NULL, '_self',
       true, true, false, NOW(), NOW()
FROM app_menus m WHERE m.menu_code = 'MOD_TRAVEL'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Packages & Tours leaves ───────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_PACKAGE', 'Packages', '/travel/packages', 'fa fa-suitcase-rolling', g.id, 10, 'LEAF', 'TRAVEL',
       'trv.package.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_PACKAGES'
ON CONFLICT (menu_code) DO NOTHING;
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_TOUR', 'Tours', '/travel/tours', 'fa fa-route', g.id, 20, 'LEAF', 'TRAVEL', 'trv.tour.view',
       '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_PACKAGES'
ON CONFLICT (menu_code) DO NOTHING;

-- ── Visa Services leaf ────────────────────────────────────────────────────────
INSERT INTO app_menus (menu_code, menu_name, menu_url, icon, parent_id, display_order, menu_type, module_name,
                       required_permission, target, active, visible, deleted, created_at, updated_at)
SELECT 'TRV_VISA', 'Visa Applications', '/travel/visa-applications', 'fa fa-passport', g.id, 10, 'LEAF', 'TRAVEL',
       'trv.visa.view', '_self', true, true, false, NOW(), NOW()
FROM app_menus g WHERE g.menu_code = 'GRP_TRV_VISA'
ON CONFLICT (menu_code) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 3. ROLE PERMISSIONS  (extend existing roles from V101)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_TRAVEL_MANAGER — full access to Packages, Tours, Visa
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'trv.package.view', 'trv.package.create', 'trv.package.edit',
                                              'trv.package.delete',
                                              'trv.tour.view', 'trv.tour.create', 'trv.tour.edit',
                                              'trv.tour.delete',
                                              'trv.visa.view', 'trv.visa.create', 'trv.visa.edit',
                                              'trv.visa.status', 'trv.visa.delete'
    )
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
ON CONFLICT DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE — view + create/edit, no delete
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sec_roles r
         JOIN sec_permissions p ON p.name IN (
                                              'trv.package.view',
                                              'trv.tour.view',
                                              'trv.visa.view', 'trv.visa.create', 'trv.visa.edit',
                                              'trv.visa.status'
    )
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
ON CONFLICT DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 4. ROLE MENUS  (extend existing roles from V101)
-- ═════════════════════════════════════════════════════════════════════════════

-- ROLE_TRAVEL_MANAGER — full create/edit/delete on the new leaves
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA'),
       m.menu_code IN ('TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA'),
       m.menu_code IN ('TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA'),
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_MANAGER'
  AND m.menu_code IN ('GRP_TRV_PACKAGES', 'GRP_TRV_VISA', 'TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA')
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- ROLE_TRAVEL_EXECUTIVE — view + create/edit, no delete
INSERT INTO sec_mrole_menus (role_id, menu_id, can_view, can_create, can_edit, can_delete, created_at, updated_at)
SELECT r.id,
       m.id,
       true,
       m.menu_code IN ('TRV_VISA'),
       m.menu_code IN ('TRV_VISA'),
       false,
       NOW(),
       NOW()
FROM sec_roles r
         CROSS JOIN app_menus m
WHERE r.name = 'ROLE_TRAVEL_EXECUTIVE'
  AND m.menu_code IN ('GRP_TRV_PACKAGES', 'GRP_TRV_VISA', 'TRV_PACKAGE', 'TRV_TOUR', 'TRV_VISA')
ON CONFLICT (role_id, menu_id) DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- REFERENCE DATA — common visa types (global, not org-scoped; edit/extend freely)
-- ═════════════════════════════════════════════════════════════════════════════

INSERT INTO trv_visa_types (country, visa_category, processing_days, fee_amount, currency, description)
SELECT * FROM (VALUES
    ('Saudi Arabia', 'Umrah Visa',      7,  8000.00, 'BDT', 'Single-entry Umrah pilgrimage visa'),
    ('Saudi Arabia', 'Tourist Visa',    5,  6000.00, 'BDT', 'eVisa, multiple entry, 1 year validity'),
    ('UAE',          'Tourist Visa',    3,  7500.00, 'BDT', '30-day single entry'),
    ('Thailand',     'Tourist Visa',    5,  3500.00, 'BDT', 'Single entry, 60-day stay'),
    ('Malaysia',     'Tourist Visa',    3,  2500.00, 'BDT', 'eVisa, single entry'),
    ('India',        'Tourist Visa',    3,  1200.00, 'BDT', 'eVisa, multiple entry'),
    ('Singapore',    'Tourist Visa',    5,  3000.00, 'BDT', 'Single entry, 30-day stay'),
    ('Schengen',     'Tourist Visa',   15, 12000.00, 'BDT', 'Short-stay Schengen visa, up to 90 days')
) AS v(country, visa_category, processing_days, fee_amount, currency, description)
WHERE NOT EXISTS (
    SELECT 1 FROM trv_visa_types t WHERE t.country = v.country AND t.visa_category = v.visa_category
);


-- ═════════════════════════════════════════════════════════════════════════════
-- VERIFICATION (uncomment to run counts after execution)
-- ═════════════════════════════════════════════════════════════════════════════
-- SELECT 'TRV Phase-2 Permissions' AS table_name, COUNT(*) AS total FROM sec_permissions
--             WHERE name LIKE 'trv.package.%' OR name LIKE 'trv.tour.%' OR name LIKE 'trv.visa.%'
-- UNION ALL SELECT 'TRV Phase-2 Menus', COUNT(*) FROM app_menus
--             WHERE menu_code IN ('GRP_TRV_PACKAGES','GRP_TRV_VISA','TRV_PACKAGE','TRV_TOUR','TRV_VISA')
-- UNION ALL SELECT 'TRV Visa Types', COUNT(*) FROM trv_visa_types;

COMMIT;





INSERT INTO trv_hotel_categories
(category_name, description, organization_id, created_at)
VALUES

    ('1 Star', 'Basic budget accommodation with limited facilities', 1, NOW()),

    ('2 Star', 'Budget hotel with essential amenities', 1, NOW()),

    ('3 Star', 'Standard hotel with comfortable rooms and services', 1, NOW()),

    ('4 Star', 'Premium hotel offering superior comfort and facilities', 1, NOW()),

    ('5 Star', 'Luxury hotel with world-class amenities and services', 1, NOW()),

    ('Boutique Hotel', 'Small luxury hotel with unique style and personalized service', 1, NOW()),

    ('Business Hotel', 'Hotel designed primarily for business travelers', 1, NOW()),

    ('Resort', 'Full-service resort with recreation and leisure facilities', 1, NOW()),

    ('Apartment Hotel', 'Serviced apartments for short and long stays', 1, NOW()),

    ('Villa', 'Private luxury villa accommodation', 1, NOW()),

    ('Guest House', 'Small family-operated accommodation', 1, NOW()),

    ('Hostel', 'Shared budget accommodation for travelers', 1, NOW()),

    ('Motel', 'Roadside accommodation for motorists', 1, NOW()),

    ('Eco Resort', 'Environmentally friendly accommodation', 1, NOW()),

    ('Heritage Hotel', 'Historic property converted into a hotel', 1, NOW()),

    ('Beach Resort', 'Hotel located near the beach with resort facilities', 1, NOW()),

    ('Mountain Resort', 'Resort located in hill or mountain areas', 1, NOW()),

    ('Airport Hotel', 'Hotel located near an airport for transit passengers', 1, NOW()),

    ('Transit Hotel', 'Short-stay hotel for travelers in transit', 1, NOW()),

    ('Luxury Resort', 'High-end resort with premium hospitality services', 1, NOW())

ON CONFLICT DO NOTHING;




INSERT INTO trv_hotels
(
    hotel_code,
    hotel_name,
    address,
    city,
    country,
    category_id,
    star_rating,
    contact_person,
    contact_phone,
    contact_email,
    is_active,
    organization_id,
    created_at
)
VALUES

-- Bangladesh
('HTL0001','Pan Pacific Sonargaon',
 '107 Kazi Nazrul Islam Avenue',
 'Dhaka','Bangladesh',
 5,5,
 'Sales Office',
 '+880255055000',
 'sales@panpacific.com',
 true,1,NOW()),

('HTL0002','InterContinental Dhaka',
 '1 Minto Road',
 'Dhaka','Bangladesh',
 5,5,
 'Reservation Desk',
 '+880255666000',
 'reservation.dhaka@ihg.com',
 true,1,NOW()),

('HTL0003','Sea Pearl Beach Resort',
 'Inani Beach',
 'Coxs Bazar','Bangladesh',
 5,5,
 'Reservation',
 '+880341000000',
 'info@seapearlbd.com',
 true,1,NOW()),

('HTL0004','Hotel The Cox Today',
 'Hotel Motel Zone',
 'Coxs Bazar','Bangladesh',
 4,4,
 'Reservations',
 '+880341111111',
 'reservation@coxtoday.com',
 true,1,NOW()),

('HTL0005','Grand Sultan Tea Resort',
 'Sreemangal',
 'Sylhet','Bangladesh',
 5,5,
 'Sales',
 '+880821000000',
 'info@grandsultanresort.com',
 true,1,NOW()),

-- UAE
('HTL0006','Atlantis The Palm',
 'Palm Jumeirah',
 'Dubai','UAE',
 5,5,
 'Reservations',
 '+97144260000',
 'reservations@atlantis.com',
 true,1,NOW()),

('HTL0007','Burj Al Arab',
 'Jumeirah Beach',
 'Dubai','UAE',
 5,5,
 'Reservations',
 '+97143017777',
 'reservation@jumeirah.com',
 true,1,NOW()),

-- Saudi Arabia
('HTL0008','Swissotel Makkah',
 'King Abdul Aziz Endowment',
 'Makkah','Saudi Arabia',
 5,5,
 'Reservations',
 '+966125718000',
 'reservations@swissotel.com',
 true,1,NOW()),

('HTL0009','Pullman ZamZam Madina',
 'Central Area',
 'Madinah','Saudi Arabia',
 5,5,
 'Reservations',
 '+966148210500',
 'reservation@accor.com',
 true,1,NOW()),

-- Thailand
('HTL0010','Amari Bangkok',
 'Phetchaburi Road',
 'Bangkok','Thailand',
 5,5,
 'Sales',
 '+6626539000',
 'reservation@amari.com',
 true,1,NOW()),

('HTL0011','Holiday Inn Pattaya',
 'Beach Road',
 'Pattaya','Thailand',
 4,4,
 'Reservations',
 '+6638725555',
 'reservation@ihg.com',
 true,1,NOW()),

-- Malaysia
('HTL0012','Berjaya Times Square Hotel',
 'Bukit Bintang',
 'Kuala Lumpur','Malaysia',
 5,5,
 'Reservation',
 '+60321178000',
 'reservation@berjayahotel.com',
 true,1,NOW()),

-- Singapore
('HTL0013','Marina Bay Sands',
 'Bayfront Avenue',
 'Singapore','Singapore',
 5,5,
 'Reservations',
 '+6566888868',
 'reservation@marinabaysands.com',
 true,1,NOW()),

-- India
('HTL0014','The Oberoi New Delhi',
 'Dr Zakir Hussain Marg',
 'New Delhi','India',
 5,5,
 'Reservations',
 '+911124366666',
 'reservation@oberoihotels.com',
 true,1,NOW()),

-- Maldives
('HTL0015','Hard Rock Hotel Maldives',
 'Emboodhoo Lagoon',
 'Male','Maldives',
 5,5,
 'Reservations',
 '+9606651400',
 'reservation@hrhmaldives.com',
 true,1,NOW()),

-- Turkey
('HTL0016','CVK Park Bosphorus Hotel',
 'Gümüşsuyu',
 'Istanbul','Turkey',
 5,5,
 'Sales',
 '+902123777777',
 'reservation@cvkhotels.com',
 true,1,NOW()),

-- France
('HTL0017','Pullman Paris Tour Eiffel',
 'Avenue de Suffren',
 'Paris','France',
 4,4,
 'Reservations',
 '+33144385600',
 'reservation@accor.com',
 true,1,NOW()),

-- Switzerland
('HTL0018','Hotel Schweizerhof',
 'Bahnhofplatz',
 'Lucerne','Switzerland',
 5,5,
 'Reservations',
 '+41414101111',
 'reservation@schweizerhof-luzern.ch',
 true,1,NOW()),

-- Indonesia
('HTL0019','The Apurva Kempinski Bali',
 'Nusa Dua',
 'Bali','Indonesia',
 5,5,
 'Sales',
 '+623612090999',
 'reservation@kempinski.com',
 true,1,NOW()),

-- Nepal
('HTL0020','Hotel Yak & Yeti',
 'Durbar Marg',
 'Kathmandu','Nepal',
 5,5,
 'Reservations',
 '+97714248999',
 'reservation@yakandyeti.com',
 true,1,NOW())

ON CONFLICT (organization_id, hotel_code) DO NOTHING;



-- trv_hotel_rooms skipped here: hotel bookings are user-created, not seed data.
-- Rooms are added via the hotel booking UI after a booking is saved.


INSERT INTO trv_airports
(airport_code, airport_name, city, country, created_at)
VALUES

-- Bangladesh
('DAC','Hazrat Shahjalal International Airport','Dhaka','Bangladesh',NOW()),
('CGP','Shah Amanat International Airport','Chattogram','Bangladesh',NOW()),
('CXB','Cox''s Bazar Airport','Cox''s Bazar','Bangladesh',NOW()),
('JSR','Jashore Airport','Jashore','Bangladesh',NOW()),
('ZYL','Osmani International Airport','Sylhet','Bangladesh',NOW()),
('RJH','Shah Makhdum Airport','Rajshahi','Bangladesh',NOW()),
('SPD','Saidpur Airport','Saidpur','Bangladesh',NOW()),

-- Saudi Arabia
('JED','King Abdulaziz International Airport','Jeddah','Saudi Arabia',NOW()),
('MED','Prince Mohammad Bin Abdulaziz Airport','Madinah','Saudi Arabia',NOW()),
('RUH','King Khalid International Airport','Riyadh','Saudi Arabia',NOW()),
('DMM','King Fahd International Airport','Dammam','Saudi Arabia',NOW()),

-- UAE
('DXB','Dubai International Airport','Dubai','United Arab Emirates',NOW()),
('DWC','Al Maktoum International Airport','Dubai','United Arab Emirates',NOW()),
('AUH','Abu Dhabi International Airport','Abu Dhabi','United Arab Emirates',NOW()),
('SHJ','Sharjah International Airport','Sharjah','United Arab Emirates',NOW()),

-- Qatar
('DOH','Hamad International Airport','Doha','Qatar',NOW()),

-- Oman
('MCT','Muscat International Airport','Muscat','Oman',NOW()),

-- Kuwait
('KWI','Kuwait International Airport','Kuwait City','Kuwait',NOW()),

-- Bahrain
('BAH','Bahrain International Airport','Manama','Bahrain',NOW()),

-- Malaysia
('KUL','Kuala Lumpur International Airport','Kuala Lumpur','Malaysia',NOW()),

-- Singapore
('SIN','Singapore Changi Airport','Singapore','Singapore',NOW()),

-- Thailand
('BKK','Suvarnabhumi Airport','Bangkok','Thailand',NOW()),
('DMK','Don Mueang International Airport','Bangkok','Thailand',NOW()),
('HKT','Phuket International Airport','Phuket','Thailand',NOW()),
('CNX','Chiang Mai International Airport','Chiang Mai','Thailand',NOW()),

-- Indonesia
('DPS','Ngurah Rai International Airport','Bali','Indonesia',NOW()),
('CGK','Soekarno–Hatta International Airport','Jakarta','Indonesia',NOW()),

-- Maldives
('MLE','Velana International Airport','Malé','Maldives',NOW()),

-- India
('DEL','Indira Gandhi International Airport','New Delhi','India',NOW()),
('BOM','Chhatrapati Shivaji Maharaj International Airport','Mumbai','India',NOW()),
('CCU','Netaji Subhas Chandra Bose International Airport','Kolkata','India',NOW()),
('MAA','Chennai International Airport','Chennai','India',NOW()),

-- Nepal
('KTM','Tribhuvan International Airport','Kathmandu','Nepal',NOW()),

-- Sri Lanka
('CMB','Bandaranaike International Airport','Colombo','Sri Lanka',NOW()),

-- Turkey
('IST','Istanbul Airport','Istanbul','Turkey',NOW()),

-- France
('CDG','Charles de Gaulle Airport','Paris','France',NOW()),

-- United Kingdom
('LHR','Heathrow Airport','London','United Kingdom',NOW()),

-- Germany
('FRA','Frankfurt Airport','Frankfurt','Germany',NOW()),

-- Switzerland
('ZRH','Zurich Airport','Zurich','Switzerland',NOW()),

-- Italy
('FCO','Leonardo da Vinci–Fiumicino Airport','Rome','Italy',NOW()),

-- United States
('JFK','John F. Kennedy International Airport','New York','USA',NOW()),
('LAX','Los Angeles International Airport','Los Angeles','USA',NOW()),
('ORD','O''Hare International Airport','Chicago','USA',NOW()),

-- Canada
('YYZ','Toronto Pearson International Airport','Toronto','Canada',NOW()),

-- Australia
('SYD','Sydney Kingsford Smith Airport','Sydney','Australia',NOW()),
('MEL','Melbourne Airport','Melbourne','Australia',NOW())

ON CONFLICT (airport_code) DO NOTHING;


INSERT INTO trv_airlines
(airline_code, airline_name, created_at, is_active)
VALUES

-- Bangladesh
('BG', 'Biman Bangladesh Airlines', NOW(), true),
('BS', 'US-Bangla Airlines', NOW(), true),
('VQ', 'Novoair', NOW(), true),

-- Middle East
('EK', 'Emirates', NOW(), true),
('EY', 'Etihad Airways', NOW(), true),
('QR', 'Qatar Airways', NOW(), true),
('SV', 'Saudia', NOW(), true),
('FZ', 'flydubai', NOW(), true),
('G9', 'Air Arabia', NOW(), true),
('XY', 'flynas', NOW(), true),
('WY', 'Oman Air', NOW(), true),
('GF', 'Gulf Air', NOW(), true),
('KU', 'Kuwait Airways', NOW(), true),

-- India
('AI', 'Air India', NOW(), true),
('6E', 'IndiGo', NOW(), true),
('UK', 'Vistara', NOW(), true),
('SG', 'SpiceJet', NOW(), true),
('IX', 'Air India Express', NOW(), true),
('AK', 'AirAsia', NOW(), true),

-- Southeast Asia
('MH', 'Malaysia Airlines', NOW(), true),
('SQ', 'Singapore Airlines', NOW(), true),
('TR', 'Scoot', NOW(), true),
('TG', 'Thai Airways', NOW(), true),
('FD', 'Thai AirAsia', NOW(), true),
('OD', 'Batik Air Malaysia', NOW(), true),
('GA', 'Garuda Indonesia', NOW(), true),

-- Sri Lanka / Nepal / Maldives
('UL', 'SriLankan Airlines', NOW(), true),
('RA', 'Nepal Airlines', NOW(), true),
('Q2', 'Maldivian', NOW(), true),

-- Turkey
('TK', 'Turkish Airlines', NOW(), true),
('PC', 'Pegasus Airlines', NOW(), true),

-- Europe
('BA', 'British Airways', NOW(), true),
('LH', 'Lufthansa', NOW(), true),
('AF', 'Air France', NOW(), true),
('KL', 'KLM Royal Dutch Airlines', NOW(), true),
('LX', 'Swiss International Air Lines', NOW(), true),
('OS', 'Austrian Airlines', NOW(), true),
('AY', 'Finnair', NOW(), true),
('IB', 'Iberia', NOW(), true),

-- North America
('AA', 'American Airlines', NOW(), true),
('DL', 'Delta Air Lines', NOW(), true),
('UA', 'United Airlines', NOW(), true),
('AC', 'Air Canada', NOW(), true),

-- East Asia
('CX', 'Cathay Pacific', NOW(), true),
('JL', 'Japan Airlines', NOW(), true),
('NH', 'All Nippon Airways', NOW(), true),
('KE', 'Korean Air', NOW(), true),
('OZ', 'Asiana Airlines', NOW(), true),
('CI', 'China Airlines', NOW(), true),
('BR', 'EVA Air', NOW(), true),
('CA', 'Air China', NOW(), true),
('MU', 'China Eastern Airlines', NOW(), true),
('CZ', 'China Southern Airlines', NOW(), true),

-- Oceania
('QF', 'Qantas', NOW(), true),
('NZ', 'Air New Zealand', NOW(), true)

ON CONFLICT (airline_code) DO NOTHING;



INSERT INTO trv_tour_guides
(
    guide_name,
    phone,
    email,
    languages,
    is_active
)
VALUES

    ('Ahmed Rahman',
     '+8801711000001',
     'ahmed.rahman@travel.com',
     'Bengali,English',
     true),

    ('Fatema Akter',
     '+8801711000002',
     'fatema.akter@travel.com',
     'Bengali,English,Hindi',
     true),

    ('Mohammad Karim',
     '+8801711000003',
     'karim@travel.com',
     'Bengali,English,Arabic',
     true),

    ('Sarah Islam',
     '+8801711000004',
     'sarah.islam@travel.com',
     'English,Arabic',
     true),

    ('John Smith',
     '+971501111111',
     'john.smith@travel.com',
     'English',
     true),

    ('Ali Hassan',
     '+966501111111',
     'ali.hassan@travel.com',
     'Arabic,English',
     true),

    ('Somsak Chai',
     '+66811111111',
     'somsak@travel.com',
     'Thai,English',
     true),

    ('Nur Aisyah',
     '+60121111111',
     'aisyah@travel.com',
     'Malay,English',
     true),

    ('Rajesh Kumar',
     '+919811111111',
     'rajesh@travel.com',
     'Hindi,English',
     true),

    ('Tenzin Sherpa',
     '+9779811111111',
     'tenzin@travel.com',
     'Nepali,English',
     true),

    ('Mehmet Demir',
     '+905321111111',
     'mehmet@travel.com',
     'Turkish,English',
     true),

    ('Maria Rossi',
     '+393331111111',
     'maria.rossi@travel.com',
     'Italian,English',
     true),

    ('Pierre Martin',
     '+33611111111',
     'pierre.martin@travel.com',
     'French,English',
     true),

    ('Hans Müller',
     '+4915111111111',
     'hans.mueller@travel.com',
     'German,English',
     true),

    ('Kenji Tanaka',
     '+818011111111',
     'kenji.tanaka@travel.com',
     'Japanese,English',
     true),

    ('Li Wei',
     '+8613811111111',
     'li.wei@travel.com',
     'Chinese,English',
     true),

    ('David Brown',
     '+61411111111',
     'david.brown@travel.com',
     'English',
     true),

    ('Abdul Aziz',
     '+96550111111',
     'abdul.aziz@travel.com',
     'Arabic,English',
     true),

    ('Farzana Kabir',
     '+8801711000005',
     'farzana.kabir@travel.com',
     'Bengali,English',
     true),

    ('Imran Hossain',
     '+8801711000006',
     'imran.hossain@travel.com',
     'Bengali,English,Arabic',
     true)

ON CONFLICT DO NOTHING;


INSERT INTO trv_room_types
(
    room_type_name,
    max_occupancy,
    base_price,
    currency,
    hotel_id,
    is_active,
    organization_id,
    created_at
)
VALUES

-- Hotel 1 : Pan Pacific Sonargaon
('Standard Room',        2,  8500.00, 'BDT', 1, true, 1, NOW()),
('Deluxe Room',          2, 12000.00, 'BDT', 1, true, 1, NOW()),
('Executive Room',       2, 15500.00, 'BDT', 1, true, 1, NOW()),
('Junior Suite',         3, 22000.00, 'BDT', 1, true, 1, NOW()),
('Presidential Suite',   4, 55000.00, 'BDT', 1, true, 1, NOW()),

-- Hotel 2 : InterContinental Dhaka
('Classic Room',         2, 11000.00, 'BDT', 2, true, 1, NOW()),
('Premium Room',         2, 14500.00, 'BDT', 2, true, 1, NOW()),
('Club Room',            2, 18000.00, 'BDT', 2, true, 1, NOW()),
('Executive Suite',      3, 32000.00, 'BDT', 2, true, 1, NOW()),

-- Hotel 3 : Sea Pearl Beach Resort
('Superior Room',        2, 7000.00, 'BDT', 3, true, 1, NOW()),
('Deluxe Sea View',      2, 9500.00, 'BDT', 3, true, 1, NOW()),
('Family Suite',         4, 17000.00, 'BDT', 3, true, 1, NOW()),

-- Hotel 4 : Hotel The Cox Today
('Standard Twin',        2, 5500.00, 'BDT', 4, true, 1, NOW()),
('Deluxe Twin',          2, 7500.00, 'BDT', 4, true, 1, NOW()),
('Executive Suite',      3, 14500.00, 'BDT', 4, true, 1, NOW()),

-- Hotel 5 : Grand Sultan
('Deluxe Garden View',   2, 10000.00, 'BDT', 5, true, 1, NOW()),
('Premium Suite',        3, 19000.00, 'BDT', 5, true, 1, NOW()),

-- Hotel 6 : Atlantis Dubai
('Palm View Room',       2, 32000.00, 'AED', 6, true, 1, NOW()),
('Ocean View Room',      2, 42000.00, 'AED', 6, true, 1, NOW()),
('Imperial Suite',       4, 95000.00, 'AED', 6, true, 1, NOW()),

-- Hotel 7 : Burj Al Arab
('Deluxe Suite',         2, 85000.00, 'AED', 7, true, 1, NOW()),
('Panoramic Suite',      2,120000.00, 'AED', 7, true, 1, NOW()),
('Royal Suite',          4,250000.00, 'AED', 7, true, 1, NOW()),

-- Hotel 8 : Swissotel Makkah
('Classic Room',         2, 450.00, 'SAR', 8, true, 1, NOW()),
('Kaaba View Room',      2, 850.00, 'SAR', 8, true, 1, NOW()),
('Family Suite',         5,1500.00, 'SAR', 8, true, 1, NOW()),

-- Hotel 9 : Pullman ZamZam Madina
('Superior Room',        2, 420.00, 'SAR', 9, true, 1, NOW()),
('Executive Room',       2, 700.00, 'SAR', 9, true, 1, NOW()),

-- Hotel 10 : Amari Bangkok
('Superior Room',        2, 3800.00, 'THB',10, true, 1, NOW()),
('Deluxe Room',          2, 5200.00, 'THB',10, true, 1, NOW()),
('Executive Suite',      3, 8800.00, 'THB',10, true, 1, NOW()),

-- Hotel 11 : Holiday Inn Pattaya
('Standard Room',        2, 3200.00, 'THB',11, true, 1, NOW()),
('Ocean View Room',      2, 4600.00, 'THB',11, true, 1, NOW()),

-- Hotel 12 : Berjaya Times Square
('Deluxe Room',          2, 550.00, 'MYR',12, true, 1, NOW()),
('Premier Room',         2, 720.00, 'MYR',12, true, 1, NOW()),

-- Hotel 13 : Marina Bay Sands
('Deluxe Room',          2, 650.00, 'SGD',13, true, 1, NOW()),
('Club Room',            2, 980.00, 'SGD',13, true, 1, NOW()),
('Sky Suite',            4,1800.00, 'SGD',13, true, 1, NOW()),

-- Hotel 14 : Oberoi New Delhi
('Luxury Room',          2,12000.00, 'INR',14, true, 1, NOW()),
('Premier Room',         2,16500.00, 'INR',14, true, 1, NOW()),

-- Hotel 15 : Hard Rock Maldives
('Beach Villa',          2,950.00, 'USD',15, true, 1, NOW()),
('Overwater Villa',      2,1450.00, 'USD',15, true, 1, NOW()),

-- Hotel 16 : CVK Park Istanbul
('Standard Room',        2,250.00, 'EUR',16, true, 1, NOW()),
('Bosphorus Suite',      3,650.00, 'EUR',16, true, 1, NOW()),

-- Hotel 17 : Pullman Paris
('Classic Room',         2,280.00, 'EUR',17, true, 1, NOW()),
('Eiffel View Room',     2,420.00, 'EUR',17, true, 1, NOW()),

-- Hotel 18 : Schweizerhof Lucerne
('Superior Room',        2,380.00, 'CHF',18, true, 1, NOW()),
('Lake View Suite',      3,780.00, 'CHF',18, true, 1, NOW()),

-- Hotel 19 : Kempinski Bali
('Garden View',          2,420.00, 'USD',19, true, 1, NOW()),
('Ocean Front Suite',    3,920.00, 'USD',19, true, 1, NOW()),

-- Hotel 20 : Hotel Yak & Yeti
('Deluxe Room',          2,140.00, 'USD',20, true, 1, NOW()),
('Heritage Suite',       3,290.00, 'USD',20, true, 1, NOW());