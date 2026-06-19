package com.asg.spindleserp.security.auth;

import com.asg.spindleserp.security.entity.User;
import com.asg.spindleserp.security.repository.UserRepository;
import com.asg.spindleserp.security.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * LoginSuccessHandler
 *
 * After successful authentication:
 *   1. Loads User WITH all allowed scope sets (one query).
 *   2. Calls userContextService.loadContext() → populates the session holder.
 *      From this point forward every request uses ContextProvider — zero DB.
 *   3. Stamps lastLoginAt.
 *   4. Redirects to the user's configured defaultDashboard.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository     userRepository;
    private final UserContextService userContextService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest  request, HttpServletResponse response, Authentication      authentication) throws IOException {
        String username = authentication.getName();
        // One query: user + organizations + allowedBU + allowedCC + allowedWH
        User user = userRepository.findByUsernameWithAllContext(username).orElseThrow(() -> new IllegalStateException("User not found after login: " + username));
        // Build session context (single DB round-trip inside)
        userContextService.loadContext(user);
        // Stamp last login
        userRepository.updateLastLogin(username, LocalDateTime.now());

        // Anti-cache headers
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma",        "no-cache");
        response.setHeader("Expires",       "0");

        log.info("LOGIN OK  user='{}' org='{}'", username,
                user.getOrganization() != null ? user.getOrganization().getName() : "—");

        getRedirectStrategy().sendRedirect(request, response, resolveDashboard(user));
    }

    private String resolveDashboard(User user) {
        if (user.getDefaultDashboard() == null) return "/dashboard";
        return switch (user.getDefaultDashboard()) {
            case ACCOUNTS   -> "/accounts/dashboard";
            case INVENTORY  -> "/inventory/dashboard";
            case PRODUCTION -> "/production/dashboard";
            case SALES      -> "/sales/dashboard";
            case PURCHASE   -> "/purchase/dashboard";
            case HRM        -> "/hrm/dashboard";
            case COMMERCIAL -> "/commercial/dashboard";
            default         -> "/dashboard";
        };
    }
}
