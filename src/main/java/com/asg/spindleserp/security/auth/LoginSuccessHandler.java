package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.repository.UserRepository;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Runs after successful login.
 * Updates last_login_at and redirects to the dashboard.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Stamp last login time
        userRepository.findById(userDetails.getUserId()).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });

        log.info("LOGIN SUCCESS  user='{}' org={} ip={}",
                userDetails.getUsername(),
                userDetails.getOrganizationId(),
                request.getRemoteAddr());

        // Prevent caching of authenticated pages
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Redirect
        getRedirectStrategy().sendRedirect(request, response, "/dashboard");
    }
}
