-- Path: src/main/resources/db/migration/V_add_ec_customer_password_hash.sql
-- Adds password_hash column to ec_customers for storefront self-service auth.
-- EcCustomer does NOT extend BaseEntity and has no ERP-side password management;
-- this column is exclusively for StorefrontAuthService (BCrypt, cost factor 12).

ALTER TABLE ec_customers
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

COMMENT ON COLUMN ec_customers.password_hash IS
    'BCrypt hash for storefront self-service login. NULL = customer was created by admin/import and has not set a password yet (must use "forgot password" flow before first login).';

CREATE INDEX IF NOT EXISTS idx_ec_cust_email_lookup
    ON ec_customers (organization_id, email)
    WHERE email IS NOT NULL;
