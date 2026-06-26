package com.asg.spindleserp.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DashboardController
 *
 * Routes:
 *   GET /dashboard                → erp-main-dashboard.html   ← MAIN ERP DASHBOARD
 *   GET /dashboard/erp-summary    → JSON  (AJAX data endpoint)
 *
 *   ── Specialist Dashboards ──
 *   GET /dashboard/commercial                   → dashboard/commercial-dashboard.html  (legacy route)
 *   GET /dashboard/accounts-dashboard           → accounts-dashboard.html
 *   GET /dashboard/accounts-dashboard-spinning  → accounts-dashboard-spinning.html
 *   GET /dashboard/accounts-dashboard-fabrics   → accounts-dashboard-fabrics.html
 *   GET /dashboard/asfl-erp-dashborad           → asfl-erp-control-tower-dashborad.html
 *   GET /dashboard/fabric-precosting-dashboard  → fabric-precosting-dashboard.html
 *
 *   NOTE — Module-specific dashboards now live in their own controllers:
 *     /inventory/dashboard       → ModuleDashboardController
 *     /sales/dashboard           → ModuleDashboardController
 *     /accounts/dashboard        → ModuleDashboardController
 *     /production/dashboard      → ProductionController
 *     /hrm/dashboard             → ModuleDashboardController (full-summary at /hrm/dashboard/full-summary)
 *     /crm/dashboard             → CrmController
 *     /budget/dashboard          → BudgetController
 *     /fixed-assets/dashboard    → FixedAssetController
 *     /commercial/dashboard      → CommercialController  (module-level)
 *     /purchase/dashboard        → PurchaseController
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ── MAIN ERP DASHBOARD ────────────────────────────────────────────────────

    @GetMapping
    public String home(Model m) {
        return "redirect:/dashboard";
    }

    /** Main landing page — full ERP summary for all permitted modules */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/erp-main-dashboard";
    }

    /**
     * AJAX endpoint consumed by erp-main-dashboard.html
     * Single optimised PostgreSQL query returns all module KPIs in one round-trip.
     */
    @GetMapping("/dashboard/erp-summary")
    @ResponseBody
    public Map<String, Object> erpSummary() {
        return dashboardService.erpSummary();
    }

    // ── SPECIALIST DASHBOARDS ─────────────────────────────────────────────────

    @GetMapping("/dashboard/commercial")
    public String commercialDashboard(Model m) {
        m.addAttribute("activePage", "commercial-dashboard");
        return "dashboard/commercial-dashboard";
    }

    @GetMapping("/dashboard/accounts-dashboard")
    public String financialDashboard() {
        return "dashboard/accounts-dashboard";
    }

    @GetMapping("/dashboard/accounts-dashboard-spinning")
    public String financialDashboardSpinning() {
        return "dashboard/accounts-dashboard-spinning";
    }

    @GetMapping("/dashboard/accounts-dashboard-fabrics")
    public String financialDashboardFabrics() {
        return "dashboard/accounts-dashboard-fabrics";
    }

    @GetMapping("/dashboard/asfl-erp-dashborad")
    public String financialErpDashboard() {
        return "dashboard/asfl-erp-control-tower-dashborad";
    }

    @GetMapping("/dashboard/fabric-precosting-dashboard")
    public String fabricPreCostingDashboard() {
        return "dashboard/fabric-precosting-dashboard";
    }
}
