# Spindle ERP — Security Analysis: eCommerce Backend Login + Storefront Customer Login

**Scope reviewed:** `security.zip`, `ecommerce.zip`, `organization.zip`, `pom.xml`, `application.properties`
**Stack confirmed from source:** Spring Boot 4.1.0 · Spring Security 7.x · Java 21 · PostgreSQL · Spring Session JDBC · Thymeleaf
**Deliverable:** 20 complete drop-in Java files + 1 `application.properties`. No partial patches, no stubs.

---

## 0. The one-paragraph version

The **ERP staff login** is architecturally sound — the seven session/CSRF fixes already in `SecurityConfig` were correct and are all retained. What it was missing is everything that sits *around* authentication: no brute-force limit at all, a remember-me signing key hard-coded in a Git-tracked file, no security response headers, and a failure handler that mutated shared singleton state on every request.

The **storefront customer login** is a different story. Because `EcCustomer` deliberately lives outside Spring Security, it also lives outside every protection Spring Security would have given it for free — and nobody re-implemented them. The result is a **session-fixation account takeover**, an **open redirect**, **no organisation binding on the session in a multi-tenant system**, and **unlimited password guessing**. Separately, `POST /cart/add` looks products up by primary key with no org filter, which is a **cross-tenant catalogue and pricing leak from an unauthenticated endpoint**.

Two live functional bugs also fell out of the review: **`/about`, `/contact`, `/faq` and all CMS pages redirect anonymous shoppers to the ERP login page**, and **`/login?disabled` and `/login?locked` have never once rendered** — they are unreachable dead code.

---

## 1. Findings

| # | Severity | Where | Finding |
|---|----------|-------|---------|
| 1 | **CRITICAL** | `StorefrontAuthService.loginSession()` | **Session fixation → customer account takeover.** Session ID is not rotated on login. |
| 2 | **HIGH** | `StorefrontAuthService.currentCustomerOrNull()` | **No org binding.** Session holds only `customerId`; the org invariant is asserted once at login and never re-checked. |
| 3 | **HIGH** | `StorefrontCartService.addItem()` | **Cross-tenant IDOR.** `productRepository.findById(productId)` — no org / published / active / deleted filter, on a `permitAll` endpoint. |
| 4 | **HIGH** | `StorefrontAuthController` | **Open redirect** via the raw `?redirect=` parameter → ready-made phishing primitive on the real domain. |
| 5 | **HIGH** | Both logins | **No brute-force protection anywhere.** `account_non_locked` existed and nothing ever set it. |
| 6 | **HIGH** | `SecurityConfig` | **Remember-me signing key hard-coded in source** (`"spindleErpRememberMeKey2026"`). It is a credential. |
| 7 | **HIGH** | `SecurityConfig.PUBLIC_URLS` | `/about`, `/contact`, `/faq`, `/page/**`, `/newsletter/**` missing → **anonymous shoppers bounced to the ERP login**. |
| 8 | **MEDIUM** | `LoginFailureHandler` | **Thread-safety bug.** `setDefaultFailureUrl()` mutates the shared `@Component` on every request. |
| 9 | **MEDIUM** | `UserDetailsServiceImpl` | `DisabledException` / `LockedException` get wrapped → **`/login?disabled` and `/login?locked` are dead code**. |
| 10 | **MEDIUM** | `StorefrontAuthService.login()` | **Account enumeration** by timing (~2 ms vs ~250 ms) *and* by message ("This account has been blocked"). |
| 11 | **MEDIUM** | `StorefrontAuthService.logout()` | Removes one attribute. Session, cart and **the customer's home address** (`SF_CHECKOUT_INFO`) survive. |
| 12 | **MEDIUM** | `SecurityConfig` | **No CSP, no Referrer-Policy, no Permissions-Policy.** |
| 13 | **MEDIUM** | Storefront controllers | Internal exception messages echoed to the browser via `catch (Exception e) → e.getMessage()`. |
| 14 | **MEDIUM** | `StorefrontCartService` | Quantity completely unbounded — `"1E+40"` is a valid `BigDecimal`. |
| 15 | **MEDIUM** | `application.properties` | `logging.level...web.csrf=TRACE` — **logs CSRF token values to disk**. |
| 16 | **LOW** | `StorefrontAuthService.recordLogin()` | `X-Forwarded-For` trusted unconditionally → **audit trail is attacker-controlled**. |
| 17 | **LOW** | Auth logging | Raw `username` parameter logged → **log injection (CWE-117)**. |
| 18 | **LOW** | `ContextProvider.ctx()` | Throws `IllegalStateException` on any non-request thread. `EcCustomer`'s field initialiser calls it on **every entity load**. |
| 19 | **LOW** | `StorefrontAuthService` | 6-char passwords, no composition rule; a second hand-built `BCryptPasswordEncoder` outside the container. |
| 20 | **LOW** | `StorefrontAuthService` | `"CUST-" + currentTimeMillis() % 1000000` collides against a UNIQUE constraint. Wraps every ~16 min. |

---

## 2. The three that actually matter

### 2.1 CRITICAL — Session fixation on the customer login

The entire login was:

```java
private void loginSession(HttpServletRequest request, EcCustomer customer) {
    HttpSession session = request.getSession(true);
    session.setAttribute(SESSION_CUSTOMER_ID, customer.getId());
}
```

The session ID is never rotated. `SecurityConfig` *does* configure `.sessionFixation().changeSessionId()` — but that fires on **Spring Security authentication events**, and this login path never touches Spring Security. So it never fires here.

**Exploit, end to end:**

1. Attacker loads the shop, receives `SESSION=abc123` — a valid, empty session.
2. Attacker gets the victim's browser to adopt that cookie value. Any of: a reflected XSS **anywhere on the domain or any subdomain** (the cookie has no `__Host-` prefix and no domain lock, so one XSS on any `*.asg.com` host is enough); a MITM on plain HTTP (`cookie.secure` was never set); a shared kiosk.
3. Victim logs in normally. `SF_CUSTOMER_ID` is written into session `abc123`. **The cookie value does not change**, because nothing rotates it.
4. Attacker, still holding `SESSION=abc123`, **is now the victim.** Order history, saved addresses, wallet balance, checkout — everything.

**Fix:** `request.changeSessionId()` on every successful login and registration. Spring Session's `SessionRepositoryRequestWrapper` implements it against the JDBC store, so the `SPRING_SESSION` primary key rotates atomically while all attributes survive — which is required, because `SF_CART_SESSION_ID` must live long enough for the guest-cart merge to run a moment later. This is precisely what Spring Security's own `ChangeSessionIdAuthenticationStrategy` does; the customer path simply never got it.

### 2.2 HIGH — Cross-tenant IDOR in add-to-cart

```java
EcProductCatalog product = productRepository.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("Product not found."));
```

`POST /cart/add` is `permitAll` (guests must be able to build a cart) and its body is `{"productId": N}`. No org check. No published check. No active check. No deleted check.

So any anonymous visitor can:

- **Enumerate ids 1..N and add another tenant's products to their cart.** The response echoes the whole cart back — `product_title`, `slug`, image URL, `unit_price`. That is a complete dump of every other organisation's catalogue *and their pricing*, from an unauthenticated endpoint, **in a multi-tenant ERP**.
- Add an **unpublished** product — a draft, an embargoed launch, a deliberately-hidden SKU — and then check out and buy it.
- Add a **soft-deleted** or **inactive** product.

Every catalogue query enforces `organization_id = ?` **and** `published = true AND active = true AND deleted = false`. The cart enforced none of it — it trusted that a product id could only have come from a page that had already applied those filters. **The listing page is a UI, not an access control**, and an attacker does not use the UI.

**Fix:** a new repository method, `findByIdAndOrganizationIdAndPublishedTrueAndActiveTrueAndDeletedFalse(...)`. Same predicate as the catalogue, expressed once. If a product is not visible in the shop, it cannot enter a cart. `mergeGuestCartOnLogin()` re-validates every line too — a guest cart is client-influenced state that has been sitting in the database, and merging it blindly would launder exactly the rows this fix now blocks.

### 2.3 HIGH — No organisation binding on the customer session

Login looks the customer up scoped by org (`findByOrganizationIdAndPhone(orgId, phone)`). **Every request after login resolves them by primary key alone:**

```java
Long customerId = (Long) session.getAttribute(SESSION_CUSTOMER_ID);
return customerRepository.findById(customerId)...          // ← no org check
```

`ec_customers` is a shared, org-scoped table. The invariant *"the customer in my session belongs to the org this request is running as"* was asserted exactly once and then never re-checked — while `ContextProvider`'s org **can change underneath a live session** (`UserContextController` lets an ERP user switch org context; `StorefrontOrgContextFilter` seeds an org into whatever session it finds). Everything downstream — `/account/orders`, the address book, the wishlist, checkout — is keyed on `customer_id` alone and trusts that invariant.

**Fix:** bind `SF_CUSTOMER_ORG` at login and re-assert on **every** `currentCustomerOrNull()` call: session org == request org == the customer row's own `organization_id`. Any mismatch purges the session keys and fails closed. The same call now also re-checks `BLOCKED` — previously an admin blocking an account mid-session left that customer fully signed in until the session expired.

---

## 3. Two live bugs found while reading

### 3.1 Anonymous shoppers are bounced to the ERP login page

`SecurityConfig.PUBLIC_URLS` and `StorefrontOrgContextFilter.STOREFRONT_PATTERNS` were two hand-maintained copies of the same list — one of them carrying the comment *"Kept in sync with SecurityConfig.PUBLIC_URLS"*. **They were not in sync.** The filter had `/about`, `/contact`, `/faq`, `/page/**`, `/newsletter/**`. `SecurityConfig` did not.

All five are real, mapped, footer-linked endpoints (`StorefrontSiteController`, `StorefrontContentController`). Missing from `PUBLIC_URLS`, they fell through to `.anyRequest().access(dynamicAuthorizationManager)`, which denies anonymous principals unconditionally. **Clicking "About Us" on a public shop sent the visitor to `/login?expired`.**

**Fix:** one list — `StorefrontPaths` — imported by both. It cannot drift again.

### 3.2 `/login?disabled` and `/login?locked` have never rendered

`UserDetailsServiceImpl` threw `DisabledException` / `LockedException` by hand from inside `loadUserByUsername()`. But `DaoAuthenticationProvider.retrieveUser()` wraps anything from there that isn't a `UsernameNotFoundException`:

```java
catch (Exception ex) {
    throw new InternalAuthenticationServiceException(ex.getMessage(), ex);   // ← here
}
```

So `LoginFailureHandler`'s `exception instanceof DisabledException` was tested against an `InternalAuthenticationServiceException` and was **always false**. Every failure fell through to `/login?error`. `LoginController` had the `@RequestParam` and the alert message for both cases, wired and ready — and neither could ever be triggered. **Two complete UX paths that had never once executed.** A disabled user was told "invalid password" and had no idea why their correct password stopped working.

**Fix:** stop throwing them by hand. `CustomUserDetails` already exposes all four flags, so Spring's own `DefaultPreAuthenticationChecks` throws them — from *after* `retrieveUser()` returns, where nothing wraps them. `LoginFailureHandler` also unwraps defensively, so it stays correct if anyone re-introduces a manual throw.

---

## 4. Where the throttle had to go, and why

Two obvious-looking spots were tried and rejected. Both **look** correct.

- ✗ **In `UserDetailsServiceImpl`** — same wrapping problem as §3.2. A `LockedException` thrown there arrives as an `InternalAuthenticationServiceException`.
- ✗ **In `LoginFailureHandler`** — runs *after* authentication was attempted. The BCrypt(12) comparison has already happened. That is the right place to **count** a failure; it is the wrong place to **refuse** one.

BCrypt(12) is ~250 ms per attempt, which is often mistaken for "that *is* the rate limit". It is not. It is a rate limit on the **attacker's** CPU, not on yours, and it parallelises trivially. Meanwhile each of those attempts burns one of your 20 Hikari connections and 250 ms of *your* CPU — so an unthrottled BCrypt endpoint is simultaneously a password oracle **and** a self-inflicted DoS amplifier.

So: `LoginThrottleFilter`, ahead of `UsernamePasswordAuthenticationFilter` — the only spot both early enough and outside Spring's exception-wrapping machinery.

`LoginAttemptService` keeps **two** counters per surface: per-identifier (targeted attacks) and per-IP (**password spraying** — one common password against hundreds of accounts, which the per-identifier counter cannot see because each account only ever records 1–2 failures). A success clears the identifier counter but **not** the IP counter: a sprayer *will* eventually land one valid credential, and clearing the IP counter on that hit would hand them a free reset of the spray detector.

> **★ Stated limitation, not hidden:** counters are in-memory and per-JVM. There is no Flyway in `pom.xml`, so adding `failed_attempts` / `locked_until` columns to `User` means touching the entity that has already broken Spring Session serialisation in this codebase once. If Spindle is ever scaled past one instance, swap the `ConcurrentHashMap` for Redis — the public API of `LoginAttemptService` does not change, only four private map operations do.

---

## 5. What was already right (and is untouched)

Worth stating, because several of these are the parts people break when "hardening":

- ✅ All **seven session/CSRF fixes** in `SecurityConfig` — `changeSessionId` over `migrateSession`, no `invalidSessionUrl`, `maximumSessions > 1`, `deleteCookies("SESSION")` not `"JSESSIONID"`, `SpringSessionBackedSessionRegistry`, `XorCsrfTokenRequestAttributeHandler`, SS7 constructor injection. Every one is correct, load-bearing, and retained with its reasoning restated so nobody re-introduces the bug it solved.
- ✅ **Cart prices are always resolved server-side** from the product/variant. The client never supplies a price and never could. This is the single most important thing a cart can get right.
- ✅ **Cart-item ownership** — `updateQuantity()` / `removeItem()` resolve the item id *inside the caller's own cart*. Already IDOR-safe.
- ✅ `StorefrontAddressService` — every read and write scoped by `customer_id`. Correct.
- ✅ Admin eCommerce controllers consistently use `SecurityHelper.requireOrgId()` — never a client-supplied org.
- ✅ `CustomAccessDeniedHandler`'s CSRF-aware routing (stale token → `/login?expired`, not `/access-denied`). Good call.
- ✅ `peekCart()` — anonymous visitors no longer spawn an `ec_carts` row per pageview.
- ✅ `CommonUtils.searchILike()` escapes single quotes.
- ✅ Storefront POSTs are `permitAll` but **not** CSRF-exempt. Correct — `permitAll ≠ CSRF-exempt`.

### One correction to the record

`application.properties` and `SecurityConfig` both state, as fact, that `spring.session.jdbc.initialize-schema=always` *"drops and recreates SPRING_SESSION on every startup"* and that this was *"the actual live cause of the need to submit the login twice"*.

**That is not what it does.** Spring Session's `schema-postgresql.sql` contains only `CREATE TABLE` / `CREATE INDEX` — there is no `DROP` anywhere in it — and Boot runs it with `continueOnError(true)` (`JdbcSessionDataSourceScriptDatabaseInitializer.getSettings()`). On an existing schema every statement fails with *"relation already exists"* and is swallowed. **No session is ever deleted by it.** The double-login bug was the two things that *were* correctly identified: the eager-CSRF-token race and the wrong cookie name on logout.

It is still worth changing — `always` runs DDL as the app's DB user on every boot, which a hardened deployment should not permit — but it is left as the **default** in the shipped file, because with no Flyway on the classpath **nothing else creates those tables** and flipping it would break a fresh deployment. It is now `${SESSION_SCHEMA_INIT:always}`; set it to `never` in production once the tables exist, then revoke DDL rights.

---

## 6. Files delivered

**New (8)**

| File | Purpose |
|---|---|
| `security/auth/WebSecurityUtils.java` | `clientIp` (XFF gate), `isAjax`, `sanitizeForLog` (CWE-117), `safeRedirect` (CWE-601) |
| `security/auth/LoginAttemptService.java` | Brute-force / spray throttle, shared by both logins |
| `security/auth/LoginThrottleFilter.java` | Refuses `POST /login` **before** the BCrypt compare |
| `security/config/SpindleSecurityProperties.java` | Every security knob, externalised |
| `ecommerce/storefront/StorefrontPaths.java` | Single source of truth for storefront URL patterns |
| `ecommerce/storefront/security/StorefrontAuthInterceptor.java` | Central login gate for `/account/**`, `/checkout/**`, `/wishlist/**` |
| `ecommerce/storefront/security/StorefrontAuthException.java` | Typed, safe-to-display auth errors |
| *(schema unchanged — no migration required)* | |

**Replaced (12)**

`SecurityConfig` · `WebMvcConfig` · `LoginController` · `LoginSuccessHandler` · `LoginFailureHandler` · `CustomAccessDeniedHandler` · `ContextProvider` · `UserDetailsServiceImpl` · `StorefrontAuthService` · `StorefrontAuthController` · `StorefrontCartService` · `StorefrontOrgContextFilter` · `EcProductCatalogRepository` · `application.properties`

### Compatibility

- **No schema change. No migration. No forced password resets.** BCrypt hashes are self-describing, so every existing customer and staff hash still verifies.
- **No frontend change required.** The storefront JSON envelope is byte-for-byte what the existing JS parses (`{success, message, customer, login}`). New fields (`redirectUrl`, `blocked`, `retryAfterSeconds`) are additive and ignored by old JS. `StorefrontAuthInterceptor` reproduces *exactly* the two response shapes the controllers already returned, so `if (r.login) location.href='/account/login'` keeps working untouched.
- **Every public method signature is unchanged.** `StorefrontAuthService.SESSION_CUSTOMER_ID` keeps its name and value. Exceptions still extend `IllegalArgumentException`, so every existing `catch (Exception e) → e.getMessage()` still compiles and behaves.

---

## 7. Deploy checklist

```bash
# 1. Rotate the remember-me key (it was in Git, so treat it as burned)
export APP_REMEMBER_ME_KEY="$(openssl rand -base64 48)"

# 2. Database password out of the properties file
export DB_PASSWORD="…"

# 3. Production
export REQUIRE_HTTPS=true
export THYMELEAF_CACHE=true
export APP_LOG_LEVEL=INFO

# 4. Only if a reverse proxy OVERWRITES X-Forwarded-For.
#    Setting this true without one makes rate limiting and the audit trail spoofable.
export TRUST_FORWARDED_HEADERS=true

# 5. Once SPRING_SESSION exists — then revoke DDL from the app's DB user
export SESSION_SCHEMA_INIT=never
```

Confirm `app.storefront.default-organization-id` matches the real row:

```sql
SELECT id, code, name FROM org_organizations ORDER BY id;
```

### Smoke tests

| Test | Expected |
|---|---|
| Note `SESSION` cookie → log in at `/account/login` → note it again | **Value must change.** If it does not, fixation protection is off — check the `SECURITY:` ERROR line in the log. |
| `/account/login?redirect=https://evil.com` → sign in | Lands on `/account/dashboard`, **not** evil.com. |
| `POST /cart/add` with another org's `productId` | `"Product not available."` |
| `POST /cart/add` with `{"quantity":"1E+40"}` | `"Please enter a valid quantity."` |
| 6 wrong passwords at `/login` | 6th → `/login?blocked=15` |
| 9 wrong passwords at `/account/login` | 9th → HTTP 429 + `Retry-After` |
| Disable a user in `sec_users`, then log in | `/login?disabled` — **for the first time ever** |
| Anonymous → `/about`, `/contact`, `/faq` | Renders. No redirect to `/login`. |
| Log in on the storefront → log in to the ERP in the same browser | ERP login purges `SF_CUSTOMER_ID`. One session, one identity. |

---

## 8. Deliberately not done

Flagged, not silently fixed — each is a bigger decision than a security patch should make on its own:

1. **`StorefrontProductService.browse()` interpolates `orgId` into SQL text.** Not injectable (it is a `Long` from the session), but it produces the literal `= null` and a Postgres syntax error whenever the org context is missing. Parameterise it.
2. **Password reset / OTP.** `ec_customer_otp` exists as an entity. There is no forgot-password flow at all — a customer who forgets their password is permanently locked out. `/account/forgot-password` and `/account/reset-password` are already carved out of `StorefrontPaths.CUSTOMER_OPEN`, ready for the controller.
3. **`StorefrontAccountController` passes the whole `EcCustomer` entity to the model**, `passwordHash` included. Templates don't render it, so this is latent rather than live — but it is one careless `th:text` from being real.
4. **`spring.jpa.hibernate.ddl-auto=update` in production.** The usual advice is `validate`. **Do not apply it here yet** — there is no Flyway in `pom.xml`, so Hibernate *is* the schema manager and `validate` would turn a drifted entity into a failed startup. Add Flyway first.
5. **The storefront session still gets created by the CSRF token repository** on any page rendering `<meta name="_csrf">`. `ContextProvider`'s new request-scoped fallback means org resolution no longer *forces* it — which is the precondition for moving CSRF to a cookie repository later and getting genuinely session-free anonymous browsing. That is a separate, carefully-tested change; the current CSRF setup was hard-won and is not something to touch casually.
6. **Cross-package import** (`security.config` → `ecommerce.storefront.StorefrontPaths`). A layering smell. The alternative — duplicating the path list — is the bug this exists to fix, so the smell wins. Move `StorefrontPaths` to a shared `common` package if it starts to bother you.
