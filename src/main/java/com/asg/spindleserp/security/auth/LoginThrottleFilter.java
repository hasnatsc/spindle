package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.auth.LoginAttemptService.Surface;
import com.asg.spindleserp.security.config.SpindleSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * LoginThrottleFilter — refuses POST /login for a locked-out identifier or IP,
 * BEFORE Spring Security attempts authentication.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHY A FILTER, AND NOT THE OBVIOUS PLACES
 * ══════════════════════════════════════════════════════════════════════════
 * The throttle has to run before the BCrypt(12) comparison, because that
 * comparison is the expensive thing we are trying not to do for an attacker:
 * ~250ms of OUR CPU, holding one of the 20 Hikari connections, per guess. A
 * throttle that runs after it has already paid the cost it exists to avoid.
 *
 * Two more obvious-looking spots were rejected, and it is worth recording why,
 * because both LOOK correct and neither is:
 *
 *  ✗ Inside UserDetailsServiceImpl.loadUserByUsername()
 *      DaoAuthenticationProvider.retrieveUser() wraps ANY exception out of
 *      loadUserByUsername — other than UsernameNotFoundException and
 *      InternalAuthenticationServiceException — into
 *      InternalAuthenticationServiceException:
 *
 *          catch (Exception ex) {
 *              throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
 *          }
 *
 *      So a LockedException thrown from there does NOT arrive at the failure
 *      handler as a LockedException. This is not a theory: it is exactly why
 *      the previous LoginFailureHandler's `exception instanceof DisabledException`
 *      and `instanceof LockedException` branches — and the /login?disabled and
 *      /login?locked pages behind them — were DEAD CODE that could never
 *      execute. UserDetailsServiceImpl threw those two exceptions by hand, they
 *      got wrapped, and every failure silently fell through to the /login?error
 *      branch. (That is fixed separately: UserDetailsServiceImpl no longer
 *      throws them, so Spring's own DefaultPreAuthenticationChecks does — and
 *      those are NOT wrapped, because they run after retrieveUser returns.)
 *
 *  ✗ Inside LoginFailureHandler
 *      Runs only AFTER authentication has been attempted. Too late — the hash
 *      comparison has already happened. It is the right place to COUNT a
 *      failure, which is what it now does, but the wrong place to REFUSE one.
 *
 * A filter ahead of UsernamePasswordAuthenticationFilter is the only spot that
 * is both early enough and outside Spring's exception-wrapping machinery.
 *
 * ── On reading the username parameter here ────────────────────────────────
 * request.getParameter() on a form POST parses and CACHES the body in the
 * container's parameter map; UsernamePasswordAuthenticationFilter then reads
 * from that same cached map a moment later. Spring's own
 * AbstractAuthenticationProcessingFilter does exactly this. No body is consumed
 * out from under anyone.
 *
 * ── Why this is NOT a @Component ──────────────────────────────────────────
 * Spring Boot auto-registers EVERY Filter bean into the servlet container's
 * filter chain. This filter belongs in the SPRING SECURITY chain (added via
 * http.addFilterBefore in SecurityConfig), and the servlet chain runs at
 * LOWEST_PRECEDENCE — i.e. AFTER the entire security chain has already run,
 * which is uselessly late for a pre-authentication check. A @Component here
 * would therefore put the same filter in the request path twice, once of them
 * at the wrong point.
 *
 * OncePerRequestFilter's guard attribute happens to suppress the second
 * invocation, so it would "work" — but only by coincidence. Keeping it out of
 * the bean container removes the coincidence. SecurityConfig constructs it
 * directly; it has exactly two dependencies and no lifecycle needs.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@RequiredArgsConstructor
public class LoginThrottleFilter extends OncePerRequestFilter {

    private static final String LOGIN_URL = "/login";

    private final LoginAttemptService loginAttemptService;
    private final SpindleSecurityProperties props;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!isLoginSubmission(request)) {
            chain.doFilter(request, response);
            return;
        }

        String username = request.getParameter("username");
        String ip       = WebSecurityUtils.clientIp(request, props.isTrustForwardedHeaders());

        long blockedFor = loginAttemptService.blockedSeconds(Surface.ERP, username, ip);
        if (blockedFor <= 0) {
            chain.doFilter(request, response);
            return;
        }

        long minutes = Math.max(1, (blockedFor + 59) / 60);
        log.warn("LOGIN BLOCKED  identifier='{}' ip='{}' — {} min remaining",
                WebSecurityUtils.sanitizeForLog(username),
                WebSecurityUtils.sanitizeForLog(ip),
                minutes);

        response.setHeader("Retry-After", String.valueOf(blockedFor));

        if (WebSecurityUtils.isAjax(request)) {
            response.setStatus(429);                                   // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"blocked\":true,\"retryAfterSeconds\":" + blockedFor +
                    ",\"message\":\"Too many failed sign-in attempts. Please try again in " +
                    minutes + (minutes == 1 ? " minute.\"}" : " minutes.\"}"));
            return;
        }

        // Browser form post → the standard failure-page contract, with a new code
        // that LoginController renders as a clear "temporarily locked" message.
        response.sendRedirect(request.getContextPath() + "/login?blocked=" + minutes);
    }

    private boolean isLoginSubmission(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return false;

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx))
                ? uri.substring(ctx.length())
                : uri;

        return LOGIN_URL.equals(path);
    }
}
