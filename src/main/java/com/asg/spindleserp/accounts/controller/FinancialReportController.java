package com.asg.spindleserp.accounts.controller;

import com.asg.spindleserp.accounts.service.FinancialReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FinancialReportController — all financial reporting pages and data endpoints.
 *
 * Pages (Thymeleaf):
 *   GET /accounts/reports/day-book            → day-book.html
 *   GET /accounts/reports/voucher-register    → voucher-register.html
 *   GET /accounts/reports/cash-flow           → cash-flow.html
 *   GET /accounts/reports/bank-book           → bank-book.html
 *   GET /accounts/reports/cash-book           → cash-book.html
 *   GET /accounts/reports/party-ledger        → party-ledger.html
 *   GET /accounts/reports/comparative-pl      → comparative-pl.html
 *   GET /accounts/reports/comparative-tb      → comparative-trial-balance.html
 *   GET /accounts/reports/tax-summary         → tax-summary.html
 *   GET /accounts/reports/financial-kpis      → financial-kpis.html
 *   GET /accounts/reports/cost-center-summary → cost-center-summary.html
 *   GET /accounts/reports/account-summary     → account-summary.html
 *   GET /accounts/reports/sub-account-summary → sub-account-summary.html
 *   GET /accounts/reports/budget-vs-actual    → budget-vs-actual.html
 *
 * Data endpoints (JSON):
 *   GET /accounts/reports/data/day-book?startDate=&endDate=
 *   GET /accounts/reports/data/voucher-register?voucherType=&startDate=&endDate=
 *   GET /accounts/reports/data/cash-flow?startDate=&endDate=
 *   GET /accounts/reports/data/bank-book?bankAccountId=&startDate=&endDate=
 *   GET /accounts/reports/data/cash-book?cashAccountId=&startDate=&endDate=
 *   GET /accounts/reports/data/party-ledger?partyId=&partyType=&startDate=&endDate=
 *   GET /accounts/reports/data/comparative-pl?startDate=&endDate=&prevStartDate=&prevEndDate=
 *   GET /accounts/reports/data/comparative-tb?asOfDate=&prevAsOfDate=
 *   GET /accounts/reports/data/tax-summary?startDate=&endDate=
 *   GET /accounts/reports/data/financial-kpis?asOfDate=
 *   GET /accounts/reports/data/cost-center-summary?startDate=&endDate=
 *   GET /accounts/reports/data/account-summary?accountId=&startDate=&endDate=
 *   GET /accounts/reports/data/sub-account-summary?subAccountType=
 *   GET /accounts/reports/data/budget-vs-actual?budgetId=&startDate=&endDate=
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/accounts/reports")
public class FinancialReportController {

    private final FinancialReportService reportService;

    // ═════════════════════════════════════════════════════════════════════════
    // PAGES
    // ═════════════════════════════════════════════════════════════════════════

    @GetMapping("/day-book")
    public String dayBookPage(Model model) {
        model.addAttribute("activePage", "rep-day-book");
        return "accounts/day-book";
    }

    @GetMapping("/voucher-register")
    public String voucherRegisterPage(Model model) {
        model.addAttribute("activePage", "rep-voucher-register");
        return "accounts/voucher-register";
    }

    @GetMapping("/cash-flow")
    public String cashFlowPage(Model model) {
        model.addAttribute("activePage", "rep-cash-flow");
        return "accounts/cash-flow";
    }

    @GetMapping("/bank-book")
    public String bankBookPage(Model model) {
        model.addAttribute("activePage", "rep-bank-book");
        return "accounts/bank-book";
    }

    @GetMapping("/cash-book")
    public String cashBookPage(Model model) {
        model.addAttribute("activePage", "rep-cash-book");
        return "accounts/cash-book";
    }

    @GetMapping("/party-ledger")
    public String partyLedgerPage(Model model) {
        model.addAttribute("activePage", "rep-party-ledger");
        return "accounts/party-ledger";
    }

    @GetMapping("/comparative-pl")
    public String comparativePLPage(Model model) {
        model.addAttribute("activePage", "rep-comparative-pl");
        return "accounts/comparative-pl";
    }

    @GetMapping("/comparative-tb")
    public String comparativeTBPage(Model model) {
        model.addAttribute("activePage", "rep-comparative-tb");
        return "accounts/comparative-trial-balance";
    }

    @GetMapping("/tax-summary")
    public String taxSummaryPage(Model model) {
        model.addAttribute("activePage", "rep-tax-summary");
        return "accounts/tax-summary";
    }

    @GetMapping("/financial-kpis")
    public String financialKpisPage(Model model) {
        model.addAttribute("activePage", "rep-financial-kpis");
        return "accounts/financial-kpis";
    }

    @GetMapping("/cost-center-summary")
    public String costCenterSummaryPage(Model model) {
        model.addAttribute("activePage", "rep-cost-center");
        return "accounts/cost-center-summary";
    }

    @GetMapping("/account-summary")
    public String accountSummaryPage(Model model) {
        model.addAttribute("activePage", "rep-account-summary");
        return "accounts/account-summary";
    }

    @GetMapping("/sub-account-summary")
    public String subAccountSummaryPage(Model model) {
        model.addAttribute("activePage", "rep-sub-account-summary");
        return "accounts/sub-account-summary";
    }

    @GetMapping("/budget-vs-actual")
    public String budgetVsActualPage(Model model) {
        model.addAttribute("activePage", "rep-budget-actual");
        return "accounts/budget-vs-actual";
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DATA ENDPOINTS
    // ═════════════════════════════════════════════════════════════════════════

    @GetMapping("/data/day-book")
    @ResponseBody
    public Map<String, Object> dayBookData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.dayBook(startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/voucher-register")
    @ResponseBody
    public Map<String, Object> voucherRegisterData(
            @RequestParam(required = false) String voucherType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.voucherRegister(voucherType, startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/cash-flow")
    @ResponseBody
    public Map<String, Object> cashFlowData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.cashFlowStatement(startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/bank-book")
    @ResponseBody
    public Map<String, Object> bankBookData(
            @RequestParam Long bankAccountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.bankBook(bankAccountId, startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/cash-book")
    @ResponseBody
    public Map<String, Object> cashBookData(
            @RequestParam Long cashAccountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.cashBook(cashAccountId, startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/party-ledger")
    @ResponseBody
    public Map<String, Object> partyLedgerData(
            @RequestParam Long partyId,
            @RequestParam(defaultValue = "CUSTOMER") String partyType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.partyLedger(partyId, partyType, startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/comparative-pl")
    @ResponseBody
    public Map<String, Object> comparativePLData(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String prevStartDate,
            @RequestParam(required = false) String prevEndDate) {
        try { return reportService.comparativePL(startDate, endDate, prevStartDate, prevEndDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/comparative-tb")
    @ResponseBody
    public Map<String, Object> comparativeTBData(
            @RequestParam(required = false) String asOfDate,
            @RequestParam(required = false) String prevAsOfDate) {
        try { return reportService.comparativeTrialBalance(asOfDate, prevAsOfDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/tax-summary")
    @ResponseBody
    public Map<String, Object> taxSummaryData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.taxSummary(startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/financial-kpis")
    @ResponseBody
    public Map<String, Object> financialKpisData(
            @RequestParam(required = false) String asOfDate) {
        try { return reportService.financialKpis(asOfDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/cost-center-summary")
    @ResponseBody
    public Map<String, Object> costCenterSummaryData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.costCenterSummary(startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/account-summary")
    @ResponseBody
    public Map<String, Object> accountSummaryData(
            @RequestParam Long accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.accountTransactionSummary(accountId, startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/sub-account-summary")
    @ResponseBody
    public Map<String, Object> subAccountSummaryData(
            @RequestParam(defaultValue = "CUSTOMER") String subAccountType) {
        try { return reportService.subAccountSummary(subAccountType); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }

    @GetMapping("/data/budget-vs-actual")
    @ResponseBody
    public Map<String, Object> budgetVsActualData(
            @RequestParam Long budgetId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try { return reportService.budgetVsActual(budgetId, startDate, endDate); }
        catch (Exception e) { return Map.of("error", e.getMessage()); }
    }
}
