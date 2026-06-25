package com.asg.spindleserp.security.controller;

import com.asg.spindleserp.security.service.SecurityDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SecurityDashboardController
 *
 * Routes:
 *   GET /security/dashboard           → security/security-dashboard.html
 *   GET /security/dashboard/summary   → JSON  (AJAX)
 *
 * Also maps the /security/users convenience alias:
 *   GET /security/users  → redirect to /users   (fixes sidebar link)
 */
@Controller
@RequiredArgsConstructor
public class SecurityDashboardController {

    private final SecurityDashboardService securityDashboardService;

    // ── Dashboard page ────────────────────────────────────────────────────────

    @GetMapping("/security/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("activePage", "security-dashboard");
        return "security/security-dashboard";
    }

    // ── Dashboard AJAX data ───────────────────────────────────────────────────

    @GetMapping("/security/dashboard/summary")
    @ResponseBody
    public Map<String, Object> dashboardSummary() {
        return securityDashboardService.summary();
    }

    // ── Convenience redirects from sidebar links ───────────────────────────────

    /** /security/users → /users (canonical UserController) */
    @GetMapping("/security/users")
    public String usersRedirect() { return "redirect:/users"; }

    /** /security/roles → /roles */
    @GetMapping("/security/roles")
    public String rolesRedirect() { return "redirect:/roles"; }

    /** /security/permissions → /permissions */
    @GetMapping("/security/permissions")
    public String permissionsRedirect() { return "redirect:/permissions"; }

    /** /security/menus → /menus */
    @GetMapping("/security/menus")
    public String menusRedirect() { return "redirect:/menus"; }

    /** /security/role-menus → /role-menus */
    @GetMapping("/security/role-menus")
    public String roleMenusRedirect() { return "redirect:/role-menus"; }
}
