// Path: com/asg/spindleserp/ecommerce/dashboard/EcDashboardController.java
package com.asg.spindleserp.ecommerce;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * EcDashboardController
 *
 *   GET /ecommerce/dashboard         → Thymeleaf page (ec-dashboard.html)
 *   GET /ecommerce/dashboard/summary → JSON (called by the page's IIFE JS)
 */
@Controller
@RequestMapping("/ecommerce/dashboard")
@RequiredArgsConstructor
public class EcDashboardController {

    private final EcDashboardService dashboardService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activePage", "ec-dashboard");
        return "ecommerce/ec-dashboard";
    }

    @GetMapping("/summary")
    @ResponseBody
    public Map<String, Object> summary() {
        return dashboardService.summary();
    }
}
