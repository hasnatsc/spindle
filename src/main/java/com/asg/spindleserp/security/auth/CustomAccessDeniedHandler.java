package com.asg.spindleserp.security.auth;

import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 403 Forbidden.
 * - AJAX / API calls (Accept: application/json) → returns JSON 403
 * - Browser requests → redirects to /access-denied page
 *
 * ── CSRF-aware routing (added during session-bug follow-up) ────────────────
 * CsrfFilter runs ahead of the authentication filter and shares this same
 * accessDeniedHandler when no CSRF-specific one is configured, so a stale
 * form token (InvalidCsrfTokenException / MissingCsrfTokenException) lands
 * here too — and always with an anonymous-looking principal, even if the
 * user is mid-login, because CSRF is checked before authentication runs.
 * That previously sent people to /access-denied, which is a misleading
 * message for someone who was never "denied" anything — their page (often
 * /login itself, left open in a stale tab across a logout or a restart)
 * just had an out-of-date token. Those cases now redirect to /login?expired
 * instead, reusing the existing "please sign in again" messaging. Genuine
 * authorization failures (a real, authenticated user lacking a permission)
 * are unaffected and still go to /access-denied.
 *
 * The previous version of this handler also logged ACCESS DENIED with no
 * exception detail at all, which made this class of failure indistinguishable
 * from a real authorization denial in the logs. exception.getClass() /
 * getMessage() are now included so the two are never ambiguous again.
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {

        String username = (request.getUserPrincipal() != null)
                ? request.getUserPrincipal().getName() : "anonymous";
        boolean isCsrfFailure = exception instanceof CsrfException;

        log.warn("ACCESS DENIED  user='{}' uri='{}' method='{}' type='{}' reason='{}'",
                username, request.getRequestURI(), request.getMethod(),
                exception.getClass().getSimpleName(), exception.getMessage());

        if (isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            String message = isCsrfFailure
                    ? "Your form has expired. Please refresh the page and try again."
                    : "Access denied. You do not have permission for this action.";
            response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
            return;
        }

        if (isCsrfFailure) {
            response.sendRedirect(request.getContextPath() + "/login?expired");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/access-denied");
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String xhr    = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(xhr)
                || (accept != null && accept.contains("application/json"));
    }
}