package com.asg.spindleserp.security.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 403 Forbidden.
 *   AJAX / API calls  → JSON 403
 *   Browser requests  → redirect to /access-denied
 *
 * ══════════════════════════════════════════════════════════════════════════
 * RETAINED — CSRF-aware routing (this was a good call and is kept verbatim)
 * ══════════════════════════════════════════════════════════════════════════
 * CsrfFilter runs ahead of the authentication filter and shares this same
 * accessDeniedHandler when no CSRF-specific one is configured. So a stale form
 * token (InvalidCsrfTokenException / MissingCsrfTokenException) lands here too,
 * and ALWAYS with an anonymous-looking principal — because CSRF is checked
 * before authentication runs. Sending those people to /access-denied is a
 * misleading message for someone who was never denied anything: their page
 * (often /login itself, left open in a stale tab across a logout or a restart)
 * simply had an out-of-date token. Those cases redirect to /login?expired
 * instead. Genuine authorization failures — a real, authenticated user lacking a
 * permission — still go to /access-denied.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * FIXED — two small things
 * ══════════════════════════════════════════════════════════════════════════
 * 1. ★ LOG INJECTION (CWE-117). The URI and method were logged raw. Both are
 *    attacker-controlled: a request to a path containing an encoded CR/LF that
 *    the container decodes will write forged lines into the log. Both now go
 *    through WebSecurityUtils.sanitizeForLog().
 *
 * 2. ★ Hand-built JSON. The response body was assembled with string
 *    concatenation:
 *        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
 *    That is safe TODAY because both messages are string literals defined three
 *    lines above. It is one careless edit away from someone interpolating
 *    exception.getMessage() into it and producing broken JSON (or worse) the
 *    first time a message contains a quote. The two responses are now constants.
 *
 * 3. The AJAX check was a private copy of the same logic in SecurityConfig.
 *    Both now call WebSecurityUtils.isAjax() — one definition, one behaviour.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final String JSON_CSRF_EXPIRED = """
            {"success":false,"message":"Your form has expired. Please refresh the page and try again."}""";

    private static final String JSON_ACCESS_DENIED = """
            {"success":false,"message":"Access denied. You do not have permission for this action."}""";

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {

        String username = (request.getUserPrincipal() != null)
                ? request.getUserPrincipal().getName() : "anonymous";

        boolean isCsrfFailure = exception instanceof CsrfException;

        log.warn("ACCESS DENIED  user='{}' uri='{}' method='{}' type='{}'",
                WebSecurityUtils.sanitizeForLog(username),
                WebSecurityUtils.sanitizeForLog(request.getRequestURI()),   // ✅ CWE-117
                WebSecurityUtils.sanitizeForLog(request.getMethod()),       // ✅ CWE-117
                exception.getClass().getSimpleName());

        if (WebSecurityUtils.isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(isCsrfFailure ? JSON_CSRF_EXPIRED : JSON_ACCESS_DENIED);
            return;
        }

        if (isCsrfFailure) {
            response.sendRedirect(request.getContextPath() + "/login?expired");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/access-denied");
    }
}
