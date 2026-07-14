package com.asg.spindleserp.security.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * WebSecurityUtils — shared, dependency-free helpers used by every auth
 * surface in the app (ERP form-login, storefront customer login, the access-
 * denied handler, the authentication entry point and the storefront gate).
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHY THESE FOUR HELPERS EXIST IN ONE PLACE
 * ══════════════════════════════════════════════════════════════════════════
 *
 * 1. clientIp(request, trustForwardedHeaders)
 *    ─────────────────────────────────────────────────────────────────────
 *    ★ FIXES A REAL VULNERABILITY.
 *    StorefrontAuthService.recordLogin() previously did this:
 *
 *        String fwd = request.getHeader("X-Forwarded-For");
 *        String ip  = (fwd != null && !fwd.isBlank())
 *                       ? fwd.split(",")[0].trim()
 *                       : request.getRemoteAddr();
 *
 *    X-Forwarded-For is a plain request header. ANY client can send it with
 *    ANY value. Unconditionally trusting it means:
 *      • the audit trail in ec_customer_login_history is attacker-controlled
 *        (an attacker brute-forcing logins simply stamps every attempt with a
 *        different fake IP, so the audit log is worthless), and
 *      • ANY IP-based rate limit built on top of it is trivially bypassed —
 *        rotate the header value on every request and you get unlimited
 *        attempts from a single machine.
 *
 *    X-Forwarded-For is only meaningful when the app sits BEHIND a reverse
 *    proxy that OVERWRITES (not appends to) the header. So it is now gated
 *    behind an explicit opt-in:
 *
 *        app.security.trust-forwarded-headers=true   # ONLY behind nginx/ALB
 *
 *    Default false → request.getRemoteAddr() (the real TCP peer), which
 *    cannot be spoofed.
 *
 * 2. isAjax(request)
 *    Identical logic was copy-pasted into SecurityConfig's entry point and
 *    CustomAccessDeniedHandler. One definition, one behaviour.
 *
 * 3. sanitizeForLog(value)
 *    ★ FIXES LOG INJECTION (CWE-117).
 *    LoginFailureHandler logged the raw `username` request parameter:
 *
 *        log.warn("LOGIN FAIL  identifier='{}' ...", identifier, ...);
 *
 *    A login attempt with the username
 *        admin\r\n2026-07-11 10:00:00 WARN  LOGIN OK user='admin'
 *    writes a forged second line into the log file. Anyone reading (or
 *    grepping, or SIEM-ingesting) that log is now looking at attacker-authored
 *    content. Every identifier is now stripped of CR/LF/TAB and truncated.
 *
 * 4. safeRedirect(candidate, fallback)
 *    ★ FIXES AN OPEN REDIRECT (CWE-601).
 *    StorefrontAuthController put the raw ?redirect= query parameter straight
 *    into the model:
 *
 *        model.addAttribute("redirectUrl", redirect != null ? redirect : "/account/dashboard");
 *
 *    …and the login template hands that to the browser after a successful
 *    sign-in. So:
 *
 *        https://shop.asg.com/account/login?redirect=https://evil.example/login
 *
 *    is a link on the REAL domain, with the REAL TLS certificate, that logs a
 *    customer in and then dumps them on a pixel-perfect phishing clone. This
 *    is the single most effective phishing primitive there is, and it is a
 *    one-line fix: only ever accept a same-site, absolute PATH.
 *
 *    Rejected: anything not starting with '/', protocol-relative '//host',
 *    backslash variants ('/\evil.com' — browsers normalise '\' to '/'),
 *    embedded scheme (':'), CR/LF, and any '..' traversal.
 * ══════════════════════════════════════════════════════════════════════════
 */
public final class WebSecurityUtils {

    private WebSecurityUtils() {}

    private static final int MAX_LOG_LEN = 120;

    // ── 1. Client IP ─────────────────────────────────────────────────────────

    /**
     * Resolve the caller's IP.
     *
     * @param trustForwardedHeaders true ONLY when the app runs behind a reverse
     *                              proxy that overwrites X-Forwarded-For.
     *                              Wire it from app.security.trust-forwarded-headers.
     */
    public static String clientIp(HttpServletRequest request, boolean trustForwardedHeaders) {
        if (request == null) return "unknown";

        if (trustForwardedHeaders) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // Left-most entry is the original client when the edge proxy
                // OVERWRITES the header. Cap the length: the header is still
                // attacker-influenced in size even when the value is trusted.
                String first = xff.split(",")[0].trim();
                if (!first.isBlank() && first.length() <= 45) return first;   // 45 = max IPv6 text length
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank() && realIp.length() <= 45) return realIp.trim();
        }

        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }

    // ── 2. AJAX / JSON detection ─────────────────────────────────────────────

    public static boolean isAjax(HttpServletRequest request) {
        if (request == null) return false;
        String xhr = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(xhr)) return true;

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) return true;

        // fetch()/secureFetch() posting JSON bodies without an explicit Accept
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    // ── 3. Log sanitisation (CWE-117) ────────────────────────────────────────

    /** Strip CR/LF/TAB and truncate — never let user input forge a log line. */
    public static String sanitizeForLog(String value) {
        if (value == null) return "-";
        String cleaned = value.replaceAll("[\\r\\n\\t]", "_");
        if (cleaned.length() > MAX_LOG_LEN) cleaned = cleaned.substring(0, MAX_LOG_LEN) + "…";
        return cleaned;
    }

    // ── 4. Open-redirect guard (CWE-601) ─────────────────────────────────────

    /**
     * Returns {@code candidate} only if it is a safe, same-site absolute path.
     * Otherwise returns {@code fallback}.
     *
     * Accepted:   /account/dashboard      /checkout/info?step=2
     * Rejected:   https://evil.com        //evil.com        /\evil.com
     *             \/evil.com              javascript:alert(1)
     *             /a/../../etc/passwd     anything with CR/LF
     */
    public static String safeRedirect(String candidate, String fallback) {
        if (candidate == null) return fallback;

        String c = candidate.trim();
        if (c.isEmpty())                       return fallback;
        if (c.length() > 512)                  return fallback;   // absurd length → drop
        if (c.indexOf('\r') >= 0
                || c.indexOf('\n') >= 0)       return fallback;   // header/CRLF injection
        if (!c.startsWith("/"))                return fallback;   // must be an absolute path
        if (c.startsWith("//"))                return fallback;   // protocol-relative → external host
        if (c.startsWith("/\\") || c.startsWith("/%5C") || c.startsWith("/%5c"))
                                               return fallback;   // browsers fold '\' to '/'
        if (c.indexOf('\\') >= 0)              return fallback;
        if (c.contains(".."))                  return fallback;   // path traversal
        if (c.contains(":"))                   return fallback;   // any embedded scheme

        return c;
    }
}
