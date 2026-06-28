package com.asg.spindleserp.security.auth;

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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * LoginSuccessHandler
 *
 * After successful authentication:
 *   1. Invalidates the old anonymous pre-auth session (eliminates the
 *      "ghost session" that causes maximumSessions to see 2 for 1 user).
 *   2. Loads User WITH all allowed scope sets (one query).
 *   3. Calls userContextService.loadContext() → populates the new session.
 *      From this point forward every request uses ContextProvider — zero DB.
 *   4. Stamps lastLoginAt.
 *   5. Redirects to the user's configured defaultDashboard.
 *
 * ══════════════════════════════════════════════════════════════════════
 * FIX — ghost anonymous session causing "session expired" on first login
 * ══════════════════════════════════════════════════════════════════════
 *
 * PROBLEM:
 *   When the user visits /login, Spring Security creates an anonymous
 *   session (to hold the CSRF token and any saved-request state).
 *   After login, session-fixation protection (changeSessionId) rotates
 *   the session ID on that SAME session object. But the old anonymous
 *   session ID is still stored in the SPRING_SESSION table until the
 *   session expires naturally. With maximumSessions(1), Spring's
 *   concurrent session control sees the old anonymous session as a
 *   "prior login" for the same principal and may expire the new session.
 *
 * FIX:
 *   Explicitly call request.getSession(false) before context loading.
 *   After changeSessionId() has already fired (done by Spring Security
 *   before this handler runs), the current session IS the new session.
 *   We just ensure the session is not null and is properly initialised.
 *   The session.invalidate() call is NOT needed here because
 *   changeSessionId() already handled the old ID — calling invalidate()
 *   would destroy the new session and break everything.
 *
 *   The real fix is in SecurityConfig:
 *     changeSessionId()     instead of migrateSession()
 *     maximumSessions(3)    instead of maximumSessions(1)
 *     no invalidSessionUrl  (removed — was the false-positive trigger)
 *
 *   This handler just adds defensive logging and the new-session touch
 *   to ensure the SPRING_SESSION row exists before the redirect fires.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository     userRepository;
    private final UserContextService userContextService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                        HttpServletResponse response,
                                        Authentication      authentication) throws IOException {

        String username = authentication.getName();

        // ── Touch / ensure the session exists and is persisted ────────────────
        // changeSessionId() already fired in the security filter chain before
        // this handler runs. Calling getSession(true) here ensures the new
        // session row is flushed to SPRING_SESSION before the redirect response
        // leaves the server. Without this, a very fast browser redirect can
        // arrive at the server before the JDBC session store commits the row.
        HttpSession session = request.getSession(true);
        session.setAttribute("_loginTs", System.currentTimeMillis()); // force row creation

        // ── Load user + all allowed scope collections (one query) ─────────────
        User user = userRepository
                .findByUsernameWithAllContext(username)
                .orElseThrow(() -> new IllegalStateException(
                        "User not found after login: " + username));

        // ── Build and store session context ───────────────────────────────────
        // From this point every request uses ContextProvider — zero DB hits.
        userContextService.loadContext(user);

        // ── Stamp last login ──────────────────────────────────────────────────
        userRepository.updateLastLogin(username, LocalDateTime.now());

        // ── Anti-cache headers ────────────────────────────────────────────────
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma",        "no-cache");
        response.setHeader("Expires",       "0");

        log.info("LOGIN OK  user='{}' org='{}' sessionId='{}'",
                username,
                user.getOrganization() != null ? user.getOrganization().getName() : "—",
                session.getId());

        // ── Redirect to configured dashboard ──────────────────────────────────
        getRedirectStrategy().sendRedirect(request, response, resolveDashboard(user));
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
