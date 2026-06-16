package com.asg.spindleserp.security.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    

    @GetMapping
    public String dashboard() {
        return "dashboard/fabric-precosting-dashboard"; // templates/dashboard.html
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