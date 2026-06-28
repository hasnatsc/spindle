package com.asg.spindleserp.budget.controller;

import com.asg.spindleserp.budget.dto.*;
import com.asg.spindleserp.budget.service.BudgetService;
import com.asg.spindleserp.common.dto.DataTableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BudgetController — one controller for all budget-module pages.
 *
 * Pages:
 *   GET /budget/dashboard         → budget/budget-dashboard.html   ← NEW
 *   GET /budget/fiscal-years      → budget/fiscal-years.html
 *   GET /budget/heads             → budget/budget-heads.html
 *   GET /budget/list              → budget/budget-list.html
 *   GET /budget/revisions         → budget/budget-revisions.html
 *   GET /budget/transfers         → budget/budget-transfers.html
 *
 * Dashboard API:
 *   GET /budget/dashboard/summary → dashboardSummary()
 *
 * JS prefixes:
 *   fy* | head* | bgt* | rev* | tfr*
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // ── Pages ──────────────────────────────────────────────────────────────────

    @GetMapping("/budget/dashboard")
    public String dashboardPage(Model m) {
        m.addAttribute("activePage", "budget-dashboard");
        return "budget/budget-dashboard";
    }

    @GetMapping("/budget/fiscal-years")
    public String fyPage(Model m) { m.addAttribute("activePage","budget-fiscal-years"); return "budget/fiscal-years"; }

    @GetMapping("/budget/heads")
    public String headPage(Model m) { m.addAttribute("activePage","budget-heads"); return "budget/budget-heads"; }

    @GetMapping("/budget/list")
    public String budgetPage(Model m) { m.addAttribute("activePage","budget-list"); return "budget/budget-list"; }

    @GetMapping("/budget/revisions")
    public String revisionPage(Model m) { m.addAttribute("activePage","budget-revisions"); return "budget/budget-revisions"; }

    @GetMapping("/budget/transfers")
    public String transferPage(Model m) { m.addAttribute("activePage","budget-transfers"); return "budget/budget-transfers"; }

    // ── Dashboard API ──────────────────────────────────────────────────────────

    /**
     * Org-wide dashboard summary — single-pass CTE, called by budget-dashboard.html on load.
     * Also reloadable on refresh button click.
     */
    @GetMapping("/budget/dashboard/summary")
    @ResponseBody
    public Map<String, Object> dashboardSummary() {
        return budgetService.dashboardSummary();
    }

    // ── Fiscal Year ────────────────────────────────────────────────────────────

    @GetMapping("/budget/fiscal-years/list")
    @ResponseBody
    public DataTableResponse fyList(
            @RequestParam(defaultValue="1") int draw,
            @RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="25") int length,
            @RequestParam(value="search[value]",defaultValue="") String search) {
        return budgetService.fiscalYearDatatable(draw, start, length, search);
    }

    @GetMapping("/budget/fiscal-years/show/{id}")
    @ResponseBody
    public Map<String,Object> fyShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", budgetService.findFiscalYearById(id)));
    }

    @PostMapping("/budget/fiscal-years/save")
    @ResponseBody
    public Map<String,Object> fySave(@RequestBody @Valid FiscalYearDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { budgetService.updateFiscalYear(dto.getId(), dto); return "Fiscal year updated."; }
            else { budgetService.createFiscalYear(dto); return "Fiscal year created."; }
        });
    }

    @PostMapping("/budget/fiscal-years/status/{id}")
    @ResponseBody
    public Map<String,Object> fyStatus(@PathVariable Long id, @RequestParam String status) {
        return ok(() -> { budgetService.updateFiscalYearStatus(id, status); return "Status updated to " + status + "."; });
    }

    @DeleteMapping("/budget/fiscal-years/delete/{id}")
    @ResponseBody
    public Map<String,Object> fyDelete(@PathVariable Long id) {
        return ok(() -> { budgetService.deleteFiscalYear(id); return "Fiscal year deleted."; });
    }

    @GetMapping("/budget/fiscal-years/search")
    @ResponseBody
    public Map<String,Object> fySearch(
            @RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="1") int page) {

        return budgetService.searchFiscalYears(search, page);
    }

    // ── Budget Head ────────────────────────────────────────────────────────────

    @GetMapping("/budget/heads/list")
    @ResponseBody
    public DataTableResponse headList(
            @RequestParam(defaultValue="1") int draw, @RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="25") int length, @RequestParam(value="search[value]",defaultValue="") String search) {
        return budgetService.headDatatable(draw, start, length, search);
    }

    @GetMapping("/budget/heads/show/{id}")
    @ResponseBody
    public Map<String,Object> headShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", budgetService.findHeadById(id)));
    }

    @PostMapping("/budget/heads/save")
    @ResponseBody
    public Map<String,Object> headSave(@RequestBody @Valid BudgetHeadDTO dto) {
        return ok(() -> {
            if (dto.getId() != null) { budgetService.updateHead(dto.getId(), dto); return "Head updated."; }
            else { budgetService.createHead(dto); return "Head created."; }
        });
    }

    @PostMapping("/budget/heads/toggle/{id}")
    @ResponseBody
    public Map<String,Object> headToggle(@PathVariable Long id) {
        return ok(() -> { BudgetHeadDTO d = budgetService.toggleHead(id); return "Head " + (Boolean.TRUE.equals(d.getActive()) ? "activated" : "deactivated") + "."; });
    }

    @DeleteMapping("/budget/heads/delete/{id}")
    @ResponseBody
    public Map<String,Object> headDelete(@PathVariable Long id) {
        return ok(() -> { budgetService.deleteHead(id); return "Head deleted."; });
    }

    @GetMapping("/budget/heads/search")
    @ResponseBody
    public Map<String,Object> headSearch(@RequestParam(defaultValue="") String search, @RequestParam(defaultValue="1") int page) {
        return budgetService.searchHeads(search, page);
    }

    // ── Budget ─────────────────────────────────────────────────────────────────

    @GetMapping("/budget/list/data")
    @ResponseBody
    public DataTableResponse budgetList(
            @RequestParam(defaultValue="1") int draw, @RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="25") int length, @RequestParam(value="search[value]",defaultValue="") String search,
            @RequestParam(defaultValue="") String status) {
        return budgetService.budgetDatatable(draw, start, length, search, status);
    }

    @GetMapping("/budget/show/{id}")
    @ResponseBody
    public Map<String,Object> budgetShow(@PathVariable Long id) {
        return ok(() -> Map.of("defaultData", budgetService.findBudgetById(id)));
    }

    @PostMapping("/budget/save")
    @ResponseBody
    public Map<String,Object> budgetSave(@RequestBody @Valid BudgetDTO dto) {
        return ok(() -> {
            BudgetDTO saved;
            if (dto.getId() != null) { saved = budgetService.updateBudget(dto.getId(), dto); return "Budget updated."; }
            else { saved = budgetService.createBudget(dto); return "Budget " + saved.getBudgetNo() + " created."; }
        });
    }

    @PostMapping("/budget/submit/{id}")
    @ResponseBody public Map<String,Object> submit(@PathVariable Long id) { return ok(() -> { budgetService.submitBudget(id); return "Budget submitted."; }); }

    @PostMapping("/budget/approve/{id}")
    @ResponseBody public Map<String,Object> approve(@PathVariable Long id) { return ok(() -> { budgetService.approveBudget(id); return "Budget approved."; }); }

    @PostMapping("/budget/activate/{id}")
    @ResponseBody public Map<String,Object> activate(@PathVariable Long id) { return ok(() -> { budgetService.activateBudget(id); return "Budget activated."; }); }

    @PostMapping("/budget/lock/{id}")
    @ResponseBody public Map<String,Object> lock(@PathVariable Long id) { return ok(() -> { budgetService.lockBudget(id); return "Budget locked."; }); }

    @PostMapping("/budget/close/{id}")
    @ResponseBody public Map<String,Object> close(@PathVariable Long id) { return ok(() -> { budgetService.closeBudget(id); return "Budget closed."; }); }

    @PostMapping("/budget/return/{id}")
    @ResponseBody
    public Map<String,Object> returnBudget(@PathVariable Long id, @RequestParam(required=false) String remarks) {
        return ok(() -> { budgetService.returnBudget(id, remarks); return "Budget returned."; });
    }

    @DeleteMapping("/budget/delete/{id}")
    @ResponseBody public Map<String,Object> delete(@PathVariable Long id) { return ok(() -> { budgetService.deleteBudget(id); return "Budget deleted."; }); }

    @GetMapping("/budget/variance/{id}")
    @ResponseBody public List<Map<String,Object>> variance(@PathVariable Long id) { return budgetService.varianceReport(id); }

    @GetMapping("/budget/summary/{id}")
    @ResponseBody public Map<String,Object> summary(@PathVariable Long id) { return budgetService.budgetSummary(id); }

    @GetMapping("/budget/search")
    @ResponseBody public Map<String,Object> budgetSearch(@RequestParam(defaultValue="") String search, @RequestParam(defaultValue="1") int page) { return budgetService.searchBudgets(search, page); }

    // ── Revisions ──────────────────────────────────────────────────────────────

    @GetMapping("/budget/revisions/list")
    @ResponseBody
    public DataTableResponse revList(
            @RequestParam(defaultValue="1") int draw, @RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="25") int length, @RequestParam(value="search[value]",defaultValue="") String search,
            @RequestParam(required=false) Long budgetId) {
        return budgetService.revisionDatatable(draw, start, length, search, budgetId);
    }

    @GetMapping("/budget/revisions/show/{id}")
    @ResponseBody public Map<String,Object> revShow(@PathVariable Long id) { return ok(() -> Map.of("defaultData", budgetService.findRevisionById(id))); }

    @PostMapping("/budget/revisions/save")
    @ResponseBody
    public Map<String,Object> revSave(@RequestBody @Valid BudgetRevisionDTO dto) {
        return ok(() -> { BudgetRevisionDTO saved = budgetService.createRevision(dto); return "Revision " + saved.getRevisionNo() + " created."; });
    }

    @PostMapping("/budget/revisions/approve/{id}")
    @ResponseBody public Map<String,Object> revApprove(@PathVariable Long id) { return ok(() -> { budgetService.approveRevision(id); return "Revision approved and applied."; }); }

    @PostMapping("/budget/revisions/reject/{id}")
    @ResponseBody public Map<String,Object> revReject(@PathVariable Long id, @RequestParam(required=false) String remarks) { return ok(() -> { budgetService.rejectRevision(id, remarks); return "Revision rejected."; }); }

    // ── Transfers ──────────────────────────────────────────────────────────────

    @GetMapping("/budget/transfers/list")
    @ResponseBody
    public DataTableResponse tfrList(
            @RequestParam(defaultValue="1") int draw, @RequestParam(defaultValue="0") int start,
            @RequestParam(defaultValue="25") int length, @RequestParam(value="search[value]",defaultValue="") String search,
            @RequestParam(required=false) Long budgetId) {
        return budgetService.transferDatatable(draw, start, length, search, budgetId);
    }

    @PostMapping("/budget/transfers/save")
    @ResponseBody
    public Map<String,Object> tfrSave(@RequestBody @Valid BudgetTransferDTO dto) {
        return ok(() -> { BudgetTransferDTO saved = budgetService.createTransfer(dto); return "Transfer " + saved.getTransferNo() + " created."; });
    }

    @PostMapping("/budget/transfers/approve/{id}")
    @ResponseBody public Map<String,Object> tfrApprove(@PathVariable Long id) { return ok(() -> { budgetService.approveTransfer(id); return "Transfer approved and applied."; }); }

    @PostMapping("/budget/transfers/reject/{id}")
    @ResponseBody public Map<String,Object> tfrReject(@PathVariable Long id) { return ok(() -> { budgetService.rejectTransfer(id); return "Transfer rejected."; }); }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Map<String,Object> ok(Checked action) {
        Map<String,Object> res = new HashMap<>();
        try {
            Object r = action.run();
            res.put("success", true);
            if (r instanceof String msg) res.put("message", msg);
            else if (r instanceof Map<?,?> m) res.put("obj", m);
        } catch (Exception e) { res.put("success", false); res.put("message", e.getMessage()); }
        return res;
    }

    @FunctionalInterface interface Checked { Object run() throws Exception; }
}
