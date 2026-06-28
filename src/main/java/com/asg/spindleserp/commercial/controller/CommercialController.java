package com.asg.spindleserp.commercial.controller;

import com.asg.spindleserp.accounts.service.GlReportService;
import com.asg.spindleserp.commercial.dto.*;
import com.asg.spindleserp.commercial.service.CommercialService;
import com.asg.spindleserp.common.dto.DataTableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CommercialController
 *
 * Pages:
 *   GET /commercial/dashboard  → commercial/commercial-dashboard.html
 *   GET /commercial/exports    → commercial/commercial-exports.html
 *   GET /commercial/imports    → commercial/commercial-imports.html
 *   GET /commercial/lc         → commercial/commercial-lc.html
 *
 * GL Integration (bridges to GlReportService — no page navigation required):
 *   GET /commercial/gl/ledger?accountId=&startDate=&endDate=
 *   GET /commercial/gl/trial-balance?asOfDate=&showZeroBalance=
 *   GET /commercial/gl/profit-loss?startDate=&endDate=
 *   GET /commercial/gl/balance-sheet?asOfDate=&compareDate=
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CommercialController {

    private final CommercialService commercialService;
    private final GlReportService   glReportService;

    // ── Pages ──────────────────────────────────────────────────────────────────
    @GetMapping("/commercial/dashboard")
    public String dashboard(Model m) { m.addAttribute("activePage","commercial-dashboard"); return "commercial/commercial-dashboard"; }

    @GetMapping("/commercial/exports")
    public String exportsPage(Model m) { m.addAttribute("activePage","commercial-exports"); m.addAttribute("invoiceType","EXPORT"); return "commercial/commercial-exports"; }

    @GetMapping("/commercial/imports")
    public String importsPage(Model m) { m.addAttribute("activePage","commercial-imports"); m.addAttribute("invoiceType","IMPORT"); return "commercial/commercial-imports"; }

    @GetMapping("/commercial/lc")
    public String lcPage(Model m) { m.addAttribute("activePage","commercial-lc"); return "commercial/commercial-lc"; }

    // ── Dashboard ──────────────────────────────────────────────────────────────
    @GetMapping("/commercial/dashboard/summary")
    @ResponseBody public Map<String,Object> summary() { return commercialService.dashboardSummary(); }

    // ── Commercial Invoice ─────────────────────────────────────────────────────
    @GetMapping("/commercial/invoices/list")
    @ResponseBody
    public DataTableResponse ciList(@RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start, @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search,
            @RequestParam(defaultValue="") String invoiceType) {
        return commercialService.invoiceDatatable(draw, start, length, search, invoiceType);
    }

    @GetMapping("/commercial/invoices/show/{id}")
    @ResponseBody public Map<String,Object> ciShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", commercialService.findInvoiceById(id)));
    }

    @PostMapping("/commercial/invoices/save")
    @ResponseBody public Map<String,Object> ciSave(@RequestBody @Valid CommercialInvoiceDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { commercialService.updateInvoice(dto.getId(), dto); return "Invoice updated."; }
            else { CommercialInvoiceDTO s = commercialService.createInvoice(dto); return "Invoice " + s.getInvoiceNo() + " created."; }
        });
    }

    @DeleteMapping("/commercial/invoices/delete/{id}")
    @ResponseBody public Map<String,Object> ciDelete(@PathVariable Long id) {
        return ok(() -> { commercialService.deleteInvoice(id); return "Invoice deleted."; });
    }

    @PostMapping("/commercial/invoices/finalize/{id}")
    @ResponseBody public Map<String,Object> ciFinalize(@PathVariable Long id) {
        return ok(() -> { commercialService.finalizeInvoice(id); return "Invoice finalized."; });
    }

    @PostMapping("/commercial/invoices/post/{id}")
    @ResponseBody public Map<String,Object> ciPost(@PathVariable Long id) {
        return ok(() -> { commercialService.postInvoice(id); return "Invoice posted."; });
    }

    @PostMapping("/commercial/invoices/cancel/{id}")
    @ResponseBody public Map<String,Object> ciCancel(@PathVariable Long id, @RequestParam(required=false) String remarks) {
        return ok(() -> { commercialService.cancelInvoice(id, remarks); return "Invoice cancelled."; });
    }

    @GetMapping("/commercial/invoices/search")
    @ResponseBody public Map<String,Object> ciSearch(
            @RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="") String invoiceType,
            @RequestParam(defaultValue="1") int page) {
        return commercialService.searchInvoices(search, invoiceType, page);
    }

    // ── LC Settlement ──────────────────────────────────────────────────────────
    @GetMapping("/commercial/settlements/list")
    @ResponseBody
    public DataTableResponse stlList(@RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start, @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search,
            @RequestParam(required=false) Long lcId) {
        return commercialService.settlementDatatable(draw, start, length, search, lcId);
    }

    @GetMapping("/commercial/settlements/show/{id}")
    @ResponseBody public Map<String,Object> stlShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", commercialService.findSettlementById(id)));
    }

    @PostMapping("/commercial/settlements/save")
    @ResponseBody public Map<String,Object> stlSave(@RequestBody @Valid LcSettlementDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { commercialService.updateSettlement(dto.getId(), dto); return "Settlement updated."; }
            else { commercialService.createSettlement(dto); return "Settlement created."; }
        });
    }

    @DeleteMapping("/commercial/settlements/delete/{id}")
    @ResponseBody public Map<String,Object> stlDelete(@PathVariable Long id) {
        return ok(() -> { commercialService.deleteSettlement(id); return "Settlement deleted."; });
    }

    @PostMapping("/commercial/settlements/settle/{id}")
    @ResponseBody public Map<String,Object> stlSettle(@PathVariable Long id) {
        return ok(() -> { commercialService.settleSettlement(id); return "Settlement marked as SETTLED."; });
    }

    @PostMapping("/commercial/settlements/reverse/{id}")
    @ResponseBody public Map<String,Object> stlReverse(@PathVariable Long id) {
        return ok(() -> { commercialService.reverseSettlement(id); return "Settlement reversed."; });
    }

    // ── GL Report Bridge — Commercial module inline GL view ────────────────────
    @GetMapping("/commercial/gl/ledger")
    @ResponseBody
    public Map<String,Object> glLedger(@RequestParam Long accountId,
            @RequestParam(required=false) String startDate, @RequestParam(required=false) String endDate) {
        try { return glReportService.generalLedger(accountId, startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/commercial/gl/trial-balance")
    @ResponseBody
    public Map<String,Object> glTrialBalance(
            @RequestParam(required=false) String asOfDate,
            @RequestParam(defaultValue="false") boolean showZeroBalance) {
        try { return glReportService.trialBalance(asOfDate, showZeroBalance); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/commercial/gl/profit-loss")
    @ResponseBody
    public Map<String,Object> glProfitLoss(@RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required=false) String compareStartDate, @RequestParam(required=false) String compareEndDate) {
        try { return glReportService.profitAndLoss(startDate, endDate, compareStartDate, compareEndDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/commercial/gl/balance-sheet")
    @ResponseBody
    public Map<String,Object> glBalanceSheet(
            @RequestParam(required=false) String asOfDate, @RequestParam(required=false) String compareDate) {
        try { return glReportService.balanceSheet(asOfDate, compareDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────
    private Map<String,Object> ok(Checked a) {
        Map<String,Object> res = new HashMap<>();
        try { Object r = a.run(); res.put("success", true);
              if (r instanceof String msg) res.put("message", msg);
              else if (r instanceof Map<?,?> m) res.put("obj", m);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }
    @FunctionalInterface interface Checked { Object run() throws Exception; }
}
