package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.auth.LoginAttemptService.Surface;
import com.asg.spindleserp.security.config.SpindleSecurityProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * LoginFailureHandler — ERP staff login failures.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * BUG 1 — ★ THREAD-SAFETY. The old handler mutated shared singleton state.
 * ══════════════════════════════════════════════════════════════════════════
 * The previous implementation extended SimpleUrlAuthenticationFailureHandler
 * and did this:
 *
 *     if (exception instanceof DisabledException) {
 *         setDefaultFailureUrl("/login?disabled");      // ← mutates the BEAN
 *     } else {
 *         setDefaultFailureUrl("/login?error");         // ← mutates the BEAN
 *     }
 *     super.onAuthenticationFailure(request, response, exception);
 *     setDefaultFailureUrl("/login?error");             // ← "reset" afterwards
 *
 * This is a @Component. There is ONE instance, shared by every request thread.
 * setDefaultFailureUrl() writes to an instance field. Two failed logins landing
 * concurrently interleave like this:
 *
 *     Thread A (disabled account)  setDefaultFailureUrl("/login?disabled")
 *     Thread B (wrong password)    setDefaultFailureUrl("/login?error")
 *     Thread A                     super.onAuthenticationFailure() → reads
 *                                  "/login?error"   ← WRONG. A's user is told
 *                                                     "bad password" when their
 *                                                     account is actually disabled.
 *     Thread B                     super.onAuthenticationFailure() → reads
 *                                  whatever was written last
 *
 * The trailing "reset to safe default" line makes it worse, not better: it is a
 * third unsynchronised write to the same field. Under any concurrent login load
 * — which is every morning at 9am in a real ERP — users are shown each other's
 * failure reasons at random.
 *
 * FIX: implement AuthenticationFailureHandler directly. No inherited mutable
 * state, no setters, nothing shared. The redirect URL is a local variable.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * BUG 2 — ★ The DisabledException / LockedException branches were DEAD CODE.
 * ══════════════════════════════════════════════════════════════════════════
 * They could never fire, so /login?disabled and /login?locked never rendered,
 * and a disabled user was always told "invalid username or password".
 *
 * Why: UserDetailsServiceImpl threw those two exceptions by hand from inside
 * loadUserByUsername(). But DaoAuthenticationProvider.retrieveUser() wraps
 * anything coming out of loadUserByUsername() that is not a
 * UsernameNotFoundException:
 *
 *     catch (Exception ex) {
 *         throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
 *     }
 *
 * So `exception instanceof DisabledException` was tested against an
 * InternalAuthenticationServiceException, was always false, and every failure
 * fell through to the generic /login?error branch.
 *
 * FIX (two halves):
 *   • UserDetailsServiceImpl no longer throws them. It returns the UserDetails
 *     with the correct flags and lets Spring's own DefaultPreAuthenticationChecks
 *     throw — those run AFTER retrieveUser() returns, so they are NOT wrapped.
 *   • This handler also unwraps InternalAuthenticationServiceException, so if
 *     any other code path ever wraps one again, it still routes correctly rather
 *     than silently degrading to "bad password".
 *
 * ══════════════════════════════════════════════════════════════════════════
 * BUG 3 — ★ LOG INJECTION (CWE-117).
 * ══════════════════════════════════════════════════════════════════════════
 *     log.warn("LOGIN FAIL  identifier='{}' reason=BAD_CREDENTIALS  ip={}",
 *              identifier, request.getRemoteAddr());
 *
 * `identifier` is the raw, unvalidated `username` request parameter. Submitting
 * a username containing CR/LF writes forged lines into the log file. Every
 * identifier now goes through WebSecurityUtils.sanitizeForLog().
 *
 * ══════════════════════════════════════════════════════════════════════════
 * NEW — this handler now COUNTS the failure (LoginAttemptService).
 * ══════════════════════════════════════════════════════════════════════════
 * Counting happens here; REFUSING happens in LoginThrottleFilter, which runs
 * before the password comparison. See that class for why the two cannot be the
 * same place.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;
    private final SpindleSecurityProperties props;

    /** Stateless and thread-safe — unlike the field this class used to mutate. */
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String identifier = request.getParameter("username");
        String ip         = WebSecurityUtils.clientIp(request, props.isTrustForwardedHeaders());
        String safeId     = WebSecurityUtils.sanitizeForLog(identifier);   // ✅ CWE-117
        String safeIp     = WebSecurityUtils.sanitizeForLog(ip);

        AuthenticationException cause = unwrap(exception);
        String reason = reasonOf(cause);

        // ── Count it. An internal service error is NOT the user's fault and must
        //    not push them towards a lockout, so it is excluded.
        if (!(cause instanceof InternalAuthenticationServiceException)) {
            loginAttemptService.loginFailed(Surface.ERP, identifier, ip);
        }

        log.warn("LOGIN FAIL  identifier='{}' reason={} ip={}", safeId, reason, safeIp);

        // ── Route. Local variable. No shared state. No race. ──────────────────
        String target = targetFor(cause);
        redirectStrategy.sendRedirect(request, response, target);
    }

    /**
     * Recover the real cause when Spring wrapped it.
     *
     * DaoAuthenticationProvider.retrieveUser() wraps every non-
     * UsernameNotFoundException from loadUserByUsername() into an
     * InternalAuthenticationServiceException. This unwrapping is why the
     * disabled/locked branches now actually work, and why they will keep working
     * even if someone later re-introduces a manual throw inside a
     * UserDetailsService.
     */
    private static AuthenticationException unwrap(AuthenticationException exception) {
        if (exception instanceof InternalAuthenticationServiceException
                && exception.getCause() instanceof AuthenticationException inner) {
            return inner;
        }
        return exception;
    }

    private static String reasonOf(AuthenticationException e) {
        if (e instanceof UsernameNotFoundException)             return "USER_NOT_FOUND";
        if (e instanceof DisabledException)                     return "ACCOUNT_DISABLED";
        if (e instanceof LockedException)                       return "ACCOUNT_LOCKED";
        if (e instanceof CredentialsExpiredException)           return "CREDENTIALS_EXPIRED";
        if (e instanceof InternalAuthenticationServiceException) return "INTERNAL_ERROR";
        return "BAD_CREDENTIALS";
    }

    private String targetFor(AuthenticationException e) {
        // Collapse everything to a single generic outcome when configured to.
        // See SpindleSecurityProperties.Login.genericErrors for the trade-off:
        // distinguishing "disabled" from "wrong password" is good UX for an
        // internal ERP and an enumeration oracle for an internet-facing one.
        if (props.getLogin().isGenericErrors()) return "/login?error";

        if (e instanceof DisabledException)           return "/login?disabled";
        if (e instanceof LockedException)             return "/login?locked";
        if (e instanceof CredentialsExpiredException) return "/login?credentialsExpired";
        return "/login?error";
    }
}
