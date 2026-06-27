package com.asg.spindleserp.dashboard;

import com.asg.spindleserp.accounts.controller.AccountsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ModuleDashboardController
 *
 * Routes:
 *   GET  /inventory/dashboard           → dashboard/inventory-dashboard.html
 *   GET  /inventory/dashboard/summary   → JSON
 *
 *   GET  /sales/dashboard               → dashboard/sales-dashboard.html
 *   GET  /sales/dashboard/summary       → JSON
 *
 *   GET  /accounts/dashboard            → dashboard/accounts-module-dashboard.html
 *   GET  /accounts/dashboard/summary    → JSON
 *
 *   GET  /production/dashboard          → dashboard/production-dashboard.html
 *   GET  /production/dashboard/summary  → JSON
 *
 *   GET  /hrm/dashboard                 → dashboard/hrm-dashboard.html
 *   GET  /hrm/dashboard/full-summary    → JSON
 *
 * Note: Purchase → PurchaseController, CRM → CrmController,
 *       Commercial → CommercialController, Budget → BudgetController,
 *       Fixed Assets → FixedAssetController.
 */
@Controller
@RequiredArgsConstructor
public class ModuleDashboardController {

    private final InventoryDashboardService        inventoryDashboardService;
    private final SalesDashboardService            salesDashboardService;
    private final AccountsDashboardService         accountsDashboardService;
    private final ProductionModuleDashboardService productionDashboardService;
    private final HrmModuleDashboardService        hrmDashboardService;

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

//    @GetMapping("/production/dashboard")
//    public String productionDashboardPage(Model model) {
//        model.addAttribute("activePage", "production-dashboard");
//        return "dashboard/production-dashboard";
//    }
//
//    @GetMapping("/production/dashboard/summary")
//    @ResponseBody
//    public Map<String, Object> productionSummary() {
//        return productionDashboardService.summary();
//    }

    // ── HRM ───────────────────────────────────────────────────────────────────

//    @GetMapping("/hrm/dashboard")
//    public String hrmDashboardPage(Model model) {
//        model.addAttribute("activePage", "hrm-dashboard");
//        return "dashboard/hrm-dashboard";
//    }

    /**
     * Full HRM dashboard summary — richer than HrmController's basic endpoint.
     * Remove @GetMapping("/hrm/dashboard/summary") from HrmController if both exist.
     */
//    @GetMapping("/hrm/dashboard/full-summary")
//    @ResponseBody
//    public Map<String, Object> hrmFullSummary() {
//        return hrmDashboardService.summary();
//    }
}
