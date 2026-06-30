-- ============================================================
-- Flyway: V{NEXT}__add_ec_product_catalog_permissions.sql
-- Path: resources/db/migration/
-- Add eCommerce → Product Catalog menu + permissions
-- Idempotent: ON CONFLICT DO NOTHING
-- ============================================================

-- ── 1. Menu entry (parent = eCommerce section) ─────────────────────────────
INSERT INTO app_menus (name, url, icon, parent_id, display_order, active)
SELECT 'Product Catalog', '/ecommerce/products', 'fa-store', m.id, 20, true
FROM app_menus m WHERE m.name = 'eCommerce' AND m.parent_id IS NULL
ON CONFLICT (url) DO NOTHING;

-- ── 2. Permissions ─────────────────────────────────────────────────────────
INSERT INTO sec_permissions (name, description, module, active) VALUES
  ('ec.product.view',    'View EC Product Catalog list',     'ECOMMERCE', true),
  ('ec.product.create',  'Create EC Product',                'ECOMMERCE', true),
  ('ec.product.update',  'Update EC Product',                'ECOMMERCE', true),
  ('ec.product.delete',  'Delete EC Product',                'ECOMMERCE', true),
  ('ec.product.toggle',  'Activate/Deactivate EC Product',   'ECOMMERCE', true),
  ('ec.product.publish', 'Publish/Draft EC Product',         'ECOMMERCE', true)
ON CONFLICT (name) DO NOTHING;

-- ── 3. Assign to SUPER_ADMIN / ADMIN roles ─────────────────────────────────
INSERT INTO sec_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM   sec_roles r, sec_permissions p
WHERE  r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND  p.name IN (
         'ec.product.view', 'ec.product.create', 'ec.product.update',
         'ec.product.delete', 'ec.product.toggle', 'ec.product.publish')
ON CONFLICT DO NOTHING;
