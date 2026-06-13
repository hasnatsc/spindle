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
 * Spring Security 7.x / Spring Boot 4.1.0 — complete configuration.
 *
 * All three breaking-change fixes applied:
 *   ✅ DaoAuthenticationProvider(userDetailsService) — no-arg constructor removed in SS7
 *   ✅ DynamicAuthorizationManager.authorize() — not check() in SS7
 *   ✅ LoginFailureHandler throws IOException, ServletException — SS7 requirement
 *
 * Public paths: /login, /css/**, /js/**, /images/**, /fonts/**, /error, /access-denied
 * Everything else → DynamicAuthorizationManager (zero DB hit per request)
 * SUPER_ADMIN → bypasses all checks
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

    // ── Public paths ──────────────────────────────────────────────────────────
    private static final String[] PUBLIC = {
        "/login", "/login/**",
        "/css/**", "/js/**", "/images/**", "/fonts/**", "/img/**",
        "/webjars/**", "/favicon.ico", "/favicon.svg",
        "/error", "/access-denied",
        "/actuator/health"
    };

    // ── Password encoder ──────────────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ── Authentication provider ───────────────────────────────────────────────
    // ✅ FIX: SS7 requires UserDetailsService in the constructor
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false); // allow LoginFailureHandler to distinguish
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }

    // ── Session event publisher (required for concurrent session control) ─────
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // ── Main security filter chain ────────────────────────────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ── CSRF ──────────────────────────────────────────────────────────
            // CookieCsrfTokenRepository.withHttpOnlyFalse() → JS can read
            // XSRF-TOKEN cookie. secureFetch.js injects X-XSRF-TOKEN on mutations.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )

            // ── Authorization ─────────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC).permitAll()
                .anyRequest().access(dynamicAuthorizationManager)
            )

            // ── Form login ────────────────────────────────────────────────────
            // usernameParameter("username") — matches name="username" in login.html
            // UserDetailsServiceImpl tries username → email → phone in sequence
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )

            // ── Remember-me (7-day cookie) ────────────────────────────────────
            // name="remember-me" in login.html checkbox
            .rememberMe(rm -> rm
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(7 * 24 * 60 * 60)
                .rememberMeParameter("remember-me")
                .key("spindleErpRememberMeKey2026")  // move to application.properties in prod
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

            // ── Session management ────────────────────────────────────────────
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fix -> fix.newSession())
                .invalidSessionUrl("/login?expired")
                .maximumSessions(1)
                    .maxSessionsPreventsLogin(false)  // new login kicks old session
                    .expiredUrl("/login?expired")
            )

            // ── Exception handling ────────────────────────────────────────────
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
