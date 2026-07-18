package com.asg.spindleserp.ecommerce.storefront;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * StorefrontPaths — the ONE place every storefront URL pattern is declared.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHY THIS CLASS EXISTS: THREE LISTS THAT HAD ALREADY DRIFTED APART
 * ══════════════════════════════════════════════════════════════════════════
 * The same set of storefront URLs was hand-maintained in two separate files,
 * with a comment in one of them literally saying "Kept in sync with
 * SecurityConfig.PUBLIC_URLS" — and they were, in fact, already out of sync:
 *
 *   StorefrontOrgContextFilter.STOREFRONT_PATTERNS had:
 *       "/page/**", "/newsletter/**", "/about", "/contact", "/contact/**", "/faq"
 *
 *   SecurityConfig.PUBLIC_URLS did NOT.
 *
 * Consequence — a live, reproducible production bug:
 *   An anonymous visitor clicking "About Us", "Contact" or "FAQ" in the
 *   storefront footer (all three are real, mapped endpoints in
 *   StorefrontSiteController) fell through to
 *       .anyRequest().access(dynamicAuthorizationManager)
 *   which denies anonymous principals unconditionally → the visitor was
 *   bounced to /login?expired. On a PUBLIC SHOP. The same applies to
 *   StorefrontContentController's CMS pages (/page/**) and the newsletter
 *   signup (/newsletter/**).
 *
 * Two hand-maintained copies of a security-relevant list will always drift.
 * There is now exactly one copy, and both consumers import it.
 * ══════════════════════════════════════════════════════════════════════════
 */
public final class StorefrontPaths {

    private StorefrontPaths() {}

    private static final PathMatcher MATCHER = new AntPathMatcher();

    /**
     * ── PUBLIC ────────────────────────────────────────────────────────────
     * Every URL Spring Security must permitAll, because the entire customer
     * surface lives OUTSIDE Spring Security's Authentication model.
     *
     * Customer identity is StorefrontAuthService's SF_CUSTOMER_ID session
     * attribute — there is no sec_users row, no GrantedAuthority, nothing
     * DynamicAuthorizationManager can ever see. So if these were not
     * permitAll, an anonymous shopper would be denied before any controller
     * ran, and the shop would be unreachable.
     *
     * ★ permitAll here does NOT mean "anonymous can do anything".
     *   The login gate for /account/**, /checkout/** and /wishlist/** is
     *   StorefrontAuthInterceptor (plus the controllers' own checks, kept as
     *   defence in depth). Spring Security's job here is only to get out of
     *   the way of a surface it does not model — not to authorise it.
     */
    public static final String[] PUBLIC = {
            // Home + catalogue browsing
            "/",
            "/shop", "/shop/**",
            "/product/**",
            "/category/**",

            // Cart — guests may add to cart without an account
            "/cart", "/cart/**",

            // Checkout + account + wishlist — gated by StorefrontAuthInterceptor,
            // NOT by Spring Security (see note above).
            "/checkout", "/checkout/**",
            "/account", "/account/**",
            "/wishlist", "/wishlist/**",

            // ★ These four were MISSING from SecurityConfig.PUBLIC_URLS and are
            //   the live bug described in the class javadoc. All four are real
            //   mapped endpoints that anonymous visitors are supposed to reach.
            "/page/**",              // StorefrontContentController — CMS pages
            "/newsletter/**",        // StorefrontContentController — signup
            "/about", "/faq",        // StorefrontSiteController
            "/contact", "/contact/**",

            // Public travel portal — no login at all, lead capture only
            "/travel-site", "/travel-site/**",

            // Travel customer portal — gated by StorefrontAuthInterceptor
            "/travel-portal", "/travel-portal/**"
    };

    /**
     * ── CUSTOMER_ONLY ─────────────────────────────────────────────────────
     * Requires a logged-in EcCustomer. Enforced centrally by
     * StorefrontAuthInterceptor so that a newly-added controller method under
     * these prefixes CANNOT forget its guard.
     *
     * This is the fix for a structural weakness in the original design: the
     * login check was a hand-written
     *     if (authService.currentCustomerOrNull(request) == null) return "redirect:…";
     * repeated at the top of 12 different handler methods across 4 controllers.
     * Twelve chances to forget. Now it is one interceptor plus twelve
     * belt-and-braces checks that will simply never be reached.
     */
    public static final String[] CUSTOMER_ONLY = {
            "/account/**",
            "/checkout/**",
            "/wishlist/**"
    };

    /**
     * ── CUSTOMER_OPEN ─────────────────────────────────────────────────────
     * Carve-outs inside CUSTOMER_ONLY that must stay reachable while signed
     * out — otherwise the login page itself would require being logged in.
     */
    public static final String[] CUSTOMER_OPEN = {
            "/account",
            "/account/login",
            "/account/register",
            "/account/logout",
            "/account/forgot-password",
            "/account/reset-password"
    };

    /**
     * ── ORG_CONTEXT ───────────────────────────────────────────────────────
     * Paths where StorefrontOrgContextFilter must seed the anonymous default
     * organisation, because the request will call
     * ContextProvider.getOrganizationId() somewhere downstream.
     *
     * Identical to PUBLIC — deliberately. The two lists drifting apart is the
     * exact bug this class exists to prevent, so they are the same array
     * rather than two arrays that happen to agree today.
     */
    public static final String[] ORG_CONTEXT = PUBLIC;

    // ── Matching helpers ─────────────────────────────────────────────────────

    public static boolean isPublic(String uri)        { return matchesAny(PUBLIC, uri); }
    public static boolean isCustomerOnly(String uri)  { return matchesAny(CUSTOMER_ONLY, uri)
                                                            && !matchesAny(CUSTOMER_OPEN, uri); }
    public static boolean needsOrgContext(String uri) { return matchesAny(ORG_CONTEXT, uri); }

    public static boolean matchesAny(String[] patterns, String uri) {
        if (uri == null) return false;
        for (String pattern : patterns) {
            if (MATCHER.match(pattern, uri)) return true;
        }
        return false;
    }
}
