# Org-Module Access Control — Integration Guide

## What this adds

```
Super Admin
  └── manages which MODULES each Org can use   (OrgModule / sec_org_modules)
  └── designates Org Admins for each org        (OrgAdminScope / sec_org_admin_scopes)

Org Admin
  └── manages Users / Roles / Permissions       (existing — now filtered)
  └── CANNOT assign permissions for disabled modules
  └── CANNOT see other organizations

DynamicAuthorizationManager
  └── Layer 1: does user have a matching permission?   (existing)
  └── Layer 2: is that permission's module ACTIVE for user's org?  (new)
```

---

## Files changed / added

| File | Status | Notes |
|---|---|---|
| `security/entity/OrgModule.java` | NEW | `sec_org_modules` entity |
| `security/entity/OrgAdminScope.java` | NEW | `sec_org_admin_scopes` entity |
| `security/repository/OrgModuleRepository.java` | NEW | |
| `security/repository/OrgAdminScopeRepository.java` | NEW | |
| `security/dto/OrgModuleDTO.java` | NEW | |
| `security/service/OrgModuleService.java` | NEW | Core business logic |
| `security/controller/OrgModuleController.java` | NEW | REST + pages |
| `security/auth/DynamicAuthorizationManager.java` | UPDATED | +org-module cache layer |
| `security/service/RoleService.java` | UPDATED | +findAllForCurrentOrg() |
| `security/service/RoleServiceImpl.java` | UPDATED | +org-admin module enforcement |
| `db/migration/V10__org_module_access.sql` | NEW | Flyway migration |

---

## Step-by-step integration

### 1. Run the migration
Flyway picks up `V10__org_module_access.sql` automatically on startup.
Check that your last Flyway version is V9 or lower. Rename to V11/V12 if needed.

### 2. Enable all modules for your existing org (first time setup)
After migration, all modules except CORE_SECURITY are **OFF** for the default org.
Log in as superadmin → `/security/org-modules` → enable the modules you use.

The super admin can also use the API directly:
```sql
UPDATE sec_org_modules SET active = true WHERE organization_id = 1;
```

### 3. Register the new Thymeleaf pages
Create two templates (see structure below):
- `templates/security/org-modules-index.html`  — overview of all orgs
- `templates/security/org-modules-detail.html` — module grid for one org

### 4. Add navigation menu entries
In your AppMenu seed / admin UI, add:
```
MODULE: Security
  GROUP: Administration
    LEAF: Organization Module Access   → /security/org-modules   (SUPER_ADMIN only)
```

### 5. Update RoleController to use findAllForCurrentOrg()
In `RoleController.allPermissions()` and the roles dropdown endpoint,
call `roleService.findAllForCurrentOrg()` instead of `roleService.findAll()`:

```java
// RoleController.java — role dropdown for user-form
@GetMapping("/for-current-org")
@ResponseBody
public List<Map<String, Object>> rolesForCurrentOrg() {
    return roleService.findAllForCurrentOrg().stream()
            .map(r -> Map.of("id", r.getId(), "name", r.getName()))
            .toList();
}
```

And in the user-form JS, load roles via `/security/roles/for-current-org`
instead of `/security/roles/list`.

### 6. Permission seeds for OrgModuleController
Add these permissions to your seed SQL so super-admin can access the UI
(super-admin bypasses the check anyway, but the permission rows are needed
for the menu system to show the link):

```sql
INSERT INTO sec_permissions (name, description, url_pattern, http_method, module, active)
VALUES
  ('ORG_MODULE_VIEW',   'View org module access',   '/security/org-modules/**', 'GET',  'CORE_SECURITY', true),
  ('ORG_MODULE_MANAGE', 'Manage org module access',  '/security/org-modules/**', 'POST', 'CORE_SECURITY', true)
ON CONFLICT (name) DO NOTHING;
```

---

## Authorization flow (per request)

```
Browser → POST /purchase/orders
    DynamicAuthorizationManager.authorize()
        1. Is user authenticated?           No → 401
        2. Is user SUPER_ADMIN?             Yes → GRANT (skip to 5)
        3. Does user have any permission
           whose urlPattern matches
           /purchase/orders?                No → DENY
        4. Is that permission's module
           (PURCHASE_SUPPLIER) ACTIVE for
           the user's org?                 No → DENY
        5. GRANT
```

Cache TTL: **5 minutes** (both permission and org-module caches).
Immediate invalidation: every module toggle calls `authManager.invalidateCache()`.

---

## Org-admin role assignment enforcement

When an org-admin saves a role with permissions:

```
RoleServiceImpl.create()
  → resolvePermissions(dto.getPermissionIds())
  → assertModulesAllowed(orgId, perms)          ← NEW
       → orgModuleService.assertPermissionsAllowedForOrg(orgId, moduleKeys)
            → if any moduleKey ∉ activeModules for org → throw 400
```

This means: if COMMERCIAL is OFF for Org A, the Org A admin cannot create
a role that includes any COMMERCIAL permission — even if they somehow get
the permission IDs.

---

## New tables

### sec_org_modules
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| organization_id | BIGINT FK | → org_organizations |
| module_key | VARCHAR(60) | e.g. "HRM", "PRODUCTION" |
| active | BOOLEAN | true = module ON for this org |
| granted_by | VARCHAR(100) | username of superadmin who enabled |
| granted_at | TIMESTAMP | |
| revoked_by | VARCHAR(100) | username who last disabled |
| revoked_at | TIMESTAMP | |
| notes | VARCHAR(500) | free text |

### sec_org_admin_scopes
| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK | → sec_users |
| organization_id | BIGINT FK | → org_organizations |
| active | BOOLEAN | revocable without delete |
| granted_by | VARCHAR(100) | |
| granted_at | TIMESTAMP | |
| notes | VARCHAR(500) | |
