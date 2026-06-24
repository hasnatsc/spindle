package com.asg.spindleserp.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ModuleDashboardController
 *
 * Routes:
 *
 *   Inventory
 *     GET  /inventory/dashboard           → dashboard/inventory-dashboard.html
 *     GET  /inventory/dashboard/summary   → JSON
 *
 *   Sales
 *     GET  /sales/dashboard               → dashboard/sales-dashboard.html
 *     GET  /sales/dashboard/summary       → JSON
 *
 *   Accounts
 *     GET  /accounts/dashboard            → dashboard/accounts-module-dashboard.html
 *     GET  /accounts/dashboard/summary    → JSON
 *
 *   Production
 *     GET  /production/dashboard          → dashboard/production-dashboard.html
 *     GET  /production/dashboard/summary  → JSON   (replaces stub in ProductionController)
 *
 *   HRM
 *     GET  /hrm/dashboard                 → dashboard/hrm-dashboard.html
 *     GET  /hrm/dashboard/summary         → JSON   (delegates to HrmModuleDashboardService)
 *
 * Note: Purchase dashboard lives in PurchaseController (/purchase/dashboard).
 *       CRM dashboard lives in CrmController (/crm/dashboard/summary).
 *       Commercial dashboard lives in DashboardController (/dashboard/commercial).
 */
@Controller
@RequiredArgsConstructor
public class ModuleDashboardController {

    private final InventoryDashboardService          inventoryDashboardService;
    private final SalesDashboardService              salesDashboardService;
    private final AccountsDashboardService           accountsDashboardService;
    private final ProductionModuleDashboardService   productionDashboardService;
    private final HrmModuleDashboardService          hrmDashboardService;

    // ── INVENTORY ─────────────────────────────────────────────────────────────

    @GetMapping("/inventory/dashboard")
    public String inventoryDashboardPage(Model model) {
        model.addAttribute("activePage", "inventory-dashboard");
        return "dashboard/inventory-dashboard";
    }

    @GetMapping("/inventory/dashboard/summary")
    @ResponseBody
    public Map<String, Object> inventorySummary() {
        return inventoryDashboardService.summary();
    }

    // ── SALES ─────────────────────────────────────────────────────────────────

    @GetMapping("/sales/dashboard")
    public String salesDashboardPage(Model model) {
        model.addAttribute("activePage", "sales-dashboard");
        return "dashboard/sales-dashboard";
    }

    @GetMapping("/sales/dashboard/summary")
    @ResponseBody
    public Map<String, Object> salesSummary() {
        return salesDashboardService.summary();
    }

    // ── ACCOUNTS ──────────────────────────────────────────────────────────────

    @GetMapping("/accounts/dashboard")
    public String accountsDashboardPage(Model model) {
        model.addAttribute("activePage", "accounts-dashboard");
        return "dashboard/accounts-module-dashboard";
    }

    @GetMapping("/accounts/dashboard/summary")
    @ResponseBody
    public Map<String, Object> accountsSummary() {
        return accountsDashboardService.summary();
    }

    // ── PRODUCTION ────────────────────────────────────────────────────────────

    @GetMapping("/production/dashboards")
    public String productionDashboardPage(Model model) {
        model.addAttribute("activePage", "production-dashboard");
        return "dashboard/production-dashboard";
    }

    @GetMapping("/production/dashboard/summarys")
    @ResponseBody
    public Map<String, Object> productionSummary() {
        return productionDashboardService.summary();
    }

    // ── HRM ───────────────────────────────────────────────────────────────────

    @GetMapping("/hrm/dashboard")
    public String hrmDashboardPage(Model model) {
        model.addAttribute("activePage", "hrm-dashboard");
        return "dashboard/hrm-dashboard";
    }

    /**
     * Overrides /hrm/dashboard/summary from HrmController.
     * HrmController still has @GetMapping("/hrm/dashboard/summary") pointing to HrmService.dashboardSummary().
     * This controller replaces that with the richer HrmModuleDashboardService.
     * If both exist, remove the one in HrmController to avoid ambiguity.
     */
    @GetMapping("/hrm/dashboard/full-summary")
    @ResponseBody
    public Map<String, Object> hrmFullSummary() {
        return hrmDashboardService.summary();
    }
}
