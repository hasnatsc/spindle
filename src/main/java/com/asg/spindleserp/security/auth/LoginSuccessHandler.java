package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.auth.LoginAttemptService.Surface;
import com.asg.spindleserp.security.config.SpindleSecurityProperties;
import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.UserRepository;
import com.asg.spindleserp.security.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * LoginSuccessHandler — everything that must happen the instant an ERP user
 * authenticates.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * RETAINED (all correct, all load-bearing)
 * ══════════════════════════════════════════════════════════════════════════
 *   1. Touch the session so the SPRING_SESSION row is flushed BEFORE the
 *      redirect response leaves the server. changeSessionId() has already run
 *      in the filter chain by the time this handler executes; without an
 *      explicit write, a very fast browser can come back with the new cookie
 *      before the JDBC store has committed the row.
 *   2. Load the User WITH all allowed scope collections in ONE query, then
 *      userContextService.loadContext() → from here on every request reads
 *      ContextProvider, zero DB.
 *   3. Stamp last_login_at.
 *   4. Anti-cache headers.
 *   5. Redirect to the user's configured defaultDashboard.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * NEW — 1. Reset the brute-force counter
 * ══════════════════════════════════════════════════════════════════════════
 * A correct password clears the failure count for that identifier. (The per-IP
 * counter is deliberately NOT cleared — see LoginAttemptService: an attacker
 * spraying one common password across many accounts WILL eventually hit one,
 * and clearing the IP counter on that hit would hand them a free reset of the
 * spray detector.)
 *
 * ══════════════════════════════════════════════════════════════════════════
 * NEW — 2. ★ Purge any storefront customer identity from this session
 * ══════════════════════════════════════════════════════════════════════════
 * The ERP and the storefront SHARE one HttpSession and one SESSION cookie —
 * the storefront is permitAll inside the same Spring Security chain, not a
 * separate app. So a browser can genuinely arrive at /login already carrying
 * SF_CUSTOMER_ID from a storefront login (staff testing the shop; a shared
 * shop-floor terminal; anyone who browsed the store before logging in).
 *
 * Left alone, that session now holds TWO identities at once: an ERP
 * Authentication AND a customer identity. Every storefront controller resolves
 * the customer via currentCustomerOrNull() — which reads that attribute and
 * neither knows nor cares that an ERP user is now driving. An accounts clerk
 * who logs into the ERP on a terminal where a customer forgot to sign out is
 * silently acting AS that customer on /account/orders and /checkout.
 *
 * A fresh ERP authentication means "this session is now this staff member".
 * Any prior customer identity is stale by definition and is dropped here.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * NEW — 3. Honour the saved request (deep links)
 * ══════════════════════════════════════════════════════════════════════════
 * The old handler ALWAYS redirected to the user's default dashboard, discarding
 * Spring Security's SavedRequest entirely. So a user who clicked a deep link in
 * an email — /purchase/orders/1042, say — was bounced to /login, authenticated,
 * and then dumped on /dashboard, with no idea where the thing they clicked
 * went. They had to find it by hand. Every time.
 *
 * SavedRequest is now honoured when present, with the default dashboard as the
 * fallback. The target still comes from Spring's own request cache (a
 * server-side session attribute), never from a user-supplied parameter, so this
 * introduces no open-redirect surface.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /** Storefront-owned session keys. A new ERP login invalidates all of them. */
    private static final String[] STOREFRONT_SESSION_KEYS = {
            "SF_CUSTOMER_ID",
            "SF_CUSTOMER_ORG",
            "SF_LAST_SEEN",
            "SF_CART_SESSION_ID",
            "SF_CHECKOUT_INFO"
    };

    private final UserRepository            userRepository;
    private final UserContextService        userContextService;
    private final LoginAttemptService       loginAttemptService;
    private final SpindleSecurityProperties props;

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        Authentication      authentication) throws IOException {

        String username = authentication.getName();

        // ── Clear the failure counter for this identifier ─────────────────────
        loginAttemptService.loginSucceeded(Surface.ERP, username);

        // ── Ensure the (already-rotated) session row is persisted ─────────────
        HttpSession session = request.getSession(true);
        session.setAttribute("_loginTs", System.currentTimeMillis());

        // ── ★ Drop any storefront customer identity riding on this session ────
        purgeStorefrontIdentity(session, username);

        // ── Load user + all allowed scope collections (ONE query) ─────────────
        User user = userRepository
                .findByUsernameWithAllContext(username)
                .orElseThrow(() -> new IllegalStateException(
                        "User not found after login: " + username));

        // ── Build and store session context — zero DB from here on ────────────
        userContextService.loadContext(user);

        // ── Stamp last login ──────────────────────────────────────────────────
        userRepository.updateLastLogin(username, LocalDateTime.now());

        // ── Anti-cache headers ────────────────────────────────────────────────
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma",        "no-cache");
        response.setHeader("Expires",       "0");

        log.info("LOGIN OK  user='{}' org='{}' ip='{}' sessionId='{}'",
                WebSecurityUtils.sanitizeForLog(username),
                user.getOrganization() != null ? user.getOrganization().getName() : "—",
                WebSecurityUtils.sanitizeForLog(
                        WebSecurityUtils.clientIp(request, props.isTrustForwardedHeaders())),
                session.getId());

        getRedirectStrategy().sendRedirect(request, response, resolveTarget(request, response, user));
    }

    // ── Storefront identity purge ────────────────────────────────────────────

    private void purgeStorefrontIdentity(HttpSession session, String username) {
        if (session.getAttribute("SF_CUSTOMER_ID") == null) return;

        log.info("LOGIN  session for '{}' was carrying a storefront customer identity " +
                 "(SF_CUSTOMER_ID). Purging it — one session holds one identity.",
                 WebSecurityUtils.sanitizeForLog(username));

        for (String key : STOREFRONT_SESSION_KEYS) session.removeAttribute(key);
    }

    // ── Redirect target ──────────────────────────────────────────────────────

    /**
     * SavedRequest (a deep link the user was bounced off) wins; the configured
     * dashboard is the fallback.
     *
     * The URL comes from Spring's server-side request cache, NOT from any
     * request parameter — so there is no open-redirect surface here, unlike the
     * ?redirect= parameter on the storefront login (which is separately fixed in
     * WebSecurityUtils.safeRedirect).
     */
    private String resolveTarget(HttpServletRequest request,
                                 HttpServletResponse response,
                                 User user) {

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            String url = saved.getRedirectUrl();
            requestCache.removeRequest(request, response);
            if (url != null && !url.isBlank() && !isAssetOrAuthUrl(url)) return url;
        }
        return resolveDashboard(user);
    }

    /**
     * Never redirect a freshly-authenticated user to /login (a loop) or to an
     * asset the browser happened to request while the session was expiring.
     */
    private static boolean isAssetOrAuthUrl(String url) {
        String lower = url.toLowerCase();
        return lower.contains("/login")
                || lower.contains("/logout")
                || lower.contains("/css/")   || lower.contains("/js/")
                || lower.contains("/img/")   || lower.contains("/images/")
                || lower.contains("/fonts/") || lower.contains("/webjars/")
                || lower.endsWith(".ico")    || lower.endsWith(".png")
                || lower.endsWith(".js")     || lower.endsWith(".css")
                || lower.endsWith(".map");
    }

    private String resolveDashboard(User user) {
        if (user.getDefaultDashboard() == null) return "/dashboard";
        return switch (user.getDefaultDashboard()) {
            case ACCOUNTS   -> "/accounts/dashboard";
            case INVENTORY  -> "/inventory/dashboard";
            case PRODUCTION -> "/production/dashboard";
            case SALES      -> "/sales/dashboard";
            case PURCHASE   -> "/purchase/dashboard";
            case HRM        -> "/hrm/dashboard";
            case COMMERCIAL -> "/commercial/dashboard";
            default         -> "/dashboard";
        };
    }
}
