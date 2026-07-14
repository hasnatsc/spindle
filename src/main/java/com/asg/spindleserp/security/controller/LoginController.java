package com.asg.spindleserp.security.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renders the public authentication pages.
 *
 *   GET /login          → templates/auth/login.html
 *   GET /access-denied  → templates/auth/access-denied.html
 *
 * Spring Security handles POST /login and POST /logout. This controller only
 * renders — no processing logic.
 *
 * ══════════════════════════════════════════════════════════════════════════
 * WHAT CHANGED
 * ══════════════════════════════════════════════════════════════════════════
 *
 * 1. ★ ?disabled and ?locked now actually happen.
 *    Both @RequestParams already existed here, both had alert messages written
 *    for them, and NEITHER could ever be reached — because
 *    UserDetailsServiceImpl threw DisabledException/LockedException from inside
 *    loadUserByUsername(), where DaoAuthenticationProvider wraps every such
 *    exception into InternalAuthenticationServiceException, so
 *    LoginFailureHandler's `instanceof DisabledException` test was always false
 *    and every failure redirected to ?error. Two fully-built UX paths that had
 *    never once rendered. Fixed in UserDetailsServiceImpl + LoginFailureHandler;
 *    this page is what they were always supposed to reach.
 *
 * 2. NEW ?blocked=<minutes> — emitted by LoginThrottleFilter when an identifier
 *    or IP is temporarily locked out after repeated failures. Without this, a
 *    throttled user would just see "invalid password" forever and keep trying,
 *    which is both a terrible experience for a colleague who fat-fingered their
 *    password five times and a wasted signal for a real attacker.
 *
 * 3. NEW ?credentialsExpired — for when password expiry is eventually turned on
 *    (User.credentialsNonExpired already exists as a column and is already
 *    checked by Spring's post-authentication checks; there was simply nowhere
 *    for the outcome to land).
 * ══════════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String expired,
            @RequestParam(required = false) String disabled,
            @RequestParam(required = false) String locked,
            @RequestParam(required = false) String blocked,
            @RequestParam(required = false) String credentialsExpired,
            @RequestParam(required = false) String sessionExpired,
            Model model) {

        // Already authenticated → straight to the dashboard.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/dashboard";
        }

        String alertType = null;
        String alertMsg  = null;

        if (blocked != null) {
            // Most specific outcome first: a throttled user has ALSO just had a
            // failed attempt, so ?blocked must win over ?error.
            alertType = "warning";
            alertMsg  = "Too many failed sign-in attempts. Your account is temporarily locked. "
                      + "Please try again in " + safeMinutes(blocked) + ".";

        } else if (disabled != null) {
            alertType = "warning";
            alertMsg  = "Your account has been disabled. Please contact your administrator.";

        } else if (locked != null) {
            alertType = "warning";
            alertMsg  = "Your account is locked. Please contact your administrator.";

        } else if (credentialsExpired != null) {
            alertType = "warning";
            alertMsg  = "Your password has expired. Please contact your administrator to reset it.";

        } else if (error != null) {
            alertType = "error";
            alertMsg  = "Invalid username / email / phone or password. Please try again.";

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

        return "auth/login";
    }

    /** Simple 403 page. */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }

    /**
     * ?blocked carries a number of minutes, but it arrives as a raw query
     * parameter and is rendered into the page. Parse it as an int and clamp it —
     * never echo the raw string back into the HTML.
     */
    private static String safeMinutes(String raw) {
        int minutes;
        try {
            minutes = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return "a few minutes";
        }
        if (minutes < 1)    minutes = 1;
        if (minutes > 1440) minutes = 1440;   // a day is the sane ceiling
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}
