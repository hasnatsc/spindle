# Spindle ERP — Spring Security Configuration
## Package: com.asg.spindleserp | Spring Boot 4.1.0 | Java 21

---

## File Placement

```
src/main/java/com/asg/spindleserp/
│
├── security/
│   ├── auth/
│   │   ├── CustomUserDetails.java          ← Wraps User entity, caches authorities
│   │   ├── DynamicAuthorizationManager.java ← Runtime URL authorization (no DB/request)
│   │   ├── LoginSuccessHandler.java         ← Updates last_login_at, redirects /dashboard
│   │   ├── LoginFailureHandler.java         ← Logs failed attempts, redirects /login?error
│   │   ├── CustomAccessDeniedHandler.java   ← JSON 403 for AJAX, redirect for browser
│   │   └── SecurityHelper.java              ← Static util: SecurityHelper.requireOrgId()
│   │
│   ├── config/
│   │   ├── SecurityConfig.java              ← Main @EnableWebSecurity configuration
│   │   └── JpaConfig.java                   ← @EnableJpaAuditing (createdBy/updatedBy)
│   │
│   ├── service/
│   │   ├── UserDetailsServiceImpl.java      ← Multi-field login (username/email/phone)
│   │   └── MenuService.java                 ← Builds navigation tree per user
│   │
│   ├── init/
│   │   └── SecurityDataInitializer.java     ← Seeds superadmin on first boot
│   │
│   ├── repository/
│   │   ├── UserRepository.java              ← With JOIN FETCH for security loading
│   │   ├── RoleRepository.java
│   │   ├── PermissionRepository.java
│   │   ├── AppMenuRepository.java
│   │   └── RoleMenuAccessRepository.java
│   │
│   └── entity/
│       ├── User.java                        ← YOUR FILE (uploaded)
│       ├── Role.java                        ← YOUR FILE (uploaded)
│       ├── Permission.java                  ← YOUR FILE (uploaded)
│       ├── AppMenu.java                     ← YOUR FILE (uploaded)
│       └── RoleMenuAccess.java              ← YOUR FILE (uploaded)

src/main/resources/
├── application.properties                   ← REPLACE with updated version
└── templates/
    ├── login.html                           ← Multi-field login page
    ├── access-denied.html                   ← 403 page

src/main/resources/static/js/
└── secureFetch.js                           ← Add to application.js or import separately
```

---

## Required: Role.permissions must be EAGER

Your `Role.java` has `permissions` loaded `FetchType.LAZY`.
This MUST be changed to EAGER, OR keep LAZY and ensure the
`UserDetailsServiceImpl` JOIN FETCH loads them in the same transaction.

The `UserRepository` queries already use `LEFT JOIN FETCH r.permissions` so
LAZY is fine as long as you use those queries. Do NOT call
`userRepository.findByUsername(plain)` in the security context — only use
the `WithRolesAndPermissions` variants.

---

## How the Authorization Flow Works

```
HTTP Request
     │
     ▼
SecurityConfig.filterChain()
     │
     ├─ PUBLIC? (/login, /css/**, etc.) → ALLOW immediately
     │
     └─ ANY OTHER URL
            │
            ▼
     DynamicAuthorizationManager.check()
            │
            ├─ Not authenticated? → DENY → redirect /login
            │
            ├─ ROLE_SUPER_ADMIN? → ALLOW immediately (no DB)
            │
            └─ Check user's cached authorities (built at login, stored in session)
                   │
                   └─ Match against permission.urlPattern + permission.httpMethod
                          (loaded from DB once, cached 5 minutes)
                                │
                                ├─ Match found → ALLOW
                                └─ No match → DENY → CustomAccessDeniedHandler
```

**Zero database hits per request for authorization.**

---

## CSRF in AJAX calls

All AJAX calls MUST use `secureFetch()` instead of raw `fetch()`:

```javascript
// ✅ Correct
const res = await secureFetch('/api/v1/items', {
    method: 'POST',
    body: JSON.stringify({ itemCode: 'RM001', itemName: 'Flour' })
});

// ❌ Wrong — missing CSRF token, POST will get 403
const res = await fetch('/api/v1/items', { method: 'POST', ... });
```

`secureFetch` automatically reads the `XSRF-TOKEN` cookie and sends it
as the `X-XSRF-TOKEN` header. Spring Security's `CookieCsrfTokenRepository`
validates this on every mutating request.

---

## @PreAuthorize on Controllers (optional second layer)

```java
@RestController
@RequestMapping("/api/v1/purchase/orders")
public class PurchaseOrderController {

    @GetMapping
    @PreAuthorize("hasAuthority('purchase.order.view')")
    public ResponseEntity<?> list(...) { ... }

    @PostMapping
    @PreAuthorize("hasAuthority('purchase.order.create')")
    public ResponseEntity<?> create(...) { ... }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('purchase.order.approve')")
    public ResponseEntity<?> approve(...) { ... }
}
```

---

## Thymeleaf — show/hide based on permission

```html
<!-- Using Spring Security Thymeleaf extras (already in your pom.xml) -->
<li sec:authorize="hasAuthority('purchase.order.view')">
    <a th:href="@{/purchase/orders}">Purchase Orders</a>
</li>

<button sec:authorize="hasAuthority('purchase.order.create')"
        class="btn btn-primary">New Order</button>

<!-- Show edit button only if user has canEdit for this menu -->
<button th:if="${menuAccess[menuId]?.canEdit}"
        class="btn btn-sm btn-warning">Edit</button>
```

---

## Default Credentials (CHANGE IMMEDIATELY)

```
Username : superadmin
Password : Admin@1234
```

---

## Database Tables Required

Spring Session JDBC needs two tables. With `spring.session.jdbc.initialize-schema=always`
Spring Boot will create them automatically.

If you use `never`, run this SQL manually:
```sql
-- Provided by Spring Session — also in spring-session-jdbc jar
CREATE TABLE SPRING_SESSION ( ... );
CREATE TABLE SPRING_SESSION_ATTRIBUTES ( ... );
```

---

## Permission Cache Invalidation

When an admin updates a Permission URL pattern, call:
```java
dynamicAuthorizationManager.invalidateCache();
```

Or it auto-refreshes every 5 minutes.

---

## superadmin Password After First Login

```java
// In a setup/admin controller:
@PostMapping("/settings/change-password")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDto dto) {
    User user = userRepo.findById(SecurityHelper.requireCurrentUser().getUserId()).orElseThrow();
    if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Current password incorrect"));
    }
    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    userRepo.save(user);
    return ResponseEntity.ok(ApiResponse.ok("Password changed", null));
}
```
