package com.asg.spindleserp.production.controller;

import com.asg.spindleserp.common.dto.DataTableResponse;
import com.asg.spindleserp.production.dto.*;
import com.asg.spindleserp.production.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * ProductionController — all production module pages and REST endpoints.
 *
 * Pages:
 *   GET /production/dashboard → production/production-dashboard.html  ← NEW
 *   GET /production/boms      → production/production-boms.html
 *   GET /production/orders    → production/production-orders.html
 *
 * Dashboard API:
 *   GET /production/dashboard/summary → dashboardSummary() (single-pass CTE)
 *
 * JS prefixes:  bom* | prod*
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    // ── Pages ──────────────────────────────────────────────────────────────────

    /** Full standalone production dashboard */
    @GetMapping("/production/dashboard")
    public String dashboardPage(Model m) {
        m.addAttribute("activePage", "production-dashboard");
        return "production/production-dashboard";
    }

    @GetMapping("/production/boms")
    public String bomsPage(Model m) {
        m.addAttribute("activePage", "production-boms");
        return "production/production-boms";
    }

    @GetMapping("/production/orders")
    public String ordersPage(Model m) {
        m.addAttribute("activePage", "production-orders");
        return "production/production-orders";
    }

    // ── Dashboard API ──────────────────────────────────────────────────────────

    /**
     * Fully optimised — single-pass CTE with COUNT(*) FILTER.
     * Called by both:
     *   - production-dashboard.html on page load
     *   - production-orders.html KPI strip on load + after each status change
     */
    @GetMapping("/production/dashboard/summary")
    @ResponseBody
    public Map<String,Object> summary() {
        return productionService.dashboardSummary();
    }

    // ── BOM ────────────────────────────────────────────────────────────────────

    @GetMapping("/production/boms/list")
    @ResponseBody
    public DataTableResponse bomList(
            @RequestParam(defaultValue="1")  int draw,
            @RequestParam(defaultValue="0")  int start,
            @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search) {
        return productionService.bomDatatable(draw, start, length, search);
    }

    @GetMapping("/production/boms/show/{id}")
    @ResponseBody
    public Map<String,Object> bomShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", productionService.findBomById(id)));
    }

    @PostMapping("/production/boms/save")
    @ResponseBody
    public Map<String,Object> bomSave(@RequestBody @Valid BomDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) {
                productionService.updateBom(dto.getId(), dto);
                return "BOM updated.";
            }
            BomDTO saved = productionService.createBom(dto);
            return "BOM " + saved.getBomCode() + " created.";
        });
    }

    @PostMapping("/production/boms/toggle/{id}")
    @ResponseBody
    public Map<String,Object> bomToggle(@PathVariable Long id) {
        return ok(() -> { productionService.toggleBom(id); return "BOM status toggled."; });
    }

    @DeleteMapping("/production/boms/delete/{id}")
    @ResponseBody
    public Map<String,Object> bomDelete(@PathVariable Long id) {
        return ok(() -> { productionService.deleteBom(id); return "BOM deleted."; });
    }

    @GetMapping("/production/boms/search")
    @ResponseBody
    public Map<String,Object> bomSearch(
            @RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="1") int page) {
        return productionService.searchBoms(search, page);
    }

    @GetMapping("/production/boms/populate")
    @ResponseBody
    public Map<String,Object> bomPopulate(
            @RequestParam Long bomId,
            @RequestParam(required=false) Long      warehouseId,
            @RequestParam(required=false) BigDecimal quantity) {
        return ok(() -> Map.of("defaultData",
            productionService.populateFromBom(bomId, warehouseId, quantity)));
    }

    // ── Production Work Orders ─────────────────────────────────────────────────

    @GetMapping("/production/orders/list")
    @ResponseBody
    public DataTableResponse prodList(
            @RequestParam(defaultValue="1")  int draw,
            @RequestParam(defaultValue="0")  int start,
            @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search,
            @RequestParam(defaultValue="") String status) {
        return productionService.productionDatatable(draw, start, length, search, status);
    }

    @GetMapping("/production/orders/show/{id}")
    @ResponseBody
    public Map<String,Object> prodShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", productionService.findProductionById(id)));
    }

    @PostMapping("/production/orders/save")
    @ResponseBody
    public Map<String,Object> prodSave(@RequestBody @Valid ProductionDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) {
                productionService.updateProduction(dto.getId(), dto);
                return "Production order updated.";
            }
            ProductionDTO saved = productionService.createProduction(dto);
            return "Production order " + saved.getProductionNo() + " created.";
        });
    }

    @DeleteMapping("/production/orders/delete/{id}")
    @ResponseBody
    public Map<String,Object> prodDelete(@PathVariable Long id) {
        return ok(() -> { productionService.deleteProduction(id); return "Production order deleted."; });
    }

    @GetMapping("/production/orders/search")
    @ResponseBody
    public Map<String,Object> prodSearch(
            @RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="1") int page) {
        return productionService.searchProductions(search, page);
    }

    // ── Status transitions ─────────────────────────────────────────────────────

    @PostMapping("/production/orders/submit/{id}")
    @ResponseBody
    public Map<String,Object> prodSubmit(@PathVariable Long id) {
        return ok(() -> { productionService.submitProduction(id); return "Production order submitted."; });
    }

    @PostMapping("/production/orders/approve/{id}")
    @ResponseBody
    public Map<String,Object> prodApprove(@PathVariable Long id) {
        return ok(() -> { productionService.approveProduction(id); return "Production order approved."; });
    }

    @PostMapping("/production/orders/release/{id}")
    @ResponseBody
    public Map<String,Object> prodRelease(@PathVariable Long id) {
        return ok(() -> { productionService.releaseProduction(id); return "Production order released to floor."; });
    }

    @PostMapping("/production/orders/start/{id}")
    @ResponseBody
    public Map<String,Object> prodStart(@PathVariable Long id) {
        return ok(() -> { productionService.startProduction(id); return "Production started."; });
    }

    @PostMapping("/production/orders/complete/{id}")
    @ResponseBody
    public Map<String,Object> prodComplete(@PathVariable Long id) {
        return ok(() -> { productionService.completeProduction(id); return "Production completed. Stock + GL updated."; });
    }

    @PostMapping("/production/orders/reject/{id}")
    @ResponseBody
    public Map<String,Object> prodReject(@PathVariable Long id,
                                          @RequestParam(required=false) String remarks) {
        return ok(() -> { productionService.rejectProduction(id, remarks); return "Production order rejected."; });
    }

    @PostMapping("/production/orders/cancel/{id}")
    @ResponseBody
    public Map<String,Object> prodCancel(@PathVariable Long id,
                                          @RequestParam(required=false) String remarks) {
        return ok(() -> { productionService.cancelProduction(id, remarks); return "Production order cancelled."; });
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Map<String,Object> ok(Checked a) {
        Map<String,Object> res = new HashMap<>();
        try {
            Object r = a.run();
            res.put("success", true);
            if      (r instanceof String  msg) res.put("message", msg);
            else if (r instanceof Map<?,?> m)  res.put("obj", m);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    @FunctionalInterface interface Checked { Object run() throws Exception; }
}
