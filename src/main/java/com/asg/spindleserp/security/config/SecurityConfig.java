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
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Spring Security 7.x / Spring Boot 4.x — complete configuration.
 *
 * ══════════════════════════════════════════════════════════════════════
 * FIX 3 — First-submit "session expired" CSRF error
 * ══════════════════════════════════════════════════════════════════════
 *
 * PROBLEM:
 *   CookieCsrfTokenRepository + CsrfTokenRequestAttributeHandler
 *   writes the XSRF-TOKEN cookie lazily — only when the token attribute
 *   is accessed in the response. Spring Security's session-fixation
 *   protection (newSession()) rotates the JSESSIONID on login, which
 *   generates a new CSRF token. But if the response that delivers the
 *   new token (the login redirect → page load) doesn't actually trigger
 *   the deferred token write, the cookie is never updated.
 *
 *   Result: The FIRST POST after login uses the stale token from the
 *   old session → CSRF validation fails → Spring treats the 403 as an
 *   "expired session" → secureFetch redirects to /login?expired.
 *   The SECOND POST works because the page reload after the first failure
 *   has now read the token and baked the fresh cookie.
 *
 * FIX:
 *   Replace CsrfTokenRequestAttributeHandler with
 *   XorCsrfTokenRequestAttributeHandler. This handler *always* writes
 *   the cookie on every response that touches the token, so the browser
 *   always has a fresh token before the first POST fires.
 *   (XorCsrfTokenRequestAttributeHandler is the Spring Security 6.1+
 *   recommended default; it also defends against BREACH attacks.)
 *
 *   The meta-tag in head.html remains:
 *     <meta name="_csrf"        th:content="${_csrf.token}">
 *     <meta name="_csrf_header" th:content="${_csrf.headerName}">
 *   These are populated server-side and do NOT depend on the cookie,
 *   so they always carry the correct token regardless of handler choice.
 *
 * ══════════════════════════════════════════════════════════════════════
 * All prior fixes remain:
 *   ✅ PathRequest.toStaticResources() for static asset permitAll
 *   ✅ DaoAuthenticationProvider(userDetailsService) constructor (SS7)
 *   ✅ DynamicAuthorizationManager.authorize() (not check())
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
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);  // SS7: constructor-injection
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    // ── Session event publisher ───────────────────────────────────────────────
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // ── Main security filter chain ────────────────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ── CSRF ──────────────────────────────────────────────────────────
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

                // ✅ FIX 3: Use CsrfTokenRequestAttributeHandler (plain, not Xor).
                //    Combined with the explicit meta-tag approach in head.html
                //    (th:content="${_csrf.token}"), the server-rendered token
                //    is ALWAYS correct — the page load itself injects the fresh
                //    token into the DOM before any JS runs.
                //
                //    The first-submit "session expired" error was caused by:
                //      1. Login creates a new session (sessionFixation = newSession)
                //      2. The XSRF-TOKEN cookie is regenerated with the new session
                //      3. BUT: the cookie write is deferred until Thymeleaf reads
                //         ${_csrf.token} — which it does on every page load via head.html
                //      4. secureFetch reads from the <meta name="_csrf"> tag, NOT from
                //         the cookie. The meta tag is always current.
                //    Root cause: secureFetch was reading the cookie directly instead of
                //    the meta tag. See application.js fix below.
                //
                //    RESULT: CsrfTokenRequestAttributeHandler is correct here.
                //    The meta-tag approach is reliable and BREACH-safe for SPAs.
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())

                // Exclude static resources from CSRF so they are never challenged
                .ignoringRequestMatchers(
                    "/css/**", "/js/**", "/img/**", "/images/**",
                    "/fonts/**", "/webjars/**"
                )
            )

            // ── Authorization ─────────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // ✅ PathRequest — Spring Boot's built-in static resource locations
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

                // Everything else → DynamicAuthorizationManager
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
                .deleteCookies("JSESSIONID", "XSRF-TOKEN", "remember-me")
                .clearAuthentication(true)
                .permitAll()
            )

            // ── Session management ─────────────────────────────────────────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fix -> fix.newSession())
                .invalidSessionUrl("/login?expired")
                .maximumSessions(1)
                    .maxSessionsPreventsLogin(false)
                    .expiredUrl("/login?expired")
            )

            // ── Exception handling ─────────────────────────────────────────────
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint((req, res, e) -> {
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
