package com.asg.spindleserp.security.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renders public authentication pages.
 *
 * GET /login   → templates/auth/login.html
 *
 * Spring Security handles POST /login and POST /logout automatically.
 * This controller only renders the page — no processing logic here.
 */
@Slf4j
@Controller
public class LoginController {

    /**
     * @param error   appended by Spring Security on auth failure
     *                or by LoginFailureHandler with specific codes
     * @param logout  appended by Spring Security after /logout
     * @param expired appended by session management on session expiry
     */
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String  error,
            @RequestParam(required = false) String  logout,
            @RequestParam(required = false) String  expired,
            @RequestParam(required = false) String  disabled,
            @RequestParam(required = false) String  locked,
            @RequestParam(required = false) String  sessionExpired,
            Model model) {

        // If already authenticated, redirect to dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/dashboard";
        }

        // ── Map URL params → alert message + type ─────────────────────────────
        String alertType = null;
        String alertMsg  = null;

        if (error != null) {
            alertType = "error";
            alertMsg  = "Invalid username / email / phone or password. Please try again.";
        } else if (disabled != null) {
            alertType = "warning";
            alertMsg  = "Your account has been disabled. Contact your administrator.";
        } else if (locked != null) {
            alertType = "warning";
            alertMsg  = "Your account is locked. Contact your administrator.";
        } else if (logout != null) {
            alertType = "success";
            alertMsg  = "You have been signed out successfully.";
        } else if (expired != null || sessionExpired != null) {
            alertType = "info";
            alertMsg  = "Your session has expired. Please sign in again.";
        }

        if (alertType != null) {
            model.addAttribute("alertType", alertType);
            model.addAttribute("alertMsg",  alertMsg);
        }

        return "auth/login";   // → src/main/resources/templates/auth/login.html
    }

    /** Simple 403 page */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }
}
