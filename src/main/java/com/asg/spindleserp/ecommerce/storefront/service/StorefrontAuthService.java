// Path: com/asg/spindleserp/ecommerce/storefront/service/StorefrontAuthService.java
package com.asg.spindleserp.ecommerce.storefront.service;

import com.asg.spindleserp.ecommerce.customerSupport.entity.EcCustomer;
import com.asg.spindleserp.ecommerce.customerSupport.repository.EcCustomerRepository;
import com.asg.spindleserp.ecommerce.storefront.dto.SfAuthDTO;
import com.asg.spindleserp.ecommerce.storefront.security.StorefrontAuthException;
import com.asg.spindleserp.security.auth.ContextProvider;
import com.asg.spindleserp.security.auth.LoginAttemptService;
import com.asg.spindleserp.security.auth.LoginAttemptService.Surface;
import com.asg.spindleserp.security.auth.WebSecurityUtils;
import com.asg.spindleserp.security.config.SpindleSecurityProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * StorefrontAuthService — session-based authentication for EcCustomer.
 *
 * Deliberately NOT wired into Spring Security's filter chain or sec_users:
 * EcCustomer is a portal-only self-registration, there is no ERP session
 * context at signup, and the customer has no roles, permissions or org scope
 * that DynamicAuthorizationManager could ever evaluate. That design is sound
 * and is preserved. What it is NOT allowed to mean is "and therefore none of
 * the protections Spring Security would have given us for free" — which is
 * what it meant before this rewrite.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHAT WAS WRONG — in severity order
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [CRITICAL] 1. SESSION FIXATION → full customer account takeover       ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   The entire login was:
 *
 *       private void loginSession(HttpServletRequest request, EcCustomer c) {
 *           HttpSession session = request.getSession(true);
 *           session.setAttribute(SESSION_CUSTOMER_ID, c.getId());
 *       }
 *
 *   The session ID is NOT rotated. Spring Security's session-fixation
 *   protection (.sessionFixation().changeSessionId() in SecurityConfig) fires
 *   on SPRING SECURITY authentication events only. This login path never
 *   touches Spring Security, so it never fires here.
 *
 *   Exploit, end to end:
 *     1. Attacker loads the shop, gets SESSION=abc123 (a valid, empty session).
 *     2. Attacker gets the victim's browser to adopt that same cookie value.
 *        Any of: a cached/reflected XSS anywhere on the domain OR ANY SUBDOMAIN
 *        (the session cookie has no __Host- prefix and no domain lock, so a
 *        single XSS on any *.asg.com host is enough), a MITM on plain HTTP
 *        (session.cookie.secure was not set), or a physical/shared-kiosk
 *        scenario.
 *     3. Victim logs in normally. SF_CUSTOMER_ID is written into session abc123.
 *        The cookie value does not change — because nothing rotates it.
 *     4. Attacker, still holding SESSION=abc123, is now the victim. Order
 *        history, saved addresses, wallet balance, checkout, everything.
 *
 *   FIX: request.changeSessionId() on every successful login and registration.
 *   Spring Session's SessionRepositoryRequestWrapper implements it against the
 *   JDBC store, so the SPRING_SESSION primary key rotates atomically while all
 *   attributes (including the guest cart key, which must survive so the cart
 *   merge still works) are preserved. This is exactly what Spring Security's
 *   own ChangeSessionIdAuthenticationStrategy does — this login path simply
 *   never got it.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [HIGH] 2. No organisation binding → cross-tenant identity confusion   ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   The session held ONLY the customer id:
 *
 *       Long customerId = (Long) session.getAttribute(SESSION_CUSTOMER_ID);
 *       return customerRepository.findById(customerId)...       // ← no org check
 *
 *   Login LOOKS the customer up scoped by org
 *   (findByOrganizationIdAndPhone(orgId, phone)), but every request AFTER
 *   login resolves them by primary key alone, with no org check at all.
 *
 *   ec_customers is a shared, org-scoped table (uq: organization_id +
 *   customer_code). So the invariant "the customer in my session belongs to the
 *   org this request is running as" was asserted exactly once, at login, and
 *   then never re-checked — while ContextProvider's org CAN change underneath
 *   a live session (UserContextController lets an ERP user switch org context,
 *   and StorefrontOrgContextFilter seeds an org into whatever session it finds).
 *   Everything downstream — /account/orders, the address book, the wishlist,
 *   checkout — is keyed on customer_id alone and trusts that invariant.
 *
 *   FIX: bind SF_CUSTOMER_ORG into the session at login, and re-assert on EVERY
 *   currentCustomerOrNull() call: session org == request org == customer's own
 *   organization_id. Any mismatch → session keys purged, treated as logged out.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [HIGH] 3. Unlimited password guessing                                 ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   No rate limit, no lockout, no CAPTCHA, no delay. A customer phone number
 *   is a 11-digit Bangladeshi mobile in a known format — the identifier space
 *   is public knowledge. Credential stuffing against it was completely
 *   unimpeded. FIX: LoginAttemptService, checked BEFORE the BCrypt compare.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] 4. Account enumeration by timing AND by message              ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   (a) TIMING. Unknown identifier → .orElseThrow() fires immediately, ~2ms.
 *       Known identifier → a BCrypt(12) comparison runs first, ~250ms. That is
 *       a 100× difference, measurable over the public internet with no
 *       statistics required. An attacker can enumerate which phone numbers
 *       have accounts at ~10/second.
 *       FIX: on a miss, run a BCrypt compare against a dummy hash of the same
 *       cost. Both paths now cost the same.
 *
 *   (b) MESSAGE. Status was checked BEFORE the password:
 *           if (customer.getAccountStatus() == BLOCKED)
 *               throw new IllegalArgumentException("This account has been blocked…");
 *       "This account has been blocked" for a wrong password confirms the
 *       account exists. Same for "This account is no longer active."
 *       FIX: verify the password FIRST. Status is only revealed to someone who
 *       has already proved they hold the credential.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] 5. Logout did not end the session                            ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *       public void logout(HttpServletRequest request) {
 *           HttpSession session = request.getSession(false);
 *           if (session != null) session.removeAttribute(SESSION_CUSTOMER_ID);
 *       }
 *   One attribute removed. The session, its ID, the cart, the half-finished
 *   checkout address (SF_CHECKOUT_INFO — which contains the customer's full
 *   name, phone and home address) all survive. On any shared machine — an
 *   internet café, a shop-floor terminal — the next person gets the previous
 *   customer's delivery address pre-filled at checkout.
 *   FIX: purge every SF_* key and rotate/invalidate the session.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] 6. Weak password policy + a second, unmanaged encoder        ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   "Password must be at least 6 characters." — nothing else. "123456"
 *   accepted. And:
 *       private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
 *   A second encoder instance, constructed by hand, outside the Spring
 *   container, with the cost factor duplicated as a magic number. The day
 *   someone raises the cost on the PasswordEncoder @Bean in SecurityConfig,
 *   customer passwords silently keep hashing at the old cost forever.
 *   FIX: inject the container's PasswordEncoder bean. Policy is configurable
 *   (app.security.storefront.*). BCrypt hashes are self-describing, so every
 *   existing customer hash still verifies — no migration, no forced resets.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [LOW] 7. Spoofable audit trail                                        ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   recordLogin() trusted X-Forwarded-For unconditionally. Any client can send
 *   that header with any value, so ec_customer_login_history was recording
 *   attacker-chosen IPs. See WebSecurityUtils.clientIp().
 *
 * ══════════════════════════════════════════════════════════════════════════
 * PRESERVED — everything the original got right
 * ══════════════════════════════════════════════════════════════════════════
 *   ✅ SESSION_CUSTOMER_ID constant name and value — unchanged, so any other
 *      caller referencing StorefrontAuthService.SESSION_CUSTOMER_ID still works.
 *   ✅ Method signatures of register / login / logout / currentCustomerOrNull /
 *      isLoggedIn / updateProfile — unchanged. Drop-in.
 *   ✅ Exceptions still extend IllegalArgumentException, so every existing
 *      `catch (Exception e) → e.getMessage()` controller keeps working.
 *   ✅ Bangladeshi phone normalisation (+880/880/0 → 0…) and validation.
 *   ✅ @Transactional boundaries, last_login_at stamping, login-history rows.
 *   ✅ Guest-cart session key (SF_CART_SESSION_ID) survives login so
 *      StorefrontCartService.mergeGuestCartOnLogin() still finds it.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorefrontAuthService {

    // ── Session keys ─────────────────────────────────────────────────────────
    /** Unchanged from v2 — external callers may reference this. */
    public static final String SESSION_CUSTOMER_ID  = "SF_CUSTOMER_ID";
    /** NEW — the org the customer was authenticated against. Re-checked every request. */
    public static final String SESSION_CUSTOMER_ORG = "SF_CUSTOMER_ORG";
    /** NEW — epoch millis of last activity, for the optional idle timeout. */
    public static final String SESSION_LAST_SEEN    = "SF_LAST_SEEN";

    /** Per-request memo of currentCustomerOrNull() — see that method. */
    private static final String REQUEST_ATTR_CUSTOMER = "SF_CURRENT_CUSTOMER";
    /** Sentinel so a negative result is cached too, not re-queried. */
    private static final Object NO_CUSTOMER = "SF_NO_CUSTOMER";

    /** Every storefront-owned session attribute — purged as a set on logout. */
    private static final String[] STOREFRONT_SESSION_KEYS = {
            SESSION_CUSTOMER_ID,
            SESSION_CUSTOMER_ORG,
            SESSION_LAST_SEEN,
            "SF_CART_SESSION_ID",   // StorefrontCartService
            "SF_CHECKOUT_INFO"      // StorefrontCheckoutController — holds a home address
    };

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?880|0)1[3-9]\\d{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern HAS_LETTER    = Pattern.compile(".*[a-zA-Z].*");
    private static final Pattern HAS_DIGIT     = Pattern.compile(".*\\d.*");

    /** One generic string for every credential failure — no enumeration surface. */
    private static final String GENERIC_LOGIN_FAILURE = "Invalid phone/email or password.";

    private final EcCustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;          // ✅ the container's bean
    private final LoginAttemptService loginAttemptService;
    private final SpindleSecurityProperties securityProps;

    /**
     * A real BCrypt hash of a random string, computed once at startup.
     *
     * Used ONLY to burn the same CPU time on an unknown-identifier login as on a
     * known one, so the two are indistinguishable by wall-clock. It is generated
     * rather than hard-coded because a malformed constant would make
     * passwordEncoder.matches() bail out in microseconds and quietly defeat the
     * whole point — the exact failure mode this is meant to prevent.
     */
    private String timingEqualiserHash;

    @PostConstruct
    void initTimingEqualiser() {
        byte[] noise = new byte[32];
        new SecureRandom().nextBytes(noise);
        this.timingEqualiserHash =
                passwordEncoder.encode(Base64.getEncoder().encodeToString(noise));
    }

    // ══════════════════════════════════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public SfAuthDTO register(SfAuthDTO dto, HttpServletRequest request) {
        String ip = clientIp(request);

        // ✅ Bot-signup flood control (was: none — an unauthenticated endpoint
        //    that INSERTs a row and runs a BCrypt(12) hash, with no limit).
        if (loginAttemptService.registrationBlocked(ip)) {
            throw new StorefrontAuthException.TooManyAttempts(60L * 60L);
        }
        loginAttemptService.registrationAttempted(ip);

        Long orgId = requireOrgId();
        String phone = normalizePhone(dto.getPhone());

        if (!PHONE_PATTERN.matcher(phone).matches())
            throw new StorefrontAuthException("Please enter a valid Bangladeshi mobile number.");
        if (customerRepository.existsByOrganizationIdAndPhone(orgId, phone))
            throw new StorefrontAuthException("An account with this phone number already exists.");
        if (dto.getEmail() != null && !dto.getEmail().isBlank()
                && !EMAIL_PATTERN.matcher(dto.getEmail().trim()).matches())
            throw new StorefrontAuthException("Please enter a valid email address.");
        if (dto.getFirstName() == null || dto.getFirstName().isBlank())
            throw new StorefrontAuthException("Please enter your name.");

        validatePasswordPolicy(dto.getPassword());

        String email = normalizeEmail(dto.getEmail());
        if (email != null && customerRepository.findByOrganizationIdAndEmail(orgId, email).isPresent())
            throw new StorefrontAuthException("An account with this email address already exists.");

        String firstName = dto.getFirstName().trim();
        String lastName  = (dto.getLastName() != null && !dto.getLastName().isBlank())
                ? dto.getLastName().trim() : null;

        EcCustomer customer = EcCustomer.builder()
                .organizationId(orgId)
                .customerCode(nextCustomerCode(orgId))
                .firstName(firstName)
                .lastName(lastName)
                .fullName(buildFullName(firstName, lastName))
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .emailVerified(false)
                .phoneVerified(false)
                .accountStatus(EcCustomer.AccountStatus.ACTIVE)
                .active(true)
                .deleted(false)
                .build();

        customer = customerRepository.save(customer);

        establishSession(request, customer);                 // ✅ rotates the session ID
        recordLogin(customer.getId(), request, "SUCCESS");

        log.info("Storefront registration: customer #{} org={} ip={}",
                customer.getId(), orgId, WebSecurityUtils.sanitizeForLog(ip));
        return toAuthDTO(customer);
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════════════════════

    @Transactional
    public SfAuthDTO login(String identifier, String password, HttpServletRequest request) {

        if (identifier == null || identifier.isBlank() || password == null || password.isEmpty())
            throw new StorefrontAuthException(GENERIC_LOGIN_FAILURE);

        String ip = clientIp(request);
        String normalized = normalizeIdentifier(identifier);

        // ── 1. Throttle BEFORE any hashing. ──────────────────────────────────
        //    The BCrypt compare is the expensive part (~250ms of OUR CPU and one
        //    of the 20 Hikari connections). Checking the limit first means a
        //    locked-out attacker costs us a hash-map lookup, not a hash.
        long blockedFor = loginAttemptService.blockedSeconds(Surface.STOREFRONT, normalized, ip);
        if (blockedFor > 0) throw new StorefrontAuthException.TooManyAttempts(blockedFor);

        Long orgId = requireOrgId();

        Optional<EcCustomer> found = normalized.contains("@")
                ? customerRepository.findByOrganizationIdAndEmail(orgId, normalized)
                : customerRepository.findByOrganizationIdAndPhone(orgId, normalized);

        // ── 2. Unknown identifier → burn the SAME time as a real BCrypt check. ─
        if (found.isEmpty()) {
            passwordEncoder.matches(password, timingEqualiserHash);   // ✅ constant-time-ish
            loginAttemptService.loginFailed(Surface.STOREFRONT, normalized, ip);
            log.warn("SF LOGIN FAIL  identifier='{}' reason=NO_SUCH_CUSTOMER org={} ip={}",
                    WebSecurityUtils.sanitizeForLog(normalized), orgId, WebSecurityUtils.sanitizeForLog(ip));
            throw new StorefrontAuthException(GENERIC_LOGIN_FAILURE);
        }

        EcCustomer customer = found.get();

        // ── 3. PASSWORD FIRST — status checks come after. ────────────────────
        //    The old order (status → password) told an unauthenticated stranger
        //    "this account is blocked", which is a yes/no oracle on account
        //    existence. Nobody learns anything about an account until they have
        //    proved they hold its password.
        if (customer.getPasswordHash() == null
                || !passwordEncoder.matches(password, customer.getPasswordHash())) {
            loginAttemptService.loginFailed(Surface.STOREFRONT, normalized, ip);
            recordLogin(customer.getId(), request, "FAILED");
            log.warn("SF LOGIN FAIL  customer=#{} reason=BAD_PASSWORD ip={}",
                    customer.getId(), WebSecurityUtils.sanitizeForLog(ip));
            throw new StorefrontAuthException(GENERIC_LOGIN_FAILURE);
        }

        // ── 4. Credential is correct — NOW it is safe to explain the status. ──
        assertUsable(customer);

        // ── 5. Success. ──────────────────────────────────────────────────────
        loginAttemptService.loginSucceeded(Surface.STOREFRONT, normalized);

        customer.setLastLoginAt(LocalDateTime.now());
        customer = customerRepository.save(customer);

        establishSession(request, customer);                 // ✅ rotates the session ID
        recordLogin(customer.getId(), request, "SUCCESS");

        log.info("SF LOGIN OK  customer=#{} org={} ip={}",
                customer.getId(), customer.getOrganizationId(), WebSecurityUtils.sanitizeForLog(ip));
        return toAuthDTO(customer);
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Purges every storefront session key and rotates or invalidates the session.
     *
     * Two paths, deliberately:
     *   • The session ALSO carries a Spring Security context (an ERP staff member
     *     who happens to be browsing the storefront) → do NOT invalidate, or we
     *     would silently log them out of the ERP as a side-effect of clicking
     *     "sign out" on the shop. Purge the SF_* keys and rotate the ID instead.
     *   • Otherwise (the normal case: a plain customer) → invalidate outright.
     *     Nothing in the session is worth keeping, and a full invalidate is the
     *     strongest guarantee that the old cookie value is dead.
     */
    public void logout(HttpServletRequest request) {
        request.setAttribute(REQUEST_ATTR_CUSTOMER, NO_CUSTOMER);   // drop the memo

        HttpSession session = request.getSession(false);
        if (session == null) return;

        Long customerId = readLong(session, SESSION_CUSTOMER_ID);

        boolean hasErpAuthentication =
                session.getAttribute("SPRING_SECURITY_CONTEXT") != null;

        for (String key : STOREFRONT_SESSION_KEYS) session.removeAttribute(key);

        if (hasErpAuthentication) {
            rotateSessionId(request);
        } else {
            try {
                session.invalidate();
            } catch (IllegalStateException alreadyInvalid) {
                // Concurrent logout in another tab. Nothing to do.
            }
        }

        if (customerId != null) log.info("SF LOGOUT  customer=#{}", customerId);
    }

    // ══════════════════════════════════════════════════════════════════════
    // CURRENT CUSTOMER
    // ══════════════════════════════════════════════════════════════════════

    /**
     * The single authority on "who is signed in on the storefront right now".
     *
     * Re-asserts, on EVERY call, all four things that were only ever checked at
     * login (or not at all):
     *   1. a customer id is present in the session
     *   2. the org the session was authenticated against still matches the org
     *      THIS request is running as                       ← was never checked
     *   3. the customer row still belongs to that org        ← was never checked
     *   4. the account is still active / not deleted / not blocked
     *      (blocked was checked at login only, so an account blocked by an admin
     *       mid-session stayed fully signed in until the session expired)
     *
     * Any failure → the session keys are purged and the caller sees a clean
     * "logged out", rather than a half-valid identity.
     *
     * ── Memoised per request ─────────────────────────────────────────────
     * StorefrontAuthInterceptor calls this, and then the controller it guards
     * calls it again (the controllers keep their own checks as defence in
     * depth). Without memoisation that is two identical SELECTs on every single
     * page of the customer portal. The result — including a negative result — is
     * cached in a request attribute, so the extra safety layer costs nothing.
     */
    @Transactional(readOnly = true)
    public EcCustomer currentCustomerOrNull(HttpServletRequest request) {

        Object cached = request.getAttribute(REQUEST_ATTR_CUSTOMER);
        if (cached instanceof EcCustomer c) return c;
        if (NO_CUSTOMER.equals(cached))     return null;

        EcCustomer resolved = resolveCurrentCustomer(request);
        request.setAttribute(REQUEST_ATTR_CUSTOMER, resolved != null ? resolved : NO_CUSTOMER);
        return resolved;
    }

    private EcCustomer resolveCurrentCustomer(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;

        Long customerId  = readLong(session, SESSION_CUSTOMER_ID);
        if (customerId == null) return null;

        Long sessionOrg  = readLong(session, SESSION_CUSTOMER_ORG);
        Long requestOrg  = ContextProvider.getOrganizationId();

        // ── 2. session org vs request org ────────────────────────────────────
        //    sessionOrg == null means the session predates this fix (a customer
        //    who was already signed in when the app was upgraded). Treat that as
        //    "must re-authenticate" rather than "trust it" — fail closed.
        if (sessionOrg == null || requestOrg == null || !sessionOrg.equals(requestOrg)) {
            log.warn("SF SESSION REJECTED  customer=#{} sessionOrg={} requestOrg={} — purging.",
                    customerId, sessionOrg, requestOrg);
            purgeCustomerKeys(session);
            return null;
        }

        // ── Optional idle timeout ────────────────────────────────────────────
        int idleMinutes = securityProps.getStorefront().getIdleTimeoutMinutes();
        if (idleMinutes > 0) {
            Long lastSeen = readLong(session, SESSION_LAST_SEEN);
            long now = System.currentTimeMillis();
            if (lastSeen != null
                    && now - lastSeen > Duration.ofMinutes(idleMinutes).toMillis()) {
                log.info("SF SESSION IDLE-EXPIRED  customer=#{} after {} min", customerId, idleMinutes);
                purgeCustomerKeys(session);
                return null;
            }
            session.setAttribute(SESSION_LAST_SEEN, now);
        }

        EcCustomer customer = customerRepository.findById(customerId).orElse(null);

        // ── 3. + 4. the row itself must still be valid, and still ours ───────
        if (customer == null
                || customer.getOrganizationId() == null
                || !customer.getOrganizationId().equals(requestOrg)
                || customer.isDeleted()
                || !customer.isActive()
                || customer.getAccountStatus() == EcCustomer.AccountStatus.BLOCKED
                || customer.getAccountStatus() == EcCustomer.AccountStatus.DELETED) {
            purgeCustomerKeys(session);
            return null;
        }

        return customer;
    }

    public boolean isLoggedIn(HttpServletRequest request) {
        return currentCustomerOrNull(request) != null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // PROFILE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Unchanged signature. The org guard is new: the caller passes a customerId,
     * and nothing previously stopped that id from belonging to a different org.
     * (Today every caller sources it from currentCustomerOrNull(), so it was
     * safe in practice — this closes it by construction rather than by
     * convention, so a future caller cannot get it wrong.)
     */
    @Transactional
    public EcCustomer updateProfile(Long customerId, String firstName, String lastName, String email) {
        EcCustomer customer = loadOwnCustomer(customerId);

        if (firstName == null || firstName.isBlank())
            throw new StorefrontAuthException("Please enter your first name.");

        String cleanEmail = normalizeEmail(email);
        if (email != null && !email.isBlank() && cleanEmail == null)
            throw new StorefrontAuthException("Please enter a valid email address.");

        // Email must stay unique within the org — previously unchecked, so two
        // customers could end up sharing an email and login-by-email became
        // non-deterministic (findByOrganizationIdAndEmail returns one of them).
        if (cleanEmail != null) {
            Optional<EcCustomer> clash =
                    customerRepository.findByOrganizationIdAndEmail(customer.getOrganizationId(), cleanEmail);
            if (clash.isPresent() && !clash.get().getId().equals(customer.getId()))
                throw new StorefrontAuthException("That email address is already in use.");
        }

        String fn = firstName.trim();
        String ln = (lastName != null && !lastName.isBlank()) ? lastName.trim() : null;

        customer.setFirstName(fn);
        customer.setLastName(ln);
        customer.setFullName(buildFullName(fn, ln));
        customer.setEmail(cleanEmail);
        return customerRepository.save(customer);
    }

    /**
     * NEW — customers had no way to change their own password.
     * Requires the current password (so a stolen session cannot lock the real
     * owner out) and rotates the session ID on success (so any OTHER session
     * holding the old cookie is not silently upgraded alongside).
     */
    @Transactional
    public void changePassword(Long customerId, String currentPassword, String newPassword,
                               HttpServletRequest request) {
        EcCustomer customer = loadOwnCustomer(customerId);

        if (customer.getPasswordHash() == null
                || currentPassword == null
                || !passwordEncoder.matches(currentPassword, customer.getPasswordHash()))
            throw new StorefrontAuthException("Your current password is incorrect.");

        validatePasswordPolicy(newPassword);

        if (passwordEncoder.matches(newPassword, customer.getPasswordHash()))
            throw new StorefrontAuthException("Your new password must be different from the current one.");

        customer.setPasswordHash(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);

        rotateSessionId(request);
        log.info("SF PASSWORD CHANGED  customer=#{}", customerId);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SESSION PLUMBING
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ★ THE SESSION-FIXATION FIX.
     *
     * request.changeSessionId() is the Servlet 3.1 API. Spring Session's
     * SessionRepositoryRequestWrapper overrides it and delegates to the
     * JDBC-backed repository, so the SPRING_SESSION primary key is rotated
     * atomically. Crucially it KEEPS the same session object and all of its
     * attributes — which is required here, because SF_CART_SESSION_ID must
     * survive for StorefrontCartService.mergeGuestCartOnLogin() to find the
     * guest cart a moment later.
     *
     * This is the identical mechanism SecurityConfig already relies on for the
     * ERP login (.sessionFixation().changeSessionId()); the customer login path
     * simply never invoked it, because it never goes through Spring Security.
     */
    private void establishSession(HttpServletRequest request, EcCustomer customer) {
        request.getSession(true);       // guarantee one exists before rotating
        rotateSessionId(request);

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_CUSTOMER_ID,  customer.getId());
        session.setAttribute(SESSION_CUSTOMER_ORG, customer.getOrganizationId());
        session.setAttribute(SESSION_LAST_SEEN,    System.currentTimeMillis());

        // The identity of this request just changed — drop any memoised "guest".
        request.setAttribute(REQUEST_ATTR_CUSTOMER, customer);
    }

    private void rotateSessionId(HttpServletRequest request) {
        try {
            if (request.getSession(false) != null) request.changeSessionId();
        } catch (IllegalStateException | UnsupportedOperationException e) {
            // Some containers/wrappers may not support it. Never fail a login
            // over it — but say so loudly, because it means fixation protection
            // is NOT active and that needs to be visible, not silent.
            log.error("SECURITY: could not rotate the session ID on customer login — " +
                      "session-fixation protection is NOT active on the storefront. " +
                      "Cause: {}", e.toString());
        }
    }

    private void purgeCustomerKeys(HttpSession session) {
        session.removeAttribute(SESSION_CUSTOMER_ID);
        session.removeAttribute(SESSION_CUSTOMER_ORG);
        session.removeAttribute(SESSION_LAST_SEEN);
    }

    private static Long readLong(HttpSession session, String key) {
        Object value = session.getAttribute(key);
        if (value instanceof Long l)   return l;
        if (value instanceof Number n) return n.longValue();
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDATION HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private Long requireOrgId() {
        Long orgId = ContextProvider.getOrganizationId();
        if (orgId == null) {
            // Fail closed and loudly. Without this, register() would INSERT with
            // organization_id = NULL (the column is NOT NULL, so it would blow up
            // with a raw constraint violation) and login() would query
            // "WHERE organization_id IS NULL" and silently never match anyone.
            log.error("Storefront request has no organization context. Set " +
                      "app.storefront.default-organization-id and confirm " +
                      "StorefrontOrgContextFilter is running for this path.");
            throw new StorefrontAuthException("The store is not available right now. Please try again later.");
        }
        return orgId;
    }

    /** Only ever called once the caller has proved they hold the password. */
    private void assertUsable(EcCustomer customer) {
        if (customer.isDeleted()
                || customer.getAccountStatus() == EcCustomer.AccountStatus.DELETED)
            throw new StorefrontAuthException("This account is no longer active.");
        if (!customer.isActive())
            throw new StorefrontAuthException("This account is no longer active.");
        if (customer.getAccountStatus() == EcCustomer.AccountStatus.BLOCKED)
            throw new StorefrontAuthException("This account has been blocked. Please contact support.");
    }

    private void validatePasswordPolicy(String password) {
        var sf = securityProps.getStorefront();
        int min = Math.max(6, sf.getMinPasswordLength());

        if (password == null || password.length() < min)
            throw new StorefrontAuthException("Password must be at least " + min + " characters.");
        if (password.length() > 128)
            throw new StorefrontAuthException("Password must be 128 characters or fewer.");
        if (sf.isRequireLetterAndDigit()
                && (!HAS_LETTER.matcher(password).matches() || !HAS_DIGIT.matcher(password).matches()))
            throw new StorefrontAuthException("Password must contain at least one letter and one number.");
    }

    /**
     * Customer code was: "CUST-" + System.currentTimeMillis() % 1000000.
     *
     * That is a 6-digit value derived from the clock, and the table has a UNIQUE
     * constraint on (organization_id, customer_code). Two signups inside the same
     * millisecond-mod-1000000 window collide and the second one dies on a raw
     * Postgres constraint violation — whose message the old controller then
     * echoed verbatim to the browser. It also wraps every ~16 minutes, so
     * collisions are not even rare at any real signup rate.
     *
     * Now: derived from the actual max code in the table, retried on collision.
     */
    private String nextCustomerCode(Long orgId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Long next = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(MAX(NULLIF(regexp_replace(customer_code, '\\D', '', 'g'), '')::bigint), 0) + 1
                    FROM ec_customers
                    WHERE organization_id = ?
                    """, Long.class, orgId);
            String code = String.format("CUST-%06d", next == null ? 1L : next);
            if (!customerRepository.existsByOrganizationIdAndCustomerCode(orgId, code)) return code;
        }
        // Vanishingly unlikely; still better than a raw constraint violation.
        return "CUST-" + System.currentTimeMillis();
    }

    private String normalizeIdentifier(String raw) {
        String trimmed = raw.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase() : normalizePhone(trimmed);
    }

    private String normalizePhone(String raw) {
        if (raw == null) return "";
        String p = raw.trim().replaceAll("[\\s-]", "");
        if (p.startsWith("+880")) return "0" + p.substring(4);
        if (p.startsWith("880"))  return "0" + p.substring(3);
        return p;
    }

    /** @return a clean lower-cased email, or null if absent/blank/invalid. */
    private String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String e = raw.trim().toLowerCase();
        return EMAIL_PATTERN.matcher(e).matches() ? e : null;
    }

    private static String buildFullName(String firstName, String lastName) {
        return (firstName + " " + (lastName != null ? lastName : "")).trim();
    }

    private EcCustomer loadOwnCustomer(Long customerId) {
        Long orgId = requireOrgId();
        return customerRepository.findById(customerId)
                .filter(c -> !c.isDeleted() && c.isActive())
                .filter(c -> orgId.equals(c.getOrganizationId()))     // ✅ org guard
                .orElseThrow(() -> new StorefrontAuthException("Account not found."));
    }

    private String clientIp(HttpServletRequest request) {
        return WebSecurityUtils.clientIp(request, securityProps.isTrustForwardedHeaders());
    }

    // ══════════════════════════════════════════════════════════════════════
    // AUDIT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Best-effort audit row in ec_customer_login_history — never fails the login.
     *
     * The IP now comes from WebSecurityUtils.clientIp(), which only honours
     * X-Forwarded-For when app.security.trust-forwarded-headers=true. Previously
     * the header was trusted unconditionally, which meant every row in this
     * table recorded whatever IP the client felt like claiming — i.e. the audit
     * trail was decorative.
     */
    private void recordLogin(Long customerId, HttpServletRequest request, String status) {
        if (customerId == null) return;   // FK column is NOT NULL — nothing to write
        try {
            String ua = request.getHeader("User-Agent");
            if (ua != null && ua.length() > 150) ua = ua.substring(0, 150);

            String ip = clientIp(request);
            if (ip.length() > 50) ip = ip.substring(0, 50);

            jdbcTemplate.update("""
                    INSERT INTO ec_customer_login_history
                        (customer_id, login_time, ip_address, browser, login_source, login_status)
                    VALUES (?, now(), ?, ?, 'WEB', ?)
                    """, customerId, ip, ua, status);
        } catch (Exception e) {
            log.warn("recordLogin failed for customer #{}: {}", customerId, e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // MAPPING
    // ══════════════════════════════════════════════════════════════════════

    /** Never carries passwordHash. SfAuthDTO.password is write-only by contract. */
    private SfAuthDTO toAuthDTO(EcCustomer c) {
        return SfAuthDTO.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .build();
    }
}
