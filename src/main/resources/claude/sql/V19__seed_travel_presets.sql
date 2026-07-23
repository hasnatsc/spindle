-- =============================================================================
--  Spindle ERP  —  Travel Portal Seed Data
--  File   : V105__seed_travel_portal_presets.sql
--  Target : PostgreSQL 15+
--
--  Beautiful preset packages, tours, hotels, and more for the public
--  travel-site (no-login-required customer portal).
--
--  All data is Bangladesh/South-Asia focused with realistic pricing in BDT.
--  Safe to re-run: all INSERTs use ON CONFLICT DO NOTHING.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1.  HOTEL CATEGORIES
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_hotel_categories (category_name, description, organization_id, created_at, created_by)
SELECT v.* FROM (VALUES
    ('5 Star',  'Luxury international-standard hotels with premium amenities', 1, NOW(), 'system'),
    ('4 Star',  'Superior comfort with excellent service and facilities',       1, NOW(), 'system'),
    ('3 Star',  'Quality accommodation at a great value',                      1, NOW(), 'system'),
    ('Boutique','Unique, stylish properties with a personal touch',            1, NOW(), 'system'),
    ('Resort',  'Full-service resort properties with leisure facilities',      1, NOW(), 'system'),
    ('Budget',  'Clean, comfortable, and affordable',                          1, NOW(), 'system')
) AS v(category_name, description, organization_id, created_at, created_by)
WHERE NOT EXISTS (
    SELECT 1 FROM trv_hotel_categories t
    WHERE t.category_name = v.category_name AND t.organization_id = v.organization_id
);

-- ═════════════════════════════════════════════════════════════════════════════
-- 2.  MEAL PLANS
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_meal_plans (plan_code, plan_name, description, created_at)
VALUES
    ('RO', 'Room Only',   'No meals included',                     NOW()),
    ('BB', 'Bed & Breakfast','Continental or local breakfast daily', NOW()),
    ('HB', 'Half Board',  'Breakfast & dinner daily',              NOW()),
    ('FB', 'Full Board',  'Breakfast, lunch & dinner daily',       NOW()),
    ('AI', 'All Inclusive','All meals, snacks & selected beverages', NOW())
ON CONFLICT (plan_code) DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 3.  HOTELS
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_hotels (hotel_code, hotel_name, city, country, address, star_rating, category_id, contact_phone, contact_email, is_active, organization_id, created_at, created_by)
VALUES
    ('HT-DHK-001', 'The Pan Pacific Sonargaon',   'Dhaka',    'Bangladesh', '107 Kazi Nazrul Islam Ave, Dhaka 1215',     5, (SELECT id FROM trv_hotel_categories WHERE category_name = '5 Star'), '+880-2-55512345', 'reservations@panpacific.dhaka.com', true, 1, NOW(), 'system'),
    ('HT-DHK-002', 'Hotel InterContinental Dhaka','Dhaka',    'Bangladesh', '1 Minto Rd, Dhaka 1000',                    5, (SELECT id FROM trv_hotel_categories WHERE category_name = '5 Star'), '+880-2-55663000', 'dhaka@intercontinental.com', true, 1, NOW(), 'system'),
    ('HT-DHK-003', 'Six Seasons Hotel',           'Dhaka',    'Bangladesh', '5/B, Road 99, Gulshan 2, Dhaka 1212',      5, (SELECT id FROM trv_hotel_categories WHERE category_name = '5 Star'), '+880-2-58811200', 'reservations@sixseasons.com', true, 1, NOW(), 'system'),
    ('HT-COX-001', 'Ocean Paradise Hotel',        'Cox''s Bazar', 'Bangladesh', 'Hotel Motel Zone, Cox''s Bazar 4700',     4, (SELECT id FROM trv_hotel_categories WHERE category_name = '4 Star'), '+880-341-52345', 'info@oceanparadise.com', true, 1, NOW(), 'system'),
    ('HT-COX-002', 'Royal Tulip Sea Pearl',       'Cox''s Bazar', 'Bangladesh', 'Sugandha Point, Cox''s Bazar 4700',      5, (SELECT id FROM trv_hotel_categories WHERE category_name = '5 Star'), '+880-341-62345', 'reservations@royaltulip.com', true, 1, NOW(), 'system'),
    ('HT-COX-003', 'Hotel Media Cox',             'Cox''s Bazar', 'Bangladesh', '4no Ghat, Main Road, Cox''s Bazar',      3, (SELECT id FROM trv_hotel_categories WHERE category_name = '3 Star'), '+880-341-52346', 'info@hotelmediacox.com', true, 1, NOW(), 'system'),
    ('HT-SYL-001', 'Grand Sultan Tea Resort',     'Sylhet',   'Bangladesh', 'Mugla Para, Upashahar, Sylhet',             5, (SELECT id FROM trv_hotel_categories WHERE category_name = 'Resort'), '+880-821-72345', 'reservations@grandsultan.com', true, 1, NOW(), 'system'),
    ('HT-SYL-002', 'Rose View Hotel',             'Sylhet',   'Bangladesh', 'Sylhet 3100, Shahjalal Upashahar',          4, (SELECT id FROM trv_hotel_categories WHERE category_name = '4 Star'), '+880-821-71777', 'info@roseviewhotel.com', true, 1, NOW(), 'system'),
    ('HT-SG-001',  'Hotel Agrabad',               'Chittagong','Bangladesh', 'Agrabad C/A, Chittagong 4100',             4, (SELECT id FROM trv_hotel_categories WHERE category_name = '4 Star'), '+880-31-713311', 'info@hotelagrabad.com', true, 1, NOW(), 'system'),
    ('HT-KH-001',  'Nilgiri Hill Resort',         'Bandarban','Bangladesh', 'Nilgiri, Bandarban Hill District',           3, (SELECT id FROM trv_hotel_categories WHERE category_name = 'Resort'), '+880-371-41234', 'nilgiri@resort.com', true, 1, NOW(), 'system'),
    ('HT-KH-002',  'Hill Side Resort',            'Rangamati','Bangladesh', 'Tabalchari, Rangamati',                    3, (SELECT id FROM trv_hotel_categories WHERE category_name = 'Resort'), '+880-352-51234', 'hillside@rangamati.com', true, 1, NOW(), 'system'),
    ('HT-KL-001',  'Hotel Castle Salam',          'Khulna',   'Bangladesh', '2 KDA Ave, Khulna 9100',                   3, (SELECT id FROM trv_hotel_categories WHERE category_name = '3 Star'), '+880-41-720123', 'info@castlesalam.com', true, 1, NOW(), 'system')
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 4.  AIRLINES
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_airlines (airline_code, airline_name, is_active, created_at)
VALUES
    ('BG', 'Biman Bangladesh Airlines', true, NOW()),
    ('BS', 'US-Bangla Airlines',        true, NOW()),
    ('NH', 'Novoair',                   true, NOW()),
    ('RT', 'Regent Airways',            true, NOW()),
    ('EK', 'Emirates',                  true, NOW()),
    ('QR', 'Qatar Airways',             true, NOW()),
    ('TK', 'Turkish Airlines',          true, NOW()),
    ('SQ', 'Singapore Airlines',        true, NOW()),
    ('MH', 'Malaysia Airlines',         true, NOW()),
    ('TG', 'Thai Airways',              true, NOW())
ON CONFLICT (airline_code) DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 5.  AIRPORTS
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_airports (airport_code, airport_name, city, country, created_at)
VALUES
    ('DAC', 'Hazrat Shahjalal International Airport',     'Dhaka',       'Bangladesh', NOW()),
    ('CXB', 'Cox''s Bazar Airport',                       'Cox''s Bazar','Bangladesh', NOW()),
    ('SPD', 'Saidpur Airport',                            'Saidpur',     'Bangladesh', NOW()),
    ('JSR', 'Jessore Airport',                            'Jessore',     'Bangladesh', NOW()),
    ('CGP', 'Shah Amanat International Airport',           'Chittagong',  'Bangladesh', NOW()),
    ('ZYL', 'Osmani International Airport',               'Sylhet',      'Bangladesh', NOW()),
    ('DAC', 'Hazrat Shahjalal International Airport',     'Dhaka',       'Bangladesh', NOW()),
    ('DXB', 'Dubai International Airport',                'Dubai',       'UAE',        NOW()),
    ('DOH', 'Hamad International Airport',                'Doha',        'Qatar',      NOW()),
    ('IST', 'Istanbul Airport',                           'Istanbul',    'Turkey',     NOW()),
    ('SIN', 'Singapore Changi Airport',                   'Singapore',   'Singapore',  NOW()),
    ('KUL', 'Kuala Lumpur International Airport',         'Kuala Lumpur','Malaysia',   NOW()),
    ('BKK', 'Suvarnabhumi Airport',                       'Bangkok',     'Thailand',   NOW()),
    ('CCU', 'Netaji Subhas Chandra Bose International Airport', 'Kolkata','India',     NOW()),
    ('KTM', 'Tribhuvan International Airport',            'Kathmandu',   'Nepal',      NOW()),
    ('CMB', 'Bandaranaike International Airport',         'Colombo',     'Sri Lanka',  NOW()),
    ('MLE', 'Velana International Airport',               'Malé',        'Maldives',   NOW())
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 6.  TOUR GUIDES
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_tour_guides (guide_name, phone, email, languages, is_active)
VALUES
    ('Arif Rahman',       '01711-223344', 'arif@asgtravel.com',  'Bengali, English, Hindi',        true),
    ('Fatima Begum',      '01722-334455', 'fatima@asgtravel.com','Bengali, English, Arabic',        true),
    ('Kamal Hossain',     '01733-445566', 'kamal@asgtravel.com', 'Bengali, English, Japanese',      true),
    ('Nusrat Jahan',      '01744-556677', 'nusrat@asgtravel.com','Bengali, English, French',         true),
    ('Shahidul Islam',    '01755-667788', 'shahidul@asgtravel.com','Bengali, English, Hindi, Urdu', true),
    ('Tahmina Akhter',    '01766-778899', 'tahmina@asgtravel.com','Bengali, English, Spanish',      true),
    ('Rafiq Hasan',       '01777-889900', 'rafiq@asgtravel.com', 'Bengali, English, Mandarin',      true),
    ('Sultana Razia',     '01788-990011', 'sultana@asgtravel.com','Bengali, English, Hindi',         true)
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 7.  TOURS  (day-trips & excursions)
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_tours (tour_code, tour_name, destination, duration_hours, base_price, currency, description, is_active, organization_id, created_at, created_by)
VALUES
    ('T-SYL-01', 'Srimangal Sunrise Tea Tour',
     'Srimangal, Sylhet', 8, 2500.00, 'BDT',
     'Walk through endless green tea gardens at dawn, visit a working tea factory, and taste the famous 7-layer tea from the original shop. Includes a guided nature walk through Lawachara Rainforest where you might spot gibbons swinging overhead.',
     true, 1, NOW(), 'system'),

    ('T-SYL-02', 'Ratargul Swamp Forest Boat Trip',
     'Ratargul, Sylhet', 6, 1800.00, 'BDT',
     'Glide by rowboat through the "Amazon of Bangladesh" — a pristine freshwater swamp forest. Watch kingfishers dive from overhanging branches as your boatman navigates through submerged trees. Eerie, beautiful, unforgettable.',
     true, 1, NOW(), 'system'),

    ('T-COX-01', 'Himchori Sunset & Waterfall Hike',
     'Himchori, Cox''s Bazar', 5, 1200.00, 'BDT',
     'A guided hike through coastal hills to the Himchori waterfall and sea-view point. Reach the top just as the sun sinks into the Bay of Bengal — the most photographed sunset in Bangladesh.',
     true, 1, NOW(), 'system'),

    ('T-COX-02', 'St. Martin''s Coral Island Escape',
     'St. Martin''s Island', 12, 3500.00, 'BDT',
     'Take the morning speedboat to the only coral island in Bangladesh. Snorkel in crystal-clear water, walk on beaches of crushed coral, and feast on freshly grilled lobster. Returns before dusk.',
     true, 1, NOW(), 'system'),

    ('T-BND-01', 'Bandarban Hill Trek — Nilgiri',
     'Nilgiri, Bandarban', 10, 3000.00, 'BDT',
     'An invigorating trek through the hills of the Bandarban district to Nilgiri, the highest peak in Bangladesh accessible to visitors. Panoramic views of Myanmar on clear days. Meet indigenous Marma communities along the way.',
     true, 1, NOW(), 'system'),

    ('T-KHK-01', 'Sajek Valley Sunrise & Indigenous Village',
     'Sajek Valley', 24, 4500.00, 'BDT',
     'An overnight trip to the "Queen of Hills" — Sajek Valley. Stay in a traditional Kuki lodge, watch sunrise over clouds from Kong Lak Hill, and share a meal with indigenous families. Blankets of mist, towering mountains, warm hospitality.',
     true, 1, NOW(), 'system'),

    ('T-DHK-01', 'Old Dhaka Heritage Rickshaw Tour',
     'Old Dhaka', 5, 1500.00, 'BDT',
     'Climb aboard a colourful rickshaw and weave through the narrow lanes of Puran Dhaka. Visit Armenian Church, Star Mosque, Lalbagh Fort, and Ahsan Manzil. Taste authentic Bhai Bhai Pitha and the best biryani in town.',
     true, 1, NOW(), 'system'),

    ('T-KHL-01', 'Sundarbans Mangrove Forest Expedition',
     'Sundarbans, Khulna', 24, 5500.00, 'BDT',
     'A full-day-and-a-half expedition into the world''s largest mangrove forest, a UNESCO World Heritage site. Cruise the river channels in a traditional wooden boat, spot Bengal tigers (with luck!), deer, crocodiles, and hundreds of bird species.',
     true, 1, NOW(), 'system'),

    ('T-KHL-02', 'Sixty Dome Mosque & Shat Gambuj Heritage',
     'Bagerhat, Khulna', 8, 2000.00, 'BDT',
     'Step back 600 years to the historic mosque city of Bagerhat, another UNESCO site. The Sixty Dome Mosque is an architectural marvel of baked brick and terracotta. Also visit the shrines and ancient water tanks scattered through the tranquil grounds.',
     true, 1, NOW(), 'system'),

    ('T-MDV-01', 'Maldives Overwater Sunset Cruise',
     'Malé, Maldives', 4, 8500.00, 'BDT',
     'Sail into the Indian Ocean aboard a traditional dhoni as the sky turns every shade of orange and pink. Spot flying fish and dolphins, and anchor on a sandbank. Includes sparkling drinks and canapés.',
     true, 1, NOW(), 'system'),

    ('T-NPL-01', 'Kathmandu Valley Heritage & Peace Pagoda',
     'Kathmandu, Nepal', 10, 4000.00, 'BDT',
     'Explore the ancient Durbar Squares, the towering Boudhanath Stupa, and the peaceful Swayambhunath Monkey Temple. End the day at the World Peace Pagoda with a Himalayan sunset backdrop.',
     true, 1, NOW(), 'system'),

    ('T-THA-01', 'Bangkok Floating Market & Temples',
     'Bangkok, Thailand', 10, 5500.00, 'BDT',
     'Ride a long-tail boat through the vibrant Damnoen Saduak floating market, then marvel at the Grand Palace and the Reclining Buddha at Wat Pho. A whirlwind taste of Thailand''s kaleidoscopic culture.',
     true, 1, NOW(), 'system')
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 8.  PACKAGES  (multi-day bundled experiences)
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_packages (package_code, package_name, destination, category, duration_days, duration_nights, base_price, currency, description, is_active, organization_id, created_at, created_by)
VALUES
    ('P-COX-01', 'Cox''s Bazar Beach & Marine Getaway',
     'Cox''s Bazar', 'Beach', 4, 3, 18500.00, 'BDT',
     'Three nights of sun, sea, and sand at the world''s longest natural beach. Stay at the beachfront Ocean Paradise, visit Himchori and Inani Beach, take the St. Martin''s Island day trip, and watch the sunset from Kolatoli Point. All transfers and breakfast included.',
     true, 1, NOW(), 'system'),

    ('P-SYL-01', 'Sylhet Tea Country & Nature Retreat',
     'Sylhet', 'Nature', 3, 2, 14500.00, 'BDT',
     'Two nights in the heart of Bangladesh''s tea country. Stay at the Grand Sultan Tea Resort, visit Srimangal''s organic tea gardens and the Lawachara Rainforest, explore Ratargul Swamp Forest by boat, and taste authentic Sylheti cuisine.',
     true, 1, NOW(), 'system'),

    ('P-KHL-01', 'Sundarbans Explorer — Tiger Territory',
     'Sundarbans, Khulna', 'Wildlife', 3, 2, 22500.00, 'BDT',
     'An organized two-night expedition into the Sundarbans mangrove forest. Travel by riverboat, watch for Bengal tigers and estuarine crocodiles, visit forest watchtowers, and learn about the unique ecology from our expert naturalist guide. All meals on board.',
     true, 1, NOW(), 'system'),

    ('P-BND-01', 'Hill Tracts Explorer — Bandarban & Rangamati',
     'Bandarban & Rangamati', 'Adventure', 5, 4, 28500.00, 'BDT',
     'A four-night journey through the breathtaking Chittagong Hill Tracts. Trek to Nilgiri peak, explore the tribal markets of Bandarban, cruise Kaptai Lake in Rangamati, and stay at a hilltop resort. A perfect mix of trekking, culture, and scenery.',
     true, 1, NOW(), 'system'),

    ('P-DHK-01', 'Dhaka City & Heritage Discovery',
     'Dhaka', 'Cultural', 3, 2, 12500.00, 'BDT',
     'Discover the vibrant capital of Bangladesh. Explore Mughal-era architecture in Old Dhaka, visit the National Museum and Liberation War Museum, cruise the Buriganga River, and enjoy a fine-dining experience at one of Dhaka''s top restaurants. Luxury transport included.',
     true, 1, NOW(), 'system'),

    ('P-SYLC-01', 'Sylhet & Srimangal Romantic Escape',
     'Sylhet & Srimangal', 'Romantic', 4, 3, 32000.00, 'BDT',
     'A romantic three-night getaway for couples. Stay in a luxury tea-bungalow suite overlooking endless green estates, enjoy a candle-lit dinner among the tea gardens, take a private sunset boat ride on a tranquil lake, and relax with couple spa treatments.',
     true, 1, NOW(), 'system'),

    ('P-SAJEK-01', 'Sajek Valley Cloud Trail',
     'Sajek Valley', 'Adventure', 3, 2, 16500.00, 'BDT',
     'Two nights in the cloud-capped hills of Sajek Valley. Stay in traditional Kuki-style cottages, watch sunrise from Kong Lak Hill, hike through bamboo forests, and spend an evening around a campfire with indigenous Marma storytellers.',
     true, 1, NOW(), 'system'),

    ('P-MALD-01', 'Maldives Island Paradise — 4 Nights',
     'Malé & North Male Atoll, Maldives', 'International', 5, 4, 85000.00, 'BDT',
     'Four nights in a stunning overwater villa in the Maldives. Snorkel with manta rays and whale sharks, enjoy a private sandbank dinner, watch dolphins from your deck, and do absolutely nothing on powdery white beaches. Includes return flights from Dhaka, transfers, and breakfast.',
     true, 1, NOW(), 'system'),

    ('P-THAI-01', 'Bangkok & Pattaya — 5 Days',
     'Bangkok & Pattaya, Thailand', 'International', 5, 4, 52000.00, 'BDT',
     'Five days in the land of smiles. Visit Bangkok''s Grand Palace and floating markets, shop at Chatuchak weekend market, enjoy Pattaya''s beaches and Coral Island snorkelling, and indulge in world-class Thai street food. 4-star hotels with breakfast.',
     true, 1, NOW(), 'system'),

    ('P-NPL-01', 'Kathmandu & Pokhara Himalayan Adventure',
     'Kathmandu & Pokhara, Nepal', 'International', 7, 6, 65000.00, 'BDT',
     'A week in the shadow of the Himalayas. Explore Kathmandu''s UNESCO heritage sites, fly to Pokhara for sunrise over Annapurna, go paragliding above Fewa Lake, and trek to the Peace Pagoda. Return with stories that last a lifetime.',
     true, 1, NOW(), 'system'),

    ('P-SIN-01', 'Singapore & Malaysia — 6 Nights',
     'Singapore & Kuala Lumpur', 'International', 7, 6, 78000.00, 'BDT',
     'Two iconic Southeast Asian cities in one trip. Gardens by the Bay, Sentosa Island, and Orchard Road in Singapore. Petronas Towers, Batu Caves, and street-food paradise in Kuala Lumpur. 5-star hotels, flights, and all transfers included.',
     true, 1, NOW(), 'system')
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 9.  PACKAGE INCLUSIONS
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', '3 nights hotel accommodation at Ocean Paradise or similar'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Daily breakfast at hotel'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Airport-to-hotel private transfers'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'St. Martin Island full-day speedboat trip with lunch'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Himchori & Inani Beach guided tour'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Flight tickets to Cox''s Bazar'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Personal expenses and tips'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Travel insurance'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

-- Sylhet Tea Country Package inclusions
INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', '2 nights at Grand Sultan Tea Resort or similar'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Daily breakfast and dinner'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Tea garden walking tour with factory visit'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Ratargul swamp forest boat ride'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Lawachara Rainforest guided nature walk'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Transportation to/from Sylhet'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Lunch (available at local restaurants)'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

-- Sundarbans Package
INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', '2 nights on riverboat with AC cabins'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'All meals on board (Bengali cuisine)'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Professional naturalist guide'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Forest watchtower visits'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Transportation to Khulna'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Personal travel insurance'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

-- Maldives Package
INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Return flights Dhaka–Malé'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', '4 nights overwater villa with private deck'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Daily breakfast & dinner'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Snorkelling gear & dolphin cruise'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'INCLUDED', 'Speedboat airport transfers'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Visa fees (if applicable)'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Personal expenses & tips'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_inclusions (package_id, inclusion_type, description)
SELECT p.id, 'EXCLUDED', 'Travel insurance'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 10. PACKAGE ITINERARY DAYS
-- ═════════════════════════════════════════════════════════════════════════════
-- Cox's Bazar 4D/3N itinerary
INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 1, 'Arrival & Sunset at Kolatoli',
       'Arrive in Cox''s Bazar and check into Ocean Paradise Hotel. Relax on the beach, then head to Kolatoli Point for a spectacular sunset over the Bay of Bengal. Welcome dinner at a beachfront restaurant.'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 2, 'St. Martin''s Island Adventure',
       'Early morning speedboat to St. Martin''s Island (approx 2.5 hrs). Snorkel in crystal waters, walk on coral-sand beaches, and enjoy fresh seafood lunch. Return by late afternoon. Evening free to explore Cox''s Bazar''s famous Burmese market.'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 3, 'Himchori, Inani & Local Life',
       'Morning hike to Himchori waterfall and viewpoint. Then drive to Inani Beach — 18 km of golden sand framed by rocky cliffs. Stop at a local fishing village to see traditional wooden boat building. Farewell dinner at a seaside barbecue.'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 4, 'Departure',
       'Breakfast at the hotel. Last minute souvenir shopping at the beachside arcades. Transfer to Cox''s Bazar airport for your flight back to Dhaka.'
FROM trv_packages p WHERE p.package_code = 'P-COX-01'
ON CONFLICT DO NOTHING;

-- Sylhet 3D/2N itinerary
INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 1, 'Arrival & Tea Estate Exploration',
       'Arrive in Sylhet, transfer to Grand Sultan Tea Resort. Afternoon guided walk through the magnificent tea estates — endless green carpets stretching to the horizon. Evening tea-tasting session.'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 2, 'Ratargul Swamp & Srimangal',
       'Morning boat trip through the hauntingly beautiful Ratargul Swamp Forest. Afternoon drive to Srimangal — the tea capital of Bangladesh. Visit a working tea factory and taste the famous 7-layer tea.'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 3, 'Lawachara Rainforest & Departure',
       'Early morning nature walk in Lawachara National Park — spot langurs, gibbons, and exotic birds. Visit a tribal village before transferring to Sylhet airport for departure.'
FROM trv_packages p WHERE p.package_code = 'P-SYL-01'
ON CONFLICT DO NOTHING;

-- Sundarbans 3D/2N
INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 1, 'Boarding & River Cruise',
       'Meet at Mongla port and board your traditional riverboat. Cruise through the Sundarbans'' intricate network of rivers and creeks. Afternoon nature talk by your guide. Watch the sunset from the upper deck.'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 2, 'Deep Forest Exploration',
       'Full day exploring the heart of the mangrove forest. Visit watchtowers for wildlife viewing, walk on a forest trail (with a ranger), and cruise narrow creeks where crocodiles bask in the sun. Evening slideshow and documentary screening.'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 3, 'Final Safari & Disembark',
       'Early morning river safari — the best time to spot Bengal tigers drinking at the water''s edge. Breakfast on board, then cruise back to Mongla for disembarkation and your onward journey.'
FROM trv_packages p WHERE p.package_code = 'P-KHL-01'
ON CONFLICT DO NOTHING;

-- Hill Tracts 5D/4N
INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 1, 'Arrival in Bandarban & Bazar Walk',
       'Arrive in Bandarban town. Check into hill-view resort. Afternoon guided walk through the indigenous tribal market — meet the Marma and Bawm communities, see traditional textiles and bamboo crafts.'
FROM trv_packages p WHERE p.package_code = 'P-BND-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 2, 'Nilgiri Trek & Panoramic Views',
       'Early start for the trek to Nilgiri peak. The trail winds through bamboo groves and pine forests. At the summit (2,300 ft), enjoy sweeping views of the hills stretching to Myanmar. Picnic lunch with a view.'
FROM trv_packages p WHERE p.package_code = 'P-BND-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 3, 'Rangamati — Kaptai Lake & Hanging Bridge',
       'Drive to Rangamati. Cruise Kaptai Lake — the largest man-made lake in Bangladesh. Visit the Hanging Bridge over the lake, the Shuvolong waterfall, and a Buddhist monastery on a hilltop island.'
FROM trv_packages p WHERE p.package_code = 'P-BND-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 4, 'Indigenous Culture & Handicrafts',
       'Visit the Tribal Cultural Institute and a traditional weaving centre. Try your hand at bamboo weaving. Evening cultural performance by local artists around a bonfire. Overnight at lakeside resort.'
FROM trv_packages p WHERE p.package_code = 'P-BND-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 5, 'Departure',
       'Breakfast overlooking the lake. Visit the local handicraft market before departing for Chittagong or Dhaka.'
FROM trv_packages p WHERE p.package_code = 'P-BND-01'
ON CONFLICT DO NOTHING;

-- Maldives 5D/4N
INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 1, 'Arrival in Paradise',
       'Fly from Dhaka to Malé. Speedboat transfer to your overwater villa. Welcome cocktail and orientation. Spend the afternoon settling in — step directly from your deck into warm turquoise water.'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 2, 'Manta Rays & Sandbank Lunch',
       'Morning snorkelling at a manta ray cleaning station. Lunch on a private sandbank — just you, the ocean, and a chef. Afternoon dolphin cruise as the sun paints the sky in psychedelic colours.'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 3, 'Island Hopping & Local Life',
       'Visit a local island to see Maldivian village life, a traditional fish market, and a handicraft centre. Snorkel a coral garden teeming with clownfish, parrotfish, and sea turtles. Night fishing experience.'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 4, 'Spa & Sunset',
       'A day of pure indulgence. Morning spa treatment in an overwater treatment room. Afternoon at leisure — read, swim, or nap in a hammock. Farewell dinner on the beach under the stars.'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

INSERT INTO trv_package_itinerary_days (package_id, day_number, title, description)
SELECT p.id, 5, 'Departure',
       'Final breakfast in paradise. Speedboat back to Malé. Flight to Dhaka with memories to last a lifetime.'
FROM trv_packages p WHERE p.package_code = 'P-MALD-01'
ON CONFLICT DO NOTHING;

COMMIT;
