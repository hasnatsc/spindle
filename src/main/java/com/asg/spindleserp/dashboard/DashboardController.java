package com.asg.spindleserp.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DashboardController
 *
 * Routes:
 *   GET /dashboard                → erp-main-dashboard.html   ← MAIN ERP DASHBOARD
 *   GET /dashboard/erp-summary    → JSON  (AJAX data endpoint)
 *   GET /dashboard/accounts-dashboard           → accounts-dashboard.html
 *   GET /dashboard/accounts-dashboard-spinning  → accounts-dashboard-spinning.html
 *   GET /dashboard/accounts-dashboard-fabrics   → accounts-dashboard-fabrics.html
 *   GET /dashboard/asfl-erp-dashborad          → asfl-erp-control-tower-dashborad.html
 *   GET /dashboard/fabric-precosting-dashboard  → fabric-precosting-dashboard.html
 *   GET /dashboard/commercial                   → commercial-dashboard.html
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ── MAIN ERP DASHBOARD ────────────────────────────────────────────────────

    /** Main landing page — full ERP summary for all permitted modules */
    @GetMapping
    public String dashboard() {
        return "dashboard/erp-main-dashboard";
    }

    /**
     * AJAX endpoint consumed by erp-main-dashboard.html
     * Single optimised PostgreSQL query returns all module KPIs in one round-trip.
     *
     * Response shape:
     * {
     *   inventory:   { totalStockValue, totalActiveItems, lowStockCount, … },
     *   purchase:    { openPOCount, openPOValue, prPendingCount, grnPendingCount, … },
     *   sales:       { openSOCount, openSOValue, openSQCount, deliveryPendingCount, … },
     *   accounts:    { totalReceivable, totalPayable, overdueReceivable, … },
     *   hrm:         { activeEmployees, onLeaveToday, presentToday, … },
     *   production:  { inProgressCount, completedMTD, activeBOMCount, … },
     *   commercial:  { activeLCCount, activeLCValue, expiredLCCount, … },
     *   crm:         { activeLeadsCount, openOpportunityCount, pipelineValue },
     *   budget:      { activeBudgetCount, overBudgetLineCount },
     *   fixedAssets: { activeAssetCount },
     *   approvals:   { pendingCount, overdueCount },
     *   lifecycle:   { purchase: {PR,RFQ,CS,PO,GRN,PI}, sales: {SQ,SO,DO,DC,INV,PAID} },
     *   exceptions:  [ {priority, module, exceptionType, count, value, actionUrl} ],
     *   exceptionSummary: { critical, high, medium, slaBreach },
     *   pendingApprovalItems: [ {referenceNumber, documentType, documentSummary, isUrgent} ],
     *   stockAlerts: [ {itemName, quantity, reorderLevel} ],
     *   topReceivables: [ {partyName, balance} ],
     *   topPayables:    [ {partyName, balance} ],
     *   monthlyTrend: [ {month, salesValue, purchaseValue, outstandingAR} ]
     * }
     */
    @GetMapping("/erp-summary")
    @ResponseBody
    public Map<String, Object> erpSummary() {
        return dashboardService.erpSummary();
    }

    // ── SPECIALIST DASHBOARDS ─────────────────────────────────────────────────

    @GetMapping("/commercial")
    public String commercialDashboard() {
        return "dashboard/commercial-dashboard";
    }

    @GetMapping("/accounts-dashboard")
    public String financialDashboardInteractive() {
        return "dashboard/accounts-dashboard";
    }

    @GetMapping("/accounts-dashboard-spinning")
    public String financialDashboardFabrics() {
        return "dashboard/accounts-dashboard-spinning";
    }

    @GetMapping("/accounts-dashboard-fabrics")
    public String financialDashboardSpinning() {
        return "dashboard/accounts-dashboard-fabrics";
    }

    @GetMapping("/asfl-erp-dashborad")
    public String financialErpDashboard() {
        return "dashboard/asfl-erp-control-tower-dashborad";
    }

    @GetMapping("/fabric-precosting-dashboard")
    public String financialPreCostingDashboard() {
        return "dashboard/fabric-precosting-dashboard";
    }
}
