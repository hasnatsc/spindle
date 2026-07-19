-- ============================================================================
-- V10__org_module_access.sql
-- Organization module access control seed data
-- ============================================================================
-- NOTE: Tables sec_org_modules and sec_org_admin_scopes are now defined in
--       V1__optimum_complete_schema_v2.sql (merged from earlier versions).
--       This file contains ONLY the seed data for those tables.
--
-- Safe to re-run: INSERTs use ON CONFLICT DO NOTHING.
-- ============================================================================

DO $$
DECLARE
    v_org_id BIGINT;
BEGIN
    SELECT id INTO v_org_id FROM org_organizations ORDER BY id ASC LIMIT 1;
    IF v_org_id IS NOT NULL THEN
        INSERT INTO sec_org_modules (organization_id, module_key, active, granted_by, granted_at, notes)
        VALUES
            (v_org_id, 'CORE_SECURITY',              true,  'system', NOW(), 'Always on'),
            (v_org_id, 'HRM',                        false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'SALES_CUSTOMER_OPERATIONS',  false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'PURCHASE_SUPPLIER',          false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'INVENTORY_WAREHOUSE',        false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'FINANCE_ACCOUNTS',           false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'PRODUCTION',                 false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'PRODUCT_CATALOG_ECOMMERCE',  false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'POS',                        false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'CRM',                        false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'COMMUNICATION_NOTIFICATION', false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'COMMERCIAL',                 false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'REPORTS_ANALYTICS',          false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'BUDGET',                     false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'FIXED_ASSETS',               false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'TRAVEL',                     false, 'system', NOW(), 'Disabled by default'),
            (v_org_id, 'ECOMMERCE',                  false, 'system', NOW(), 'Disabled by default')
        ON CONFLICT (organization_id, module_key) DO NOTHING;
    END IF;
END $$;
