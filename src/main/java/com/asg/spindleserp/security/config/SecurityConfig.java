package com.asg.spindleserp.security.config;

import com.asg.spindleserp.ecommerce.storefront.StorefrontPaths;
import com.asg.spindleserp.security.auth.CustomAccessDeniedHandler;
import com.asg.spindleserp.security.auth.DynamicAuthorizationManager;
import com.asg.spindleserp.security.auth.LoginAttemptService;
import com.asg.spindleserp.security.auth.LoginFailureHandler;
import com.asg.spindleserp.security.auth.LoginSuccessHandler;
import com.asg.spindleserp.security.auth.LoginThrottleFilter;
import com.asg.spindleserp.security.auth.WebSecurityUtils;
import com.asg.spindleserp.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 7.x / Spring Boot 4.x — complete configuration.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * PART 1 — ALL SEVEN SESSION FIXES FROM THE PREVIOUS ITERATION ARE RETAINED
 * ══════════════════════════════════════════════════════════════════════════
 * They were correct, they are load-bearing, and none of them are touched here.
 * Restated in one line each so nobody re-introduces the bug they solved:
 *
 *   ✅ sessionFixation(changeSessionId)  — NOT migrateSession(). migrateSession()
 *      creates a new session object and a new cookie; the browser can still be
 *      carrying the old ID on the very next request → no JDBC row → "expired".
 *   ✅ NO invalidSessionUrl              — it intercepts the post-login redirect
 *      itself and bounces the user back to /login before auth is even checked.
 *   ✅ maximumSessions > 1               — during login there are briefly TWO
 *      live sessions for one principal (the anonymous pre-auth one and the new
 *      authenticated one); a limit of 1 expires the new one.
 *   ✅ deleteCookies("SESSION")          — with store-type=jdbc, Spring Session
 *      issues a cookie named SESSION, not JSESSIONID. The old config deleted
 *      "JSESSIONID", a cookie that does not exist in this app — a total no-op,
 *      so logout never actually cleared the browser's session cookie.
 *   ✅ SpringSessionBackedSessionRegistry — HttpSessionEventPublisher listens for
 *      SERVLET-container session events, which never fire for Spring Session, so
 *      the in-memory SessionRegistryImpl accumulated ghost sessions forever.
 *   ✅ XorCsrfTokenRequestAttributeHandler — the non-Xor handler resolves the CSRF
 *      token EAGERLY on every request that reaches CsrfFilter, including the dozen
 *      parallel /css + /js requests on first page load, each generating its own
 *      competing token. Lazy resolution = no race = no first-submit CSRF failure.
 *   ✅ DaoAuthenticationProvider(userDetailsService) constructor injection (SS7).
 *
 * ══════════════════════════════════════════════════════════════════════════
 * PART 2 — WHAT THIS REVISION CHANGES
 * ══════════════════════════════════════════════════════════════════════════
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [HIGH] A. remember-me key was a hard-coded secret in source control   ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *     .key("spindleErpRememberMeKey2026")
 *
 *   TokenBasedRememberMeServices builds its cookie as
 *       base64( username : expiry : md5(username : expiry : password : KEY) )
 *   The KEY is the ONLY thing standing between an attacker and a forged
 *   remember-me cookie for an arbitrary user. It sat in a Git-tracked .java
 *   file — known to every developer who ever cloned the repo, every CI runner
 *   that ever built it, and anyone who ever gains read access to the source.
 *   It is a credential, and it now lives in the environment:
 *       APP_REMEMBER_ME_KEY=$(openssl rand -base64 48)
 *   A loud WARN fires at boot if the old default is still in use, and the whole
 *   feature can be switched off with app.security.remember-me.enabled=false.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [HIGH] B. No brute-force protection on POST /login                    ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   sec_users.account_non_locked existed and was checked — and NOTHING in the
 *   codebase ever set it to false. Unlimited password guessing, forever.
 *   Now: LoginAttemptService, wired through LoginFailureHandler (count) and
 *   LoginSuccessHandler (reset). See that class for why BCrypt(12) is not,
 *   by itself, a rate limit.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [HIGH] C. Public storefront pages were unreachable → /login?expired   ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   PUBLIC_URLS listed the storefront prefixes but MISSED /about, /contact,
 *   /faq, /page/** and /newsletter/** — all of which are real mapped endpoints
 *   (StorefrontSiteController, StorefrontContentController) linked from the
 *   storefront footer. They fell through to
 *   .anyRequest().access(dynamicAuthorizationManager), which denies anonymous
 *   principals unconditionally, so clicking "About Us" on a PUBLIC SHOP sent
 *   the visitor to the ERP login page.
 *
 *   Root cause was structural: the list was hand-maintained in two files
 *   (here and in StorefrontOrgContextFilter), one of which carried a comment
 *   claiming they were kept in sync. They were not. There is now ONE list —
 *   StorefrontPaths — imported by both.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] D. hideUserNotFoundExceptions(false) → account enumeration   ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   With it false, a nonexistent username throws UsernameNotFoundException
 *   while a wrong password throws BadCredentialsException. Those are two
 *   distinguishable outcomes for an attacker probing which staff accounts
 *   exist. Now true: both collapse to BadCredentialsException.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [MEDIUM] E. No security response headers at all                       ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   There was no .headers(...) block, so the app shipped Spring's bare
 *   defaults and NOTHING else. Added:
 *     Content-Security-Policy   frame-ancestors / base-uri / form-action /
 *                               object-src — see SpindleSecurityProperties for
 *                               why script-src is deliberately NOT restricted
 *                               (the Color Admin theme is wall-to-wall inline JS
 *                               and a script-src policy would white-screen the
 *                               entire admin).
 *     Referrer-Policy           stops full ERP URLs — which contain document
 *                               numbers and record ids — leaking to third-party
 *                               hosts in the Referer header.
 *     Permissions-Policy        camera/mic/geolocation off.
 *     HSTS                      only emitted over HTTPS, gated on require-https.
 *     X-Frame-Options SAMEORIGIN + CSP frame-ancestors — clickjacking.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║ [LOW] F. Hard-coded maximumSessions(3)                                ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *   Now app.security.max-sessions-per-user.
 *
 * ── A note on why the storefront is permitAll ─────────────────────────────
 *   /account/**, /checkout/** and /wishlist/** are permitAll here. That is NOT
 *   a hole. Storefront customers are EcCustomer rows with a session attribute —
 *   they have no sec_users row, no Authentication, no GrantedAuthority, so
 *   DynamicAuthorizationManager literally cannot see them and would deny every
 *   real customer. The login gate for those paths is StorefrontAuthInterceptor
 *   (registered in WebMvcConfig), backed by the controllers' own checks.
 *   Spring Security's job here is to get out of the way of a surface it does
 *   not model — not to authorise it.
 * ══════════════════════════════════════════════════════════════════════════
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UserDetailsServiceImpl      userDetailsService;
    private final DynamicAuthorizationManager dynamicAuthorizationManager;
    private final LoginSuccessHandler         loginSuccessHandler;
    private final LoginFailureHandler         loginFailureHandler;
    private final LoginAttemptService         loginAttemptService;
    private final CustomAccessDeniedHandler   accessDeniedHandler;
    private final SpindleSecurityProperties   props;

    /** ERP-side public endpoints. Storefront paths come from StorefrontPaths. */
    private static final String[] ERP_PUBLIC_URLS = {
            "/login", "/login/**",
            "/error",
            "/access-denied",
            "/actuator/health", "/actuator/health/**",
            "/favicon.ico",
            "/favicon.svg"
    };

    /** Static assets — permitAll and CSRF-exempt. */
    private static final String[] STATIC_URLS = {
            "/css/**", "/js/**", "/img/**", "/images/**",
            "/fonts/**", "/webjars/**",
            "/favicon.ico", "/favicon.svg"
    };

    /**
     * ✅ ONE list, assembled from ONE source of truth.
     * StorefrontPaths.PUBLIC is the same array StorefrontOrgContextFilter uses,
     * so the two can never drift apart again.
     */
    private static String[] publicUrls() {
        List<String> all = new ArrayList<>(List.of(ERP_PUBLIC_URLS));
        all.addAll(List.of(StorefrontPaths.PUBLIC));
        return all.toArray(String[]::new);
    }

    // ── Password encoder ──────────────────────────────────────────────────────
    /**
     * The single PasswordEncoder for the whole app. StorefrontAuthService now
     * INJECTS this bean instead of doing `new BCryptPasswordEncoder(12)` in a
     * field initialiser — so raising the cost factor here actually raises it
     * everywhere, instead of silently leaving customer passwords at the old cost.
     *
     * BCrypt hashes are self-describing ($2a$12$…), so changing this strength
     * does not invalidate any existing hash: old hashes keep verifying at their
     * own recorded cost, new ones are written at the new cost.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── Authentication provider ───────────────────────────────────────────────
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // ✅ SS7: constructor-injection of UserDetailsService (not setter).
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        // ✅ FIX D — was false, which let an attacker distinguish "no such user"
        //    (UsernameNotFoundException) from "wrong password"
        //    (BadCredentialsException). true collapses both into
        //    BadCredentialsException. DaoAuthenticationProvider still runs its
        //    own dummy-hash timing mitigation on the not-found path, so the two
        //    are indistinguishable by clock as well as by exception type.
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    // NOTE: no standalone AuthenticationManager @Bean on purpose.
    // .authenticationProvider(...) on HttpSecurity is what builds the
    // AuthenticationManager formLogin actually uses. A separate bean would be a
    // second, unused ProviderManager that someone will eventually autowire by
    // mistake.

    // ── Spring-Session-aware concurrent session registry ──────────────────────
    @Bean
    public SpringSessionBackedSessionRegistry<?> sessionRegistry(
            FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(sessionRepository);
    }

    // ── Main security filter chain ────────────────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SpringSessionBackedSessionRegistry<?> sessionRegistry)
            throws Exception {

        http
                // ══ CSRF ═══════════════════════════════════════════════════════
                .csrf(csrf -> csrf
                        // ✅ RETAINED: lazy token resolution. The non-Xor handler
                        //    resolves eagerly on every request reaching CsrfFilter —
                        //    including the ~12 parallel asset requests on first page
                        //    load, each generating and Set-Cookie-ing its own token.
                        //    Last response to land wins the cookie jar, at random,
                        //    while the login form's hidden field was rendered from a
                        //    different token → MissingCsrfTokenException on first
                        //    submit, deterministically. Lazy = no race.
                        .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())

                        // Static resources never need a token.
                        // NOTE: the storefront's POST endpoints (/cart/add,
                        // /account/login, /checkout/place-order, /wishlist/toggle …)
                        // are deliberately NOT exempt. They are permitAll, but
                        // permitAll ≠ CSRF-exempt — a cross-site POST that adds items
                        // to a victim's cart or changes their delivery address is a
                        // real attack, and the storefront JS already sends the token
                        // via secureFetch()'s <meta name="_csrf"> read.
                        .ignoringRequestMatchers(STATIC_URLS)
                )

                // ══ SECURITY HEADERS (FIX E — there were none) ═════════════════
                .headers(headers -> {
                    headers
                            // Clickjacking. CSP frame-ancestors below is the modern
                            // control; X-Frame-Options is kept for old browsers.
                            .frameOptions(frame -> frame.sameOrigin())

                            // Do not leak "/purchase/orders/PO-25-000417" to any
                            // third-party host the browser happens to call.
                            .referrerPolicy(referrer -> referrer.policy(
                                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));

                    // Emitted by Spring only over HTTPS — safe to always configure.
                    if (props.isRequireHttps()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(false)
                                .maxAgeInSeconds(31_536_000L));   // 1 year
                    }

                    String csp = props.getContentSecurityPolicy();
                    if (csp != null && !csp.isBlank()) {
                        headers.contentSecurityPolicy(policy -> policy.policyDirectives(csp));
                    }

                    String permissions = props.getPermissionsPolicy();
                    if (permissions != null && !permissions.isBlank()) {
                        headers.addHeaderWriter(
                                new StaticHeadersWriter("Permissions-Policy", permissions));
                    }
                })

                // ══ AUTHORIZATION ══════════════════════════════════════════════
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers(STATIC_URLS).permitAll()

                        // ✅ FIX C — ERP public URLs + the single shared storefront list.
                        .requestMatchers(publicUrls()).permitAll()

                        // Everything else → DynamicAuthorizationManager (DB-driven, cached).
                        .anyRequest().access(dynamicAuthorizationManager)
                )

                // ══ FORM LOGIN ═════════════════════════════════════════════════
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )

                // ══ LOGOUT ═════════════════════════════════════════════════════
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)      // nukes SF_* keys too
                        .clearAuthentication(true)
                        // ✅ RETAINED: with store-type=jdbc the cookie is SESSION, not
                        //    JSESSIONID. The old config deleted "JSESSIONID" — a cookie
                        //    that never exists in this app — so logout was a no-op on
                        //    the browser side and the dead cookie was replayed on the
                        //    next request, including the next login.
                        .deleteCookies("SESSION", "XSRF-TOKEN",
                                "remember-me", props.getRememberMe().getCookieName())
                )

                // ══ SESSION MANAGEMENT ═════════════════════════════════════════
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)

                        // ✅ RETAINED: same session object, ID rotates in place, JDBC
                        //    primary key updated atomically. No lost-session window.
                        //    (StorefrontAuthService now does the equivalent by hand for
                        //    the customer login, which never passes through here.)
                        .sessionFixation(fix -> fix.changeSessionId())

                        // ✅ RETAINED: NO invalidSessionUrl. It fires on the post-login
                        //    redirect while the old cookie is still in flight, and
                        //    bounces the user back to /login before auth is checked.
                        //    The authenticationEntryPoint below is the single
                        //    authoritative handler for genuinely-unauthenticated
                        //    requests, including real expiries.

                        .maximumSessions(props.getMaxSessionsPerUser())   // ✅ FIX F
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired")
                        .sessionRegistry(sessionRegistry)                 // ✅ RETAINED
                )

                // ══ EXCEPTION HANDLING ═════════════════════════════════════════
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint((req, res, e) -> {
                            log.warn("AUTH REQUIRED  uri='{}' method='{}' type='{}'",
                                    WebSecurityUtils.sanitizeForLog(req.getRequestURI()),
                                    WebSecurityUtils.sanitizeForLog(req.getMethod()),
                                    e.getClass().getSimpleName());

                            if (WebSecurityUtils.isAjax(req)) {
                                res.setStatus(401);
                                res.setContentType("application/json;charset=UTF-8");
                                res.getWriter().write("""
                                        {"success":false,"message":"Session expired. Please log in again."}""");
                            } else {
                                res.sendRedirect(req.getContextPath() + "/login?expired");
                            }
                        })
                )

                .authenticationProvider(authenticationProvider())

                // ══ BRUTE-FORCE THROTTLE (FIX B) ═══════════════════════════════
                // MUST sit ahead of UsernamePasswordAuthenticationFilter: the whole
                // point is to refuse a locked-out attempt BEFORE the BCrypt(12)
                // comparison spends ~250ms of our CPU and one Hikari connection on
                // it. Placing it in the failure handler instead would mean paying
                // exactly the cost the throttle exists to avoid, on every guess.
                //
                // Constructed here rather than injected as a @Component: Spring Boot
                // auto-registers every Filter BEAN into the servlet chain as well, which
                // would put this filter in the request path a second time, at
                // LOWEST_PRECEDENCE — i.e. after the whole security chain, uselessly late
                // for a pre-auth check. Not a bean, no double registration.
                .addFilterBefore(new LoginThrottleFilter(loginAttemptService, props),
                                 UsernamePasswordAuthenticationFilter.class);

        // ══ REMEMBER-ME (FIX A) ════════════════════════════════════════════════
        // Registered only when enabled, and only ever with an externalised key.
        if (props.getRememberMe().isEnabled()) {
            var rm = props.getRememberMe();
            http.rememberMe(remember -> remember
                    .userDetailsService(userDetailsService)
                    .key(rm.getKey())                                   // ✅ from env, not source
                    .rememberMeParameter("remember-me")
                    .rememberMeCookieName(rm.getCookieName())
                    .tokenValiditySeconds(rm.getValiditySeconds())
                    .useSecureCookie(props.isRequireHttps())            // ✅ Secure flag in prod
            );
        } else {
            log.info("SECURITY: remember-me is DISABLED (app.security.remember-me.enabled=false).");
        }

        // ══ HTTPS CHANNEL (optional — leave off if the proxy terminates TLS) ═══
        if (props.isRedirectToHttps()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }
}
