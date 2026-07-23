-- =============================================================================
--  Spindle ERP  —  Travel Transaction Seed Data
--  File   : V106__seed_travel_transactions.sql
--  Target : PostgreSQL 15+
--
--  Realistic booking transactions to populate the travel portal and
--  admin booking management screens.
-- =============================================================================

BEGIN;

-- ═════════════════════════════════════════════════════════════════════════════
-- 1.  BOOKINGS  (various statuses for a realistic mix)
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_bookings (booking_no, booking_type, booking_date, travel_start_date, travel_end_date, status, currency, exchange_rate, subtotal_amount, discount_amount, tax_amount, total_amount, paid_amount, due_amount, remarks, party_id, sales_agent_id, organization_id, created_at, created_by, updated_at, updated_by)
VALUES
    ('BKG-25-0001', 'PACKAGE',  '2026-06-10', '2026-07-15', '2026-07-18', 'DRAFT',  'BDT', 1.0000, 18500.00, 1000.00, 0.00, 17500.00, 5000.00, 12500.00,
     'Cox''s Bazar family trip — 2 adults, 1 child. Early bird discount applied.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0002', 'PACKAGE',  '2026-06-12', '2026-08-01', '2026-08-04', 'DRAFT',  'BDT', 1.0000, 14500.00, 0.00, 0.00, 14500.00, 14500.00, 0.00,
     'Sylhet tea country weekend. Fully paid — wedding anniversary gift.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0003', 'PACKAGE',  '2026-06-15', '2026-07-20', '2026-07-24', 'PAID',       'BDT', 1.0000, 28500.00, 0.00, 0.00, 28500.00, 28500.00, 0.00,
     'Hill Tracts Explorer — corporate retreat for 6 people. Full payment received.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0004', 'HOTEL',    '2026-06-18', '2026-08-10', '2026-08-14', 'DRAFT',  'BDT', 1.0000, 42000.00, 2000.00, 0.00, 40000.00, 0.00, 40000.00,
     'Pan Pacific Dhaka — 4 nights executive suite. Corporate rate applied.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0005', 'COMBINED', '2026-06-20', '2026-09-05', '2026-09-12', 'DRAFT',      'BDT', 1.0000, 125000.00, 5000.00, 0.00, 120000.00, 0.00, 120000.00,
     'Maldives family holiday — 2 adults, 2 children. Awaiting customer confirmation.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0006', 'PACKAGE',  '2026-06-22', '2026-07-25', '2026-07-29', 'DRAFT',  'BDT', 1.0000, 22500.00, 0.00, 0.00, 22500.00, 0.00, 0.00,
     'Sundarbans trip — DRAFT due to weather. Full refund processed.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0007', 'AIR',      '2026-06-25', '2026-08-15', '2026-08-15', 'PAID',       'BDT', 1.0000, 24500.00, 0.00, 0.00, 24500.00, 24500.00, 0.00,
     'Dhaka–Cox''s Bazar return ticket. Web booking — instant confirmation.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0008', 'PACKAGE',  '2026-06-28', '2026-07-10', '2026-07-14', 'COMPLETED',  'BDT', 1.0000, 32000.00, 0.00, 0.00, 32000.00, 32000.00, 0.00,
     'Sylhet Romantic Escape — honeymoon package. Both guests very satisfied. Completed.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0009', 'PACKAGE',  '2026-07-01', '2026-08-20', '2026-08-22', 'DRAFT',      'BDT', 1.0000, 16500.00, 0.00, 0.00, 16500.00, 0.00, 16500.00,
     'Sajek Valley quick getaway — website enquiry, not yet contacted.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0010', 'HOTEL',    '2026-07-02', '2026-09-01', '2026-09-05', 'DRAFT',  'BDT', 1.0000, 28000.00, 0.00, 0.00, 28000.00, 14000.00, 14000.00,
     'Royal Tulip Sea Pearl — 4 nights, sea-view suite. 50% advance paid.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0011', 'PACKAGE',  '2026-07-05', '2026-10-01', '2026-10-07', 'DRAFT',  'BDT', 1.0000, 65000.00, 0.00, 0.00, 65000.00, 20000.00, 45000.00,
     'Nepal Himalayan Adventure — group of 4 friends. Deposit paid, balance due 2 weeks before departure.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0012', 'PACKAGE',  '2026-07-08', '2026-07-28', '2026-07-30', 'DRAFT',  'BDT', 1.0000, 12500.00, 0.00, 0.00, 12500.00, 12500.00, 0.00,
     'Dhaka City Discovery — 2 nights, single traveller. All inclusive.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0013', 'COMBINED', '2026-07-10', '2026-08-05', '2026-08-10', 'DRAFT', 'BDT', 1.0000, 78000.00, 3000.00, 0.00, 75000.00, 30000.00, 45000.00,
     'Singapore & Malaysia — 6 nights family trip. First instalment paid.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0014', 'HOTEL',    '2026-07-12', '2026-07-30', '2026-08-02', 'DRAFT',      'BDT', 1.0000, 12500.00, 0.00, 0.00, 12500.00, 0.00, 12500.00,
     'Hotel Agrabad Chittagong — 3 nights business trip. Not yet approved.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0015', 'PACKAGE',  '2026-07-14', '2026-09-15', '2026-09-19', 'DRAFT',  'BDT', 1.0000, 22500.00, 0.00, 0.00, 22500.00, 22500.00, 0.00,
     'Sundarbans Explorer — photography tour with naturalist guide. Full payment.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0016', 'AIR',      '2026-07-15', '2026-08-12', '2026-08-12', 'DRAFT',  'BDT', 1.0000, 32000.00, 0.00, 0.00, 32000.00, 0.00, 32000.00,
     'Dhaka–Dubai return — Emirates Economy. Booking DRAFT, payment pending.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0017', 'PACKAGE',  '2026-07-16', '2026-09-25', '2026-09-29', 'DRAFT',      'BDT', 1.0000, 18500.00, 0.00, 0.00, 18500.00, 0.00, 18500.00,
     'Cox''s Bazar return trip — returning customer. Waiting for dates confirmation.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0018', 'HOTEL',    '2026-07-18', '2026-08-18', '2026-08-22', 'DRAFT',  'BDT', 1.0000, 35000.00, 0.00, 0.00, 35000.00, 17500.00, 17500.00,
     'Six Seasons Dhaka — 4 nights deluxe room. Corporate account.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0019', 'PACKAGE',  '2026-07-20', '2026-10-10', '2026-10-14', 'DRAFT',      'BDT', 1.0000, 16500.00, 0.00, 0.00, 16500.00, 0.00, 16500.00,
     'Sajek Valley Cloud Trail — website enquiry, pax 2.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0020', 'PACKAGE',  '2026-07-22', '2026-08-25', '2026-08-30', 'PAID',       'BDT', 1.0000, 85000.00, 0.00, 0.00, 85000.00, 85000.00, 0.00,
     'Maldives Island Paradise — 5th anniversary celebration. Full payment received.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0021', 'COMBINED', '2026-07-24', '2026-11-01', '2026-11-07', 'DRAFT',  'BDT', 1.0000, 52000.00, 2000.00, 0.00, 50000.00, 25000.00, 25000.00,
     'Bangkok & Pattaya — 6 nights, 2 adults. Half paid.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0022', 'HOTEL',    '2026-07-25', '2026-09-10', '2026-09-13', 'DRAFT',  'BDT', 1.0000, 14000.00, 0.00, 0.00, 14000.00, 0.00, 0.00,
     'Rose View Sylhet — DRAFT due to scheduling conflict.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0023', 'PACKAGE',  '2026-07-26', '2026-10-20', '2026-10-26', 'DRAFT',  'BDT', 1.0000, 28500.00, 1000.00, 0.00, 27500.00, 10000.00, 17500.00,
     'Hill Tracts Explorer — university geography department field trip. 8 students, 2 faculty. Deposit paid.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0024', 'AIR',      '2026-07-28', '2026-09-20', '2026-09-20', 'DRAFT',      'BDT', 1.0000, 8500.00, 0.00, 0.00, 8500.00, 0.00, 8500.00,
     'Dhaka–Sylhet one-way — Novoair. Single traveller.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system'),

    ('BKG-25-0025', 'PACKAGE',  '2026-07-30', '2026-11-15', '2026-11-19', 'DRAFT',      'BDT', 1.0000, 22500.00, 0.00, 0.00, 22500.00, 0.00, 22500.00,
     'Sundarbans Explorer — corporate team-building enquiry. 12 pax tentative.', NULL, NULL, 1, NOW(), 'system', NOW(), 'system')
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 2.  BOOKING SERVICES  (service lines for each booking)
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Cox''s Bazar Beach & Marine Getaway — 4D/3N package', 1, 17500.00, 12000.00, 1000.00, 0.00, 17500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-COX-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0001'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'HOTEL', 'Ocean Paradise Hotel — Deluxe Sea-View Room × 3 nights', 1, 10000.00, 7000.00, 0.00, 0.00, 10000.00, (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-COX-001'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0001'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'TOUR', 'Himchori Sunset & Waterfall Hike — for 2 adults', 2, 1200.00, 600.00, 0.00, 0.00, 2400.00, (SELECT id FROM trv_tours WHERE tour_code = 'T-COX-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0001'
ON CONFLICT DO NOTHING;

-- Booking 2: Sylhet Tea Country
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Sylhet Tea Country & Nature Retreat — 3D/2N', 1, 14500.00, 9500.00, 0.00, 0.00, 14500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-SYL-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0002'
ON CONFLICT DO NOTHING;

-- Booking 3: Hill Tracts Explorer (corporate retreat, 6 pax)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Hill Tracts Explorer — Bandarban & Rangamati 5D/4N', 1, 28500.00, 18000.00, 0.00, 0.00, 28500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-BND-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0003'
ON CONFLICT DO NOTHING;

-- Booking 5: Maldives (DRAFT, multi-service)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Maldives Island Paradise — 5D/4N overwater villa', 2, 85000.00, 55000.00, 5000.00, 0.00, 85000.00, (SELECT id FROM trv_packages WHERE package_code = 'P-MALD-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0005'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'AIR', 'Dhaka–Malé–Dhaka — Biman Bangladesh (Economy)', 4, 18000.00, 14000.00, 0.00, 0.00, 72000.00, NULL, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0005'
ON CONFLICT DO NOTHING;

-- Booking 8: Sylhet Romantic Escape (COMPLETED)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Sylhet & Srimangal Romantic Escape — 4D/3N', 1, 32000.00, 20000.00, 0.00, 0.00, 32000.00, (SELECT id FROM trv_packages WHERE package_code = 'P-SYLC-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0008'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'HOTEL', 'Grand Sultan Tea Resort — Luxury Suite 3 nights', 1, 15000.00, 9000.00, 0.00, 0.00, 15000.00, (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-SYL-001'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0008'
ON CONFLICT DO NOTHING;

-- Booking 11: Nepal (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Kathmandu & Pokhara Himalayan Adventure — 7D/6N', 1, 65000.00, 42000.00, 0.00, 0.00, 65000.00, (SELECT id FROM trv_packages WHERE package_code = 'P-NPL-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0011'
ON CONFLICT DO NOTHING;

-- Booking 13: Singapore & Malaysia (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Singapore & Malaysia — 7D/6N', 1, 78000.00, 52000.00, 3000.00, 0.00, 78000.00, (SELECT id FROM trv_packages WHERE package_code = 'P-SIN-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0013'
ON CONFLICT DO NOTHING;

-- Booking 15: Sundarbans (DRAFT, photography tour)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Sundarbans Explorer — Tiger Territory 3D/2N', 1, 22500.00, 15000.00, 0.00, 0.00, 22500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-KHL-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0015'
ON CONFLICT DO NOTHING;

-- Booking 20: Maldives (PAID)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Maldives Island Paradise — 5D/4N', 1, 85000.00, 55000.00, 0.00, 0.00, 85000.00, (SELECT id FROM trv_packages WHERE package_code = 'P-MALD-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0020'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'AIR', 'Dhaka–Malé–Dhaka — Biman Bangladesh Business Class', 2, 32000.00, 24000.00, 0.00, 0.00, 64000.00, NULL, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0020'
ON CONFLICT DO NOTHING;

-- Booking 21: Bangkok (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Bangkok & Pattaya — 5D/4N', 2, 52000.00, 32000.00, 2000.00, 0.00, 52000.00, (SELECT id FROM trv_packages WHERE package_code = 'P-THAI-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0021'
ON CONFLICT DO NOTHING;

-- Booking 4: Pan Pacific Dhaka (HOTEL, DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'HOTEL', 'Pan Pacific Sonargaon — Executive Suite 4 nights', 1, 40000.00, 22000.00, 2000.00, 0.00, 40000.00, (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-DHK-001'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0004'
ON CONFLICT DO NOTHING;

-- Booking 6: Sundarbans (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Sundarbans Explorer — Tiger Territory 3D/2N', 1, 22500.00, 15000.00, 0.00, 0.00, 22500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-KHL-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0006'
ON CONFLICT DO NOTHING;

-- Booking 7: Dhaka–Cox's Bazar Air (PAID)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'AIR', 'Dhaka–Cox''s Bazar–Dhaka — US-Bangla Airlines (Economy)', 1, 24500.00, 18000.00, 0.00, 0.00, 24500.00, NULL, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0007'
ON CONFLICT DO NOTHING;

-- Booking 9: Sajek Valley (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Sajek Valley Cloud Trail — 3D/2N', 1, 16500.00, 10000.00, 0.00, 0.00, 16500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-SAJEK-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0009'
ON CONFLICT DO NOTHING;

-- Booking 10: Royal Tulip Sea Pearl (HOTEL, DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'HOTEL', 'Royal Tulip Sea Pearl — Sea-View Suite 4 nights', 1, 28000.00, 16000.00, 0.00, 0.00, 28000.00, (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-COX-002'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0010'
ON CONFLICT DO NOTHING;

-- Booking 12: Dhaka City Discovery (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Dhaka City & Heritage Discovery — 3D/2N', 1, 12500.00, 7500.00, 0.00, 0.00, 12500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-DHK-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0012'
ON CONFLICT DO NOTHING;

-- Booking 14: Hotel Agrabad (HOTEL, DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'HOTEL', 'Hotel Agrabad — Executive Room 3 nights', 1, 12500.00, 7500.00, 0.00, 0.00, 12500.00, (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-SG-001'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0014'
ON CONFLICT DO NOTHING;

-- Booking 16: Dhaka–Dubai Air (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'AIR', 'Dhaka–Dubai–Dhaka — Emirates Economy Class', 1, 32000.00, 24000.00, 0.00, 0.00, 32000.00, NULL, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0016'
ON CONFLICT DO NOTHING;

-- Booking 17: Cox's Bazar return (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Cox''s Bazar Beach & Marine Getaway — 4D/3N', 1, 18500.00, 12000.00, 0.00, 0.00, 18500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-COX-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0017'
ON CONFLICT DO NOTHING;

-- Booking 18: Six Seasons Dhaka (HOTEL, DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'HOTEL', 'Six Seasons Hotel — Deluxe Room 4 nights', 1, 35000.00, 20000.00, 0.00, 0.00, 35000.00, (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-DHK-003'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0018'
ON CONFLICT DO NOTHING;

-- Booking 19: Sajek Valley (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Sajek Valley Cloud Trail — 3D/2N', 1, 16500.00, 10000.00, 0.00, 0.00, 16500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-SAJEK-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0019'
ON CONFLICT DO NOTHING;

-- Booking 22: Rose View Sylhet (HOTEL, DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'HOTEL', 'Rose View Hotel — Standard Room 3 nights', 1, 14000.00, 8000.00, 0.00, 0.00, 14000.00, (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-SYL-002'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0022'
ON CONFLICT DO NOTHING;

-- Booking 23: Hill Tracts field trip (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Hill Tracts Explorer — Bandarban & Rangamati 5D/4N', 1, 27500.00, 18000.00, 1000.00, 0.00, 27500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-BND-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0023'
ON CONFLICT DO NOTHING;

-- Booking 24: Dhaka–Sylhet Air (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'AIR', 'Dhaka–Sylhet — Novoair (Economy) one-way', 1, 8500.00, 5500.00, 0.00, 0.00, 8500.00, NULL, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0024'
ON CONFLICT DO NOTHING;

-- Booking 25: Sundarbans team-building (DRAFT)
INSERT INTO trv_booking_services (booking_id, service_type, description, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, reference_id, created_at, created_by)
SELECT b.id, 'PACKAGE', 'Sundarbans Explorer — Tiger Territory 3D/2N', 1, 22500.00, 15000.00, 0.00, 0.00, 22500.00, (SELECT id FROM trv_packages WHERE package_code = 'P-KHL-01'), NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0025'
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 3.  PASSENGERS
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passport_number, passport_expiry, nationality, passenger_type, is_lead_passenger, phone, email, remarks, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Rafiq',   'Hassan',  '1985-03-15', 'MALE', 'AB123456', '2027-12-31', 'Bangladeshi', 'ADULT',  true,  '+8801711223344', 'rafiq.hassan@email.com',  'Lead passenger — family trip', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0001'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mrs', 'Nasreen', 'Hassan',  '1988-07-22', 'FEMALE', 'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0001'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Amin',    'Hassan',  '2017-05-10', 'MALE',   'CHILD', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0001'
ON CONFLICT DO NOTHING;

-- Booking 2: Sylhet — couple
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, remarks, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Shahid',  'Ahmed',   '1982-11-05', 'MALE',   'ADULT', true,  '+8801811223344', 'shahid.ahmed@email.com', 'Wedding anniversary trip', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0002'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mrs', 'Farida', 'Ahmed',   '1985-03-20', 'FEMALE', 'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0002'
ON CONFLICT DO NOTHING;

-- Booking 3: Hill Tracts — corporate group lead
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Kabir',   'Chowdhury', '1979-09-12', 'MALE',   'ADULT', true,  '+8801911223344', 'kabir.c@company.com', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0003'
ON CONFLICT DO NOTHING;

-- Booking 5: Maldives DRAFT
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Tanvir',  'Rahman',  '1987-06-30', 'MALE',   'ADULT', true,  '+8801712345678', 'tanvir@email.com', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0005'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mrs', 'Saima',  'Rahman',  '1990-12-15', 'FEMALE', 'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0005'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Arif',    'Rahman',  '2019-04-20', 'MALE',   'CHILD', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0005'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Ms', 'Amina',   'Rahman',  '2021-08-11', 'FEMALE', 'INFANT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0005'
ON CONFLICT DO NOTHING;

-- Booking 8: Romantic Escape (COMPLETED)
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Fahim',   'Islam',   '1992-01-14', 'MALE',   'ADULT', true,  '+8801812345678', 'fahim.islam@email.com', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0008'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mrs', 'Nabila', 'Islam',   '1994-09-08', 'FEMALE', 'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0008'
ON CONFLICT DO NOTHING;

-- Booking 11: Nepal (DRAFT, group)
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Imtiaz',  'Hossain', '1984-03-22', 'MALE',   'ADULT', true,  '+8801911223344', 'imtiaz.h@email.com', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0011'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Zubair', 'Karim',   '1986-07-15', 'MALE',   'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0011'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Mehedi', 'Hasan',   '1990-11-30', 'MALE',   'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0011'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Rashed', 'Siddique','1988-05-10', 'MALE',   'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0011'
ON CONFLICT DO NOTHING;

-- Booking 13: Singapore & Malaysia (DRAFT)
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Shafiq', 'Ahmed',   '1980-08-05', 'MALE',   'ADULT', true,  '+8801712345678', 'shafiq.a@email.com', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0013'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mrs', 'Roksana', 'Ahmed',  '1983-04-12', 'FEMALE', 'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0013'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Saad',    'Ahmed',   '2016-01-25', 'MALE',   'CHILD', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0013'
ON CONFLICT DO NOTHING;

-- Booking 15: Sundarbans Photography
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Tariq',   'Hassan',  '1976-12-01', 'MALE',   'ADULT', true,  '+8801711229988', 'tariq.h@photography.com', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0015'
ON CONFLICT DO NOTHING;

-- Booking 20: Maldives (PAID) — anniversary couple
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Farhan',  'Siddiqui','1989-06-18', 'MALE',   'ADULT', true,  '+8801811002233', 'farhan.s@email.com', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0020'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mrs', 'Samira', 'Siddiqui','1992-02-14', 'FEMALE', 'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0020'
ON CONFLICT DO NOTHING;

-- Booking 21: Bangkok (DRAFT)
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Mr', 'Jahid',   'Malik',   '1991-10-08', 'MALE',   'ADULT', true,  '+8801711009988', 'jahid.m@email.com', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0021'
ON CONFLICT DO NOTHING;

INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, organization_id, created_at, created_by)
SELECT b.id, 'Mrs', 'Tahmina','Malik',   '1993-05-22', 'FEMALE', 'ADULT', false, 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0021'
ON CONFLICT DO NOTHING;

-- Booking 23: Hill Tracts — geography field trip lead
INSERT INTO trv_passengers (booking_id, title, first_name, last_name, date_of_birth, gender, passenger_type, is_lead_passenger, phone, email, organization_id, created_at, created_by)
SELECT b.id, 'Dr', 'Ataur',   'Rahman',  '1972-03-15', 'MALE',   'ADULT', true,  '+8801911005566', 'ataur.rahman@university.edu', 1, NOW(), 'system'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0023'
ON CONFLICT DO NOTHING;

-- ═════════════════════════════════════════════════════════════════════════════
-- 4.  BOOKING STATUS HISTORY
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system', NOW() - INTERVAL '5 days',  'Booking created via web enquiry'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0001'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin', NOW() - INTERVAL '3 days', 'Customer DRAFT via phone. Early bird discount applied.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0001'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '4 days', 'Booking created via website — Sylhet romantic package'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0002'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '2 days', 'Full payment received via bKYC. Booking DRAFT.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0002'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '7 days', 'Corporate retreat booking — 6 pax'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0003'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '5 days', 'Corporate rate negotiated. Full payment via bank transfer.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0003'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'PAID',      'admin',    NOW() - INTERVAL '4 days', 'Full amount credited. Booking marked as paid.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0003'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '6 days', 'Maldives family holiday enquiry — 2A+2C'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0005'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '10 days', 'Sundarbans enquiry — photography tour'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0006'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '8 days', 'DRAFT due to monsoon weather warning. Full refund processed.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0006'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '20 days', 'Honeymoon package booking — Sylhet romantic escape'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0008'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '18 days', 'DRAFT. Special honeymoon arrangement requested.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0008'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'COMPLETED', 'system',   NOW() - INTERVAL '3 days', 'Trip completed. Follow-up feedback collected.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0008'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '2 days', 'Nepal trekking group — 4 friends'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0011'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '1 day', 'Group deposit received. Flights booked.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0011'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '8 days', 'Singapore & Malaysia family trip enquiry'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0013'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '5 days', 'DRAFT. First instalment plan set up.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0013'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin', NOW() - INTERVAL '4 days', 'First instalment (40%) received. Remaining due 30 days before travel.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0013'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '3 days', 'Website enquiry — Sundarbans photography tour'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0015'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '1 day', 'Full payment received. Special photography guide arranged.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0015'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '6 days', 'Website enquiry — Maldives anniversary trip'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0020'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '4 days', 'Customer called to confirm. Requested overwater villa with sunset view.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0020'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'PAID',      'admin',    NOW() - INTERVAL '2 days', 'Full payment received via credit card. Happy anniversary!'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0020'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '1 day', 'Bangkok and Pattaya online enquiry'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0021'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '5 hours', 'DRAFT. Half payment received via mobile banking.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0021'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT',     'system',   NOW() - INTERVAL '12 days', 'University field trip booking — geography department'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0023'
ON CONFLICT DO NOTHING;

INSERT INTO trv_booking_status_history (booking_id, status, changed_by, changed_at, remarks)
SELECT b.id, 'DRAFT', 'admin',    NOW() - INTERVAL '8 days', 'DRAFT with university administration. Educational discount applied.'
FROM trv_bookings b WHERE b.booking_no = 'BKG-25-0023'
ON CONFLICT DO NOTHING;


-- ═════════════════════════════════════════════════════════════════════════════
-- 5.  HOTEL BOOKINGS  (reservations for each HOTEL-type booking service)
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_hotel_bookings (organization_id, booking_service_id, hotel_id,
    check_in_date, check_out_date, adults, children, rooms_count, status,
    total_amount, rate_per_night, nights,
    booking_currency, vendor_confirmation_received,
    created_at, created_by, updated_at)
SELECT 1,
    (SELECT bs.id FROM trv_booking_services bs JOIN trv_bookings b ON b.id = bs.booking_id
     WHERE b.booking_no = 'BKG-25-0001' AND bs.service_type = 'HOTEL'),
    (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-COX-001'),
    '2026-07-15', '2026-07-18', 2, 1, 2, 'CONFIRMED',
    10000.00, 3333.33, 3,
    'BDT', TRUE,
    NOW(), 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM trv_hotel_bookings hb
    JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
    JOIN trv_bookings b ON b.id = bs.booking_id WHERE b.booking_no = 'BKG-25-0001');

INSERT INTO trv_hotel_bookings (organization_id, booking_service_id, hotel_id,
    check_in_date, check_out_date, adults, children, rooms_count, status,
    total_amount, rate_per_night, nights,
    booking_currency, vendor_confirmation_received,
    created_at, created_by, updated_at)
SELECT 1,
    (SELECT bs.id FROM trv_booking_services bs JOIN trv_bookings b ON b.id = bs.booking_id
     WHERE b.booking_no = 'BKG-25-0004' AND bs.service_type = 'HOTEL'),
    (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-DHK-001'),
    '2026-08-10', '2026-08-14', 2, 0, 1, 'CONFIRMED',
    40000.00, 10000.00, 4,
    'BDT', TRUE,
    NOW(), 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM trv_hotel_bookings hb
    JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
    JOIN trv_bookings b ON b.id = bs.booking_id WHERE b.booking_no = 'BKG-25-0004');

INSERT INTO trv_hotel_bookings (organization_id, booking_service_id, hotel_id,
    check_in_date, check_out_date, adults, children, rooms_count, status,
    total_amount, rate_per_night, nights,
    booking_currency, vendor_confirmation_received,
    created_at, created_by, updated_at)
SELECT 1,
    (SELECT bs.id FROM trv_booking_services bs JOIN trv_bookings b ON b.id = bs.booking_id
     WHERE b.booking_no = 'BKG-25-0010' AND bs.service_type = 'HOTEL'),
    (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-COX-002'),
    '2026-09-01', '2026-09-05', 2, 0, 1, 'CONFIRMED',
    28000.00, 7000.00, 4,
    'BDT', TRUE,
    NOW(), 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM trv_hotel_bookings hb
    JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
    JOIN trv_bookings b ON b.id = bs.booking_id WHERE b.booking_no = 'BKG-25-0010');

INSERT INTO trv_hotel_bookings (organization_id, booking_service_id, hotel_id,
    check_in_date, check_out_date, adults, children, rooms_count, status,
    total_amount, rate_per_night, nights,
    booking_currency, vendor_confirmation_received,
    created_at, created_by, updated_at)
SELECT 1,
    (SELECT bs.id FROM trv_booking_services bs JOIN trv_bookings b ON b.id = bs.booking_id
     WHERE b.booking_no = 'BKG-25-0014' AND bs.service_type = 'HOTEL'),
    (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-SG-001'),
    '2026-07-30', '2026-08-02', 1, 0, 1, 'CONFIRMED',
    12500.00, 4166.67, 3,
    'BDT', TRUE,
    NOW(), 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM trv_hotel_bookings hb
    JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
    JOIN trv_bookings b ON b.id = bs.booking_id WHERE b.booking_no = 'BKG-25-0014');

INSERT INTO trv_hotel_bookings (organization_id, booking_service_id, hotel_id,
    check_in_date, check_out_date, adults, children, rooms_count, status,
    total_amount, rate_per_night, nights,
    booking_currency, vendor_confirmation_received,
    created_at, created_by, updated_at)
SELECT 1,
    (SELECT bs.id FROM trv_booking_services bs JOIN trv_bookings b ON b.id = bs.booking_id
     WHERE b.booking_no = 'BKG-25-0018' AND bs.service_type = 'HOTEL'),
    (SELECT id FROM trv_hotels WHERE hotel_code = 'HT-DHK-003'),
    '2026-08-18', '2026-08-22', 2, 0, 1, 'CONFIRMED',
    35000.00, 8750.00, 4,
    'BDT', TRUE,
    NOW(), 'system', NOW()
WHERE NOT EXISTS (SELECT 1 FROM trv_hotel_bookings hb
    JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
    JOIN trv_bookings b ON b.id = bs.booking_id WHERE b.booking_no = 'BKG-25-0018');

-- ═════════════════════════════════════════════════════════════════════════════
-- 6.  HOTEL ROOMS  (specific room assignments per hotel booking)
-- ═════════════════════════════════════════════════════════════════════════════
INSERT INTO trv_hotel_rooms (hotel_booking_id, room_number, room_type_snapshot)
SELECT hb.id, '1201', 'Deluxe King'
FROM trv_hotel_bookings hb
JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
JOIN trv_bookings b ON b.id = bs.booking_id
WHERE b.booking_no = 'BKG-25-0001'
  AND NOT EXISTS (SELECT 1 FROM trv_hotel_rooms r WHERE r.hotel_booking_id = hb.id AND r.room_number = '1201');

INSERT INTO trv_hotel_rooms (hotel_booking_id, room_number, room_type_snapshot)
SELECT hb.id, '1202', 'Deluxe Twin'
FROM trv_hotel_bookings hb
JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
JOIN trv_bookings b ON b.id = bs.booking_id
WHERE b.booking_no = 'BKG-25-0001'
  AND NOT EXISTS (SELECT 1 FROM trv_hotel_rooms r WHERE r.hotel_booking_id = hb.id AND r.room_number = '1202');

INSERT INTO trv_hotel_rooms (hotel_booking_id, room_number, room_type_snapshot)
SELECT hb.id, '805', 'Executive Suite'
FROM trv_hotel_bookings hb
JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
JOIN trv_bookings b ON b.id = bs.booking_id
WHERE b.booking_no = 'BKG-25-0004'
  AND NOT EXISTS (SELECT 1 FROM trv_hotel_rooms r WHERE r.hotel_booking_id = hb.id AND r.room_number = '805');

INSERT INTO trv_hotel_rooms (hotel_booking_id, room_number, room_type_snapshot)
SELECT hb.id, '501', 'Family Suite'
FROM trv_hotel_bookings hb
JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
JOIN trv_bookings b ON b.id = bs.booking_id
WHERE b.booking_no = 'BKG-25-0010'
  AND NOT EXISTS (SELECT 1 FROM trv_hotel_rooms r WHERE r.hotel_booking_id = hb.id AND r.room_number = '501');

INSERT INTO trv_hotel_rooms (hotel_booking_id, room_number, room_type_snapshot)
SELECT hb.id, '1108', 'Presidential Suite'
FROM trv_hotel_bookings hb
JOIN trv_booking_services bs ON bs.id = hb.booking_service_id
JOIN trv_bookings b ON b.id = bs.booking_id
WHERE b.booking_no = 'BKG-25-0018'
  AND NOT EXISTS (SELECT 1 FROM trv_hotel_rooms r WHERE r.hotel_booking_id = hb.id AND r.room_number = '1108');

COMMIT;
