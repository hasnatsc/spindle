package com.asg.spindleserp.security.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SpindleSecurityProperties — every security knob in ONE place, bound from
 * application.properties under the {@code app.security.*} prefix.
 *
 * Named SpindleSecurityProperties (not SecurityProperties) so it can never be
 * confused with Spring Boot's own
 * org.springframework.boot.autoconfigure.security.SecurityProperties.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHY EACH SETTING EXISTS — the vulnerability it closes
 * ══════════════════════════════════════════════════════════════════════════
 *
 * remember-me.key
 *   ★ WAS HARD-CODED IN SOURCE: .key("spindleErpRememberMeKey2026").
 *   Spring's TokenBasedRememberMeServices builds the cookie as
 *       base64(username + ":" + expiry + ":" + md5Hex(username + ":" + expiry
 *                                                     + ":" + password + ":" + KEY))
 *   Anyone who has that key AND a user's password hash can forge a valid
 *   remember-me cookie for ANY account. The key sat in a Git-tracked .java
 *   file — i.e. it is known to everyone who has ever cloned the repo, every
 *   CI runner, and anyone who ever gets read access to the source. It is a
 *   credential, and credentials belong in the environment. Now:
 *       APP_REMEMBER_ME_KEY=<64 random chars>
 *   A loud WARN is logged at boot if the insecure default is still in use.
 *
 * login.max-attempts / window-minutes / lock-minutes
 *   ★ THERE WAS NO BRUTE-FORCE PROTECTION AT ALL — on either login surface.
 *   /login and /account/login both accepted unlimited password guesses at
 *   whatever rate the network allowed. sec_users.account_non_locked existed
 *   but nothing ever set it. LoginAttemptService now enforces this.
 *
 * storefront.min-password-length
 *   ★ Registration accepted 6-character passwords with no composition rule.
 *
 * trust-forwarded-headers
 *   ★ X-Forwarded-For was trusted unconditionally (see WebSecurityUtils).
 *   Default false. Set true ONLY behind nginx / ALB / Cloudflare.
 *
 * require-https
 *   Turns on HSTS + Secure cookies + (optionally) an HTTP→HTTPS channel
 *   redirect. Off in dev, on in production.
 *
 * content-security-policy
 *   ★ NO CSP HEADER WAS SENT AT ALL.
 *   The default value below is deliberately CONSERVATIVE — it does NOT try to
 *   restrict script-src, because the Color Admin theme is wall-to-wall inline
 *   <script> and inline style="…", and a script-src policy would white-screen
 *   the entire admin. What it DOES lock down costs nothing and blocks three
 *   whole attack classes:
 *       frame-ancestors 'self'  → clickjacking (stronger than X-Frame-Options,
 *                                 and the only one modern browsers still honour)
 *       base-uri 'self'         → <base href> injection, which silently
 *                                 re-points every relative script/form URL on
 *                                 the page at an attacker's host
 *       object-src 'none'       → legacy <object>/<embed> plugin XSS
 *       form-action 'self'      → an injected <form> cannot POST the CSRF
 *                                 token or form data off-site
 *   Tighten it later (nonce-based script-src) once the theme's inline JS is
 *   externalised; the property is here so that is a config change, not a code
 *   change.
 *
 * max-sessions-per-user
 *   Was hard-coded to 3. Now tunable without a rebuild.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SpindleSecurityProperties {

    /** The literal that was previously compiled into SecurityConfig.java. */
    public static final String INSECURE_DEFAULT_REMEMBER_ME_KEY = "spindleErpRememberMeKey2026";

    /**
     * Trust X-Forwarded-For / X-Real-IP when resolving the client IP.
     * ONLY enable when a reverse proxy in front of the app OVERWRITES the
     * header. Enabling this without such a proxy makes every IP-based control
     * (rate limiting, audit trail) spoofable by any client.
     */
    private boolean trustForwardedHeaders = false;

    /** Production HTTPS mode: HSTS + Secure session cookie. */
    private boolean requireHttps = false;

    /** Redirect plain HTTP to HTTPS at the app layer (leave off if the proxy does it). */
    private boolean redirectToHttps = false;

    /** Concurrent sessions permitted per ERP user (multi-tab / multi-device headroom). */
    private int maxSessionsPerUser = 3;

    /**
     * Value for the Content-Security-Policy header. Blank/null disables the header.
     * See the class javadoc for why script-src is deliberately absent.
     */
    private String contentSecurityPolicy =
            "default-src 'self'; " +
            "img-src 'self' data: blob: https:; " +
            "font-src 'self' data: https:; " +
            "object-src 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'; " +
            "frame-ancestors 'self'";

    /** Value for the Permissions-Policy header. Blank/null disables the header. */
    private String permissionsPolicy =
            "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=()";

    private final RememberMe rememberMe = new RememberMe();
    private final Login      login      = new Login();
    private final Storefront storefront = new Storefront();

    // ── Nested groups ────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class RememberMe {
        /** Master switch. Turn OFF for high-assurance deployments. */
        private boolean enabled = true;

        /** MUST be overridden via APP_REMEMBER_ME_KEY in every real environment. */
        private String key = INSECURE_DEFAULT_REMEMBER_ME_KEY;

        /** Default 7 days (unchanged from the previous hard-coded value). */
        private int validitySeconds = 7 * 24 * 60 * 60;

        private String cookieName = "SPINDLE_RM";
    }

    /** ERP staff login (/login) throttle. */
    @Getter
    @Setter
    public static class Login {
        /** Failures allowed per identifier before a temporary lock. */
        private int maxAttempts = 5;

        /** Failures allowed per source IP before a temporary lock (catches spraying). */
        private int maxAttemptsPerIp = 20;

        /** Rolling window in which failures accumulate. */
        private int windowMinutes = 15;

        /** How long a locked identifier / IP stays locked. */
        private int lockMinutes = 15;

        /**
         * When true, ALL failures redirect to /login?error — disabled and locked
         * accounts are no longer distinguishable from a wrong password.
         *
         * Trade-off, stated plainly:
         *   false (default) — the user is told "your account is disabled /
         *     locked, contact your administrator". Excellent UX for an internal
         *     ERP, but it confirms to an attacker that the username exists.
         *     Acceptable here: /login is an internal, staff-only surface and the
         *     usernames are not secret.
         *   true — no distinction. Choose this if /login is ever exposed to the
         *     open internet.
         */
        private boolean genericErrors = false;
    }

    /** Storefront customer login (/account/login) throttle + password policy. */
    @Getter
    @Setter
    public static class Storefront {
        /** Failures allowed per phone/email before a temporary lock. */
        private int maxLoginAttempts = 8;

        /** Failures allowed per source IP (credential stuffing across many accounts). */
        private int maxLoginAttemptsPerIp = 30;

        private int windowMinutes = 15;
        private int lockMinutes   = 15;

        /** New registrations allowed per source IP per hour (bot signup floods). */
        private int maxRegistrationsPerIpPerHour = 5;

        /** Minimum customer password length. Was 6, with no composition rule. */
        private int minPasswordLength = 8;

        /** Require at least one letter AND one digit. */
        private boolean requireLetterAndDigit = true;

        /** Idle timeout, in minutes, for a logged-in customer session. 0 = disabled. */
        private int idleTimeoutMinutes = 0;
    }

    // ── Boot-time safety check ───────────────────────────────────────────────

    @PostConstruct
    void warnOnInsecureDefaults() {
        if (rememberMe.isEnabled()
                && INSECURE_DEFAULT_REMEMBER_ME_KEY.equals(rememberMe.getKey())) {
            log.warn("""
                    
                    ════════════════════════════════════════════════════════════════════
                     SECURITY: app.security.remember-me.key is still the DEFAULT value
                     that was previously hard-coded in SecurityConfig.java.
                    
                     That key is a CREDENTIAL. Anyone holding it — i.e. anyone who has
                     ever cloned this repository — can forge a valid remember-me cookie.
                    
                     Set a long random value before going live, e.g.:
                       export APP_REMEMBER_ME_KEY="$(openssl rand -base64 48)"
                    
                     Or disable the feature entirely:
                       app.security.remember-me.enabled=false
                    ════════════════════════════════════════════════════════════════════
                    """);
        }
        if (trustForwardedHeaders) {
            log.warn("SECURITY: app.security.trust-forwarded-headers=true — X-Forwarded-For " +
                     "is being trusted. This is ONLY safe if a reverse proxy OVERWRITES that " +
                     "header on every inbound request. If the app is directly internet-facing, " +
                     "rate limiting and the login audit trail are now spoofable.");
        }
    }
}
