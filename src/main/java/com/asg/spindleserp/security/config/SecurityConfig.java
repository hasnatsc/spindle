package com.asg.spindleserp.security.config;

import com.asg.spindleserp.security.auth.CustomAccessDeniedHandler;
import com.asg.spindleserp.security.auth.DynamicAuthorizationManager;
import com.asg.spindleserp.security.auth.LoginFailureHandler;
import com.asg.spindleserp.security.auth.LoginSuccessHandler;
import com.asg.spindleserp.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
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
 * Spring Security 7.x / Spring Boot 4.x configuration.
 *
 * FIXES applied vs previous version:
 *
 *   FIX 1 — DaoAuthenticationProvider constructor (Breaking change in SS7):
 *     OLD (SS6): new DaoAuthenticationProvider()  then  .setUserDetailsService(svc)
 *     NEW (SS7): new DaoAuthenticationProvider(userDetailsService)
 *                setUserDetailsService() method has been REMOVED.
 *                setPasswordEncoder() is still called separately — it was NOT moved to constructor.
 *
 *   FIX 2 — DynamicAuthorizationManager uses authorize() not check() (see that file).
 *
 *   FIX 3 — LoginFailureHandler declares throws ServletException (see that file).
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

    // ── Public paths — no login required ─────────────────────────────────
    private static final String[] PUBLIC_PATHS = {
            "/login", "/login/**",
            "/css/**", "/js/**", "/images/**", "/fonts/**",
            "/webjars/**", "/favicon.ico",
            "/error", "/access-denied",
            "/actuator/health"
    };

    // ── Password encoder ──────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── Authentication provider ───────────────────────────────────────────

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        // ✅ FIX 1: Spring Security 7 removed the no-arg constructor.
        //    UserDetailsService is now a required constructor argument.
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        // setPasswordEncoder() still exists and is still called separately
        provider.setPasswordEncoder(passwordEncoder());

        // Don't hide UsernameNotFoundException — lets LoginFailureHandler
        // distinguish "user not found" from "wrong password"
        provider.setHideUserNotFoundExceptions(false);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    // ── Session event publisher ───────────────────────────────────────────

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // ── Main filter chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ── CSRF ──────────────────────────────────────────────────────
            // CookieCsrfTokenRepository.withHttpOnlyFalse() lets JavaScript
            // read XSRF-TOKEN cookie. secureFetch.js injects X-XSRF-TOKEN.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )

            // ── Authorization ─────────────────────────────────────────────
            // All non-public URLs go through DynamicAuthorizationManager.
            // No hardcoded role or permission names here.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().access(dynamicAuthorizationManager)
            )

            // ── Form login ────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")  // HTML field name
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )

            // ── Logout ────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .clearAuthentication(true)
                .permitAll()
            )

            // ── Session management ────────────────────────────────────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fix -> fix.newSession())
                .maximumSessions(1)
                    .maxSessionsPreventsLogin(false) // new login kicks old session
                    .expiredUrl("/login?expired")
            )

            // ── Exception handling ────────────────────────────────────────
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint((req, res, e) -> {
                    if (isAjaxRequest(req)) {
                        res.setStatus(401);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write(
                            """
                            {"success":false,"message":"Session expired. Please log in again."}
                            """);
                    } else {
                        res.sendRedirect(req.getContextPath() + "/login?sessionExpired");
                    }
                })
            )

            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    private boolean isAjaxRequest(jakarta.servlet.http.HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String xhr    = req.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(xhr)
                || (accept != null && accept.contains("application/json"));
    }
}
