-- =============================================================================
--  Optimum ERP — CRM Seed Data
--  File   : 09_seed_crm_data.sql
--  Target : PostgreSQL (crm_contacts, crm_leads, crm_opportunities,
--           crm_activities, crm_customer_feedback)
--
--  NOTE: assumes organization_id = 1 and the following already seeded:
--        acc_chart_of_accounts_sub (CUST-0001 from earlier script, used as
--        the "customer" reference everywhere here), sec_users (assigned_to_id
--        lookups via username — adjust column name to match your schema).
--        crm_customer_feedback.business_document_id and
--        crm_opportunities/leads converted_to references global_business_documents,
--        a table not in any schema shared so far — left NULL where relevant.
--
--  Execution order:
--    1. crm_leads          (depends on sec_users, COA sub-accounts for converted_to)
--    2. crm_opportunities  (depends on crm_leads, COA sub-accounts, sec_users)
--    3. crm_contacts       (depends on COA sub-accounts)
--    4. crm_activities     (depends on leads, opportunities, COA sub-accounts, sec_users)
--    5. crm_customer_feedback (depends on COA sub-accounts)
--
--  Idempotent via ON CONFLICT DO NOTHING on unique constraints where available.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. LEADS
-- =============================================================================

-- ── Lead 1: New lead, unassigned conversion yet ───────────────────────────────
INSERT INTO crm_leads
(lead_no, contact_name, designation, company_name, contact_email, contact_phone,
 city, country, lead_type, source, status, estimated_qty_kg, product_interest, remarks,
 organization_id, assigned_to_id, converted_to_id, created_at, updated_at, created_by, updated_by)
SELECT
    'LEAD-0001', 'Mahmudul Hasan', 'Procurement Head', 'Hasan Textiles Ltd.', 'mahmudul@hasantextiles.com', '+8801812345001',
    'Dhaka', 'Bangladesh', 'EXPORT', 'TRADE_FAIR', 'NEW', 5000.00, 'Industrial solvents and chemicals', 'Met at Dhaka Trade Fair 2026',
    1, u.id, NULL, NOW(), NOW(), 'system', 'system'
FROM sec_users u WHERE u.username = 'sales.manager'
ON CONFLICT ON CONSTRAINT uq_crl_org_no DO NOTHING;

-- ── Lead 2: Qualified, in progress ────────────────────────────────────────────
INSERT INTO crm_leads
(lead_no, contact_name, designation, company_name, contact_email, contact_phone,
 city, country, lead_type, source, status, estimated_qty_kg, product_interest, remarks,
 organization_id, assigned_to_id, converted_to_id, created_at, updated_at, created_by, updated_by)
SELECT
    'LEAD-0002', 'Farzana Rahman', 'Supply Chain Manager', 'Greenfield Apparels', 'farzana@greenfieldapparels.com', '+8801812345002',
    'Chattogram', 'Bangladesh', 'DOMESTIC', 'REFERRAL', 'QUALIFIED', 2000.00, 'Mobile devices for warehouse staff', 'Referred by existing customer ABC Trading',
    1, u.id, NULL, NOW(), NOW(), 'system', 'system'
FROM sec_users u WHERE u.username = 'sales.executive'
ON CONFLICT ON CONSTRAINT uq_crl_org_no DO NOTHING;

-- ── Lead 3: Converted — linked to existing customer sub-account ──────────────
INSERT INTO crm_leads
(lead_no, contact_name, designation, company_name, contact_email, contact_phone,
 city, country, lead_type, source, status, estimated_qty_kg, product_interest, remarks,
 organization_id, assigned_to_id, converted_to_id, created_at, updated_at, created_by, updated_by)
SELECT
    'LEAD-0003', 'Karim Uddin', 'Owner', 'ABC Trading Co.', 'karim@abctrading.com', '+8801711000111',
    'Dhaka', 'Bangladesh', 'DOMESTIC', 'WEBSITE', 'CONVERTED', 10000.00, 'Bulk industrial chemicals', 'Converted to active customer',
    1, u.id, cust.id, NOW(), NOW(), 'system', 'system'
FROM sec_users u, acc_chart_of_accounts_sub cust
WHERE u.username = 'sales.manager'
  AND cust.sub_account_code = 'CUST-0001' AND cust.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_crl_org_no DO NOTHING;

-- ── Lead 4: Lost ───────────────────────────────────────────────────────────────
INSERT INTO crm_leads
(lead_no, contact_name, designation, company_name, contact_email, contact_phone,
 city, country, lead_type, source, status, estimated_qty_kg, product_interest, remarks,
 organization_id, assigned_to_id, converted_to_id, created_at, updated_at, created_by, updated_by)
SELECT
    'LEAD-0004', 'Sabrina Akter', 'Procurement Officer', 'Northstar Industries', 'sabrina@northstar.com', '+8801812345004',
    'Gazipur', 'Bangladesh', 'DOMESTIC', 'COLD_CALL', 'LOST', 1500.00, 'Mechanical spare parts', 'Chose competitor due to pricing',
    1, u.id, NULL, NOW(), NOW(), 'system', 'system'
FROM sec_users u WHERE u.username = 'sales.executive'
ON CONFLICT ON CONSTRAINT uq_crl_org_no DO NOTHING;


-- =============================================================================
-- 2. OPPORTUNITIES  (depends on crm_leads, COA sub-accounts)
-- =============================================================================

-- ── Opportunity 1: From converted Lead 3, won ──────────────────────────────────
INSERT INTO crm_opportunities
(opportunity_no, title, description, stage, estimated_value, currency, probability,
 expected_close_date, actual_close_date, lost_reason, remarks,
 organization_id, assigned_to_id, customer_id, lead_id, created_at, updated_at, created_by, updated_by)
SELECT
    'OPP-0001', 'ABC Trading — Bulk Chemical Supply Contract', 'Annual supply contract for industrial chemicals',
    'WON', 1200000.00, 'BDT', 100.00,
    DATE '2026-05-15', DATE '2026-05-10', NULL, 'Closed won — first annual contract signed',
    1, u.id, cust.id, ld.id, NOW(), NOW(), 'system', 'system'
FROM sec_users u, acc_chart_of_accounts_sub cust, crm_leads ld
WHERE u.username = 'sales.manager'
  AND cust.sub_account_code = 'CUST-0001' AND cust.organization_id = 1
  AND ld.lead_no = 'LEAD-0003' AND ld.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_cro_org_no DO NOTHING;

-- ── Opportunity 2: From Lead 2, in negotiation ────────────────────────────────
INSERT INTO crm_opportunities
(opportunity_no, title, description, stage, estimated_value, currency, probability,
 expected_close_date, actual_close_date, lost_reason, remarks,
 organization_id, assigned_to_id, customer_id, lead_id, created_at, updated_at, created_by, updated_by)
SELECT
    'OPP-0002', 'Greenfield Apparels — Device Procurement', 'Bulk mobile device order for warehouse digitization',
    'NEGOTIATION', 350000.00, 'BDT', 60.00,
    DATE '2026-07-01', NULL, NULL, 'Negotiating volume discount',
    1, u.id, NULL, ld.id, NOW(), NOW(), 'system', 'system'
FROM sec_users u, crm_leads ld
WHERE u.username = 'sales.executive'
  AND ld.lead_no = 'LEAD-0002' AND ld.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_cro_org_no DO NOTHING;

-- ── Opportunity 3: Standalone (existing customer, no lead) ───────────────────
INSERT INTO crm_opportunities
(opportunity_no, title, description, stage, estimated_value, currency, probability,
 expected_close_date, actual_close_date, lost_reason, remarks,
 organization_id, assigned_to_id, customer_id, lead_id, created_at, updated_at, created_by, updated_by)
SELECT
    'OPP-0003', 'ABC Trading — Upsell Spare Parts', 'Cross-sell mechanical spare parts to existing customer',
    'PROSPECT', 150000.00, 'BDT', 20.00,
    DATE '2026-08-01', NULL, NULL, 'Early-stage discussion',
    1, u.id, cust.id, NULL, NOW(), NOW(), 'system', 'system'
FROM sec_users u, acc_chart_of_accounts_sub cust
WHERE u.username = 'sales.manager'
  AND cust.sub_account_code = 'CUST-0001' AND cust.organization_id = 1
ON CONFLICT ON CONSTRAINT uq_cro_org_no DO NOTHING;


-- =============================================================================
-- 3. CONTACTS  (depends on COA sub-accounts as customer reference)
-- =============================================================================
INSERT INTO crm_contacts
(first_name, last_name, designation, department, email, phone, mobile, whatsapp,
 is_primary, is_active, notes, organization_id, customer_id, created_at, updated_at, created_by, updated_by)
SELECT 'Karim', 'Uddin', 'Owner', 'Management', 'karim@abctrading.com', '+8801711000111', '+8801711000111', '+8801711000111',
       true, true, 'Primary decision maker for ABC Trading', 1, cust.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts_sub cust WHERE cust.sub_account_code = 'CUST-0001' AND cust.organization_id = 1;

INSERT INTO crm_contacts
(first_name, last_name, designation, department, email, phone, mobile, whatsapp,
 is_primary, is_active, notes, organization_id, customer_id, created_at, updated_at, created_by, updated_by)
SELECT 'Liza', 'Akhter', 'Accounts Manager', 'Finance', 'liza@abctrading.com', '+8801711000112', '+8801711000112', NULL,
       false, true, 'Handles invoicing and payments', 1, cust.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts_sub cust WHERE cust.sub_account_code = 'CUST-0001' AND cust.organization_id = 1;


-- =============================================================================
-- 4. ACTIVITIES  (depends on leads, opportunities, COA sub-accounts, sec_users)
-- =============================================================================

-- ── Activity 1: Call logged against Lead 1 ────────────────────────────────────
INSERT INTO crm_activities
(subject, activity_type, activity_date, description, outcome, status,
 duration_minutes, next_action, next_action_date,
 organization_id, assigned_to_id, customer_id, lead_id, opportunity_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'Initial discovery call', 'CALL', DATE '2026-06-05', 'Discussed solvent requirements and volume estimates',
    'Positive — interested in samples', 'COMPLETED',
    25, 'Send product samples and pricing sheet', DATE '2026-06-12',
    1, u.id, NULL, ld.id, NULL,
    NOW(), NOW(), 'system', 'system'
FROM sec_users u, crm_leads ld
WHERE u.username = 'sales.manager'
  AND ld.lead_no = 'LEAD-0001' AND ld.organization_id = 1;

-- ── Activity 2: Meeting logged against Opportunity 2 ──────────────────────────
INSERT INTO crm_activities
(subject, activity_type, activity_date, description, outcome, status,
 duration_minutes, next_action, next_action_date,
 organization_id, assigned_to_id, customer_id, lead_id, opportunity_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'Pricing negotiation meeting', 'MEETING', DATE '2026-06-10', 'On-site meeting to discuss volume discount on device order',
    'Client requested 8% discount, awaiting internal approval', 'COMPLETED',
    60, 'Get discount approval from GM', DATE '2026-06-17',
    1, u.id, NULL, NULL, opp.id,
    NOW(), NOW(), 'system', 'system'
FROM sec_users u, crm_opportunities opp
WHERE u.username = 'sales.executive'
  AND opp.opportunity_no = 'OPP-0002' AND opp.organization_id = 1;

-- ── Activity 3: Follow-up call logged against existing customer ──────────────
INSERT INTO crm_activities
(subject, activity_type, activity_date, description, outcome, status,
 duration_minutes, next_action, next_action_date,
 organization_id, assigned_to_id, customer_id, lead_id, opportunity_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'Quarterly check-in', 'FOLLOW_UP', DATE '2026-06-14', 'Routine check-in on contract performance and satisfaction',
    'Customer satisfied, mentioned interest in spare parts', 'COMPLETED',
    15, 'Prepare spare parts catalogue', DATE '2026-06-20',
    1, u.id, cust.id, NULL, opp.id,
    NOW(), NOW(), 'system', 'system'
FROM sec_users u, acc_chart_of_accounts_sub cust, crm_opportunities opp
WHERE u.username = 'sales.manager'
  AND cust.sub_account_code = 'CUST-0001' AND cust.organization_id = 1
  AND opp.opportunity_no = 'OPP-0003' AND opp.organization_id = 1;

-- ── Activity 4: Planned future visit ──────────────────────────────────────────
INSERT INTO crm_activities
(subject, activity_type, activity_date, description, outcome, status,
 duration_minutes, next_action, next_action_date,
 organization_id, assigned_to_id, customer_id, lead_id, opportunity_id,
 created_at, updated_at, created_by, updated_by)
SELECT
    'Site visit — Greenfield Apparels', 'VISIT', DATE '2026-06-25', 'Planned on-site visit to demo devices',
    NULL, 'PLANNED',
    90, NULL, NULL,
    1, u.id, NULL, ld.id, opp.id,
    NOW(), NOW(), 'system', 'system'
FROM sec_users u, crm_leads ld, crm_opportunities opp
WHERE u.username = 'sales.executive'
  AND ld.lead_no = 'LEAD-0002' AND ld.organization_id = 1
  AND opp.opportunity_no = 'OPP-0002' AND opp.organization_id = 1;


-- =============================================================================
-- 5. CUSTOMER FEEDBACK  (depends on COA sub-accounts)
-- =============================================================================
INSERT INTO crm_customer_feedback
(subject, feedback_type, feedback_date, description, rating, status,
 resolution, resolved_by, resolved_at, business_document_id,
 organization_id, customer_id, created_at, updated_at, created_by, updated_by)
SELECT
    'Late delivery complaint', 'COMPLAINT', DATE '2026-05-20', 'Shipment arrived 3 days later than agreed delivery date',
    2, 'RESOLVED',
    'Issued partial freight credit and revised logistics SLA', 'sales.manager', NOW() - INTERVAL '5 days', NULL,
    1, cust.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts_sub cust WHERE cust.sub_account_code = 'CUST-0001' AND cust.organization_id = 1;

INSERT INTO crm_customer_feedback
(subject, feedback_type, feedback_date, description, rating, status,
 resolution, resolved_by, resolved_at, business_document_id,
 organization_id, customer_id, created_at, updated_at, created_by, updated_by)
SELECT
    'Positive feedback on product quality', 'COMPLIMENT', DATE '2026-06-01', 'Customer praised consistent quality of recent solvent batch',
    5, 'CLOSED',
    'Thanked customer and shared feedback with QA team', 'sales.manager', NOW() - INTERVAL '2 days', NULL,
    1, cust.id, NOW(), NOW(), 'system', 'system'
FROM acc_chart_of_accounts_sub cust WHERE cust.sub_account_code = 'CUST-0001' AND cust.organization_id = 1;

COMMIT;

-- =============================================================================
--  VERIFICATION QUERIES
-- =============================================================================
-- SELECT 'Leads',         COUNT(*) FROM crm_leads
-- UNION ALL SELECT 'Opportunities', COUNT(*) FROM crm_opportunities
-- UNION ALL SELECT 'Contacts',      COUNT(*) FROM crm_contacts
-- UNION ALL SELECT 'Activities',    COUNT(*) FROM crm_activities
-- UNION ALL SELECT 'Feedback',      COUNT(*) FROM crm_customer_feedback;