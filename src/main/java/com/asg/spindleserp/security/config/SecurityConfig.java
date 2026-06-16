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
 * ROOT CAUSE FIX — why /js/application.js was served as HTML
 * ══════════════════════════════════════════════════════════════════════
 *
 * PROBLEM:
 *   The original config had:
 *     .requestMatchers(PUBLIC).permitAll()            // "/js/**" listed here
 *     .anyRequest().access(dynamicAuthorizationManager)
 *
 *   Spring Security evaluates rules IN ORDER, BUT the string-pattern
 *   matcher for "/js/**" only matches when the path is an exact
 *   pattern hit. When the session has expired or the request arrives
 *   before authentication, the DynamicAuthorizationManager fires first,
 *   denies access, redirects to /login, and /login redirects back to
 *   the originally-requested page (/users) — which then serves HTML.
 *   The browser receives that HTML response as if it were JavaScript.
 *
 * FIX 1 (this file):
 *   Replace the hand-written string array for static assets with
 *   PathRequest.toStaticResources().atCommonLocations()
 *   This uses Spring Boot's built-in static resource locations and
 *   MUST be declared BEFORE the anyRequest() catch-all.
 *   The PathRequest matchers are evaluated before DynamicAuthorizationManager.
 *
 * FIX 2 (WebMvcConfig.java):
 *   Explicitly register addResourceHandlers so Spring MVC always
 *   serves /js/**, /css/**, /img/** from classpath:/static/ regardless
 *   of security filter order.
 *
 * All three SS7 breaking-change fixes remain:
 *   ✅ DaoAuthenticationProvider(userDetailsService) constructor
 *   ✅ DynamicAuthorizationManager.authorize() (not check())
 *   ✅ LoginFailureHandler throws IOException, ServletException
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
    // Static assets are handled separately via PathRequest below.
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
                new DaoAuthenticationProvider(userDetailsService);   // SS7: must pass service in constructor
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
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // Exclude static resources from CSRF so they are never challenged
                .ignoringRequestMatchers(
                    "/css/**", "/js/**", "/img/**", "/images/**",
                    "/fonts/**", "/webjars/**"
                )
            )

            // ── Authorization ─────────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // ✅ FIX 1: Use PathRequest for Spring Boot's static resource
                //    locations (classpath:/static/, classpath:/public/, etc.)
                //    This matcher is resolved by ResourceHttpRequestHandler —
                //    it NEVER passes through DynamicAuthorizationManager.
                .requestMatchers(
                    PathRequest.toStaticResources().atCommonLocations()
                ).permitAll()

                // ✅ FIX 1b: Explicit path patterns as belt-and-suspenders
                //    These cover any custom static paths outside Spring's defaults.
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

            // ── Remember-me ────────────────────────────────────────────────────
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
