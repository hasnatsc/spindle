-- ============================================================================
-- V10__org_module_access.sql
-- Organization module access control + org-admin scopes
-- ============================================================================
-- Run: Flyway picks this up automatically on next startup.
-- ============================================================================

-- ── sec_org_modules ──────────────────────────────────────────────────────────
-- One row per (org, module). active=true means the org can use that module.
-- Super admin manages these rows via /security/org-modules UI.

CREATE TABLE IF NOT EXISTS sec_org_modules (
    id              BIGSERIAL       PRIMARY KEY,
    organization_id BIGINT          NOT NULL REFERENCES org_organizations(id),
    module_key      VARCHAR(60)     NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    granted_by      VARCHAR(100),
    granted_at      TIMESTAMP,
    revoked_by      VARCHAR(100),
    revoked_at      TIMESTAMP,
    notes           VARCHAR(500),

    CONSTRAINT uq_org_module UNIQUE (organization_id, module_key)
);

CREATE INDEX IF NOT EXISTS idx_om_org    ON sec_org_modules (organization_id);
CREATE INDEX IF NOT EXISTS idx_om_module ON sec_org_modules (module_key);
CREATE INDEX IF NOT EXISTS idx_om_active ON sec_org_modules (active);

COMMENT ON TABLE  sec_org_modules IS
    'Controls which ERP modules each organization is licensed/authorized to use.';
COMMENT ON COLUMN sec_org_modules.module_key IS
    'Must match the module column in sec_permissions (e.g. HRM, SALES_CUSTOMER_OPERATIONS).';
COMMENT ON COLUMN sec_org_modules.active IS
    'true = org can use this module; false = blocked at authorization layer.';


-- ── sec_org_admin_scopes ──────────────────────────────────────────────────────
-- Designates users as org-level administrators.
-- An org-admin can manage users/roles/permissions WITHIN their org
-- but only for modules that are active for that org.

CREATE TABLE IF NOT EXISTS sec_org_admin_scopes (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES sec_users(id),
    organization_id BIGINT          NOT NULL REFERENCES org_organizations(id),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    granted_by      VARCHAR(100),
    granted_at      TIMESTAMP,
    notes           VARCHAR(500),

    CONSTRAINT uq_oas_user_org UNIQUE (user_id, organization_id)
);

CREATE INDEX IF NOT EXISTS idx_oas_user ON sec_org_admin_scopes (user_id);
CREATE INDEX IF NOT EXISTS idx_oas_org  ON sec_org_admin_scopes (organization_id);

COMMENT ON TABLE  sec_org_admin_scopes IS
    'Grants a user org-admin privileges for a specific organization.';
COMMENT ON COLUMN sec_org_admin_scopes.active IS
    'Super admin can revoke by setting active=false without deleting the row.';


-- ── Seed CORE_SECURITY for the default organization ────────────────────────
-- The default org (created by SecurityDataInitializer) always gets CORE_SECURITY
-- so the admin user can log in and manage security from day one.
-- All other modules are OFF by default — super admin enables them as needed.

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
            (v_org_id, 'FIXED_ASSETS',               false, 'system', NOW(), 'Disabled by default')
        ON CONFLICT (organization_id, module_key) DO NOTHING;
    END IF;
END $$;
