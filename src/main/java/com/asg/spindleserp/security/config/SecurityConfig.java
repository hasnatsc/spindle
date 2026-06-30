package com.asg.spindleserp.security.config;

import com.asg.spindleserp.security.auth.CustomAccessDeniedHandler;
import com.asg.spindleserp.security.auth.DynamicAuthorizationManager;
import com.asg.spindleserp.security.auth.LoginFailureHandler;
import com.asg.spindleserp.security.auth.LoginSuccessHandler;
import com.asg.spindleserp.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

/**
 * Spring Security 7.x / Spring Boot 4.x — complete configuration.
 *
 * ══════════════════════════════════════════════════════════════════════
 * FIX — "Session expired" / "not authorized" on FIRST login after logout
 * ══════════════════════════════════════════════════════════════════════
 *
 * SYMPTOMS:
 *   After logout, OR on the very first login of a browser session:
 *     1st login attempt → "Your session has expired. Please sign in again."
 *                          (sometimes 403 "access denied" on /dashboard instead)
 *     2nd login attempt → success
 *
 * Bugs 1–3 below (changeSessionId, removing invalidSessionUrl, raising
 * maximumSessions) were a correct partial fix but did NOT fully resolve
 * the issue — bugs 4 and 5 were still live, and they are the ones that
 * actually reproduce the symptom deterministically on every logout →
 * login cycle.
 *
 * BUG 1 — migrateSession() causes a lost-session window.            [FIXED]
 *   migrateSession() creates a BRAND-NEW session object and issues a
 *   new Set-Cookie: JSESSIONID. The browser must receive and store that
 *   cookie before any subsequent request fires. If the first request
 *   after the login-redirect still carries the OLD JSESSIONID (cookie
 *   hasn't settled yet, or browser sends it on the redirect itself),
 *   Spring Session JDBC finds no row for that ID → fires invalidSessionUrl
 *   → /login?expired.
 *
 *   FIX: changeSessionId() keeps the SAME session object on the server;
 *   only the ID changes in-place. The JDBC session store still has a row
 *   for the session — the ID update is atomic. No lost-session window.
 *
 * BUG 2 — invalidSessionUrl fires on the login redirect itself.     [FIXED]
 *   When Spring Security's session-fixation protection runs migrateSession()
 *   and the browser sends the old JSESSIONID on the very next request
 *   (the redirect to /dashboard), invalidSessionUrl("/login?expired")
 *   intercepts that request and bounces the user back to the login page
 *   before authentication is even checked.
 *
 *   FIX: Remove invalidSessionUrl entirely. The authenticationEntryPoint
 *   already handles truly-unauthenticated requests by redirecting to
 *   /login?expired. Duplicate coverage from invalidSessionUrl is what
 *   triggers the false positive.
 *
 * BUG 3 — maximumSessions(1) expires the just-created session.      [FIXED]
 *   With maxSessionsPreventsLogin(false), when a second login arrives
 *   Spring expires the OLDER session. But during the first-ever login
 *   flow there can briefly be TWO live sessions for the same user:
 *   the anonymous pre-auth session (which carried the username through
 *   the login form) and the new authenticated session. maximumSessions(1)
 *   sees "2 sessions" and expires one → the new one loses its row in
 *   SPRING_SESSION → next request → 401/expired.
 *
 *   FIX: Raise maximumSessions to 3 (comfortable for multi-tab ERP
 *   usage). In a single-user ERP org-admin scenario this is fine.
 *   True single-device enforcement belongs in a login-audit log, not
 *   in concurrent session limits which are too coarse for Spring JDBC
 *   sessions during auth.
 *
 * BUG 4 — deleteCookies() names the WRONG cookie, so logout never
 *         actually clears the browser's session cookie.        [NEW FIX]
 *   application.properties sets spring.session.store-type=jdbc with no
 *   cookie-name override. In that mode Spring Session's own
 *   DefaultCookieSerializer issues a cookie literally named "SESSION" —
 *   NOT the servlet container's native "JSESSIONID". The logout config
 *   below was calling:
 *       .deleteCookies("JSESSIONID", "XSRF-TOKEN", "remember-me")
 *   "JSESSIONID" never exists as a cookie in this app, so that call is a
 *   complete no-op. The browser keeps the real "SESSION" cookie after
 *   logout — pointing at a row that invalidateHttpSession(true) just
 *   deleted server-side — and sends that dead cookie on every request
 *   that follows, including the next /login page load and the next
 *   POST /login. THAT stale cookie is what creates the inconsistent
 *   session state the very next login has to fight through. Bugs 1–3
 *   were really just papering over the fallout from this.
 *
 *   FIX: delete "SESSION" (the cookie Spring Session actually issues),
 *   not "JSESSIONID". Also pin the cookie name explicitly in
 *   application.properties (server.servlet.session.cookie.name=SESSION)
 *   so the two can never silently drift apart again.
 *
 * BUG 5 — HttpSessionEventPublisher does not work with Spring Session,
 *         so SessionRegistry never learns a session has ended.  [NEW FIX]
 *   HttpSessionEventPublisher listens for the SERVLET CONTAINER's native
 *   HttpSessionListener callbacks. Spring Session (JDBC-backed) replaces
 *   the container's session mechanism with its own filter and its own
 *   repository — container-level listener events never fire for it.
 *   Result: the in-memory SessionRegistryImpl that maximumSessions(3)
 *   relies on never removes an entry when a session logs out or times
 *   out. Every login/logout cycle (the norm during active dev/QA
 *   testing) leaves one more "ghost" SessionInformation behind. Once the
 *   ghost count reaches the configured limit, ConcurrentSessionControl-
 *   AuthenticationStrategy starts expiring entries on the very NEXT
 *   login — and because the registry's bookkeeping is divorced from the
 *   real JDBC-backed store, it can end up expiring the brand-new session
 *   in that same request. ConcurrentSessionFilter then reports exactly
 *   that as "expired" on the next page load (/dashboard) via expiredUrl.
 *
 *   FIX: replace SessionRegistryImpl + HttpSessionEventPublisher with
 *   SpringSessionBackedSessionRegistry, which reads concurrent-session
 *   state directly from the same JDBC-backed SPRING_SESSION table Spring
 *   Session itself uses — always accurate, no listener events needed,
 *   and still correct if this is ever scaled to multiple app instances
 *   (the in-memory registry is per-JVM and silently wrong the moment
 *   there's more than one).
 *
 * ══════════════════════════════════════════════════════════════════════
 * ALL PRIOR FIXES RETAINED:
 *   ✅ CsrfTokenRequestAttributeHandler + meta-tag approach (FIX 3 from
 *      prior iteration — secureFetch reads from <meta name="_csrf">,
 *      not from the cookie, so the first-submit CSRF error is gone)
 *   ✅ PathRequest.toStaticResources() for static asset permitAll
 *   ✅ DaoAuthenticationProvider(userDetailsService) constructor (SS7)
 *   ✅ DynamicAuthorizationManager.authorize() wildcard signature
 *   ✅ LoginFailureHandler throws IOException, ServletException
 * ══════════════════════════════════════════════════════════════════════
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl      userDetailsService;
    private final DynamicAuthorizationManager dynamicAuthorizationManager;
    private final LoginSuccessHandler         loginSuccessHandler;
    private final LoginFailureHandler         loginFailureHandler;
    private final CustomAccessDeniedHandler   accessDeniedHandler;

    // ── Explicit public URL patterns ──────────────────────────────────────────
    private static final String[] PUBLIC_URLS = {
            "/login", "/login/**",
            "/error",
            "/access-denied",
            "/actuator/health",
            "/favicon.ico",
            "/favicon.svg"
    };

    // ── Password encoder ──────────────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── Authentication provider ───────────────────────────────────────────────
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // ✅ SS7: constructor-injection of UserDetailsService (not setter)
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    // ── Spring-Session-aware concurrent session registry ───────────────────────
    // ✅ FIX BUG 5: HttpSessionEventPublisher (a servlet HttpSessionListener)
    //    never fires for Spring-Session-backed sessions, so the old in-memory
    //    SessionRegistryImpl silently never removed an entry on logout/expiry.
    //    SpringSessionBackedSessionRegistry reads concurrent-session state
    //    straight from the JDBC-backed SPRING_SESSION table instead — always
    //    accurate, no listener events required.
    @Bean
    public SpringSessionBackedSessionRegistry<?> sessionRegistry(
            FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(sessionRepository);
    }

    // ── Main security filter chain ────────────────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SpringSessionBackedSessionRegistry<?> sessionRegistry) throws Exception {

        http
                // ── CSRF ──────────────────────────────────────────────────────────
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

                        // CsrfTokenRequestAttributeHandler: server-renders token into
                        // <meta name="_csrf" th:content="${_csrf.token}"> via head.html.
                        // secureFetch() reads from that meta tag — always fresh, never stale.
                        // The XSRF-TOKEN cookie is a secondary path for Thymeleaf forms.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())

                        // Static resources never need a CSRF token
                        .ignoringRequestMatchers(
                                "/css/**", "/js/**", "/img/**", "/images/**",
                                "/fonts/**", "/webjars/**"
                        )
                )

                // ── Authorization ─────────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // Spring Boot's built-in static resource locations
                        .requestMatchers(
                                PathRequest.toStaticResources().atCommonLocations()
                        ).permitAll()

                        // Belt-and-suspenders explicit patterns
                        .requestMatchers(
                                "/css/**", "/js/**", "/img/**", "/images/**",
                                "/fonts/**", "/webjars/**",
                                "/favicon.ico", "/favicon.svg"
                        ).permitAll()

                        // Public endpoints
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // Everything else → DynamicAuthorizationManager (DB-driven, cached)
                        .anyRequest().access(dynamicAuthorizationManager)
                )

                // ── Form login ────────────────────────────────────────────────────
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )

                // ── Remember-me ───────────────────────────────────────────────────
                .rememberMe(rm -> rm
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                        .rememberMeParameter("remember-me")
                        .key("spindleErpRememberMeKey2026")
                )

                // ── Logout ────────────────────────────────────────────────────────
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        // ✅ FIX BUG 4: Spring Session (store-type=jdbc) issues a cookie
                        //    named "SESSION", not "JSESSIONID". Deleting "JSESSIONID" was
                        //    a no-op — the browser kept the real session cookie after
                        //    logout, pointing at a DB row that no longer existed.
                        .deleteCookies("SESSION", "XSRF-TOKEN", "remember-me")
                        .clearAuthentication(true)
                        .permitAll()
                )

                // ── Session management ─────────────────────────────────────────────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)

                        // ✅ FIX BUG 1 + 2: changeSessionId() instead of migrateSession().
                        //    - Same session object, only the ID rotates in-place.
                        //    - JDBC store updates the primary key atomically.
                        //    - No "lost session" window between old and new JSESSIONID.
                        //    - migrateSession() was causing the browser to carry the old
                        //      JSESSIONID on the login redirect → JDBC row not found →
                        //      Spring fires invalidSessionUrl → /login?expired.
                        .sessionFixation(fix -> fix.changeSessionId())

                        // ✅ FIX BUG 2: DO NOT set invalidSessionUrl here.
                        //    invalidSessionUrl intercepts ANY request with an unrecognised
                        //    JSESSIONID — including the redirect immediately after login
                        //    when the old cookie is still in flight.
                        //    Removed: .invalidSessionUrl("/login?expired")
                        //    The authenticationEntryPoint below handles all truly-unauth
                        //    requests (including real expired sessions) correctly.

                        // ✅ FIX BUG 3: maximumSessions raised from 1 → 3.
                        //    During login there can briefly be 2 live sessions:
                        //    the anonymous pre-auth session + the new authenticated one.
                        //    With limit=1 + maxSessionsPreventsLogin=false, Spring
                        //    expires the oldest session (which might be the new one)
                        //    → JDBC row gone → first request fails → /login?expired.
                        //    Limit=3 gives comfortable multi-tab ERP headroom and
                        //    eliminates this race entirely.
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired")
                        // ✅ FIX BUG 5: Spring-Session-aware registry (bean above)
                        //    instead of the default in-memory SessionRegistryImpl.
                        .sessionRegistry(sessionRegistry)
                )

                // ── Exception handling ─────────────────────────────────────────────
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler)
                        .authenticationEntryPoint((req, res, e) -> {
                            // This is the single authoritative handler for unauthenticated
                            // requests (covers truly expired sessions, not the login race).
                            if (isAjax(req)) {
                                res.setStatus(401);
                                res.setContentType("application/json;charset=UTF-8");
                                res.getWriter().write(
                                        """
                                        {"success":false,"message":"Session expired. Please log in again."}
                                        """);
                            } else {
                                res.sendRedirect(req.getContextPath() + "/login?expired");
                            }
                        })
                )

                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    private boolean isAjax(jakarta.servlet.http.HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String xhr    = req.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(xhr)
                || (accept != null && accept.contains("application/json"));
    }
}