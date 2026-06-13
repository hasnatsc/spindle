package com.asg.spindleserp.security.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles failed login attempts.
 *
 * FIX (Spring Security 7.x):
 *   super.onAuthenticationFailure() now declares throws ServletException.
 *   The @Override method must also declare it.
 */
@Component
@Slf4j
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public LoginFailureHandler() {
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            // ✅ FIX: declare throws ServletException (required in Spring Security 7)
            throws IOException, ServletException {

        String identifier = request.getParameter("username");

        if (exception instanceof UsernameNotFoundException) {
            log.warn("LOGIN FAIL  identifier='{}' reason=USER_NOT_FOUND  ip={}",
                    identifier, request.getRemoteAddr());
            setDefaultFailureUrl("/login?error");

        } else if (exception instanceof DisabledException) {
            log.warn("LOGIN FAIL  identifier='{}' reason=ACCOUNT_DISABLED  ip={}",
                    identifier, request.getRemoteAddr());
            setDefaultFailureUrl("/login?disabled");

        } else if (exception instanceof LockedException) {
            log.warn("LOGIN FAIL  identifier='{}' reason=ACCOUNT_LOCKED  ip={}",
                    identifier, request.getRemoteAddr());
            setDefaultFailureUrl("/login?locked");

        } else {
            log.warn("LOGIN FAIL  identifier='{}' reason=BAD_CREDENTIALS  ip={}",
                    identifier, request.getRemoteAddr());
            setDefaultFailureUrl("/login?error");
        }

        // ✅ Now compiles — super also declares throws ServletException in SS7
        super.onAuthenticationFailure(request, response, exception);

        // Reset to safe default after the redirect is done
        setDefaultFailureUrl("/login?error");
    }
}
